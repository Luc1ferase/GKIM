package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.CompanionTurnMeta
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.data.repository.DefaultCompanionMemoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BubblePinActionTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Test
    fun `draft from user bubble uses message body as english primary when language is english`() {
        val message = outgoingMessage(id = "msg-user-1", body = "  Remind me to call mom.  ")
        val draft = buildBubblePinDraft(message, AppLanguage.English)

        assertEquals("msg-user-1", draft.sourceMessageId)
        assertEquals("Remind me to call mom.", draft.text.english)
        assertEquals(BubblePinActionDefaults.SecondaryStubChinese, draft.text.chinese)
    }

    @Test
    fun `draft from user bubble uses message body as chinese primary when language is chinese`() {
        val message = outgoingMessage(id = "msg-user-2", body = "记得给妈妈打电话。")
        val draft = buildBubblePinDraft(message, AppLanguage.Chinese)

        assertEquals("msg-user-2", draft.sourceMessageId)
        assertEquals(BubblePinActionDefaults.SecondaryStubEnglish, draft.text.english)
        assertEquals("记得给妈妈打电话。", draft.text.chinese)
    }

    @Test
    fun `draft from companion variant preserves source message id and variant body`() {
        val variant = companionVariant(
            id = "msg-companion-v2",
            body = "I remember our picnic vividly.",
            variantGroupId = "group-1",
            variantIndex = 2,
        )
        val draft = buildBubblePinDraft(variant, AppLanguage.English)

        assertEquals("msg-companion-v2", draft.sourceMessageId)
        assertEquals("I remember our picnic vividly.", draft.text.english)
    }

    @Test
    fun `pin action on user bubble produces pin observable via memory panel observer`() = runTest(dispatcher) {
        val repo = DefaultCompanionMemoryRepository()
        val cardId = "card-nova"
        val message = outgoingMessage(id = "msg-user-1", body = "Birthday is April 22.")

        val draft = buildBubblePinDraft(message, AppLanguage.English)
        val result = submitBubblePin(draft, cardId, repo)

        assertTrue(result.isSuccess)
        val pins = repo.observePins(cardId).first()
        assertEquals(1, pins.size)
        val pin = pins.first()
        assertEquals("msg-user-1", pin.sourceMessageId)
        assertEquals("Birthday is April 22.", pin.text.english)
        assertEquals(BubblePinActionDefaults.SecondaryStubChinese, pin.text.chinese)
    }

    @Test
    fun `pin action on companion variant produces pin observable via memory panel observer`() = runTest(dispatcher) {
        val repo = DefaultCompanionMemoryRepository()
        val cardId = "card-nova"
        val variant = companionVariant(
            id = "msg-companion-v1",
            body = "Your favorite flower is hyacinth.",
            variantGroupId = "group-7",
            variantIndex = 1,
        )

        val draft = buildBubblePinDraft(variant, AppLanguage.English)
        val result = submitBubblePin(draft, cardId, repo)

        assertTrue(result.isSuccess)
        val pins = repo.observePins(cardId).first()
        assertEquals(1, pins.size)
        val pin = pins.first()
        assertEquals("msg-companion-v1", pin.sourceMessageId)
        assertEquals("Your favorite flower is hyacinth.", pin.text.english)
    }

    @Test
    fun `pin action works on non-active variant within a variant group`() = runTest(dispatcher) {
        val repo = DefaultCompanionMemoryRepository()
        val cardId = "card-nova"
        val activeVariant = companionVariant(
            id = "msg-companion-active",
            body = "Active path reply.",
            variantGroupId = "group-9",
            variantIndex = 0,
        )
        val inactiveVariant = companionVariant(
            id = "msg-companion-inactive",
            body = "Inactive variant reply that the user still wants to pin.",
            variantGroupId = "group-9",
            variantIndex = 2,
        )

        submitBubblePin(buildBubblePinDraft(activeVariant, AppLanguage.English), cardId, repo)
        submitBubblePin(buildBubblePinDraft(inactiveVariant, AppLanguage.English), cardId, repo)

        val pins = repo.observePins(cardId).first()
        assertEquals("both variants pin independently", 2, pins.size)
        val inactivePin = pins.firstOrNull { it.sourceMessageId == "msg-companion-inactive" }
        assertNotNull(
            "non-active variant's pin must be retrievable — spec requires pinning any variant, not only active-path",
            inactivePin,
        )
        assertEquals("Inactive variant reply that the user still wants to pin.", inactivePin!!.text.english)
    }

    @Test
    fun `draft trims body whitespace before inserting as primary language`() {
        val message = outgoingMessage(id = "msg-trim", body = "\n   Trimmed body.   \n")
        val draft = buildBubblePinDraft(message, AppLanguage.English)

        assertEquals("Trimmed body.", draft.text.english)
    }

    private fun outgoingMessage(id: String, body: String): ChatMessage = ChatMessage(
        id = id,
        direction = MessageDirection.Outgoing,
        kind = MessageKind.Text,
        body = body,
        createdAt = "2026-04-23T10:00:00Z",
    )

    private fun companionVariant(
        id: String,
        body: String,
        variantGroupId: String,
        variantIndex: Int,
    ): ChatMessage = ChatMessage(
        id = id,
        direction = MessageDirection.Incoming,
        kind = MessageKind.Text,
        body = body,
        createdAt = "2026-04-23T10:00:00Z",
        companionTurnMeta = CompanionTurnMeta(
            turnId = "turn-$variantGroupId",
            variantGroupId = variantGroupId,
            variantIndex = variantIndex,
        ),
    )
}
