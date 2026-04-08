use crate::config::AppConfig;
use axum::{extract::State, routing::get, Json, Router};
use serde::Serialize;

#[derive(Clone)]
struct AppState {
    service_name: String,
}

#[derive(Serialize)]
struct HealthResponse {
    service: String,
    status: &'static str,
}

pub fn build_router(config: AppConfig) -> Router {
    Router::new()
        .route("/health", get(health_handler))
        .with_state(AppState {
            service_name: config.service_name,
        })
}

async fn health_handler(State(state): State<AppState>) -> Json<HealthResponse> {
    Json(HealthResponse {
        service: state.service_name,
        status: "ok",
    })
}

#[cfg(test)]
mod tests {
    use super::build_router;
    use crate::config::AppConfig;
    use axum::body::Body;
    use axum::http::{Request, StatusCode};
    use http_body_util::BodyExt;
    use tower::util::ServiceExt;

    #[tokio::test]
    async fn health_endpoint_reports_service_name_and_status() {
        let router = build_router(AppConfig {
            service_name: "gkim-im-backend".to_string(),
            bind_addr: "127.0.0.1:8080".to_string(),
            database_url: "postgres://example".to_string(),
            log_filter: "info".to_string(),
        });

        let response = router
            .oneshot(
                Request::builder()
                    .uri("/health")
                    .body(Body::empty())
                    .unwrap(),
            )
            .await
            .expect("request should succeed");

        assert_eq!(response.status(), StatusCode::OK);

        let body = response.into_body().collect().await.unwrap().to_bytes();
        let text = String::from_utf8(body.to_vec()).unwrap();
        assert!(text.contains("\"service\":\"gkim-im-backend\""));
        assert!(text.contains("\"status\":\"ok\""));
    }
}
