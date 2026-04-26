package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.MessageStatus
import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import com.gkim.im.android.data.remote.im.EditUserTurnResponseDto
import com.gkim.im.android.data.remote.im.NewUserMessageRecordDto
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * §4.1 verification — `ChatViewModel.editUserTurn(messageId, newDraftText)` resolves the
 * bubble from the active path, builds the §3.2 sheet state, calls the repository's
 * `editUserTurn`, and surfaces in-flight + failure lifecycle through `uiState`'s
 * `treeAffordanceLifecycle` field.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelEditUserTurnTest {

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

    private val companionConversationId = "room-daylight-listener"

    private fun seededViewModel(
        backend: RecordingCompanionTurnRepository,
        messaging: MessagingRepository = run {
            val m = InMemoryMessagingRepository(emptyList())
            m.ensureConversation(contact = companionContact, companionCardId = companionContact.id)
            m
        },
    ): ChatViewModel = ChatViewModel(
        conversationId = companionConversationId,
        messagingRepository = messaging,
        companionTurnRepository = backend,
        aigcRepository = StubAigcRepository(),
        generatedImageSaver = StubImageSaver,
        userPersonaRepository = StubUserPersonaRepository,
            companionRosterRepository = stubCompanionRosterRepository(),
    )

    private fun seedActivePath(
        backend: RecordingCompanionTurnRepository,
        userMessageId: String,
        parentMessageId: String?,
        body: String,
    ) {
        // Inject a user message into the local tree so editUserTurn can resolve it.
        backend.recordUserTurn(
            ChatMessage(
                id = userMessageId,
                direction = MessageDirection.Outgoing,
                kind = MessageKind.Text,
                body = body,
                createdAt = "2026-04-26T12:00:00Z",
                parentMessageId = parentMessageId,
                status = MessageStatus.Completed,
            ),
            companionConversationId,
        )
    }

    @Test
    fun `editUserTurn delegates to the repository with the right wire shape`() = runTest(testDispatcher) {
        val backend = RecordingCompanionTurnRepository(
            editResult = { Result.success(sampleResponse()) },
        )
        val viewModel = seededViewModel(backend)
        seedActivePath(backend, userMessageId = "user-msg-7", parentMessageId = "msg-conv-root", body = "original")
        advanceUntilIdle()

        viewModel.editUserTurn(messageId = "user-msg-7", newDraftText = "rewritten", activeLanguage = AppLanguage.English)
        advanceUntilIdle()

        val call = backend.editCalls.single()
        assertEquals(companionConversationId, call.conversationId)
        assertEquals("msg-conv-root", call.parentMessageId)
        assertEquals("rewritten", call.newUserText)
        assertEquals("daylight-listener", call.activeCompanionId)
        assertEquals("en", call.activeLanguage)
    }

    @Test
    fun `editUserTurn lifecycle goes inFlight then back to idle on success`() = runTest(testDispatcher) {
        val backend = RecordingCompanionTurnRepository(
            editResult = { Result.success(sampleResponse()) },
        )
        val viewModel = seededViewModel(backend)
        seedActivePath(backend, userMessageId = "user-msg-7", parentMessageId = "msg-conv-root", body = "original")
        advanceUntilIdle()

        viewModel.editUserTurn(messageId = "user-msg-7", newDraftText = "rewritten")
        advanceUntilIdle()

        val lifecycle = viewModel.treeAffordanceLifecycle.value
        assertNull(lifecycle.inFlightForMessageId)
        assertNull(lifecycle.failedForMessageId)
        assertNull(lifecycle.failureReason)
    }

    @Test
    fun `editUserTurn lifecycle goes failed on transport failure with the error reason`() = runTest(testDispatcher) {
        val backend = RecordingCompanionTurnRepository(
            editResult = { Result.failure(IOException("network_timeout")) },
        )
        val viewModel = seededViewModel(backend)
        seedActivePath(backend, userMessageId = "user-msg-7", parentMessageId = "msg-conv-root", body = "original")
        advanceUntilIdle()

        viewModel.editUserTurn(messageId = "user-msg-7", newDraftText = "rewritten")
        advanceUntilIdle()

        val lifecycle = viewModel.treeAffordanceLifecycle.value
        assertEquals("user-msg-7", lifecycle.failedForMessageId)
        assertEquals("network_timeout", lifecycle.failureReason)
        assertNull(lifecycle.inFlightForMessageId)
    }

    @Test
    fun `editUserTurn no-ops when the bubble is not in the active path`() = runTest(testDispatcher) {
        val backend = RecordingCompanionTurnRepository(editResult = { Result.success(sampleResponse()) })
        val viewModel = seededViewModel(backend)
        // No active path seeding — bubble lookup fails.
        advanceUntilIdle()

        viewModel.editUserTurn(messageId = "ghost-bubble", newDraftText = "x")
        advanceUntilIdle()

        assertTrue(backend.editCalls.isEmpty())
        assertNull(viewModel.treeAffordanceLifecycle.value.inFlightForMessageId)
    }

    @Test
    fun `editUserTurn no-ops when the draft equals the original (canSubmit false)`() = runTest(testDispatcher) {
        val backend = RecordingCompanionTurnRepository(editResult = { Result.success(sampleResponse()) })
        val viewModel = seededViewModel(backend)
        seedActivePath(backend, userMessageId = "user-msg-7", parentMessageId = "msg-conv-root", body = "original")
        advanceUntilIdle()

        // Draft equals the bubble's body → §3.2 canSubmit returns false → no wire call.
        viewModel.editUserTurn(messageId = "user-msg-7", newDraftText = "original")
        advanceUntilIdle()

        assertTrue(backend.editCalls.isEmpty())
    }

    @Test
    fun `editUserTurn applies repository response so active path advances`() = runTest(testDispatcher) {
        val backend = RecordingCompanionTurnRepository(editResult = { Result.success(sampleResponse()) })
        val viewModel = seededViewModel(backend)
        seedActivePath(backend, userMessageId = "user-msg-7", parentMessageId = "msg-conv-root", body = "original")
        advanceUntilIdle()

        viewModel.editUserTurn(messageId = "user-msg-7", newDraftText = "rewritten")
        advanceUntilIdle()

        // The response's userMessage + companionTurn land in the local tree (the recording
        // delegate forwards to a real DefaultCompanionTurnRepository).
        val tree = backend.delegate.treeByConversation.value[companionConversationId]!!
        val newUserMessage = tree.messagesById["user-message-edit-smoke-01"]
        assertNotNull(newUserMessage)
        val newCompanionTurn = tree.messagesById["turn-edit-companion-smoke-01"]
        assertNotNull(newCompanionTurn)
    }

    @Test
    fun `dismissTreeAffordanceError clears the failed lifecycle`() = runTest(testDispatcher) {
        val backend = RecordingCompanionTurnRepository(editResult = { Result.failure(IOException("boom")) })
        val viewModel = seededViewModel(backend)
        seedActivePath(backend, userMessageId = "user-msg-7", parentMessageId = "msg-conv-root", body = "original")
        advanceUntilIdle()

        viewModel.editUserTurn(messageId = "user-msg-7", newDraftText = "rewritten")
        advanceUntilIdle()
        assertEquals("user-msg-7", viewModel.treeAffordanceLifecycle.value.failedForMessageId)

        viewModel.dismissTreeAffordanceError()
        advanceUntilIdle()
        assertNull(viewModel.treeAffordanceLifecycle.value.failedForMessageId)
        assertNull(viewModel.treeAffordanceLifecycle.value.failureReason)
    }

    private fun sampleResponse(): EditUserTurnResponseDto = EditUserTurnResponseDto(
        userMessage = NewUserMessageRecordDto(
            messageId = "user-message-edit-smoke-01",
            variantGroupId = "variant-group-edit-user-smoke-01",
            variantIndex = 0,
            parentMessageId = "msg-conv-root",
            role = "user",
        ),
        companionTurn = CompanionTurnRecordDto(
            turnId = "turn-edit-companion-smoke-01",
            conversationId = companionConversationId,
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

    internal data class RecordedEditCall(
        val conversationId: String,
        val parentMessageId: String,
        val newUserText: String,
        val activeCompanionId: String,
        val activeLanguage: String,
        val characterPromptContext: com.gkim.im.android.data.remote.im.CharacterPromptContextDto?,
    )

    internal class RecordingCompanionTurnRepository(
        val delegate: DefaultCompanionTurnRepository = DefaultCompanionTurnRepository(),
        private val editResult: ((RecordedEditCall) -> Result<EditUserTurnResponseDto>)? = null,
    ) : CompanionTurnRepository by delegate {
        val editCalls = mutableListOf<RecordedEditCall>()

        override suspend fun editUserTurn(
            conversationId: String,
            parentMessageId: String,
            newUserText: String,
            activeCompanionId: String,
            activeLanguage: String,
            characterPromptContext: com.gkim.im.android.data.remote.im.CharacterPromptContextDto?,
        ): Result<EditUserTurnResponseDto> {
            val call = RecordedEditCall(
                conversationId,
                parentMessageId,
                newUserText,
                activeCompanionId,
                activeLanguage,
                characterPromptContext,
            )
            editCalls += call
            val result = editResult?.invoke(call) ?: Result.failure(IllegalStateException("editResult not configured"))
            // Mirror the real LiveCompanionTurnRepository's projection so the test can
            // assert local-tree side effects.
            result.onSuccess { response ->
                val newUserMessage = ChatMessage(
                    id = response.userMessage.messageId,
                    direction = MessageDirection.Outgoing,
                    kind = MessageKind.Text,
                    body = newUserText,
                    createdAt = "2026-04-26T12:00:00Z",
                    parentMessageId = response.userMessage.parentMessageId,
                    status = MessageStatus.Completed,
                )
                delegate.recordUserTurn(newUserMessage, conversationId)
                delegate.applyRecord(response.companionTurn)
            }
            return result
        }
    }
}
