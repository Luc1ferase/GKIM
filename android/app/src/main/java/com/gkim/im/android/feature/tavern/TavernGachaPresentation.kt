package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.CompanionCharacterCard

/**
 * §7.1 / §7.2 — presentation-layer helpers for the gacha surface.
 *
 * The pre-draw surface shows a rarity / probability breakdown (§7.1); the post-draw surface
 * switches between a "new card" and an "already owned" variant (§7.2). Both flows are driven
 * by pure functions defined here so they can be exercised without a Compose tree.
 */

// -------------------------------------------------------------------------
// §7.1 — Rarity + probability breakdown
// -------------------------------------------------------------------------

/**
 * Card rarity inferred from a card's tags. The first matching tag (case-insensitive, in
 * declaration order) wins; cards with no rarity tag default to [Common].
 *
 * The tag prefix is matched as an exact equality to `tag.lowercase()` rather than a substring
 * so a tag like "common-room-background" does not accidentally classify a card.
 */
enum class GachaRarity(val tag: String, val englishLabel: String, val chineseLabel: String) {
    Legendary("legendary", "Legendary", "传说"),
    Epic("epic", "Epic", "史诗"),
    Rare("rare", "Rare", "稀有"),
    Common("common", "Common", "普通");

    companion object {
        fun forTags(tags: List<String>): GachaRarity {
            val lowered = tags.map { it.lowercase() }
            return entries.firstOrNull { it.tag in lowered } ?: Common
        }
    }
}

data class GachaRarityEntry(
    val rarity: GachaRarity,
    val count: Int,
    /** Probability as a fraction of the pool total, in [0f, 1f]. */
    val probability: Float,
)

data class GachaProbabilityBreakdown(val entries: List<GachaRarityEntry>) {
    val isEmpty: Boolean get() = entries.isEmpty()
}

/**
 * Group [drawPool] by inferred rarity, return entries in canonical [GachaRarity] order with
 * zero-count rarities omitted. Returns an empty breakdown for an empty pool.
 */
internal fun computeProbabilityBreakdown(
    drawPool: List<CompanionCharacterCard>,
): GachaProbabilityBreakdown {
    if (drawPool.isEmpty()) return GachaProbabilityBreakdown(emptyList())
    val total = drawPool.size
    val grouped = drawPool.groupBy { GachaRarity.forTags(it.tags) }
    val entries = GachaRarity.entries.mapNotNull { rarity ->
        val count = grouped[rarity]?.size ?: 0
        if (count == 0) {
            null
        } else {
            GachaRarityEntry(
                rarity = rarity,
                count = count,
                probability = count.toFloat() / total,
            )
        }
    }
    return GachaProbabilityBreakdown(entries)
}

/**
 * Render a probability as a percent string. Values ≥10% round to an integer; values under
 * 10% render with a single decimal. `0f` yields `"0%"`; `1f` yields `"100%"`.
 */
internal fun formatProbabilityPercent(probability: Float): String {
    val percent = probability.coerceIn(0f, 1f) * 100f
    return when {
        percent >= 10f -> "${percent.toInt()}%"
        percent > 0f -> String.format("%.1f%%", percent)
        else -> "0%"
    }
}

