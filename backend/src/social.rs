use crate::im::model::UserProfile;
use anyhow::{Context, Result};
use sqlx::{PgPool, Row};

const RFC3339_UTC_SQL: &str = r#"YYYY-MM-DD"T"HH24:MI:SS"Z""#;

#[derive(Debug, Clone)]
pub struct SocialService {
    pool: PgPool,
}

#[derive(Debug, Clone, serde::Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UserSearchResult {
    pub id: String,
    pub username: String,
    pub display_name: String,
    pub avatar_text: String,
    pub contact_status: ContactStatus,
}

#[derive(Debug, Clone, serde::Serialize)]
#[serde(rename_all = "snake_case")]
pub enum ContactStatus {
    Contact,
    PendingSent,
    PendingReceived,
    None,
}

#[derive(Debug, Clone, serde::Serialize)]
#[serde(rename_all = "camelCase")]
pub struct FriendRequestView {
    pub id: String,
    pub from_user: UserProfile,
    pub to_user_id: String,
    pub to_user_external_id: String,
    pub status: String,
    pub created_at: String,
}

#[derive(Debug)]
pub enum FriendRequestError {
    AlreadyContacts,
    DuplicateRequest,
    NotFound,
    NotPending,
    Internal(anyhow::Error),
}

impl SocialService {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }

    pub async fn search_users(
        &self,
        requesting_user_id: &str,
        query: &str,
    ) -> Result<Vec<UserSearchResult>> {
        let query_param = format!("%{}%", query.trim());
        let rows = sqlx::query(
            "select
                u.id::text as id,
                u.username,
                u.display_name,
                u.avatar_text,
                case
                    when c.contact_user_id is not null then 'contact'
                    when fr_sent.id is not null then 'pending_sent'
                    when fr_recv.id is not null then 'pending_received'
                    else 'none'
                end as contact_status
             from users u
             left join contacts c
                on c.user_id = ($1)::uuid and c.contact_user_id = u.id
             left join friend_requests fr_sent
                on fr_sent.from_user_id = ($1)::uuid and fr_sent.to_user_id = u.id and fr_sent.status = 'pending'
             left join friend_requests fr_recv
                on fr_recv.to_user_id = ($1)::uuid and fr_recv.from_user_id = u.id and fr_recv.status = 'pending'
             where u.id <> ($1)::uuid
               and (lower(u.username) like lower($2) or lower(u.display_name) like lower($2))
             order by u.display_name asc
             limit 20",
        )
        .bind(requesting_user_id)
        .bind(&query_param)
        .fetch_all(&self.pool)
        .await
        .context("search users")?;

        rows.into_iter()
            .map(|row| {
                let status_str: String = row
                    .try_get("contact_status")
                    .context("read contact_status")?;
                let contact_status = match status_str.as_str() {
                    "contact" => ContactStatus::Contact,
                    "pending_sent" => ContactStatus::PendingSent,
                    "pending_received" => ContactStatus::PendingReceived,
                    _ => ContactStatus::None,
                };
                Ok(UserSearchResult {
                    id: row.try_get("id").context("read id")?,
                    username: row.try_get("username").context("read username")?,
                    display_name: row.try_get("display_name").context("read display_name")?,
                    avatar_text: row.try_get("avatar_text").context("read avatar_text")?,
                    contact_status,
                })
            })
            .collect()
    }

    pub async fn send_friend_request(
        &self,
        from_user_id: &str,
        to_user_id: &str,
    ) -> Result<FriendRequestView, FriendRequestError> {
        // Check if already contacts
        let is_contact = sqlx::query_scalar::<_, bool>(
            "select exists(
                select 1 from contacts
                where user_id = ($1)::uuid and contact_user_id = ($2)::uuid
            )",
        )
        .bind(from_user_id)
        .bind(to_user_id)
        .fetch_one(&self.pool)
        .await
        .map_err(|e| FriendRequestError::Internal(anyhow::Error::from(e)))?;

        if is_contact {
            return Err(FriendRequestError::AlreadyContacts);
        }

        let query = format!(
            "insert into friend_requests (from_user_id, to_user_id)
             values (($1)::uuid, ($2)::uuid)
             returning
                id::text as id,
                status,
                to_char(created_at at time zone 'utc', '{RFC3339_UTC_SQL}') as created_at"
        );

        let row = sqlx::query(&query)
            .bind(from_user_id)
            .bind(to_user_id)
            .fetch_one(&self.pool)
            .await
            .map_err(|e| {
                let msg = e.to_string();
                if msg.contains("friend_requests_pending_pair_idx") || msg.contains("unique") {
                    return FriendRequestError::DuplicateRequest;
                }
                FriendRequestError::Internal(anyhow::Error::from(e))
            })?;

        let from_user = self
            .load_user_profile(from_user_id)
            .await
            .map_err(FriendRequestError::Internal)?;
        let to_user = self
            .load_user_profile(to_user_id)
            .await
            .map_err(FriendRequestError::Internal)?;

        Ok(FriendRequestView {
            id: row
                .try_get("id")
                .map_err(|e| FriendRequestError::Internal(anyhow::Error::from(e)))?,
            from_user,
            to_user_id: to_user_id.to_string(),
            to_user_external_id: to_user.external_id,
            status: row
                .try_get("status")
                .map_err(|e| FriendRequestError::Internal(anyhow::Error::from(e)))?,
            created_at: row
                .try_get("created_at")
                .map_err(|e| FriendRequestError::Internal(anyhow::Error::from(e)))?,
        })
    }

    pub async fn list_pending_requests(&self, to_user_id: &str) -> Result<Vec<FriendRequestView>> {
        let query = format!(
            "select
                fr.id::text as id,
                fr.from_user_id::text as from_user_id,
                fr.to_user_id::text as to_user_id,
                fr.status,
                to_char(fr.created_at at time zone 'utc', '{RFC3339_UTC_SQL}') as created_at,
                u.id::text as user_id,
                u.external_id,
                u.display_name,
                u.title,
                u.avatar_text,
                tu.external_id as to_user_external_id
             from friend_requests fr
             join users u on u.id = fr.from_user_id
             join users tu on tu.id = fr.to_user_id
             where fr.to_user_id = ($1)::uuid and fr.status = 'pending'
             order by fr.created_at desc"
        );

        let rows = sqlx::query(&query)
            .bind(to_user_id)
            .fetch_all(&self.pool)
            .await
            .context("list pending friend requests")?;

        rows.into_iter()
            .map(|row| {
                Ok(FriendRequestView {
                    id: row.try_get("id").context("read id")?,
                    from_user: UserProfile {
                        id: row.try_get("user_id").context("read user_id")?,
                        external_id: row.try_get("external_id").context("read external_id")?,
                        display_name: row.try_get("display_name").context("read display_name")?,
                        title: row.try_get("title").context("read title")?,
                        avatar_text: row.try_get("avatar_text").context("read avatar_text")?,
                    },
                    to_user_id: row
                        .try_get::<String, _>("to_user_id")
                        .context("read to_user_id")?,
                    to_user_external_id: row
                        .try_get::<String, _>("to_user_external_id")
                        .context("read to_user_external_id")?,
                    status: row.try_get("status").context("read status")?,
                    created_at: row.try_get("created_at").context("read created_at")?,
                })
            })
            .collect()
    }

    pub async fn accept_friend_request(
        &self,
        request_id: &str,
        accepting_user_id: &str,
    ) -> Result<FriendRequestView, FriendRequestError> {
        let mut tx = self
            .pool
            .begin()
            .await
            .map_err(|e| FriendRequestError::Internal(anyhow::Error::from(e)))?;

        let query = format!(
            "update friend_requests
             set status = 'accepted', responded_at = timezone('utc', now())
             where id = ($1)::uuid and to_user_id = ($2)::uuid and status = 'pending'
             returning
                id::text as id,
                from_user_id::text as from_user_id,
                to_user_id::text as to_user_id,
                status,
                to_char(created_at at time zone 'utc', '{RFC3339_UTC_SQL}') as created_at"
        );

        let row = sqlx::query(&query)
            .bind(request_id)
            .bind(accepting_user_id)
            .fetch_optional(&mut *tx)
            .await
            .map_err(|e| FriendRequestError::Internal(anyhow::Error::from(e)))?;

        let row = row.ok_or(FriendRequestError::NotFound)?;

        let from_user_id: String = row
            .try_get("from_user_id")
            .map_err(|e| FriendRequestError::Internal(anyhow::Error::from(e)))?;

        // Create reciprocal contacts
        sqlx::query(
            "insert into contacts (user_id, contact_user_id)
             values (($1)::uuid, ($2)::uuid), (($2)::uuid, ($1)::uuid)
             on conflict (user_id, contact_user_id) do nothing",
        )
        .bind(&from_user_id)
        .bind(accepting_user_id)
        .execute(&mut *tx)
        .await
        .map_err(|e| {
            FriendRequestError::Internal(anyhow::Error::from(e).context("create contacts"))
        })?;

        tx.commit()
            .await
            .map_err(|e| FriendRequestError::Internal(anyhow::Error::from(e)))?;

        let from_user = self
            .load_user_profile(&from_user_id)
            .await
            .map_err(FriendRequestError::Internal)?;
        let to_user = self
            .load_user_profile(accepting_user_id)
            .await
            .map_err(FriendRequestError::Internal)?;

        Ok(FriendRequestView {
            id: row
                .try_get("id")
                .map_err(|e| FriendRequestError::Internal(anyhow::Error::from(e)))?,
            from_user,
            to_user_id: accepting_user_id.to_string(),
            to_user_external_id: to_user.external_id,
            status: row
                .try_get("status")
                .map_err(|e| FriendRequestError::Internal(anyhow::Error::from(e)))?,
            created_at: row
                .try_get("created_at")
                .map_err(|e| FriendRequestError::Internal(anyhow::Error::from(e)))?,
        })
    }

    pub async fn reject_friend_request(
        &self,
        request_id: &str,
        rejecting_user_id: &str,
    ) -> Result<FriendRequestView, FriendRequestError> {
        let query = format!(
            "update friend_requests
             set status = 'rejected', responded_at = timezone('utc', now())
             where id = ($1)::uuid and to_user_id = ($2)::uuid and status = 'pending'
             returning
                id::text as id,
                from_user_id::text as from_user_id,
                to_user_id::text as to_user_id,
                status,
                to_char(created_at at time zone 'utc', '{RFC3339_UTC_SQL}') as created_at"
        );

        let row = sqlx::query(&query)
            .bind(request_id)
            .bind(rejecting_user_id)
            .fetch_optional(&self.pool)
            .await
            .map_err(|e| FriendRequestError::Internal(anyhow::Error::from(e)))?;

        let row = row.ok_or(FriendRequestError::NotFound)?;

        let from_user_id: String = row
            .try_get("from_user_id")
            .map_err(|e| FriendRequestError::Internal(anyhow::Error::from(e)))?;

        let from_user = self
            .load_user_profile(&from_user_id)
            .await
            .map_err(FriendRequestError::Internal)?;
        let to_user = self
            .load_user_profile(rejecting_user_id)
            .await
            .map_err(FriendRequestError::Internal)?;

        Ok(FriendRequestView {
            id: row
                .try_get("id")
                .map_err(|e| FriendRequestError::Internal(anyhow::Error::from(e)))?,
            from_user,
            to_user_id: rejecting_user_id.to_string(),
            to_user_external_id: to_user.external_id,
            status: row
                .try_get("status")
                .map_err(|e| FriendRequestError::Internal(anyhow::Error::from(e)))?,
            created_at: row
                .try_get("created_at")
                .map_err(|e| FriendRequestError::Internal(anyhow::Error::from(e)))?,
        })
    }

    async fn load_user_profile(&self, user_id: &str) -> Result<UserProfile> {
        let row = sqlx::query(
            "select id::text as id, external_id, display_name, title, avatar_text
             from users where id = ($1)::uuid",
        )
        .bind(user_id)
        .fetch_one(&self.pool)
        .await
        .with_context(|| format!("load user profile for `{user_id}`"))?;

        Ok(UserProfile {
            id: row.try_get("id").context("read id")?,
            external_id: row.try_get("external_id").context("read external_id")?,
            display_name: row.try_get("display_name").context("read display_name")?,
            title: row.try_get("title").context("read title")?,
            avatar_text: row.try_get("avatar_text").context("read avatar_text")?,
        })
    }
}
