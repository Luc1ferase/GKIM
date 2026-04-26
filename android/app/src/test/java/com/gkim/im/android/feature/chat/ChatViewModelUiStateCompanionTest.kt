package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.AigcProvider
import com.gkim.im.android.core.model.AigcTask
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.core.model.CustomProviderConfig
import com.gkim.im.android.core.model.DraftAigcRequest
import com.gkim.im.android.core.model.MediaInput
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.MessageStatus
import com.gkim.im.android.core.model.PresetProviderConfig
import com.gkim.im.android.core.model.UserPersona
import com.gkim.im.android.core.media.GeneratedImageSaveResult
import com.gkim.im.android.core.media.GeneratedImageSaver
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.repository.AigcRepository
import com.gkim.im.android.data.repository.DefaultCompanionTurnRepository
import com.gkim.im.android.data.repository.InMemoryMessagingRepository
import com.gkim.im.android.data.repository.UserPersonaMutationResult
import com.gkim.im.android.data.repository.UserPersonaRepository
import com.gkim.im.android.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelUiStateCompanionTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val companionContact = Contact(
        id = "daylight-listener",
        nickname = "晴光抚慰者",
        title = "温柔倾听者",
        avatarText = "DL",
        addedAt = "2026-04-24T00:00:00Z",
        isOnline = true,
    )
    private val peerContact = Contact(
        id = "peer-friend",
        nickname = "Friend",
        title = "",
        avatarText = "FR",
        addedAt = "2026-04-24T00:00:00Z",
        isOnline = true,
    )

    private val conversationId = "room-${companionContact.id}"

    @Test
    fun `companion uiState carries companion messages from the state flow`() = runTest {
        val messaging = InMemoryMessagingRepository(emptyList())
        messaging.ensureConversation(contact = companionContact, companionCardId = companionContact.id)
        val companionTurn = DefaultCompanionTurnRepository()

        val viewModel = ChatViewModel(
            conversationId = conversationId,
            messagingRepository = messaging,
            companionTurnRepository = companionTurn,
            aigcRepository = StubAigcRepository(),
            generatedImageSaver = StubImageSaver,
            userPersonaRepository = StubUserPersonaRepository,
            companionRosterRepository = stubCompanionRosterRepository(),
        )
        val collectorJob = viewModel.uiState.launchIn(backgroundScope)

        // Record optimistic user bubble at Pending.
        val userMessage = ChatMessage(
            id = "user-1",
            direction = MessageDirection.Outgoing,
            kind = MessageKind.Text,
            body = "hello",
            createdAt = "",
            status = MessageStatus.Pending,
        )
        companionTurn.recordUserTurn(userMessage, conversationId)
        advanceUntilIdle()
        val afterPending = viewModel.uiState.value.companionMessages
        assertNotNull(afterPending)
        assertEquals(listOf(MessageStatus.Pending), afterPending!!.map { it.status })

        // Turn started → Thinking bubble appears.
        companionTurn.handleTurnStarted(
            ImGatewayEvent.CompanionTurnStarted(
                conversationId = conversationId,
                turnId = "turn-1",
                messageId = "msg-1",
                variantGroupId = "vg-1",
                variantIndex = 0,
                parentMessageId = "user-1",
                providerId = "stub",
                model = "stub",
            )
        )
        advanceUntilIdle()
        val afterStarted = viewModel.uiState.value.companionMessages
        assertEquals(
            listOf(MessageStatus.Pending, MessageStatus.Thinking),
            afterStarted!!.map { it.status },
        )

        // Delta → Streaming status.
        companionTurn.handleTurnDelta(
            ImGatewayEvent.CompanionTurnDelta(
                conversationId = conversationId,
                turnId = "turn-1",
                messageId = "msg-1",
                deltaSeq = 0,
                textDelta = "hi",
            )
        )
        advanceUntilIdle()
        val afterDelta = viewModel.uiState.value.companionMessages
        assertEquals(
            listOf(MessageStatus.Pending, MessageStatus.Streaming),
            afterDelta!!.map { it.status },
        )
        assertEquals("hi", afterDelta.last().body)

        // Completed → Completed status.
        companionTurn.handleTurnCompleted(
            ImGatewayEvent.CompanionTurnCompleted(
                conversationId = conversationId,
                turnId = "turn-1",
                messageId = "msg-1",
                finalBody = "hi there",
                completedAt = "2026-04-24T00:00:01Z",
            )
        )
        advanceUntilIdle()
        val afterCompleted = viewModel.uiState.value.companionMessages
        assertEquals(
            listOf(MessageStatus.Pending, MessageStatus.Completed),
            afterCompleted!!.map { it.status },
        )
        assertEquals("hi there", afterCompleted.last().body)
    }

    @Test
    fun `peer conversation uiState keeps companionMessages null`() = runTest {
        val messaging = InMemoryMessagingRepository(emptyList())
        messaging.ensureConversation(peerContact)
        val companionTurn = DefaultCompanionTurnRepository()

        val viewModel = ChatViewModel(
            conversationId = "room-${peerContact.id}",
            messagingRepository = messaging,
            companionTurnRepository = companionTurn,
            aigcRepository = StubAigcRepository(),
            generatedImageSaver = StubImageSaver,
            userPersonaRepository = StubUserPersonaRepository,
            companionRosterRepository = stubCompanionRosterRepository(),
        )
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()

        assertNull(
            "peer-IM conversations must leave companionMessages null so the renderer falls back to conversation.messages",
            viewModel.uiState.value.companionMessages,
        )
    }
}

