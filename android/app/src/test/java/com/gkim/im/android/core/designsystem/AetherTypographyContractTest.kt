package com.gkim.im.android.core.designsystem

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AetherTypographyContractTest {

    @Test
    fun `headline large binds to display serif`() {
        assertEquals(AetherFonts.DisplaySerif, AetherTypography.headlineLarge.fontFamily)
    }

    @Test
    fun `headline medium binds to display serif`() {
        assertEquals(AetherFonts.DisplaySerif, AetherTypography.headlineMedium.fontFamily)
    }

    @Test
    fun `title large binds to display serif`() {
        assertEquals(AetherFonts.DisplaySerif, AetherTypography.titleLarge.fontFamily)
    }

    @Test
    fun `body large binds to ui sans`() {
        assertEquals(AetherFonts.UiSans, AetherTypography.bodyLarge.fontFamily)
    }

    @Test
    fun `body medium binds to ui sans`() {
        assertEquals(AetherFonts.UiSans, AetherTypography.bodyMedium.fontFamily)
    }

    @Test
    fun `label large binds to ui sans`() {
        assertEquals(AetherFonts.UiSans, AetherTypography.labelLarge.fontFamily)
    }

    @Test
    fun `default platform sans-serif and default families never appear in typography`() {
        val forbidden = setOf<FontFamily>(FontFamily.SansSerif, FontFamily.Default)
        val roles = listOf(
            AetherTypography.headlineLarge.fontFamily,
            AetherTypography.headlineMedium.fontFamily,
            AetherTypography.titleLarge.fontFamily,
            AetherTypography.bodyLarge.fontFamily,
            AetherTypography.bodyMedium.fontFamily,
            AetherTypography.labelLarge.fontFamily,
        )
        roles.forEach { family ->
            assertNotEquals("Forbidden platform-default family used: $family", FontFamily.SansSerif, family)
            assertNotEquals("Forbidden platform-default family used: $family", FontFamily.Default, family)
            assert(family !in forbidden) { "Forbidden platform-default family used: $family" }
        }
    }

    @Test
    fun `headline weights match design doc`() {
        assertEquals(FontWeight.Bold, AetherTypography.headlineLarge.fontWeight)
        assertEquals(FontWeight.SemiBold, AetherTypography.headlineMedium.fontWeight)
        assertEquals(FontWeight.SemiBold, AetherTypography.titleLarge.fontWeight)
    }

    @Test
    fun `body and label weights match design doc`() {
        assertEquals(FontWeight.Normal, AetherTypography.bodyLarge.fontWeight)
        assertEquals(FontWeight.Normal, AetherTypography.bodyMedium.fontWeight)
        assertEquals(FontWeight.Medium, AetherTypography.labelLarge.fontWeight)
    }

    @Test
    fun `font sizes preserve previous calibration`() {
        assertEquals(34f, AetherTypography.headlineLarge.fontSize.value, 0.0001f)
        assertEquals(28f, AetherTypography.headlineMedium.fontSize.value, 0.0001f)
        assertEquals(22f, AetherTypography.titleLarge.fontSize.value, 0.0001f)
        assertEquals(15f, AetherTypography.bodyLarge.fontSize.value, 0.0001f)
        assertEquals(13f, AetherTypography.bodyMedium.fontSize.value, 0.0001f)
        assertEquals(12f, AetherTypography.labelLarge.fontSize.value, 0.0001f)
    }

    @Test
    fun `line heights preserve previous calibration`() {
        assertEquals(38f, AetherTypography.headlineLarge.lineHeight.value, 0.0001f)
        assertEquals(32f, AetherTypography.headlineMedium.lineHeight.value, 0.0001f)
        assertEquals(26f, AetherTypography.titleLarge.lineHeight.value, 0.0001f)
        assertEquals(24f, AetherTypography.bodyLarge.lineHeight.value, 0.0001f)
        assertEquals(20f, AetherTypography.bodyMedium.lineHeight.value, 0.0001f)
    }

    @Test
    fun `label large keeps wide letter spacing`() {
        val spacing: TextUnit = AetherTypography.labelLarge.letterSpacing
        assertEquals(1.4f, spacing.value, 0.0001f)
        assertEquals(1.4.sp.value, spacing.value, 0.0001f)
    }
}
