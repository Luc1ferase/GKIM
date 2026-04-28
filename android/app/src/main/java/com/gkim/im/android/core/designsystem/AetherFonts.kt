package com.gkim.im.android.core.designsystem

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.gkim.im.android.R

// CJK fallback strategy (option C):
// Android 7+ resolves Han glyphs through the system font fallback chain. With
// DisplaySerif (Newsreader) the system serves Noto Serif CJK SC for any glyph
// missing from the Latin face; with UiSans (Inter) it serves Noto Sans CJK SC.
// We therefore package only the Latin endpoints of each bilingual chain.

object AetherFonts {
    val DisplaySerif: FontFamily = FontFamily(
        Font(R.font.newsreader_regular, FontWeight.Normal),
        Font(R.font.newsreader_semibold, FontWeight.SemiBold),
        Font(R.font.newsreader_bold, FontWeight.Bold),
    )

    val UiSans: FontFamily = FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
    )
}

internal enum class AetherFontFamilyId { DisplaySerif, UiSans }

internal data class AetherFontAsset(
    val resId: Int,
    val fileName: String,
    val weight: FontWeight,
    val family: AetherFontFamilyId,
)

internal val AetherFontAssets: List<AetherFontAsset> = listOf(
    AetherFontAsset(R.font.newsreader_regular, "newsreader_regular.ttf", FontWeight.Normal, AetherFontFamilyId.DisplaySerif),
    AetherFontAsset(R.font.newsreader_semibold, "newsreader_semibold.ttf", FontWeight.SemiBold, AetherFontFamilyId.DisplaySerif),
    AetherFontAsset(R.font.newsreader_bold, "newsreader_bold.ttf", FontWeight.Bold, AetherFontFamilyId.DisplaySerif),
    AetherFontAsset(R.font.inter_regular, "inter_regular.ttf", FontWeight.Normal, AetherFontFamilyId.UiSans),
    AetherFontAsset(R.font.inter_medium, "inter_medium.ttf", FontWeight.Medium, AetherFontFamilyId.UiSans),
)
