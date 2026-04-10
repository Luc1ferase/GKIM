use crate::im::model::UserProfile;
use anyhow::{anyhow, Context, Result};
use argon2::{
    password_hash::{rand_core::OsRng, PasswordHash, PasswordHasher, PasswordVerifier, SaltString},
    Argon2,
};
use hex::encode as hex_encode;
use sha2::{Digest, Sha256};
use sqlx::{PgPool, Row};

const RFC3339_UTC_SQL: &str = r#"YYYY-MM-DD"T"HH24:MI:SS"Z""#;

#[derive(Debug, Clone)]
pub struct AuthService {
    pool: PgPool,
}

#[derive(Debug)]
pub enum RegisterError {
    UsernameTaken,
    Validation(String),
    Internal(anyhow::Error),
}

#[derive(Debug)]
pub enum LoginError {
    InvalidCredentials,
    Internal(anyhow::Error),
}

#[derive(Debug, Clone, serde::Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthResponse {
    pub token: String,
    pub expires_at: String,
    pub user: UserProfile,
}

impl AuthService {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }

    pub async fn register(
        &self,
        username: &str,
        password: &str,
        display_name: &str,
    ) -> Result<AuthResponse, RegisterError> {
        // Validate input
        let username = username.trim();
        let display_name = display_name.trim();

        if username.len() < 3 || username.len() > 20 {
            return Err(RegisterError::Validation(
                "username must be between 3 and 20 characters".to_string(),
            ));
        }
        if !username
            .chars()
            .all(|c| c.is_ascii_alphanumeric() || c == '_')
        {
            return Err(RegisterError::Validation(
                "username must contain only letters, digits, and underscores".to_string(),
            ));
        }
        if password.len() < 8 {
            return Err(RegisterError::Validation(
                "password must be at least 8 characters".to_string(),
            ));
        }
        if display_name.is_empty() || display_name.len() > 30 {
            return Err(RegisterError::Validation(
                "displayName must be between 1 and 30 characters".to_string(),
            ));
        }

        let password_hash = hash_password(password).map_err(RegisterError::Internal)?;

        // Derive avatar_text from first two chars of display_name uppercased
        let avatar_text: String = display_name
            .chars()
            .take(2)
            .collect::<String>()
            .to_uppercase();

        // Insert user — unique constraint on lower(username) will catch duplicates
        let insert_result = sqlx::query(
            "insert into users (external_id, username, password_hash, display_name, title, avatar_text)
             values ($1, $2, $3, $4, '', $5)
             returning id::text as id",
        )
        .bind(username) // external_id = username for registered users
        .bind(username)
        .bind(&password_hash)
        .bind(display_name)
        .bind(&avatar_text)
        .fetch_one(&self.pool)
        .await;

        let row = match insert_result {
            Ok(row) => row,
            Err(error) => {
                let error_str = error.to_string();
                if error_str.contains("users_username_lower_idx") || error_str.contains("unique") {
                    return Err(RegisterError::UsernameTaken);
                }
                return Err(RegisterError::Internal(
                    anyhow::Error::from(error).context("insert new user"),
                ));
            }
        };

        let user_id: String = row
            .try_get("id")
            .map_err(|e| RegisterError::Internal(anyhow::Error::from(e)))?;

        let user = UserProfile {
            id: user_id.clone(),
            external_id: username.to_string(),
            display_name: display_name.to_string(),
            title: String::new(),
            avatar_text,
        };

        let auth_response = self
            .issue_session_for_user(&user_id, &user, "registration")
            .await
            .map_err(RegisterError::Internal)?;

        Ok(auth_response)
    }

    pub async fn login(&self, username: &str, password: &str) -> Result<AuthResponse, LoginError> {
        let username = username.trim();

        // Look up user by username (case-insensitive)
        let row = sqlx::query(
            "select
                id::text as id,
                external_id,
                username,
                password_hash,
                display_name,
                title,
                avatar_text
             from users
             where lower(username) = lower($1)",
        )
        .bind(username)
        .fetch_optional(&self.pool)
        .await
        .map_err(|e| LoginError::Internal(anyhow::Error::from(e).context("look up user")))?;

        let row = row.ok_or(LoginError::InvalidCredentials)?;

        let stored_hash: Option<String> = row
            .try_get("password_hash")
            .map_err(|e| LoginError::Internal(anyhow::Error::from(e)))?;

        // Dev users have null password_hash — they can't use credential login
        let stored_hash = stored_hash.ok_or(LoginError::InvalidCredentials)?;

        // Verify password
        if !verify_password(password, &stored_hash) {
            return Err(LoginError::InvalidCredentials);
        }

        let user_id: String = row
            .try_get("id")
            .map_err(|e| LoginError::Internal(anyhow::Error::from(e)))?;

        let user = UserProfile {
            id: user_id.clone(),
            external_id: row
                .try_get("external_id")
                .map_err(|e| LoginError::Internal(anyhow::Error::from(e)))?,
            display_name: row
                .try_get("display_name")
                .map_err(|e| LoginError::Internal(anyhow::Error::from(e)))?,
            title: row
                .try_get("title")
                .map_err(|e| LoginError::Internal(anyhow::Error::from(e)))?,
            avatar_text: row
                .try_get("avatar_text")
                .map_err(|e| LoginError::Internal(anyhow::Error::from(e)))?,
        };

        let auth_response = self
            .issue_session_for_user(&user_id, &user, "credential-login")
            .await
            .map_err(LoginError::Internal)?;

        Ok(auth_response)
    }

    async fn issue_session_for_user(
        &self,
        user_id: &str,
        user: &UserProfile,
        created_for: &str,
    ) -> Result<AuthResponse> {
        let token: String = sqlx::query_scalar("select gen_random_uuid()::text")
            .fetch_one(&self.pool)
            .await
            .context("generate session token")?;

        let token_hash = hash_token(&token);

        let query = format!(
            "insert into session_tokens (user_id, token_hash, expires_at, created_for)
             values (($1)::uuid, $2, timezone('utc', now()) + interval '7 days', $3)
             returning to_char(expires_at at time zone 'utc', '{RFC3339_UTC_SQL}') as expires_at"
        );

        let row = sqlx::query(&query)
            .bind(user_id)
            .bind(token_hash)
            .bind(created_for)
            .fetch_one(&self.pool)
            .await
            .context("persist session token")?;

        Ok(AuthResponse {
            token,
            expires_at: row
                .try_get::<String, _>("expires_at")
                .context("read expires_at")?,
            user: user.clone(),
        })
    }
}

fn hash_password(password: &str) -> Result<String> {
    let salt = SaltString::generate(&mut OsRng);
    let argon2 = Argon2::default();
    let hash = argon2
        .hash_password(password.as_bytes(), &salt)
        .map_err(|e| anyhow!("hash password: {e}"))?;
    Ok(hash.to_string())
}

fn verify_password(password: &str, hash: &str) -> bool {
    let Ok(parsed_hash) = PasswordHash::new(hash) else {
        return false;
    };
    Argon2::default()
        .verify_password(password.as_bytes(), &parsed_hash)
        .is_ok()
}

fn hash_token(token: &str) -> String {
    let mut hasher = Sha256::new();
    hasher.update(token.as_bytes());
    hex_encode(hasher.finalize())
}
