package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Preset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatChromePresentationTest {

    @Test
    fun `active preset label resolves in english`() {
        val preset = samplePreset()
        val pill = chatChromePresetPill(preset, AppLanguage.English)
        assertEquals("Concise", pill.label)
        assertEquals(preset.id, pill.activePresetId)
    }

    @Test
    fun `active preset label resolves in chinese`() {
        val preset = samplePreset()
        val pill = chatChromePresetPill(preset, AppLanguage.Chinese)
        assertEquals("简洁", pill.label)
    }

    @Test
    fun `null active preset falls back to english placeholder in english`() {
        val pill = chatChromePresetPill(activePreset = null, language = AppLanguage.English)
        assertEquals(ChatChromePresetPillDefaults.FallbackLabelEnglish, pill.label)
        assertNull(pill.activePresetId)
    }

    @Test
    fun `null active preset falls back to chinese placeholder in chinese`() {
        val pill = chatChromePresetPill(activePreset = null, language = AppLanguage.Chinese)
        assertEquals(ChatChromePresetPillDefaults.FallbackLabelChinese, pill.label)
    }

    @Test
    fun `preset pill destination route points at settings`() {
        val pill = chatChromePresetPill(samplePreset(), AppLanguage.English)
        assertEquals("settings", pill.destinationRoute)
    }

    @Test
    fun `preset pill label updates when active preset changes`() {
        val consise = chatChromePresetPill(samplePreset(), AppLanguage.English)
        val warm = samplePreset().copy(
            id = "preset-warm",
            displayName = LocalizedText("Warm", "温暖"),
        )
        val warmPill = chatChromePresetPill(warm, AppLanguage.English)
        assertNotEquals(consise.label, warmPill.label)
        assertEquals("Warm", warmPill.label)
        assertNotEquals(consise.activePresetId, warmPill.activePresetId)
    }

    @Test
    fun `preset pill label updates when language changes without switching preset`() {
        val preset = samplePreset()
        val english = chatChromePresetPill(preset, AppLanguage.English)
        val chinese = chatChromePresetPill(preset, AppLanguage.Chinese)
        assertNotEquals(english.label, chinese.label)
        assertEquals(english.activePresetId, chinese.activePresetId)
    }

    @Test
    fun `memory entry is disabled when card id is null`() {
        val entry = chatChromeMemoryEntry(cardId = null, language = AppLanguage.English)
        assertFalse(entry.isEnabled)
        assertNull(entry.cardId)
        assertEquals("memory-panel", entry.destinationRoute)
    }

    @Test
    fun `memory entry is enabled when card id is present and routes scoped to card`() {
        val entry = chatChromeMemoryEntry(cardId = "card-123", language = AppLanguage.English)
        assertTrue(entry.isEnabled)
        assertEquals("card-123", entry.cardId)
        assertEquals("memory-panel/card-123", entry.destinationRoute)
    }

    @Test
    fun `memory entry label resolves in english`() {
        val entry = chatChromeMemoryEntry(cardId = "card-123", language = AppLanguage.English)
        assertEquals("Memory", entry.label)
    }

    @Test
    fun `memory entry label resolves in chinese`() {
        val entry = chatChromeMemoryEntry(cardId = "card-123", language = AppLanguage.Chinese)
        assertEquals("记忆", entry.label)
    }

    @Test
    fun `memory entry label updates when language changes without switching card`() {
        val cardId = "card-123"
        val english = chatChromeMemoryEntry(cardId, AppLanguage.English)
        val chinese = chatChromeMemoryEntry(cardId, AppLanguage.Chinese)
        assertNotEquals(english.label, chinese.label)
        assertEquals(english.cardId, chinese.cardId)
        assertEquals(english.isEnabled, chinese.isEnabled)
    }

    private fun samplePreset(): Preset = Preset(
        id = "preset-concise",
        displayName = LocalizedText("Concise", "简洁"),
        description = LocalizedText("Short replies.", "简短回复。"),
        isBuiltIn = false,
        isActive = true,
    )
}
