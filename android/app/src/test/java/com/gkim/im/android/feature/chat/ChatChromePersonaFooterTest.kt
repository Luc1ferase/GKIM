package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.UserPersona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatChromePersonaFooterTest {

    @Test
    fun `english footer renders the active persona display name`() {
        val footer = chatChromePersonaFooter(samplePersona(), AppLanguage.English)
        assertEquals("Talking as Nova", footer?.text)
    }

    @Test
    fun `chinese footer renders the active persona display name`() {
        val footer = chatChromePersonaFooter(samplePersona(), AppLanguage.Chinese)
        assertEquals("以 新星 的身份对话", footer?.text)
    }

    @Test
    fun `content description mirrors the visible text for screen readers`() {
        val englishFooter = chatChromePersonaFooter(samplePersona(), AppLanguage.English)
        val chineseFooter = chatChromePersonaFooter(samplePersona(), AppLanguage.Chinese)
        assertEquals(englishFooter?.text, englishFooter?.contentDescription)
        assertEquals(chineseFooter?.text, chineseFooter?.contentDescription)
    }

    @Test
    fun `null active persona returns null footer so chrome omits the line`() {
        assertNull(chatChromePersonaFooter(null, AppLanguage.English))
        assertNull(chatChromePersonaFooter(null, AppLanguage.Chinese))
    }

    @Test
    fun `footer updates when the active persona changes without switching language`() {
        val nova = chatChromePersonaFooter(samplePersona(), AppLanguage.English)
        val auric = chatChromePersonaFooter(
            samplePersona().copy(
                id = "persona-auric",
                displayName = LocalizedText("Auric", "金辉"),
            ),
            AppLanguage.English,
        )
        assertNotEquals(nova?.text, auric?.text)
        assertEquals("Talking as Auric", auric?.text)
        assertNotEquals(nova?.activePersonaId, auric?.activePersonaId)
    }

    @Test
    fun `footer updates when language changes without switching persona`() {
        val persona = samplePersona()
        val english = chatChromePersonaFooter(persona, AppLanguage.English)
        val chinese = chatChromePersonaFooter(persona, AppLanguage.Chinese)
        assertNotEquals(english?.text, chinese?.text)
        assertEquals(english?.activePersonaId, chinese?.activePersonaId)
    }

    @Test
    fun `activePersonaId propagates from the active persona`() {
        val footer = chatChromePersonaFooter(samplePersona(), AppLanguage.English)
        assertEquals("persona-nova", footer?.activePersonaId)
    }

    @Test
    fun `english prefix and chinese prefix + suffix match the spec labels`() {
        assertEquals("Talking as ", ChatChromePersonaFooterDefaults.EnglishPrefix)
        assertEquals("以 ", ChatChromePersonaFooterDefaults.ChinesePrefix)
        assertEquals(" 的身份对话", ChatChromePersonaFooterDefaults.ChineseSuffix)
    }

    private fun samplePersona(): UserPersona = UserPersona(
        id = "persona-nova",
        displayName = LocalizedText("Nova", "新星"),
        description = LocalizedText("A thoughtful traveller.", "一位深思熟虑的旅人。"),
        isBuiltIn = false,
        isActive = true,
    )
}
