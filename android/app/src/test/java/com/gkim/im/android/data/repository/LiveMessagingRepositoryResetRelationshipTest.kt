package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.data.remote.im.AuthResponseDto
import com.gkim.im.android.data.remote.im.BackendUserDto
import com.gkim.im.android.data.remote.im.BootstrapBundleDto
import com.gkim.im.android.data.remote.im.ContactProfileDto
import com.gkim.im.android.data.remote.im.ConversationSummaryDto
import com.gkim.im.android.data.remote.im.DevSessionResponseDto
import com.gkim.im.android.data.remote.im.FriendRequestViewDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.remote.im.MessageHistoryPageDto
import com.gkim.im.android.data.remote.im.RelationshipResetResponseDto
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * §4.1 verification — `LiveMessagingRepository.resetRelationship(characterId)` calls
 * `POST /api/relationships/{characterId}/reset`, removes any conversation with
 * `companionCardId == characterId` from the local cache on success, and remaps wire failures
 * to stable error codes (`character_not_available`, `network_failure`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveMessagingRepositoryResetRelationshipTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `successful reset removes only conversations matching the character id`() =
        runTest(mainDispatcherRule.dispatcher) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val backendClient = FakeBackend()
            val repository = buildRepository(backendClient, dispatcher)
            advanceUntilIdle()

            repository.ensureConversation(contactFor("char-A"), companionCardId = "char-A")
            repository.ensureConversation(contactFor("char-B"), companionCardId = "char-B")
            repository.ensureConversation(contactFor("char-C"), companionCardId = "char-C")

            val result = repository.resetRelationship("char-B")
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            val remainingCardIds = repository.conversations.value.mapNotNull { it.companionCardId }.toSet()
            assertEquals(setOf("char-A", "char-C"), remainingCardIds)
            assertEquals(listOf("char-B"), backendClient.resetCalls)
        }

    @Test
    fun `successful reset leaves conversations without companionCardId untouched`() =
        runTest(mainDispatcherRule.dispatcher) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val backendClient = FakeBackend()
            val repository = buildRepository(backendClient, dispatcher)
            advanceUntilIdle()

            // Bootstrap seed has a peer conversation (no companionCardId). Resetting
            // a companion that does not exist locally should not touch the peer.
            val baselineCount = repository.conversations.value.size
            repository.ensureConversation(contactFor("char-A"), companionCardId = "char-A")

            repository.resetRelationship("char-A")
            advanceUntilIdle()

            assertEquals(baselineCount, repository.conversations.value.size)
            assertEquals(0, repository.conversations.value.count { it.companionCardId == "char-A" })
        }

    @Test
    fun `403 character_not_available maps to character_not_available code`() =
        runTest(mainDispatcherRule.dispatcher) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val backendClient = FakeBackend(
                resetFailure = httpException(403, "{\"error\":\"character_not_available\",\"message\":\"x\"}"),
            )
            val repository = buildRepository(backendClient, dispatcher)
            advanceUntilIdle()
            repository.ensureConversation(contactFor("char-A"), companionCardId = "char-A")

            val result = repository.resetRelationship("char-A")
            advanceUntilIdle()

            assertTrue(result.isFailure)
            assertEquals("character_not_available", result.exceptionOrNull()?.message)
            // Local cache untouched on failure.
            assertEquals(1, repository.conversations.value.count { it.companionCardId == "char-A" })
        }

    @Test
    fun `IOException maps to network_failure and leaves cache intact`() =
        runTest(mainDispatcherRule.dispatcher) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val backendClient = FakeBackend(resetFailure = IOException("boom"))
            val repository = buildRepository(backendClient, dispatcher)
            advanceUntilIdle()
            repository.ensureConversation(contactFor("char-A"), companionCardId = "char-A")

            val result = repository.resetRelationship("char-A")
            advanceUntilIdle()

            assertTrue(result.isFailure)
            assertEquals("network_failure", result.exceptionOrNull()?.message)
            assertEquals(1, repository.conversations.value.count { it.companionCardId == "char-A" })
        }

    @Test
    fun `403 with non-character_not_available body maps to network_failure`() =
        runTest(mainDispatcherRule.dispatcher) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val backendClient = FakeBackend(
                resetFailure = httpException(403, "{\"error\":\"forbidden\",\"message\":\"x\"}"),
            )
            val repository = buildRepository(backendClient, dispatcher)
            advanceUntilIdle()

            val result = repository.resetRelationship("char-A")
            advanceUntilIdle()

            assertTrue(result.isFailure)
            assertEquals("network_failure", result.exceptionOrNull()?.message)
        }

    private fun contactFor(id: String): Contact = Contact(
        id = id,
        nickname = id,
        title = "Companion",
        avatarText = id.take(2).uppercase(),
        addedAt = "2026-04-26T12:00:00Z",
        isOnline = true,
    )

    private fun httpException(code: Int, body: String): HttpException {
        val errBody = body.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Unit>(code, errBody))
    }

    private fun buildRepository(
        backendClient: FakeBackend,
        dispatcher: kotlinx.coroutines.CoroutineDispatcher,
    ): LiveMessagingRepository = LiveMessagingRepository(
        backendClient = backendClient,
        realtimeGateway = FakeRealtimeGateway(),
        sessionStore = FakeRuntimeSessionStore(),
        preferencesStore = FakePreferencesStore(),
        fallbackRepository = InMemoryMessagingRepository(emptyList()),
        dispatcher = dispatcher,
    )

    private class FakeBackend(
        private val resetFailure: Throwable? = null,
    ) : ImBackendClient {
        val resetCalls = mutableListOf<String>()

        override suspend fun resetRelationship(
            baseUrl: String,
            token: String,
            characterId: String,
        ): RelationshipResetResponseDto {
            resetFailure?.let { throw it }
            resetCalls += characterId
            return RelationshipResetResponseDto(ok = true)
        }

        override suspend fun issueDevSession(baseUrl: String, externalId: String): DevSessionResponseDto =
            DevSessionResponseDto(
                token = "session-token-1",
                expiresAt = "2026-04-30T00:00:00Z",
                user = BackendUserDto(
                    id = "user-nox", externalId = "nox-dev", displayName = "Nox",
                    title = "Tester", avatarText = "NX",
                ),
            )

        override suspend fun loadBootstrap(baseUrl: String, token: String): BootstrapBundleDto =
            BootstrapBundleDto(
                user = BackendUserDto(
                    id = "user-nox", externalId = "nox-dev", displayName = "Nox",
                    title = "Tester", avatarText = "NX",
                ),
                contacts = emptyList(),
                conversations = emptyList(),
            )

        override suspend fun register(baseUrl: String, username: String, password: String, displayName: String): AuthResponseDto = error("n/a")
        override suspend fun login(baseUrl: String, username: String, password: String): AuthResponseDto = error("n/a")
        override suspend fun searchUsers(baseUrl: String, token: String, query: String): List<UserSearchResultDto> = emptyList()
        override suspend fun sendFriendRequest(baseUrl: String, token: String, toUserId: String): FriendRequestViewDto = error("n/a")
        override suspend fun listFriendRequests(baseUrl: String, token: String): List<FriendRequestViewDto> = emptyList()
        override suspend fun acceptFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto = error("n/a")
        override suspend fun rejectFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto = error("n/a")
        override suspend fun sendDirectImageMessage(
            baseUrl: String,
            token: String,
            request: SendDirectImageMessageRequestDto,
        ): SendDirectMessageResultDto = error("n/a")
        override suspend fun loadHistory(
            baseUrl: String,
            token: String,
            conversationId: String,
            limit: Int,
            before: String?,
        ): MessageHistoryPageDto = MessageHistoryPageDto(
            conversationId = conversationId,
            hasMore = false,
            messages = emptyList(),
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
        }
        override fun send(payload: String): Boolean = true
        override fun sendMessage(recipientExternalId: String, clientMessageId: String?, body: String): Boolean = true
        override fun markRead(conversationId: String, messageId: String): Boolean = true
        override fun disconnect() = Unit
    }
}
