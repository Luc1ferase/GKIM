use crate::im::model::UserProfile;
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
}

#[derive(Debug, Deserialize)]
#[serde(tag = "type", rename_all = "camelCase")]
pub enum ClientEvent {
    #[serde(rename = "ping")]
    Ping,
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
}

pub async fn serve_socket(
    socket: axum::extract::ws::WebSocket,
    hub: ConnectionHub,
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
                        if let Ok(ClientEvent::Ping) = serde_json::from_str::<ClientEvent>(&text) {
                            if send_event(&mut sender, GatewayEvent::Pong { at: now_rfc3339() }).await.is_err() {
                                break;
                            }
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
