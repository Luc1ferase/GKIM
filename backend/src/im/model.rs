use serde::Serialize;

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UserProfile {
    pub id: String,
    pub external_id: String,
    pub display_name: String,
    pub title: String,
    pub avatar_text: String,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ContactProfile {
    pub user_id: String,
    pub external_id: String,
    pub display_name: String,
    pub title: String,
    pub avatar_text: String,
    pub added_at: String,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct MessageRecord {
    pub id: String,
    pub conversation_id: String,
    pub sender_user_id: String,
    pub sender_external_id: String,
    pub kind: String,
    pub body: String,
    pub created_at: String,
    pub delivered_at: Option<String>,
    pub read_at: Option<String>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ConversationSummary {
    pub conversation_id: String,
    pub contact: ContactProfile,
    pub unread_count: i64,
    pub last_message: Option<MessageRecord>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct BootstrapBundle {
    pub user: UserProfile,
    pub contacts: Vec<ContactProfile>,
    pub conversations: Vec<ConversationSummary>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct MessageHistoryPage {
    pub conversation_id: String,
    pub messages: Vec<MessageRecord>,
    pub has_more: bool,
}
