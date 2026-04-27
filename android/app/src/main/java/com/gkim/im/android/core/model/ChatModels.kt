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

enum class MessageStatus {
    Pending,
    Thinking,
    Streaming,
    Completed,
    Failed,
    Blocked,
    Timeout,
}

data class CompanionTurnMeta(
    val turnId: String,
    val variantGroupId: String,
    val variantIndex: Int,
    val providerId: String? = null,
    val model: String? = null,
    val isEditable: Boolean = false,
    val canRegenerate: Boolean = false,
    val blockReasonKey: String? = null,
    val failedSubtypeKey: String? = null,
    val timeoutElapsedMs: Long? = null,
    val siblingCount: Int = 1,
    val siblingActiveIndex: Int = 0,
)

data class MessageAttachment(
    val type: AttachmentType,
    val preview: String,
    val prompt: String? = null,
    val generationId: String? = null,
    val authToken: String? = null,
)

data class ChatMessage(
    val id: String,
    val direction: MessageDirection,
    val kind: MessageKind,
    val body: String,
    val createdAt: String,
    val deliveredAt: String? = null,
    val readAt: String? = null,
    val chips: List<String> = emptyList(),
    val attachment: MessageAttachment? = null,
    val parentMessageId: String? = null,
    val status: MessageStatus = MessageStatus.Completed,
    val companionTurnMeta: CompanionTurnMeta? = null,
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
    val companionCardId: String? = null,
)
