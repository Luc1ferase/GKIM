use crate::im::{model::UserProfile, repository::ImRepository};
use anyhow::{Context, Result};
use hex::encode as hex_encode;
use serde::Serialize;
use sha2::{Digest, Sha256};
use sqlx::{PgPool, Row};

const RFC3339_UTC_SQL: &str = r#"YYYY-MM-DD"T"HH24:MI:SS"Z""#;

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionIssue {
    pub token: String,
    pub expires_at: String,
    pub user: UserProfile,
}

#[derive(Debug, Clone)]
pub struct SessionService {
    pool: PgPool,
    repository: ImRepository,
}

impl SessionService {
    pub fn new(pool: PgPool) -> Self {
        Self {
            repository: ImRepository::new(pool.clone()),
            pool,
        }
    }

    pub async fn issue_dev_session(&self, external_id: &str) -> Result<Option<SessionIssue>> {
        let Some(user) = self
            .repository
            .find_user_by_external_id(external_id)
            .await?
        else {
            return Ok(None);
        };

        let token: String = sqlx::query_scalar("select gen_random_uuid()::text")
            .fetch_one(&self.pool)
            .await
            .with_context(|| format!("generate dev session token for `{external_id}`"))?;
        let token_hash = hash_token(&token);

        let query = format!(
            "insert into session_tokens (user_id, token_hash, expires_at, created_for)
             values (($1)::uuid, $2, timezone('utc', now()) + interval '7 days', 'dev-session-http')
             returning to_char(expires_at at time zone 'utc', '{RFC3339_UTC_SQL}') as expires_at"
        );

        let row = sqlx::query(&query)
            .bind(&user.id)
            .bind(token_hash)
            .fetch_one(&self.pool)
            .await
            .with_context(|| format!("persist dev session token for `{external_id}`"))?;

        Ok(Some(SessionIssue {
            token,
            expires_at: row
                .try_get::<String, _>("expires_at")
                .context("read `expires_at` from session insert")?,
            user,
        }))
    }

    pub async fn authenticate(&self, token: &str) -> Result<Option<UserProfile>> {
        let token_hash = hash_token(token);
        let query = format!(
            "with touched as (
                update session_tokens
                set last_used_at = timezone('utc', now())
                where token_hash = $1
                  and revoked_at is null
                  and expires_at > timezone('utc', now())
                returning user_id
             )
             select
                u.id::text as id,
                u.external_id,
                u.display_name,
                u.title,
                u.avatar_text
             from touched
             join users u on u.id = touched.user_id"
        );

        let row = sqlx::query(&query)
            .bind(token_hash)
            .fetch_optional(&self.pool)
            .await
            .context("authenticate session token")?;

        row.map(|row| {
            Ok(UserProfile {
                id: row.try_get("id").context("read `id` from session auth")?,
                external_id: row
                    .try_get("external_id")
                    .context("read `external_id` from session auth")?,
                display_name: row
                    .try_get("display_name")
                    .context("read `display_name` from session auth")?,
                title: row
                    .try_get("title")
                    .context("read `title` from session auth")?,
                avatar_text: row
                    .try_get("avatar_text")
                    .context("read `avatar_text` from session auth")?,
            })
        })
        .transpose()
    }
}

fn hash_token(token: &str) -> String {
    let mut hasher = Sha256::new();
    hasher.update(token.as_bytes());
    hex_encode(hasher.finalize())
}
