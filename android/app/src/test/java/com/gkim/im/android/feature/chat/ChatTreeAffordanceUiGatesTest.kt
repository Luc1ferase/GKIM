package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.CompanionTurnMeta
import com.gkim.im.android.core.model.Conversation
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §5.1 / §5.2 verification — visibility gates for the Edit user-bubble overflow and the
 * Regenerate-from-here companion-bubble overflow. The ChatMessageRow composable consults
 * these helpers per render; their tests pin the gating contract independently of Compose.
 *
 * §5.3 (ChatRoute → ChatViewModel wiring) is verified via the §4.1 / §4.2 test suites
 * (`ChatViewModelEditUserTurnTest`, `ChatViewModelRegenerateFromHereTest`) which exercise
 * the handler entry-points the ChatRoute callbacks delegate to. The ChatRoute wiring itself
 * is mostly a thin pass-through.
 */
class ChatTreeAffordanceUiGatesTest {

    private val companionConversation = Conversation(
        id = "room-c",
        contactId = "contact-c",
        contactName = "Companion",
        contactTitle = "Title",
        avatarText = "DL",
        lastMessage = "",
        lastTimestamp = "2026-04-26T12:00:00Z",
        unreadCount = 0,
        isOnline = true,
        messages = emptyList(),
        companionCardId = "companion-card-id",
    )

    private val peerConversation = companionConversation.copy(companionCardId = null)

    private fun userBubble(
        body: String = "hello",
        parentMessageId: String? = "msg-parent",
    ): ChatMessage = ChatMessage(
        id = "user-msg-1",
        direction = MessageDirection.Outgoing,
        kind = MessageKind.Text,
        body = body,
        createdAt = "2026-04-26T12:00:00Z",
        parentMessageId = parentMessageId,
        status = MessageStatus.Completed,
    )

    private fun companionBubble(
        status: MessageStatus = MessageStatus.Completed,
        withMeta: Boolean = true,
    ): ChatMessage = ChatMessage(
        id = "companion-msg-1",
        direction = MessageDirection.Incoming,
        kind = MessageKind.Text,
        body = "reply",
        createdAt = "2026-04-26T12:00:01Z",
        status = status,
        companionTurnMeta = if (withMeta) CompanionTurnMeta(
            turnId = "turn-1",
            variantGroupId = "vg-1",
            variantIndex = 0,
        ) else null,
    )

    // -------------------------------------------------------------------------
    // §5.1 — shouldShowUserBubbleEdit
    // -------------------------------------------------------------------------

    @Test
    fun `shouldShowUserBubbleEdit true on user bubble with parent in companion conversation`() {
        assertTrue(shouldShowUserBubbleEdit(userBubble(), companionConversation))
    }

    @Test
    fun `shouldShowUserBubbleEdit false on incoming bubble`() {
        val incoming = companionBubble()
        assertFalse(shouldShowUserBubbleEdit(incoming, companionConversation))
    }

    @Test
    fun `shouldShowUserBubbleEdit false when parentMessageId is null (root user message)`() {
        assertFalse(shouldShowUserBubbleEdit(userBubble(parentMessageId = null), companionConversation))
    }

    @Test
    fun `shouldShowUserBubbleEdit false in peer conversation (no companion card)`() {
        assertFalse(shouldShowUserBubbleEdit(userBubble(), peerConversation))
    }

    @Test
    fun `shouldShowUserBubbleEdit false when conversation is null`() {
        assertFalse(shouldShowUserBubbleEdit(userBubble(), null))
    }

    // -------------------------------------------------------------------------
    // §5.2 — shouldShowRegenerateFromHere
    // -------------------------------------------------------------------------

    @Test
    fun `shouldShowRegenerateFromHere true on completed companion bubble`() {
        assertTrue(shouldShowRegenerateFromHere(companionBubble(status = MessageStatus.Completed)))
    }

    @Test
    fun `shouldShowRegenerateFromHere true on failed companion bubble (retry semantically equivalent)`() {
        assertTrue(shouldShowRegenerateFromHere(companionBubble(status = MessageStatus.Failed)))
    }

    @Test
    fun `shouldShowRegenerateFromHere true on timeout companion bubble`() {
        assertTrue(shouldShowRegenerateFromHere(companionBubble(status = MessageStatus.Timeout)))
    }

    @Test
    fun `shouldShowRegenerateFromHere true on blocked companion bubble`() {
        assertTrue(shouldShowRegenerateFromHere(companionBubble(status = MessageStatus.Blocked)))
    }

    @Test
    fun `shouldShowRegenerateFromHere false while companion bubble is mid-flight (Thinking)`() {
        assertFalse(shouldShowRegenerateFromHere(companionBubble(status = MessageStatus.Thinking)))
    }

    @Test
    fun `shouldShowRegenerateFromHere false while companion bubble is mid-flight (Streaming)`() {
        assertFalse(shouldShowRegenerateFromHere(companionBubble(status = MessageStatus.Streaming)))
    }

    @Test
    fun `shouldShowRegenerateFromHere false on outgoing user bubble`() {
        assertFalse(shouldShowRegenerateFromHere(userBubble()))
    }

    @Test
    fun `shouldShowRegenerateFromHere false on incoming bubble without companionTurnMeta`() {
        assertFalse(shouldShowRegenerateFromHere(companionBubble(withMeta = false)))
    }

    // -------------------------------------------------------------------------
    // §5.3 — TreeAffordanceLifecycle shape contract
    // -------------------------------------------------------------------------

    @Test
    fun `TreeAffordanceLifecycle defaults to all-null (no banner shown)`() {
        val lifecycle = TreeAffordanceLifecycle()
        // Banner-visibility predicates the ChatScreen banner block reads:
        assertEquals(null, lifecycle.inFlightForMessageId)
        assertEquals(null, lifecycle.failedForMessageId)
        assertEquals(null, lifecycle.failureReason)
    }

    @Test
    fun `TreeAffordanceLifecycle inFlight shows the in-flight banner`() {
        val lifecycle = TreeAffordanceLifecycle(inFlightForMessageId = "user-msg-7")
        assertEquals("user-msg-7", lifecycle.inFlightForMessageId)
        assertEquals(null, lifecycle.failedForMessageId)
    }

    @Test
    fun `TreeAffordanceLifecycle failed carries reason for inline error banner`() {
        val lifecycle = TreeAffordanceLifecycle(
            failedForMessageId = "user-msg-7",
            failureReason = "network_timeout",
        )
        assertEquals("user-msg-7", lifecycle.failedForMessageId)
        assertEquals("network_timeout", lifecycle.failureReason)
        assertEquals(null, lifecycle.inFlightForMessageId)
    }
}
