package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.Conversation
import com.gkim.im.android.core.model.MessageDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * §1.2 verification — tapping an avatar on any of three surfaces (tavern card row, chat
 * header, chat bubble) resolves to the same `tavern/portrait/{characterId}` route. The
 * routing contract is captured as three pure resolver functions, one per surface, so each
 * surface's "given context, what route" decision is exercised in isolation.
 */
class PortraitTapRoutingTest {

    private fun conversation(
        companionCardId: String? = null,
    ): Conversation = Conversation(
        id = "conv-1",
        contactId = "companion-1",
        contactName = "Daylight Listener",
        contactTitle = "Tester",
        avatarText = "DL",
        lastMessage = "hi",
        lastTimestamp = "2026-04-25T00:00:00Z",
        unreadCount = 0,
        isOnline = true,
        messages = emptyList<ChatMessage>(),
        companionCardId = companionCardId,
    )

    // -------------------------------------------------------------------------
    // Surface 1: tavern card row
    // -------------------------------------------------------------------------

    @Test
    fun `tavern card row avatar tap routes to portrait with the card id`() {
        assertEquals(
            "tavern/portrait/daylight-listener",
            portraitTapRouteForTavernCard("daylight-listener"),
        )
    }

    @Test
    fun `tavern card row resolver respects arbitrary id shapes`() {
        assertEquals(
            "tavern/portrait/user-authored_42",
            portraitTapRouteForTavernCard("user-authored_42"),
        )
    }

    // -------------------------------------------------------------------------
    // Surface 2: chat header
    // -------------------------------------------------------------------------

    @Test
    fun `chat header avatar tap routes to portrait when conversation has companionCardId`() {
        assertEquals(
            "tavern/portrait/daylight-listener",
            portraitTapRouteForChatHeader(conversation(companionCardId = "daylight-listener")),
        )
    }

    @Test
    fun `chat header avatar tap is noop when conversation has no companionCardId`() {
        assertNull(portraitTapRouteForChatHeader(conversation(companionCardId = null)))
    }

    @Test
    fun `chat header avatar tap is noop when conversation is null`() {
        assertNull(portraitTapRouteForChatHeader(conversation = null))
    }

    // -------------------------------------------------------------------------
    // Surface 3: chat bubble
    // -------------------------------------------------------------------------

    @Test
    fun `chat bubble avatar tap routes to portrait with the conversation's companionCardId`() {
        assertEquals(
            "tavern/portrait/daylight-listener",
            portraitTapRouteForChatBubble(conversation(companionCardId = "daylight-listener")),
        )
    }

    @Test
    fun `chat bubble avatar tap is noop for non-companion conversations`() {
        assertNull(portraitTapRouteForChatBubble(conversation(companionCardId = null)))
    }

    @Test
    fun `chat bubble avatar tap is noop when conversation is null`() {
        assertNull(portraitTapRouteForChatBubble(conversation = null))
    }

    // -------------------------------------------------------------------------
    // All three surfaces converge on the same route pattern
    // -------------------------------------------------------------------------

    @Test
    fun `all three surfaces produce the same route for the same card id`() {
        val cardId = "daylight-listener"
        val expected = "tavern/portrait/$cardId"
        val conv = conversation(companionCardId = cardId)
        assertEquals(expected, portraitTapRouteForTavernCard(cardId))
        assertEquals(expected, portraitTapRouteForChatHeader(conv))
        assertEquals(expected, portraitTapRouteForChatBubble(conv))
    }

    @Test
    fun `MessageDirection is orthogonal to bubble route resolution`() {
        // The resolver does not inspect direction; the composable decides whether the
        // incoming-avatar Box gets the clickable. Here we just verify that the resolver
        // shape doesn't change based on which message is in play.
        val conv = conversation(companionCardId = "daylight-listener")
        val expected = "tavern/portrait/daylight-listener"
        @Suppress("UNUSED_VARIABLE")
        val dir: MessageDirection = MessageDirection.Incoming
        assertEquals(expected, portraitTapRouteForChatBubble(conv))
    }
}
