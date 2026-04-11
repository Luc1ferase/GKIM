use anyhow::{anyhow, Context, Result};
use sqlx::{
    migrate::{Migrate, Migration, Migrator},
    query, query_as, query_scalar, PgConnection, PgPool,
};
use std::path::Path;
use tracing::info;

const BOOTSTRAP_MIGRATION_VERSION: i64 = 202604080001;
const AUTH_MIGRATION_VERSION: i64 = 202604100001;

#[derive(Debug)]
enum SchemaState {
    Absent,
    Complete,
    Partial(String),
}

pub async fn apply_runtime_migrations(pool: &PgPool) -> Result<()> {
    let mut migrator = Migrator::new(Path::new("./migrations"))
        .await
        .context("load runtime migrations from ./migrations")?;
    migrator.set_locking(false);

    let mut conn = pool
        .acquire()
        .await
        .context("acquire postgres connection for runtime migrations")?;

    (&mut *conn)
        .lock()
        .await
        .context("acquire runtime migration lock")?;

    let outcome = async {
        (&mut *conn)
            .ensure_migrations_table()
            .await
            .context("ensure _sqlx_migrations table")?;
        seed_legacy_applied_migrations_if_needed(&mut conn, &migrator).await?;
        migrator
            .run_direct(&mut *conn)
            .await
            .context("run pending runtime migrations")?;
        Ok::<(), anyhow::Error>(())
    }
    .await;

    let unlock = (&mut *conn)
        .unlock()
        .await
        .context("release runtime migration lock");

    match (outcome, unlock) {
        (Ok(()), Ok(())) => Ok(()),
        (Err(error), Ok(())) => Err(error),
        (Ok(()), Err(error)) => Err(error),
        (Err(error), Err(unlock_error)) => Err(error.context(format!(
            "release runtime migration lock after failure: {unlock_error}"
        ))),
    }
}

async fn seed_legacy_applied_migrations_if_needed(
    conn: &mut PgConnection,
    migrator: &Migrator,
) -> Result<()> {
    let applied_count: i64 = query_scalar("select count(*) from _sqlx_migrations")
        .fetch_one(&mut *conn)
        .await
        .context("count applied runtime migrations")?;

    if applied_count > 0 {
        return Ok(());
    }

    let bootstrap_state = bootstrap_schema_state(conn).await?;
    let auth_state = auth_schema_state(conn).await?;

    match bootstrap_state {
        SchemaState::Absent => {
            if matches!(auth_state, SchemaState::Absent) {
                return Ok(());
            }

            return Err(anyhow!(
                "auth schema markers exist without the full legacy bootstrap schema"
            ));
        }
        SchemaState::Partial(details) => {
            return Err(anyhow!(
                "legacy bootstrap schema is only partially present: {details}"
            ));
        }
        SchemaState::Complete => {}
    }

    let bootstrap_migration = migration_by_version(migrator, BOOTSTRAP_MIGRATION_VERSION)?;
    insert_applied_migration(conn, bootstrap_migration).await?;
    info!(
        version = BOOTSTRAP_MIGRATION_VERSION,
        "seeded legacy bootstrap migration record"
    );

    match auth_state {
        SchemaState::Absent => Ok(()),
        SchemaState::Complete => {
            let auth_migration = migration_by_version(migrator, AUTH_MIGRATION_VERSION)?;
            insert_applied_migration(conn, auth_migration).await?;
            info!(
                version = AUTH_MIGRATION_VERSION,
                "seeded legacy auth migration record"
            );
            Ok(())
        }
        SchemaState::Partial(details) => Err(anyhow!(
            "legacy auth schema is only partially present: {details}"
        )),
    }
}

fn migration_by_version<'a>(migrator: &'a Migrator, version: i64) -> Result<&'a Migration> {
    migrator
        .iter()
        .find(|migration| migration.version == version)
        .with_context(|| format!("resolve migration metadata for version {version}"))
}

async fn insert_applied_migration(conn: &mut PgConnection, migration: &Migration) -> Result<()> {
    query(
        r#"
insert into _sqlx_migrations (version, description, success, checksum, execution_time)
values ($1, $2, true, $3, 0)
on conflict (version) do nothing
        "#,
    )
    .bind(migration.version)
    .bind(&*migration.description)
    .bind(migration.checksum.as_ref())
    .execute(&mut *conn)
    .await
    .with_context(|| format!("insert applied migration row {}", migration.version))?;

    Ok(())
}

async fn bootstrap_schema_state(conn: &mut PgConnection) -> Result<SchemaState> {
    let (
        users,
        contacts,
        direct_conversations,
        conversation_members,
        messages,
        message_receipts,
        session_tokens,
    ): (bool, bool, bool, bool, bool, bool, bool) = query_as(
        r#"
select
    to_regclass('public.users') is not null,
    to_regclass('public.contacts') is not null,
    to_regclass('public.direct_conversations') is not null,
    to_regclass('public.conversation_members') is not null,
    to_regclass('public.messages') is not null,
    to_regclass('public.message_receipts') is not null,
    to_regclass('public.session_tokens') is not null
        "#,
    )
    .fetch_one(&mut *conn)
    .await
    .context("inspect legacy bootstrap schema markers")?;

    Ok(schema_state(&[
        ("users", users),
        ("contacts", contacts),
        ("direct_conversations", direct_conversations),
        ("conversation_members", conversation_members),
        ("messages", messages),
        ("message_receipts", message_receipts),
        ("session_tokens", session_tokens),
    ]))
}

async fn auth_schema_state(conn: &mut PgConnection) -> Result<SchemaState> {
    let (
        username_column,
        password_hash_column,
        username_index,
        friend_requests_table,
        pending_pair_index,
        pending_to_user_index,
    ): (bool, bool, bool, bool, bool, bool) = query_as(
        r#"
select
    exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'users'
          and column_name = 'username'
    ),
    exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'users'
          and column_name = 'password_hash'
    ),
    to_regclass('public.users_username_lower_idx') is not null,
    to_regclass('public.friend_requests') is not null,
    to_regclass('public.friend_requests_pending_pair_idx') is not null,
    to_regclass('public.friend_requests_to_user_pending_idx') is not null
        "#,
    )
    .fetch_one(&mut *conn)
    .await
    .context("inspect legacy auth schema markers")?;

    Ok(schema_state(&[
        ("users.username", username_column),
        ("users.password_hash", password_hash_column),
        ("users_username_lower_idx", username_index),
        ("friend_requests", friend_requests_table),
        (
            "friend_requests_pending_pair_idx",
            pending_pair_index,
        ),
        (
            "friend_requests_to_user_pending_idx",
            pending_to_user_index,
        ),
    ]))
}

fn schema_state(markers: &[(&str, bool)]) -> SchemaState {
    let present: Vec<&str> = markers
        .iter()
        .filter_map(|(name, exists)| exists.then_some(*name))
        .collect();

    if present.is_empty() {
        SchemaState::Absent
    } else if present.len() == markers.len() {
        SchemaState::Complete
    } else {
        let missing = markers
            .iter()
            .filter_map(|(name, exists)| (!exists).then_some(*name))
            .collect::<Vec<_>>()
            .join(", ");
        SchemaState::Partial(format!("present [{}], missing [{}]", present.join(", "), missing))
    }
}
