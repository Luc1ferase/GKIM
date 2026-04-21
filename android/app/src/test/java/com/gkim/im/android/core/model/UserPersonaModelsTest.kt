package com.gkim.im.android.core.model

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPersonaModelsTest {

    private val traveller = UserPersona(
        id = "user-alpha",
        displayName = LocalizedText("Traveller", "旅人"),
        description = LocalizedText("A curious wanderer.", "好奇的漫游者。"),
        isBuiltIn = false,
        isActive = true,
        createdAt = 1_000L,
        updatedAt = 2_000L,
        extensions = buildJsonObject { put("nickname", "T") },
    )

    @Test
    fun dataClassEqualityConsidersEveryField() {
        val twin = traveller.copy()
        assertEquals(traveller, twin)

        val retimed = traveller.copy(updatedAt = 3_000L)
        assertNotEquals(traveller, retimed)

        val relabeled = traveller.copy(
            displayName = LocalizedText("Wanderer", "流浪者"),
        )
        assertNotEquals(traveller, relabeled)
    }

    @Test
    fun resolveReturnsActiveLanguageStrings() {
        val en = traveller.resolve(AppLanguage.English)
        assertEquals("Traveller", en.displayName)
        assertEquals("A curious wanderer.", en.description)

        val zh = traveller.resolve(AppLanguage.Chinese)
        assertEquals("旅人", zh.displayName)
        assertEquals("好奇的漫游者。", zh.description)
        assertTrue(zh.isActive)
        assertFalse(zh.isBuiltIn)
    }

    @Test
    fun isDeletableRequiresUserOwnedAndInactive() {
        assertFalse("active personas cannot be deleted", traveller.isDeletable)
        assertFalse(
            "built-in personas cannot be deleted even when inactive",
            traveller.copy(isBuiltIn = true, isActive = false).isDeletable,
        )
        assertTrue(
            "inactive user-owned personas are deletable",
            traveller.copy(isActive = false).isDeletable,
        )
    }

    @Test
    fun extensionsPassthroughSurvivesCopy() {
        val original = traveller.extensions
        val renamed = traveller.copy(displayName = LocalizedText("Pilgrim", "朝圣者"))
        assertEquals(original, renamed.extensions)
        assertEquals(JsonPrimitive("T"), renamed.extensions["nickname"])
    }

    @Test
    fun validationAcceptsCompleteBilingualPersona() {
        val result = UserPersonaValidation.validate(traveller)
        assertEquals(UserPersonaValidationResult.Valid, result)
    }

    @Test
    fun validationRejectsBlankEnglishDisplayName() {
        val broken = traveller.copy(displayName = LocalizedText("", "旅人"))
        val result = UserPersonaValidation.validate(broken) as UserPersonaValidationResult.Invalid
        assertEquals(listOf(UserPersonaValidationError.DisplayNameEnglishBlank), result.errors)
    }

    @Test
    fun validationRejectsBlankChineseDescription() {
        val broken = traveller.copy(description = LocalizedText("A curious wanderer.", "   "))
        val result = UserPersonaValidation.validate(broken) as UserPersonaValidationResult.Invalid
        assertEquals(listOf(UserPersonaValidationError.DescriptionChineseBlank), result.errors)
    }

    @Test
    fun validationReportsEveryBlankSideAtOnce() {
        val broken = traveller.copy(
            displayName = LocalizedText("", ""),
            description = LocalizedText("", ""),
        )
        val result = UserPersonaValidation.validate(broken) as UserPersonaValidationResult.Invalid
        assertEquals(
            listOf(
                UserPersonaValidationError.DisplayNameEnglishBlank,
                UserPersonaValidationError.DisplayNameChineseBlank,
                UserPersonaValidationError.DescriptionEnglishBlank,
                UserPersonaValidationError.DescriptionChineseBlank,
            ),
            result.errors,
        )
    }
}
