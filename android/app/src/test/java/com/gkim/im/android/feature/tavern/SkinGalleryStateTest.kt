package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.SkinRarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinGalleryStateTest {

    @Test
    fun `gallery cell states are exactly the closed three-state set`() {
        // R3.2 contract: every cell renders exactly one of these three.
        // No fourth state may appear without a corresponding spec.
        assertEquals(
            setOf(
                GalleryCellState.OwnedActive,
                GalleryCellState.OwnedInactive,
                GalleryCellState.Locked,
            ),
            GalleryCellState.values().toSet(),
        )
    }

    @Test
    fun `every state has a render spec`() {
        for (state in GalleryCellState.values()) {
            galleryCellSpec(state) // throws if missing
        }
        assertEquals(
            "There must be exactly 3 specs, one per state",
            3,
            GalleryCellSpecs.size,
        )
    }

    @Test
    fun `OwnedActive renders 2dp brass border + 1dp ember inner ring + active caption color`() {
        val spec = galleryCellSpec(GalleryCellState.OwnedActive)
        assertEquals(2, spec.borderDp)
        assertEquals("primary", spec.borderColorToken)
        assertTrue("active cell must show inner ember ring", spec.showInnerEmberRing)
        assertEquals(1.0f, spec.opacity, 0.0001f)
        assertFalse("active cell never shows the lock icon", spec.showLockIcon)
        assertTrue("active cell renders the actual thumb", spec.showsActualThumb)
        assertEquals("primary", spec.captionColorToken)
    }

    @Test
    fun `OwnedInactive renders 1dp outlineVariant border without ember ring`() {
        val spec = galleryCellSpec(GalleryCellState.OwnedInactive)
        assertEquals(1, spec.borderDp)
        assertEquals("outlineVariant", spec.borderColorToken)
        assertFalse(spec.showInnerEmberRing)
        assertEquals(1.0f, spec.opacity, 0.0001f)
        assertFalse(spec.showLockIcon)
        assertTrue("owned-inactive renders the actual thumb", spec.showsActualThumb)
        assertEquals("onSurface", spec.captionColorToken)
    }

    @Test
    fun `Locked never renders the actual thumb art`() {
        // companion-skin-gacha capability: locked cells MUST NOT render
        // the skin's actual thumb. This is the load-bearing transparency
        // contract — users see *that* a skin exists and *what traits* it
        // unlocks, but never the art itself before they own it.
        val spec = galleryCellSpec(GalleryCellState.Locked)
        assertFalse(
            "locked cell MUST render silhouette + lock icon, never the real thumb",
            spec.showsActualThumb,
        )
        assertTrue("locked cell shows the lock icon", spec.showLockIcon)
        assertEquals("locked cell rendered at 40% opacity per design.md", 0.4f, spec.opacity, 0.0001f)
        assertEquals("locked border is rarity-coded", "rarity", spec.borderColorToken)
        assertEquals("locked caption uses the muted variant token", "onSurfaceVariant", spec.captionColorToken)
    }

    @Test
    fun `every rarity has a distinct locked-border palette token`() {
        // Locked cells use a rarity-coded border so the user can tell
        // "this is the legendary one I'm chasing" at a glance even with
        // the art hidden. Tokens are resolved against the composition
        // palette at render time; the test pins the mapping itself.
        val tokens = SkinRarity.values().map { rarityBorderToken(it) }.toSet()
        assertEquals(
            "Each of the 4 rarities must have its own palette token",
            SkinRarity.values().size,
            tokens.size,
        )
    }

    @Test
    fun `active and inactive states differ visually in border + ember ring + caption`() {
        // Anti-regression: the two owned states MUST have at least three
        // independent visual differences so an at-a-glance scan tells
        // them apart even with the same thumb.
        val active = galleryCellSpec(GalleryCellState.OwnedActive)
        val inactive = galleryCellSpec(GalleryCellState.OwnedInactive)
        assertNotEquals(active.borderDp, inactive.borderDp)
        assertNotEquals(active.borderColorToken, inactive.borderColorToken)
        assertNotEquals(active.showInnerEmberRing, inactive.showInnerEmberRing)
        assertNotEquals(active.captionColorToken, inactive.captionColorToken)
    }
}
