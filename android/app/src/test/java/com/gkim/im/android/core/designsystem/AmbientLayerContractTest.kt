package com.gkim.im.android.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientLayerContractTest {

    @Test
    fun `tavern grain opacity ceiling is 8 percent`() {
        assertEquals(0.08f, TavernGrainOpacityCeiling, 0.0001f)
    }

    @Test
    fun `candle glow opacity ceiling is 5 percent`() {
        assertEquals(0.05f, CandleGlowOpacityCeiling, 0.0001f)
    }

    @Test
    fun `tavern grain ceiling stays subtle`() {
        // The "no full-screen indigo to magenta gradient" anti-pattern is
        // really a budget on chrome-effect opacity. Keep grain <= 8 % so
        // the surface reads as "weighted" rather than "patterned".
        assertTrue(TavernGrainOpacityCeiling <= 0.08f)
        assertTrue(TavernGrainOpacityCeiling >= 0.02f)
    }

    @Test
    fun `candle glow ceiling stays calm-bar-not-club`() {
        // The bar is calm, not a club. Cap radial highlight at 5 % so it
        // reads as warmth in motion rather than as a visible gradient.
        assertTrue(CandleGlowOpacityCeiling <= 0.05f)
        assertTrue(CandleGlowOpacityCeiling >= 0.01f)
    }

    @Test
    fun `grain and glow ceilings are not the same number`() {
        // Belt-and-suspenders: catches an obvious copy-paste regression.
        assertTrue(TavernGrainOpacityCeiling != CandleGlowOpacityCeiling)
    }
}
