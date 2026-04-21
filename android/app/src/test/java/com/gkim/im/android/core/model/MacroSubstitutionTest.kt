package com.gkim.im.android.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MacroSubstitutionTest {

    @Test
    fun substitutesDoubleBraceUserForm() {
        val result = MacroSubstitution.substituteMacros(
            template = "Hello {{user}}, how are you?",
            userDisplayName = "Aria",
            charDisplayName = "Oracle",
        )
        assertEquals("Hello Aria, how are you?", result)
    }

    @Test
    fun substitutesSingleBraceUserForm() {
        val result = MacroSubstitution.substituteMacros(
            template = "Hello {user}.",
            userDisplayName = "Aria",
            charDisplayName = "Oracle",
        )
        assertEquals("Hello Aria.", result)
    }

    @Test
    fun substitutesAngleBracketUserForm() {
        val result = MacroSubstitution.substituteMacros(
            template = "<user> arrives.",
            userDisplayName = "Aria",
            charDisplayName = "Oracle",
        )
        assertEquals("Aria arrives.", result)
    }

    @Test
    fun substitutesAllThreeCharForms() {
        val result = MacroSubstitution.substituteMacros(
            template = "{{char}} meets {char} meets <char>.",
            userDisplayName = "Aria",
            charDisplayName = "Oracle",
        )
        assertEquals("Oracle meets Oracle meets Oracle.", result)
    }

    @Test
    fun substitutesMixedUserAndCharFormsInOneTemplate() {
        val result = MacroSubstitution.substituteMacros(
            template = "{{user}}: hi\n{{char}}: welcome, <user>.",
            userDisplayName = "Aria",
            charDisplayName = "Oracle",
        )
        assertEquals("Aria: hi\nOracle: welcome, Aria.", result)
    }

    @Test
    fun ignoresCaseOnAllSixForms() {
        val result = MacroSubstitution.substituteMacros(
            template = "{{USER}} {{User}} {USER} <User> {{Char}} <CHAR>",
            userDisplayName = "Aria",
            charDisplayName = "Oracle",
        )
        assertEquals("Aria Aria Aria Aria Oracle Oracle", result)
    }

    @Test
    fun leavesUnknownMacrosUntouched() {
        val result = MacroSubstitution.substituteMacros(
            template = "{{random}} and {foo} and <bar> and {{ user }}",
            userDisplayName = "Aria",
            charDisplayName = "Oracle",
        )
        assertEquals("{{random}} and {foo} and <bar> and {{ user }}", result)
    }

    @Test
    fun emptyUserNameLeavesUserMacrosRaw() {
        val result = MacroSubstitution.substituteMacros(
            template = "Hello {{user}}, I am {{char}}.",
            userDisplayName = "",
            charDisplayName = "Oracle",
        )
        assertEquals("Hello {{user}}, I am Oracle.", result)
    }

    @Test
    fun emptyCharNameLeavesCharMacrosRaw() {
        val result = MacroSubstitution.substituteMacros(
            template = "{{user}} greets {{char}}.",
            userDisplayName = "Aria",
            charDisplayName = "",
        )
        assertEquals("Aria greets {{char}}.", result)
    }

    @Test
    fun bothEmptyLeavesTemplateUnchanged() {
        val template = "{{user}} {{char}} {user} {char} <user> <char>"
        val result = MacroSubstitution.substituteMacros(
            template = template,
            userDisplayName = "",
            charDisplayName = "",
        )
        assertEquals(template, result)
    }

    @Test
    fun templateWithNoMacrosIsUnchanged() {
        val template = "A quiet sentence with no macros at all."
        val result = MacroSubstitution.substituteMacros(template, "Aria", "Oracle")
        assertEquals(template, result)
    }

    @Test
    fun repeatedFormsAllSubstitute() {
        val result = MacroSubstitution.substituteMacros(
            template = "{{user}} {{user}} {{user}}",
            userDisplayName = "Aria",
            charDisplayName = "Oracle",
        )
        assertEquals("Aria Aria Aria", result)
    }

    @Test
    fun userFormsAndCharFormsExposeTheExactSixForms() {
        assertEquals(listOf("{{user}}", "{user}", "<user>"), MacroSubstitution.UserForms)
        assertEquals(listOf("{{char}}", "{char}", "<char>"), MacroSubstitution.CharForms)
    }
}
