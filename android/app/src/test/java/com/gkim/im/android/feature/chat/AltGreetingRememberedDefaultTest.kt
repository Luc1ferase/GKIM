package com.gkim.im.android.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * §2.2 verification — the opener picker remembers the last-selected greeting per companion
 * and defaults the highlight to that greeting on the next render (after a relationship reset
 * or a fresh conversation). When the remembered index is stale (option was removed, or index
 * is out-of-range for a new, shorter options list), the picker falls back gracefully to the
 * first option rather than crashing or hiding all options.
 *
 * The "Remembered from last time" caption is rendered when the remembered index is non-null
 * AND equals the default-highlight index. Tests below drive [defaultSelectionIndex] directly;
 * the caller condition (`lastSelected != null && lastSelected == defaultSelectionIndex(...)`)
 * is the same two-value check expressed in the composable.
 */
class AltGreetingRememberedDefaultTest {

    private fun options(count: Int): List<CompanionGreetingOption> =
        (0 until count).map { i ->
            CompanionGreetingOption(
                index = i,
                label = if (i == 0) "Greeting" else "Alt $i",
                body = "Greeting body $i",
            )
        }

    // -------------------------------------------------------------------------
    // Empty options → null default
    // -------------------------------------------------------------------------

    @Test
    fun `empty options yields null default regardless of remembered index`() {
        assertNull(defaultSelectionIndex(options = emptyList(), lastSelected = null))
        assertNull(defaultSelectionIndex(options = emptyList(), lastSelected = 0))
        assertNull(defaultSelectionIndex(options = emptyList(), lastSelected = 42))
    }

    // -------------------------------------------------------------------------
    // No memory → fall back to first option
    // -------------------------------------------------------------------------

    @Test
    fun `null remembered index defaults to first option when options exist`() {
        assertEquals(0, defaultSelectionIndex(options = options(1), lastSelected = null))
        assertEquals(0, defaultSelectionIndex(options = options(3), lastSelected = null))
    }

    // -------------------------------------------------------------------------
    // Valid remembered index is honored
    // -------------------------------------------------------------------------

    @Test
    fun `remembered index in range is honored as the default`() {
        assertEquals(0, defaultSelectionIndex(options = options(3), lastSelected = 0))
        assertEquals(1, defaultSelectionIndex(options = options(3), lastSelected = 1))
        assertEquals(2, defaultSelectionIndex(options = options(3), lastSelected = 2))
    }

    // -------------------------------------------------------------------------
    // Stale / out-of-range remembered index → fall back to first option
    // -------------------------------------------------------------------------

    @Test
    fun `remembered index past end falls back to first option (stale memory)`() {
        // User previously selected alt-greeting #2 (index 2), but the card has been edited
        // to remove alternates, leaving only firstMes (one option at index 0).
        assertEquals(0, defaultSelectionIndex(options = options(1), lastSelected = 2))
    }

    @Test
    fun `remembered index equal to size falls back to first option`() {
        // Off-by-one edge: size=3 means valid indices are 0..2. Remembered=3 is stale.
        assertEquals(0, defaultSelectionIndex(options = options(3), lastSelected = 3))
    }

    @Test
    fun `negative remembered index falls back to first option`() {
        assertEquals(0, defaultSelectionIndex(options = options(3), lastSelected = -1))
        assertEquals(0, defaultSelectionIndex(options = options(3), lastSelected = -42))
    }

    // -------------------------------------------------------------------------
    // "Remembered" caption condition — reflects the composable's derivation
    // -------------------------------------------------------------------------

    @Test
    fun `remembered caption is shown only when lastSelected matches the default`() {
        // Composable shows caption when: lastSelected != null && defaultSelectionIndex(...) == lastSelected.
        fun captionShown(opts: List<CompanionGreetingOption>, lastSelected: Int?): Boolean {
            val d = defaultSelectionIndex(opts, lastSelected)
            return lastSelected != null && d == lastSelected
        }

        // valid match: shown
        assertEquals(true, captionShown(options(3), lastSelected = 1))

        // null remembered: not shown (first-option default has no "remembered" provenance)
        assertEquals(false, captionShown(options(3), lastSelected = null))

        // stale remembered falls back to 0, so lastSelected=2 != default=0 → not shown
        assertEquals(false, captionShown(options(1), lastSelected = 2))

        // empty options: not shown
        assertEquals(false, captionShown(emptyList(), lastSelected = 0))
    }

    // -------------------------------------------------------------------------
    // Round-trip: selecting an option and using it as remembered yields the same default
    // -------------------------------------------------------------------------

    @Test
    fun `persisting a selection and reloading yields the same default highlight`() {
        val opts = options(4)
        val selectedIndex = 2 // user commits alt-greeting #2

        // First-render default (before any selection) points at index 0.
        assertEquals(0, defaultSelectionIndex(opts, lastSelected = null))

        // After committing, composable's caller persists selectedIndex. On reload:
        val persisted: Int? = selectedIndex
        assertEquals(selectedIndex, defaultSelectionIndex(opts, lastSelected = persisted))
    }
}
