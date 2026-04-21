package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.LocalizedText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionGreetingPickerTest {
    @Test
    fun `resolve returns empty list when card is null`() {
        val options = resolveCompanionGreetings(card = null, language = AppLanguage.English)
        assertTrue(options.isEmpty())
    }

    @Test
    fun `resolve emits firstMes then alternate greetings in order`() {
        val card = card(
            firstMesEn = "Hello traveller.",
            alternateGreetingsEn = listOf("Welcome back.", "A new day begins."),
        )
        val options = resolveCompanionGreetings(card, AppLanguage.English)
        assertEquals(3, options.size)
        assertEquals(listOf(0, 1, 2), options.map { it.index })
        assertEquals("Hello traveller.", options[0].body)
        assertEquals("Welcome back.", options[1].body)
        assertEquals("A new day begins.", options[2].body)
        assertEquals("Greeting", options[0].label)
        assertEquals("Alt 1", options[1].label)
        assertEquals("Alt 2", options[2].label)
    }

    @Test
    fun `resolve skips blank greetings but keeps other entries contiguous`() {
        val card = card(
            firstMesEn = "Hi.",
            alternateGreetingsEn = listOf("", "Only alt two is kept."),
        )
        val options = resolveCompanionGreetings(card, AppLanguage.English)
        assertEquals(2, options.size)
        assertEquals(listOf(0, 1), options.map { it.index })
        assertEquals("Hi.", options[0].body)
        assertEquals("Only alt two is kept.", options[1].body)
    }

    @Test
    fun `resolve skips firstMes when blank`() {
        val card = card(
            firstMesEn = "",
            alternateGreetingsEn = listOf("Just an alt."),
        )
        val options = resolveCompanionGreetings(card, AppLanguage.English)
        assertEquals(1, options.size)
        assertEquals("Just an alt.", options.single().body)
        assertEquals("Alt 1", options.single().label)
    }

    @Test
    fun `resolve honours the active language`() {
        val card = card(
            firstMesEn = "Hello.",
            firstMesZh = "你好。",
            alternateGreetingsEn = listOf("Welcome."),
            alternateGreetingsZh = listOf("欢迎回来。"),
        )
        val en = resolveCompanionGreetings(card, AppLanguage.English)
        val zh = resolveCompanionGreetings(card, AppLanguage.Chinese)
        assertEquals("Hello.", en[0].body)
        assertEquals("Welcome.", en[1].body)
        assertEquals("你好。", zh[0].body)
        assertEquals("欢迎回来。", zh[1].body)
    }

    @Test
    fun `shouldShowGreetingPicker is true only when path is empty and options exist`() {
        val options = listOf(
            CompanionGreetingOption(index = 0, label = "Greeting", body = "Hi."),
        )
        assertTrue(shouldShowGreetingPicker(companionPathIsEmpty = true, options = options))
        assertFalse(shouldShowGreetingPicker(companionPathIsEmpty = false, options = options))
        assertFalse(shouldShowGreetingPicker(companionPathIsEmpty = true, options = emptyList()))
    }

    private fun card(
        firstMesEn: String,
        firstMesZh: String = firstMesEn,
        alternateGreetingsEn: List<String> = emptyList(),
        alternateGreetingsZh: List<String> = alternateGreetingsEn,
    ): CompanionCharacterCard = CompanionCharacterCard(
        id = "card-1",
        displayName = LocalizedText("Aria", "Aria"),
        roleLabel = LocalizedText("Guide", "向导"),
        summary = LocalizedText("Guide", "向导"),
        firstMes = LocalizedText(firstMesEn, firstMesZh),
        alternateGreetings = alternateGreetingsEn.zip(alternateGreetingsZh).map { (en, zh) ->
            LocalizedText(en, zh)
        },
        avatarText = "AR",
        accent = AccentTone.Primary,
        source = CompanionCharacterSource.Preset,
    )
}
