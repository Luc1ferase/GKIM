use crate::auth::{AuthResponse, AuthService, LoginError, RegisterError};
use crate::config::AppConfig;
use base64::Engine;
use crate::im::service::ImService;
use crate::session::{SessionIssue, SessionService};
use crate::social::{FriendRequestError, FriendRequestView, SocialService, UserSearchResult};
use crate::ws::{serve_socket, ConnectionHub, GatewayEvent};
use axum::{
    extract::{ws::WebSocketUpgrade, Path, Query, State},
    http::{header::CONTENT_TYPE, HeaderMap, StatusCode},
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
    auth_service: AuthService,
    social_service: SocialService,
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

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct RegisterRequest {
    username: String,
    password: String,
    display_name: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct LoginRequest {
    username: String,
    password: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct SearchQuery {
    q: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct FriendRequestBody {
    to_user_id: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct DirectImageMessageRequest {
    recipient_external_id: String,
    client_message_id: Option<String>,
    #[serde(default)]
    body: String,
    content_type: String,
    image_base64: String,
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
        .route("/api/auth/register", post(register_handler))
        .route("/api/auth/login", post(login_handler))
        .route("/api/users/search", get(search_users_handler))
        .route("/api/friends/request", post(send_friend_request_handler))
        .route("/api/friends/requests", get(list_friend_requests_handler))
        .route(
            "/api/friends/requests/:request_id/accept",
            post(accept_friend_request_handler),
        )
        .route(
            "/api/friends/requests/:request_id/reject",
            post(reject_friend_request_handler),
        )
        .route("/api/bootstrap", get(get_bootstrap))
        .route(
            "/api/conversations/:conversation_id/messages",
            get(get_message_history),
        )
        .route("/api/direct-messages/image", post(send_direct_image_message_handler))
        .route(
            "/api/messages/:message_id/attachment",
            get(get_message_attachment),
        )
        .with_state(AppState {
            service_name: config.service_name,
            session_service: SessionService::new(pool.clone()),
            auth_service: AuthService::new(pool.clone()),
            social_service: SocialService::new(pool.clone()),
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

async fn register_handler(
    State(state): State<AppState>,
    Json(request): Json<RegisterRequest>,
) -> Result<Json<AuthResponse>, AppError> {
    state
        .auth_service
        .register(&request.username, &request.password, &request.display_name)
        .await
        .map(Json)
        .map_err(|e| match e {
            RegisterError::UsernameTaken => AppError {
                status: StatusCode::CONFLICT,
                error: "username_taken",
                message: "username is already taken".to_string(),
            },
            RegisterError::Validation(msg) => AppError::bad_request(msg),
            RegisterError::Internal(err) => AppError::internal(format!("register: {err}")),
        })
}

async fn login_handler(
    State(state): State<AppState>,
    Json(request): Json<LoginRequest>,
) -> Result<Json<AuthResponse>, AppError> {
    state
        .auth_service
        .login(&request.username, &request.password)
        .await
        .map(Json)
        .map_err(|e| match e {
            LoginError::InvalidCredentials => {
                AppError::unauthorized("invalid username or password")
            }
            LoginError::Internal(err) => AppError::internal(format!("login: {err}")),
        })
}

async fn search_users_handler(
    State(state): State<AppState>,
    headers: HeaderMap,
    Query(query): Query<SearchQuery>,
) -> Result<Json<Vec<UserSearchResult>>, AppError> {
    let user = authenticated_user(&state, &headers).await?;
    if query.q.trim().is_empty() {
        return Ok(Json(vec![]));
    }
    let results = state
        .social_service
        .search_users(&user.id, &query.q)
        .await
        .map_err(|e| AppError::internal(format!("search users: {e}")))?;
    Ok(Json(results))
}

async fn send_friend_request_handler(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<FriendRequestBody>,
) -> Result<Json<FriendRequestView>, AppError> {
    let user = authenticated_user(&state, &headers).await?;
    let view = state
        .social_service
        .send_friend_request(&user.id, &body.to_user_id)
        .await
        .map_err(|e| match e {
            FriendRequestError::AlreadyContacts => AppError {
                status: StatusCode::CONFLICT,
                error: "already_contacts",
                message: "users are already contacts".to_string(),
            },
            FriendRequestError::DuplicateRequest => AppError {
                status: StatusCode::CONFLICT,
                error: "duplicate_request",
                message: "a pending friend request already exists".to_string(),
            },
            FriendRequestError::NotFound | FriendRequestError::NotPending => {
                AppError::not_found("friend request not found")
            }
            FriendRequestError::Internal(err) => {
                AppError::internal(format!("send friend request: {err}"))
            }
        })?;

    // Push WS event to the recipient
    state
        .connection_hub
        .send_to_user(
            &view.to_user_external_id,
            GatewayEvent::FriendRequestReceived {
                request_id: view.id.clone(),
                from_user: view.from_user.clone(),
            },
        )
        .await;

    Ok(Json(view))
}

async fn list_friend_requests_handler(
    State(state): State<AppState>,
    headers: HeaderMap,
) -> Result<Json<Vec<FriendRequestView>>, AppError> {
    let user = authenticated_user(&state, &headers).await?;
    let requests = state
        .social_service
        .list_pending_requests(&user.id)
        .await
        .map_err(|e| AppError::internal(format!("list friend requests: {e}")))?;
    Ok(Json(requests))
}

async fn accept_friend_request_handler(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(request_id): Path<String>,
) -> Result<Json<FriendRequestView>, AppError> {
    let user = authenticated_user(&state, &headers).await?;
    let view = state
        .social_service
        .accept_friend_request(&request_id, &user.id)
        .await
        .map_err(|e| match e {
            FriendRequestError::NotFound => {
                AppError::not_found("friend request not found or not addressed to you")
            }
            FriendRequestError::NotPending => AppError {
                status: StatusCode::CONFLICT,
                error: "not_pending",
                message: "friend request is no longer pending".to_string(),
            },
            _ => AppError::internal(format!("accept friend request: {e:?}")),
        })?;

    state
        .connection_hub
        .send_to_user(
            &view.from_user.external_id,
            GatewayEvent::FriendRequestAccepted {
                request_id: view.id.clone(),
                by_user: user.clone(),
            },
        )
        .await;

    Ok(Json(view))
}

async fn reject_friend_request_handler(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(request_id): Path<String>,
) -> Result<Json<FriendRequestView>, AppError> {
    let user = authenticated_user(&state, &headers).await?;
    let view = state
        .social_service
        .reject_friend_request(&request_id, &user.id)
        .await
        .map_err(|e| match e {
            FriendRequestError::NotFound => {
                AppError::not_found("friend request not found or not addressed to you")
            }
            FriendRequestError::NotPending => AppError {
                status: StatusCode::CONFLICT,
                error: "not_pending",
                message: "friend request is no longer pending".to_string(),
            },
            _ => AppError::internal(format!("reject friend request: {e:?}")),
        })?;

    state
        .connection_hub
        .send_to_user(
            &view.from_user.external_id,
            GatewayEvent::FriendRequestRejected {
                request_id: view.id.clone(),
                by_user_id: user.id.clone(),
            },
        )
        .await;

    Ok(Json(view))
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

async fn send_direct_image_message_handler(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(request): Json<DirectImageMessageRequest>,
) -> Result<Json<crate::im::model::SendMessageResult>, AppError> {
    let user = authenticated_user(&state, &headers).await?;
    let image_bytes = base64::engine::general_purpose::STANDARD
        .decode(request.image_base64.trim())
        .map_err(|_| AppError::bad_request("`imageBase64` must be valid base64"))?;

    let send_result = state
        .im_service
        .send_direct_image_message(
            &user.external_id,
            &request.recipient_external_id,
            request.client_message_id.as_deref(),
            &request.body,
            &request.content_type,
            image_bytes,
        )
        .await
        .map_err(|error| AppError::internal(format!("send direct image message: {error}")))?;

    fan_out_direct_message(
        &state.connection_hub,
        &state.im_service,
        &user.external_id,
        &send_result,
    )
    .await
    .map_err(|error| AppError::internal(format!("fan out direct image message: {error}")))?;

    Ok(Json(send_result))
}

async fn get_message_attachment(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(message_id): Path<String>,
) -> Result<Response, AppError> {
    let user = authenticated_user(&state, &headers).await?;
    let Some(attachment) = state
        .im_service
        .attachment_for_user(&user.external_id, &message_id)
        .await
        .map_err(|error| AppError::internal(format!("load message attachment: {error}")))? else {
        return Err(AppError::not_found("message attachment was not found"));
    };

    Response::builder()
        .status(StatusCode::OK)
        .header(CONTENT_TYPE, attachment.content_type)
        .body(axum::body::Body::from(attachment.data))
        .map_err(|error| AppError::internal(format!("build attachment response: {error}")))
}

async fn fan_out_direct_message(
    hub: &ConnectionHub,
    im_service: &ImService,
    sender_external_id: &str,
    send_result: &crate::im::model::SendMessageResult,
) -> anyhow::Result<()> {
    hub.send_to_user(
        sender_external_id,
        GatewayEvent::MessageSent {
            conversation_id: send_result.conversation_id.clone(),
            message: send_result.message.clone(),
        },
    )
    .await;

    let delivered_connections = hub
        .send_to_user(
            &send_result.recipient_external_id,
            GatewayEvent::MessageReceived {
                conversation_id: send_result.conversation_id.clone(),
                unread_count: send_result.recipient_unread_count,
                message: send_result.message.clone(),
            },
        )
        .await;

    if delivered_connections > 0 {
        let delivery = im_service
            .mark_message_delivered(
                &send_result.recipient_external_id,
                &send_result.conversation_id,
                &send_result.message.id,
            )
            .await?;

        hub.send_to_user(
            sender_external_id,
            GatewayEvent::MessageDelivered {
                conversation_id: delivery.conversation_id,
                message_id: delivery.message_id,
                recipient_external_id: delivery.recipient_external_id,
                delivered_at: delivery.delivered_at,
            },
        )
        .await;
    }

    Ok(())
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
