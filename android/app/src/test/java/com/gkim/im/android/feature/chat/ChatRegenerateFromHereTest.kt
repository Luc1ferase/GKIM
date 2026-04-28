package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.CompanionTurnMeta
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.MessageStatus
import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import com.gkim.im.android.data.remote.im.RegenerateAtRequestDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * §3.3 verification — the "Regenerate from here" overflow on every companion bubble (not only
 * the latest) calls `POST /api/companion-turns/{conversationId}/regenerate-at` with
 * `{ clientTurnId, targetMessageId = bubble.messageId }`. The response carries a new sibling
 * under the same `variantGroupId` with the next available `variantIndex`; the UI switches the
 * active path to that new sibling.
 *
 * Tests drive the pure helpers ([regenerateFromHereRequest],
 * [regenerateFromHereActivePathEffect]) that back the overflow's click handler and the
 * post-response active-path mutation. The composable's overflow tap →
 * regenerateFromHereRequest, and onResponse → regenerateFromHereActivePathEffect; asserting
 * the helpers' contracts implicitly covers the endpoint call shape, the sibling creation
 * (verified through the response shape), and the path switch — without standing up Compose.
 */
class ChatRegenerateFromHereTest {

    private fun companionBubble(
        id: String = "companion-msg-1",
        body: String = "ready",
        parentMessageId: String? = "user-msg-1",
        variantGroupId: String = "vgroup-companion-1",
        variantIndex: Int = 0,
        siblingCount: Int = 1,
        status: MessageStatus = MessageStatus.Completed,
    ): ChatMessage = ChatMessage(
        id = id,
        direction = MessageDirection.Incoming,
        kind = MessageKind.Text,
        body = body,
        createdAt = "2026-04-25T12:00:01Z",
        status = status,
        parentMessageId = parentMessageId,
        companionTurnMeta = CompanionTurnMeta(
            turnId = id,
            variantGroupId = variantGroupId,
            variantIndex = variantIndex,
            siblingCount = siblingCount,
            siblingActiveIndex = variantIndex,
        ),
    )

    private fun userBubble(
        id: String = "user-msg-1",
        body: String = "hi",
    ): ChatMessage = ChatMessage(
        id = id,
        direction = MessageDirection.Outgoing,
        kind = MessageKind.Text,
        body = body,
        createdAt = "2026-04-25T12:00:00Z",
    )

    private fun systemBubble(
        id: String = "sys-msg-1",
        body: String = "system note",
    ): ChatMessage = ChatMessage(
        id = id,
        direction = MessageDirection.System,
        kind = MessageKind.Text,
        body = body,
        createdAt = "2026-04-25T12:00:00Z",
    )

    private fun newSiblingResponse(
        turnId: String = "turn-regen-at-smoke-01",
        variantGroupId: String = "vgroup-companion-1",
        variantIndex: Int = 1,
        parentMessageId: String? = "user-msg-1",
        status: String = "thinking",
    ): CompanionTurnRecordDto = CompanionTurnRecordDto(
        turnId = turnId,
        conversationId = "conversation-daylight-listener-smoke",
        messageId = turnId,
        variantGroupId = variantGroupId,
        variantIndex = variantIndex,
        parentMessageId = parentMessageId,
        status = status,
        accumulatedBody = "",
        lastDeltaSeq = 0,
        startedAt = "2026-04-25T12:00:05Z",
    )

    // -------------------------------------------------------------------------
    // regenerateFromHereRequest — gating + endpoint call shape
    // -------------------------------------------------------------------------

    @Test
    fun `regenerateFromHereRequest returns null for an outgoing user bubble`() {
        assertNull(regenerateFromHereRequest(userBubble(), clientTurnId = "client-x"))
    }

    @Test
    fun `regenerateFromHereRequest returns null for a system bubble (no companionTurnMeta)`() {
        assertNull(regenerateFromHereRequest(systemBubble(), clientTurnId = "client-x"))
    }

    @Test
    fun `regenerateFromHereRequest returns null for an incoming bubble that lacks companionTurnMeta`() {
        // A non-companion direct-message contact bubble would be Incoming but with no companion meta.
        val plain = ChatMessage(
            id = "plain-incoming-1",
            direction = MessageDirection.Incoming,
            kind = MessageKind.Text,
            body = "from a friend",
            createdAt = "2026-04-25T12:00:00Z",
        )
        assertNull(regenerateFromHereRequest(plain, clientTurnId = "client-x"))
    }

    @Test
    fun `regenerateFromHereRequest builds the wire DTO with the bubble's messageId as targetMessageId`() {
        val request = regenerateFromHereRequest(
            bubble = companionBubble(id = "companion-msg-7"),
            clientTurnId = "client-turn-regenerate-at-smoke-01",
        )
        assertEquals(
            RegenerateAtRequestDto(
                clientTurnId = "client-turn-regenerate-at-smoke-01",
                targetMessageId = "companion-msg-7",
            ),
            request,
        )
    }

