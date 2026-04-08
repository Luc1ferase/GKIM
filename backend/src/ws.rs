use crate::im::{model::UserProfile, service::ImService};
use anyhow::Result;
use axum::extract::ws::Message;
use futures_util::{SinkExt, StreamExt};
use serde::{Deserialize, Serialize};
use std::{
    collections::HashMap,
    sync::{
        atomic::{AtomicU64, Ordering},
        Arc,
    },
    time::{SystemTime, UNIX_EPOCH},
};
use tokio::sync::{mpsc, RwLock};

#[derive(Debug, Clone, Default)]
pub struct ConnectionHub {
    connections: Arc<RwLock<HashMap<String, HashMap<String, mpsc::UnboundedSender<GatewayEvent>>>>>,
    counter: Arc<AtomicU64>,
}

#[derive(Debug)]
pub struct ConnectionRegistration {
    pub connection_id: String,
    pub active_connections: usize,
    pub receiver: mpsc::UnboundedReceiver<GatewayEvent>,
}

#[derive(Debug, Serialize, Clone)]
#[serde(tag = "type", rename_all = "camelCase")]
pub enum GatewayEvent {
    #[serde(rename = "session.registered")]
    #[serde(rename_all = "camelCase")]
    SessionRegistered {
        connection_id: String,
        active_connections: usize,
        user: UserProfile,
    },
    #[serde(rename = "pong")]
    #[serde(rename_all = "camelCase")]
    Pong { at: String },
    #[serde(rename = "message.sent")]
    #[serde(rename_all = "camelCase")]
    MessageSent {
        conversation_id: String,
        message: crate::im::model::MessageRecord,
    },
    #[serde(rename = "message.received")]
    #[serde(rename_all = "camelCase")]
    MessageReceived {
        conversation_id: String,
        unread_count: i64,
        message: crate::im::model::MessageRecord,
    },
    #[serde(rename = "message.delivered")]
    #[serde(rename_all = "camelCase")]
    MessageDelivered {
        conversation_id: String,
        message_id: String,
        recipient_external_id: String,
        delivered_at: String,
    },
    #[serde(rename = "message.read")]
    #[serde(rename_all = "camelCase")]
    MessageRead {
        conversation_id: String,
        message_id: String,
        reader_external_id: String,
        unread_count: i64,
        read_at: String,
    },
    #[serde(rename = "error")]
    #[serde(rename_all = "camelCase")]
    Error { code: String, message: String },
}

#[derive(Debug, Deserialize)]
#[serde(tag = "type", rename_all = "camelCase")]
pub enum ClientEvent {
    #[serde(rename = "ping")]
    Ping,
    #[serde(rename = "message.send")]
    #[serde(rename_all = "camelCase")]
    MessageSend {
        recipient_external_id: String,
        client_message_id: Option<String>,
        body: String,
    },
    #[serde(rename = "message.read")]
    #[serde(rename_all = "camelCase")]
    MessageRead {
        conversation_id: String,
        message_id: String,
    },
}

impl ConnectionHub {
    pub async fn register(&self, user: &UserProfile) -> ConnectionRegistration {
        let connection_id = format!("ws-{}", self.counter.fetch_add(1, Ordering::Relaxed) + 1);
        let (sender, receiver) = mpsc::unbounded_channel();

        let mut guard = self.connections.write().await;
        let user_connections = guard.entry(user.external_id.clone()).or_default();
        user_connections.insert(connection_id.clone(), sender);
        let active_connections = user_connections.len();

        ConnectionRegistration {
            connection_id,
            active_connections,
            receiver,
        }
    }

    pub async fn unregister(&self, user_external_id: &str, connection_id: &str) {
        let mut guard = self.connections.write().await;
        if let Some(user_connections) = guard.get_mut(user_external_id) {
            user_connections.remove(connection_id);
            if user_connections.is_empty() {
                guard.remove(user_external_id);
            }
        }
    }

