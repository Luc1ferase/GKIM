package com.gkim.im.android.feature.tavern.animation

// R5.2 — server-state → reveal-track router.
//
// The backend's draw response carries a `state` field that's one of
// NEW_CHARACTER / NEW_SKIN / DUPLICATE_SKIN. The router maps it to
// the matching SkinRevealTrack. Pulled out as a closed enum so
// network-layer DTO mapping (R4.2) and UI animation routing (R5.2)
// share a single contract.

enum class SkinDrawResultState {
    NewCharacter,
    NewSkin,
    DuplicateSkin,
}

internal val SkinDrawStateToTrack: Map<SkinDrawResultState, SkinRevealTrack> = mapOf(
    SkinDrawResultState.NewCharacter   to SkinRevealTracks.NewCharacterReveal,
    SkinDrawResultState.NewSkin        to SkinRevealTracks.NewSkinReveal,
    SkinDrawResultState.DuplicateSkin  to SkinRevealTracks.DuplicateSkinReveal,
)

fun trackForState(state: SkinDrawResultState): SkinRevealTrack =
    SkinDrawStateToTrack.getValue(state)
