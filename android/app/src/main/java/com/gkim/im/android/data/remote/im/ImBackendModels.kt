package com.gkim.im.android.data.remote.im

import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.core.model.Conversation
import com.gkim.im.android.core.model.AttachmentType
import com.gkim.im.android.core.model.MessageAttachment
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class BackendUserDto(
    val id: String,
    val externalId: String,
    val displayName: String,
    val title: String,
    val avatarText: String,
)

@Serializable
data class ContactProfileDto(
    val userId: String,
    val externalId: String,
    val displayName: String,
    val title: String,
    val avatarText: String,
    val addedAt: String,
)

@Serializable
data class MessageAttachmentDto(
    val type: String,
    val contentType: String,
    val fetchPath: String,
    val sizeBytes: Long,
)

@Serializable
data class MessageRecordDto(
    val id: String,
    val conversationId: String,
    val senderUserId: String,
    val senderExternalId: String,
    val kind: String,
    val body: String,
    val createdAt: String,
    val deliveredAt: String? = null,
    val readAt: String? = null,
    val attachment: MessageAttachmentDto? = null,
) {
    fun toChatMessage(
        activeUserExternalId: String,
        backendBaseUrl: String? = null,
        authToken: String? = null,
    ): ChatMessage = ChatMessage(
        id = id,
        direction = if (senderExternalId == activeUserExternalId) {
            MessageDirection.Outgoing
        } else {
            MessageDirection.Incoming
        },
        kind = when (kind.lowercase()) {
            "aigc" -> MessageKind.Aigc
            else -> MessageKind.Text
        },
        body = body,
        createdAt = createdAt,
        deliveredAt = deliveredAt,
        readAt = readAt,
        attachment = attachment?.toMessageAttachment(backendBaseUrl = backendBaseUrl, authToken = authToken),
    )
}

@Serializable
data class SendDirectImageMessageRequestDto(
    val recipientExternalId: String,
    val clientMessageId: String? = null,
    val body: String,
    val contentType: String,
    val imageBase64: String,
)

@Serializable
data class SendDirectMessageResultDto(
    val conversationId: String,
    val recipientExternalId: String,
    val recipientUnreadCount: Int,
    val message: MessageRecordDto,
)

@Serializable
data class ConversationSummaryDto(
    val conversationId: String,
    val contact: ContactProfileDto,
    val unreadCount: Int,
    val lastMessage: MessageRecordDto? = null,
)

@Serializable
data class DevSessionResponseDto(
    val token: String,
    val expiresAt: String,
    val user: BackendUserDto,
)

@Serializable
data class AuthResponseDto(
    val token: String,
    val expiresAt: String,
    val user: BackendUserDto,
)

@Serializable
data class RegisterRequestDto(
    val username: String,
    val password: String,
    val displayName: String,
)

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String,
)

@Serializable
data class UserSearchResultDto(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarText: String,
    val contactStatus: String,
)

@Serializable
data class FriendRequestBodyDto(
    val toUserId: String,
)

@Serializable
data class FriendRequestViewDto(
    val id: String,
    val fromUser: BackendUserDto,
    val toUserId: String,
    val toUserExternalId: String,
    val status: String,
    val createdAt: String,
)

@Serializable
data class BootstrapBundleDto(
    val user: BackendUserDto,
    val contacts: List<ContactProfileDto>,
    val conversations: List<ConversationSummaryDto>,
) {
    fun toBootstrapState(
        activeUserExternalId: String,
        backendBaseUrl: String? = null,
        authToken: String? = null,
    ): ImBootstrapState = ImBootstrapState(
        user = user,
        contacts = contacts.map { contact ->
            Contact(
                id = contact.externalId,
                nickname = contact.displayName,
                title = contact.title,
                avatarText = contact.avatarText,
                addedAt = contact.addedAt,
                isOnline = false,
            )
        },
        conversations = conversations.map { conversation ->
            Conversation(
                id = conversation.conversationId,
                contactId = conversation.contact.externalId,
                contactName = conversation.contact.displayName,
                contactTitle = conversation.contact.title,
                avatarText = conversation.contact.avatarText,
                lastMessage = conversation.lastMessage?.summaryText().orEmpty(),
                lastTimestamp = conversation.lastMessage?.createdAt ?: conversation.contact.addedAt,
                unreadCount = conversation.unreadCount,
                isOnline = false,
                messages = conversation.lastMessage
                    ?.let {
                        listOf(
                            it.toChatMessage(
                                activeUserExternalId = activeUserExternalId,
                                backendBaseUrl = backendBaseUrl,
                                authToken = authToken,
                            )
                        )
                    }
                    ?: emptyList(),
            )
        },
    )
}

