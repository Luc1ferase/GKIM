package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.LocalizedText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §7.1 verification — the pre-draw UI computes a rarity / probability breakdown from the
 * draw pool's existing per-card tags. Cards tagged with one of `legendary` / `epic` / `rare`
 * / `common` classify into the matching [GachaRarity]; others default to [GachaRarity.Common].
 *
 * Tests drive [computeProbabilityBreakdown] against several fixture distributions plus
 * [formatProbabilityPercent] across its rendering branches (≥10% integer, <10% one-decimal,
 * 0% literal). Zero-count rarities are omitted from the output so the UI never renders an
 * empty row.
 */
class GachaProbabilitySurfacingTest {

    private fun card(id: String, tags: List<String>): CompanionCharacterCard =
        CompanionCharacterCard(
            id = id,
            displayName = LocalizedText.of(id),
            roleLabel = LocalizedText.of("Role"),
            summary = LocalizedText.Empty,
            firstMes = LocalizedText.Empty,
            tags = tags,
            avatarText = id.take(2).uppercase(),
            accent = AccentTone.Primary,
            source = CompanionCharacterSource.Preset,
        )

    // -------------------------------------------------------------------------
    // computeProbabilityBreakdown — grouping + ordering
    // -------------------------------------------------------------------------

    @Test
    fun `empty pool yields an empty breakdown`() {
        val breakdown = computeProbabilityBreakdown(emptyList())
        assertTrue(breakdown.isEmpty)
        assertTrue(breakdown.entries.isEmpty())
    }

    @Test
    fun `pool with only untagged cards surfaces 100 percent Common`() {
        val pool = listOf(card("a", emptyList()), card("b", emptyList()), card("c", emptyList()))
        val breakdown = computeProbabilityBreakdown(pool)
        assertEquals(1, breakdown.entries.size)
        assertEquals(GachaRarity.Common, breakdown.entries.single().rarity)
        assertEquals(3, breakdown.entries.single().count)
        assertEquals(1f, breakdown.entries.single().probability, 0.0001f)
    }

    @Test
    fun `mixed pool groups by first-matching rarity tag (case-insensitive)`() {
        val pool = listOf(
            card("a", listOf("Legendary", "main")),
            card("b", listOf("EPIC")),
            card("c", listOf("rare")),
            card("d", listOf("common")),
            card("e", listOf("rare", "also-legendary-but-ignored")),
        )
        val breakdown = computeProbabilityBreakdown(pool)
        // Canonical order: Legendary, Epic, Rare, Common
        val rarities = breakdown.entries.map { it.rarity }
        assertEquals(listOf(GachaRarity.Legendary, GachaRarity.Epic, GachaRarity.Rare, GachaRarity.Common), rarities)
        // Each rarity index has the expected count
        assertEquals(1, breakdown.entries.first { it.rarity == GachaRarity.Legendary }.count)
        assertEquals(1, breakdown.entries.first { it.rarity == GachaRarity.Epic }.count)
        assertEquals(2, breakdown.entries.first { it.rarity == GachaRarity.Rare }.count)
        assertEquals(1, breakdown.entries.first { it.rarity == GachaRarity.Common }.count)
    }

    @Test
    fun `zero-count rarities are omitted from the output`() {
        // Pool has only legendary + common. Epic and Rare must not appear.
        val pool = listOf(
            card("a", listOf("legendary")),
            card("b", listOf("common")),
            card("c", emptyList()), // default Common
        )
        val breakdown = computeProbabilityBreakdown(pool)
        val rarities = breakdown.entries.map { it.rarity }
        assertEquals(listOf(GachaRarity.Legendary, GachaRarity.Common), rarities)
        assertFalse("Epic must not appear", rarities.contains(GachaRarity.Epic))
        assertFalse("Rare must not appear", rarities.contains(GachaRarity.Rare))
    }

    @Test
    fun `probability sums to 1 for non-empty pools`() {
        val pool = listOf(
            card("a", listOf("legendary")),
            card("b", listOf("epic")),
            card("c", listOf("rare")),
            card("d", listOf("rare")),
            card("e", listOf("common")),
            card("f", listOf("common")),
            card("g", listOf("common")),
            card("h", listOf("common")),
            card("i", listOf("common")),
            card("j", listOf("common")),
        )
        val breakdown = computeProbabilityBreakdown(pool)
        assertEquals(1f, breakdown.entries.sumOf { it.probability.toDouble() }.toFloat(), 0.0001f)
        assertEquals(0.1f, breakdown.entries.first { it.rarity == GachaRarity.Legendary }.probability, 0.0001f)
        assertEquals(0.6f, breakdown.entries.first { it.rarity == GachaRarity.Common }.probability, 0.0001f)
    }

    @Test
    fun `substring-looking tags do not match rarity equality`() {
        // "common-room-background" must NOT classify the card as Common — the match is on
        // the exact lowercased tag, not a substring.
        val pool = listOf(
            card("a", listOf("common-room-background")),
            card("b", listOf("epic-narrative-theme")),
        )
        val breakdown = computeProbabilityBreakdown(pool)
        // Both default to Common because neither carries an exact rarity tag.
        assertEquals(1, breakdown.entries.size)
        assertEquals(GachaRarity.Common, breakdown.entries.single().rarity)
        assertEquals(2, breakdown.entries.single().count)
    }

    // -------------------------------------------------------------------------
    // formatProbabilityPercent — render contract
    // -------------------------------------------------------------------------

    @Test
    fun `format integer percent for values at or above 10 percent`() {
        assertEquals("100%", formatProbabilityPercent(1f))
        assertEquals("50%", formatProbabilityPercent(0.5f))
        assertEquals("10%", formatProbabilityPercent(0.1f))
        assertEquals("33%", formatProbabilityPercent(1f / 3f))
    }

    @Test
    fun `format one-decimal percent for values under 10 percent`() {
        assertEquals("5.0%", formatProbabilityPercent(0.05f))
        assertEquals("1.5%", formatProbabilityPercent(0.015f))
        assertEquals("0.5%", formatProbabilityPercent(0.005f))
    }

    @Test
    fun `format 0 percent for zero probability`() {
        assertEquals("0%", formatProbabilityPercent(0f))
    }

    @Test
    fun `clamp out-of-range probabilities gracefully`() {
        assertEquals("100%", formatProbabilityPercent(1.2f))
        assertEquals("0%", formatProbabilityPercent(-0.1f))
    }

    // -------------------------------------------------------------------------
    // GachaRarity.forTags — classifier contract
    // -------------------------------------------------------------------------

    @Test
    fun `forTags picks highest-priority rarity when multiple tags match`() {
        // Canonical declaration order is Legendary < Epic < Rare < Common. firstOrNull in
        // that order means Legendary wins when both legendary + rare are present.
        assertEquals(GachaRarity.Legendary, GachaRarity.forTags(listOf("rare", "legendary")))
        assertEquals(GachaRarity.Epic, GachaRarity.forTags(listOf("common", "epic")))
    }
}
