package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.SkinRarity
import com.gkim.im.android.core.model.resolve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionRosterRepositoryActiveSkinTest {

    @Test
    fun `every CompanionCharacterCard defaults activeSkinId to default`() {
        val card = newCard(id = "architect-oracle")
        assertEquals(CompanionCharacterCard.DEFAULT_SKIN_ID, card.activeSkinId)
        assertEquals("default", CompanionCharacterCard.DEFAULT_SKIN_ID)
    }

    @Test
    fun `resolve preserves activeSkinId from the source card`() {
        val card = newCard(id = "sunlit-almoner").copy(activeSkinId = "sunlit-almoner-lantern")
        val resolved = card.resolve(AppLanguage.English)
        assertEquals("sunlit-almoner-lantern", resolved.activeSkinId)
    }

    @Test
    fun `seed catalog contains exactly the five seeded characters' default skins`() {
        val skins = SeedDefaultSkins
        assertEquals(5, skins.size)
        val expectedCharacterIds = setOf(
            "architect-oracle",
            "sunlit-almoner",
            "midnight-sutler",
            "opal-lantern",
            "glass-mariner",
        )
        assertEquals(expectedCharacterIds, skins.map { it.characterId }.toSet())
        // Every seed row is the character's `is_default = true` row.
        assertTrue("Every seed row must be a default skin", skins.all { it.isDefault })
        // Every seed row's PK is qualified as {characterId}-default per design.md.
        assertTrue(
            "Every seed skinId is suffixed -default",
            skins.all { it.skinId.endsWith("-default") },
        )
        // Every seed row's rarity is Common (default skins are never EPIC+).
        assertTrue("Every seed skin is Common rarity", skins.all { it.rarity == SkinRarity.Common })
        // Default skins MUST NOT carry traits (companion-skin-gacha capability scenario).
        assertTrue("Default skins carry zero traits", skins.all { it.traits.isEmpty() })
    }

    @Test
    fun `repository activeSkinIdFor falls back to default when no override exists`() {
        val repo = InMemoryCompanionSkinRepository()
        assertEquals(CompanionCharacterCard.DEFAULT_SKIN_ID, repo.activeSkinIdFor("architect-oracle"))
        assertEquals(CompanionCharacterCard.DEFAULT_SKIN_ID, repo.activeSkinIdFor("any-unknown-character"))
    }

    @Test
    fun `repository activateSkin updates the active map`() {
        val repo = InMemoryCompanionSkinRepository()
        // Default skin is owned implicitly per the seed.
        repo.activateSkin("architect-oracle", "architect-oracle-default")
        assertEquals("architect-oracle-default", repo.activeSkinIdFor("architect-oracle"))
    }

    @Test
    fun `repository activateSkin rejects unowned skin`() {
        val repo = InMemoryCompanionSkinRepository()
        assertThrows(IllegalArgumentException::class.java) {
            repo.activateSkin("architect-oracle", "architect-oracle-lantern-keeper")
        }
    }

    private fun newCard(id: String): CompanionCharacterCard = CompanionCharacterCard(
        id = id,
        displayName = LocalizedText("Name", "名"),
        roleLabel = LocalizedText("Role", "职"),
        summary = LocalizedText("Summary", "概要"),
        firstMes = LocalizedText("Hi", "你好"),
        avatarText = "X",
        accent = AccentTone.Primary,
        source = CompanionCharacterSource.Preset,
    )
}
