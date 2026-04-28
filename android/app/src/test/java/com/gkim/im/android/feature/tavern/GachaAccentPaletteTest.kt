package com.gkim.im.android.feature.tavern

import androidx.compose.ui.graphics.Color
import com.gkim.im.android.core.designsystem.DarkAetherPalette
import com.gkim.im.android.core.designsystem.LightAetherPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GachaAccentPaletteTest {

    @Test
    fun `gacha accent manifest binds only to primary and tertiary tokens`() {
        val tokens = GachaResultAccents.map { it.paletteToken }.toSet()
        assertTrue(
            "Gacha accents must only reference primary or tertiary tokens, got $tokens",
            tokens.all { it == "primary" || it == "tertiary" },
        )
    }

    @Test
    fun `dark tertiary is ember red - not lavender`() {
        // R1.1 swapped this token to #B85450 (ember red). The previous value
        // was #FF9DD1 (high-saturation pink) — anti-regression check.
        assertEquals(Color(0xFFB85450), DarkAetherPalette.tertiary)
    }

    @Test
    fun `dark primary is brass - not lavender`() {
        // R1.1 swapped this token to #E0A04D (brass). The previous value was
        // #C3C0FF (lavender) — anti-regression check.
        assertEquals(Color(0xFFE0A04D), DarkAetherPalette.primary)
    }

    @Test
    fun `light tertiary is terracotta - not magenta`() {
        // Light variant must also be re-toned. Was #D946EF magenta; now #9B3C30
        // terracotta.
        assertEquals(Color(0xFF9B3C30), LightAetherPalette.tertiary)
    }

    @Test
    fun `light primary is saddle brown - not indigo`() {
        // Was #4F46E5 indigo; now #8B4513 saddle brown.
        assertEquals(Color(0xFF8B4513), LightAetherPalette.primary)
    }

    @Test
    fun `gacha accent manifest covers the three known result surfaces`() {
        val tags = GachaResultAccents.map { it.testTag }.toSet()
        assertEquals(
            setOf(
                "tavern-draw-result-latest-label",
                "tavern-draw-result-duplicate-label",
                "tavern-draw-keep-as-bonus",
            ),
            tags,
        )
    }
}
