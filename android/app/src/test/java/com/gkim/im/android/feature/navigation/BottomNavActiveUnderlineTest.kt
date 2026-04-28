package com.gkim.im.android.feature.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavActiveUnderlineTest {

    @Test
    fun `active underline thickness is 2 dp`() {
        assertEquals(2, BottomNavActiveUnderlineThicknessDp)
    }

    @Test
    fun `underline thickness stays warm-fixture-thin`() {
        // Anti-regression: thicker than 2 dp would read as a "selected pill"
        // again rather than a brass-fixture underline.
        assertTrue(
            "underline must remain <= 4 dp to read as a fixture line",
            BottomNavActiveUnderlineThicknessDp <= 4,
        )
        assertTrue(
            "underline must remain >= 1 dp to be visible at common densities",
            BottomNavActiveUnderlineThicknessDp >= 1,
        )
    }
}
