package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.UserPersona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatChromePersonaPillTest {

    @Test
    fun `active persona resolves label in active english language`() {
        val persona = samplePersona()
        val pill = chatChromePersonaPill(persona, AppLanguage.English)
        assertEquals("Nova", pill.label)
        assertEquals(persona.id, pill.activePersonaId)
    }

    @Test
    fun `active persona resolves label in active chinese language`() {
        val persona = samplePersona()
        val pill = chatChromePersonaPill(persona, AppLanguage.Chinese)
        assertEquals("新星", pill.label)
    }

    @Test
    fun `null active persona falls back to english placeholder in english`() {
        val pill = chatChromePersonaPill(activePersona = null, language = AppLanguage.English)
        assertEquals(ChatChromePersonaPillDefaults.FallbackLabelEnglish, pill.label)
        assertNull(pill.activePersonaId)
    }

    @Test
    fun `null active persona falls back to chinese placeholder in chinese`() {
        val pill = chatChromePersonaPill(activePersona = null, language = AppLanguage.Chinese)
        assertEquals(ChatChromePersonaPillDefaults.FallbackLabelChinese, pill.label)
    }

    @Test
    fun `destination route points at settings nav entry`() {
        val pill = chatChromePersonaPill(samplePersona(), AppLanguage.English)
        assertEquals("settings", pill.destinationRoute)
    }

    @Test
    fun `pill label updates when active persona changes`() {
        val novaPill = chatChromePersonaPill(samplePersona(), AppLanguage.English)
        val other = samplePersona().copy(
            id = "persona-auric",
            displayName = LocalizedText("Auric", "金辉"),
        )
        val auricPill = chatChromePersonaPill(other, AppLanguage.English)
        assertNotEquals(novaPill.label, auricPill.label)
        assertEquals("Auric", auricPill.label)
        assertNotEquals(novaPill.activePersonaId, auricPill.activePersonaId)
    }

    @Test
    fun `pill label updates when language changes without switching persona`() {
        val persona = samplePersona()
        val english = chatChromePersonaPill(persona, AppLanguage.English)
        val chinese = chatChromePersonaPill(persona, AppLanguage.Chinese)
        assertNotEquals(english.label, chinese.label)
        assertEquals(english.activePersonaId, chinese.activePersonaId)
    }

    private fun samplePersona(): UserPersona = UserPersona(
        id = "persona-nova",
        displayName = LocalizedText("Nova", "新星"),
        description = LocalizedText("A thoughtful traveller.", "一位深思熟虑的旅人。"),
        isBuiltIn = false,
        isActive = true,
    )
}
