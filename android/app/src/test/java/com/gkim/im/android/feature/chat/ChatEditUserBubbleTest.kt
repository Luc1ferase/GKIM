package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.CompanionTurnMeta
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.MessageStatus
import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import com.gkim.im.android.data.remote.im.EditUserTurnRequestDto
import com.gkim.im.android.data.remote.im.EditUserTurnResponseDto
import com.gkim.im.android.data.remote.im.NewUserMessageRecordDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §3.2 verification — the "Edit" overflow on every user bubble opens an edit sheet
 * prefilled with the bubble's content. Submitting calls
 * `POST /api/companion-turns/{conversationId}/edit` with the right wire shape, and the
 * response's new user-message + companion-turn become the active path for their parents.
 *
 * Tests drive the pure helpers ([editUserBubbleSheetState],
 * [EditUserBubbleSheetState.withDraft], [EditUserBubbleSheetState.canSubmit],
 * [EditUserBubbleSheetState.toRequestDto], [editUserBubbleActivePathEffect]) that back the
 * sheet's state, the submit gate, the wire-DTO builder, and the post-response active-path
 * mutation. The composable's textfield change → withDraft, submit-button-tap → toRequestDto,
 * and onResponse → editUserBubbleActivePathEffect; asserting these helpers' contracts
 * implicitly covers the edit-sheet prefill, the backend call shape, and the active-path
 * switch without standing up Compose.
 */
class ChatEditUserBubbleTest {

    private fun userBubble(
        id: String = "user-msg-1",
        body: String = "original message body",
        parentMessageId: String? = "parent-msg-0",
    ): ChatMessage = ChatMessage(
        id = id,
        direction = MessageDirection.Outgoing,
        kind = MessageKind.Text,
        body = body,
        createdAt = "2026-04-25T12:00:00Z",
        parentMessageId = parentMessageId,
    )

    private fun companionBubble(
        id: String = "companion-msg-1",
        body: String = "ready",
        parentMessageId: String? = "user-msg-1",
    ): ChatMessage = ChatMessage(
        id = id,
        direction = MessageDirection.Incoming,
        kind = MessageKind.Text,
        body = body,
        createdAt = "2026-04-25T12:00:01Z",
        status = MessageStatus.Completed,
        parentMessageId = parentMessageId,
        companionTurnMeta = CompanionTurnMeta(
            turnId = id,
            variantGroupId = "vgroup-companion",
            variantIndex = 0,
        ),
    )

    private fun newUserMessageDto(
        messageId: String = "user-message-edit-smoke-01",
        variantGroupId: String = "variant-group-edit-user-smoke-01",
        parentMessageId: String? = "parent-msg-0",
    ): NewUserMessageRecordDto = NewUserMessageRecordDto(
        messageId = messageId,
        variantGroupId = variantGroupId,
        variantIndex = 0,
        parentMessageId = parentMessageId,
        role = "user",
    )

    private fun companionTurnDto(
        turnId: String = "turn-edit-companion-smoke-01",
        parentMessageId: String? = "user-message-edit-smoke-01",
    ): CompanionTurnRecordDto = CompanionTurnRecordDto(
        turnId = turnId,
        conversationId = "conversation-daylight-listener-smoke",
        messageId = turnId,
        variantGroupId = "variant-group-edit-companion-smoke-01",
        variantIndex = 0,
        parentMessageId = parentMessageId,
        status = "thinking",
        accumulatedBody = "",
        lastDeltaSeq = 0,
        startedAt = "2026-04-25T12:00:00Z",
    )

    // -------------------------------------------------------------------------
    // editUserBubbleSheetState — sheet prefill from the source bubble
    // -------------------------------------------------------------------------

    @Test
    fun `editUserBubbleSheetState returns null for an incoming companion bubble`() {
        val state = editUserBubbleSheetState(
            bubble = companionBubble(),
            conversationId = "conv-1",
            activeCompanionId = "daylight-listener",
            activeLanguage = AppLanguage.English,
        )
        assertNull(state)
    }

    @Test
    fun `editUserBubbleSheetState returns null for a root user bubble (no parentMessageId)`() {
        val state = editUserBubbleSheetState(
            bubble = userBubble(parentMessageId = null),
            conversationId = "conv-1",
            activeCompanionId = "daylight-listener",
            activeLanguage = AppLanguage.English,
        )
        assertNull(state)
    }

