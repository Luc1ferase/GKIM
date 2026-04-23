package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.CompanionMemory
import com.gkim.im.android.core.model.CompanionMemoryPin
import com.gkim.im.android.core.model.CompanionMemoryResetScope
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.data.remote.im.AuthResponseDto
import com.gkim.im.android.data.remote.im.BootstrapBundleDto
import com.gkim.im.android.data.remote.im.CompanionMemoryDto
import com.gkim.im.android.data.remote.im.CompanionMemoryPinDto
import com.gkim.im.android.data.remote.im.CompanionMemoryPinListDto
import com.gkim.im.android.data.remote.im.DevSessionResponseDto
import com.gkim.im.android.data.remote.im.FriendRequestViewDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.LocalizedTextDto
import com.gkim.im.android.data.remote.im.MessageHistoryPageDto
import com.gkim.im.android.data.remote.im.SendDirectImageMessageRequestDto
import com.gkim.im.android.data.remote.im.SendDirectMessageResultDto
import com.gkim.im.android.data.remote.im.UserSearchResultDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LiveCompanionMemoryRepositoryTest {

    private val cardId = "card-aria"
    private val baseUrl = "https://im.example.com"
    private val token = "token-nox"

    private fun seededSnapshot(
        pins: List<CompanionMemoryPin> = emptyList(),
    ): CompanionMemorySnapshot = CompanionMemorySnapshot(
        memory = CompanionMemory(
            userId = "user-nox",
            companionCardId = cardId,
            summary = LocalizedText("base", "基础"),
            summaryUpdatedAt = 500L,
            summaryTurnCursor = 7,
        ),
        pins = pins,
    )

    private fun pin(
        id: String,
        english: String,
        chinese: String,
        sourceMessageId: String? = null,
        createdAt: Long = 1_000L,
    ): CompanionMemoryPin = CompanionMemoryPin(
        id = id,
        sourceMessageId = sourceMessageId,
        text = LocalizedText(english, chinese),
        createdAt = createdAt,
        pinnedByUser = true,
    )

    private fun liveRepoWith(
        backend: FakeImBackendClient,
        initialSnapshot: CompanionMemorySnapshot? = null,
        idStart: Int = 1,
        clockTicks: LongArray = longArrayOf(2_000L),
    ): LiveCompanionMemoryRepository {
        var counter = idStart
        var tick = 0
        return LiveCompanionMemoryRepository(
            backend = backend,
            baseUrlProvider = { baseUrl },
            tokenProvider = { token },
            initial = initialSnapshot?.let { mapOf(cardId to it) }.orEmpty(),
            idGenerator = { "pin-generated-${counter++}" },
            clock = {
                val next = clockTicks[tick.coerceAtMost(clockTicks.lastIndex)]
                tick++
                next
            },
        )
    }

    @Test
    fun `refresh pulls memory and pins from backend and applies them to the snapshot`() = runBlocking {
        val backend = FakeImBackendClient(
            memoryResponse = CompanionMemoryDto(
                userId = "user-nox",
                companionCardId = cardId,
                summary = LocalizedTextDto("hello world", "你好世界"),
                summaryUpdatedAt = 1_700_000_000_000L,
                summaryTurnCursor = 12,
                tokenBudgetHint = 800,
            ),
            pinsResponse = CompanionMemoryPinListDto(
                pins = listOf(
                    CompanionMemoryPinDto(
                        id = "pin-backend-1",
                        sourceMessageId = "msg-9",
                        text = LocalizedTextDto("Remember me", "记住我"),
                        createdAt = 1_700_000_001_000L,
                        pinnedByUser = true,
                    ),
                ),
            ),
        )
        val repo = liveRepoWith(backend)

        repo.refresh(cardId)

        assertEquals(listOf(cardId), backend.getMemoryCardIds)
        assertEquals(listOf(cardId), backend.listPinsCardIds)
        assertEquals(listOf(baseUrl, baseUrl), backend.seenBaseUrls)
        assertEquals(listOf(token, token), backend.seenTokens)

        val memory = repo.observeMemory(cardId).first()
        assertNotNull(memory)
        assertEquals("hello world", memory!!.summary.english)
        assertEquals("你好世界", memory.summary.chinese)
        assertEquals(1_700_000_000_000L, memory.summaryUpdatedAt)
        assertEquals(12, memory.summaryTurnCursor)
        assertEquals(800, memory.tokenBudgetHint)

        val pins = repo.observePins(cardId).first()
        assertEquals(listOf("pin-backend-1"), pins.map { it.id })
        assertEquals("Remember me", pins.single().text.english)
    }

    @Test
    fun `createPin optimistically adds pin then replaces it with backend-returned pin`() = runBlocking {
        val backend = FakeImBackendClient(
            createPinResponse = CompanionMemoryPinDto(
                id = "pin-server-1",
                sourceMessageId = "msg-9",
                text = LocalizedTextDto("Hi", "嗨"),
                createdAt = 3_000L,
                pinnedByUser = true,
            ),
        )
        val repo = liveRepoWith(backend, initialSnapshot = seededSnapshot(), clockTicks = longArrayOf(2_000L))

        val created = repo.createPin(
            cardId = cardId,
            sourceMessageId = "msg-9",
            text = LocalizedText("Hi", "嗨"),
        ).getOrThrow()

        assertEquals("pin-server-1", created.id)
        assertEquals(3_000L, created.createdAt)
        val pins = repo.observePins(cardId).first()
        assertEquals(listOf("pin-server-1"), pins.map { it.id })

        assertEquals(1, backend.createPinRequests.size)
        val sent = backend.createPinRequests.single()
        assertEquals(cardId, sent.cardId)
        assertEquals("pin-generated-1", sent.pin.id)
        assertEquals("msg-9", sent.pin.sourceMessageId)
        assertEquals("Hi", sent.pin.text.english)
        assertEquals("嗨", sent.pin.text.chinese)
    }

    @Test
    fun `createPin rolls back optimistic add and returns Result failure when backend 5xx`() = runBlocking {
        val backend = FakeImBackendClient(createPinFailure = IOException("HTTP 503"))
        val repo = liveRepoWith(backend, initialSnapshot = seededSnapshot())

        val result = repo.createPin(
            cardId = cardId,
            sourceMessageId = null,
            text = LocalizedText("Hi", "嗨"),
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(
            "local pin list must be restored on rollback",
            repo.observePins(cardId).first().isEmpty(),
        )
    }

    @Test
    fun `updatePin forwards DTO to backend and replaces optimistic pin with server response`() = runBlocking {
        val existing = pin(id = "pin-a", english = "A", chinese = "甲", createdAt = 1_000L)
        val backend = FakeImBackendClient(
            updatePinResponse = CompanionMemoryPinDto(
                id = "pin-a",
                sourceMessageId = null,
                text = LocalizedTextDto("A prime", "甲'"),
                createdAt = 1_000L,
                pinnedByUser = true,
            ),
        )
        val repo = liveRepoWith(backend, initialSnapshot = seededSnapshot(pins = listOf(existing)))

        val updated = repo.updatePin("pin-a", LocalizedText("A prime", "甲'")).getOrThrow()

        assertEquals("A prime", updated.text.english)
        assertEquals("甲'", updated.text.chinese)
        assertEquals(1_000L, updated.createdAt)

        assertEquals(1, backend.updatePinRequests.size)
        val sent = backend.updatePinRequests.single()
        assertEquals(cardId, sent.cardId)
        assertEquals("pin-a", sent.pin.id)
        assertEquals("A prime", sent.pin.text.english)
    }

    @Test
    fun `updatePin rolls back and surfaces failure when backend 5xx`() = runBlocking {
        val existing = pin(id = "pin-a", english = "A", chinese = "甲", createdAt = 1_000L)
        val backend = FakeImBackendClient(updatePinFailure = IOException("HTTP 500"))
        val repo = liveRepoWith(backend, initialSnapshot = seededSnapshot(pins = listOf(existing)))

        val result = repo.updatePin("pin-a", LocalizedText("A prime", "甲'"))

        assertTrue(result.isFailure)
        val pinsAfter = repo.observePins(cardId).first()
        assertEquals(listOf(existing), pinsAfter)
    }

    @Test
    fun `updatePin on unknown pin skips backend and returns failure`() = runBlocking {
        val backend = FakeImBackendClient()
        val repo = liveRepoWith(backend, initialSnapshot = seededSnapshot())

        val result = repo.updatePin("pin-missing", LocalizedText("X", "X"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnknownPinException)
        assertTrue(backend.updatePinRequests.isEmpty())
    }

    @Test
    fun `deletePin forwards pinId to backend and removes pin on success`() = runBlocking {
        val existing = pin(id = "pin-a", english = "A", chinese = "甲", createdAt = 1_000L)
        val backend = FakeImBackendClient()
        val repo = liveRepoWith(backend, initialSnapshot = seededSnapshot(pins = listOf(existing)))

        val result = repo.deletePin("pin-a")

        assertTrue(result.isSuccess)
        assertEquals(listOf(Pair(cardId, "pin-a")), backend.deletePinRequests)
        assertTrue(repo.observePins(cardId).first().isEmpty())
    }

    @Test
    fun `deletePin rolls back removed pin when backend 5xx`() = runBlocking {
        val existing = pin(id = "pin-a", english = "A", chinese = "甲", createdAt = 1_000L)
        val backend = FakeImBackendClient(deletePinFailure = IOException("HTTP 502"))
        val repo = liveRepoWith(backend, initialSnapshot = seededSnapshot(pins = listOf(existing)))

        val result = repo.deletePin("pin-a")

        assertTrue(result.isFailure)
        assertEquals(listOf(existing), repo.observePins(cardId).first())
    }

    @Test
    fun `deletePin on unknown pin skips backend and returns failure`() = runBlocking {
        val backend = FakeImBackendClient()
        val repo = liveRepoWith(backend, initialSnapshot = seededSnapshot())

        val result = repo.deletePin("pin-missing")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnknownPinException)
        assertTrue(backend.deletePinRequests.isEmpty())
    }

    @Test
    fun `reset forwards the scope enum to backend after clearing local state`() = runBlocking {
        val existing = pin(id = "pin-a", english = "A", chinese = "甲", createdAt = 1_000L)
        val backend = FakeImBackendClient()
        val repo = liveRepoWith(backend, initialSnapshot = seededSnapshot(pins = listOf(existing)))

        repo.reset(cardId, CompanionMemoryResetScope.Pins)
        repo.reset(cardId, CompanionMemoryResetScope.Summary)
        repo.reset(cardId, CompanionMemoryResetScope.All)

        assertEquals(
            listOf(
                Pair(cardId, CompanionMemoryResetScope.Pins),
                Pair(cardId, CompanionMemoryResetScope.Summary),
                Pair(cardId, CompanionMemoryResetScope.All),
            ),
            backend.resetRequests,
        )
    }

    data class RecordedPinRequest(val cardId: String, val pin: CompanionMemoryPinDto)

    private class FakeImBackendClient(
        private val memoryResponse: CompanionMemoryDto? = null,
        private val pinsResponse: CompanionMemoryPinListDto? = null,
        private val createPinResponse: CompanionMemoryPinDto? = null,
        private val updatePinResponse: CompanionMemoryPinDto? = null,
        private val createPinFailure: Throwable? = null,
        private val updatePinFailure: Throwable? = null,
        private val deletePinFailure: Throwable? = null,
    ) : ImBackendClient {

        val seenBaseUrls = mutableListOf<String>()
        val seenTokens = mutableListOf<String>()
        val getMemoryCardIds = mutableListOf<String>()
        val listPinsCardIds = mutableListOf<String>()
        val createPinRequests = mutableListOf<RecordedPinRequest>()
        val updatePinRequests = mutableListOf<RecordedPinRequest>()
        val deletePinRequests = mutableListOf<Pair<String, String>>()
        val resetRequests = mutableListOf<Pair<String, CompanionMemoryResetScope>>()

        override suspend fun getCompanionMemory(
            baseUrl: String,
            token: String,
            cardId: String,
        ): CompanionMemoryDto {
            seenBaseUrls += baseUrl
            seenTokens += token
            getMemoryCardIds += cardId
            return memoryResponse ?: error("no memoryResponse configured")
        }

        override suspend fun listCompanionMemoryPins(
            baseUrl: String,
            token: String,
            cardId: String,
        ): CompanionMemoryPinListDto {
            seenBaseUrls += baseUrl
            seenTokens += token
            listPinsCardIds += cardId
            return pinsResponse ?: error("no pinsResponse configured")
        }

        override suspend fun createCompanionMemoryPin(
            baseUrl: String,
            token: String,
            cardId: String,
            pin: CompanionMemoryPinDto,
        ): CompanionMemoryPinDto {
            createPinRequests += RecordedPinRequest(cardId, pin)
            createPinFailure?.let { throw it }
            return createPinResponse ?: pin
        }

        override suspend fun updateCompanionMemoryPin(
            baseUrl: String,
            token: String,
            cardId: String,
            pin: CompanionMemoryPinDto,
        ): CompanionMemoryPinDto {
            updatePinRequests += RecordedPinRequest(cardId, pin)
            updatePinFailure?.let { throw it }
            return updatePinResponse ?: pin
        }

        override suspend fun deleteCompanionMemoryPin(
            baseUrl: String,
            token: String,
            cardId: String,
            pinId: String,
        ) {
            deletePinRequests += cardId to pinId
            deletePinFailure?.let { throw it }
        }

        override suspend fun resetCompanionMemory(
            baseUrl: String,
            token: String,
            cardId: String,
            scope: CompanionMemoryResetScope,
        ) {
            resetRequests += cardId to scope
        }

        override suspend fun issueDevSession(baseUrl: String, externalId: String): DevSessionResponseDto =
            error("not used in these tests")

        override suspend fun register(
            baseUrl: String,
            username: String,
            password: String,
            displayName: String,
        ): AuthResponseDto = error("not used in these tests")

        override suspend fun login(
            baseUrl: String,
            username: String,
            password: String,
        ): AuthResponseDto = error("not used in these tests")

        override suspend fun searchUsers(
            baseUrl: String,
            token: String,
            query: String,
        ): List<UserSearchResultDto> = emptyList()

        override suspend fun sendFriendRequest(
            baseUrl: String,
            token: String,
            toUserId: String,
        ): FriendRequestViewDto = error("not used in these tests")

        override suspend fun listFriendRequests(
            baseUrl: String,
            token: String,
        ): List<FriendRequestViewDto> = emptyList()

        override suspend fun acceptFriendRequest(
            baseUrl: String,
            token: String,
            requestId: String,
        ): FriendRequestViewDto = error("not used in these tests")

        override suspend fun rejectFriendRequest(
            baseUrl: String,
            token: String,
            requestId: String,
        ): FriendRequestViewDto = error("not used in these tests")

        override suspend fun loadBootstrap(baseUrl: String, token: String): BootstrapBundleDto =
            error("not used in these tests")

        override suspend fun sendDirectImageMessage(
            baseUrl: String,
            token: String,
            request: SendDirectImageMessageRequestDto,
        ): SendDirectMessageResultDto = error("not used in these tests")

        override suspend fun loadHistory(
            baseUrl: String,
            token: String,
            conversationId: String,
            limit: Int,
            before: String?,
        ): MessageHistoryPageDto = error("not used in these tests")
    }
}
