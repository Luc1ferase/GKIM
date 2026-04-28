package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.CharacterSkin

// R6.2 — trait progress strip math.
//
// "{ownedSkins} / {totalSkins} skins · {ownedTraits} / {totalTraits}
// traits unlocked" rendered as a horizontal segmented bar (brass for
// unlocked, surfaceContainerHigh for locked) on the character detail
// screen.
//
// All math lives in pure functions so the contract test pins the
// segment-count + tooltip-target rules without spinning up Compose.

data class TraitProgressSnapshot(
    val ownedSkins: Int,
    val totalSkins: Int,
    val ownedTraits: Int,
    val totalTraits: Int,
    val nextChaseSkinId: String?,        // null when everything is owned
)

internal fun computeTraitProgress(
    catalog: List<CharacterSkin>,
    ownedSkinIds: Set<String>,
): TraitProgressSnapshot {
    val total = catalog.size
    val ownedCatalog = catalog.filter { ownedSkinIds.contains(it.skinId) }
    val totalTraits = catalog.sumOf { it.traits.size }
    val ownedTraits = ownedCatalog.sumOf { it.traits.size }

    // Tooltip target = the lowest-rarity unowned skin (most attainable
    // next chase). Tied rarities resolve alphabetically by skinId.
    val nextChase = catalog
        .filterNot { ownedSkinIds.contains(it.skinId) }
        .sortedWith(compareBy({ it.rarity.value }, { it.skinId }))
        .firstOrNull()
        ?.skinId

    return TraitProgressSnapshot(
        ownedSkins = ownedCatalog.size,
        totalSkins = total,
        ownedTraits = ownedTraits,
        totalTraits = totalTraits,
        nextChaseSkinId = nextChase,
    )
}
