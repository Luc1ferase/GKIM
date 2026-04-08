use anyhow::{anyhow, Context, Result};
use gkim_im_backend::{app::build_router, config::AppConfig};
use sqlx::postgres::PgPoolOptions;
use tokio::net::TcpListener;
use tracing::info;
use tracing_subscriber::EnvFilter;

#[tokio::main]
async fn main() -> Result<()> {
    let config = AppConfig::from_env().context("load backend configuration")?;
    init_tracing(&config.log_filter)?;
    let pool = PgPoolOptions::new()
        .max_connections(10)
        .connect(&config.database_url)
        .await
        .context("connect postgres pool")?;

    let listener = TcpListener::bind(&config.bind_addr)
        .await
        .with_context(|| format!("bind backend listener on {}", config.bind_addr))?;
    let router = build_router(config.clone(), pool);

    info!(
        service = %config.service_name,
        bind_addr = %config.bind_addr,
        "backend listener ready"
    );

    axum::serve(listener, router)
        .await
        .context("backend server exited unexpectedly")?;

    Ok(())
}

fn init_tracing(log_filter: &str) -> Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::new(log_filter))
        .with_target(false)
        .compact()
        .try_init()
        .map_err(|error| anyhow!("initialize tracing subscriber: {error}"))
}
