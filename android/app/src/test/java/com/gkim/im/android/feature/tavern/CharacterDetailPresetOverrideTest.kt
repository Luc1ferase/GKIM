package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Preset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §4.2 verification — the character editor exposes an "Override preset" row backed by three
 * pure helpers (`presetOverrideOptions`, `resolvePresetOverrideRowLabel`, `applyPresetSelection`)
 * plus the `DefaultPresetOverrideLabel` sentinel. The composable in `PresetOverrideRow.kt`
 * holds a small open/closed boolean for the picker dialog, but the option list, the selection
 * commit shape, and the row's rendered label all flow through these helpers — so this test
 * exercises the data contract the picker presents and the writeback path that lands a
 * selection on the card.
 *
 * The verification command names this class explicitly:
 * `:app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.CharacterDetailPresetOverrideTest`.
 */
class CharacterDetailPresetOverrideTest {

    // -------------------------------------------------------------------------
    // Picker invocation — what the picker shows when opened
    // -------------------------------------------------------------------------

    @Test
    fun `presetOverrideOptions always begins with the Default sentinel`() {
        val options = presetOverrideOptions(presets = listOf(presetA, presetB))
        assertTrue("options must not be empty", options.isNotEmpty())
        assertNull("the leading option's presetId must be null (Default)", options.first().presetId)
        assertEquals(DefaultPresetOverrideLabel, options.first().displayName)
    }

    @Test
    fun `presetOverrideOptions includes every preset preserving repository order`() {
        val options = presetOverrideOptions(presets = listOf(presetA, presetB, presetC))
        // 1 Default sentinel + 3 presets
        assertEquals(4, options.size)
        assertEquals(listOf(null, "preset-a", "preset-b", "preset-c"), options.map { it.presetId })
    }

    @Test
    fun `presetOverrideOptions with empty preset library still surfaces the Default option`() {
        val options = presetOverrideOptions(presets = emptyList())
        assertEquals(1, options.size)
        assertNull(options.single().presetId)
    }

    @Test
    fun `Default sentinel option carries a bilingual label`() {
        assertEquals("Default (follow global active)", DefaultPresetOverrideLabel.english)
        assertEquals("默认（跟随全局活跃 preset）", DefaultPresetOverrideLabel.chinese)
    }

    @Test
    fun `each non-default option carries its preset's bilingual display name unchanged`() {
        val options = presetOverrideOptions(presets = listOf(presetA, presetB))
        val nonDefault = options.drop(1)
        assertEquals(presetA.displayName, nonDefault[0].displayName)
        assertEquals(presetB.displayName, nonDefault[1].displayName)
    }

    // -------------------------------------------------------------------------
    // Selection persistence — what lands on the card when the user picks an option
    // -------------------------------------------------------------------------

    @Test
    fun `applyPresetSelection writes the chosen presetId onto characterPresetId`() {
        val card = baseCard()
        val updated = applyPresetSelection(card, "preset-a")
        assertEquals("preset-a", updated.characterPresetId)
    }

    @Test
    fun `applyPresetSelection preserves every other field on the card`() {
        val card = baseCard().copy(
            roleLabel = LocalizedText("Sage", "智者"),
            tags = listOf("scholar", "loremaster"),
        )
        val updated = applyPresetSelection(card, "preset-b")
        assertEquals(card.id, updated.id)
        assertEquals(card.displayName, updated.displayName)
        assertEquals(card.roleLabel, updated.roleLabel)
        assertEquals(card.tags, updated.tags)
        assertEquals(card.source, updated.source)
        assertEquals(card.extensions, updated.extensions)
        assertEquals("preset-b", updated.characterPresetId)
    }

    @Test
    fun `selecting a preset replaces a prior override on the card`() {
        val starting = baseCard().copy(characterPresetId = "preset-a")
        val updated = applyPresetSelection(starting, "preset-c")
        assertEquals("preset-c", updated.characterPresetId)
    }

