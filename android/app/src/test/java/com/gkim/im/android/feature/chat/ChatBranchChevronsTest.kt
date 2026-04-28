package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.CompanionTurnMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §3.1 verification — every companion bubble whose variantGroupId has more than one sibling
 * renders left/right swipe chevrons plus an `n/total` caption, and tapping a chevron emits
 * the new active-path index for that variantGroupId so the conversation can re-resolve.
 *
 * Tests drive the pure helpers ([chatBubbleVariantNavigation], [resolveVariantSelection]) that
 * back the composable's chevron-rendering and click-handler. The composable renders the chevron
 * row when [chatBubbleVariantNavigation] returns non-null, and each chevron tap delegates the
 * (variantGroupId, newIndex) payload computed by [resolveVariantSelection] to the
 * onSelectVariantAt callback — so asserting the helpers' contract implicitly covers the
 * rendering path and the active-path mutation hand-off without standing up Compose.
 */
class ChatBranchChevronsTest {

    private fun meta(
        variantGroupId: String = "group-default",
        siblingCount: Int = 1,
        siblingActiveIndex: Int = 0,
        variantIndex: Int = siblingActiveIndex,
    ): CompanionTurnMeta = CompanionTurnMeta(
        turnId = "turn-$variantGroupId-$siblingActiveIndex",
        variantGroupId = variantGroupId,
        variantIndex = variantIndex,
        siblingCount = siblingCount,
        siblingActiveIndex = siblingActiveIndex,
    )

    // -------------------------------------------------------------------------
    // Chevron visibility — non-companion + single-variant suppress the chevron row
    // -------------------------------------------------------------------------

    @Test
    fun `non-companion bubble (null meta) suppresses the chevron row`() {
        assertNull(chatBubbleVariantNavigation(null))
    }

    @Test
    fun `companion bubble with siblingCount of one suppresses the chevron row`() {
        assertNull(chatBubbleVariantNavigation(meta(siblingCount = 1)))
    }

    @Test
    fun `companion bubble with zero siblings suppresses the chevron row`() {
        assertNull(chatBubbleVariantNavigation(meta(siblingCount = 0)))
    }

    // -------------------------------------------------------------------------
    // Chevron visibility — multi-sibling groups always render, regardless of position
    // -------------------------------------------------------------------------

    @Test
    fun `companion bubble with two siblings renders the chevron row`() {
        val nav = chatBubbleVariantNavigation(meta(siblingCount = 2, siblingActiveIndex = 0))
        assertEquals("1/2", nav?.indicator)
        assertEquals(2, nav?.total)
        assertTrue(nav?.hasNext == true)
        assertFalse(nav?.hasPrevious == true)
    }

    @Test
    fun `companion bubble with three siblings at middle index renders both chevrons`() {
        val nav = chatBubbleVariantNavigation(meta(siblingCount = 3, siblingActiveIndex = 1))!!
        assertEquals("2/3", nav.indicator)
        assertTrue(nav.hasPrevious)
        assertTrue(nav.hasNext)
    }

    @Test
    fun `companion bubble at terminal index disables only the next chevron`() {
        val nav = chatBubbleVariantNavigation(meta(siblingCount = 3, siblingActiveIndex = 2))!!
        assertEquals("3/3", nav.indicator)
        assertTrue(nav.hasPrevious)
        assertFalse(nav.hasNext)
    }

    // -------------------------------------------------------------------------
    // Caption format — `n/total` derived from clamped active index
    // -------------------------------------------------------------------------

    @Test
    fun `caption uses 1-based numerator and clamps activeIndex into range`() {
        val low = chatBubbleVariantNavigation(meta(siblingCount = 4, siblingActiveIndex = -3))!!
        assertEquals("1/4", low.indicator)
        assertEquals(0, low.activeIndex)

        val high = chatBubbleVariantNavigation(meta(siblingCount = 4, siblingActiveIndex = 99))!!
        assertEquals("4/4", high.indicator)
        assertEquals(3, high.activeIndex)
    }

    // -------------------------------------------------------------------------
    // Active-path mutation — chevron tap emits (variantGroupId, newIndex)
    // -------------------------------------------------------------------------

    @Test
    fun `previous chevron tap emits activeIndex minus one for the bubble's variantGroupId`() {
        val target = meta(variantGroupId = "group-prev", siblingCount = 3, siblingActiveIndex = 2)
        val selection = resolveVariantSelection(target, VariantSwipeDirection.Previous)
        assertEquals("group-prev" to 1, selection)
    }

    @Test
    fun `next chevron tap emits activeIndex plus one for the bubble's variantGroupId`() {
        val target = meta(variantGroupId = "group-next", siblingCount = 3, siblingActiveIndex = 0)
        val selection = resolveVariantSelection(target, VariantSwipeDirection.Next)
        assertEquals("group-next" to 1, selection)
    }

    @Test
    fun `previous chevron tap at index zero emits no selection (boundary)`() {
        val target = meta(variantGroupId = "group-zero", siblingCount = 3, siblingActiveIndex = 0)
        assertNull(resolveVariantSelection(target, VariantSwipeDirection.Previous))
    }

    @Test
    fun `next chevron tap at terminal index emits no selection (boundary)`() {
        val target = meta(variantGroupId = "group-terminal", siblingCount = 3, siblingActiveIndex = 2)
        assertNull(resolveVariantSelection(target, VariantSwipeDirection.Next))
    }

    @Test
    fun `non-companion bubble (null meta) emits no selection in either direction`() {
        assertNull(resolveVariantSelection(null, VariantSwipeDirection.Previous))
        assertNull(resolveVariantSelection(null, VariantSwipeDirection.Next))
    }

    @Test
    fun `single-variant bubble emits no selection in either direction`() {
        val solo = meta(variantGroupId = "group-solo", siblingCount = 1, siblingActiveIndex = 0)
        assertNull(resolveVariantSelection(solo, VariantSwipeDirection.Previous))
        assertNull(resolveVariantSelection(solo, VariantSwipeDirection.Next))
    }

    // -------------------------------------------------------------------------
    // Callback shape — onSelectVariantAt receives exactly the payload built by resolveVariantSelection
    // -------------------------------------------------------------------------

    @Test
    fun `onSelectVariantAt callback receives exactly the (variantGroupId, newIndex) pair from resolveVariantSelection`() {
        val target = meta(variantGroupId = "group-callback", siblingCount = 4, siblingActiveIndex = 1)
        val captured = mutableListOf<Pair<String, Int>>()
        val onSelectVariantAt: (String, Int) -> Unit = { groupId, newIndex ->
            captured += groupId to newIndex
        }

        // Simulate the composable's prev / next click handlers in turn.
        resolveVariantSelection(target, VariantSwipeDirection.Previous)
            ?.let { (groupId, newIndex) -> onSelectVariantAt(groupId, newIndex) }
        resolveVariantSelection(target, VariantSwipeDirection.Next)
            ?.let { (groupId, newIndex) -> onSelectVariantAt(groupId, newIndex) }

        assertEquals(
            listOf(
                "group-callback" to 0,
                "group-callback" to 2,
            ),
            captured,
        )
    }
}
