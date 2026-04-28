package com.gkim.im.android.core.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AvatarFallbackTest {

    @Test
    fun `tavern card avatar shape is rounded-square at 12 dp radius`() {
        assertEquals(RoundedCornerShape(12.dp), TavernCardAvatarShape)
    }

    @Test
    fun `chat avatar shape is circular`() {
        assertEquals(CircleShape, ChatAvatarShape)
    }

    @Test
    fun `tavern and chat avatar shapes are distinct`() {
        // Belt-and-suspenders: tavern cards must NOT regress to a circle and
        // chat avatars must NOT regress to a rounded-square. The R3.2 spec
        // explicitly preserves the surrounding shape per call site.
        assertNotEquals(TavernCardAvatarShape, ChatAvatarShape)
    }
}
