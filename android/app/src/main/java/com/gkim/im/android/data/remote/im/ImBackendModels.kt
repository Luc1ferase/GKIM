package com.gkim.im.android.data.remote.im

import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.core.model.Conversation
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
) {
    fun toChatMessage(activeUserExternalId: String): ChatMessage = ChatMessage(
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
    )
}

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
data class BootstrapBundleDto(
    val user: BackendUserDto,
    val contacts: List<ContactProfileDto>,
    val conversations: List<ConversationSummaryDto>,
) {
    fun toBootstrapState(activeUserExternalId: String): ImBootstrapState = ImBootstrapState(
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
                lastMessage = conversation.lastMessage?.body ?: "",
                lastTimestamp = conversation.lastMessage?.createdAt ?: conversation.contact.addedAt,
                unreadCount = conversation.unreadCount,
                isOnline = false,
                messages = conversation.lastMessage
                    ?.let { listOf(it.toChatMessage(activeUserExternalId)) }
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
    fun toChatMessages(activeUserExternalId: String): List<ChatMessage> =
        messages.map { it.toChatMessage(activeUserExternalId) }
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
            else -> throw IllegalArgumentException("Unsupported gateway event type")
        }
    }
}
