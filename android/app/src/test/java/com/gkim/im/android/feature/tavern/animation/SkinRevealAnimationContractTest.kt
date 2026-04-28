package com.gkim.im.android.feature.tavern.animation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinRevealAnimationContractTest {

    @Test
    fun `there are exactly three reveal tracks`() {
        assertEquals(3, SkinRevealTracks.All.size)
    }

    @Test
    fun `each track has a unique surface scale`() {
        val scales = SkinRevealTracks.All.map { it.surfaceScale }.toSet()
        assertEquals(
            "Each track must occupy a distinct surface scale (full / mid-card / inline)",
            3,
            scales.size,
        )
    }

    @Test
    fun `NewCharacterReveal pins full-screen + 480ms slide-in + 2200ms hold + 2 brass cycles`() {
        val t = SkinRevealTracks.NewCharacterReveal
        assertEquals(RevealSurfaceScale.FullScreen, t.surfaceScale)
        assertEquals(480, t.slideInMs)
        assertEquals(2_200, t.holdMs)
        assertEquals(2, t.pulseCycles)
        assertEquals(RevealAccentColor.BrassPrimary, t.accentColor)
        assertEquals(RevealCaptionKind.None, t.caption)
    }

    @Test
    fun `NewSkinReveal pins mid-card + 360ms slide-up + 1800ms hold + 2 ember cycles + new-attire caption`() {
        val t = SkinRevealTracks.NewSkinReveal
        assertEquals(RevealSurfaceScale.MidCard, t.surfaceScale)
        assertEquals(360, t.slideInMs)
        assertEquals(1_800, t.holdMs)
        assertEquals(2, t.pulseCycles)
        assertEquals(RevealAccentColor.EmberTertiary, t.accentColor)
        assertEquals(RevealCaptionKind.NewAttire, t.caption)
    }

    @Test
    fun `DuplicateSkinReveal pins inline + 240ms crossfade + 0 hold + 0 pulses + currency caption`() {
        val t = SkinRevealTracks.DuplicateSkinReveal
        assertEquals(RevealSurfaceScale.Inline, t.surfaceScale)
        assertEquals(240, t.slideInMs)
        assertEquals(0, t.holdMs)
        assertEquals(0, t.pulseCycles)
        assertEquals(RevealAccentColor.None, t.accentColor)
        assertEquals(RevealCaptionKind.CurrencyDelta, t.caption)
    }

    @Test
    fun `track durations are ordered NEW_CHARACTER greatest, then NEW_SKIN, then DUPLICATE`() {
        // The most ceremonial event (NEW_CHARACTER) holds longest; the
        // routine event (DUPLICATE_SKIN) is the briefest. Anti-regression
        // for the relative-cadence rule.
        val char = SkinRevealTracks.NewCharacterReveal.totalDurationMs
        val skin = SkinRevealTracks.NewSkinReveal.totalDurationMs
        val dup  = SkinRevealTracks.DuplicateSkinReveal.totalDurationMs
        assertTrue("NEW_CHARACTER total ($char) must be > NEW_SKIN total ($skin)", char > skin)
        assertTrue("NEW_SKIN total ($skin) must be > DUPLICATE_SKIN total ($dup)", skin > dup)
    }

    @Test
    fun `NEW_CHARACTER and NEW_SKIN use distinct accent colors`() {
        assertNotEquals(
            "Brass halo must not be reused for the new-skin track — they're meant to read as different events",
            SkinRevealTracks.NewCharacterReveal.accentColor,
            SkinRevealTracks.NewSkinReveal.accentColor,
        )
    }

    @Test
    fun `mid-card surface dimensions match design contract`() {
        assertEquals(320, NewSkinRevealCardWidthDp)
        assertEquals(480, NewSkinRevealCardHeightDp)
    }
}
