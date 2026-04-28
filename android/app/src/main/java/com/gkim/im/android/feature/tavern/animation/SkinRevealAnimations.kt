package com.gkim.im.android.feature.tavern.animation

// R5.1 — companion-skin-gacha reveal-animation timing contract.
//
// design.md pins three distinct tracks routed by the draw response
// `state` value. Timing is exposed as plain Int / Float / enum data so
// the contract test can lock every value without spinning up Compose
// or animation primitives. The composable layer wraps these values
// into `tween()` / `keyframes()` specs at use site (R5.2).
//
// Surface scales:
//   NEW_CHARACTER  → full-screen
//   NEW_SKIN       → mid-card 320 × 480
//   DUPLICATE_SKIN → inline within draw-result strip

enum class RevealSurfaceScale {
    FullScreen,
    MidCard,
    Inline,
}

enum class RevealAccentColor {
    BrassPrimary,    // #E0A04D — NEW_CHARACTER halo
    EmberTertiary,   // #B85450 — NEW_SKIN border pulse
    None,            // DUPLICATE_SKIN reuses R4.3 ember surface, no new pulse
}

enum class RevealCaptionKind {
    None,            // NEW_CHARACTER — banner alone tells the story
    NewAttire,       // NEW_SKIN — "新装束" / "New attire"
    CurrencyDelta,   // DUPLICATE_SKIN — "+N 故事碎片 / story shards"
}

data class SkinRevealTrack(
    val name: String,
    val surfaceScale: RevealSurfaceScale,
    val slideInMs: Int,
    val holdMs: Int,
    val pulseCycles: Int,
    val accentColor: RevealAccentColor,
    val caption: RevealCaptionKind,
) {
    val totalDurationMs: Int get() = slideInMs + holdMs
}

object SkinRevealTracks {
    val NewCharacterReveal: SkinRevealTrack = SkinRevealTrack(
        name = "NewCharacterReveal",
        surfaceScale = RevealSurfaceScale.FullScreen,
        slideInMs = 480,
        holdMs = 2_200,
        pulseCycles = 2,
        accentColor = RevealAccentColor.BrassPrimary,
        caption = RevealCaptionKind.None,
    )

    val NewSkinReveal: SkinRevealTrack = SkinRevealTrack(
        name = "NewSkinReveal",
        surfaceScale = RevealSurfaceScale.MidCard,
        slideInMs = 360,
        holdMs = 1_800,
        pulseCycles = 2,
        accentColor = RevealAccentColor.EmberTertiary,
        caption = RevealCaptionKind.NewAttire,
    )

    val DuplicateSkinReveal: SkinRevealTrack = SkinRevealTrack(
        name = "DuplicateSkinReveal",
        surfaceScale = RevealSurfaceScale.Inline,
        slideInMs = 240,
        holdMs = 0,
        pulseCycles = 0,
        accentColor = RevealAccentColor.None,
        caption = RevealCaptionKind.CurrencyDelta,
    )

    val All: List<SkinRevealTrack> = listOf(
        NewCharacterReveal,
        NewSkinReveal,
        DuplicateSkinReveal,
    )
}

// Mid-card portrait reveal surface dimensions (NEW_SKIN). Pinned for
// the surface-scale assertion so a layout drift doesn't silently
// upgrade the mid-card track to a full-screen track.
const val NewSkinRevealCardWidthDp: Int = 320
const val NewSkinRevealCardHeightDp: Int = 480
