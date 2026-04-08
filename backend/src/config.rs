use anyhow::{anyhow, ensure, Result};
use std::collections::HashMap;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AppConfig {
    pub service_name: String,
    pub bind_addr: String,
    pub database_url: String,
    pub log_filter: String,
}

impl AppConfig {
    pub fn from_env() -> Result<Self> {
        let values = std::env::vars().collect::<HashMap<_, _>>();
        Self::from_map(&values)
    }

    pub fn from_map(values: &HashMap<String, String>) -> Result<Self> {
        let service_name = value_or_default(values, "APP_SERVICE_NAME", "gkim-im-backend");
        let bind_addr = value_or_default(values, "APP_BIND_ADDR", "0.0.0.0:8080");
        let log_filter = values
            .get("APP_LOG_FILTER")
            .or_else(|| values.get("RUST_LOG"))
            .map(|value| value.trim().to_string())
            .filter(|value| !value.is_empty())
            .unwrap_or_else(|| "info".to_string());

        let database_url = values
            .get("DATABASE_URL")
            .map(|value| value.trim().to_string())
            .filter(|value| !value.is_empty())
            .map(Ok)
            .unwrap_or_else(|| build_database_url(values))?;

        Ok(Self {
            service_name,
            bind_addr,
            database_url,
            log_filter,
        })
    }
}

fn build_database_url(values: &HashMap<String, String>) -> Result<String> {
    let host = required_value(values, "PGHOST")?;
    let port = required_value(values, "PGPORT")?;
    let user = required_value(values, "PGUSER")?;
    let password = required_value(values, "PGPASSWORD")?;
    let database = required_value(values, "PGDATABASE")?;

    let mut database_url = format!(
        "postgres://{}:{}@{}:{}/{}",
        encode_url_component(user),
        encode_url_component(password),
        host,
        port,
        encode_url_component(database)
    );

    let mut query = Vec::new();

    if let Some(ssl_mode) = optional_value(values, "PGSSLMODE") {
        query.push(format!("sslmode={}", encode_url_component(ssl_mode)));
    }

    if let Some(root_cert) = optional_value(values, "PGSSLROOTCERT") {
        query.push(format!("sslrootcert={}", encode_url_component(root_cert)));
    }

    if !query.is_empty() {
        database_url.push('?');
        database_url.push_str(&query.join("&"));
    }

    Ok(database_url)
}

fn required_value<'a>(values: &'a HashMap<String, String>, key: &str) -> Result<&'a str> {
    let value = optional_value(values, key)
        .ok_or_else(|| anyhow!("missing required environment variable `{key}`"))?;
    ensure!(
        !value.is_empty(),
        "environment variable `{key}` must not be empty"
    );
    Ok(value)
}

fn optional_value<'a>(values: &'a HashMap<String, String>, key: &str) -> Option<&'a str> {
    values.get(key).map(String::as_str).map(str::trim)
}

fn value_or_default(values: &HashMap<String, String>, key: &str, default: &str) -> String {
    optional_value(values, key)
        .filter(|value| !value.is_empty())
        .unwrap_or(default)
        .to_string()
}

fn encode_url_component(value: &str) -> String {
    value
        .bytes()
        .flat_map(|byte| match byte {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => {
                vec![byte as char]
            }
            _ => format!("%{byte:02X}").chars().collect::<Vec<_>>(),
        })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::AppConfig;
    use std::collections::HashMap;

    #[test]
    fn builds_database_url_from_discrete_postgres_env() {
        let values = HashMap::from([
            (
                "APP_SERVICE_NAME".to_string(),
                "gkim-im-backend".to_string(),
            ),
            ("APP_BIND_ADDR".to_string(), "127.0.0.1:8080".to_string()),
            ("PGHOST".to_string(), "124.222.15.128".to_string()),
            ("PGPORT".to_string(), "5432".to_string()),
            ("PGUSER".to_string(), "postgres".to_string()),
            ("PGPASSWORD".to_string(), "secret".to_string()),
            ("PGDATABASE".to_string(), "gkim_im".to_string()),
            ("PGSSLMODE".to_string(), "disable".to_string()),
        ]);

        let config = AppConfig::from_map(&values).expect("config should load");

        assert_eq!(config.service_name, "gkim-im-backend");
        assert_eq!(config.bind_addr, "127.0.0.1:8080");
        assert_eq!(
            config.database_url,
            "postgres://postgres:secret@124.222.15.128:5432/gkim_im?sslmode=disable"
        );
    }

    #[test]
    fn prefers_explicit_database_url_when_present() {
        let values = HashMap::from([
            ("DATABASE_URL".to_string(), "postgres://example".to_string()),
            ("PGHOST".to_string(), "ignored".to_string()),
            ("PGPORT".to_string(), "5432".to_string()),
            ("PGUSER".to_string(), "ignored".to_string()),
            ("PGPASSWORD".to_string(), "ignored".to_string()),
            ("PGDATABASE".to_string(), "ignored".to_string()),
        ]);

        let config = AppConfig::from_map(&values).expect("config should load");

        assert_eq!(config.database_url, "postgres://example");
        assert_eq!(config.service_name, "gkim-im-backend");
        assert_eq!(config.bind_addr, "0.0.0.0:8080");
        assert_eq!(config.log_filter, "info");
    }
}
