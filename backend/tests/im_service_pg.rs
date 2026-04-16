use anyhow::Result;
use gkim_im_backend::{im::service::ImService, social::SocialService};
use sqlx::{raw_sql, PgPool};
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};

const BOOTSTRAP_MIGRATION_SQL: &str =
    include_str!("../migrations/202604080001_bootstrap_im_schema.sql");
const AUTH_MIGRATION_SQL: &str =
    include_str!("../migrations/202604100001_auth_and_friend_requests.sql");
const ATTACHMENT_MIGRATION_SQL: &str =
    include_str!("../migrations/202604160001_direct_message_attachments.sql");

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
        let database_name = format!("gkim_im_test_{}", unique_suffix());

        sqlx::query(&format!("create database {database_name}"))
            .execute(&admin_pool)
            .await?;

        let database_url = database_url_for_db(&admin_url, &database_name);
        let pool = PgPool::connect(&database_url).await?;
        raw_sql(BOOTSTRAP_MIGRATION_SQL).execute(&pool).await?;
        raw_sql(AUTH_MIGRATION_SQL).execute(&pool).await?;
        raw_sql(ATTACHMENT_MIGRATION_SQL).execute(&pool).await?;

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

#[tokio::test]
async fn bootstrap_for_seed_user_rebuilds_contacts_conversations_and_unread_counts() -> Result<()> {
    let Some(database) = TestDatabase::new().await? else {
        eprintln!("skipping postgres bootstrap test because GKIM_TEST_DATABASE_URL is not set");
        return Ok(());
    };

    let nox_id = user_id(&database.pool, "nox-dev").await?;
    let leo_id = user_id(&database.pool, "leo-vance").await?;
    let aria_id = user_id(&database.pool, "aria-thorne").await?;

    let leo_conversation_id = insert_conversation(&database.pool, &nox_id, &leo_id).await?;
    insert_member(&database.pool, &leo_conversation_id, &nox_id, &leo_id, 2).await?;
    insert_member(&database.pool, &leo_conversation_id, &leo_id, &nox_id, 0).await?;

    let _leo_m1 = insert_message(
        &database.pool,
        &leo_conversation_id,
        &leo_id,
        "The orbital thread is stable.",
        "2026-04-06T13:40:00Z",
    )
    .await?;
    let _leo_m2 = insert_message(
        &database.pool,
        &leo_conversation_id,
        &nox_id,
        "Push the workshop update.",
        "2026-04-06T13:44:00Z",
    )
    .await?;
    let leo_m3 = insert_message(
        &database.pool,
        &leo_conversation_id,
        &leo_id,
        "Ready for review.",
        "2026-04-06T13:45:00Z",
    )
    .await?;
    insert_receipt(
        &database.pool,
        &leo_m3,
        &nox_id,
        Some("2026-04-06T13:45:01Z"),
        None,
    )
    .await?;

    let aria_conversation_id = insert_conversation(&database.pool, &nox_id, &aria_id).await?;
    insert_member(&database.pool, &aria_conversation_id, &nox_id, &aria_id, 0).await?;
    insert_member(&database.pool, &aria_conversation_id, &aria_id, &nox_id, 0).await?;
    insert_message(
        &database.pool,
        &aria_conversation_id,
        &aria_id,
        "Uploaded a portrait remix prompt set.",
        "2026-04-05T20:12:00Z",
    )
    .await?;

    let service = ImService::new(database.pool.clone());
    let bundle = service.bootstrap_for_user("nox-dev").await?;

    assert_eq!(bundle.user.external_id, "nox-dev");
    assert_eq!(bundle.contacts.len(), 3);
    assert_eq!(bundle.conversations.len(), 2);
    assert_eq!(bundle.contacts[0].external_id, "aria-thorne");
    assert_eq!(bundle.contacts[1].external_id, "clara-wu");
    assert_eq!(bundle.contacts[2].external_id, "leo-vance");

    let latest_conversation = &bundle.conversations[0];
    assert_eq!(latest_conversation.contact.external_id, "leo-vance");
    assert_eq!(latest_conversation.unread_count, 2);
    assert_eq!(
        latest_conversation
            .last_message
            .as_ref()
            .expect("latest message should exist")
            .body,
        "Ready for review."
    );
    assert_eq!(
        latest_conversation
            .last_message
            .as_ref()
            .expect("latest message should exist")
            .sender_external_id,
        "leo-vance"
    );

    database.cleanup().await
}

