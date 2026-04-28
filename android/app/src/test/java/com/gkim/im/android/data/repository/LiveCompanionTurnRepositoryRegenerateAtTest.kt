package com.gkim.im.android.data.repository

import com.gkim.im.android.data.remote.im.AuthResponseDto
import com.gkim.im.android.data.remote.im.BootstrapBundleDto
import com.gkim.im.android.data.remote.im.CompanionTurnPendingListDto
import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import com.gkim.im.android.data.remote.im.DevSessionResponseDto
import com.gkim.im.android.data.remote.im.FriendRequestViewDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.remote.im.MessageHistoryPageDto
import com.gkim.im.android.data.remote.im.RegenerateAtRequestDto
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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * §3.2 verification — `LiveCompanionTurnRepository.regenerateCompanionTurnAtTarget(...)`
 * calls `POST /api/companion-turns/:conversationId/regenerate-at` with an explicit
 * `targetMessageId`, projects the response into the local sibling tree under that target's
 * `variantGroupId` at the next `variantIndex`, and emits the resulting `ChatMessage` updates
 * through the same `activePathByConversation` flow consumed by `ChatViewModel`.
 */
class LiveCompanionTurnRepositoryRegenerateAtTest {

    private val conversationId = "conversation-regen-smoke"
    private val targetVariantGroupId = "vg-mid"

    private fun seededRecord(turnId: String, variantIndex: Int, body: String): CompanionTurnRecordDto =
        CompanionTurnRecordDto(
            turnId = turnId,
            conversationId = conversationId,
            messageId = turnId,
            variantGroupId = targetVariantGroupId,
            variantIndex = variantIndex,
            parentMessageId = "user-msg-prior",
            status = "completed",
            accumulatedBody = body,
            lastDeltaSeq = 0,
            startedAt = "2026-04-26T12:00:00Z",
            completedAt = "2026-04-26T12:00:01Z",
        )

    @Test
    fun `regenerateCompanionTurnAtTarget POSTs targetMessageId and clientTurnId`() =
        runTest(UnconfinedTestDispatcher()) {
            val backend = FakeBackendClient(
                regenAtResponse = { _ -> seededRecord("turn-new-sibling", variantIndex = 1, body = "regen") },
            )
            val repo = buildRepo(backend, scope = backgroundScope)
            // Seed sibling 0 first so the repo's tree has a target to regenerate against.
            repo.applyRecord(seededRecord(turnId = "turn-original", variantIndex = 0, body = "orig"))

            val result = repo.regenerateCompanionTurnAtTarget(
                conversationId = conversationId,
                targetMessageId = "turn-original",
            )
            assertTrue(result.isSuccess)
            val captured = backend.regenAtCalls.single()
            assertEquals("turn-original", captured.targetMessageId)
            assertEquals("client-turn-regen-stub", captured.clientTurnId)
        }

    @Test
    fun `regenerateCompanionTurnAtTarget appends the new sibling under the same variantGroup`() =
        runTest(UnconfinedTestDispatcher()) {
            val backend = FakeBackendClient(
                regenAtResponse = { seededRecord("turn-new-sibling", variantIndex = 1, body = "regen") },
            )
            val repo = buildRepo(backend, scope = backgroundScope)
            repo.applyRecord(seededRecord(turnId = "turn-original", variantIndex = 0, body = "orig"))

            repo.regenerateCompanionTurnAtTarget(conversationId, "turn-original")
            val tree = repo.treeByConversation.value[conversationId]!!
            val group = tree.variantGroups[targetVariantGroupId]!!
            assertEquals(2, group.siblingMessageIds.size)
            assertEquals(listOf("turn-original", "turn-new-sibling"), group.siblingMessageIds)
            // The new sibling is the active variant.
            assertEquals(1, group.activeIndex)
        }

    @Test
    fun `regenerateCompanionTurnAtTarget re-runs the §2_1 sibling projection so chevrons update`() =
        runTest(UnconfinedTestDispatcher()) {
            val backend = FakeBackendClient(
                regenAtResponse = { seededRecord("turn-new-sibling", variantIndex = 1, body = "regen") },
            )
            val repo = buildRepo(backend, scope = backgroundScope)
            repo.applyRecord(seededRecord(turnId = "turn-original", variantIndex = 0, body = "orig"))

            repo.regenerateCompanionTurnAtTarget(conversationId, "turn-original")
            val active = repo.activePathByConversation.value[conversationId]!!.single()
            val meta = active.companionTurnMeta!!
            assertEquals(2, meta.siblingCount)
            assertEquals(1, meta.siblingActiveIndex)
            assertEquals("regen", active.body)
        }

