package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionDrawResult
import com.gkim.im.android.core.model.SkinDrawResultState

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

// -------------------------------------------------------------------------
// §7.2 — Draw-result variant + bonus event
// -------------------------------------------------------------------------

/**
 * Which post-draw animation the surface should render.
 *
 * - [NewCard] is the standard "added to roster" flow from the original gacha (§7.2)
 *   and the R4.3 mapping target for `NEW_CHARACTER`.
 * - [AlreadyOwned] is the duplicate variant whose rendering includes a "Keep as
 *   bonus" CTA (§7.2) and the R4.3 mapping target for `DUPLICATE_SKIN`.
 * - [NewSkin] is the R4.3 surface for the case where the user already owns the
 *   *character* but tonight's draw landed a new outfit. Carries its own caption
 *   ("新装束" / "New attire") wired via [com.gkim.im.android.feature.tavern.animation.SkinRevealTracks.NewSkinReveal].
 */
sealed interface GachaResultVariant {
    data object NewCard : GachaResultVariant
    data object AlreadyOwned : GachaResultVariant
    data object NewSkin : GachaResultVariant
}

/**
 * R4.3 — three-state router. Maps the closed-set [SkinDrawResultState] from the
 * `/api/v1/skins/draw` response to the matching surface variant. Every state must
 * map to a unique variant; the contract test in `GachaThreeStateRouterTest` pins
 * the surjection so a future fourth state can't silently collapse onto an
 * existing surface.
 */
internal val SkinDrawStateToVariant: Map<SkinDrawResultState, GachaResultVariant> = mapOf(
    SkinDrawResultState.NewCharacter  to GachaResultVariant.NewCard,
    SkinDrawResultState.NewSkin       to GachaResultVariant.NewSkin,
    SkinDrawResultState.DuplicateSkin to GachaResultVariant.AlreadyOwned,
)

internal fun gachaResultVariantForState(state: SkinDrawResultState): GachaResultVariant =
    SkinDrawStateToVariant.getValue(state)

/**
 * Resolve the post-draw surface variant for a result.
 *
 * When the backend ships a [CompanionDrawResult.drawResultState] (R4.3), the
 * router uses it directly. Pre-R4.3 results (legacy `/draw` endpoint, in-memory
 * roster repo) leave the field null and fall back to the `wasNew` heuristic —
 * which collapses NEW_SKIN onto AlreadyOwned because there's no way to tell.
 */
internal fun gachaResultVariant(result: CompanionDrawResult): GachaResultVariant =
    result.drawResultState
        ?.let { gachaResultVariantForState(it) }
        ?: if (result.wasNew) GachaResultVariant.NewCard else GachaResultVariant.AlreadyOwned

/**
 * Event payload recorded when a user taps "Keep as bonus" on an already-owned duplicate
 * draw result. Carries the card id (for attribution) plus an epoch-ms timestamp so downstream
 * analytics / compensation flows can order events.
 */
data class BonusAwardedEvent(
    val cardId: String,
    val timestampEpochMs: Long,
)

/**
 * Build a [BonusAwardedEvent] for a draw result. Factored into a pure function so the test
 * can assert the exact payload shape without touching the composable's callback-invocation.
 */
internal fun bonusAwardedEvent(
    result: CompanionDrawResult,
    timestampEpochMs: Long,
): BonusAwardedEvent = BonusAwardedEvent(
    cardId = result.card.id,
    timestampEpochMs = timestampEpochMs,
)

