package com.gkim.im.android.core.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class AetherPaletteContractTest {

    @Test
    fun `dark palette anchors on espresso surface and brass primary`() {
        assertEquals(Color(0xFF1A0F0A), DarkAetherPalette.surface)
        assertEquals(Color(0xFFE0A04D), DarkAetherPalette.primary)
    }

    @Test
    fun `dark palette surface containers step warm-cocoa`() {
        assertEquals(Color(0xFF271812), DarkAetherPalette.surfaceContainerLow)
        assertEquals(Color(0xFF3A2419), DarkAetherPalette.surfaceContainerHigh)
        assertEquals(Color(0xFF4D2E1F), DarkAetherPalette.surfaceContainerHighest)
    }

    @Test
    fun `dark palette accent and ink tokens match design`() {
        assertEquals(Color(0xFF0E0805), DarkAetherPalette.surfaceLowest)
        assertEquals(Color(0xFFA06135), DarkAetherPalette.primaryContainer)
        assertEquals(Color(0xFF8B5E3C), DarkAetherPalette.secondary)
        assertEquals(Color(0xFFB85450), DarkAetherPalette.tertiary)
        assertEquals(Color(0xFFF4ECDD), DarkAetherPalette.onSurface)
        assertEquals(Color(0xFFB8A78D), DarkAetherPalette.onSurfaceVariant)
        assertEquals(Color(0xFF4A382C), DarkAetherPalette.outlineVariant)
        assertEquals(Color(0xFF7BA05B), DarkAetherPalette.success)
        assertEquals(Color(0xFFC24644), DarkAetherPalette.danger)
    }

    @Test
    fun `light palette anchors on aged paper surface and saddle brown primary`() {
        assertEquals(Color(0xFFF1E7D2), LightAetherPalette.surface)
        assertEquals(Color(0xFF8B4513), LightAetherPalette.primary)
    }

    @Test
    fun `light palette surface containers step linen`() {
        assertEquals(Color(0xFFF8F0DD), LightAetherPalette.surfaceContainerLow)
        assertEquals(Color(0xFFE5D5B6), LightAetherPalette.surfaceContainerHigh)
        assertEquals(Color(0xFFD8C49B), LightAetherPalette.surfaceContainerHighest)
    }

    @Test
    fun `light palette accent and ink tokens match design`() {
        assertEquals(Color(0xFFEBDFC4), LightAetherPalette.surfaceLowest)
        assertEquals(Color(0xFFC28E5A), LightAetherPalette.primaryContainer)
        assertEquals(Color(0xFF705033), LightAetherPalette.secondary)
        assertEquals(Color(0xFF9B3C30), LightAetherPalette.tertiary)
        assertEquals(Color(0xFF2A1810), LightAetherPalette.onSurface)
        assertEquals(Color(0xFF705943), LightAetherPalette.onSurfaceVariant)
        assertEquals(Color(0xFFB19877), LightAetherPalette.outlineVariant)
        assertEquals(Color(0xFF5E7E3F), LightAetherPalette.success)
        assertEquals(Color(0xFFA33A35), LightAetherPalette.danger)
    }

    @Test
    fun `forbidden cold-aether hexes do not appear in either palette`() {
        val forbidden = setOf(
            Color(0xFF091328),
            Color(0xFF4F46E5),
            Color(0xFFC3C0FF),
        )
        val darkTokens = listOf(
            DarkAetherPalette.surface,
            DarkAetherPalette.surfaceContainerLow,
            DarkAetherPalette.surfaceContainerHigh,
            DarkAetherPalette.surfaceContainerHighest,
            DarkAetherPalette.surfaceLowest,
            DarkAetherPalette.primary,
            DarkAetherPalette.primaryContainer,
            DarkAetherPalette.secondary,
            DarkAetherPalette.tertiary,
            DarkAetherPalette.onSurface,
            DarkAetherPalette.onSurfaceVariant,
            DarkAetherPalette.outlineVariant,
            DarkAetherPalette.success,
            DarkAetherPalette.danger,
        )
        val lightTokens = listOf(
            LightAetherPalette.surface,
            LightAetherPalette.surfaceContainerLow,
            LightAetherPalette.surfaceContainerHigh,
            LightAetherPalette.surfaceContainerHighest,
            LightAetherPalette.surfaceLowest,
            LightAetherPalette.primary,
            LightAetherPalette.primaryContainer,
            LightAetherPalette.secondary,
            LightAetherPalette.tertiary,
            LightAetherPalette.onSurface,
            LightAetherPalette.onSurfaceVariant,
            LightAetherPalette.outlineVariant,
            LightAetherPalette.success,
            LightAetherPalette.danger,
        )
        (darkTokens + lightTokens).forEach { token ->
            assert(token !in forbidden) { "forbidden cold-aether hex appeared in palette: $token" }
        }
    }
}
