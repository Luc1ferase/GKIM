package com.gkim.im.android.feature.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagesNewConversationFabTest {

    @Test
    fun `fab tap target is 48 dp square`() {
        assertEquals(48, MessagesNewConversationFabSizeDp)
    }

    @Test
    fun `fab corner radius is 12 dp rectangular`() {
        // R3.3 promises a rectangular reframe; 12 dp corners stay << half
        // the side, so the shape reads as a rectangle rather than a pill.
        assertEquals(12, MessagesNewConversationFabRadiusDp)
        assertTrue(
            "corner radius must be < half the side to read as rectangular",
            MessagesNewConversationFabRadiusDp < MessagesNewConversationFabSizeDp / 2,
        )
    }

    @Test
    fun `fab does not regress to a pill geometry`() {
        // The previous pill-style FAB used corner radius 999 dp on a smaller
        // tap target. Anti-regression: the new geometry must NOT match that.
        assertTrue(MessagesNewConversationFabRadiusDp != 999)
        assertTrue(MessagesNewConversationFabSizeDp >= 48)
    }
}
