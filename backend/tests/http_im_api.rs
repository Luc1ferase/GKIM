use anyhow::Result;
use axum::{
    body::Body,
    http::{Request, StatusCode},
};
use gkim_im_backend::{app::build_router, config::AppConfig};
use http_body_util::BodyExt;
use serde_json::{json, Value};
use sqlx::{raw_sql, PgPool};
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};
use tower::util::ServiceExt;

const BOOTSTRAP_MIGRATION_SQL: &str =
    include_str!("../migrations/202604080001_bootstrap_im_schema.sql");
const AUTH_MIGRATION_SQL: &str =
    include_str!("../migrations/202604100001_auth_and_friend_requests.sql");

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
        raw_sql(AUTH_MIGRATION_SQL).execute(&pool).await?;

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

async fn issue_dev_session_token(app: axum::Router, external_id: &str) -> Result<String> {
    let response = app
        .oneshot(
            Request::post("/api/session/dev")
                .header("content-type", "application/json")
                .body(Body::from(
                    json!({
                        "externalId": external_id
                    })
                    .to_string(),
                ))
                .unwrap(),
        )
        .await?;

    let body = response.into_body().collect().await?.to_bytes();
    let json: Value = serde_json::from_slice(&body)?;
    Ok(json["token"].as_str().unwrap_or_default().to_string())
}

