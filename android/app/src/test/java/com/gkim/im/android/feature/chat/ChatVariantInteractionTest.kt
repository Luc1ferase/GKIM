package com.gkim.im.android.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatVariantInteractionTest {
    @Test
    fun `single-sibling group suppresses variant navigation`() {
        assertNull(variantNavigationState(variantGroupSiblingCount = 1, activeIndex = 0))
    }

    @Test
    fun `zero-sibling group suppresses variant navigation`() {
        assertNull(variantNavigationState(variantGroupSiblingCount = 0, activeIndex = 0))
    }

    @Test
    fun `two-sibling group at index zero allows next but not previous`() {
        val state = variantNavigationState(variantGroupSiblingCount = 2, activeIndex = 0)!!
        assertEquals("1/2", state.indicator)
        assertTrue(state.hasNext)
        assertFalse(state.hasPrevious)
        assertEquals(0, state.activeIndex)
        assertEquals(2, state.total)
    }

    @Test
    fun `three-sibling group at middle index allows both directions`() {
        val state = variantNavigationState(variantGroupSiblingCount = 3, activeIndex = 1)!!
        assertEquals("2/3", state.indicator)
        assertTrue(state.hasNext)
        assertTrue(state.hasPrevious)
    }

    @Test
    fun `three-sibling group at terminal index blocks next`() {
        val state = variantNavigationState(variantGroupSiblingCount = 3, activeIndex = 2)!!
        assertEquals("3/3", state.indicator)
        assertFalse(state.hasNext)
        assertTrue(state.hasPrevious)
    }

    @Test
    fun `out-of-range active index clamps to valid range`() {
        val low = variantNavigationState(variantGroupSiblingCount = 3, activeIndex = -5)!!
        assertEquals(0, low.activeIndex)
        assertEquals("1/3", low.indicator)

        val high = variantNavigationState(variantGroupSiblingCount = 3, activeIndex = 99)!!
        assertEquals(2, high.activeIndex)
        assertEquals("3/3", high.indicator)
    }
}
