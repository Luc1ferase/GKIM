use crate::im::model::{
    ContactProfile, ConversationSummary, DeliveryUpdate, MessageRecord, ReadReceiptUpdate,
    SendMessageResult, UserProfile,
};
use anyhow::{anyhow, Context, Result};
use sqlx::{postgres::PgRow, PgPool, Postgres, Row, Transaction};

const RFC3339_UTC_SQL: &str = r#"YYYY-MM-DD"T"HH24:MI:SS"Z""#;

#[derive(Debug, Clone)]
pub struct ImRepository {
    pool: PgPool,
}

#[derive(Debug, Clone)]
pub struct MessageCursor {
    pub message_id: String,
    pub created_at: String,
}

impl ImRepository {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }

    pub async fn find_user_by_external_id(&self, external_id: &str) -> Result<Option<UserProfile>> {
        let row = sqlx::query(
            "select
                id::text as id,
                external_id,
                display_name,
                title,
                avatar_text
             from users
             where external_id = $1",
        )
        .bind(external_id)
        .fetch_optional(&self.pool)
        .await
        .with_context(|| format!("load user `{external_id}` by external id"))?;

        row.map(map_user).transpose()
    }

    pub async fn list_contacts(&self, user_id: &str) -> Result<Vec<ContactProfile>> {
        let query = format!(
            "select
                u.id::text as user_id,
                u.external_id,
                u.display_name,
                u.title,
                u.avatar_text,
                to_char(c.created_at at time zone 'utc', '{RFC3339_UTC_SQL}') as added_at
             from contacts c
             join users u on u.id = c.contact_user_id
             where c.user_id::text = $1
             order by c.created_at asc, u.display_name asc"
        );

        let rows = sqlx::query(&query)
            .bind(user_id)
            .fetch_all(&self.pool)
            .await
            .with_context(|| format!("list contacts for user `{user_id}`"))?;

        rows.into_iter().map(map_contact).collect()
    }

    pub async fn list_conversation_summaries(
        &self,
        user_id: &str,
    ) -> Result<Vec<ConversationSummary>> {
        let query = format!(
            "select
                cm.conversation_id::text as conversation_id,
                peer.id::text as contact_user_id,
                peer.external_id as contact_external_id,
                peer.display_name as contact_display_name,
                peer.title as contact_title,
                peer.avatar_text as contact_avatar_text,
                to_char(coalesce(c.created_at, cm.joined_at) at time zone 'utc', '{RFC3339_UTC_SQL}') as contact_added_at,
                cm.unread_count,
                last_message.id::text as last_message_id,
                last_message.conversation_id::text as last_message_conversation_id,
                last_message.sender_user_id::text as last_message_sender_user_id,
                last_sender.external_id as last_message_sender_external_id,
                last_message.message_kind as last_message_kind,
                last_message.body as last_message_body,
                to_char(last_message.created_at at time zone 'utc', '{RFC3339_UTC_SQL}') as last_message_created_at,
                to_char(last_receipt.delivered_at at time zone 'utc', '{RFC3339_UTC_SQL}') as last_message_delivered_at,
                to_char(last_receipt.read_at at time zone 'utc', '{RFC3339_UTC_SQL}') as last_message_read_at
             from conversation_members cm
             join users peer on peer.id = cm.peer_user_id
             left join contacts c
                on c.user_id = cm.user_id
               and c.contact_user_id = cm.peer_user_id
             left join lateral (
                select m.*
                from messages m
                where m.conversation_id = cm.conversation_id
                order by m.created_at desc, m.id desc
                limit 1
             ) last_message on true
             left join users last_sender on last_sender.id = last_message.sender_user_id
             left join message_receipts last_receipt
                on last_receipt.message_id = last_message.id
               and last_receipt.user_id = case
                    when last_message.sender_user_id = cm.user_id then cm.peer_user_id
                    else cm.user_id
               end
             where cm.user_id::text = $1
             order by coalesce(last_message.created_at, cm.joined_at) desc, cm.conversation_id desc"
        );

        let rows = sqlx::query(&query)
            .bind(user_id)
            .fetch_all(&self.pool)
            .await
            .with_context(|| format!("list conversation summaries for user `{user_id}`"))?;

        rows.into_iter().map(map_conversation_summary).collect()
    }

    pub async fn resolve_history_cursor(
        &self,
        user_id: &str,
        conversation_id: &str,
        message_id: &str,
    ) -> Result<Option<MessageCursor>> {
        let query = format!(
            "select
                m.id::text as message_id,
                to_char(m.created_at at time zone 'utc', '{RFC3339_UTC_SQL}') as created_at
             from conversation_members cm
             join messages m on m.conversation_id = cm.conversation_id
             where cm.user_id::text = $1
               and cm.conversation_id::text = $2
               and m.id::text = $3"
        );

        let row = sqlx::query(&query)
            .bind(user_id)
            .bind(conversation_id)
            .bind(message_id)
            .fetch_optional(&self.pool)
            .await
            .with_context(|| {
                format!(
                    "resolve history cursor `{message_id}` in conversation `{conversation_id}` for user `{user_id}`"
                )
            })?;

        row.map(|row| {
            Ok(MessageCursor {
                message_id: required_string(&row, "message_id")?,
                created_at: required_string(&row, "created_at")?,
            })
        })
        .transpose()
    }

    pub async fn list_message_history(
        &self,
        user_id: &str,
        conversation_id: &str,
        limit: i64,
        before: Option<&MessageCursor>,
    ) -> Result<Vec<MessageRecord>> {
        let query = format!(
            "select
                m.id::text as id,
                m.conversation_id::text as conversation_id,
                m.sender_user_id::text as sender_user_id,
                sender.external_id as sender_external_id,
                m.message_kind as kind,
                m.body,
                to_char(m.created_at at time zone 'utc', '{RFC3339_UTC_SQL}') as created_at,
                to_char(receipt.delivered_at at time zone 'utc', '{RFC3339_UTC_SQL}') as delivered_at,
                to_char(receipt.read_at at time zone 'utc', '{RFC3339_UTC_SQL}') as read_at
             from conversation_members cm
             join messages m on m.conversation_id = cm.conversation_id
             join users sender on sender.id = m.sender_user_id
             left join message_receipts receipt
                on receipt.message_id = m.id
               and receipt.user_id = case
                    when m.sender_user_id = cm.user_id then cm.peer_user_id
                    else cm.user_id
               end
             where cm.user_id::text = $1
               and cm.conversation_id::text = $2
               and (
                    $3::timestamptz is null
                    or (m.created_at, m.id) < (($3)::timestamptz, ($4)::uuid)
               )
             order by m.created_at desc, m.id desc
             limit $5"
        );

        let rows = sqlx::query(&query)
            .bind(user_id)
            .bind(conversation_id)
            .bind(before.map(|cursor| cursor.created_at.as_str()))
            .bind(before.map(|cursor| cursor.message_id.as_str()))
            .bind(limit)
            .fetch_all(&self.pool)
            .await
            .with_context(|| {
                format!(
                    "list message history for user `{user_id}` in conversation `{conversation_id}`"
                )
            })?;

        rows.into_iter().map(map_message).collect()
    }

    pub async fn persist_direct_message(
        &self,
        sender_external_id: &str,
        recipient_external_id: &str,
        client_message_id: Option<&str>,
        body: &str,
    ) -> Result<SendMessageResult> {
        let mut tx = self
            .pool
            .begin()
            .await
            .context("begin direct-message transaction")?;

        let sender = user_by_external_id_tx(&mut tx, sender_external_id)
            .await?
            .ok_or_else(|| anyhow!("sender `{sender_external_id}` was not found"))?;
        let recipient = user_by_external_id_tx(&mut tx, recipient_external_id)
            .await?
            .ok_or_else(|| anyhow!("recipient `{recipient_external_id}` was not found"))?;

        if sender.id == recipient.id {
            return Err(anyhow!("sender and recipient must be different users"));
        }

        let conversation_id =
            find_or_create_direct_conversation_tx(&mut tx, &sender.id, &recipient.id).await?;
        ensure_contact_pair_tx(&mut tx, &sender.id, &recipient.id).await?;
        ensure_member_tx(&mut tx, &conversation_id, &sender.id, &recipient.id).await?;
        ensure_member_tx(&mut tx, &conversation_id, &recipient.id, &sender.id).await?;

        let query = format!(
            "insert into messages (conversation_id, sender_user_id, client_message_id, body)
             values (($1)::uuid, ($2)::uuid, $3, $4)
             returning
                id::text as id,
                conversation_id::text as conversation_id,
                sender_user_id::text as sender_user_id,
                message_kind as kind,
                body,
                to_char(created_at at time zone 'utc', '{RFC3339_UTC_SQL}') as created_at"
        );

        let row = sqlx::query(&query)
            .bind(&conversation_id)
            .bind(&sender.id)
            .bind(client_message_id)
            .bind(body)
            .fetch_one(&mut *tx)
            .await
            .with_context(|| {
                format!("insert message from `{sender_external_id}` to `{recipient_external_id}`")
            })?;

        sqlx::query(
            "insert into message_receipts (message_id, user_id)
             values (($1)::uuid, ($2)::uuid)
             on conflict (message_id, user_id) do nothing",
        )
        .bind(
            row.try_get::<String, _>("id")
                .context("read inserted `id`")?,
        )
        .bind(&recipient.id)
        .execute(&mut *tx)
        .await
        .context("insert initial recipient receipt row")?;

        let unread_count = sqlx::query_scalar::<_, i64>(
            "update conversation_members
             set unread_count = unread_count + 1
             where conversation_id = ($1)::uuid
               and user_id = ($2)::uuid
             returning unread_count::bigint",
        )
        .bind(&conversation_id)
        .bind(&recipient.id)
        .fetch_one(&mut *tx)
        .await
        .context("increment recipient unread count")?;

        sqlx::query(
            "update direct_conversations
             set updated_at = timezone('utc', now())
             where id = ($1)::uuid",
        )
        .bind(&conversation_id)
        .execute(&mut *tx)
        .await
        .context("touch direct conversation updated_at")?;

        tx.commit()
            .await
            .context("commit direct-message transaction")?;

        Ok(SendMessageResult {
            conversation_id,
            recipient_external_id: recipient.external_id,
            recipient_unread_count: unread_count,
            message: MessageRecord {
                id: row.try_get("id").context("read inserted message `id`")?,
                conversation_id: row
                    .try_get("conversation_id")
                    .context("read inserted message `conversation_id`")?,
                sender_user_id: row
                    .try_get("sender_user_id")
                    .context("read inserted message `sender_user_id`")?,
                sender_external_id: sender.external_id,
                kind: row
                    .try_get("kind")
                    .context("read inserted message `kind`")?,
                body: row
                    .try_get("body")
                    .context("read inserted message `body`")?,
                created_at: row
                    .try_get("created_at")
                    .context("read inserted message `created_at`")?,
                delivered_at: None,
                read_at: None,
            },
        })
    }

    pub async fn mark_message_delivered(
        &self,
        recipient_external_id: &str,
        conversation_id: &str,
        message_id: &str,
    ) -> Result<DeliveryUpdate> {
        let mut tx = self
            .pool
            .begin()
            .await
            .context("begin message-delivered transaction")?;
        let recipient = user_by_external_id_tx(&mut tx, recipient_external_id)
            .await?
            .ok_or_else(|| anyhow!("recipient `{recipient_external_id}` was not found"))?;

        let query = format!(
            "insert into message_receipts (message_id, user_id, delivered_at)
             values (($1)::uuid, ($2)::uuid, timezone('utc', now()))
             on conflict (message_id, user_id) do update
                set delivered_at = coalesce(message_receipts.delivered_at, excluded.delivered_at)
             returning to_char(delivered_at at time zone 'utc', '{RFC3339_UTC_SQL}') as delivered_at"
        );
        let row = sqlx::query(&query)
            .bind(message_id)
            .bind(&recipient.id)
            .fetch_one(&mut *tx)
            .await
            .with_context(|| {
                format!(
                    "mark message `{message_id}` delivered for recipient `{recipient_external_id}`"
                )
            })?;
        let delivered_at: String = row
            .try_get("delivered_at")
            .context("read `delivered_at` from receipt upsert")?;

        sqlx::query(
            "update conversation_members
             set last_delivered_message_id = ($3)::uuid,
                 last_delivered_at = ($4)::timestamptz
             where conversation_id = ($1)::uuid
               and user_id = ($2)::uuid",
        )
        .bind(conversation_id)
        .bind(&recipient.id)
        .bind(message_id)
        .bind(&delivered_at)
        .execute(&mut *tx)
        .await
        .context("update recipient last delivered state")?;

        tx.commit()
            .await
            .context("commit message-delivered transaction")?;

        Ok(DeliveryUpdate {
            conversation_id: conversation_id.to_string(),
            message_id: message_id.to_string(),
            recipient_external_id: recipient.external_id,
            delivered_at,
        })
    }

    pub async fn mark_message_read(
        &self,
        reader_external_id: &str,
        conversation_id: &str,
        message_id: &str,
    ) -> Result<ReadReceiptUpdate> {
        let mut tx = self
            .pool
            .begin()
            .await
            .context("begin message-read transaction")?;
        let reader = user_by_external_id_tx(&mut tx, reader_external_id)
            .await?
            .ok_or_else(|| anyhow!("reader `{reader_external_id}` was not found"))?;

        let sender_external_id: String = sqlx::query_scalar(
            "select sender.external_id
             from messages m
             join users sender on sender.id = m.sender_user_id
             join conversation_members cm on cm.conversation_id = m.conversation_id
             where m.conversation_id = ($1)::uuid
               and m.id = ($2)::uuid
               and cm.user_id = ($3)::uuid",
        )
        .bind(conversation_id)
        .bind(message_id)
        .bind(&reader.id)
        .fetch_one(&mut *tx)
        .await
        .with_context(|| {
            format!("load sender for message `{message_id}` in conversation `{conversation_id}`")
        })?;

        let query = format!(
            "insert into message_receipts (message_id, user_id, delivered_at, read_at)
             values (($1)::uuid, ($2)::uuid, timezone('utc', now()), timezone('utc', now()))
             on conflict (message_id, user_id) do update
                set delivered_at = coalesce(message_receipts.delivered_at, excluded.delivered_at),
                    read_at = coalesce(message_receipts.read_at, excluded.read_at)
             returning to_char(read_at at time zone 'utc', '{RFC3339_UTC_SQL}') as read_at"
        );
        let row = sqlx::query(&query)
            .bind(message_id)
            .bind(&reader.id)
            .fetch_one(&mut *tx)
            .await
            .with_context(|| {
                format!("mark message `{message_id}` read for reader `{reader_external_id}`")
            })?;
        let read_at: String = row
            .try_get("read_at")
            .context("read `read_at` from receipt upsert")?;

        let unread_count = sqlx::query_scalar::<_, i64>(
            "update conversation_members
             set last_delivered_message_id = ($3)::uuid,
                 last_delivered_at = coalesce(last_delivered_at, ($4)::timestamptz),
                 last_read_message_id = ($3)::uuid,
                 last_read_at = ($4)::timestamptz,
                 unread_count = (
                    select count(*)::bigint
                    from messages m
                    left join message_receipts r
                      on r.message_id = m.id
                     and r.user_id = conversation_members.user_id
                    where m.conversation_id = conversation_members.conversation_id
                      and m.sender_user_id <> conversation_members.user_id
                      and r.read_at is null
                 )
             where conversation_id = ($1)::uuid
               and user_id = ($2)::uuid
             returning unread_count::bigint",
        )
        .bind(conversation_id)
        .bind(&reader.id)
        .bind(message_id)
        .bind(&read_at)
        .fetch_one(&mut *tx)
        .await
        .context("update reader unread state after read receipt")?;

        tx.commit()
            .await
            .context("commit message-read transaction")?;

        Ok(ReadReceiptUpdate {
            conversation_id: conversation_id.to_string(),
            message_id: message_id.to_string(),
            reader_external_id: reader.external_id,
            sender_external_id,
            unread_count,
            read_at,
        })
    }
}