    pub async fn connection_count(&self, user_external_id: &str) -> usize {
        let guard = self.connections.read().await;
        guard
            .get(user_external_id)
            .map(|value| value.len())
            .unwrap_or(0)
    }

    pub async fn send_to_user(&self, user_external_id: &str, event: GatewayEvent) -> usize {
        let senders = {
            let guard = self.connections.read().await;
            guard
                .get(user_external_id)
                .map(|value| value.values().cloned().collect::<Vec<_>>())
                .unwrap_or_default()
        };

        let mut delivered = 0;
        for sender in senders {
            if sender.send(event.clone()).is_ok() {
                delivered += 1;
            }
        }

        delivered
    }
}

pub async fn serve_socket(
    socket: axum::extract::ws::WebSocket,
    hub: ConnectionHub,
    im_service: ImService,
    user: UserProfile,
) {
    let registration = hub.register(&user).await;
    let connection_id = registration.connection_id.clone();

    let (mut sender, mut receiver) = socket.split();
    let mut outbound = registration.receiver;

    if send_event(
        &mut sender,
        GatewayEvent::SessionRegistered {
            connection_id: registration.connection_id,
            active_connections: registration.active_connections,
            user: user.clone(),
        },
    )
    .await
    .is_err()
    {
        hub.unregister(&user.external_id, &connection_id).await;
        return;
    }

    loop {
        tokio::select! {
            maybe_event = outbound.recv() => {
                match maybe_event {
                    Some(event) => {
                        if send_event(&mut sender, event).await.is_err() {
                            break;
                        }
                    }
                    None => break,
                }
            }
            maybe_message = receiver.next() => {
                match maybe_message {
                    Some(Ok(Message::Text(text))) => {
                        match serde_json::from_str::<ClientEvent>(&text) {
                            Ok(ClientEvent::Ping) => {
                                if send_event(&mut sender, GatewayEvent::Pong { at: now_rfc3339() }).await.is_err() {
                                    break;
                                }
                            }
                            Ok(ClientEvent::MessageSend {
                                recipient_external_id,
                                client_message_id,
                                body,
                            }) => {
                                if let Err(error) = handle_message_send(
                                    &hub,
                                    &im_service,
                                    &user,
                                    &recipient_external_id,
                                    client_message_id.as_deref(),
                                    &body,
                                )
                                .await
                                {
                                    if send_event(
                                        &mut sender,
                                        GatewayEvent::Error {
                                            code: "message.send_failed".to_string(),
                                            message: error.to_string(),
                                        },
                                    )
                                    .await
                                    .is_err()
                                    {
                                        break;
                                    }
                                }
                            }
                            Ok(ClientEvent::MessageRead {
                                conversation_id,
                                message_id,
                            }) => {
                                if let Err(error) = handle_message_read(
                                    &hub,
                                    &im_service,
                                    &user,
                                    &conversation_id,
                                    &message_id,
                                )
                                .await
                                {
                                    if send_event(
                                        &mut sender,
                                        GatewayEvent::Error {
                                            code: "message.read_failed".to_string(),
                                            message: error.to_string(),
                                        },
                                    )
                                    .await
                                    .is_err()
                                    {
                                        break;
                                    }
                                }
                            }
                            Err(_) => {}
                        }
                    }
                    Some(Ok(Message::Close(_))) | None => break,
                    Some(Ok(_)) => {}
                    Some(Err(_)) => break,
                }
            }
        }
    }

    hub.unregister(&user.external_id, &connection_id).await;
}

