package com.gkim.im.android.data.repository

import com.gkim.im.android.data.remote.im.AuthResponseDto
import com.gkim.im.android.data.remote.im.BackendUserDto
import com.gkim.im.android.data.remote.im.BootstrapBundleDto
import com.gkim.im.android.data.remote.im.DevSessionResponseDto
import com.gkim.im.android.data.remote.im.FriendRequestViewDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.remote.im.MessageHistoryPageDto
import com.gkim.im.android.data.remote.im.SendDirectImageMessageRequestDto
import com.gkim.im.android.data.remote.im.SendDirectMessageResultDto
import com.gkim.im.android.data.remote.im.UserSearchResultDto
import com.gkim.im.android.data.remote.realtime.RealtimeGateway
import com.gkim.im.android.testing.FakePreferencesStore
import com.gkim.im.android.testing.FakeRuntimeSessionStore
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
class RepositoryBootstrapTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `dev session bootstrap runs post-bootstrap hook after applying bootstrap state`() =
        runTest(mainDispatcherRule.dispatcher) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val clock = TickClock()
            val hook = RecordingPostBootstrapHook(clock)
            val backendClient = FakeBootstrapBackendClient(clock)
            val repository = LiveMessagingRepository(
                backendClient = backendClient,
                realtimeGateway = FakeRealtimeGateway(),
                sessionStore = FakeRuntimeSessionStore(),
                preferencesStore = FakePreferencesStore(),
                fallbackRepository = InMemoryMessagingRepository(emptyList()),
                onBootstrapLoaded = { hook.invoke() },
                dispatcher = dispatcher,
            )

            advanceUntilIdle()

            assertEquals(MessagingIntegrationPhase.Ready, repository.integrationState.value.phase)
            assertEquals(1, hook.invocations)
            assertNotNull(backendClient.lastBootstrapAt)
            assertNotNull(hook.invokedAt)
            assertTrue(
                "hook fires strictly after bootstrap",
                hook.invokedAt!! > backendClient.lastBootstrapAt!!,
            )
        }

    @Test
    fun `authenticated session bootstrap runs post-bootstrap hook after applying bootstrap state`() =
        runTest(mainDispatcherRule.dispatcher) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val clock = TickClock()
            val hook = RecordingPostBootstrapHook(clock)
            val backendClient = FakeBootstrapBackendClient(clock)
            val sessionStore = FakeRuntimeSessionStore().apply {
                token = "stored-token"
                baseUrl = "https://api.example.com"
                username = "nox-prod"
            }
            val repository = LiveMessagingRepository(
                backendClient = backendClient,
                realtimeGateway = FakeRealtimeGateway(),
                sessionStore = sessionStore,
                preferencesStore = FakePreferencesStore(),
                fallbackRepository = InMemoryMessagingRepository(emptyList()),
                onBootstrapLoaded = { hook.invoke() },
                dispatcher = dispatcher,
            )

            advanceUntilIdle()

            assertEquals(MessagingIntegrationPhase.Ready, repository.integrationState.value.phase)
            assertEquals(listOf("stored-token"), backendClient.bootstrapTokens)
            assertEquals(1, hook.invocations)
            assertTrue(
                "hook fires strictly after bootstrap",
                hook.invokedAt!! > backendClient.lastBootstrapAt!!,
            )
        }

    @Test
    fun `bootstrap still reaches Ready when post-bootstrap hook throws`() =
        runTest(mainDispatcherRule.dispatcher) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val clock = TickClock()
            val backendClient = FakeBootstrapBackendClient(clock)
            val repository = LiveMessagingRepository(
                backendClient = backendClient,
                realtimeGateway = FakeRealtimeGateway(),
                sessionStore = FakeRuntimeSessionStore(),
                preferencesStore = FakePreferencesStore(),
                fallbackRepository = InMemoryMessagingRepository(emptyList()),
                onBootstrapLoaded = { throw RuntimeException("hook boom") },
                dispatcher = dispatcher,
            )

            advanceUntilIdle()

            assertEquals(MessagingIntegrationPhase.Ready, repository.integrationState.value.phase)
        }

    @Test
    fun `bootstrap still succeeds when no post-bootstrap hook is registered`() =
        runTest(mainDispatcherRule.dispatcher) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val clock = TickClock()
            val backendClient = FakeBootstrapBackendClient(clock)
            val repository = LiveMessagingRepository(
                backendClient = backendClient,
                realtimeGateway = FakeRealtimeGateway(),
                sessionStore = FakeRuntimeSessionStore(),
                preferencesStore = FakePreferencesStore(),
                fallbackRepository = InMemoryMessagingRepository(emptyList()),
                dispatcher = dispatcher,
            )

            advanceUntilIdle()

            assertEquals(MessagingIntegrationPhase.Ready, repository.integrationState.value.phase)
        }

    private class TickClock {
        private var tick = 0L
        fun next(): Long {
            tick += 1
            return tick
        }
    }

    private class RecordingPostBootstrapHook(private val clock: TickClock) {
        var invocations = 0
            private set
        var invokedAt: Long? = null
            private set

        fun invoke() {
            invocations += 1
            invokedAt = clock.next()
        }
    }

    private class FakeBootstrapBackendClient(private val clock: TickClock) : ImBackendClient {
        val bootstrapTokens = mutableListOf<String>()
        var lastBootstrapAt: Long? = null
            private set

        override suspend fun issueDevSession(
            baseUrl: String,
            externalId: String,
        ): DevSessionResponseDto = DevSessionResponseDto(
            token = "dev-token",
            expiresAt = "2026-05-01T00:00:00Z",
            user = backendUser(externalId),
        )

        override suspend fun register(
            baseUrl: String,
            username: String,
            password: String,
            displayName: String,
        ): AuthResponseDto = AuthResponseDto(
            token = "reg-token",
            expiresAt = "2026-05-01T00:00:00Z",
            user = backendUser(username, displayName),
        )

        override suspend fun login(
            baseUrl: String,
            username: String,
            password: String,
        ): AuthResponseDto = AuthResponseDto(
            token = "login-token",
            expiresAt = "2026-05-01T00:00:00Z",
            user = backendUser(username, username),
        )

        override suspend fun searchUsers(
            baseUrl: String,
            token: String,
            query: String,
        ): List<UserSearchResultDto> = emptyList()

        override suspend fun sendFriendRequest(
            baseUrl: String,
            token: String,
            toUserId: String,
        ): FriendRequestViewDto = placeholderFriendRequest(toUserId, status = "pending")

        override suspend fun listFriendRequests(
            baseUrl: String,
            token: String,
        ): List<FriendRequestViewDto> = emptyList()

        override suspend fun acceptFriendRequest(
            baseUrl: String,
            token: String,
            requestId: String,
        ): FriendRequestViewDto = placeholderFriendRequest("placeholder", status = "accepted")

        override suspend fun rejectFriendRequest(
            baseUrl: String,
            token: String,
            requestId: String,
        ): FriendRequestViewDto = placeholderFriendRequest("placeholder", status = "rejected")

        override suspend fun loadBootstrap(
            baseUrl: String,
            token: String,
        ): BootstrapBundleDto {
            bootstrapTokens += token
            lastBootstrapAt = clock.next()
            return BootstrapBundleDto(
                user = backendUser("nox-dev"),
                contacts = emptyList(),
                conversations = emptyList(),
            )
        }

        override suspend fun sendDirectImageMessage(
            baseUrl: String,
            token: String,
            request: SendDirectImageMessageRequestDto,
        ): SendDirectMessageResultDto {
            throw UnsupportedOperationException("image send not exercised in bootstrap test")
        }

        override suspend fun loadHistory(
            baseUrl: String,
            token: String,
            conversationId: String,
            limit: Int,
            before: String?,
        ): MessageHistoryPageDto = MessageHistoryPageDto(
            conversationId = conversationId,
            messages = emptyList(),
            hasMore = false,
        )

        private fun backendUser(
            externalId: String,
            displayName: String = externalId,
        ): BackendUserDto = BackendUserDto(
            id = "user-$externalId",
            externalId = externalId,
            displayName = displayName,
            title = "",
            avatarText = displayName.take(2).uppercase(),
        )

        private fun placeholderFriendRequest(
            toUserId: String,
            status: String,
        ): FriendRequestViewDto = FriendRequestViewDto(
            id = "req-1",
            fromUser = backendUser("nox-dev"),
            toUserId = toUserId,
            toUserExternalId = toUserId,
            status = status,
            createdAt = "2026-04-22T00:00:00Z",
        )
    }

    private class FakeRealtimeGateway : RealtimeGateway {
        private val connectedState = MutableStateFlow(false)
        private val failureState = MutableStateFlow<String?>(null)
        private val eventFlow = MutableSharedFlow<ImGatewayEvent>(extraBufferCapacity = 8)

        override val isConnected: StateFlow<Boolean> = connectedState.asStateFlow()
        override val lastFailure: StateFlow<String?> = failureState.asStateFlow()
        override val events: SharedFlow<ImGatewayEvent> = eventFlow.asSharedFlow()

        override fun connect(token: String?, endpointOverride: String?) {
            connectedState.value = true
            eventFlow.tryEmit(
                ImGatewayEvent.SessionRegistered(
                    connectionId = "ws-1",
                    activeConnections = 1,
                    user = BackendUserDto(
                        id = "user-nox-dev",
                        externalId = "nox-dev",
                        displayName = "Nox Dev",
                        title = "",
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
    }
}
