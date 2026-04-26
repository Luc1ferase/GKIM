package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AigcProvider
import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.AigcTask
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.core.model.CustomProviderConfig
import com.gkim.im.android.core.model.DraftAigcRequest
import com.gkim.im.android.core.model.MediaInput
import com.gkim.im.android.core.model.MessageAttachment
import com.gkim.im.android.core.model.PresetProviderConfig
import com.gkim.im.android.core.model.UserPersona
import com.gkim.im.android.core.media.GeneratedImageSaver
import com.gkim.im.android.core.media.GeneratedImageSaveResult
import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import com.gkim.im.android.data.repository.AigcRepository
import com.gkim.im.android.data.repository.CompanionTurnRepository
import com.gkim.im.android.data.repository.DefaultCompanionTurnRepository
import com.gkim.im.android.data.repository.FailedCompanionSubmission
import com.gkim.im.android.data.repository.InMemoryMessagingRepository
import com.gkim.im.android.data.repository.MessagingIntegrationPhase
import com.gkim.im.android.data.repository.MessagingIntegrationState
import com.gkim.im.android.data.repository.MessagingRepository
import com.gkim.im.android.data.repository.UserPersonaRepository
import com.gkim.im.android.data.repository.UserPersonaMutationResult
import com.gkim.im.android.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelCompanionDispatchTest {
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

    @Test
    fun `companion conversation dispatches through submitUserTurn and skips sendMessage`() = runTest {
        val messaging = RecordingMessagingRepository(InMemoryMessagingRepository(emptyList()))
        val companionTurn = RecordingCompanionTurnRepository()
        messaging.ensureConversation(contact = companionContact, companionCardId = companionContact.id)

        val viewModel = ChatViewModel(
            conversationId = "room-${companionContact.id}",
            messagingRepository = messaging,
            companionTurnRepository = companionTurn,
            aigcRepository = StubAigcRepository(),
            generatedImageSaver = StubImageSaver,
            userPersonaRepository = StubUserPersonaRepository,
            companionRosterRepository = stubCompanionRosterRepository(),
        )

        viewModel.sendMessage(body = "hello", activeLanguage = AppLanguage.English)
        advanceUntilIdle()

        assertEquals(1, companionTurn.submitCalls.size)
        val submit = companionTurn.submitCalls.single()
        assertEquals("room-${companionContact.id}", submit.conversationId)
        assertEquals(companionContact.id, submit.activeCompanionId)
        assertEquals("hello", submit.userTurnBody)
        assertEquals("en", submit.activeLanguage)
        assertNull(submit.parentMessageId)
        assertTrue(
            "companion dispatch must not invoke messagingRepository.sendMessage",
            messaging.sendMessageCalls.isEmpty(),
        )
    }

    @Test
    fun `peer conversation still dispatches through messagingRepository sendMessage`() = runTest {
        val messaging = RecordingMessagingRepository(InMemoryMessagingRepository(emptyList()))
        val companionTurn = RecordingCompanionTurnRepository()
        messaging.ensureConversation(peerContact)

        val viewModel = ChatViewModel(
            conversationId = "room-${peerContact.id}",
            messagingRepository = messaging,
            companionTurnRepository = companionTurn,
            aigcRepository = StubAigcRepository(),
            generatedImageSaver = StubImageSaver,
            userPersonaRepository = StubUserPersonaRepository,
            companionRosterRepository = stubCompanionRosterRepository(),
        )

        viewModel.sendMessage(body = "hi", activeLanguage = AppLanguage.Chinese)
        advanceUntilIdle()

        assertEquals(1, messaging.sendMessageCalls.size)
        val sent = messaging.sendMessageCalls.single()
        assertEquals("room-${peerContact.id}", sent.conversationId)
        assertEquals("hi", sent.body)
        assertTrue(
            "peer dispatch must not invoke companionTurnRepository.submitUserTurn",
            companionTurn.submitCalls.isEmpty(),
        )
    }

    @Test
    fun `empty body on companion conversation is a no-op`() = runTest {
        val messaging = RecordingMessagingRepository(InMemoryMessagingRepository(emptyList()))
        val companionTurn = RecordingCompanionTurnRepository()
        messaging.ensureConversation(contact = companionContact, companionCardId = companionContact.id)

        val viewModel = ChatViewModel(
            conversationId = "room-${companionContact.id}",
            messagingRepository = messaging,
            companionTurnRepository = companionTurn,
            aigcRepository = StubAigcRepository(),
            generatedImageSaver = StubImageSaver,
            userPersonaRepository = StubUserPersonaRepository,
            companionRosterRepository = stubCompanionRosterRepository(),
        )

        viewModel.sendMessage(body = "", activeLanguage = AppLanguage.English)
        advanceUntilIdle()

        assertTrue(companionTurn.submitCalls.isEmpty())
        assertTrue(messaging.sendMessageCalls.isEmpty())
    }

    @Test
    fun `sendMessage forwards resolved characterPromptContext when an active card and persona are in scope`() = runTest {
        val messaging = RecordingMessagingRepository(InMemoryMessagingRepository(emptyList()))
        val companionTurn = RecordingCompanionTurnRepository()
        messaging.ensureConversation(contact = companionContact, companionCardId = companionContact.id)

        val card = com.gkim.im.android.core.model.CompanionCharacterCard(
            id = companionContact.id,
            displayName = com.gkim.im.android.core.model.LocalizedText("Daylight Listener", "晴光抚慰者"),
            roleLabel = com.gkim.im.android.core.model.LocalizedText("Companion", "同伴"),
            summary = com.gkim.im.android.core.model.LocalizedText("EN summary", "中文摘要"),
            firstMes = com.gkim.im.android.core.model.LocalizedText("hi", "你好"),
            systemPrompt = com.gkim.im.android.core.model.LocalizedText(
                english = "You are {{char}} listening to {{user}}.",
                chinese = "你是 {{char}}，正在倾听 {{user}}。",
            ),
            personality = com.gkim.im.android.core.model.LocalizedText("Calm.", "沉稳。"),
            scenario = com.gkim.im.android.core.model.LocalizedText("Tavern.", "酒馆。"),
            exampleDialogue = com.gkim.im.android.core.model.LocalizedText(
                english = "{{user}}: hi\n{{char}}: hello",
                chinese = "{{user}}：你好\n{{char}}：你好",
            ),
            avatarText = "DL",
            accent = com.gkim.im.android.core.model.AccentTone.Primary,
            source = com.gkim.im.android.core.model.CompanionCharacterSource.Preset,
        )
        val persona = UserPersona(
            id = "persona-aria",
            displayName = com.gkim.im.android.core.model.LocalizedText("Aria", "雅"),
            description = com.gkim.im.android.core.model.LocalizedText("d", "d"),
            isActive = true,
        )
        val viewModel = ChatViewModel(
            conversationId = "room-${companionContact.id}",
            messagingRepository = messaging,
            companionTurnRepository = companionTurn,
            aigcRepository = StubAigcRepository(),
            generatedImageSaver = StubImageSaver,
            userPersonaRepository = ActivePersonaStubRepository(persona),
            companionRosterRepository = stubCompanionRosterRepository(presetCharacters = listOf(card)),
        )

        viewModel.sendMessage(body = "hello", activeLanguage = AppLanguage.English)
        advanceUntilIdle()

        val ctx = companionTurn.submitCalls.single().characterPromptContext
            ?: throw AssertionError("characterPromptContext must be forwarded when card + persona are in scope")
        assertEquals("You are {{char}} listening to {{user}}.", ctx.systemPrompt)
        assertEquals("Calm.", ctx.personality)
        assertEquals("Tavern.", ctx.scenario)
        assertEquals("{{user}}: hi\n{{char}}: hello", ctx.exampleDialogue)
        assertEquals("Aria", ctx.userPersonaName)
        assertEquals("Daylight Listener", ctx.companionDisplayName)
    }

    @Test
    fun `sendMessage forwards null characterPromptContext when no card is registered`() = runTest {
        val messaging = RecordingMessagingRepository(InMemoryMessagingRepository(emptyList()))
        val companionTurn = RecordingCompanionTurnRepository()
        messaging.ensureConversation(contact = companionContact, companionCardId = companionContact.id)

        // Roster has zero seeded cards → resolveCharacterPromptContext returns null →
        // outbound DTO carries null per the spec scenario "Missing card omits the payload".
        val viewModel = ChatViewModel(
            conversationId = "room-${companionContact.id}",
            messagingRepository = messaging,
            companionTurnRepository = companionTurn,
            aigcRepository = StubAigcRepository(),
            generatedImageSaver = StubImageSaver,
            userPersonaRepository = StubUserPersonaRepository,
            companionRosterRepository = stubCompanionRosterRepository(),
        )

        viewModel.sendMessage(body = "hello", activeLanguage = AppLanguage.English)
        advanceUntilIdle()

        assertNull(companionTurn.submitCalls.single().characterPromptContext)
    }
}