async fn user_by_external_id_tx(
    tx: &mut Transaction<'_, Postgres>,
    external_id: &str,
) -> Result<Option<UserProfile>> {
    let row = sqlx::query(
        "select
            id::text as id,
            external_id,
            display_name,
            title,
            avatar_text
         from users
         where external_id = $1",
    )
    .bind(external_id)
    .fetch_optional(&mut **tx)
    .await
    .with_context(|| format!("load user `{external_id}` inside transaction"))?;

    row.map(map_user).transpose()
}

async fn find_or_create_direct_conversation_tx(
    tx: &mut Transaction<'_, Postgres>,
    user_a_id: &str,
    user_b_id: &str,
) -> Result<String> {
    let existing = sqlx::query_scalar(
        "select id::text
         from direct_conversations
         where least(participant_a_user_id, participant_b_user_id) = least(($1)::uuid, ($2)::uuid)
           and greatest(participant_a_user_id, participant_b_user_id) = greatest(($1)::uuid, ($2)::uuid)
         limit 1",
    )
    .bind(user_a_id)
    .bind(user_b_id)
    .fetch_optional(&mut **tx)
    .await
    .context("load existing direct conversation")?;

    if let Some(conversation_id) = existing {
        return Ok(conversation_id);
    }

    sqlx::query_scalar(
        "insert into direct_conversations (participant_a_user_id, participant_b_user_id, updated_at)
         values (($1)::uuid, ($2)::uuid, timezone('utc', now()))
         returning id::text",
    )
    .bind(user_a_id)
    .bind(user_b_id)
    .fetch_one(&mut **tx)
    .await
    .context("create direct conversation")
}

