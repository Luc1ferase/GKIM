package com.gkim.im.android.feature.tavern

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ProbabilityBreakdownTreeTest {

    @Test
    fun `empty pool yields empty tree`() {
        val tree = computeProbabilityTree(emptyList())
        assertTrue(tree.isEmpty)
        assertEquals(0, tree.characters.size)
    }

    @Test
    fun `tree is sorted by rarity descending then character id alphabetical`() {
        val pool = listOf(
            entry("c1", "C-One",   "s-c1-default",     1, GachaRarity.Common),
            entry("a1", "A-One",   "s-a1-legendary",   1, GachaRarity.Legendary),
            entry("b1", "B-One",   "s-b1-epic",        1, GachaRarity.Epic),
            entry("a2", "A-Two",   "s-a2-legendary",   1, GachaRarity.Legendary),
        )
        val tree = computeProbabilityTree(pool)
        val ids = tree.characters.map { it.characterId }
        // Legendary chars first (a1, a2 by alphabet), then epic (b1), then common (c1).
        assertEquals(listOf("a1", "a2", "b1", "c1"), ids)
    }

    @Test
    fun `skin rows within a character are sorted by rarity desc then skin id alphabetic`() {
        val pool = listOf(
            entry("hero", "Hero", "hero-zeta",    1, GachaRarity.Common),
            entry("hero", "Hero", "hero-alpha",   1, GachaRarity.Legendary),
            entry("hero", "Hero", "hero-beta",    1, GachaRarity.Legendary),
            entry("hero", "Hero", "hero-gamma",   1, GachaRarity.Epic),
        )
        val tree = computeProbabilityTree(pool)
        val skins = tree.characters.single().skins.map { it.skinId }
        // Legendary tier first (alpha then beta alphabetically), epic, common.
        assertEquals(listOf("hero-alpha", "hero-beta", "hero-gamma", "hero-zeta"), skins)
    }

    @Test
    fun `per-skin probabilities sum to character probability within rounding tolerance`() {
        // companion-skin-gacha capability scenario: per-skin probabilities
        // for a single character MUST sum to the character's rarity-
        // aggregated probability within ±1 percentage point.
        val pool = listOf(
            entry("hero", "Hero", "hero-default", 100, GachaRarity.Common),
            entry("hero", "Hero", "hero-epic",      5, GachaRarity.Epic),
            entry("hero", "Hero", "hero-legend",    1, GachaRarity.Legendary),
            entry("rival","Rival","rival-default",  50, GachaRarity.Common),
        )
        val tree = computeProbabilityTree(pool)
        for (char in tree.characters) {
            val skinSum = char.skins.sumOf { it.probability.toDouble() }
            val delta = abs(skinSum - char.probability.toDouble())
            assertTrue(
                "Per-skin sum ($skinSum) must equal character probability (${char.probability}) within 0.01",
                delta <= 0.01,
            )
        }
    }

    @Test
    fun `probabilities sum to 1 across the whole tree within rounding tolerance`() {
        val pool = listOf(
            entry("a", "A", "a-default", 70, GachaRarity.Common),
            entry("b", "B", "b-default", 25, GachaRarity.Rare),
            entry("c", "C", "c-default",  5, GachaRarity.Epic),
        )
        val tree = computeProbabilityTree(pool)
        val total = tree.characters.sumOf { it.probability.toDouble() }
        assertTrue("Total probability ($total) must round to 1.0", abs(total - 1.0) <= 0.001)
    }

    @Test
    fun `topRarity equals the highest rarity among the character's skins`() {
        val pool = listOf(
            entry("hero", "Hero", "hero-default",  100, GachaRarity.Common),
            entry("hero", "Hero", "hero-epic",       5, GachaRarity.Epic),
            entry("hero", "Hero", "hero-legend",     1, GachaRarity.Legendary),
        )
        val tree = computeProbabilityTree(pool)
        assertEquals(GachaRarity.Legendary, tree.characters.single().topRarity)
    }

    @Test
    fun `bridge projects each draw-pool card to one default-skin row`() {
        val drawPool = listOf(
            com.gkim.im.android.core.model.CompanionCharacterCard(
                id = "midnight-sutler",
                displayName = com.gkim.im.android.core.model.LocalizedText("Midnight Sutler", "午夜密使"),
                roleLabel = com.gkim.im.android.core.model.LocalizedText.Empty,
                summary = com.gkim.im.android.core.model.LocalizedText.Empty,
                firstMes = com.gkim.im.android.core.model.LocalizedText.Empty,
                avatarText = "MS",
                accent = com.gkim.im.android.core.model.AccentTone.Primary,
                source = com.gkim.im.android.core.model.CompanionCharacterSource.Drawn,
                tags = listOf("epic"),
            ),
        )
        val skinPool = projectCharacterPoolAsSkinPool(drawPool) { it.id }
        assertEquals(1, skinPool.size)
        assertEquals("midnight-sutler-default", skinPool.single().skinId)
        assertEquals(GachaRarity.Epic, skinPool.single().rarity)
    }

    private fun entry(
        cid: String,
        cname: String,
        sid: String,
        weight: Int,
        rarity: GachaRarity,
    ): SkinPoolEntry = SkinPoolEntry(
        characterId = cid,
        characterDisplayName = cname,
        skinId = sid,
        skinDisplayName = sid,
        weight = weight,
        rarity = rarity,
    )
}
