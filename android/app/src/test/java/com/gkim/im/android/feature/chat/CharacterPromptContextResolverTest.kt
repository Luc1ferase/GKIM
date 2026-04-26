package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.UserPersona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for `resolveCharacterPromptContext` (§3.1). Cover the five
 * branches the spec scenarios require:
 *  - card == null → null
 *  - full card + active persona → all six fields populated, macros un-substituted
 *  - full card + null persona → localized default name ("User" / "用户")
 *  - language switch picks the matching `LocalizedText` branch
 *  - blank persona display name falls through to the localized default
 */
class CharacterPromptContextResolverTest {

    private val englishLeaningCard = CompanionCharacterCard(
        id = "daylight-listener",
        displayName = LocalizedText("Daylight Listener", "筑谕师"),
        roleLabel = LocalizedText("Calm Companion", "冷静同伴"),
        summary = LocalizedText("EN summary", "中文摘要"),
        firstMes = LocalizedText("Hello, {{user}}.", "你好，{{user}}。"),
        systemPrompt = LocalizedText(
            english = "You are {{char}}. Listen to {{user}}.",
            chinese = "你是 {{char}}。请倾听 {{user}}。",
        ),
        personality = LocalizedText("Measured EN", "沉稳 中文"),
        scenario = LocalizedText("Tavern EN", "酒馆 中文"),
        exampleDialogue = LocalizedText(
            english = "{{user}}: hi\n{{char}}: hello",
            chinese = "{{user}}：你好\n{{char}}：你好",
        ),
        avatarText = "DL",
        accent = AccentTone.Primary,
        source = CompanionCharacterSource.Preset,
    )

    private val ariaPersona = UserPersona(
        id = "persona-aria",
        displayName = LocalizedText("Aria", "雅"),
        description = LocalizedText("desc EN", "描述"),
        isActive = true,
    )

    @Test
    fun `null card produces null context`() {
        val ctx = resolveCharacterPromptContext(
            card = null,
            persona = ariaPersona,
            language = AppLanguage.English,
        )
        assertNull(ctx)
    }

    @Test
    fun `full card and active persona produce all six fields with macros un-substituted`() {
        val ctx = resolveCharacterPromptContext(
            card = englishLeaningCard,
            persona = ariaPersona,
            language = AppLanguage.English,
        ) ?: throw AssertionError("expected non-null ctx for full card + persona")

        assertEquals("You are {{char}}. Listen to {{user}}.", ctx.systemPrompt)
        assertEquals("Measured EN", ctx.personality)
        assertEquals("Tavern EN", ctx.scenario)
        assertEquals("{{user}}: hi\n{{char}}: hello", ctx.exampleDialogue)
        assertEquals("Aria", ctx.userPersonaName)
        assertEquals("Daylight Listener", ctx.companionDisplayName)
        // Sanity: macros remain literal — substitution is the backend's job.
        assertTrue("systemPrompt must keep {{user}} literal", ctx.systemPrompt.contains("{{user}}"))
        assertTrue("systemPrompt must keep {{char}} literal", ctx.systemPrompt.contains("{{char}}"))
    }

    @Test
    fun `null persona falls back to localized default name (English)`() {
        val ctx = resolveCharacterPromptContext(
            card = englishLeaningCard,
            persona = null,
            language = AppLanguage.English,
        ) ?: throw AssertionError("expected non-null ctx for full card + null persona")

        assertEquals("User", ctx.userPersonaName)
        // Other fields still populated from the card.
        assertEquals("Daylight Listener", ctx.companionDisplayName)
        assertEquals("You are {{char}}. Listen to {{user}}.", ctx.systemPrompt)
    }

    @Test
    fun `null persona falls back to localized default name (Chinese)`() {
        val ctx = resolveCharacterPromptContext(
            card = englishLeaningCard,
            persona = null,
            language = AppLanguage.Chinese,
        ) ?: throw AssertionError("expected non-null ctx for full card + null persona")

        assertEquals("用户", ctx.userPersonaName)
        assertEquals("筑谕师", ctx.companionDisplayName)
        // Verify the resolver picked the Chinese branch of every LocalizedText field.
        assertEquals("你是 {{char}}。请倾听 {{user}}。", ctx.systemPrompt)
        assertEquals("沉稳 中文", ctx.personality)
        assertEquals("酒馆 中文", ctx.scenario)
        assertEquals("{{user}}：你好\n{{char}}：你好", ctx.exampleDialogue)
    }

    @Test
    fun `blank persona display name falls through to localized default`() {
        val blankNamePersona = ariaPersona.copy(
            displayName = LocalizedText(english = "  ", chinese = ""),
        )
        val ctx = resolveCharacterPromptContext(
            card = englishLeaningCard,
            persona = blankNamePersona,
            language = AppLanguage.English,
        ) ?: throw AssertionError("expected non-null ctx for full card + blank persona name")

        // Blank-after-trim resolves to default rather than passing through whitespace.
        assertEquals("User", ctx.userPersonaName)
    }
}
