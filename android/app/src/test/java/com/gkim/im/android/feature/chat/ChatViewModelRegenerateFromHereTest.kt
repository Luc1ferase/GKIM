package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import com.gkim.im.android.data.repository.CompanionTurnRepository
import com.gkim.im.android.data.repository.DefaultCompanionTurnRepository
import com.gkim.im.android.data.repository.InMemoryMessagingRepository
import com.gkim.im.android.data.repository.MessagingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * §4.2 verification — `ChatViewModel.regenerateFromHere(messageId)` resolves the bubble from
 * the active path, builds the §3.3 `RegenerateAtRequestDto`, calls the repository's
 * `regenerateCompanionTurnAtTarget`, and surfaces in-flight + failure lifecycle through the
 * shared `treeAffordanceLifecycle` flow. Mid-conversation invocation (not only the most
 * recent companion bubble) MUST work per the §3.3 spec.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelRegenerateFromHereTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUpDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDownDispatcher() {
        Dispatchers.resetMain()
    }

    private val companionContact = Contact(
        id = "daylight-listener",
        nickname = "Daylight Listener",
        title = "Companion",
        avatarText = "DL",
        addedAt = "2026-04-26T12:00:00Z",
        isOnline = true,
    )

    private val conversationId = "room-daylight-listener"

    private fun seededViewModel(
        backend: RecordingCompanionTurnRepository,
        messaging: MessagingRepository = run {
            val m = InMemoryMessagingRepository(emptyList())
            m.ensureConversation(contact = companionContact, companionCardId = companionContact.id)
            m
        },
    ): ChatViewModel = ChatViewModel(
        conversationId = conversationId,
        messagingRepository = messaging,
        companionTurnRepository = backend,
        aigcRepository = StubAigcRepository(),
        generatedImageSaver = StubImageSaver,
        userPersonaRepository = StubUserPersonaRepository,
            companionRosterRepository = stubCompanionRosterRepository(),
    )

    private fun seedCompanionTurn(
        backend: RecordingCompanionTurnRepository,
        turnId: String,
        variantGroupId: String,
        variantIndex: Int = 0,
        body: String = "the companion reply",
        parentMessageId: String? = "user-prev",
    ) {
        backend.delegate.applyRecord(
            CompanionTurnRecordDto(
                turnId = turnId,
                conversationId = conversationId,
                messageId = turnId,
                variantGroupId = variantGroupId,
                variantIndex = variantIndex,
                parentMessageId = parentMessageId,
                status = "completed",
                accumulatedBody = body,
                lastDeltaSeq = 0,
                startedAt = "2026-04-26T12:00:00Z",
                completedAt = "2026-04-26T12:00:01Z",
            ),
        )
    }

    @Test
    fun `regenerateFromHere delegates to the repository with targetMessageId from the bubble`() = runTest(testDispatcher) {
        val backend = RecordingCompanionTurnRepository(
            regenAtResult = { _, target ->
                Result.success(canned(turnId = "turn-regen", variantGroupId = "vg-1", variantIndex = 1, body = "regen-of-$target"))
            },
        )
        val viewModel = seededViewModel(backend)
        seedCompanionTurn(backend, turnId = "turn-original", variantGroupId = "vg-1", variantIndex = 0)
        advanceUntilIdle()

        viewModel.regenerateFromHere(messageId = "turn-original")
        advanceUntilIdle()

        val call = backend.regenAtCalls.single()
        assertEquals(conversationId, call.first)
        assertEquals("turn-original", call.second)
    }

    @Test
    fun `regenerateFromHere lifecycle goes inFlight then back to idle on success`() = runTest(testDispatcher) {
        val backend = RecordingCompanionTurnRepository(
            regenAtResult = { _, _ -> Result.success(canned("turn-regen", "vg-1", 1, "regen")) },
        )
        val viewModel = seededViewModel(backend)
        seedCompanionTurn(backend, turnId = "turn-original", variantGroupId = "vg-1")
        advanceUntilIdle()

        viewModel.regenerateFromHere(messageId = "turn-original")
        advanceUntilIdle()

        val lifecycle = viewModel.treeAffordanceLifecycle.value
        assertNull(lifecycle.inFlightForMessageId)
        assertNull(lifecycle.failedForMessageId)
        assertNull(lifecycle.failureReason)
    }

    @Test
    fun `regenerateFromHere lifecycle goes failed on transport failure`() = runTest(testDispatcher) {
        val backend = RecordingCompanionTurnRepository(
            regenAtResult = { _, _ -> Result.failure(IOException("server_busy")) },
        )
        val viewModel = seededViewModel(backend)
        seedCompanionTurn(backend, turnId = "turn-original", variantGroupId = "vg-1")
        advanceUntilIdle()

        viewModel.regenerateFromHere(messageId = "turn-original")
        advanceUntilIdle()

        val lifecycle = viewModel.treeAffordanceLifecycle.value
        assertEquals("turn-original", lifecycle.failedForMessageId)
        assertEquals("server_busy", lifecycle.failureReason)
        assertNull(lifecycle.inFlightForMessageId)
    }

    @Test
    fun `regenerateFromHere no-ops when bubble not in active path`() = runTest(testDispatcher) {
        val backend = RecordingCompanionTurnRepository(
            regenAtResult = { _, _ -> Result.success(canned("turn-regen", "vg-1", 1, "regen")) },
        )
        val viewModel = seededViewModel(backend)
        // No seed.
        advanceUntilIdle()

        viewModel.regenerateFromHere(messageId = "ghost-companion")
        advanceUntilIdle()

        assertTrue(backend.regenAtCalls.isEmpty())
        assertNull(viewModel.treeAffordanceLifecycle.value.inFlightForMessageId)
    }

    @Test
    fun `regenerateFromHere supports mid-conversation (non-latest) companion bubbles`() = runTest(testDispatcher) {
        val backend = RecordingCompanionTurnRepository(
            regenAtResult = { _, target -> Result.success(canned("regen-of-$target", "vg-2", 1, "regen-mid")) },
        )
        val viewModel = seededViewModel(backend)
        // Seed three companion turns, each in its own variantGroup. The MIDDLE one is what
        // the test exercises — the §3.3 helper does not gate on most-recent-ness.
        seedCompanionTurn(backend, "turn-1", "vg-1", body = "first")
        seedCompanionTurn(backend, "turn-2", "vg-2", body = "second")
        seedCompanionTurn(backend, "turn-3", "vg-3", body = "third")
        advanceUntilIdle()

        viewModel.regenerateFromHere(messageId = "turn-2")
        advanceUntilIdle()

        val call = backend.regenAtCalls.single()
        assertEquals("turn-2", call.second)
    }

    private fun canned(
        turnId: String,
        variantGroupId: String,
        variantIndex: Int,
        body: String,
    ): CompanionTurnRecordDto = CompanionTurnRecordDto(
        turnId = turnId,
        conversationId = conversationId,
        messageId = turnId,
        variantGroupId = variantGroupId,
        variantIndex = variantIndex,
        parentMessageId = "user-prev",
        status = "completed",
        accumulatedBody = body,
        lastDeltaSeq = 0,
        startedAt = "2026-04-26T12:00:05Z",
        completedAt = "2026-04-26T12:00:06Z",
    )

    internal class RecordingCompanionTurnRepository(
        val delegate: DefaultCompanionTurnRepository = DefaultCompanionTurnRepository(),
        private val regenAtResult: ((String, String) -> Result<CompanionTurnRecordDto>)? = null,
    ) : CompanionTurnRepository by delegate {
        val regenAtCalls = mutableListOf<Pair<String, String>>()

        override suspend fun regenerateCompanionTurnAtTarget(
            conversationId: String,
            targetMessageId: String,
            characterPromptContext: com.gkim.im.android.data.remote.im.CharacterPromptContextDto?,
        ): Result<CompanionTurnRecordDto> {
            regenAtCalls += conversationId to targetMessageId
            val result = regenAtResult?.invoke(conversationId, targetMessageId)
                ?: Result.failure(IllegalStateException("regenAtResult not configured"))
            result.onSuccess { record -> delegate.applyRecord(record) }
            return result
        }
    }
}