#[tokio::test]
async fn history_for_user_returns_paginated_message_history() -> Result<()> {
    let Some(database) = TestDatabase::new().await? else {
        eprintln!("skipping postgres history test because GKIM_TEST_DATABASE_URL is not set");
        return Ok(());
    };

    let nox_id = user_id(&database.pool, "nox-dev").await?;
    let leo_id = user_id(&database.pool, "leo-vance").await?;

    let conversation_id = insert_conversation(&database.pool, &nox_id, &leo_id).await?;
    insert_member(&database.pool, &conversation_id, &nox_id, &leo_id, 1).await?;
    insert_member(&database.pool, &conversation_id, &leo_id, &nox_id, 0).await?;

    let message_1 = insert_message(
        &database.pool,
        &conversation_id,
        &leo_id,
        "Message one",
        "2026-04-06T13:40:00Z",
    )
    .await?;
    let message_2 = insert_message(
        &database.pool,
        &conversation_id,
        &nox_id,
        "Message two",
        "2026-04-06T13:44:00Z",
    )
    .await?;
    let message_3 = insert_message(
        &database.pool,
        &conversation_id,
        &leo_id,
        "Message three",
        "2026-04-06T13:45:00Z",
    )
    .await?;
    insert_receipt(
        &database.pool,
        &message_3,
        &nox_id,
        Some("2026-04-06T13:45:01Z"),
        None,
    )
    .await?;

    let service = ImService::new(database.pool.clone());

    let latest_page = service
        .history_for_user("nox-dev", &conversation_id, 2, None)
        .await?;
    assert!(latest_page.has_more);
    assert_eq!(latest_page.messages.len(), 2);
    assert_eq!(latest_page.messages[0].id, message_2);
    assert_eq!(latest_page.messages[1].id, message_3);
    assert_eq!(latest_page.messages[1].sender_external_id, "leo-vance");
    assert_eq!(
        latest_page.messages[1].delivered_at.as_deref(),
        Some("2026-04-06T13:45:01Z")
    );

    let older_page = service
        .history_for_user(
            "nox-dev",
            &conversation_id,
            2,
            Some(latest_page.messages[0].id.as_str()),
        )
        .await?;
    assert!(!older_page.has_more);
    assert_eq!(older_page.messages.len(), 1);
    assert_eq!(older_page.messages[0].id, message_1);
    assert_eq!(older_page.messages[0].body, "Message one");

    database.cleanup().await
}

#[tokio::test]
async fn send_direct_message_requires_mutual_contacts_and_succeeds_after_friend_acceptance(
) -> Result<()> {
    let Some(database) = TestDatabase::new().await? else {
        eprintln!(
            "skipping contact-gating postgres test because GKIM_TEST_DATABASE_URL is not set"
        );
        return Ok(());
    };

    let service = ImService::new(database.pool.clone());
    let social_service = SocialService::new(database.pool.clone());
    let leo_id = user_id(&database.pool, "leo-vance").await?;
    let aria_id = user_id(&database.pool, "aria-thorne").await?;

    let send_error = service
        .send_direct_message(
            "leo-vance",
            "aria-thorne",
            Some("client-gated-before-accept"),
            "Need approval",
        )
        .await
        .expect_err("non-contacts should not be able to message");
    assert!(
        send_error
            .to_string()
            .contains("mutual contacts before messaging"),
        "unexpected error: {send_error}"
    );

    let request = social_service
        .send_friend_request(&leo_id, &aria_id)
        .await
        .expect("friend request should be created");
    let accepted = social_service
        .accept_friend_request(&request.id, &aria_id)
        .await
        .expect("friend request should be accepted");
    assert_eq!(accepted.status, "accepted");

    let sent = service
        .send_direct_message(
            "leo-vance",
            "aria-thorne",
            Some("client-gated-after-accept"),
            "Need approval",
        )
        .await
        .expect("mutual contacts should be able to message");
    assert_eq!(sent.recipient_external_id, "aria-thorne");
    assert_eq!(sent.recipient_unread_count, 1);
    assert_eq!(sent.message.body, "Need approval");

    let bundle = service.bootstrap_for_user("aria-thorne").await?;
    let conversation = bundle
        .conversations
        .iter()
        .find(|conversation| conversation.contact.external_id == "leo-vance")
        .expect("accepted contact should appear in aria bootstrap");
    assert_eq!(conversation.unread_count, 1);
    assert_eq!(
        conversation
            .last_message
            .as_ref()
            .expect("last message should exist")
            .body,
        "Need approval"
    );

    database.cleanup().await
}
