use crate::config::AppConfig;
use crate::im::service::ImService;
use crate::session::{SessionIssue, SessionService};
use crate::ws::{serve_socket, ConnectionHub};
use axum::{
    extract::{ws::WebSocketUpgrade, Path, Query, State},
    http::{HeaderMap, StatusCode},
    response::{IntoResponse, Response},
    routing::{get, post},
    Json, Router,
};
use serde::{Deserialize, Serialize};
use sqlx::PgPool;

#[derive(Clone)]
struct AppState {
    service_name: String,
    session_service: SessionService,
    im_service: ImService,
    connection_hub: ConnectionHub,
}

#[derive(Serialize)]
struct HealthResponse {
    service: String,
    status: &'static str,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct ErrorResponse {
    error: &'static str,
    message: String,
}

#[derive(Debug)]
struct AppError {
    status: StatusCode,
    error: &'static str,
    message: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct DevSessionRequest {
    external_id: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct HistoryQuery {
    limit: Option<u32>,
    before: Option<String>,
}

impl AppError {
    fn bad_request(message: impl Into<String>) -> Self {
        Self {
            status: StatusCode::BAD_REQUEST,
            error: "bad_request",
            message: message.into(),
        }
    }

    fn unauthorized(message: impl Into<String>) -> Self {
        Self {
            status: StatusCode::UNAUTHORIZED,
            error: "unauthorized",
            message: message.into(),
        }
    }

    fn not_found(message: impl Into<String>) -> Self {
        Self {
            status: StatusCode::NOT_FOUND,
            error: "not_found",
            message: message.into(),
        }
    }

    fn internal(message: impl Into<String>) -> Self {
        Self {
            status: StatusCode::INTERNAL_SERVER_ERROR,
            error: "internal_error",
            message: message.into(),
        }
    }
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        (
            self.status,
            Json(ErrorResponse {
                error: self.error,
                message: self.message,
            }),
        )
            .into_response()
    }
}

pub fn build_router(config: AppConfig, pool: PgPool) -> Router {
    Router::new()
        .route("/health", get(health_handler))
        .route("/ws", get(websocket_gateway))
        .route("/api/session/dev", post(issue_dev_session))
        .route("/api/bootstrap", get(get_bootstrap))
        .route(
            "/api/conversations/:conversation_id/messages",
            get(get_message_history),
        )
        .with_state(AppState {
            service_name: config.service_name,
            session_service: SessionService::new(pool.clone()),
            im_service: ImService::new(pool),
            connection_hub: ConnectionHub::default(),
        })
}

async fn health_handler(State(state): State<AppState>) -> Json<HealthResponse> {
    Json(HealthResponse {
        service: state.service_name,
        status: "ok",
    })
}

async fn websocket_gateway(
    State(state): State<AppState>,
    headers: HeaderMap,
    websocket: WebSocketUpgrade,
) -> Result<Response, AppError> {
    let user = authenticated_user(&state, &headers).await?;
    let hub = state.connection_hub.clone();
    let im_service = state.im_service.clone();

    Ok(websocket.on_upgrade(move |socket| async move {
        serve_socket(socket, hub, im_service, user).await;
    }))
}

async fn issue_dev_session(
    State(state): State<AppState>,
    Json(request): Json<DevSessionRequest>,
) -> Result<Json<SessionIssue>, AppError> {
    if request.external_id.trim().is_empty() {
        return Err(AppError::bad_request(
            "`externalId` must be provided for dev session bootstrap",
        ));
    }

    let session = state
        .session_service
        .issue_dev_session(request.external_id.trim())
        .await
        .map_err(|error| AppError::internal(format!("issue dev session: {error}")))?
        .ok_or_else(|| {
            AppError::not_found(format!(
                "development user `{}` was not found",
                request.external_id.trim()
            ))
        })?;

    Ok(Json(session))
}

async fn get_bootstrap(
    State(state): State<AppState>,
    headers: HeaderMap,
) -> Result<Json<crate::im::model::BootstrapBundle>, AppError> {
    let user = authenticated_user(&state, &headers).await?;
    let bundle = state
        .im_service
        .bootstrap_for_user(&user.external_id)
        .await
        .map_err(|error| AppError::internal(format!("load bootstrap payload: {error}")))?;

    Ok(Json(bundle))
}

async fn get_message_history(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(conversation_id): Path<String>,
    Query(query): Query<HistoryQuery>,
) -> Result<Json<crate::im::model::MessageHistoryPage>, AppError> {
    let user = authenticated_user(&state, &headers).await?;
    let page = state
        .im_service
        .history_for_user(
            &user.external_id,
            &conversation_id,
            query.limit.unwrap_or(50),
            query.before.as_deref(),
        )
        .await
        .map_err(|error| AppError::internal(format!("load message history: {error}")))?;

    Ok(Json(page))
}

async fn authenticated_user(
    state: &AppState,
    headers: &HeaderMap,
) -> Result<crate::im::model::UserProfile, AppError> {
    let token = bearer_token(headers)?;
    state
        .session_service
        .authenticate(token)
        .await
        .map_err(|error| AppError::internal(format!("authenticate request: {error}")))?
        .ok_or_else(|| AppError::unauthorized("session token is invalid or expired"))
}

fn bearer_token(headers: &HeaderMap) -> Result<&str, AppError> {
    let value = headers
        .get("authorization")
        .ok_or_else(|| AppError::unauthorized("missing bearer token"))?
        .to_str()
        .map_err(|_| AppError::unauthorized("authorization header must be valid UTF-8"))?;

    value
        .strip_prefix("Bearer ")
        .filter(|token| !token.trim().is_empty())
        .ok_or_else(|| {
            AppError::unauthorized("authorization header must use the `Bearer <token>` format")
        })
}

#[cfg(test)]
mod tests {
    use super::build_router;
    use crate::config::AppConfig;
    use axum::body::Body;
    use axum::http::{Request, StatusCode};
    use http_body_util::BodyExt;
    use sqlx::postgres::PgPoolOptions;
    use tower::util::ServiceExt;

    #[tokio::test]
    async fn health_endpoint_reports_service_name_and_status() {
        let pool = PgPoolOptions::new()
            .connect_lazy("postgres://example")
            .expect("lazy pool should be created");
        let router = build_router(
            AppConfig {
                service_name: "gkim-im-backend".to_string(),
                bind_addr: "127.0.0.1:8080".to_string(),
                database_url: "postgres://example".to_string(),
                log_filter: "info".to_string(),
            },
            pool,
        );

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
