use anyhow::Result;
use axum::{body::Body, http::Request};
use gkim_im_backend::{app::build_router, config::AppConfig};
use http_body_util::BodyExt;
use serde_json::Value;
use sqlx::{raw_sql, PgPool};
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};
use tower::util::ServiceExt;

const BOOTSTRAP_MIGRATION_SQL: &str =
    include_str!("../migrations/202604080001_bootstrap_im_schema.sql");

struct TestDatabase {
    admin_pool: PgPool,
    pool: PgPool,
    database_name: String,
}

impl TestDatabase {
    async fn new() -> Result<Option<Self>> {
        let Ok(admin_url) = std::env::var("GKIM_TEST_DATABASE_URL") else {
            return Ok(None);
        };

        let admin_pool = PgPool::connect(&admin_url).await?;
        let database_name = format!("gkim_http_test_{}", unique_suffix());

        sqlx::query(&format!("create database {database_name}"))
            .execute(&admin_pool)
            .await?;

        let database_url = database_url_for_db(&admin_url, &database_name);
        let pool = PgPool::connect(&database_url).await?;
        raw_sql(BOOTSTRAP_MIGRATION_SQL).execute(&pool).await?;

        Ok(Some(Self {
            admin_pool,
            pool,
            database_name,
        }))
    }

    async fn cleanup(self) -> Result<()> {
        self.pool.close().await;
        sqlx::query(&format!("drop database if exists {}", self.database_name))
            .execute(&self.admin_pool)
            .await?;
        self.admin_pool.close().await;
        Ok(())
    }
}

fn database_url_for_db(admin_url: &str, database_name: &str) -> String {
    let (base, query) = admin_url.split_once('?').unwrap_or((admin_url, ""));
    let prefix = base
        .rsplit_once('/')
        .map(|(value, _)| value)
        .unwrap_or(base);

    if query.is_empty() {
        format!("{prefix}/{database_name}")
    } else {
        format!("{prefix}/{database_name}?{query}")
    }
}

fn unique_suffix() -> u128 {
    static COUNTER: AtomicU64 = AtomicU64::new(0);

    let timestamp = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .expect("clock should be after epoch")
        .as_millis();
    let counter = COUNTER.fetch_add(1, Ordering::Relaxed) as u128;

    timestamp * 1000 + counter
}

async fn user_id(pool: &PgPool, external_id: &str) -> Result<String> {
    Ok(
        sqlx::query_scalar("select id::text from users where external_id = $1")
            .bind(external_id)
            .fetch_one(pool)
            .await?,
    )
}

async fn insert_conversation(pool: &PgPool, user_a_id: &str, user_b_id: &str) -> Result<String> {
    Ok(sqlx::query_scalar(
        "insert into direct_conversations (participant_a_user_id, participant_b_user_id)
         values (($1)::uuid, ($2)::uuid)
         returning id::text",
    )
    .bind(user_a_id)
    .bind(user_b_id)
    .fetch_one(pool)
    .await?)
}

async fn insert_member(
    pool: &PgPool,
    conversation_id: &str,
    user_id: &str,
    peer_user_id: &str,
    unread_count: i32,
) -> Result<()> {
    sqlx::query(
        "insert into conversation_members (conversation_id, user_id, peer_user_id, unread_count)
         values (($1)::uuid, ($2)::uuid, ($3)::uuid, $4)",
    )
    .bind(conversation_id)
    .bind(user_id)
    .bind(peer_user_id)
    .bind(unread_count)
    .execute(pool)
    .await?;

    Ok(())
}

async fn insert_message(
    pool: &PgPool,
    conversation_id: &str,
    sender_user_id: &str,
    body: &str,
    created_at: &str,
) -> Result<String> {
    Ok(sqlx::query_scalar(
        "insert into messages (conversation_id, sender_user_id, body, created_at)
         values (($1)::uuid, ($2)::uuid, $3, ($4)::timestamptz)
         returning id::text",
    )
    .bind(conversation_id)
    .bind(sender_user_id)
    .bind(body)
    .bind(created_at)
    .fetch_one(pool)
    .await?)
}

async fn insert_receipt(
    pool: &PgPool,
    message_id: &str,
    user_id: &str,
    delivered_at: Option<&str>,
    read_at: Option<&str>,
) -> Result<()> {
    sqlx::query(
        "insert into message_receipts (message_id, user_id, delivered_at, read_at)
         values (($1)::uuid, ($2)::uuid, ($3)::timestamptz, ($4)::timestamptz)",
    )
    .bind(message_id)
    .bind(user_id)
    .bind(delivered_at)
    .bind(read_at)
    .execute(pool)
    .await?;

    Ok(())
}

async fn seed_bootstrap_data(pool: &PgPool) -> Result<String> {
    let nox_id = user_id(pool, "nox-dev").await?;
    let leo_id = user_id(pool, "leo-vance").await?;

    let conversation_id = insert_conversation(pool, &nox_id, &leo_id).await?;
    insert_member(pool, &conversation_id, &nox_id, &leo_id, 2).await?;
    insert_member(pool, &conversation_id, &leo_id, &nox_id, 0).await?;

    insert_message(
        pool,
        &conversation_id,
        &leo_id,
        "The orbital thread is stable.",
        "2026-04-06T13:40:00Z",
    )
    .await?;
    let latest_message_id = insert_message(
        pool,
        &conversation_id,
        &leo_id,
        "Ready for review.",
        "2026-04-06T13:45:00Z",
    )
    .await?;
    insert_receipt(
        pool,
        &latest_message_id,
        &nox_id,
        Some("2026-04-06T13:45:01Z"),
        None,
    )
    .await?;

    Ok(conversation_id)
}

