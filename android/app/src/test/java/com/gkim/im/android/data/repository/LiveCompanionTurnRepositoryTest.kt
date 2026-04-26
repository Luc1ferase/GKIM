package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.MessageStatus
import com.gkim.im.android.data.remote.im.ActiveCompanionSelectionDto
import com.gkim.im.android.data.remote.im.AuthResponseDto
import com.gkim.im.android.data.remote.im.BootstrapBundleDto
import com.gkim.im.android.data.remote.im.CompanionCharacterCardDto
import com.gkim.im.android.data.remote.im.CompanionDrawResultDto
import com.gkim.im.android.data.remote.im.CompanionRosterDto
import com.gkim.im.android.data.remote.im.CharacterPromptContextDto
import com.gkim.im.android.data.remote.im.CompanionTurnPendingListDto
import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import com.gkim.im.android.data.remote.im.CompanionTurnSubmitRequestDto
import com.gkim.im.android.data.remote.im.DevSessionResponseDto
import com.gkim.im.android.data.remote.im.FriendRequestViewDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.remote.im.MessageHistoryPageDto
import com.gkim.im.android.data.remote.im.SendDirectImageMessageRequestDto
import com.gkim.im.android.data.remote.im.SendDirectMessageResultDto
import com.gkim.im.android.data.remote.im.UserSearchResultDto
import com.gkim.im.android.data.remote.realtime.RealtimeGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveCompanionTurnRepositoryTest {
    private val conversationId = "conversation-1"

    private class FakeGateway : RealtimeGateway {
        val connectionState = MutableStateFlow(false)
        val failureState = MutableStateFlow<String?>(null)
        val eventsFlow = MutableSharedFlow<ImGatewayEvent>(extraBufferCapacity = 64)

        override val isConnected: StateFlow<Boolean> = connectionState
        override val lastFailure: StateFlow<String?> = failureState
        override val events: SharedFlow<ImGatewayEvent> = eventsFlow

        override fun connect(token: String?, endpointOverride: String?) {
            connectionState.value = true
        }

        override fun send(payload: String): Boolean = true
        override fun sendMessage(
            recipientExternalId: String,
            clientMessageId: String?,
            body: String,
        ): Boolean = true

        override fun markRead(conversationId: String, messageId: String): Boolean = true

        override fun disconnect() {
            connectionState.value = false
        }
    }

    private class FakeBackendClient(
        private val pending: CompanionTurnPendingListDto = CompanionTurnPendingListDto(emptyList()),
        private val submitResponse: ((CompanionTurnSubmitRequestDto) -> CompanionTurnRecordDto)? = null,
        private val regenerateResponse: ((String, String) -> CompanionTurnRecordDto)? = null,
        private val snapshotResponse: ((String) -> CompanionTurnRecordDto)? = null,
    ) : ImBackendClient {
        val submitCalls = mutableListOf<CompanionTurnSubmitRequestDto>()
        val regenerateCalls = mutableListOf<Pair<String, String>>()
        val snapshotCalls = mutableListOf<String>()
        var pendingCalls = 0

        override suspend fun issueDevSession(baseUrl: String, externalId: String): DevSessionResponseDto = error("n/a")
        override suspend fun register(baseUrl: String, username: String, password: String, displayName: String): AuthResponseDto = error("n/a")
        override suspend fun login(baseUrl: String, username: String, password: String): AuthResponseDto = error("n/a")
        override suspend fun searchUsers(baseUrl: String, token: String, query: String): List<UserSearchResultDto> = error("n/a")
        override suspend fun sendFriendRequest(baseUrl: String, token: String, toUserId: String): FriendRequestViewDto = error("n/a")
        override suspend fun listFriendRequests(baseUrl: String, token: String): List<FriendRequestViewDto> = error("n/a")
        override suspend fun acceptFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto = error("n/a")
        override suspend fun rejectFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto = error("n/a")
        override suspend fun loadBootstrap(baseUrl: String, token: String): BootstrapBundleDto = error("n/a")
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
        ): MessageHistoryPageDto = error("n/a")

        override suspend fun submitCompanionTurn(
            baseUrl: String,
            token: String,
            request: CompanionTurnSubmitRequestDto,
        ): CompanionTurnRecordDto {
            submitCalls += request
            return submitResponse?.invoke(request)
                ?: error("submit response not configured")
        }

        override suspend fun regenerateCompanionTurn(
            baseUrl: String,
            token: String,
            turnId: String,
            clientTurnId: String,
        ): CompanionTurnRecordDto {
            regenerateCalls += turnId to clientTurnId
            return regenerateResponse?.invoke(turnId, clientTurnId)
                ?: error("regenerate response not configured")
        }

        override suspend fun listPendingCompanionTurns(
            baseUrl: String,
            token: String,
        ): CompanionTurnPendingListDto {
            pendingCalls += 1
            return pending
        }

        override suspend fun snapshotCompanionTurn(
            baseUrl: String,
            token: String,
            turnId: String,
        ): CompanionTurnRecordDto {
            snapshotCalls += turnId
            return snapshotResponse?.invoke(turnId)
                ?: error("snapshot response not configured")
        }
    }

    private fun buildRepo(
        pending: CompanionTurnPendingListDto = CompanionTurnPendingListDto(emptyList()),
        submit: ((CompanionTurnSubmitRequestDto) -> CompanionTurnRecordDto)? = null,
        regenerate: ((String, String) -> CompanionTurnRecordDto)? = null,
        snapshot: ((String) -> CompanionTurnRecordDto)? = null,
        scope: CoroutineScope,
        gateway: FakeGateway = FakeGateway(),
    ): Triple<LiveCompanionTurnRepository, FakeBackendClient, FakeGateway> {
        val backend = FakeBackendClient(
            pending = pending,
            submitResponse = submit,
            regenerateResponse = regenerate,
            snapshotResponse = snapshot,
        )
        val repo = LiveCompanionTurnRepository(
            default = DefaultCompanionTurnRepository(),
            backendClient = backend,
            gateway = gateway,
            scope = scope,
            baseUrlProvider = { "http://fake" },
            tokenProvider = { "token" },
            clientTurnIdGenerator = { "client-turn-stub" },
            clock = { "2026-04-21T08:00:00Z" },
        )
        return Triple(repo, backend, gateway)
    }

    @Test
    fun `on connect the repo fetches pending turns and rehydrates them`() = runTest(UnconfinedTestDispatcher()) {
        val pending = CompanionTurnPendingListDto(
            turns = listOf(
                CompanionTurnRecordDto(
                    turnId = "turn-42",
                    conversationId = conversationId,
                    messageId = "companion-42",
                    variantGroupId = "vg-42",
                    variantIndex = 0,
                    status = "streaming",
                    accumulatedBody = "partial reply",
                    lastDeltaSeq = 3,
                    startedAt = "2026-04-21T07:59:00Z",
                ),
            ),
        )
        val (repo, backend, gateway) = buildRepo(pending = pending, scope = backgroundScope)

        gateway.connectionState.value = true
        advanceUntilIdle()

        assertEquals(1, backend.pendingCalls)
        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals(1, path.size)
        assertEquals(MessageStatus.Streaming, path.single().status)
        assertEquals("partial reply", path.single().body)
    }

    @Test
    fun `gateway companion_turn events drive the reducer through completion`() = runTest(UnconfinedTestDispatcher()) {
        val (repo, _, gateway) = buildRepo(scope = backgroundScope)

        gateway.eventsFlow.emit(
            ImGatewayEvent.CompanionTurnStarted(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                variantGroupId = "vg-1",
                variantIndex = 0,
            ),
        )
        gateway.eventsFlow.emit(
            ImGatewayEvent.CompanionTurnDelta(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                deltaSeq = 0,
                textDelta = "hello",
            ),
        )
        gateway.eventsFlow.emit(
            ImGatewayEvent.CompanionTurnCompleted(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                finalBody = "hello world",
                completedAt = "2026-04-21T08:00:05Z",
            ),
        )
        advanceUntilIdle()

        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals(MessageStatus.Completed, path.single().status)
        assertEquals("hello world", path.single().body)
    }

    @Test
    fun `delta gap triggers snapshot fetch to resync accumulated body`() = runTest(UnconfinedTestDispatcher()) {
        val snapshotRecord = CompanionTurnRecordDto(
            turnId = "turn-1",
            conversationId = conversationId,
            messageId = "companion-1",
            variantGroupId = "vg-1",
            variantIndex = 0,
            status = "streaming",
            accumulatedBody = "repaired body",
            lastDeltaSeq = 5,
            startedAt = "2026-04-21T08:00:00Z",
        )
        val (repo, backend, gateway) = buildRepo(
            scope = backgroundScope,
            snapshot = { snapshotRecord },
        )

        gateway.eventsFlow.emit(
            ImGatewayEvent.CompanionTurnStarted(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                variantGroupId = "vg-1",
                variantIndex = 0,
            ),
        )
        gateway.eventsFlow.emit(
            ImGatewayEvent.CompanionTurnDelta(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                deltaSeq = 0,
                textDelta = "partial",
            ),
        )
        // Emit a delta with deltaSeq=4 — skipping 1,2,3 to simulate a gap
        gateway.eventsFlow.emit(
            ImGatewayEvent.CompanionTurnDelta(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                deltaSeq = 4,
                textDelta = " additional",
            ),
        )
        advanceUntilIdle()

        assertEquals(listOf("turn-1"), backend.snapshotCalls)
        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals("repaired body", path.single().body)
    }

    @Test
    fun `submitUserTurn calls backend and records user + companion message in the tree`() = runTest(UnconfinedTestDispatcher()) {
        val record = CompanionTurnRecordDto(
            turnId = "turn-1",
            conversationId = conversationId,
            messageId = "companion-1",
            variantGroupId = "vg-1",
            variantIndex = 0,
            parentMessageId = "user-1",
            status = "thinking",
            accumulatedBody = "",
            lastDeltaSeq = 0,
            startedAt = "2026-04-21T08:00:00Z",
        )
        val (repo, backend, _) = buildRepo(
            scope = backgroundScope,
            submit = { record },
        )

        val result = repo.submitUserTurn(
            conversationId = conversationId,
            activeCompanionId = "architect-oracle",
            userTurnBody = "Hi there",
            activeLanguage = "en",
        )
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertEquals(1, backend.submitCalls.size)
        assertEquals("Hi there", backend.submitCalls.single().userTurnBody)
        assertEquals("client-turn-stub", backend.submitCalls.single().clientTurnId)

        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals(2, path.size)
        assertEquals("user-client-turn-stub", path.first().id)
        assertEquals("Hi there", path.first().body)
        assertEquals(MessageStatus.Completed, path.first().status)
        assertEquals(MessageStatus.Thinking, path.last().status)
        assertTrue(repo.failedSubmissions.value.isEmpty())
    }

    @Test
    fun `regenerateTurn appends a new sibling via backend call`() = runTest(UnconfinedTestDispatcher()) {
        val existingRecord = CompanionTurnRecordDto(
            turnId = "turn-1",
            conversationId = conversationId,
            messageId = "companion-1",
            variantGroupId = "vg-1",
            variantIndex = 0,
            parentMessageId = "user-1",
            status = "completed",
            accumulatedBody = "first",
            lastDeltaSeq = 4,
            startedAt = "2026-04-21T08:00:00Z",
            completedAt = "2026-04-21T08:00:05Z",
        )
        val regeneratedRecord = CompanionTurnRecordDto(
            turnId = "turn-2",
            conversationId = conversationId,
            messageId = "companion-2",
            variantGroupId = "vg-1",
            variantIndex = 1,
            parentMessageId = "user-1",
            status = "thinking",
            accumulatedBody = "",
            lastDeltaSeq = 0,
            startedAt = "2026-04-21T08:01:00Z",
        )
        val (repo, backend, _) = buildRepo(
            scope = backgroundScope,
            regenerate = { _, _ -> regeneratedRecord },
        )
        repo.applyRecord(existingRecord)

        val result = repo.regenerateTurn(turnId = "turn-1")
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertEquals(1, backend.regenerateCalls.size)
        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals("companion-2", path.single().id)
        val tree = repo.treeByConversation.value[conversationId]!!
        val group = tree.variantGroups["vg-1"]!!
        assertEquals(listOf("companion-1", "companion-2"), group.siblingMessageIds)
        assertEquals(1, group.activeIndex)
    }

    @Test
    fun `submitUserTurn surfaces backend failure on the user bubble and records retry context`() = runTest(UnconfinedTestDispatcher()) {
        val (repo, _, _) = buildRepo(
            scope = backgroundScope,
            submit = { throw RuntimeException("offline") },
        )

        val result = repo.submitUserTurn(
            conversationId = conversationId,
            activeCompanionId = "architect-oracle",
            userTurnBody = "Hi",
            activeLanguage = "en",
        )
        advanceUntilIdle()

        assertTrue(result.isFailure)
        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals(1, path.size)
        val userMessage = path.single()
        assertEquals("user-client-turn-stub", userMessage.id)
        assertEquals("Hi", userMessage.body)
        assertEquals(MessageStatus.Failed, userMessage.status)

        val failure = repo.failedSubmissions.value["user-client-turn-stub"]!!
        assertEquals("Hi", failure.userTurnBody)
        assertEquals("architect-oracle", failure.activeCompanionId)
        assertEquals("en", failure.activeLanguage)
    }

    @Test
    fun `retrySubmitUserTurn resubmits failed context and flips user message to completed`() = runTest(UnconfinedTestDispatcher()) {
        val record = CompanionTurnRecordDto(
            turnId = "turn-1",
            conversationId = conversationId,
            messageId = "companion-1",
            variantGroupId = "vg-1",
            variantIndex = 0,
            parentMessageId = "user-client-turn-stub",
            status = "thinking",
            accumulatedBody = "",
            lastDeltaSeq = 0,
            startedAt = "2026-04-21T08:00:00Z",
        )
        var attempt = 0
        val (repo, backend, _) = buildRepo(
            scope = backgroundScope,
            submit = {
                attempt += 1
                if (attempt == 1) throw RuntimeException("offline") else record
            },
        )

        val firstResult = repo.submitUserTurn(
            conversationId = conversationId,
            activeCompanionId = "architect-oracle",
            userTurnBody = "Hi",
            activeLanguage = "en",
        )
        advanceUntilIdle()
        assertTrue(firstResult.isFailure)
        assertEquals(
            MessageStatus.Failed,
            repo.activePathByConversation.value[conversationId].orEmpty().single().status,
        )

        val retryResult = repo.retrySubmitUserTurn("user-client-turn-stub")
        advanceUntilIdle()

        assertTrue(retryResult.isSuccess)
        assertEquals(2, backend.submitCalls.size)
        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals(2, path.size)
        assertEquals("user-client-turn-stub", path.first().id)
        assertEquals(MessageStatus.Completed, path.first().status)
        assertEquals(MessageStatus.Thinking, path.last().status)
        assertTrue(repo.failedSubmissions.value.isEmpty())
    }

    @Test
    fun `retrySubmitUserTurn returns failure when no failed submission exists`() = runTest(UnconfinedTestDispatcher()) {
        val (repo, _, _) = buildRepo(scope = backgroundScope)

        val result = repo.retrySubmitUserTurn("nope")
        advanceUntilIdle()

        assertTrue(result.isFailure)
    }

    @Test
    fun `submitUserTurn without base url surfaces failure on the user bubble`() = runTest(UnconfinedTestDispatcher()) {
        val backend = FakeBackendClient()
        val repo = LiveCompanionTurnRepository(
            default = DefaultCompanionTurnRepository(),
            backendClient = backend,
            gateway = FakeGateway(),
            scope = backgroundScope,
            baseUrlProvider = { null },
            tokenProvider = { "token" },
            clientTurnIdGenerator = { "client-turn-stub" },
            clock = { "2026-04-21T08:00:00Z" },
        )

        val result = repo.submitUserTurn(
            conversationId = conversationId,
            activeCompanionId = "architect-oracle",
            userTurnBody = "Hi",
            activeLanguage = "en",
        )
        advanceUntilIdle()

        assertTrue(result.isFailure)
        assertEquals(0, backend.submitCalls.size)
        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals(MessageStatus.Failed, path.single().status)
        assertEquals("Hi", repo.failedSubmissions.value["user-client-turn-stub"]!!.userTurnBody)
    }

    @Test
    fun `pending rehydration swallows backend exception without crashing`() = runTest(UnconfinedTestDispatcher()) {
        val backend = object : ImBackendClient {
            override suspend fun issueDevSession(baseUrl: String, externalId: String): DevSessionResponseDto = error("n/a")
            override suspend fun register(baseUrl: String, username: String, password: String, displayName: String): AuthResponseDto = error("n/a")
            override suspend fun login(baseUrl: String, username: String, password: String): AuthResponseDto = error("n/a")
            override suspend fun searchUsers(baseUrl: String, token: String, query: String): List<UserSearchResultDto> = error("n/a")
            override suspend fun sendFriendRequest(baseUrl: String, token: String, toUserId: String): FriendRequestViewDto = error("n/a")
            override suspend fun listFriendRequests(baseUrl: String, token: String): List<FriendRequestViewDto> = error("n/a")
            override suspend fun acceptFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto = error("n/a")
            override suspend fun rejectFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto = error("n/a")
            override suspend fun loadBootstrap(baseUrl: String, token: String): BootstrapBundleDto = error("n/a")
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
            ): MessageHistoryPageDto = error("n/a")

            override suspend fun listPendingCompanionTurns(
                baseUrl: String,
                token: String,
            ): CompanionTurnPendingListDto = throw RuntimeException("pending blew up")
        }
        val gateway = FakeGateway()
        val repo = LiveCompanionTurnRepository(
            default = DefaultCompanionTurnRepository(),
            backendClient = backend,
            gateway = gateway,
            scope = backgroundScope,
            baseUrlProvider = { "http://fake" },
            tokenProvider = { "token" },
            clientTurnIdGenerator = { "client-turn" },
            clock = { "2026-04-21T08:00:00Z" },
        )

        gateway.connectionState.value = true
        advanceUntilIdle()

        // Should not crash; pending rehydration is silent on failure.
        assertEquals(0, repo.activePathByConversation.value.size)
    }

    // -------------------------------------------------------------------------
    // §4.2 verification — characterPromptContext is forwarded onto the wire
    // (companion-turn-character-prompt-context).
    // -------------------------------------------------------------------------

    @Test
    fun `submitUserTurn forwards characterPromptContext onto the outbound DTO`() = runTest(UnconfinedTestDispatcher()) {
        val record = CompanionTurnRecordDto(
            turnId = "turn-ctx-1",
            conversationId = conversationId,
            messageId = "companion-ctx-1",
            variantGroupId = "vg-ctx-1",
            variantIndex = 0,
            parentMessageId = "user-client-turn-stub",
            status = "thinking",
            accumulatedBody = "",
            lastDeltaSeq = 0,
            startedAt = "2026-04-21T08:00:00Z",
        )
        val (repo, backend, _) = buildRepo(scope = backgroundScope, submit = { record })
        val ctx = CharacterPromptContextDto(
            systemPrompt = "You are {{char}}.",
            personality = "Calm.",
            scenario = "Tavern.",
            exampleDialogue = "{{user}}: hi",
            userPersonaName = "Aria",
            companionDisplayName = "Daylight Listener",
        )

        val result = repo.submitUserTurn(
            conversationId = conversationId,
            activeCompanionId = "daylight-listener",
            userTurnBody = "Hi",
            activeLanguage = "en",
            characterPromptContext = ctx,
        )
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertEquals(1, backend.submitCalls.size)
        assertEquals(ctx, backend.submitCalls.single().characterPromptContext)
    }

    @Test
    fun `submitUserTurn defaults characterPromptContext to null when omitted`() = runTest(UnconfinedTestDispatcher()) {
        val record = CompanionTurnRecordDto(
            turnId = "turn-noctx-1",
            conversationId = conversationId,
            messageId = "companion-noctx-1",
            variantGroupId = "vg-noctx-1",
            variantIndex = 0,
            parentMessageId = "user-client-turn-stub",
            status = "thinking",
            accumulatedBody = "",
            lastDeltaSeq = 0,
            startedAt = "2026-04-21T08:00:00Z",
        )
        val (repo, backend, _) = buildRepo(scope = backgroundScope, submit = { record })

        repo.submitUserTurn(
            conversationId = conversationId,
            activeCompanionId = "daylight-listener",
            userTurnBody = "Hi",
            activeLanguage = "en",
        )
        advanceUntilIdle()

        assertEquals(1, backend.submitCalls.size)
        assertNull(backend.submitCalls.single().characterPromptContext)
    }

    @Test
    fun `retrySubmitUserTurn replays the captured characterPromptContext byte-equivalently`() = runTest(UnconfinedTestDispatcher()) {
        val record = CompanionTurnRecordDto(
            turnId = "turn-retry-1",
            conversationId = conversationId,
            messageId = "companion-retry-1",
            variantGroupId = "vg-retry-1",
            variantIndex = 0,
            parentMessageId = "user-client-turn-stub",
            status = "thinking",
            accumulatedBody = "",
            lastDeltaSeq = 0,
            startedAt = "2026-04-21T08:00:00Z",
        )
        var attempt = 0
        val (repo, backend, _) = buildRepo(
            scope = backgroundScope,
            submit = {
                attempt += 1
                if (attempt == 1) throw RuntimeException("offline") else record
            },
        )
        val ctx = CharacterPromptContextDto(
            systemPrompt = "You are {{char}}.",
            personality = "Calm.",
            scenario = "Tavern.",
            exampleDialogue = "{{user}}: hi",
            userPersonaName = "Aria",
            companionDisplayName = "Daylight Listener",
        )

        val firstResult = repo.submitUserTurn(
            conversationId = conversationId,
            activeCompanionId = "daylight-listener",
            userTurnBody = "Hi",
            activeLanguage = "en",
            characterPromptContext = ctx,
        )
        advanceUntilIdle()
        assertTrue(firstResult.isFailure)

        val retryResult = repo.retrySubmitUserTurn("user-client-turn-stub")
        advanceUntilIdle()
        assertTrue(retryResult.isSuccess)

        // Both the original failed call and the retry must carry the SAME ctx —
        // the retry MUST NOT re-resolve at retry time per the spec scenario.
        assertEquals(2, backend.submitCalls.size)
        assertEquals(ctx, backend.submitCalls[0].characterPromptContext)
        assertEquals(ctx, backend.submitCalls[1].characterPromptContext)
    }
}
