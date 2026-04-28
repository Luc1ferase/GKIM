package com.gkim.im.android.core.model

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LorebookModelTest {

    private val atlas = Lorebook(
        id = "lorebook-atlas",
        ownerId = "user-alpha",
        displayName = LocalizedText("Atlas", "图集"),
        description = LocalizedText("World facts.", "世界设定。"),
        isGlobal = false,
        isBuiltIn = false,
        tokenBudget = 2048,
        extensions = buildJsonObject { put("st", "carried-over") },
        createdAt = 1_000L,
        updatedAt = 2_000L,
    )

    @Test
    fun dataClassEqualityConsidersEveryField() {
        val twin = atlas.copy()
        assertEquals(atlas, twin)

        val retimed = atlas.copy(updatedAt = 3_000L)
        assertNotEquals(atlas, retimed)

        val renamed = atlas.copy(displayName = LocalizedText("Codex", "典籍"))
        assertNotEquals(atlas, renamed)

        val rebudget = atlas.copy(tokenBudget = 4096)
        assertNotEquals(atlas, rebudget)
    }

    @Test
    fun defaultTokenBudgetMatchesDesignSpec() {
        assertEquals(1024, Lorebook.DefaultTokenBudget)
        val minimal = Lorebook(
            id = "lorebook-minimal",
            ownerId = "user-alpha",
            displayName = LocalizedText("Mini", "精简"),
        )
        assertEquals(1024, minimal.tokenBudget)
        assertEquals(LocalizedText.Empty, minimal.description)
        assertFalse(minimal.isGlobal)
        assertFalse(minimal.isBuiltIn)
        assertEquals(0L, minimal.createdAt)
        assertEquals(0L, minimal.updatedAt)
    }

    @Test
    fun resolveReturnsActiveLanguageStrings() {
        val en = atlas.resolve(AppLanguage.English)
        assertEquals("Atlas", en.displayName)
        assertEquals("World facts.", en.description)
        assertEquals(2048, en.tokenBudget)

        val zh = atlas.resolve(AppLanguage.Chinese)
        assertEquals("图集", zh.displayName)
        assertEquals("世界设定。", zh.description)
        assertFalse(zh.isGlobal)
        assertFalse(zh.isBuiltIn)
    }

    @Test
    fun isDeletableRequiresUserOwned() {
        assertTrue("user-owned lorebooks are deletable", atlas.isDeletable)
        assertFalse(
            "built-in lorebooks cannot be deleted",
            atlas.copy(isBuiltIn = true).isDeletable,
        )
    }

    @Test
    fun extensionsPassthroughSurvivesCopy() {
        val original = atlas.extensions
        val renamed = atlas.copy(displayName = LocalizedText("Codex", "典籍"))
        assertEquals(original, renamed.extensions)
        assertEquals(JsonPrimitive("carried-over"), renamed.extensions["st"])
    }

    @Test
    fun globalFlagTogglesIndependentlyOfOtherFields() {
        val globalCopy = atlas.copy(isGlobal = true)
        assertTrue(globalCopy.isGlobal)
        assertEquals(atlas.tokenBudget, globalCopy.tokenBudget)
        assertEquals(atlas.displayName, globalCopy.displayName)
        assertNotEquals(atlas, globalCopy)
    }
}
