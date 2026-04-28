package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.CharacterSkin
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.SkinRarity
import com.gkim.im.android.core.model.SkinTrait
import com.gkim.im.android.core.model.SkinTraitKind
import com.gkim.im.android.core.model.SkinTraitPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TraitProgressStripTest {

    @Test
    fun `progress reports owned-vs-total counts for skins and traits`() {
        val catalog = listOf(
            skin("hero-default",  SkinRarity.Common,    isDefault = true,  traits = 0),
            skin("hero-epic",     SkinRarity.Epic,      isDefault = false, traits = 2),
            skin("hero-legend",   SkinRarity.Legendary, isDefault = false, traits = 3),
        )
        val owned = setOf("hero-default", "hero-epic")
        val snap = computeTraitProgress(catalog, owned)
        assertEquals(2, snap.ownedSkins)
        assertEquals(3, snap.totalSkins)
        assertEquals(2, snap.ownedTraits)         // only hero-epic's 2 traits owned
        assertEquals(5, snap.totalTraits)         // 0 + 2 + 3
    }

    @Test
    fun `tooltip targets the lowest-rarity unowned skin first`() {
        // R6.2 contract: next-chase tooltip points at the most attainable
        // unowned skin so the user has a near-term goal, not the rarest.
        val catalog = listOf(
            skin("hero-default",  SkinRarity.Common,    isDefault = true,  traits = 0),
            skin("hero-rare",     SkinRarity.Rare,      isDefault = false, traits = 1),
            skin("hero-epic",     SkinRarity.Epic,      isDefault = false, traits = 2),
            skin("hero-legend",   SkinRarity.Legendary, isDefault = false, traits = 3),
        )
        val ownedDefaultOnly = setOf("hero-default")
        val snap = computeTraitProgress(catalog, ownedDefaultOnly)
        assertEquals("hero-rare", snap.nextChaseSkinId)
    }

    @Test
    fun `tooltip target ties resolve alphabetically by skinId`() {
        val catalog = listOf(
            skin("hero-zeta",  SkinRarity.Epic, isDefault = false, traits = 1),
            skin("hero-alpha", SkinRarity.Epic, isDefault = false, traits = 1),
            skin("hero-beta",  SkinRarity.Epic, isDefault = false, traits = 1),
        )
        val snap = computeTraitProgress(catalog, ownedSkinIds = emptySet())
        assertEquals("hero-alpha", snap.nextChaseSkinId)
    }

    @Test
    fun `tooltip target is null when everything is owned`() {
        val catalog = listOf(
            skin("hero-default", SkinRarity.Common, isDefault = true, traits = 0),
            skin("hero-epic",    SkinRarity.Epic,   isDefault = false, traits = 1),
        )
        val snap = computeTraitProgress(catalog, ownedSkinIds = setOf("hero-default", "hero-epic"))
        assertNull(snap.nextChaseSkinId)
    }

    private fun skin(
        skinId: String,
        rarity: SkinRarity,
        isDefault: Boolean,
        traits: Int,
    ): CharacterSkin = CharacterSkin(
        skinId = skinId,
        characterId = "hero",
        displayName = LocalizedText(skinId, skinId),
        rarity = rarity,
        artVersion = 1,
        isDefault = isDefault,
        traits = (1..traits).map { i ->
            SkinTrait(
                traitId = "$skinId-trait-$i",
                kind = SkinTraitKind.PersonaMod,
                description = LocalizedText("trait-$i", "trait-$i"),
                payload = SkinTraitPayload.PersonaMod(LocalizedText("appendix", "附录")),
            )
        },
    )
}
