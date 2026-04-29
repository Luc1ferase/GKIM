package com.gkim.im.android.feature.tavern.animation

import com.gkim.im.android.core.model.SkinDrawResultState

// R5.2 — server-state → reveal-track router.
//
// The backend's draw response carries a `state` field that's one of
// NEW_CHARACTER / NEW_SKIN / DUPLICATE_SKIN. The router maps it to
// the matching SkinRevealTrack. The enum itself lives in core/model
// (R4.3) so DTO mapping and animation routing share a single source.

internal val SkinDrawStateToTrack: Map<SkinDrawResultState, SkinRevealTrack> = mapOf(
    SkinDrawResultState.NewCharacter   to SkinRevealTracks.NewCharacterReveal,
    SkinDrawResultState.NewSkin        to SkinRevealTracks.NewSkinReveal,
    SkinDrawResultState.DuplicateSkin  to SkinRevealTracks.DuplicateSkinReveal,
)

fun trackForState(state: SkinDrawResultState): SkinRevealTrack =
    SkinDrawStateToTrack.getValue(state)
