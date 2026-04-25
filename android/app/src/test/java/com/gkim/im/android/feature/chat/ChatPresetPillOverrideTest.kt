package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Preset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §4.3 verification — when the active companion card carries a `characterPresetId`, the chat
 * chrome's preset pill surfaces the override visually (suffix-marked label + `isOverride =
 * true` flag for the renderer's color/border distinction) and routes to the card's detail
 * surface (`tavern/detail/<cardId>`) where the §4.2 editor row can clear the override.
 *
 * When no override is set the pill keeps its original behavior verified by
 * [ChatChromePresentationTest]: no suffix, settings destination, `isOverride = false`.
 *
 * The verification command names this class explicitly:
 * `:app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatPresetPillOverrideTest`.
 */
class ChatPresetPillOverrideTest {

    // -------------------------------------------------------------------------
    // Override-active branch — rendered label includes the (card override) suffix
    // -------------------------------------------------------------------------

    @Test
    fun `english override label is preset name plus the english suffix`() {
        val pill = chatChromePresetPill(
            activePreset = global,
            language = AppLanguage.English,
            activeCardCharacterPresetId = "preset-warm",
            activeCardId = "card-architect",
            presets = listOf(global, warm),
        )
        assertEquals("Warm (card override)", pill.label)
        assertTrue("suffix must be present in english", pill.label.endsWith(" (card override)"))
    }

    @Test
    fun `chinese override label is preset name plus the chinese suffix`() {
        val pill = chatChromePresetPill(
            activePreset = global,
            language = AppLanguage.Chinese,
            activeCardCharacterPresetId = "preset-warm",
            activeCardId = "card-architect",
            presets = listOf(global, warm),
        )
        assertEquals("温暖（卡片覆盖）", pill.label)
        assertTrue("suffix must be present in chinese", pill.label.endsWith("（卡片覆盖）"))
    }

    @Test
    fun `english override label uses removed-fallback when the override id no longer matches`() {
        val pill = chatChromePresetPill(
            activePreset = global,
            language = AppLanguage.English,
            activeCardCharacterPresetId = "preset-vanished",
            activeCardId = "card-architect",
            presets = listOf(global, warm),
        )
        assertEquals("Override (preset removed) (card override)", pill.label)
    }

    @Test
    fun `chinese override label uses removed-fallback when the override id no longer matches`() {
        val pill = chatChromePresetPill(
            activePreset = global,
            language = AppLanguage.Chinese,
            activeCardCharacterPresetId = "preset-vanished",
            activeCardId = "card-architect",
            presets = listOf(global, warm),
        )
        assertEquals("覆盖（preset 已移除）（卡片覆盖）", pill.label)
    }

    @Test
    fun `override pill carries the override id in activePresetId not the global preset id`() {
        val pill = chatChromePresetPill(
            activePreset = global,
            language = AppLanguage.English,
            activeCardCharacterPresetId = "preset-warm",
            activeCardId = "card-architect",
            presets = listOf(global, warm),
        )
        assertEquals("preset-warm", pill.activePresetId)
    }

    @Test
    fun `override pill marks isOverride true so the renderer can apply a distinct treatment`() {
        val pill = chatChromePresetPill(
            activePreset = global,
            language = AppLanguage.English,
            activeCardCharacterPresetId = "preset-warm",
            activeCardId = "card-architect",
            presets = listOf(global, warm),
        )
        assertTrue(pill.isOverride)
    }

    // -------------------------------------------------------------------------
    // Route target — overridden pill routes to the active card's detail surface
    // -------------------------------------------------------------------------

    @Test
    fun `override pill destinationRoute targets the active card's tavern detail surface`() {
        val pill = chatChromePresetPill(
            activePreset = global,
            language = AppLanguage.English,
            activeCardCharacterPresetId = "preset-warm",
            activeCardId = "card-architect",
            presets = listOf(global, warm),
        )
        assertEquals("tavern/detail/card-architect", pill.destinationRoute)
    }

    @Test
    fun `override pill destinationRoute follows the active card's id even with non-ascii characters`() {
        val pill = chatChromePresetPill(
            activePreset = global,
            language = AppLanguage.English,
            activeCardCharacterPresetId = "preset-warm",
            activeCardId = "卡片-中文-id",
            presets = listOf(global, warm),
        )
        assertEquals("tavern/detail/卡片-中文-id", pill.destinationRoute)
    }

    @Test
    fun `override pill without activeCardId degrades route to settings rather than malformed link`() {
        val pill = chatChromePresetPill(
            activePreset = global,
            language = AppLanguage.English,
            activeCardCharacterPresetId = "preset-warm",
            activeCardId = null,
            presets = listOf(global, warm),
        )
        assertEquals(ChatChromePresetPillDefaults.DestinationRoute, pill.destinationRoute)
        assertTrue("isOverride still true so the rendered label is still suffix-marked", pill.isOverride)
        assertEquals("Warm (card override)", pill.label)
    }

    // -------------------------------------------------------------------------
    // No-override branch — keep existing behavior verified by ChatChromePresentationTest
    // -------------------------------------------------------------------------

    @Test
    fun `no-override pill keeps existing english label without any suffix`() {
        val pill = chatChromePresetPill(
            activePreset = global,
            language = AppLanguage.English,
            activeCardCharacterPresetId = null,
            activeCardId = "card-architect",
            presets = listOf(global, warm),
        )
        assertEquals("Global", pill.label)
        assertFalse(pill.isOverride)
        assertEquals(ChatChromePresetPillDefaults.DestinationRoute, pill.destinationRoute)
        assertEquals("preset-global", pill.activePresetId)
    }

    @Test
    fun `no-override pill keeps existing chinese label without any suffix`() {
        val pill = chatChromePresetPill(
            activePreset = global,
            language = AppLanguage.Chinese,
            activeCardCharacterPresetId = null,
            activeCardId = "card-architect",
            presets = listOf(global, warm),
        )
        assertEquals("全局", pill.label)
        assertFalse(pill.isOverride)
    }

    @Test
    fun `no-override pill with no active preset keeps the bilingual fallback label`() {
        val english = chatChromePresetPill(
            activePreset = null,
            language = AppLanguage.English,
            activeCardCharacterPresetId = null,
            activeCardId = null,
            presets = emptyList(),
        )
        assertEquals(ChatChromePresetPillDefaults.FallbackLabelEnglish, english.label)
        assertFalse(english.isOverride)

        val chinese = chatChromePresetPill(
            activePreset = null,
            language = AppLanguage.Chinese,
            activeCardCharacterPresetId = null,
            activeCardId = null,
            presets = emptyList(),
        )
        assertEquals(ChatChromePresetPillDefaults.FallbackLabelChinese, chinese.label)
    }

    // -------------------------------------------------------------------------
    // Backward compatibility — calling with only the original two args
    // -------------------------------------------------------------------------

    @Test
    fun `factory called with only the original two args produces a non-override pill`() {
        // Existing call sites (ChatChromePresentationTest's six tests) still work.
        val pill = chatChromePresetPill(activePreset = global, language = AppLanguage.English)
        assertEquals("Global", pill.label)
        assertFalse(pill.isOverride)
        assertEquals("settings", pill.destinationRoute)
        assertEquals("preset-global", pill.activePresetId)
        assertNull("no override present, so the override-derived id field stays null on the typed flag",
            pill.takeIf { it.isOverride }?.activePresetId)
    }

    // -------------------------------------------------------------------------
    // Round-trip / route-target hygiene — destination always parses cleanly
    // -------------------------------------------------------------------------

    @Test
    fun `override destination is always one of (settings, tavern detail prefixed)`() {
        val withCard = chatChromePresetPill(
            activePreset = global,
            language = AppLanguage.English,
            activeCardCharacterPresetId = "preset-warm",
            activeCardId = "x",
            presets = listOf(global, warm),
        )
        assertTrue(
            "with activeCardId, route must be tavern/detail-prefixed",
            withCard.destinationRoute.startsWith(
                "${ChatChromePresetPillDefaults.OverrideDestinationRoutePrefix}/",
            ),
        )

        val withoutCard = chatChromePresetPill(
            activePreset = global,
            language = AppLanguage.English,
            activeCardCharacterPresetId = "preset-warm",
            activeCardId = null,
            presets = listOf(global, warm),
        )
        assertEquals(
            "without activeCardId, route falls back to settings",
            ChatChromePresetPillDefaults.DestinationRoute,
            withoutCard.destinationRoute,
        )
    }

    @Test
    fun `override pill with empty preset library still renders the removed-fallback label`() {
        val pill = chatChromePresetPill(
            activePreset = null,
            language = AppLanguage.English,
            activeCardCharacterPresetId = "preset-anything",
            activeCardId = "card-x",
            presets = emptyList(),
        )
        assertEquals("Override (preset removed) (card override)", pill.label)
        assertTrue(pill.isOverride)
        assertEquals("tavern/detail/card-x", pill.destinationRoute)
    }

    @Test
    fun `clearing the override on the same card (id transitions to null) returns the pill to non-override shape`() {
        // Simulate the user clearing the override on the editor row: the same call site,
        // same active card, just with activeCardCharacterPresetId now null.
        val before = chatChromePresetPill(
            activePreset = global,
            language = AppLanguage.English,
            activeCardCharacterPresetId = "preset-warm",
            activeCardId = "card-architect",
            presets = listOf(global, warm),
        )
        val after = chatChromePresetPill(
            activePreset = global,
            language = AppLanguage.English,
            activeCardCharacterPresetId = null,
            activeCardId = "card-architect",
            presets = listOf(global, warm),
        )
        assertTrue(before.isOverride)
        assertFalse(after.isOverride)
        assertEquals("Warm (card override)", before.label)
        assertEquals("Global", after.label)
        assertEquals("tavern/detail/card-architect", before.destinationRoute)
        assertEquals("settings", after.destinationRoute)
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private val global = Preset(
        id = "preset-global",
        displayName = LocalizedText("Global", "全局"),
        isActive = true,
    )

    private val warm = Preset(
        id = "preset-warm",
        displayName = LocalizedText("Warm", "温暖"),
    )
}