async fn ensure_contact_pair_tx(
    tx: &mut Transaction<'_, Postgres>,
    user_a_id: &str,
    user_b_id: &str,
) -> Result<()> {
    sqlx::query(
        "insert into contacts (user_id, contact_user_id)
         values (($1)::uuid, ($2)::uuid), (($2)::uuid, ($1)::uuid)
         on conflict (user_id, contact_user_id) do nothing",
    )
    .bind(user_a_id)
    .bind(user_b_id)
    .execute(&mut **tx)
    .await
    .context("ensure reciprocal contacts")?;

    Ok(())
}

async fn ensure_member_tx(
    tx: &mut Transaction<'_, Postgres>,
    conversation_id: &str,
    user_id: &str,
    peer_user_id: &str,
) -> Result<()> {
    sqlx::query(
        "insert into conversation_members (conversation_id, user_id, peer_user_id)
         values (($1)::uuid, ($2)::uuid, ($3)::uuid)
         on conflict (conversation_id, user_id) do nothing",
    )
    .bind(conversation_id)
    .bind(user_id)
    .bind(peer_user_id)
    .execute(&mut **tx)
    .await
    .context("ensure conversation member")?;

    Ok(())
}

fn map_user(row: PgRow) -> Result<UserProfile> {
    Ok(UserProfile {
        id: required_string(&row, "id")?,
        external_id: required_string(&row, "external_id")?,
        display_name: required_string(&row, "display_name")?,
        title: required_string(&row, "title")?,
        avatar_text: required_string(&row, "avatar_text")?,
    })
}

