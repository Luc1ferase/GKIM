package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.SkinDrawResultState
import com.gkim.im.android.feature.tavern.animation.RevealCaptionKind
import com.gkim.im.android.feature.tavern.animation.RevealSurfaceScale
import com.gkim.im.android.feature.tavern.animation.SkinRevealTracks
import com.gkim.im.android.feature.tavern.animation.trackForState
import org.junit.Assert.assertEquals
import org.junit.Test

class SkinRevealRoutingTest {

    @Test
    fun `NEW_CHARACTER routes to the full-screen brass halo track`() {
        assertEquals(
            SkinRevealTracks.NewCharacterReveal,
            trackForState(SkinDrawResultState.NewCharacter),
        )
        assertEquals(
            RevealSurfaceScale.FullScreen,
            trackForState(SkinDrawResultState.NewCharacter).surfaceScale,
        )
    }

    @Test
    fun `NEW_SKIN routes to the mid-card portrait track with NewAttire caption`() {
        val track = trackForState(SkinDrawResultState.NewSkin)
        assertEquals(SkinRevealTracks.NewSkinReveal, track)
        assertEquals(RevealSurfaceScale.MidCard, track.surfaceScale)
        assertEquals(RevealCaptionKind.NewAttire, track.caption)
    }

    @Test
    fun `DUPLICATE_SKIN routes to the inline crossfade with currency caption`() {
        val track = trackForState(SkinDrawResultState.DuplicateSkin)
        assertEquals(SkinRevealTracks.DuplicateSkinReveal, track)
        assertEquals(RevealSurfaceScale.Inline, track.surfaceScale)
        assertEquals(RevealCaptionKind.CurrencyDelta, track.caption)
    }

    @Test
    fun `every state maps to a unique track`() {
        val tracks = SkinDrawResultState.values().map { trackForState(it) }.toSet()
        assertEquals(
            "Each state must route to a distinct reveal track",
            SkinDrawResultState.values().size,
            tracks.size,
        )
    }
}