    // -------------------------------------------------------------------------
    // Clear-to-default — selecting the Default option clears the override
    // -------------------------------------------------------------------------

    @Test
    fun `applying the Default option's presetId clears any prior override to null`() {
        val starting = baseCard().copy(characterPresetId = "preset-a")
        val defaultOption = presetOverrideOptions(listOf(presetA)).first()
        val cleared = applyPresetSelection(starting, defaultOption.presetId)
        assertNull(
            "selecting the Default option must yield characterPresetId == null",
            cleared.characterPresetId,
        )
    }

    @Test
    fun `applyPresetSelection with null on a card that already had no override stays null`() {
        val starting = baseCard()
        assertNull(starting.characterPresetId)
        val cleared = applyPresetSelection(starting, null)
        assertNull(cleared.characterPresetId)
    }

    // -------------------------------------------------------------------------
    // Row label resolution — what the closed row renders before the picker opens
    // -------------------------------------------------------------------------

    @Test
    fun `resolvePresetOverrideRowLabel returns the Default label when characterPresetId is null`() {
        assertEquals(
            DefaultPresetOverrideLabel.english,
            resolvePresetOverrideRowLabel(null, listOf(presetA), AppLanguage.English),
        )
        assertEquals(
            DefaultPresetOverrideLabel.chinese,
            resolvePresetOverrideRowLabel(null, listOf(presetA), AppLanguage.Chinese),
        )
    }

    @Test
    fun `resolvePresetOverrideRowLabel returns the matching preset's localized name`() {
        assertEquals(
            "Preset Alpha",
            resolvePresetOverrideRowLabel("preset-a", listOf(presetA, presetB), AppLanguage.English),
        )
        assertEquals(
            "预设甲",
            resolvePresetOverrideRowLabel("preset-a", listOf(presetA, presetB), AppLanguage.Chinese),
        )
    }

    @Test
    fun `resolvePresetOverrideRowLabel returns the stale-fallback when no preset matches`() {
        // The card carries an override pointing at a preset that has since been deleted from
        // the library. The label degrades to a localized "removed" notice rather than showing
        // the raw id (which would be confusing) or the Default label (which would mis-imply the
        // override has been cleared when in fact the typed field is still non-null).
        assertEquals(
            "Override (preset removed)",
            resolvePresetOverrideRowLabel(
                characterPresetId = "preset-vanished",
                presets = listOf(presetA, presetB),
                language = AppLanguage.English,
            ),
        )
        assertEquals(
            "覆盖（preset 已移除）",
            resolvePresetOverrideRowLabel(
                characterPresetId = "preset-vanished",
                presets = listOf(presetA, presetB),
                language = AppLanguage.Chinese,
            ),
        )
    }

    @Test
    fun `picker invocation surfaces an option whose presetId matches the row label resolution`() {
        // Round-trip: the option list shown in the picker must contain an option whose
        // presetId matches what resolvePresetOverrideRowLabel would render the row's label
        // with — this is the contract that lets a user open the picker, see their current
        // selection highlighted, and tap it (or another) to commit. Without this contract
        // a card carrying a valid override would show its name on the row but find no
        // matching option in the picker.
        val presets = listOf(presetA, presetB, presetC)
        val starting = baseCard().copy(characterPresetId = "preset-b")
        val rowLabel = resolvePresetOverrideRowLabel(
            starting.characterPresetId,
            presets,
            AppLanguage.English,
        )
        val options = presetOverrideOptions(presets)
        val matching = options.firstOrNull { it.presetId == starting.characterPresetId }
        assertNotNull("the picker must include an option matching the current override", matching)
        assertEquals(rowLabel, matching!!.displayName.resolve(AppLanguage.English))
        // And the Default option remains available so the user can clear back to default
        assertNotNull(options.firstOrNull { it.presetId == null })
    }

