package com.gkim.im.android.core.model

enum class MessageDirection {
    Incoming,
    Outgoing,
    System,
}

enum class MessageKind {
    Text,
    Aigc,
}

enum class AttachmentType {
    Image,
    Video,
}

data class MessageAttachment(
    val type: AttachmentType,
    val preview: String,
    val prompt: String? = null,
    val generationId: String? = null,
)

data class ChatMessage(
    val id: String,
    val direction: MessageDirection,
    val kind: MessageKind,
    val body: String,
    val createdAt: String,
    val chips: List<String> = emptyList(),
    val attachment: MessageAttachment? = null,
)

data class Conversation(
    val id: String,
    val contactId: String,
    val contactName: String,
    val contactTitle: String,
    val avatarText: String,
    val lastMessage: String,
    val lastTimestamp: String,
    val unreadCount: Int,
    val isOnline: Boolean,
    val messages: List<ChatMessage>,
)
