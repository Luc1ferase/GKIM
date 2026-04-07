package com.gkim.im.android.core.designsystem

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
private data class AetherPalette(
    val surface: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceLowest: Color,
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val tertiary: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outlineVariant: Color,
    val success: Color,
    val danger: Color,
)

private val DarkAetherPalette = AetherPalette(
    surface = Color(0xFF091328),
    surfaceContainerLow = Color(0xFF1A2338),
    surfaceContainerHigh = Color(0xFF2A3550),
    surfaceContainerHighest = Color(0xFF344262),
    surfaceLowest = Color(0xFF050B16),
    primary = Color(0xFFC3C0FF),
    primaryContainer = Color(0xFF4F46E5),
    secondary = Color(0xFFD5E3FD),
    tertiary = Color(0xFFFF9DD1),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFA3AAC4),
    outlineVariant = Color(0xFF464555),
    success = Color(0xFF49D39D),
    danger = Color(0xFFFF6E84),
)

private val LightAetherPalette = AetherPalette(
    surface = Color(0xFFF8FAFC),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE9EEF7),
    surfaceContainerHighest = Color(0xFFD9E2F2),
    surfaceLowest = Color(0xFFF2F5FA),
    primary = Color(0xFF4F46E5),
    primaryContainer = Color(0xFFC7D2FE),
    secondary = Color(0xFF355C7D),
    tertiary = Color(0xFFD946EF),
    onSurface = Color(0xFF101828),
    onSurfaceVariant = Color(0xFF5C667A),
    outlineVariant = Color(0xFFB6C0D4),
    success = Color(0xFF15803D),
    danger = Color(0xFFDC2626),
)

private val LocalAetherPalette = staticCompositionLocalOf { DarkAetherPalette }

object AetherColors {
    val Surface: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherPalette.current.surface

    val SurfaceContainerLow: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherPalette.current.surfaceContainerLow

    val SurfaceContainerHigh: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherPalette.current.surfaceContainerHigh

    val SurfaceContainerHighest: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherPalette.current.surfaceContainerHighest

    val SurfaceLowest: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherPalette.current.surfaceLowest

    val Primary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherPalette.current.primary

    val PrimaryContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherPalette.current.primaryContainer

    val Secondary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherPalette.current.secondary

    val Tertiary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherPalette.current.tertiary

    val OnSurface: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherPalette.current.onSurface

    val OnSurfaceVariant: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherPalette.current.onSurfaceVariant

    val OutlineVariant: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherPalette.current.outlineVariant

    val Success: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherPalette.current.success

    val Danger: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherPalette.current.danger
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
fun GkimTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkAetherPalette else LightAetherPalette
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.primary,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = palette.surface,
            surface = palette.surface,
            onPrimary = palette.surface,
            onSecondary = palette.surface,
            onTertiary = palette.surface,
            onBackground = palette.onSurface,
            onSurface = palette.onSurface,
            outline = palette.outlineVariant,
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = palette.surface,
            surface = palette.surface,
            onPrimary = palette.surface,
            onSecondary = palette.surface,
            onTertiary = palette.surface,
            onBackground = palette.onSurface,
            onSurface = palette.onSurface,
            outline = palette.outlineVariant,
        )
    }

    CompositionLocalProvider(LocalAetherPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme, typography = AetherTypography, content = content)
    }
}
