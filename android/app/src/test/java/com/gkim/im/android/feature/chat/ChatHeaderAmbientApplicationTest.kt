package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.designsystem.AmbientGlowAnchor
import com.gkim.im.android.core.designsystem.ChatHeaderAmbient
import com.gkim.im.android.core.designsystem.ChromeSurfacesWithAmbient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatHeaderAmbientApplicationTest {

    @Test
    fun `chat top-bar does not apply grain`() {
        // Per spec, only the tavern home gets the grain overlay in this
        // slice. The chat header is glow-only.
        assertFalse(ChatHeaderAmbient.grain)
    }

    @Test
    fun `chat top-bar glow anchor is TopStart`() {
        assertEquals(AmbientGlowAnchor.TopStart, ChatHeaderAmbient.glowAnchor)
    }

    @Test
    fun `chat top-bar is targeted by surface testTag`() {
        assertEquals("chat-top-bar", ChatHeaderAmbient.surfaceTestTag)
    }

    @Test
    fun `chat top-bar appears in chrome ambient manifest`() {
        assertTrue(ChatHeaderAmbient in ChromeSurfacesWithAmbient)
    }

    @Test
    fun `no other chat surface opts in to ambient in this slice`() {
        // Anti-regression: only the chat top-bar applies a glow in this
        // slice. The chat content area, message bubbles, and input bar
        // must remain ambient-free so the bar metaphor stays restrained.
        val chatSurfaces = ChromeSurfacesWithAmbient
            .filter { it.surfaceTestTag.startsWith("chat-") }
        assertEquals(1, chatSurfaces.size)
        assertEquals("chat-top-bar", chatSurfaces.single().surfaceTestTag)
    }

    @Test
    fun `chrome ambient manifest holds exactly two surfaces`() {
        // Tavern home + chat top-bar. Adding any third surface to this
        // list must come with an explicit follow-up requirement per
        // tavern-visual-direction §"ambient layer is bounded".
        assertEquals(2, ChromeSurfacesWithAmbient.size)
    }
}
