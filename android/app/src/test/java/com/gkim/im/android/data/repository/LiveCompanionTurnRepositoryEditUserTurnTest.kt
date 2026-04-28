package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageStatus
import com.gkim.im.android.data.remote.im.AuthResponseDto
import com.gkim.im.android.data.remote.im.BootstrapBundleDto
import com.gkim.im.android.data.remote.im.CompanionTurnPendingListDto
import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import com.gkim.im.android.data.remote.im.DevSessionResponseDto
import com.gkim.im.android.data.remote.im.EditUserTurnRequestDto
import com.gkim.im.android.data.remote.im.EditUserTurnResponseDto
import com.gkim.im.android.data.remote.im.FriendRequestViewDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.remote.im.MessageHistoryPageDto
import com.gkim.im.android.data.remote.im.NewUserMessageRecordDto
import com.gkim.im.android.data.remote.im.SendDirectImageMessageRequestDto
import com.gkim.im.android.data.remote.im.SendDirectMessageResultDto
import com.gkim.im.android.data.remote.im.UserSearchResultDto
import com.gkim.im.android.data.remote.realtime.RealtimeGateway
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * §3.1 verification — `LiveCompanionTurnRepository.editUserTurn(...)` calls
 * `POST /api/companion-turns/:conversationId/edit`, projects the response
 * (`EditUserTurnResponseDto`) into the local sibling tree (new user-message variant + new
 * companion-turn variant), and emits the resulting `ChatMessage` updates through the same
 * `activePathByConversation` flow consumed by `ChatViewModel`.
 */
class LiveCompanionTurnRepositoryEditUserTurnTest {

    private val conversationId = "conversation-edit-smoke"
    private val parentMessageId = "msg-original-user-7"

    private fun buildRepo(
        editResponse: ((EditUserTurnRequestDto) -> EditUserTurnResponseDto)? = null,
        scope: kotlinx.coroutines.CoroutineScope,
    ): Pair<LiveCompanionTurnRepository, FakeBackendClient> {
        val backend = FakeBackendClient(editResponse = editResponse)
        val repo = LiveCompanionTurnRepository(
            default = DefaultCompanionTurnRepository(),
            backendClient = backend,
            gateway = FakeGateway(),
            scope = scope,
            baseUrlProvider = { "http://fake" },
            tokenProvider = { "token" },
            clientTurnIdGenerator = { "client-turn-edit-stub" },
            clock = { "2026-04-26T12:00:00Z" },
        )
        return repo to backend
    }

    @Test
    fun `editUserTurn POSTs the right request shape`() = runTest(UnconfinedTestDispatcher()) {
        val (repo, backend) = buildRepo(
            editResponse = { _ -> sampleResponse() },
            scope = backgroundScope,
        )
        val result = repo.editUserTurn(
            conversationId = conversationId,
            parentMessageId = parentMessageId,
            newUserText = "the rewritten text",
            activeCompanionId = "daylight-listener",
            activeLanguage = "en",
        )
        assertTrue(result.isSuccess)
        val captured = backend.editCalls.single()
        assertEquals(parentMessageId, captured.parentMessageId)
        assertEquals("the rewritten text", captured.newUserText)
        assertEquals("daylight-listener", captured.activeCompanionId)
        assertEquals("en", captured.activeLanguage)
        assertEquals("client-turn-edit-stub", captured.clientTurnId)
    }

    @Test
    fun `editUserTurn projects the new user message into the local tree as Outgoing Completed`() =
        runTest(UnconfinedTestDispatcher()) {
            val (repo, _) = buildRepo(editResponse = { sampleResponse() }, scope = backgroundScope)
            repo.editUserTurn(
                conversationId = conversationId,
                parentMessageId = parentMessageId,
                newUserText = "the rewritten text",
                activeCompanionId = "daylight-listener",
                activeLanguage = "en",
            )
            val tree = repo.treeByConversation.value[conversationId]!!
            val newUser = tree.messagesById["user-message-edit-smoke-01"]!!
            assertEquals(MessageDirection.Outgoing, newUser.direction)
            assertEquals(MessageStatus.Completed, newUser.status)
            assertEquals("the rewritten text", newUser.body)
            assertEquals(parentMessageId, newUser.parentMessageId)
        }

    @Test
    fun `editUserTurn applies the companion turn record so the active path advances to the new sibling`() =
        runTest(UnconfinedTestDispatcher()) {
            val (repo, _) = buildRepo(editResponse = { sampleResponse() }, scope = backgroundScope)
            repo.editUserTurn(
                conversationId = conversationId,
                parentMessageId = parentMessageId,
                newUserText = "the rewritten text",
                activeCompanionId = "daylight-listener",
                activeLanguage = "en",
            )
            val activePath = repo.activePathByConversation.value[conversationId]!!
            // Tree contains both the new user message + the new companion turn.
            val newCompanion = activePath.firstOrNull { it.id == "turn-edit-companion-smoke-01" }
            assertNotNull(newCompanion)
            assertEquals("the regenerated reply", newCompanion!!.body)
            // §2.1 projection gives the new companion turn siblingCount = 1 (it's alone in
            // its variantGroup, the user-side variant under variantGroupId
            // variant-group-edit-companion-smoke-01).
            val meta = newCompanion.companionTurnMeta!!
            assertEquals("variant-group-edit-companion-smoke-01", meta.variantGroupId)
            assertEquals(1, meta.siblingCount)
            assertEquals(0, meta.siblingActiveIndex)
        }