    @Test
    fun `editUserBubbleSheetState returns null when activeCompanionId is null (non-companion conversation)`() {
        val state = editUserBubbleSheetState(
            bubble = userBubble(),
            conversationId = "conv-1",
            activeCompanionId = null,
            activeLanguage = AppLanguage.English,
        )
        assertNull(state)
    }

    @Test
    fun `editUserBubbleSheetState prefills draft with the bubble body and copies context fields`() {
        val state = editUserBubbleSheetState(
            bubble = userBubble(id = "msg-7", body = "hello traveler", parentMessageId = "msg-6"),
            conversationId = "conversation-daylight-listener-smoke",
            activeCompanionId = "daylight-listener",
            activeLanguage = AppLanguage.English,
        )
        assertNotNull(state)
        state!!
        assertEquals("msg-7", state.sourceBubbleId)
        assertEquals("msg-6", state.parentMessageId)
        assertEquals("conversation-daylight-listener-smoke", state.conversationId)
        assertEquals("daylight-listener", state.activeCompanionId)
        assertEquals("en", state.activeLanguage)
        assertEquals("hello traveler", state.originalText)
        assertEquals("hello traveler", state.draftText)
    }

    @Test
    fun `editUserBubbleSheetState wire-language tracks the active AppLanguage`() {
        val chineseState = editUserBubbleSheetState(
            bubble = userBubble(),
            conversationId = "conv-1",
            activeCompanionId = "daylight-listener",
            activeLanguage = AppLanguage.Chinese,
        )
        assertNotNull(chineseState)
        assertEquals("zh", chineseState!!.activeLanguage)
    }

    // -------------------------------------------------------------------------
    // withDraft — the user's textfield edits update the draft
    // -------------------------------------------------------------------------

    @Test
    fun `withDraft updates draftText without touching original or context`() {
        val state = editUserBubbleSheetState(
            bubble = userBubble(body = "hello traveler"),
            conversationId = "conv-1",
            activeCompanionId = "daylight-listener",
            activeLanguage = AppLanguage.English,
        )!!.withDraft("hello dear traveler")
        assertEquals("hello dear traveler", state.draftText)
        assertEquals("hello traveler", state.originalText)
        assertEquals("daylight-listener", state.activeCompanionId)
    }

    // -------------------------------------------------------------------------
    // canSubmit — the submit gate
    // -------------------------------------------------------------------------

    @Test
    fun `canSubmit is false when the draft equals the original text (no-op submit)`() {
        val state = editUserBubbleSheetState(
            bubble = userBubble(body = "unchanged"),
            conversationId = "conv-1",
            activeCompanionId = "daylight-listener",
            activeLanguage = AppLanguage.English,
        )!!
        assertFalse(state.canSubmit())
    }

    @Test
    fun `canSubmit is false when the draft is blank`() {
        val state = editUserBubbleSheetState(
            bubble = userBubble(body = "original"),
            conversationId = "conv-1",
            activeCompanionId = "daylight-listener",
            activeLanguage = AppLanguage.English,
        )!!.withDraft("   ")
        assertFalse(state.canSubmit())
    }

    @Test
    fun `canSubmit is true when the draft is non-blank and differs from the original`() {
        val state = editUserBubbleSheetState(
            bubble = userBubble(body = "original"),
            conversationId = "conv-1",
            activeCompanionId = "daylight-listener",
            activeLanguage = AppLanguage.English,
        )!!.withDraft("rewritten")
        assertTrue(state.canSubmit())
        assertNotEquals(state.originalText, state.draftText)
    }

    // -------------------------------------------------------------------------
    // toRequestDto — the wire-DTO builder
    // -------------------------------------------------------------------------

    @Test
    fun `toRequestDto returns null when canSubmit is false`() {
        val state = editUserBubbleSheetState(
            bubble = userBubble(body = "original"),
            conversationId = "conv-1",
            activeCompanionId = "daylight-listener",
            activeLanguage = AppLanguage.English,
        )!!
        assertNull(state.toRequestDto(clientTurnId = "client-turn-x"))
    }