fn map_contact(row: PgRow) -> Result<ContactProfile> {
    Ok(ContactProfile {
        user_id: required_string(&row, "user_id")?,
        external_id: required_string(&row, "external_id")?,
        display_name: required_string(&row, "display_name")?,
        title: required_string(&row, "title")?,
        avatar_text: required_string(&row, "avatar_text")?,
        added_at: required_string(&row, "added_at")?,
    })
}

fn map_conversation_summary(row: PgRow) -> Result<ConversationSummary> {
    let contact = ContactProfile {
        user_id: required_string(&row, "contact_user_id")?,
        external_id: required_string(&row, "contact_external_id")?,
        display_name: required_string(&row, "contact_display_name")?,
        title: required_string(&row, "contact_title")?,
        avatar_text: required_string(&row, "contact_avatar_text")?,
        added_at: required_string(&row, "contact_added_at")?,
    };

    let last_message = if let Some(id) = optional_string(&row, "last_message_id")? {
        Some(MessageRecord {
            id,
            conversation_id: required_string(&row, "last_message_conversation_id")?,
            sender_user_id: required_string(&row, "last_message_sender_user_id")?,
            sender_external_id: required_string(&row, "last_message_sender_external_id")?,
            kind: required_string(&row, "last_message_kind")?,
            body: required_string(&row, "last_message_body")?,
            created_at: required_string(&row, "last_message_created_at")?,
            delivered_at: optional_string(&row, "last_message_delivered_at")?,
            read_at: optional_string(&row, "last_message_read_at")?,
        })
    } else {
        None
    };

    Ok(ConversationSummary {
        conversation_id: required_string(&row, "conversation_id")?,
        contact,
        unread_count: row
            .try_get::<i32, _>("unread_count")
            .context("read `unread_count` from conversation summary")?
            .into(),
        last_message,
    })
}

fn map_message(row: PgRow) -> Result<MessageRecord> {
    Ok(MessageRecord {
        id: required_string(&row, "id")?,
        conversation_id: required_string(&row, "conversation_id")?,
        sender_user_id: required_string(&row, "sender_user_id")?,
        sender_external_id: required_string(&row, "sender_external_id")?,
        kind: required_string(&row, "kind")?,
        body: required_string(&row, "body")?,
        created_at: required_string(&row, "created_at")?,
        delivered_at: optional_string(&row, "delivered_at")?,
        read_at: optional_string(&row, "read_at")?,
    })
}

fn required_string(row: &PgRow, column: &str) -> Result<String> {
    row.try_get::<String, _>(column)
        .with_context(|| format!("read required column `{column}`"))
}

fn optional_string(row: &PgRow, column: &str) -> Result<Option<String>> {
    row.try_get::<Option<String>, _>(column)
        .with_context(|| format!("read optional column `{column}`"))
}