    @Test
    fun `editUserTurn surfaces transport failures as Result failure without mutating the tree`() =
        runTest(UnconfinedTestDispatcher()) {
            val (repo, _) = buildRepo(
                editResponse = { throw IOException("boom") },
                scope = backgroundScope,
            )
            val result = repo.editUserTurn(
                conversationId = conversationId,
                parentMessageId = parentMessageId,
                newUserText = "x",
                activeCompanionId = "y",
                activeLanguage = "en",
            )
            assertTrue(result.isFailure)
            assertEquals("boom", result.exceptionOrNull()?.message)
            // No tree state mutated when the call failed.
            assertNull(repo.treeByConversation.value[conversationId])
        }

    @Test
    fun `editUserTurn fails fast with no base url`() = runTest(UnconfinedTestDispatcher()) {
        val backend = FakeBackendClient(editResponse = { sampleResponse() })
        val repo = LiveCompanionTurnRepository(
            default = DefaultCompanionTurnRepository(),
            backendClient = backend,
            gateway = FakeGateway(),
            scope = backgroundScope,
            baseUrlProvider = { null },
            tokenProvider = { "token" },
        )
        val result = repo.editUserTurn(
            conversationId = conversationId,
            parentMessageId = parentMessageId,
            newUserText = "x",
            activeCompanionId = "y",
            activeLanguage = "en",
        )
        assertTrue(result.isFailure)
        assertTrue(backend.editCalls.isEmpty()) // never called
    }

    @Test
    fun `editUserTurn re-applied with the same response is idempotent`() =
        runTest(UnconfinedTestDispatcher()) {
            val (repo, backend) = buildRepo(editResponse = { sampleResponse() }, scope = backgroundScope)
            repo.editUserTurn(conversationId, parentMessageId, "x", "y", "en")
            repo.editUserTurn(conversationId, parentMessageId, "x", "y", "en")
            assertEquals(2, backend.editCalls.size)
            val tree = repo.treeByConversation.value[conversationId]!!
            // Only one new user message + one new companion-turn — the second call's
            // applyRecord short-circuits via the existing recordUserTurn / applyRecord
            // duplicate-check (messagesById.containsKey + variantGroup re-applies).
            val newUserCount = tree.messagesById.values.count {
                it.id == "user-message-edit-smoke-01"
            }
            assertEquals(1, newUserCount)
        }

    private fun sampleResponse(): EditUserTurnResponseDto = EditUserTurnResponseDto(
        userMessage = NewUserMessageRecordDto(
            messageId = "user-message-edit-smoke-01",
            variantGroupId = "variant-group-edit-user-smoke-01",
            variantIndex = 0,
            parentMessageId = parentMessageId,
            role = "user",
        ),
        companionTurn = CompanionTurnRecordDto(
            turnId = "turn-edit-companion-smoke-01",
            conversationId = conversationId,
            messageId = "turn-edit-companion-smoke-01",
            variantGroupId = "variant-group-edit-companion-smoke-01",
            variantIndex = 0,
            parentMessageId = "user-message-edit-smoke-01",
            status = "completed",
            accumulatedBody = "the regenerated reply",
            lastDeltaSeq = 0,
            startedAt = "2026-04-26T12:00:00Z",
            completedAt = "2026-04-26T12:00:01Z",
        ),
    )

    private class FakeGateway : RealtimeGateway {
        override val isConnected: StateFlow<Boolean> = MutableStateFlow(false)
        override val lastFailure: StateFlow<String?> = MutableStateFlow(null)
        override val events: SharedFlow<ImGatewayEvent> = MutableSharedFlow(extraBufferCapacity = 1)
        override fun connect(token: String?, endpointOverride: String?) = Unit
        override fun send(payload: String): Boolean = true
        override fun sendMessage(recipientExternalId: String, clientMessageId: String?, body: String): Boolean = true
        override fun markRead(conversationId: String, messageId: String): Boolean = true
        override fun disconnect() = Unit
    }

    private class FakeBackendClient(
        private val editResponse: ((EditUserTurnRequestDto) -> EditUserTurnResponseDto)? = null,
    ) : ImBackendClient {
        val editCalls = mutableListOf<EditUserTurnRequestDto>()

        override suspend fun issueDevSession(baseUrl: String, externalId: String): DevSessionResponseDto = error("n/a")
        override suspend fun register(baseUrl: String, username: String, password: String, displayName: String): AuthResponseDto = error("n/a")
        override suspend fun login(baseUrl: String, username: String, password: String): AuthResponseDto = error("n/a")
        override suspend fun searchUsers(baseUrl: String, token: String, query: String): List<UserSearchResultDto> = error("n/a")
        override suspend fun sendFriendRequest(baseUrl: String, token: String, toUserId: String): FriendRequestViewDto = error("n/a")
        override suspend fun listFriendRequests(baseUrl: String, token: String): List<FriendRequestViewDto> = error("n/a")
        override suspend fun acceptFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto = error("n/a")
        override suspend fun rejectFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto = error("n/a")
        override suspend fun loadBootstrap(baseUrl: String, token: String): BootstrapBundleDto = error("n/a")
        override suspend fun sendDirectImageMessage(baseUrl: String, token: String, request: SendDirectImageMessageRequestDto): SendDirectMessageResultDto = error("n/a")
        override suspend fun loadHistory(baseUrl: String, token: String, conversationId: String, limit: Int, before: String?): MessageHistoryPageDto = error("n/a")

        override suspend fun listPendingCompanionTurns(
            baseUrl: String,
            token: String,
        ): CompanionTurnPendingListDto = CompanionTurnPendingListDto(emptyList())

        override suspend fun editUserTurn(
            baseUrl: String,
            token: String,
            conversationId: String,
            request: EditUserTurnRequestDto,
        ): EditUserTurnResponseDto {
            editCalls += request
            return editResponse?.invoke(request) ?: error("edit response not configured")
        }
    }
}