private class ActivePersonaStubRepository(
    private val persona: UserPersona,
) : UserPersonaRepository {
    override fun observePersonas(): Flow<List<UserPersona>> = flowOf(listOf(persona))
    override fun observeActivePersona(): Flow<UserPersona?> = flowOf(persona)
    override suspend fun create(persona: UserPersona): UserPersonaMutationResult =
        UserPersonaMutationResult.Failed(IllegalStateException("stub"))
    override suspend fun update(persona: UserPersona): UserPersonaMutationResult =
        UserPersonaMutationResult.Failed(IllegalStateException("stub"))
    override suspend fun delete(personaId: String): UserPersonaMutationResult =
        UserPersonaMutationResult.Failed(IllegalStateException("stub"))
    override suspend fun activate(personaId: String): UserPersonaMutationResult =
        UserPersonaMutationResult.Failed(IllegalStateException("stub"))
    override suspend fun duplicate(personaId: String): UserPersonaMutationResult =
        UserPersonaMutationResult.Failed(IllegalStateException("stub"))
    override suspend fun refresh() = Unit
}

// region test doubles

private data class SubmitCall(
    val conversationId: String,
    val activeCompanionId: String,
    val userTurnBody: String,
    val activeLanguage: String,
    val parentMessageId: String?,
    val characterPromptContext: com.gkim.im.android.data.remote.im.CharacterPromptContextDto?,
)