    @Test
    fun `picker invocation contract for a stale override still exposes Default plus all live presets`() {
        // The user opened the picker on a card whose previously-selected preset has been
        // removed from the library. The picker still shows Default + the live presets so the
        // user can either clear or pick a fresh override; no option matches the stale id.
        val presets = listOf(presetA, presetB)
        val starting = baseCard().copy(characterPresetId = "preset-vanished")
        val options = presetOverrideOptions(presets)
        assertEquals(3, options.size) // Default + 2 live
        assertNull(
            "stale override must not match any option in the picker",
            options.firstOrNull { it.presetId == starting.characterPresetId },
        )
        assertNotNull(options.firstOrNull { it.presetId == null })
        assertNotNull(options.firstOrNull { it.presetId == "preset-a" })
        assertNotNull(options.firstOrNull { it.presetId == "preset-b" })
    }

    @Test
    fun `clear-to-default end-to-end flow yields a card that no longer carries an override`() {
        val presets = listOf(presetA, presetB)
        val starting = baseCard().copy(characterPresetId = "preset-a")
        // 1. picker opens, shows the option list with Default first
        val options = presetOverrideOptions(presets)
        val defaultOption = options.first()
        assertNull(defaultOption.presetId)
        // 2. user taps Default → selection is committed against the card via applyPresetSelection
        val afterCommit = applyPresetSelection(starting, defaultOption.presetId)
        // 3. row re-renders: its label resolves to the Default label, no override is carried
        assertNull(afterCommit.characterPresetId)
        assertEquals(
            DefaultPresetOverrideLabel.english,
            resolvePresetOverrideRowLabel(
                afterCommit.characterPresetId,
                presets,
                AppLanguage.English,
            ),
        )
    }

    @Test
    fun `selection persistence end-to-end flow lands the chosen presetId on the card`() {
        val presets = listOf(presetA, presetB, presetC)
        val starting = baseCard()
        assertNull(starting.characterPresetId)
        // user opens picker, selects preset-c
        val options = presetOverrideOptions(presets)
        val chosen = options.first { it.presetId == "preset-c" }
        val afterCommit = applyPresetSelection(starting, chosen.presetId)
        assertEquals("preset-c", afterCommit.characterPresetId)
        assertEquals(
            "Preset Gamma",
            resolvePresetOverrideRowLabel(
                afterCommit.characterPresetId,
                presets,
                AppLanguage.English,
            ),
        )
    }

    @Test
    fun `applyPresetSelection does not touch extensions so the §4_1 round-trip still works`() {
        // §4.1 guarantees that fromCompanionCharacterCard re-syncs extensions.st.charPresetId
        // from card.characterPresetId. §4.2's row writes only the typed field and leaves
        // extensions untouched, so the round-trip remains the responsibility of the DTO mapper
        // — which is exactly what we want, otherwise §4.2 would have to know about wire shape.
        val starting = baseCard()
        assertFalse(
            "baseline card must not carry any extensions.st key",
            starting.extensions.containsKey("st"),
        )
        val updated = applyPresetSelection(starting, "preset-a")
        assertEquals(
            "applyPresetSelection must keep extensions identical (DTO mapper handles wire sync)",
            starting.extensions,
            updated.extensions,
        )
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private val presetA = Preset(
        id = "preset-a",
        displayName = LocalizedText("Preset Alpha", "预设甲"),
    )
    private val presetB = Preset(
        id = "preset-b",
        displayName = LocalizedText("Preset Beta", "预设乙"),
    )
    private val presetC = Preset(
        id = "preset-c",
        displayName = LocalizedText("Preset Gamma", "预设丙"),
    )

    private fun baseCard(): CompanionCharacterCard = CompanionCharacterCard(
        id = "card-test",
        displayName = LocalizedText("Test Card", "测试卡"),
        roleLabel = LocalizedText("Companion", "伙伴"),
        summary = LocalizedText("A card for testing", "测试用卡片"),
        firstMes = LocalizedText("Hello!", "你好！"),
        avatarText = "T",
        accent = AccentTone.Primary,
        source = CompanionCharacterSource.UserAuthored,
    )
}