    @Test
    fun `regenerateFromHereRequest carries the caller's clientTurnId verbatim`() {
        val request = regenerateFromHereRequest(
            bubble = companionBubble(id = "companion-msg-1"),
            clientTurnId = "client-turn-from-caller-side",
        )
        assertEquals("client-turn-from-caller-side", request?.clientTurnId)
    }

    // -------------------------------------------------------------------------
    // Mid-conversation regenerate — every companion bubble, not only the latest
    // -------------------------------------------------------------------------

    @Test
    fun `regenerateFromHereRequest works on a mid-conversation bubble (turn 2 of 5)`() {
        // The proposal explicitly says "every companion bubble (not just the latest)" — assert
        // a non-latest bubble still produces a valid request without any "is this the most
        // recent" gating. The helper depends only on direction + companionTurnMeta presence.
        val midConversationBubble = companionBubble(
            id = "turn-2-of-5",
            variantGroupId = "vgroup-mid-conversation",
            variantIndex = 0,
            siblingCount = 1,
        )
        val request = regenerateFromHereRequest(
            bubble = midConversationBubble,
            clientTurnId = "client-turn-mid-regen",
        )
        assertEquals("turn-2-of-5", request?.targetMessageId)
        assertEquals("client-turn-mid-regen", request?.clientTurnId)
    }

    @Test
    fun `regenerateFromHereRequest works on a bubble that already carries multiple siblings`() {
        // After a prior regenerate, a bubble can land with siblingCount > 1. Regenerate from
        // the active sibling at index 2 in a 3-sibling group; the new sibling will become
        // index 3.
        val activeMidSibling = companionBubble(
            id = "active-sibling-at-index-2",
            variantGroupId = "vgroup-multi",
            variantIndex = 2,
            siblingCount = 3,
        )
        val request = regenerateFromHereRequest(
            bubble = activeMidSibling,
            clientTurnId = "client-turn-from-multi-sibling",
        )
        assertNotNull(request)
        assertEquals("active-sibling-at-index-2", request!!.targetMessageId)
    }

    // -------------------------------------------------------------------------
    // regenerateFromHereActivePathEffect — sibling creation + active-path switch
    // -------------------------------------------------------------------------

    @Test
    fun `regenerateFromHereActivePathEffect projects the new sibling onto its variant group`() {
        val response = newSiblingResponse(
            turnId = "turn-regen-at-smoke-01",
            variantGroupId = "vgroup-companion-1",
            variantIndex = 1,
        )
        val effect = regenerateFromHereActivePathEffect(response)
        assertEquals("vgroup-companion-1", effect.variantGroupId)
        assertEquals("turn-regen-at-smoke-01", effect.newActiveMessageId)
        assertEquals(1, effect.newActiveVariantIndex)
    }

    @Test
    fun `regenerateFromHereActivePathEffect honors the variantIndex the server allocated (max+1 in group)`() {
        // Backend semantic: new sibling lands at variantIndex = max(existing) + 1 within the
        // same variantGroupId. The helper just lifts the integer the server reports — we don't
        // re-compute it on the client.
        val response = newSiblingResponse(variantIndex = 4)
        val effect = regenerateFromHereActivePathEffect(response)
        assertEquals(4, effect.newActiveVariantIndex)
    }

    // -------------------------------------------------------------------------
    // End-to-end happy path — request → response → effect
    // -------------------------------------------------------------------------

    @Test
    fun `end-to-end happy path — mid-conversation regenerate switches active path to the new sibling`() {
        // 1. User taps "Regenerate from here" on a mid-conversation companion bubble at
        //    variantIndex=0 in a group of 1.
        val target = companionBubble(
            id = "active-sibling-mid-conv",
            variantGroupId = "vgroup-mid-active",
            variantIndex = 0,
            siblingCount = 1,
        )

        // 2. The click handler builds the wire DTO with the bubble's messageId.
        val request = regenerateFromHereRequest(target, clientTurnId = "client-turn-regen-end-to-end")!!
        assertEquals("active-sibling-mid-conv", request.targetMessageId)

        // 3. The server returns a new sibling under the same variantGroupId at variantIndex=1.
        val response = newSiblingResponse(
            turnId = "new-sibling-turn-id",
            variantGroupId = "vgroup-mid-active",
            variantIndex = 1,
            parentMessageId = "user-msg-1",
        )

        // 4. The active-path applier flips the group's active variant to the new sibling.
        val effect = regenerateFromHereActivePathEffect(response)
        assertEquals("vgroup-mid-active", effect.variantGroupId)
        assertEquals("new-sibling-turn-id", effect.newActiveMessageId)
        assertEquals(1, effect.newActiveVariantIndex)

        // 5. The chevron-rendering helper from §3.1 (chatBubbleVariantNavigation) then sees a
        //    siblingCount=2 / siblingActiveIndex=1 group on the next render and shows
        //    chevrons + "2/2" indicator on the new sibling — verified by the §3.1 test suite,
        //    so we don't re-assert it here.
    }
}
