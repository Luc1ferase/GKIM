use std::path::PathBuf;

fn bootstrap_migration_sql() -> String {
    let migration_path = PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("migrations")
        .join("202604080001_bootstrap_im_schema.sql");

    std::fs::read_to_string(&migration_path)
        .unwrap_or_else(|error| panic!("failed to read {}: {error}", migration_path.display()))
        .to_lowercase()
}

#[test]
fn bootstrap_migration_defines_core_im_tables() {
    let sql = bootstrap_migration_sql();

    for snippet in [
        "create extension if not exists pgcrypto",
        "create table users",
        "create table contacts",
        "create table direct_conversations",
        "create table conversation_members",
        "create table messages",
        "create table message_receipts",
        "create table session_tokens",
    ] {
        assert!(
            sql.contains(snippet),
            "expected migration to contain `{snippet}`"
        );
    }
}

#[test]
fn bootstrap_migration_seeds_development_users_and_runtime_state_columns() {
    let sql = bootstrap_migration_sql();

    for snippet in [
        "insert into users",
        "'nox-dev'",
        "'leo-vance'",
        "'aria-thorne'",
        "'clara-wu'",
        "unread_count integer not null default 0",
        "last_read_message_id uuid",
        "last_delivered_message_id uuid",
        "token_hash text not null unique",
        "create index messages_conversation_created_at_idx",
    ] {
        assert!(
            sql.contains(snippet),
            "expected migration to contain `{snippet}`"
        );
    }
}