async fn issue_dev_session_token(app: axum::Router) -> Result<String> {
    let response = app
        .oneshot(
            Request::post("/api/session/dev")
                .header("content-type", "application/json")
                .body(Body::from(r#"{"externalId":"nox-dev"}"#))
                .unwrap(),
        )
        .await?;

    let body = response.into_body().collect().await?.to_bytes();
    let json: Value = serde_json::from_slice(&body)?;
    Ok(json["token"].as_str().unwrap_or_default().to_string())
}

#[tokio::test]
async fn session_issue_and_bootstrap_endpoints_round_trip() -> Result<()> {
    let Some(database) = TestDatabase::new().await? else {
        eprintln!("skipping HTTP bootstrap test because GKIM_TEST_DATABASE_URL is not set");
        return Ok(());
    };

    seed_bootstrap_data(&database.pool).await?;

    let app = build_router(
        AppConfig {
            service_name: "gkim-im-backend".to_string(),
            bind_addr: "127.0.0.1:8080".to_string(),
            database_url: "postgres://example".to_string(),
            log_filter: "info".to_string(),
        },
        database.pool.clone(),
    );

    let token = issue_dev_session_token(app.clone()).await?;
    assert!(!token.is_empty());

    let response = app
        .oneshot(
            Request::get("/api/bootstrap")
                .header("authorization", format!("Bearer {token}"))
                .body(Body::empty())
                .unwrap(),
        )
        .await?;

    let body = response.into_body().collect().await?.to_bytes();
    let json: Value = serde_json::from_slice(&body)?;

    assert_eq!(json["user"]["externalId"], "nox-dev");
    assert_eq!(
        json["contacts"].as_array().map(|value| value.len()),
        Some(3)
    );
    assert_eq!(
        json["conversations"].as_array().map(|value| value.len()),
        Some(1)
    );
    assert_eq!(
        json["conversations"][0]["contact"]["externalId"],
        "leo-vance"
    );
    assert_eq!(json["conversations"][0]["unreadCount"], 2);

    database.cleanup().await
}

#[tokio::test]
async fn history_endpoint_requires_auth_and_supports_pagination() -> Result<()> {
    let Some(database) = TestDatabase::new().await? else {
        eprintln!("skipping HTTP history test because GKIM_TEST_DATABASE_URL is not set");
        return Ok(());
    };

    let conversation_id = seed_bootstrap_data(&database.pool).await?;
    let nox_id = user_id(&database.pool, "nox-dev").await?;

    let older_message_id = insert_message(
        &database.pool,
        &conversation_id,
        &nox_id,
        "Follow-up from Nox",
        "2026-04-06T13:46:00Z",
    )
    .await?;

    let app = build_router(
        AppConfig {
            service_name: "gkim-im-backend".to_string(),
            bind_addr: "127.0.0.1:8080".to_string(),
            database_url: "postgres://example".to_string(),
            log_filter: "info".to_string(),
        },
        database.pool.clone(),
    );

    let unauthorized = app
        .clone()
        .oneshot(
            Request::get(format!(
                "/api/conversations/{conversation_id}/messages?limit=2"
            ))
            .body(Body::empty())
            .unwrap(),
        )
        .await?;
    assert_eq!(unauthorized.status(), axum::http::StatusCode::UNAUTHORIZED);

    let token = issue_dev_session_token(app.clone()).await?;

    let response = app
        .clone()
        .oneshot(
            Request::get(format!(
                "/api/conversations/{conversation_id}/messages?limit=2"
            ))
            .header("authorization", format!("Bearer {token}"))
            .body(Body::empty())
            .unwrap(),
        )
        .await?;

    let body = response.into_body().collect().await?.to_bytes();
    let json: Value = serde_json::from_slice(&body)?;

    assert_eq!(json["conversationId"], conversation_id);
    assert_eq!(json["hasMore"], true);
    assert_eq!(
        json["messages"].as_array().map(|value| value.len()),
        Some(2)
    );
    assert_eq!(json["messages"][1]["body"], "Follow-up from Nox");

    let older_page = app
        .oneshot(
            Request::get(format!(
                "/api/conversations/{conversation_id}/messages?limit=2&before={older_message_id}"
            ))
            .header("authorization", format!("Bearer {token}"))
            .body(Body::empty())
            .unwrap(),
        )
        .await?;

    let older_body = older_page.into_body().collect().await?.to_bytes();
    let older_json: Value = serde_json::from_slice(&older_body)?;
    assert_eq!(older_json["hasMore"], false);
    assert_eq!(
        older_json["messages"].as_array().map(|value| value.len()),
        Some(2)
    );
    assert_eq!(
        older_json["messages"][0]["body"],
        "The orbital thread is stable."
    );

    database.cleanup().await
}
