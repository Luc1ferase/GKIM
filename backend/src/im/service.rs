use crate::im::{
    model::{BootstrapBundle, MessageHistoryPage},
    repository::ImRepository,
};
use anyhow::{anyhow, Result};
use sqlx::PgPool;

#[derive(Debug, Clone)]
pub struct ImService {
    repository: ImRepository,
}

impl ImService {
    pub fn new(pool: PgPool) -> Self {
        Self {
            repository: ImRepository::new(pool),
        }
    }

    pub async fn bootstrap_for_user(&self, external_id: &str) -> Result<BootstrapBundle> {
        let user = self
            .repository
            .find_user_by_external_id(external_id)
            .await?
            .ok_or_else(|| anyhow!("user `{external_id}` was not found"))?;
        let contacts = self.repository.list_contacts(&user.id).await?;
        let conversations = self
            .repository
            .list_conversation_summaries(&user.id)
            .await?;

        Ok(BootstrapBundle {
            user,
            contacts,
            conversations,
        })
    }

    pub async fn history_for_user(
        &self,
        external_id: &str,
        conversation_id: &str,
        page_size: u32,
        before_message_id: Option<&str>,
    ) -> Result<MessageHistoryPage> {
        let user = self
            .repository
            .find_user_by_external_id(external_id)
            .await?
            .ok_or_else(|| anyhow!("user `{external_id}` was not found"))?;
        let limit = page_size.clamp(1, 100) as i64;

        let cursor = match before_message_id {
            Some(message_id) => Some(
                self.repository
                    .resolve_history_cursor(&user.id, conversation_id, message_id)
                    .await?
                    .ok_or_else(|| {
                        anyhow!(
                            "message cursor `{message_id}` was not found in conversation `{conversation_id}` for user `{external_id}`"
                        )
                    })?,
            ),
            None => None,
        };

        let mut messages = self
            .repository
            .list_message_history(&user.id, conversation_id, limit + 1, cursor.as_ref())
            .await?;

        let has_more = messages.len() > limit as usize;
        if has_more {
            messages.pop();
        }
        messages.reverse();

        Ok(MessageHistoryPage {
            conversation_id: conversation_id.to_string(),
            messages,
            has_more,
        })
    }
}