async fn handle_message_send(
    hub: &ConnectionHub,
    im_service: &ImService,
    sender: &UserProfile,
    recipient_external_id: &str,
    client_message_id: Option<&str>,
    body: &str,
) -> Result<()> {
    let send_result = im_service
        .send_direct_message(
            &sender.external_id,
            recipient_external_id,
            client_message_id,
            body,
        )
        .await?;

    hub.send_to_user(
        &sender.external_id,
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
            &sender.external_id,
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

async fn handle_message_read(
    hub: &ConnectionHub,
    im_service: &ImService,
    reader: &UserProfile,
    conversation_id: &str,
    message_id: &str,
) -> Result<()> {
    let update = im_service
        .mark_message_read(&reader.external_id, conversation_id, message_id)
        .await?;

    let event = GatewayEvent::MessageRead {
        conversation_id: update.conversation_id.clone(),
        message_id: update.message_id.clone(),
        reader_external_id: update.reader_external_id.clone(),
        unread_count: update.unread_count,
        read_at: update.read_at.clone(),
    };

    hub.send_to_user(&update.sender_external_id, event.clone())
        .await;
    hub.send_to_user(&reader.external_id, event).await;

    Ok(())
}

async fn send_event(
    sender: &mut futures_util::stream::SplitSink<axum::extract::ws::WebSocket, Message>,
    event: GatewayEvent,
) -> Result<()> {
    sender
        .send(Message::Text(serde_json::to_string(&event)?.into()))
        .await?;
    Ok(())
}

fn now_rfc3339() -> String {
    let seconds = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .expect("clock should be after epoch")
        .as_secs();
    chrono_like_timestamp(seconds)
}

fn chrono_like_timestamp(seconds: u64) -> String {
    // Good enough UTC second-resolution formatting without introducing another time dependency.
    // Convert using the platform chrono formatter in the database-friendly shape.
    let datetime = std::time::UNIX_EPOCH + std::time::Duration::from_secs(seconds);
    let system = chrono_like_components(datetime);
    format!(
        "{:04}-{:02}-{:02}T{:02}:{:02}:{:02}Z",
        system.year, system.month, system.day, system.hour, system.minute, system.second
    )
}

struct DateTimeParts {
    year: i32,
    month: u32,
    day: u32,
    hour: u32,
    minute: u32,
    second: u32,
}

fn chrono_like_components(datetime: std::time::SystemTime) -> DateTimeParts {
    use std::time::Duration;

    let duration = datetime
        .duration_since(UNIX_EPOCH)
        .unwrap_or(Duration::from_secs(0));
    let total_seconds = duration.as_secs() as i64;
    let days = total_seconds.div_euclid(86_400);
    let seconds_of_day = total_seconds.rem_euclid(86_400);
    let hour = (seconds_of_day / 3_600) as u32;
    let minute = ((seconds_of_day % 3_600) / 60) as u32;
    let second = (seconds_of_day % 60) as u32;

    let (year, month, day) = civil_from_days(days);

    DateTimeParts {
        year,
        month,
        day,
        hour,
        minute,
        second,
    }
}

fn civil_from_days(days_since_epoch: i64) -> (i32, u32, u32) {
    let z = days_since_epoch + 719_468;
    let era = if z >= 0 { z } else { z - 146_096 } / 146_097;
    let doe = z - era * 146_097;
    let yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365;
    let y = yoe + era * 400;
    let doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
    let mp = (5 * doy + 2) / 153;
    let d = doy - (153 * mp + 2) / 5 + 1;
    let m = mp + if mp < 10 { 3 } else { -9 };
    let year = y + if m <= 2 { 1 } else { 0 };

    (year as i32, m as u32, d as u32)
}

#[cfg(test)]
mod tests {
    use super::ConnectionHub;
    use crate::im::model::UserProfile;

    #[tokio::test]
    async fn hub_tracks_multiple_connections_for_same_user() {
        let hub = ConnectionHub::default();
        let user = UserProfile {
            id: "user-1".to_string(),
            external_id: "nox-dev".to_string(),
            display_name: "Nox Dev".to_string(),
            title: "IM Milestone Owner".to_string(),
            avatar_text: "NX".to_string(),
        };

        let first = hub.register(&user).await;
        let second = hub.register(&user).await;

        assert_eq!(first.active_connections, 1);
        assert_eq!(second.active_connections, 2);
        assert_eq!(hub.connection_count("nox-dev").await, 2);

        hub.unregister("nox-dev", &first.connection_id).await;
        assert_eq!(hub.connection_count("nox-dev").await, 1);
    }
}
