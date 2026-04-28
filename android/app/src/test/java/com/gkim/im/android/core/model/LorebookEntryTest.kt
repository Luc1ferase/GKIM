package com.gkim.im.android.core.model

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LorebookEntryTest {

    private val moonFact = LorebookEntry(
        id = "entry-moons",
        lorebookId = "lorebook-atlas",
        name = LocalizedText("Two moons", "双月"),
        keysByLang = mapOf(
            AppLanguage.English to listOf("moon", "moons"),
            AppLanguage.Chinese to listOf("月亮", "双月"),
        ),
        secondaryKeysByLang = mapOf(
            AppLanguage.English to listOf("night"),
            AppLanguage.Chinese to listOf("夜晚"),
        ),
        secondaryGate = SecondaryGate.And,
        content = LocalizedText("The world has two moons.", "这个世界有两个月亮。"),
        scanDepth = 5,
        insertionOrder = 10,
        comment = "Fired when moons come up at night",
        extensions = buildJsonObject { put("st_probability", 100) },
    )

    @Test
    fun dataClassEqualityConsidersEveryField() {
        val twin = moonFact.copy()
        assertEquals(moonFact, twin)

        val renamed = moonFact.copy(name = LocalizedText("Twin Moons", "双月"))
        assertNotEquals(moonFact, renamed)

        val reordered = moonFact.copy(insertionOrder = 20)
        assertNotEquals(moonFact, reordered)

        val regated = moonFact.copy(secondaryGate = SecondaryGate.Or)
        assertNotEquals(moonFact, regated)
    }

    @Test
    fun defaultsMatchDesignSpec() {
        val minimal = LorebookEntry(
            id = "entry-minimal",
            lorebookId = "lorebook-atlas",
            name = LocalizedText("Minimal", "精简"),
        )
        assertEquals(SecondaryGate.None, minimal.secondaryGate)
        assertEquals(LocalizedText.Empty, minimal.content)
        assertTrue(minimal.enabled)
        assertFalse(minimal.constant)
        assertFalse(minimal.caseSensitive)
        assertEquals(3, minimal.scanDepth)
        assertEquals(3, LorebookEntry.DefaultScanDepth)
        assertEquals(20, LorebookEntry.MaxServerScanDepth)
        assertEquals(0, minimal.insertionOrder)
        assertEquals("", minimal.comment)
        assertTrue(minimal.keysByLang.isEmpty())
        assertTrue(minimal.secondaryKeysByLang.isEmpty())
    }

    @Test
    fun primaryKeysForLanguageReturnsPerLanguageList() {
        assertEquals(listOf("moon", "moons"), moonFact.primaryKeysFor(AppLanguage.English))
        assertEquals(listOf("月亮", "双月"), moonFact.primaryKeysFor(AppLanguage.Chinese))
    }

    @Test
    fun primaryKeysForLanguageReturnsEmptyWhenLanguageAbsent() {
        val englishOnly = moonFact.copy(
            keysByLang = mapOf(AppLanguage.English to listOf("moon")),
        )
        assertEquals(listOf("moon"), englishOnly.primaryKeysFor(AppLanguage.English))
        assertTrue(englishOnly.primaryKeysFor(AppLanguage.Chinese).isEmpty())
    }

    @Test
    fun secondaryKeysForLanguageReturnsPerLanguageList() {
        assertEquals(listOf("night"), moonFact.secondaryKeysFor(AppLanguage.English))
        assertEquals(listOf("夜晚"), moonFact.secondaryKeysFor(AppLanguage.Chinese))
    }

    @Test
    fun canMatchInLanguageWhenConstantEvenWithoutKeys() {
        val constantEntry = moonFact.copy(
            constant = true,
            keysByLang = emptyMap(),
        )
        assertTrue(constantEntry.canMatchInLanguage(AppLanguage.English))
        assertTrue(constantEntry.canMatchInLanguage(AppLanguage.Chinese))
    }

    @Test
    fun canMatchInLanguageFalseWhenNoKeysAndNotConstant() {
        val englishOnly = moonFact.copy(
            keysByLang = mapOf(AppLanguage.English to listOf("moon")),
        )
        assertTrue(englishOnly.canMatchInLanguage(AppLanguage.English))
        assertFalse(englishOnly.canMatchInLanguage(AppLanguage.Chinese))
    }

    @Test
    fun canMatchInLanguageFalseWhenAllKeysBlank() {
        val blankOnly = moonFact.copy(
            constant = false,
            keysByLang = mapOf(AppLanguage.English to listOf("", "   ")),
        )
        assertFalse(blankOnly.canMatchInLanguage(AppLanguage.English))
    }

    @Test
    fun extensionsPassthroughSurvivesCopy() {
        val renamed = moonFact.copy(name = LocalizedText("Lunar Fact", "月亮事实"))
        assertEquals(moonFact.extensions, renamed.extensions)
        assertEquals(JsonPrimitive(100), renamed.extensions["st_probability"])
    }

    @Test
    fun secondaryGateVariantsAreAllRepresented() {
        val values = SecondaryGate.values().toSet()
        assertEquals(setOf(SecondaryGate.None, SecondaryGate.And, SecondaryGate.Or), values)
    }
}