    @Test
    fun `toRequestDto packages the sheet state into the wire DTO when submittable`() {
        val state = editUserBubbleSheetState(
            bubble = userBubble(id = "msg-7", body = "original", parentMessageId = "msg-6"),
            conversationId = "conv-1",
            activeCompanionId = "daylight-listener",
            activeLanguage = AppLanguage.English,
        )!!.withDraft("rewritten greeting from user")
        val request = state.toRequestDto(clientTurnId = "client-turn-edit-smoke-01")!!
        assertEquals(
            EditUserTurnRequestDto(
                parentMessageId = "msg-6",
                newUserText = "rewritten greeting from user",
                clientTurnId = "client-turn-edit-smoke-01",
                activeCompanionId = "daylight-listener",
                activeLanguage = "en",
            ),
            request,
        )
    }

    // -------------------------------------------------------------------------
    // editUserBubbleActivePathEffect — the post-response active-path mutation
    // -------------------------------------------------------------------------

    @Test
    fun `editUserBubbleActivePathEffect returns null when userMessage has no parentMessageId`() {
        val effect = editUserBubbleActivePathEffect(
            response = EditUserTurnResponseDto(
                userMessage = newUserMessageDto(parentMessageId = null),
                companionTurn = companionTurnDto(),
            ),
        )
        assertNull(effect)
    }

    @Test
    fun `editUserBubbleActivePathEffect returns null when companionTurn has no parentMessageId`() {
        val effect = editUserBubbleActivePathEffect(
            response = EditUserTurnResponseDto(
                userMessage = newUserMessageDto(),
                companionTurn = companionTurnDto(parentMessageId = null),
            ),
        )
        assertNull(effect)
    }

    @Test
    fun `editUserBubbleActivePathEffect builds the two-edge effect from a complete response`() {
        val response = EditUserTurnResponseDto(
            userMessage = newUserMessageDto(
                messageId = "user-message-edit-smoke-01",
                parentMessageId = "msg-original-user-7",
            ),
            companionTurn = companionTurnDto(
                turnId = "turn-edit-companion-smoke-01",
                parentMessageId = "user-message-edit-smoke-01",
            ),
        )
        val effect = editUserBubbleActivePathEffect(response)!!
        assertEquals("msg-original-user-7", effect.userMessageParentId)
        assertEquals("user-message-edit-smoke-01", effect.newActiveUserMessageId)
        assertEquals("user-message-edit-smoke-01", effect.companionTurnParentId)
        assertEquals("turn-edit-companion-smoke-01", effect.newActiveCompanionMessageId)
    }

    // -------------------------------------------------------------------------
    // End-to-end happy path — sheet → request → effect (the whole edit lifecycle)
    // -------------------------------------------------------------------------

    @Test
    fun `end-to-end happy path - prefill, submit, apply response`() {
        // 1. Sheet opens prefilled with the bubble's content.
        val sheet = editUserBubbleSheetState(
            bubble = userBubble(
                id = "msg-original-user-7",
                body = "the original text",
                parentMessageId = "msg-original-conv-root",
            ),
            conversationId = "conversation-daylight-listener-smoke",
            activeCompanionId = "daylight-listener",
            activeLanguage = AppLanguage.English,
        )!!
        assertEquals("the original text", sheet.draftText)

        // 2. User edits the draft and submit becomes available.
        val edited = sheet.withDraft("the rewritten text")
        assertTrue(edited.canSubmit())

        // 3. Submit packages the wire DTO with the right shape.
        val request = edited.toRequestDto(clientTurnId = "client-turn-edit-smoke-01")!!
        assertEquals("msg-original-conv-root", request.parentMessageId)
        assertEquals("the rewritten text", request.newUserText)
        assertEquals("daylight-listener", request.activeCompanionId)
        assertEquals("en", request.activeLanguage)

        // 4. The response's two new variants become the active path for their parents.
        val response = EditUserTurnResponseDto(
            userMessage = newUserMessageDto(
                messageId = "user-message-edit-smoke-01",
                parentMessageId = "msg-original-conv-root",
            ),
            companionTurn = companionTurnDto(
                turnId = "turn-edit-companion-smoke-01",
                parentMessageId = "user-message-edit-smoke-01",
            ),
        )
        val effect = editUserBubbleActivePathEffect(response)!!
        assertEquals("msg-original-conv-root", effect.userMessageParentId)
        assertEquals("user-message-edit-smoke-01", effect.newActiveUserMessageId)
        assertEquals("user-message-edit-smoke-01", effect.companionTurnParentId)
        assertEquals("turn-edit-companion-smoke-01", effect.newActiveCompanionMessageId)
    }
}
