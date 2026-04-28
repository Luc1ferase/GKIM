package com.gkim.im.android.feature.tavern

// R5.3 — companion-skin-gacha probability detail tree.
//
// The default surface stays the rarity-aggregated breakdown produced
// by computeProbabilityBreakdown (already in TavernGachaPresentation).
// On user toggle, we render a two-level tree:
//
//   character header (name + sum-of-skins probability)
//     └── skin row (name + thumb-id + own-skin probability)
//     └── skin row …
//
// Sort: rarity descending, then alphabetical by skinId.
//
// SkinPoolEntry mirrors the day-1 R4.1 contract; until R4.1 lands the
// real skin-granular pool, the bridge in TavernRoute projects each
// character-level pool entry as a single (characterId, "{c}-default",
// weight, rarity) tuple — degenerate tree, but the math holds.

/**
 * One row of the skin-granular gacha pool. Mirrors the (character_id,
 * skin_id, weight, rarity) tuple from the R4.1 backend pool config.
 */
data class SkinPoolEntry(
    val characterId: String,
    val characterDisplayName: String,
    val skinId: String,
    val skinDisplayName: String,
    val weight: Int,
    val rarity: GachaRarity,
)

data class SkinProbabilityNode(
    val skinId: String,
    val skinDisplayName: String,
    val rarity: GachaRarity,
    val probability: Float,   // [0f, 1f]
)

data class CharacterProbabilityNode(
    val characterId: String,
    val characterDisplayName: String,
    val topRarity: GachaRarity,                    // for sort key + header glance
    val probability: Float,                         // sum of children probabilities
    val skins: List<SkinProbabilityNode>,
)

data class GachaProbabilityTree(val characters: List<CharacterProbabilityNode>) {
    val isEmpty: Boolean get() = characters.isEmpty()
}

/**
 * Build a per-character → per-skin probability tree from the
 * skin-granular pool. Empty pool returns an empty tree.
 *
 * Sort: characters by their top rarity desc (Legendary first), tied
 * characters alphabetically by characterId. Within a character, skins
 * by rarity desc, ties alphabetically by skinId.
 *
 * Per-skin probability is `weight / sum(weight)` over the *entire*
 * pool. The character's `probability` is the sum of its skins'
 * probabilities — invariant that the per-skin sum equals the
 * character's total (within ±1pp under integer rounding, asserted
 * by the contract test).
 */
internal fun computeProbabilityTree(pool: List<SkinPoolEntry>): GachaProbabilityTree {
    if (pool.isEmpty()) return GachaProbabilityTree(emptyList())
    val totalWeight = pool.sumOf { it.weight }.coerceAtLeast(1)

    val grouped = pool.groupBy { it.characterId }

    val characters = grouped.map { (characterId, rows) ->
        val skinNodes = rows
            .map { row ->
                SkinProbabilityNode(
                    skinId = row.skinId,
                    skinDisplayName = row.skinDisplayName,
                    rarity = row.rarity,
                    probability = row.weight.toFloat() / totalWeight,
                )
            }
            .sortedWith(
                compareBy(
                    { -it.rarity.sortKey() },     // rarity desc
                    { it.skinId },                 // alphabetic within a rarity tier
                ),
            )

        CharacterProbabilityNode(
            characterId = characterId,
            characterDisplayName = rows.first().characterDisplayName,
            topRarity = skinNodes.first().rarity,
            probability = skinNodes.sumOf { it.probability.toDouble() }.toFloat(),
            skins = skinNodes,
        )
    }
        .sortedWith(
            compareBy(
                { -it.topRarity.sortKey() },
                { it.characterId },
            ),
        )

    return GachaProbabilityTree(characters)
}

private fun GachaRarity.sortKey(): Int = when (this) {
    GachaRarity.Legendary -> 4
    GachaRarity.Epic      -> 3
    GachaRarity.Rare      -> 2
    GachaRarity.Common    -> 1
}

/**
 * Bridge for the day-1 (pre-R4.1) call site: project a
 * character-level draw pool into a skin-level pool by treating each
 * character as having a single default skin row. The tree degenerates
 * to a one-skin-per-character shape but the math (and the test)
 * remains valid; when R4.1 lands, the TavernRoute switches to the
 * real skin-granular pool and this projection becomes obsolete.
 */
internal fun projectCharacterPoolAsSkinPool(
    drawPool: List<com.gkim.im.android.core.model.CompanionCharacterCard>,
    resolveDisplayName: (com.gkim.im.android.core.model.CompanionCharacterCard) -> String,
): List<SkinPoolEntry> = drawPool.map { card ->
    SkinPoolEntry(
        characterId = card.id,
        characterDisplayName = resolveDisplayName(card),
        skinId = "${card.id}-default",
        skinDisplayName = resolveDisplayName(card),
        weight = 1,
        rarity = GachaRarity.forTags(card.tags),
    )
}
