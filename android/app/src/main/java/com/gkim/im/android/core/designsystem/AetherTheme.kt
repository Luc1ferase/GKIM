package com.gkim.im.android.core.designsystem

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AetherColors {
    val Surface = Color(0xFF091328)
    val SurfaceLight = Color(0xFFF8FAFC)
    val SurfaceContainerLow = Color(0xFF1A2338)
    val SurfaceContainerHigh = Color(0xFF2A3550)
    val SurfaceContainerHighest = Color(0xFF344262)
    val SurfaceLowest = Color(0xFF050B16)
    val Primary = Color(0xFFC3C0FF)
    val PrimaryContainer = Color(0xFF4F46E5)
    val Secondary = Color(0xFFD5E3FD)
    val Tertiary = Color(0xFFFF9DD1)
    val OnSurface = Color(0xFFFFFFFF)
    val OnSurfaceVariant = Color(0xFFA3AAC4)
    val OutlineVariant = Color(0xFF464555)
    val Success = Color(0xFF49D39D)
    val Danger = Color(0xFFFF6E84)
}

@Immutable
data class AetherSpacing(
    val page: Int = 24,
    val section: Int = 20,
    val card: Int = 18,
    val gap: Int = 12,
)

val LocalAetherSpacing = staticCompositionLocalOf { AetherSpacing() }

object AetherMotion {
    val standard: FiniteAnimationSpec<Float> = tween(durationMillis = 300)
}

private val AetherTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 26.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.4.sp),
)

@Composable
fun GkimTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = AetherColors.Primary,
        secondary = AetherColors.Secondary,
        tertiary = AetherColors.Tertiary,
        background = AetherColors.Surface,
        surface = AetherColors.Surface,
        onPrimary = AetherColors.Surface,
        onSecondary = AetherColors.Surface,
        onTertiary = AetherColors.Surface,
        onBackground = AetherColors.OnSurface,
        onSurface = AetherColors.OnSurface,
        outline = AetherColors.OutlineVariant,
    )

    MaterialTheme(colorScheme = colorScheme, typography = AetherTypography, content = content)
}