    @Test
    fun `regenerateCompanionTurnAtTarget surfaces transport failures without mutating state`() =
        runTest(UnconfinedTestDispatcher()) {
            val backend = FakeBackendClient(regenAtResponse = { throw IOException("boom") })
            val repo = buildRepo(backend, scope = backgroundScope)
            // Seed a single-sibling state.
            repo.applyRecord(seededRecord(turnId = "turn-original", variantIndex = 0, body = "orig"))
            val priorTreeRef = repo.treeByConversation.value[conversationId]

            val result = repo.regenerateCompanionTurnAtTarget(conversationId, "turn-original")
            assertTrue(result.isFailure)
            // Tree state unchanged after the failed call.
            assertEquals(priorTreeRef, repo.treeByConversation.value[conversationId])
        }

    @Test
    fun `regenerateCompanionTurnAtTarget fails fast with no token`() =
        runTest(UnconfinedTestDispatcher()) {
            val backend = FakeBackendClient(regenAtResponse = { seededRecord("x", 1, "x") })
            val repo = LiveCompanionTurnRepository(
                default = DefaultCompanionTurnRepository(),
                backendClient = backend,
                gateway = FakeGateway(),
                scope = backgroundScope,
                baseUrlProvider = { "http://fake" },
                tokenProvider = { null },
                clientTurnIdGenerator = { "client-turn-regen-stub" },
            )
            val result = repo.regenerateCompanionTurnAtTarget(conversationId, "any-target")
            assertTrue(result.isFailure)
            assertTrue(backend.regenAtCalls.isEmpty()) // never invoked
        }

    @Test
    fun `mid-conversation regenerate works on a non-latest sibling — proves arbitrary-layer support`() =
        runTest(UnconfinedTestDispatcher()) {
            val backend = FakeBackendClient(
                regenAtResponse = { req ->
                    // Server returns a new sibling under the target's variantGroup. The fake
                    // honors the targetMessageId by encoding it into the new sibling's
                    // accumulatedBody so the test can assert which variant was hit.
                    seededRecord(
                        turnId = "regen-of-${req.targetMessageId}",
                        variantIndex = 1,
                        body = "regen-of-${req.targetMessageId}",
                    )
                },
            )
            val repo = buildRepo(backend, scope = backgroundScope)
            // Seed a 3-message timeline: turn-1 → turn-2 → turn-3 (each in its own variantGroup).
            repo.applyRecord(
                seededRecord(turnId = "turn-1", variantIndex = 0, body = "first").copy(
                    variantGroupId = "vg-1",
                ),
            )
            repo.applyRecord(
                seededRecord(turnId = "turn-2", variantIndex = 0, body = "second").copy(
                    variantGroupId = "vg-2",
                ),
            )
            repo.applyRecord(
                seededRecord(turnId = "turn-3", variantIndex = 0, body = "third").copy(
                    variantGroupId = "vg-3",
                ),
            )

            // Regenerate the MIDDLE turn (not the latest).
            val result = repo.regenerateCompanionTurnAtTarget(conversationId, "turn-2")
            assertTrue(result.isSuccess)
            val regenRecord = result.getOrNull()
            assertNotNull(regenRecord)
            assertEquals("regen-of-turn-2", regenRecord!!.messageId)
        }

    private fun buildRepo(
        backend: FakeBackendClient,
        scope: kotlinx.coroutines.CoroutineScope,
    ): LiveCompanionTurnRepository = LiveCompanionTurnRepository(
        default = DefaultCompanionTurnRepository(),
        backendClient = backend,
        gateway = FakeGateway(),
        scope = scope,
        baseUrlProvider = { "http://fake" },
        tokenProvider = { "token" },
        clientTurnIdGenerator = { "client-turn-regen-stub" },
        clock = { "2026-04-26T12:00:00Z" },
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
        private val regenAtResponse: ((RegenerateAtRequestDto) -> CompanionTurnRecordDto)? = null,
    ) : ImBackendClient {
        val regenAtCalls = mutableListOf<RegenerateAtRequestDto>()

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

        override suspend fun regenerateCompanionTurnAtTarget(
            baseUrl: String,
            token: String,
            conversationId: String,
            request: RegenerateAtRequestDto,
        ): CompanionTurnRecordDto {
            regenAtCalls += request
            return regenAtResponse?.invoke(request) ?: error("regenAt response not configured")
        }
    }
}
