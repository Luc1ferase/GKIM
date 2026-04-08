use anyhow::Result;
use futures_util::{SinkExt, StreamExt};
use gkim_im_backend::{
    app::build_router, config::AppConfig, im::service::ImService, session::SessionService,
};
use serde_json::{json, Value};
use sqlx::{raw_sql, PgPool};
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};
use tokio::net::TcpListener;
use tokio::time::{timeout, Duration};
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

type TestSocket =
    tokio_tungstenite::WebSocketStream<tokio_tungstenite::MaybeTlsStream<tokio::net::TcpStream>>;

async fn next_json_event(socket: &mut TestSocket) -> Result<Value> {
    let frame = timeout(Duration::from_secs(3), socket.next())
        .await
        .expect("websocket frame should arrive in time")
        .expect("websocket stream should stay open")?;
    let text = frame.into_text()?;
    Ok(serde_json::from_str(&text)?)
}

async fn wait_for_event_type(socket: &mut TestSocket, event_type: &str) -> Result<Value> {
    loop {
        let event = next_json_event(socket).await?;
        if event["type"] == event_type {
            return Ok(event);
        }
    }
}

async fn connect_authenticated_socket(
    addr: std::net::SocketAddr,
    token: &str,
) -> Result<TestSocket> {
    let mut request = format!("ws://{addr}/ws").into_client_request()?;
    request
        .headers_mut()
        .insert("authorization", format!("Bearer {token}").parse()?);

    let (socket, _) = connect_async(request).await?;
    Ok(socket)
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

    let mut socket = connect_authenticated_socket(addr, &session.token).await?;

    let ready_json = next_json_event(&mut socket).await?;
    assert_eq!(ready_json["type"], "session.registered");
    assert_eq!(ready_json["user"]["externalId"], "nox-dev");
    assert_eq!(ready_json["activeConnections"], 1);

    socket
        .send(Message::Text(r#"{"type":"ping"}"#.into()))
        .await?;
    let pong_json = next_json_event(&mut socket).await?;
    assert_eq!(pong_json["type"], "pong");

    socket.close(None).await?;
    server.abort();
    let _ = server.await;
    database.cleanup().await
}

#[tokio::test]
async fn online_send_flow_persists_message_and_emits_delivery_and_read_events() -> Result<()> {
    let Some(database) = TestDatabase::new().await? else {
        eprintln!("skipping websocket send-flow test because GKIM_TEST_DATABASE_URL is not set");
        return Ok(());
    };

    let session_service = SessionService::new(database.pool.clone());
    let sender_session = session_service
        .issue_dev_session("nox-dev")
        .await?
        .expect("sender session should be created");
    let recipient_session = session_service
        .issue_dev_session("leo-vance")
        .await?
        .expect("recipient session should be created");

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

    let mut sender_socket = connect_authenticated_socket(addr, &sender_session.token).await?;
    let mut recipient_socket = connect_authenticated_socket(addr, &recipient_session.token).await?;

    let _ = next_json_event(&mut sender_socket).await?;
    let _ = next_json_event(&mut recipient_socket).await?;

    sender_socket
        .send(Message::Text(
            json!({
                "type": "message.send",
                "recipientExternalId": "leo-vance",
                "clientMessageId": "client-online-1",
                "body": "Hello Leo"
            })
            .to_string()
            .into(),
        ))
        .await?;

    let sender_sent = wait_for_event_type(&mut sender_socket, "message.sent").await?;
    let conversation_id = sender_sent["conversationId"]
        .as_str()
        .expect("conversation id should be present")
        .to_string();
    let message_id = sender_sent["message"]["id"]
        .as_str()
        .expect("message id should be present")
        .to_string();
    assert_eq!(sender_sent["message"]["body"], "Hello Leo");

    let recipient_received = wait_for_event_type(&mut recipient_socket, "message.received").await?;
    assert_eq!(recipient_received["conversationId"], conversation_id);
    assert_eq!(recipient_received["message"]["id"], message_id);
    assert_eq!(recipient_received["message"]["body"], "Hello Leo");
    assert_eq!(recipient_received["unreadCount"], 1);

    let sender_delivered = wait_for_event_type(&mut sender_socket, "message.delivered").await?;
    assert_eq!(sender_delivered["conversationId"], conversation_id);
    assert_eq!(sender_delivered["messageId"], message_id);
    assert_eq!(sender_delivered["recipientExternalId"], "leo-vance");

    recipient_socket
        .send(Message::Text(
            json!({
                "type": "message.read",
                "conversationId": conversation_id,
                "messageId": message_id
            })
            .to_string()
            .into(),
        ))
        .await?;

    let sender_read = wait_for_event_type(&mut sender_socket, "message.read").await?;
    assert_eq!(sender_read["messageId"], message_id);
    assert_eq!(sender_read["readerExternalId"], "leo-vance");

    let history = ImService::new(database.pool.clone())
        .history_for_user("nox-dev", &conversation_id, 20, None)
        .await?;
    let last_message = history
        .messages
        .last()
        .expect("history should contain sent message");
    assert_eq!(last_message.body, "Hello Leo");
    assert!(last_message.delivered_at.is_some());
    assert!(last_message.read_at.is_some());

    sender_socket.close(None).await?;
    recipient_socket.close(None).await?;
    server.abort();
    let _ = server.await;
    database.cleanup().await
}

#[tokio::test]
async fn offline_recipient_recovers_unread_state_from_bootstrap_and_history() -> Result<()> {
    let Some(database) = TestDatabase::new().await? else {
        eprintln!(
            "skipping websocket offline-recovery test because GKIM_TEST_DATABASE_URL is not set"
        );
        return Ok(());
    };

    let session_service = SessionService::new(database.pool.clone());
    let sender_session = session_service
        .issue_dev_session("nox-dev")
        .await?
        .expect("sender session should be created");

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

    let mut sender_socket = connect_authenticated_socket(addr, &sender_session.token).await?;
    let _ = next_json_event(&mut sender_socket).await?;

    sender_socket
        .send(Message::Text(
            json!({
                "type": "message.send",
                "recipientExternalId": "clara-wu",
                "clientMessageId": "client-offline-1",
                "body": "Offline hello"
            })
            .to_string()
            .into(),
        ))
        .await?;

    let sender_sent = wait_for_event_type(&mut sender_socket, "message.sent").await?;
    let conversation_id = sender_sent["conversationId"]
        .as_str()
        .expect("conversation id should be present")
        .to_string();

    let service = ImService::new(database.pool.clone());
    let bootstrap = service.bootstrap_for_user("clara-wu").await?;
    assert_eq!(bootstrap.conversations.len(), 1);
    assert_eq!(bootstrap.conversations[0].contact.external_id, "nox-dev");
    assert_eq!(bootstrap.conversations[0].unread_count, 1);
    assert_eq!(
        bootstrap.conversations[0]
            .last_message
            .as_ref()
            .expect("latest message should exist")
            .body,
        "Offline hello"
    );

    let history = service
        .history_for_user("clara-wu", &conversation_id, 20, None)
        .await?;
    let last_message = history
        .messages
        .last()
        .expect("history should contain sent message");
    assert_eq!(last_message.body, "Offline hello");
    assert!(last_message.delivered_at.is_none());
    assert!(last_message.read_at.is_none());

    sender_socket.close(None).await?;
    server.abort();
    let _ = server.await;
    database.cleanup().await
}
