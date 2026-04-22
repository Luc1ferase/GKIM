package com.gkim.im.android.feature.settings.worldinfo

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Lorebook
import com.gkim.im.android.core.model.LorebookEntry
import com.gkim.im.android.core.model.SecondaryGate
import com.gkim.im.android.data.repository.DefaultWorldInfoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorldInfoEntryEditorPresentationTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `draft seeds from repository once lorebook and entry are present`() = runTest(dispatcher) {
        val entry = sampleEntry(
            englishName = "Guild",
            chineseName = "公会",
            englishContent = "The mercenary guild headquarters.",
            chineseContent = "佣兵公会总部。",
            englishKeys = listOf("guild"),
            chineseKeys = listOf("公会"),
            englishSecondaryKeys = listOf("hall"),
            secondaryGate = SecondaryGate.And,
            enabled = false,
            constant = true,
            caseSensitive = true,
            scanDepth = 7,
            insertionOrder = 3,
            comment = "authored for demo",
        )
        val repo = buildRepo(listOf(entry))
        val viewModel = WorldInfoEntryEditorViewModel(repo, LOREBOOK_ID, ENTRY_ID)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.loaded)
        assertEquals("Guild", state.englishName)
        assertEquals("公会", state.chineseName)
        assertEquals(listOf("guild"), state.englishKeys)
        assertEquals(listOf("公会"), state.chineseKeys)
        assertEquals(listOf("hall"), state.englishSecondaryKeys)
        assertTrue(state.chineseSecondaryKeys.isEmpty())
        assertEquals(SecondaryGate.And, state.secondaryGate)
        assertEquals("The mercenary guild headquarters.", state.englishContent)
        assertEquals("佣兵公会总部。", state.chineseContent)
        assertFalse(state.enabled)
        assertTrue(state.constant)
        assertTrue(state.caseSensitive)
        assertEquals(7, state.scanDepth)
        assertEquals(3, state.insertionOrder)
        assertEquals("authored for demo", state.comment)
    }

    @Test
    fun `setEnglishName and setChineseName update the draft independently`() = runTest(dispatcher) {
        val viewModel = buildViewModel(listOf(sampleEntry()))
        advanceUntilIdle()

        viewModel.setEnglishName("Tavern")
        viewModel.setChineseName("酒馆")

        val state = viewModel.uiState.value
        assertEquals("Tavern", state.englishName)
        assertEquals("酒馆", state.chineseName)
    }

    @Test
    fun `addKey appends to the language it is keyed on and leaves the other language alone`() = runTest(dispatcher) {
        val viewModel = buildViewModel(listOf(sampleEntry()))
        advanceUntilIdle()

        viewModel.addKey(AppLanguage.English, "mercenary")
        viewModel.addKey(AppLanguage.English, "  guild  ")
        viewModel.addKey(AppLanguage.Chinese, "公会")

        val state = viewModel.uiState.value
        assertEquals(listOf("mercenary", "guild"), state.englishKeys)
        assertEquals(listOf("公会"), state.chineseKeys)
    }

    @Test
    fun `addKey ignores blank input so whitespace does not pollute the list`() = runTest(dispatcher) {
        val viewModel = buildViewModel(listOf(sampleEntry()))
        advanceUntilIdle()

        viewModel.addKey(AppLanguage.English, "   ")
        viewModel.addKey(AppLanguage.Chinese, "")

        val state = viewModel.uiState.value
        assertTrue(state.englishKeys.isEmpty())
        assertTrue(state.chineseKeys.isEmpty())
    }

    @Test
    fun `removeKey removes by index on the targeted language only`() = runTest(dispatcher) {
        val entry = sampleEntry(
            englishKeys = listOf("alpha", "beta", "gamma"),
            chineseKeys = listOf("甲", "乙"),
        )
        val viewModel = buildViewModel(listOf(entry))
        advanceUntilIdle()

        viewModel.removeKey(AppLanguage.English, 1)

        val state = viewModel.uiState.value
        assertEquals(listOf("alpha", "gamma"), state.englishKeys)
        assertEquals(listOf("甲", "乙"), state.chineseKeys)
    }

    @Test
    fun `removeKey with an out-of-range index is a safe no-op`() = runTest(dispatcher) {
        val entry = sampleEntry(englishKeys = listOf("alpha"))
        val viewModel = buildViewModel(listOf(entry))
        advanceUntilIdle()

        viewModel.removeKey(AppLanguage.English, 5)

        assertEquals(listOf("alpha"), viewModel.uiState.value.englishKeys)
    }

    @Test
    fun `addSecondaryKey and removeSecondaryKey operate on the secondary list only`() = runTest(dispatcher) {
        val viewModel = buildViewModel(listOf(sampleEntry(englishKeys = listOf("alpha"))))
        advanceUntilIdle()

        viewModel.addSecondaryKey(AppLanguage.English, "beta")
        viewModel.addSecondaryKey(AppLanguage.Chinese, "乙")
        viewModel.removeSecondaryKey(AppLanguage.English, 0)

        val state = viewModel.uiState.value
        assertEquals(listOf("alpha"), state.englishKeys)
        assertTrue(state.englishSecondaryKeys.isEmpty())
        assertEquals(listOf("乙"), state.chineseSecondaryKeys)
    }

    @Test
    fun `setSecondaryGate covers None And Or`() = runTest(dispatcher) {
        val viewModel = buildViewModel(listOf(sampleEntry()))
        advanceUntilIdle()

        viewModel.setSecondaryGate(SecondaryGate.And)
        assertEquals(SecondaryGate.And, viewModel.uiState.value.secondaryGate)
        viewModel.setSecondaryGate(SecondaryGate.Or)
        assertEquals(SecondaryGate.Or, viewModel.uiState.value.secondaryGate)
        viewModel.setSecondaryGate(SecondaryGate.None)
        assertEquals(SecondaryGate.None, viewModel.uiState.value.secondaryGate)
    }

    @Test
    fun `setEnglishContent and setChineseContent update the draft content fields`() = runTest(dispatcher) {
        val viewModel = buildViewModel(listOf(sampleEntry()))
        advanceUntilIdle()

        viewModel.setEnglishContent("Heroes gather here.")
        viewModel.setChineseContent("英雄齐聚于此。")

        val state = viewModel.uiState.value
        assertEquals("Heroes gather here.", state.englishContent)
        assertEquals("英雄齐聚于此。", state.chineseContent)
    }

    @Test
    fun `setEnabled setConstant setCaseSensitive flip independently`() = runTest(dispatcher) {
        val viewModel = buildViewModel(listOf(sampleEntry()))
        advanceUntilIdle()

        viewModel.setEnabled(false)
        viewModel.setConstant(true)
        viewModel.setCaseSensitive(true)

        val state = viewModel.uiState.value
        assertFalse(state.enabled)
        assertTrue(state.constant)
        assertTrue(state.caseSensitive)
    }

    @Test
    fun `setScanDepth clamps into 0 through MaxServerScanDepth`() = runTest(dispatcher) {
        val viewModel = buildViewModel(listOf(sampleEntry()))
        advanceUntilIdle()

        viewModel.setScanDepth(-5)
        assertEquals(0, viewModel.uiState.value.scanDepth)

        viewModel.setScanDepth(LorebookEntry.MaxServerScanDepth + 10)
        assertEquals(LorebookEntry.MaxServerScanDepth, viewModel.uiState.value.scanDepth)

        viewModel.setScanDepth(5)
        assertEquals(5, viewModel.uiState.value.scanDepth)
    }

    @Test
    fun `setInsertionOrder and setComment update their fields`() = runTest(dispatcher) {
        val viewModel = buildViewModel(listOf(sampleEntry()))
        advanceUntilIdle()

        viewModel.setInsertionOrder(17)
        viewModel.setComment("authoring note")

        val state = viewModel.uiState.value
        assertEquals(17, state.insertionOrder)
        assertEquals("authoring note", state.comment)
    }

    @Test
    fun `save persists every field back to the repository and increments saveCompleted`() = runTest(dispatcher) {
        val repo = buildRepo(listOf(sampleEntry()))
        val viewModel = WorldInfoEntryEditorViewModel(repo, LOREBOOK_ID, ENTRY_ID)
        advanceUntilIdle()

        viewModel.setEnglishName("Guild")
        viewModel.setChineseName("公会")
        viewModel.addKey(AppLanguage.English, "mercenary")
        viewModel.addKey(AppLanguage.Chinese, "佣兵")
        viewModel.addSecondaryKey(AppLanguage.English, "hall")
        viewModel.setSecondaryGate(SecondaryGate.Or)
        viewModel.setEnglishContent("Mercenary guild HQ.")
        viewModel.setChineseContent("佣兵公会总部。")
        viewModel.setEnabled(false)
        viewModel.setConstant(true)
        viewModel.setCaseSensitive(true)
        viewModel.setScanDepth(9)
        viewModel.setInsertionOrder(4)
        viewModel.setComment("author")
        val initialCompletion = viewModel.uiState.value.saveCompleted

        viewModel.save()
        advanceUntilIdle()

        val stored = repo.entries.value[LOREBOOK_ID]!!.single { it.id == ENTRY_ID }
        assertEquals("Guild", stored.name.english)
        assertEquals("公会", stored.name.chinese)
        assertEquals(listOf("mercenary"), stored.keysByLang[AppLanguage.English])
        assertEquals(listOf("佣兵"), stored.keysByLang[AppLanguage.Chinese])
        assertEquals(listOf("hall"), stored.secondaryKeysByLang[AppLanguage.English])
        assertEquals(SecondaryGate.Or, stored.secondaryGate)
        assertEquals("Mercenary guild HQ.", stored.content.english)
        assertEquals("佣兵公会总部。", stored.content.chinese)
        assertFalse(stored.enabled)
        assertTrue(stored.constant)
        assertTrue(stored.caseSensitive)
        assertEquals(9, stored.scanDepth)
        assertEquals(4, stored.insertionOrder)
        assertEquals("author", stored.comment)
        assertNotEquals(initialCompletion, viewModel.uiState.value.saveCompleted)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `save strips empty-language lists so the entry only carries populated languages`() = runTest(dispatcher) {
        val repo = buildRepo(listOf(sampleEntry()))
        val viewModel = WorldInfoEntryEditorViewModel(repo, LOREBOOK_ID, ENTRY_ID)
        advanceUntilIdle()

        viewModel.addKey(AppLanguage.English, "alpha")
        viewModel.save()
        advanceUntilIdle()

        val stored = repo.entries.value[LOREBOOK_ID]!!.single()
        assertTrue(stored.keysByLang.containsKey(AppLanguage.English))
        assertFalse(stored.keysByLang.containsKey(AppLanguage.Chinese))
    }

    @Test
    fun `save surfaces an error when the entry is missing`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(sampleLorebook()),
            initialEntries = mapOf(LOREBOOK_ID to emptyList()),
        )
        val viewModel = WorldInfoEntryEditorViewModel(repo, LOREBOOK_ID, ENTRY_ID)
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertEquals("Entry not loaded yet", viewModel.uiState.value.errorMessage)
        assertEquals(0L, viewModel.uiState.value.saveCompleted)
    }

    @Test
    fun `clearError resets the error banner without discarding draft edits`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(sampleLorebook()),
            initialEntries = mapOf(LOREBOOK_ID to emptyList()),
        )
        val viewModel = WorldInfoEntryEditorViewModel(repo, LOREBOOK_ID, ENTRY_ID)
        viewModel.setEnglishName("Pending")
        viewModel.save()
        advanceUntilIdle()
        assertEquals("Entry not loaded yet", viewModel.uiState.value.errorMessage)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("Pending", viewModel.uiState.value.englishName)
    }

    @Test
    fun `upstream updates after the initial load do not clobber in-progress draft edits`() = runTest(dispatcher) {
        val repo = buildRepo(listOf(sampleEntry(englishName = "Original")))
        val viewModel = WorldInfoEntryEditorViewModel(repo, LOREBOOK_ID, ENTRY_ID)
        advanceUntilIdle()
        assertEquals("Original", viewModel.uiState.value.englishName)

        viewModel.setEnglishName("Edited")
        // Simulate a concurrent upstream update (e.g., reconciliation from server)
        repo.updateEntry(
            sampleEntry(
                englishName = "Remote",
                chineseName = "远端",
            ),
        )
        advanceUntilIdle()

        assertEquals("Edited", viewModel.uiState.value.englishName)
    }

    companion object {
        private const val LOREBOOK_ID = "lb-1"
        private const val ENTRY_ID = "entry-1"
    }

    private fun buildViewModel(entries: List<LorebookEntry>): WorldInfoEntryEditorViewModel {
        val repo = buildRepo(entries)
        return WorldInfoEntryEditorViewModel(repo, LOREBOOK_ID, ENTRY_ID)
    }

    private fun buildRepo(entries: List<LorebookEntry>): DefaultWorldInfoRepository {
        return DefaultWorldInfoRepository(
            initialLorebooks = listOf(sampleLorebook()),
            initialEntries = mapOf(LOREBOOK_ID to entries),
        )
    }

    private fun sampleLorebook(): Lorebook = Lorebook(
        id = LOREBOOK_ID,
        ownerId = "owner",
        displayName = LocalizedText(english = "Lorebook", chinese = "世界书"),
        description = LocalizedText.Empty,
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )

    private fun sampleEntry(
        englishName: String = "Entry",
        chineseName: String = "条目",
        englishKeys: List<String> = emptyList(),
        chineseKeys: List<String> = emptyList(),
        englishSecondaryKeys: List<String> = emptyList(),
        chineseSecondaryKeys: List<String> = emptyList(),
        secondaryGate: SecondaryGate = SecondaryGate.None,
        englishContent: String = "",
        chineseContent: String = "",
        enabled: Boolean = true,
        constant: Boolean = false,
        caseSensitive: Boolean = false,
        scanDepth: Int = LorebookEntry.DefaultScanDepth,
        insertionOrder: Int = 0,
        comment: String = "",
    ): LorebookEntry {
        val keys = buildMap<AppLanguage, List<String>> {
            if (englishKeys.isNotEmpty()) put(AppLanguage.English, englishKeys)
            if (chineseKeys.isNotEmpty()) put(AppLanguage.Chinese, chineseKeys)
        }
        val secondary = buildMap<AppLanguage, List<String>> {
            if (englishSecondaryKeys.isNotEmpty()) put(AppLanguage.English, englishSecondaryKeys)
            if (chineseSecondaryKeys.isNotEmpty()) put(AppLanguage.Chinese, chineseSecondaryKeys)
        }
        return LorebookEntry(
            id = ENTRY_ID,
            lorebookId = LOREBOOK_ID,
            name = LocalizedText(english = englishName, chinese = chineseName),
            keysByLang = keys,
            secondaryKeysByLang = secondary,
            secondaryGate = secondaryGate,
            content = LocalizedText(english = englishContent, chinese = chineseContent),
            enabled = enabled,
            constant = constant,
            caseSensitive = caseSensitive,
            scanDepth = scanDepth,
            insertionOrder = insertionOrder,
            comment = comment,
        )
    }
}
