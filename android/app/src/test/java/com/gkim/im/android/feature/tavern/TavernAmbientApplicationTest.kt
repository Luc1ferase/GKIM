package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.designsystem.AmbientGlowAnchor
import com.gkim.im.android.core.designsystem.ChromeSurfacesWithAmbient
import com.gkim.im.android.core.designsystem.TavernHomeAmbient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernAmbientApplicationTest {

    @Test
    fun `tavern home outer column applies grain`() {
        assertTrue(TavernHomeAmbient.grain)
    }

    @Test
    fun `tavern home glow anchor is TopEnd`() {
        assertEquals(AmbientGlowAnchor.TopEnd, TavernHomeAmbient.glowAnchor)
    }

    @Test
    fun `tavern home is targeted by surface testTag`() {
        assertEquals("tavern-screen", TavernHomeAmbient.surfaceTestTag)
    }

    @Test
    fun `tavern home appears in chrome ambient manifest`() {
        assertTrue(TavernHomeAmbient in ChromeSurfacesWithAmbient)
    }

    @Test
    fun `no other tavern surface opts in to ambient in this slice`() {
        // Anti-regression: only the tavern home outer column applies the
        // ambient layer in this slice. No character detail / portrait
        // viewer / chat editor surface should silently start drawing
        // tavernGrain or candleGlow.
        val tavernSurfaces = ChromeSurfacesWithAmbient
            .filter { it.surfaceTestTag.startsWith("tavern-") }
        assertEquals(1, tavernSurfaces.size)
        assertEquals("tavern-screen", tavernSurfaces.single().surfaceTestTag)
    }
}