async fn json_response(response: axum::response::Response) -> Result<(StatusCode, Value)> {
    let status = response.status();
    let body = response.into_body().collect().await?.to_bytes();
    Ok((status, serde_json::from_slice(&body)?))
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

    let token = issue_dev_session_token(app.clone(), "nox-dev").await?;
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

    let token = issue_dev_session_token(app.clone(), "nox-dev").await?;

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

#[tokio::test]
async fn auth_and_friend_request_endpoints_round_trip() -> Result<()> {
    let Some(database) = TestDatabase::new().await? else {
        eprintln!("skipping auth/social HTTP test because GKIM_TEST_DATABASE_URL is not set");
        return Ok(());
    };

    let app = build_router(
        AppConfig {
            service_name: "gkim-im-backend".to_string(),
            bind_addr: "127.0.0.1:8080".to_string(),
            database_url: "postgres://example".to_string(),
            log_filter: "info".to_string(),
        },
        database.pool.clone(),
    );

    let (register_status, register_json) = json_response(
        app.clone()
            .oneshot(
                Request::post("/api/auth/register")
                    .header("content-type", "application/json")
                    .body(Body::from(
                        json!({
                            "username": "nova_user",
                            "password": "passw0rd!",
                            "displayName": "Nova User"
                        })
                        .to_string(),
                    ))
                    .unwrap(),
            )
            .await?,
    )
    .await?;
    assert_eq!(register_status, StatusCode::OK);
    let register_token = register_json["token"]
        .as_str()
        .expect("register token should exist")
        .to_string();
    assert_eq!(register_json["user"]["externalId"], "nova_user");

    let (duplicate_status, duplicate_json) = json_response(
        app.clone()
            .oneshot(
                Request::post("/api/auth/register")
                    .header("content-type", "application/json")
                    .body(Body::from(
                        json!({
                            "username": "Nova_User",
                            "password": "passw0rd!",
                            "displayName": "Duplicate"
                        })
                        .to_string(),
                    ))
                    .unwrap(),
            )
            .await?,
    )
    .await?;
    assert_eq!(duplicate_status, StatusCode::CONFLICT);
    assert_eq!(duplicate_json["error"], "username_taken");

    let (invalid_login_status, invalid_login_json) = json_response(
        app.clone()
            .oneshot(
                Request::post("/api/auth/login")
                    .header("content-type", "application/json")
                    .body(Body::from(
                        json!({
                            "username": "nova_user",
                            "password": "wrong-pass"
                        })
                        .to_string(),
                    ))
                    .unwrap(),
            )
            .await?,
    )
    .await?;
    assert_eq!(invalid_login_status, StatusCode::UNAUTHORIZED);
    assert_eq!(invalid_login_json["error"], "unauthorized");

    let (login_status, login_json) = json_response(
        app.clone()
            .oneshot(
                Request::post("/api/auth/login")
                    .header("content-type", "application/json")
                    .body(Body::from(
                        json!({
                            "username": "nova_user",
                            "password": "passw0rd!"
                        })
                        .to_string(),
                    ))
                    .unwrap(),
            )
            .await?,
    )
    .await?;
    assert_eq!(login_status, StatusCode::OK);
    let login_token = login_json["token"]
        .as_str()
        .expect("login token should exist")
        .to_string();
    assert_eq!(login_json["user"]["externalId"], "nova_user");

    let (initial_search_status, initial_search_json) = json_response(
        app.clone()
            .oneshot(
                Request::get("/api/users/search?q=leo")
                    .header("authorization", format!("Bearer {register_token}"))
                    .body(Body::empty())
                    .unwrap(),
            )
            .await?,
    )
    .await?;
    assert_eq!(initial_search_status, StatusCode::OK);
    let leo_result = initial_search_json
        .as_array()
        .and_then(|items| items.first())
        .expect("search should return leo");
    let leo_user_id = leo_result["id"]
        .as_str()
        .expect("leo user id should exist")
        .to_string();
    assert_eq!(leo_result["username"], "leo-vance");
    assert_eq!(leo_result["contactStatus"], "none");

    let (send_request_status, send_request_json) = json_response(
        app.clone()
            .oneshot(
                Request::post("/api/friends/request")
                    .header("authorization", format!("Bearer {login_token}"))
                    .header("content-type", "application/json")
                    .body(Body::from(
                        json!({
                            "toUserId": leo_user_id
                        })
                        .to_string(),
                    ))
                    .unwrap(),
            )
            .await?,
    )
    .await?;
    assert_eq!(send_request_status, StatusCode::OK);
    let request_id = send_request_json["id"]
        .as_str()
        .expect("request id should exist")
        .to_string();
    assert_eq!(send_request_json["status"], "pending");
    assert_eq!(send_request_json["toUserExternalId"], "leo-vance");

    let (pending_sent_status, pending_sent_json) = json_response(
        app.clone()
            .oneshot(
                Request::get("/api/users/search?q=leo")
                    .header("authorization", format!("Bearer {login_token}"))
                    .body(Body::empty())
                    .unwrap(),
            )
            .await?,
    )
    .await?;
    assert_eq!(pending_sent_status, StatusCode::OK);
    assert_eq!(pending_sent_json[0]["contactStatus"], "pending_sent");

    let leo_token = issue_dev_session_token(app.clone(), "leo-vance").await?;

    let (pending_received_status, pending_received_json) = json_response(
        app.clone()
            .oneshot(
                Request::get("/api/users/search?q=nova")
                    .header("authorization", format!("Bearer {leo_token}"))
                    .body(Body::empty())
                    .unwrap(),
            )
            .await?,
    )
    .await?;
    assert_eq!(pending_received_status, StatusCode::OK);
    assert_eq!(
        pending_received_json[0]["contactStatus"],
        "pending_received"
    );

    let (list_status, list_json) = json_response(
        app.clone()
            .oneshot(
                Request::get("/api/friends/requests")
                    .header("authorization", format!("Bearer {leo_token}"))
                    .body(Body::empty())
                    .unwrap(),
            )
            .await?,
    )
    .await?;
    assert_eq!(list_status, StatusCode::OK);
    assert_eq!(list_json.as_array().map(|items| items.len()), Some(1));
    assert_eq!(list_json[0]["fromUser"]["externalId"], "nova_user");

    let (accept_status, accept_json) = json_response(
        app.clone()
            .oneshot(
                Request::post(format!("/api/friends/requests/{request_id}/accept"))
                    .header("authorization", format!("Bearer {leo_token}"))
                    .body(Body::empty())
                    .unwrap(),
            )
            .await?,
    )
    .await?;
    assert_eq!(accept_status, StatusCode::OK);
    assert_eq!(accept_json["status"], "accepted");

    let (contact_status, contact_json) = json_response(
        app.clone()
            .oneshot(
                Request::get("/api/users/search?q=leo")
                    .header("authorization", format!("Bearer {login_token}"))
                    .body(Body::empty())
                    .unwrap(),
            )
            .await?,
    )
    .await?;
    assert_eq!(contact_status, StatusCode::OK);
    assert_eq!(contact_json[0]["contactStatus"], "contact");

    database.cleanup().await
}

#[tokio::test]
async fn rejecting_friend_request_restores_non_contact_search_state() -> Result<()> {
    let Some(database) = TestDatabase::new().await? else {
        eprintln!(
            "skipping friend-request rejection HTTP test because GKIM_TEST_DATABASE_URL is not set"
        );
        return Ok(());
    };

    let app = build_router(
        AppConfig {
            service_name: "gkim-im-backend".to_string(),
            bind_addr: "127.0.0.1:8080".to_string(),
            database_url: "postgres://example".to_string(),
            log_filter: "info".to_string(),
        },
        database.pool.clone(),
    );

    let (register_status, register_json) = json_response(
        app.clone()
            .oneshot(
                Request::post("/api/auth/register")
                    .header("content-type", "application/json")
                    .body(Body::from(
                        json!({
                            "username": "orbit_user",
                            "password": "passw0rd!",
                            "displayName": "Orbit User"
                        })
                        .to_string(),
                    ))
                    .unwrap(),
            )
            .await?,
    )
    .await?;
    assert_eq!(register_status, StatusCode::OK);
    let orbit_token = register_json["token"]
        .as_str()
        .expect("register token should exist")
        .to_string();

    let (search_status, search_json) = json_response(
        app.clone()
            .oneshot(
                Request::get("/api/users/search?q=clara")
                    .header("authorization", format!("Bearer {orbit_token}"))
                    .body(Body::empty())
                    .unwrap(),
            )
            .await?,
    )
    .await?;
    assert_eq!(search_status, StatusCode::OK);
    let clara_user_id = search_json[0]["id"]
        .as_str()
        .expect("clara user id should exist")
        .to_string();

    let (send_status, send_json) = json_response(
        app.clone()
            .oneshot(
                Request::post("/api/friends/request")
                    .header("authorization", format!("Bearer {orbit_token}"))
                    .header("content-type", "application/json")
                    .body(Body::from(
                        json!({
                            "toUserId": clara_user_id
                        })
                        .to_string(),
                    ))
                    .unwrap(),
            )
            .await?,
    )
    .await?;
    assert_eq!(send_status, StatusCode::OK);
    let request_id = send_json["id"]
        .as_str()
        .expect("request id should exist")
        .to_string();

    let clara_token = app
        .clone()
        .oneshot(
            Request::post("/api/session/dev")
                .header("content-type", "application/json")
                .body(Body::from(r#"{"externalId":"clara-wu"}"#))
                .unwrap(),
        )
        .await?;
    let (_, clara_session_json) = json_response(clara_token).await?;
    let clara_bearer = clara_session_json["token"]
        .as_str()
        .expect("clara token should exist");

    let (reject_status, reject_json) = json_response(
        app.clone()
            .oneshot(
                Request::post(format!("/api/friends/requests/{request_id}/reject"))
                    .header("authorization", format!("Bearer {clara_bearer}"))
                    .body(Body::empty())
                    .unwrap(),
            )
            .await?,
    )
    .await?;
    assert_eq!(reject_status, StatusCode::OK);
    assert_eq!(reject_json["status"], "rejected");

    let (post_reject_status, post_reject_json) = json_response(
        app.clone()
            .oneshot(
                Request::get("/api/users/search?q=clara")
                    .header("authorization", format!("Bearer {orbit_token}"))
                    .body(Body::empty())
                    .unwrap(),
            )
            .await?,
    )
    .await?;
    assert_eq!(post_reject_status, StatusCode::OK);
    assert_eq!(post_reject_json[0]["contactStatus"], "none");

    let (pending_status, pending_json) = json_response(
        app.oneshot(
            Request::get("/api/friends/requests")
                .header("authorization", format!("Bearer {clara_bearer}"))
                .body(Body::empty())
                .unwrap(),
        )
        .await?,
    )
    .await?;
    assert_eq!(pending_status, StatusCode::OK);
    assert_eq!(pending_json.as_array().map(|items| items.len()), Some(0));

    database.cleanup().await
}
