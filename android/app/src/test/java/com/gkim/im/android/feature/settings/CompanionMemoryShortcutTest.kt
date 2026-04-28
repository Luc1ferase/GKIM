package com.gkim.im.android.feature.settings

import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.LocalizedText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionMemoryShortcutTest {

    private fun card(
        id: String,
        english: String,
        chinese: String,
        source: CompanionCharacterSource,
    ): CompanionCharacterCard = CompanionCharacterCard(
        id = id,
        displayName = LocalizedText(english = english, chinese = chinese),
        roleLabel = LocalizedText(english = "role-$english", chinese = "角色-$chinese"),
        summary = LocalizedText.Empty,
        firstMes = LocalizedText(english = "hi", chinese = "你好"),
        avatarText = english.take(1),
        accent = AccentTone.Primary,
        source = source,
    )

    @Test
    fun `menu exposes companion memory shortcut routing to chooser destination`() {
        val item = buildSettingsMenuItems(SettingsUiState())
            .firstOrNull { it.testTag == "settings-menu-companion-memory" }
        assertTrue("companion memory shortcut must be present", item != null)
        assertEquals(SettingsDestination.CompanionMemoryChooser, item!!.destination)
        assertEquals("Companion memory", item.englishLabel)
        assertEquals("伙伴记忆", item.chineseLabel)
    }

    @Test
    fun `companion section includes memory shortcut as its last item`() {
        val sections = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
        val companion = sections.first { it.id == SettingsSectionId.Companion }
        val last = companion.items.lastOrNull()
        assertTrue("companion section must have memory shortcut last", last != null)
        assertEquals(SettingsDestination.CompanionMemoryChooser, last!!.destination)
    }

    @Test
    fun `chooser entries hoist the active card to the top when it is not already first`() {
        val preset1 = card("preset-1", "Alice", "爱丽丝", CompanionCharacterSource.Preset)
        val preset2 = card("preset-2", "Bob", "鲍勃", CompanionCharacterSource.Preset)
        val owned = card("drawn-1", "Cara", "卡拉", CompanionCharacterSource.Drawn)

        val entries = buildCompanionMemoryChooserEntries(
            presetCharacters = listOf(preset1, preset2),
            ownedCharacters = listOf(owned),
            userCharacters = emptyList(),
            activeCardId = "preset-2",
        )

        assertEquals("active card must be first", "preset-2", entries.first().cardId)
        val remaining = entries.drop(1).map { it.cardId }
        assertEquals(listOf("drawn-1", "preset-1"), remaining)
    }

    @Test
    fun `chooser entries keep natural order when active card is already first`() {
        val user1 = card("user-1", "Dana", "丹娜", CompanionCharacterSource.UserAuthored)
        val preset1 = card("preset-1", "Alice", "爱丽丝", CompanionCharacterSource.Preset)

        val entries = buildCompanionMemoryChooserEntries(
            presetCharacters = listOf(preset1),
            ownedCharacters = emptyList(),
            userCharacters = listOf(user1),
            activeCardId = "user-1",
        )

        assertEquals(listOf("user-1", "preset-1"), entries.map { it.cardId })
        assertTrue("user-1 is flagged active", entries.first().isActive)
        assertFalse("preset-1 is not active", entries[1].isActive)
    }

    @Test
    fun `chooser entries fall back to natural order when no active card matches`() {
        val preset1 = card("preset-1", "Alice", "爱丽丝", CompanionCharacterSource.Preset)
        val user1 = card("user-1", "Dana", "丹娜", CompanionCharacterSource.UserAuthored)

        val entries = buildCompanionMemoryChooserEntries(
            presetCharacters = listOf(preset1),
            ownedCharacters = emptyList(),
            userCharacters = listOf(user1),
            activeCardId = "ghost-id",
        )

        assertEquals(listOf("user-1", "preset-1"), entries.map { it.cardId })
        entries.forEach { assertFalse("nothing active when id does not match", it.isActive) }
    }

    @Test
    fun `chooser entries merge user then owned then preset in that order`() {
        val preset = card("preset-x", "P", "P", CompanionCharacterSource.Preset)
        val owned = card("drawn-y", "O", "O", CompanionCharacterSource.Drawn)
        val user = card("user-z", "U", "U", CompanionCharacterSource.UserAuthored)

        val entries = buildCompanionMemoryChooserEntries(
            presetCharacters = listOf(preset),
            ownedCharacters = listOf(owned),
            userCharacters = listOf(user),
            activeCardId = "",
        )

        assertEquals(
            "user first, then owned, then preset",
            listOf("user-z", "drawn-y", "preset-x"),
            entries.map { it.cardId },
        )
    }

    @Test
    fun `chooser entries deduplicate ids across lists keeping first occurrence`() {
        val shared = card("shared", "Shared", "共有", CompanionCharacterSource.Preset)
        val alsoInOwned = card("shared", "Shared", "共有", CompanionCharacterSource.Drawn)

        val entries = buildCompanionMemoryChooserEntries(
            presetCharacters = listOf(shared),
            ownedCharacters = listOf(alsoInOwned),
            userCharacters = emptyList(),
            activeCardId = "",
        )

        assertEquals(1, entries.size)
        assertEquals("shared", entries.single().cardId)
    }

    @Test
    fun `chooser entries carry bilingual display and role labels`() {
        val card = card("card-1", "Eve", "伊芙", CompanionCharacterSource.Preset)

        val entry = buildCompanionMemoryChooserEntries(
            presetCharacters = listOf(card),
            ownedCharacters = emptyList(),
            userCharacters = emptyList(),
            activeCardId = "card-1",
        ).single()

        assertEquals("Eve", entry.displayName.english)
        assertEquals("伊芙", entry.displayName.chinese)
        assertEquals("role-Eve", entry.roleLabel.english)
        assertEquals("角色-伊芙", entry.roleLabel.chinese)
        assertTrue(entry.isActive)
    }

    @Test
    fun `chooser entries handle empty rosters gracefully`() {
        val entries = buildCompanionMemoryChooserEntries(
            presetCharacters = emptyList(),
            ownedCharacters = emptyList(),
            userCharacters = emptyList(),
            activeCardId = "anything",
        )

        assertTrue("empty roster yields empty chooser", entries.isEmpty())
    }

    @Test
    fun `companion memory panel destination is reachable from chooser selection`() {
        val destinationFromChooser = SettingsDestination.CompanionMemoryPanel
        assertEquals("CompanionMemoryPanel", destinationFromChooser.name)
        val chooserDestination = SettingsDestination.CompanionMemoryChooser
        assertEquals("CompanionMemoryChooser", chooserDestination.name)
    }
}
