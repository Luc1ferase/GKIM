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
pub struct MessageAttachment {
    #[serde(rename = "type")]
    pub attachment_type: String,
    pub content_type: String,
    pub fetch_path: String,
    pub size_bytes: i64,
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
    pub attachment: Option<MessageAttachment>,
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

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SendMessageResult {
    pub conversation_id: String,
    pub recipient_external_id: String,
    pub recipient_unread_count: i64,
    pub message: MessageRecord,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DeliveryUpdate {
    pub conversation_id: String,
    pub message_id: String,
    pub recipient_external_id: String,
    pub delivered_at: String,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ReadReceiptUpdate {
    pub conversation_id: String,
    pub message_id: String,
    pub reader_external_id: String,
    pub sender_external_id: String,
    pub unread_count: i64,
    pub read_at: String,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NewMessageAttachment {
    pub attachment_type: String,
    pub content_type: String,
    pub bytes: Vec<u8>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct StoredMessageAttachment {
    pub content_type: String,
    pub data: Vec<u8>,
}