private data class SendMessageCall(
    val conversationId: String,
    val body: String,
    val attachment: MessageAttachment?,
)

private class RecordingCompanionTurnRepository(
    delegate: DefaultCompanionTurnRepository = DefaultCompanionTurnRepository(),
) : CompanionTurnRepository by delegate {
    val submitCalls = mutableListOf<SubmitCall>()
    override suspend fun submitUserTurn(
        conversationId: String,
        activeCompanionId: String,
        userTurnBody: String,
        activeLanguage: String,
        parentMessageId: String?,
        characterPromptContext: com.gkim.im.android.data.remote.im.CharacterPromptContextDto?,
    ): Result<CompanionTurnRecordDto> {
        submitCalls += SubmitCall(
            conversationId,
            activeCompanionId,
            userTurnBody,
            activeLanguage,
            parentMessageId,
            characterPromptContext,
        )
        return Result.failure(IllegalStateException("test stub — no backend"))
    }
}

private class RecordingMessagingRepository(
    private val delegate: InMemoryMessagingRepository,
) : MessagingRepository by delegate {
    val sendMessageCalls = mutableListOf<SendMessageCall>()
    override fun sendMessage(conversationId: String, body: String, attachment: MessageAttachment?) {
        sendMessageCalls += SendMessageCall(conversationId, body, attachment)
        delegate.sendMessage(conversationId, body, attachment)
    }
}

// endregion
