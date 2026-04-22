package com.gkim.im.android.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class GreetingPickerMacroSubstitutionTest {

    private val userName = "Nova"
    private val charName = "Architect Oracle"

    @Test
    fun `user and char macros resolve in the preview body`() {
        val options = listOf(
            CompanionGreetingOption(
                index = 0,
                label = "Greeting",
                body = "Hello {{user}}, I am {{char}}.",
            ),
        )
        val substituted = applyPersonaMacros(options, userName, charName)
        assertEquals("Hello Nova, I am Architect Oracle.", substituted[0].body)
    }

    @Test
    fun `stored options keep raw macros after substitution`() {
        val originalBody = "Hello {{user}}, I am {{char}}."
        val options = listOf(
            CompanionGreetingOption(index = 0, label = "Greeting", body = originalBody),
        )
        applyPersonaMacros(options, userName, charName)
        assertEquals(originalBody, options[0].body)
    }

    @Test
    fun `returned list is a fresh copy distinct from the input`() {
        val options = listOf(
            CompanionGreetingOption(index = 0, label = "Greeting", body = "{{user}} enters."),
        )
        val substituted = applyPersonaMacros(options, userName, charName)
        assertNotSame(options, substituted)
        assertEquals("Nova enters.", substituted[0].body)
    }

    @Test
    fun `all six forms substitute when both names are present`() {
        val options = listOf(
            CompanionGreetingOption(
                index = 0,
                label = "Greeting",
                body = "{{user}} / {user} / <user> · {{char}} / {char} / <char>",
            ),
        )
        val substituted = applyPersonaMacros(options, userName, charName)
        assertEquals(
            "Nova / Nova / Nova · Architect Oracle / Architect Oracle / Architect Oracle",
            substituted[0].body,
        )
    }

    @Test
    fun `substitution is case-insensitive for imported ST cards`() {
        val options = listOf(
            CompanionGreetingOption(
                index = 0,
                label = "Greeting",
                body = "Welcome {{User}} to the hall of {{CHAR}}.",
            ),
        )
        val substituted = applyPersonaMacros(options, userName, charName)
        assertEquals("Welcome Nova to the hall of Architect Oracle.", substituted[0].body)
    }

    @Test
    fun `blank user name leaves user macros raw but still substitutes char`() {
        val options = listOf(
            CompanionGreetingOption(
                index = 0,
                label = "Greeting",
                body = "Hello {{user}}, I am {{char}}.",
            ),
        )
        val substituted = applyPersonaMacros(options, userDisplayName = "", charDisplayName = charName)
        assertEquals("Hello {{user}}, I am Architect Oracle.", substituted[0].body)
    }

    @Test
    fun `blank char name leaves char macros raw but still substitutes user`() {
        val options = listOf(
            CompanionGreetingOption(
                index = 0,
                label = "Greeting",
                body = "Hello {{user}}, I am {{char}}.",
            ),
        )
        val substituted = applyPersonaMacros(options, userDisplayName = userName, charDisplayName = "")
        assertEquals("Hello Nova, I am {{char}}.", substituted[0].body)
    }

    @Test
    fun `both names blank leaves every option body untouched`() {
        val options = listOf(
            CompanionGreetingOption(
                index = 0,
                label = "Greeting",
                body = "{{user}} meets {{char}}.",
            ),
            CompanionGreetingOption(
                index = 1,
                label = "Alt 1",
                body = "{{char}} welcomes {{user}}.",
            ),
        )
        val substituted = applyPersonaMacros(options, userDisplayName = "", charDisplayName = "")
        assertEquals("{{user}} meets {{char}}.", substituted[0].body)
        assertEquals("{{char}} welcomes {{user}}.", substituted[1].body)
    }

    @Test
    fun `index and label are preserved through substitution`() {
        val options = listOf(
            CompanionGreetingOption(index = 0, label = "Greeting", body = "{{user}} arrives."),
            CompanionGreetingOption(index = 1, label = "Alt 1", body = "{{char}} waves."),
            CompanionGreetingOption(index = 2, label = "Alt 2", body = "Nothing to substitute."),
        )
        val substituted = applyPersonaMacros(options, userName, charName)
        assertEquals(0, substituted[0].index)
        assertEquals("Greeting", substituted[0].label)
        assertEquals(1, substituted[1].index)
        assertEquals("Alt 1", substituted[1].label)
        assertEquals(2, substituted[2].index)
        assertEquals("Alt 2", substituted[2].label)
        assertEquals("Nothing to substitute.", substituted[2].body)
    }

    @Test
    fun `empty options list returns an empty list`() {
        val substituted = applyPersonaMacros(emptyList(), userName, charName)
        assertEquals(emptyList<CompanionGreetingOption>(), substituted)
    }
}
