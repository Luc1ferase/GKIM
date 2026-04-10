package com.gkim.im.android.data.repository

import com.gkim.im.android.data.remote.im.BackendUserDto
import com.gkim.im.android.data.remote.im.BootstrapBundleDto
import com.gkim.im.android.data.remote.im.ContactProfileDto
import com.gkim.im.android.data.remote.im.ConversationSummaryDto
import com.gkim.im.android.data.remote.im.DevSessionResponseDto
import com.gkim.im.android.data.remote.im.AuthResponseDto
import com.gkim.im.android.data.remote.im.FriendRequestViewDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.remote.im.MessageHistoryPageDto
import com.gkim.im.android.data.remote.im.MessageRecordDto
import com.gkim.im.android.data.remote.im.UserSearchResultDto
import com.gkim.im.android.data.remote.realtime.RealtimeGateway
import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.testing.FakePreferencesStore
import com.gkim.im.android.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveMessagingRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `repository authenticates bootstraps conversations and connects realtime`() = runTest(mainDispatcherRule.dispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val backendClient = FakeImBackendClient()
        val realtimeGateway = FakeRealtimeGateway()
        val repository = LiveMessagingRepository(
            backendClient = backendClient,
            realtimeGateway = realtimeGateway,
            preferencesStore = FakePreferencesStore(),
            fallbackRepository = InMemoryMessagingRepository(seedConversations),
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        assertEquals(MessagingIntegrationPhase.Ready, repository.integrationState.value.phase)
        assertEquals("nox-dev", repository.integrationState.value.activeUserExternalId)
        assertEquals("session-token-1", backendClient.issuedToken)
        assertEquals("session-token-1", realtimeGateway.connectedToken)
        assertEquals("ws://127.0.0.1:18080/ws", realtimeGateway.connectedEndpoint)
        assertEquals(1, repository.conversations.value.size)
        assertEquals("conversation-1", repository.conversations.value.single().id)
        assertEquals("leo-vance", repository.conversations.value.single().contactId)
        assertEquals("Hello Nox", repository.conversations.value.single().lastMessage)
    }

    @Test
    fun `repository loads history into selected conversation`() = runTest(mainDispatcherRule.dispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val backendClient = FakeImBackendClient()
        val realtimeGateway = FakeRealtimeGateway()
        val repository = LiveMessagingRepository(
            backendClient = backendClient,
            realtimeGateway = realtimeGateway,
            preferencesStore = FakePreferencesStore(),
            fallbackRepository = InMemoryMessagingRepository(seedConversations),
            dispatcher = dispatcher,
        )

        advanceUntilIdle()
        repository.loadConversationHistory("conversation-1")
        advanceUntilIdle()

        val conversation = repository.conversations.value.single()
        assertEquals(2, conversation.messages.size)
        assertEquals("First outbound", conversation.messages.first().body)
        assertEquals("Inbound reply", conversation.messages.last().body)
        assertEquals("2026-04-08T09:02:00Z", conversation.lastTimestamp)
    }

    @Test
    fun `repository surfaces history failure through integration state`() = runTest(mainDispatcherRule.dispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val backendClient = FakeImBackendClient(
            historyFailure = IllegalStateException("history offline")
        )
        val realtimeGateway = FakeRealtimeGateway()
        val repository = LiveMessagingRepository(
            backendClient = backendClient,
            realtimeGateway = realtimeGateway,
            preferencesStore = FakePreferencesStore(),
            fallbackRepository = InMemoryMessagingRepository(seedConversations),
            dispatcher = dispatcher,
        )

        advanceUntilIdle()
        repository.loadConversationHistory("conversation-1")
        advanceUntilIdle()

        assertEquals(MessagingIntegrationPhase.Error, repository.integrationState.value.phase)
        assertEquals("history offline", repository.integrationState.value.message)
        assertEquals(1, repository.conversations.value.single().messages.size)
        assertEquals("Hello Nox", repository.conversations.value.single().lastMessage)
    }

    @Test
    fun `repository surfaces bootstrap failure through integration state`() = runTest(mainDispatcherRule.dispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val backendClient = FakeImBackendClient(
            bootstrapFailure = IllegalStateException("bootstrap offline")
        )
        val realtimeGateway = FakeRealtimeGateway()
        val repository = LiveMessagingRepository(
            backendClient = backendClient,
            realtimeGateway = realtimeGateway,
            preferencesStore = FakePreferencesStore(),
            fallbackRepository = InMemoryMessagingRepository(seedConversations),
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        assertEquals(MessagingIntegrationPhase.Error, repository.integrationState.value.phase)
        assertEquals("bootstrap offline", repository.integrationState.value.message)
        assertTrue(repository.conversations.value.isEmpty())
    }

    @Test
    fun `repository skips history fetch for fallback-only conversations`() = runTest(mainDispatcherRule.dispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val backendClient = FakeImBackendClient()
        val realtimeGateway = FakeRealtimeGateway()
        val repository = LiveMessagingRepository(
            backendClient = backendClient,
            realtimeGateway = realtimeGateway,
            preferencesStore = FakePreferencesStore(),
            fallbackRepository = InMemoryMessagingRepository(seedConversations),
            dispatcher = dispatcher,
        )

        advanceUntilIdle()
        val fallbackConversation = repository.ensureConversation(
            Contact(
                id = "aria-thorne",
                nickname = "Aria Thorne",
                title = "Prompt Architect",
                avatarText = "AT",
                addedAt = "2026-04-08T09:04:00Z",
                isOnline = true,
            )
        )

        repository.loadConversationHistory(fallbackConversation.id)
        advanceUntilIdle()

        assertTrue(backendClient.historyRequests.none { it == fallbackConversation.id })
        assertEquals(MessagingIntegrationPhase.Ready, repository.integrationState.value.phase)
    }

    @Test
    fun `repository reconciles realtime receive delivered and read events into conversation state`() = runTest(mainDispatcherRule.dispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val backendClient = FakeImBackendClient()
        val realtimeGateway = FakeRealtimeGateway()
        val repository = LiveMessagingRepository(
            backendClient = backendClient,
            realtimeGateway = realtimeGateway,
            preferencesStore = FakePreferencesStore(),
            fallbackRepository = InMemoryMessagingRepository(seedConversations),
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        realtimeGateway.emit(
            ImGatewayEvent.MessageReceived(
                conversationId = "conversation-1",
                unreadCount = 3,
                message = MessageRecordDto(
                    id = "message-live",
                    conversationId = "conversation-1",
                    senderUserId = "user-leo",
                    senderExternalId = "leo-vance",
                    kind = "text",
                    body = "Realtime hello",
                    createdAt = "2026-04-08T09:03:00Z",
                    deliveredAt = null,
                    readAt = null,
                ),
            )
        )
        advanceUntilIdle()

        var conversation = repository.conversations.value.single()
        assertEquals(3, conversation.unreadCount)
        assertEquals("Realtime hello", conversation.lastMessage)
        assertEquals("message-live", conversation.messages.last().id)

        realtimeGateway.emit(
            ImGatewayEvent.MessageDelivered(
                conversationId = "conversation-1",
                messageId = "message-live",
                recipientExternalId = "nox-dev",
                deliveredAt = "2026-04-08T09:03:01Z",
            )
        )
        advanceUntilIdle()

        conversation = repository.conversations.value.single()
        assertEquals("2026-04-08T09:03:01Z", conversation.messages.last().deliveredAt)

        realtimeGateway.emit(
            ImGatewayEvent.MessageRead(
                conversationId = "conversation-1",
                messageId = "message-live",
                readerExternalId = "nox-dev",
                unreadCount = 0,
                readAt = "2026-04-08T09:03:05Z",
            )
        )
        advanceUntilIdle()

        conversation = repository.conversations.value.single()
        assertEquals(0, conversation.unreadCount)
        assertEquals("2026-04-08T09:03:05Z", conversation.messages.last().readAt)
    }

    private class FakeImBackendClient(
        private val sessionFailure: Throwable? = null,
        private val bootstrapFailure: Throwable? = null,
        private val historyFailure: Throwable? = null,
    ) : ImBackendClient {
        var issuedToken: String? = null
        val historyRequests = mutableListOf<String>()

        override suspend fun issueDevSession(baseUrl: String, externalId: String): DevSessionResponseDto {
            sessionFailure?.let { throw it }
            issuedToken = "session-token-1"
            return DevSessionResponseDto(
                token = "session-token-1",
                expiresAt = "2026-04-15T10:00:00Z",
                user = BackendUserDto(
                    id = "user-nox",
                    externalId = "nox-dev",
                    displayName = "Nox Dev",
                    title = "IM Milestone Owner",
                    avatarText = "NX",
                ),
            )
        }

        override suspend fun register(
            baseUrl: String,
            username: String,
            password: String,
            displayName: String,
        ): AuthResponseDto = authResponse(username, displayName)

        override suspend fun login(
            baseUrl: String,
            username: String,
            password: String,
        ): AuthResponseDto = authResponse(username, username)

        override suspend fun searchUsers(
            baseUrl: String,
            token: String,
            query: String,
        ): List<UserSearchResultDto> = emptyList()

        override suspend fun sendFriendRequest(
            baseUrl: String,
            token: String,
            toUserId: String,
        ): FriendRequestViewDto = friendRequestView(toUserId = toUserId)

        override suspend fun listFriendRequests(
            baseUrl: String,
            token: String,
        ): List<FriendRequestViewDto> = emptyList()

        override suspend fun acceptFriendRequest(
            baseUrl: String,
            token: String,
            requestId: String,
        ): FriendRequestViewDto = friendRequestView(status = "accepted")

        override suspend fun rejectFriendRequest(
            baseUrl: String,
            token: String,
            requestId: String,
        ): FriendRequestViewDto = friendRequestView(status = "rejected")

        override suspend fun loadBootstrap(baseUrl: String, token: String): BootstrapBundleDto {
            bootstrapFailure?.let { throw it }
            return BootstrapBundleDto(
                user = BackendUserDto(
                    id = "user-nox",
                    externalId = "nox-dev",
                    displayName = "Nox Dev",
                    title = "IM Milestone Owner",
                    avatarText = "NX",
                ),
                contacts = listOf(
                    ContactProfileDto(
                        userId = "user-leo",
                        externalId = "leo-vance",
                        displayName = "Leo Vance",
                        title = "Realtime Systems",
                        avatarText = "LV",
                        addedAt = "2026-04-08T08:58:00Z",
                    )
                ),
                conversations = listOf(
                    ConversationSummaryDto(
                        conversationId = "conversation-1",
                        contact = ContactProfileDto(
                            userId = "user-leo",
                            externalId = "leo-vance",
                            displayName = "Leo Vance",
                            title = "Realtime Systems",
                            avatarText = "LV",
                            addedAt = "2026-04-08T08:58:00Z",
                        ),
                        unreadCount = 2,
                        lastMessage = MessageRecordDto(
                            id = "message-1",
                            conversationId = "conversation-1",
                            senderUserId = "user-leo",
                            senderExternalId = "leo-vance",
                            kind = "text",
                            body = "Hello Nox",
                            createdAt = "2026-04-08T09:00:00Z",
                            deliveredAt = "2026-04-08T09:00:01Z",
                            readAt = null,
                        ),
                    ),
                )
            )
        }

        override suspend fun loadHistory(
            baseUrl: String,
            token: String,
            conversationId: String,
            limit: Int,
            before: String?,
        ): MessageHistoryPageDto {
            historyFailure?.let { throw it }
            historyRequests += conversationId
            return MessageHistoryPageDto(
                conversationId = conversationId,
                hasMore = false,
                messages = listOf(
                    MessageRecordDto(
                        id = "message-1",
                        conversationId = conversationId,
                        senderUserId = "user-nox",
                        senderExternalId = "nox-dev",
                        kind = "text",
                        body = "First outbound",
                        createdAt = "2026-04-08T09:01:00Z",
                        deliveredAt = "2026-04-08T09:01:01Z",
                        readAt = "2026-04-08T09:01:03Z",
                    ),
                    MessageRecordDto(
                        id = "message-2",
                        conversationId = conversationId,
                        senderUserId = "user-leo",
                        senderExternalId = "leo-vance",
                        kind = "text",
                        body = "Inbound reply",
                        createdAt = "2026-04-08T09:02:00Z",
                        deliveredAt = null,
                        readAt = null,
                    ),
                ),
            )
        }

        private fun authResponse(externalId: String, displayName: String): AuthResponseDto {
            return AuthResponseDto(
                token = "auth-token-1",
                expiresAt = "2026-04-15T10:00:00Z",
                user = BackendUserDto(
                    id = "user-$externalId",
                    externalId = externalId,
                    displayName = displayName,
                    title = "",
                    avatarText = externalId.take(2).uppercase(),
                ),
            )
        }

        private fun friendRequestView(
            toUserId: String = "user-leo",
            status: String = "pending",
        ): FriendRequestViewDto {
            return FriendRequestViewDto(
                id = "request-1",
                fromUser = BackendUserDto(
                    id = "user-nox",
                    externalId = "nox-dev",
                    displayName = "Nox Dev",
                    title = "IM Milestone Owner",
                    avatarText = "NX",
                ),
                toUserId = toUserId,
                toUserExternalId = "leo-vance",
                status = status,
                createdAt = "2026-04-08T09:00:00Z",
            )
        }
    }

    private class FakeRealtimeGateway : RealtimeGateway {
        private val connectedState = MutableStateFlow(false)
        private val failureState = MutableStateFlow<String?>(null)
        private val eventFlow = MutableSharedFlow<ImGatewayEvent>(extraBufferCapacity = 8)

        var connectedToken: String? = null
        var connectedEndpoint: String? = null

        override val isConnected: StateFlow<Boolean> = connectedState.asStateFlow()
        override val lastFailure: StateFlow<String?> = failureState.asStateFlow()
        override val events: SharedFlow<ImGatewayEvent> = eventFlow.asSharedFlow()

        override fun connect(token: String?, endpointOverride: String?) {
            connectedToken = token
            connectedEndpoint = endpointOverride
            connectedState.value = true
            eventFlow.tryEmit(
                ImGatewayEvent.SessionRegistered(
                    connectionId = "ws-77",
                    activeConnections = 1,
                    user = BackendUserDto(
                        id = "user-nox",
                        externalId = "nox-dev",
                        displayName = "Nox Dev",
                        title = "IM Milestone Owner",
                        avatarText = "NX",
                    ),
                )
            )
        }

        override fun send(payload: String): Boolean = true

        override fun sendMessage(
            recipientExternalId: String,
            clientMessageId: String?,
            body: String,
        ): Boolean = true

        override fun markRead(conversationId: String, messageId: String): Boolean = true

        override fun disconnect() {
            connectedState.value = false
        }

        fun emit(event: ImGatewayEvent) {
            eventFlow.tryEmit(event)
        }
    }
}
