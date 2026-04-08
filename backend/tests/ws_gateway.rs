use anyhow::Result;
use futures_util::{SinkExt, StreamExt};
use gkim_im_backend::{app::build_router, config::AppConfig, session::SessionService};
use serde_json::Value;
use sqlx::{raw_sql, PgPool};
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};
use tokio::net::TcpListener;
use tokio_tungstenite::{
    connect_async,
    tungstenite::{client::IntoClientRequest, Message},
};

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
        let database_name = format!("gkim_ws_test_{}", unique_suffix());

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
        sqlx::query(
            "select pg_terminate_backend(pid)
             from pg_stat_activity
             where datname = $1
               and pid <> pg_backend_pid()",
        )
        .bind(&self.database_name)
        .execute(&self.admin_pool)
        .await?;
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

#[tokio::test]
async fn authenticated_websocket_registers_user_and_replies_to_ping() -> Result<()> {
    let Some(database) = TestDatabase::new().await? else {
        eprintln!("skipping websocket gateway test because GKIM_TEST_DATABASE_URL is not set");
        return Ok(());
    };

    let session_service = SessionService::new(database.pool.clone());
    let session = session_service
        .issue_dev_session("nox-dev")
        .await?
        .expect("seed user should exist");

    let listener = TcpListener::bind("127.0.0.1:0").await?;
    let addr = listener.local_addr()?;
    let app = build_router(
        AppConfig {
            service_name: "gkim-im-backend".to_string(),
            bind_addr: addr.to_string(),
            database_url: "postgres://example".to_string(),
            log_filter: "info".to_string(),
        },
        database.pool.clone(),
    );

    let server = tokio::spawn(async move {
        axum::serve(listener, app)
            .await
            .expect("websocket test server should run");
    });

    let mut request = format!("ws://{addr}/ws").into_client_request()?;
    request.headers_mut().insert(
        "authorization",
        format!("Bearer {}", session.token).parse()?,
    );

    let (mut socket, _) = connect_async(request).await?;

    let ready_frame = socket
        .next()
        .await
        .expect("ready frame should arrive")?
        .into_text()?;
    let ready_json: Value = serde_json::from_str(&ready_frame)?;
    assert_eq!(ready_json["type"], "session.registered");
    assert_eq!(ready_json["user"]["externalId"], "nox-dev");
    assert_eq!(ready_json["activeConnections"], 1);

    socket
        .send(Message::Text(r#"{"type":"ping"}"#.into()))
        .await?;
    let pong_frame = socket
        .next()
        .await
        .expect("pong frame should arrive")?
        .into_text()?;
    let pong_json: Value = serde_json::from_str(&pong_frame)?;
    assert_eq!(pong_json["type"], "pong");

    socket.close(None).await?;
    server.abort();
    let _ = server.await;
    database.cleanup().await
}
