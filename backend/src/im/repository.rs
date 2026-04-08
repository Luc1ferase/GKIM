use crate::im::model::{ContactProfile, ConversationSummary, MessageRecord, UserProfile};
use anyhow::{Context, Result};
use sqlx::{postgres::PgRow, PgPool, Row};

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
               and last_receipt.user_id = cm.user_id
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
               and receipt.user_id = cm.user_id
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