data class ImBootstrapState(
    val user: BackendUserDto,
    val contacts: List<Contact>,
    val conversations: List<Conversation>,
)

@Serializable
data class MessageHistoryPageDto(
    val conversationId: String,
    val messages: List<MessageRecordDto>,
    val hasMore: Boolean,
) {
    fun toChatMessages(
        activeUserExternalId: String,
        backendBaseUrl: String? = null,
        authToken: String? = null,
    ): List<ChatMessage> = messages.map {
        it.toChatMessage(
            activeUserExternalId = activeUserExternalId,
            backendBaseUrl = backendBaseUrl,
            authToken = authToken,
        )
    }
}

private fun MessageAttachmentDto.toMessageAttachment(
    backendBaseUrl: String?,
    authToken: String?,
): MessageAttachment {
    val resolvedPreview = if (backendBaseUrl.isNullOrBlank() || fetchPath.startsWith("http")) {
        fetchPath
    } else {
        val normalizedBase = if (backendBaseUrl.endsWith("/")) backendBaseUrl.dropLast(1) else backendBaseUrl
        "$normalizedBase$fetchPath"
    }
    return MessageAttachment(
        type = when (type.lowercase()) {
            "video" -> AttachmentType.Video
            else -> AttachmentType.Image
        },
        preview = resolvedPreview,
        authToken = authToken,
    )
}

private fun MessageRecordDto.summaryText(): String = when {
    body.isNotBlank() -> body
    attachment?.type?.lowercase() == "video" -> "Sent a video"
    attachment != null -> "Sent an image"
    else -> ""
}

sealed interface ImGatewayEvent {
    @Serializable
    data class SessionRegistered(
        val connectionId: String,
        val activeConnections: Int,
        val user: BackendUserDto,
    ) : ImGatewayEvent

    @Serializable
    data class Pong(val at: String) : ImGatewayEvent

    @Serializable
    data class MessageSent(
        val conversationId: String,
        val message: MessageRecordDto,
    ) : ImGatewayEvent

    @Serializable
    data class MessageReceived(
        val conversationId: String,
        val unreadCount: Int,
        val message: MessageRecordDto,
    ) : ImGatewayEvent

    @Serializable
    data class MessageDelivered(
        val conversationId: String,
        val messageId: String,
        val recipientExternalId: String,
        val deliveredAt: String,
    ) : ImGatewayEvent

    @Serializable
    data class MessageRead(
        val conversationId: String,
        val messageId: String,
        val readerExternalId: String,
        val unreadCount: Int,
        val readAt: String,
    ) : ImGatewayEvent

    @Serializable
    data class Error(
        val code: String,
        val message: String,
    ) : ImGatewayEvent

    @Serializable
    data class FriendRequestReceived(
        val requestId: String,
        val fromUser: BackendUserDto,
    ) : ImGatewayEvent

    @Serializable
    data class FriendRequestAccepted(
        val requestId: String,
        val byUser: BackendUserDto,
    ) : ImGatewayEvent

    @Serializable
    data class FriendRequestRejected(
        val requestId: String,
        val byUserId: String,
    ) : ImGatewayEvent
}

object ImGatewayEventParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(payload: String): ImGatewayEvent {
        val element = json.parseToJsonElement(payload) as JsonObject
        return when (element["type"]?.jsonPrimitive?.content) {
            "session.registered" -> json.decodeFromJsonElement<ImGatewayEvent.SessionRegistered>(element)
            "pong" -> json.decodeFromJsonElement<ImGatewayEvent.Pong>(element)
            "message.sent" -> json.decodeFromJsonElement<ImGatewayEvent.MessageSent>(element)
            "message.received" -> json.decodeFromJsonElement<ImGatewayEvent.MessageReceived>(element)
            "message.delivered" -> json.decodeFromJsonElement<ImGatewayEvent.MessageDelivered>(element)
            "message.read" -> json.decodeFromJsonElement<ImGatewayEvent.MessageRead>(element)
            "error" -> json.decodeFromJsonElement<ImGatewayEvent.Error>(element)
            "friend_request.received" -> json.decodeFromJsonElement<ImGatewayEvent.FriendRequestReceived>(element)
            "friend_request.accepted" -> json.decodeFromJsonElement<ImGatewayEvent.FriendRequestAccepted>(element)
            "friend_request.rejected" -> json.decodeFromJsonElement<ImGatewayEvent.FriendRequestRejected>(element)
            else -> throw IllegalArgumentException("Unsupported gateway event type")
        }
    }
}
