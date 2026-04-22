package com.gkim.im.android.feature.settings.worldinfo

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Lorebook
import com.gkim.im.android.core.model.LorebookBinding
import com.gkim.im.android.core.model.LorebookEntry
import com.gkim.im.android.data.repository.DefaultCompanionRosterRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorldInfoEditorPresentationTest {

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
    fun `header exposes the current lorebook fields resolved for language`() = runTest(dispatcher) {
        val lorebook = sampleLorebook(
            id = "lb-1",
            english = "Alpha",
            chinese = "阿尔法",
            descriptionEn = "Outer ring",
            descriptionZh = "外环",
            tokenBudget = 2_048,
            isGlobal = true,
        )
        val (repo, roster) = buildRepos(listOf(lorebook))
        val viewModel = WorldInfoEditorViewModel(repo, roster, "lb-1", language = { AppLanguage.English })
        advanceUntilIdle()

        val header = viewModel.uiState.value.header
        assertNotNull(header)
        assertEquals("Alpha", header!!.englishName)
        assertEquals("阿尔法", header.chineseName)
        assertEquals("Outer ring", header.englishDescription)
        assertEquals("外环", header.chineseDescription)
        assertEquals(2_048, header.tokenBudget)
        assertTrue(header.isGlobal)
        assertFalse(header.isBuiltIn)
    }

    @Test
    fun `saveHeader dispatches updateLorebook with the new fields`() = runTest(dispatcher) {
        val lorebook = sampleLorebook(id = "lb-1", english = "Alpha", chinese = "阿尔法")
        val (repo, roster) = buildRepos(listOf(lorebook))
        val viewModel = WorldInfoEditorViewModel(repo, roster, "lb-1", language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.saveHeader(
            englishName = "Alpha 2",
            chineseName = "阿尔法二",
            englishDescription = "Updated",
            chineseDescription = "更新",
            tokenBudget = 4_096,
            isGlobal = true,
        )
        advanceUntilIdle()

        val stored = repo.lorebooks.value.single()
        assertEquals("Alpha 2", stored.displayName.english)
        assertEquals("阿尔法二", stored.displayName.chinese)
        assertEquals("Updated", stored.description.english)
        assertEquals("更新", stored.description.chinese)
        assertEquals(4_096, stored.tokenBudget)
        assertTrue(stored.isGlobal)
    }

    @Test
    fun `saveHeader surfaces an error when the lorebook is built-in`() = runTest(dispatcher) {
        val lorebook = sampleLorebook(id = "lb-1", isBuiltIn = true)
        val (repo, roster) = buildRepos(listOf(lorebook))
        val viewModel = WorldInfoEditorViewModel(repo, roster, "lb-1", language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.saveHeader("X", "Y", "", "", 1_024, false)
        advanceUntilIdle()

        assertEquals("Built-in lorebook cannot be modified", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `entries are sorted by insertionOrder ascending`() = runTest(dispatcher) {
        val lorebook = sampleLorebook(id = "lb-1")
        val entries = listOf(
            sampleEntry("lb-1", id = "e-3", insertionOrder = 30),
            sampleEntry("lb-1", id = "e-1", insertionOrder = 10),
            sampleEntry("lb-1", id = "e-2", insertionOrder = 20),
        )
        val (repo, roster) = buildRepos(listOf(lorebook), mapOf("lb-1" to entries))
        val viewModel = WorldInfoEditorViewModel(repo, roster, "lb-1", language = { AppLanguage.English })
        advanceUntilIdle()

        val ids = viewModel.uiState.value.entries.map { it.entryId }
        assertEquals(listOf("e-1", "e-2", "e-3"), ids)
        assertEquals(0, viewModel.uiState.value.entries.first().position)
        assertEquals(2, viewModel.uiState.value.entries.last().position)
        assertFalse("first entry cannot move up", viewModel.uiState.value.entries.first().canMoveUp)
        assertFalse("last entry cannot move down", viewModel.uiState.value.entries.last().canMoveDown)
    }

    @Test
    fun `addEntry appends an entry after the highest insertionOrder`() = runTest(dispatcher) {
        val lorebook = sampleLorebook(id = "lb-1")
        val entries = listOf(
            sampleEntry("lb-1", id = "e-1", insertionOrder = 5),
            sampleEntry("lb-1", id = "e-2", insertionOrder = 8),
        )
        val (repo, roster) = buildRepos(listOf(lorebook), mapOf("lb-1" to entries))
        val viewModel = WorldInfoEditorViewModel(repo, roster, "lb-1", language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.addEntry()
        advanceUntilIdle()

        val stored = repo.entries.value["lb-1"].orEmpty()
        assertEquals(3, stored.size)
        val newest = stored.maxByOrNull { it.insertionOrder }!!
        assertEquals(9, newest.insertionOrder)
        assertEquals("New entry", newest.name.english)
        assertEquals("新条目", newest.name.chinese)
    }

    @Test
    fun `moveEntryUp swaps insertionOrder with the previous entry`() = runTest(dispatcher) {
        val lorebook = sampleLorebook(id = "lb-1")
        val entries = listOf(
            sampleEntry("lb-1", id = "e-1", insertionOrder = 10),
            sampleEntry("lb-1", id = "e-2", insertionOrder = 20),
            sampleEntry("lb-1", id = "e-3", insertionOrder = 30),
        )
        val (repo, roster) = buildRepos(listOf(lorebook), mapOf("lb-1" to entries))
        val viewModel = WorldInfoEditorViewModel(repo, roster, "lb-1", language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.moveEntryUp("e-3")
        advanceUntilIdle()

        val sortedIds = repo.entries.value["lb-1"].orEmpty()
            .sortedWith(compareBy({ it.insertionOrder }, { it.id }))
            .map { it.id }
        assertEquals(listOf("e-1", "e-3", "e-2"), sortedIds)
    }

    @Test
    fun `moveEntryDown swaps insertionOrder with the next entry`() = runTest(dispatcher) {
        val lorebook = sampleLorebook(id = "lb-1")
        val entries = listOf(
            sampleEntry("lb-1", id = "e-1", insertionOrder = 10),
            sampleEntry("lb-1", id = "e-2", insertionOrder = 20),
            sampleEntry("lb-1", id = "e-3", insertionOrder = 30),
        )
        val (repo, roster) = buildRepos(listOf(lorebook), mapOf("lb-1" to entries))
        val viewModel = WorldInfoEditorViewModel(repo, roster, "lb-1", language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.moveEntryDown("e-1")
        advanceUntilIdle()

        val sortedIds = repo.entries.value["lb-1"].orEmpty()
            .sortedWith(compareBy({ it.insertionOrder }, { it.id }))
            .map { it.id }
        assertEquals(listOf("e-2", "e-1", "e-3"), sortedIds)
    }

    @Test
    fun `moveEntryUp at the top is a safe no-op`() = runTest(dispatcher) {
        val lorebook = sampleLorebook(id = "lb-1")
        val entries = listOf(
            sampleEntry("lb-1", id = "e-1", insertionOrder = 10),
            sampleEntry("lb-1", id = "e-2", insertionOrder = 20),
        )
        val (repo, roster) = buildRepos(listOf(lorebook), mapOf("lb-1" to entries))
        val viewModel = WorldInfoEditorViewModel(repo, roster, "lb-1", language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.moveEntryUp("e-1")
        advanceUntilIdle()

        val stored = repo.entries.value["lb-1"].orEmpty().associate { it.id to it.insertionOrder }
        assertEquals(10, stored["e-1"])
        assertEquals(20, stored["e-2"])
    }

    @Test
    fun `toggleEntryEnabled flips the enabled flag`() = runTest(dispatcher) {
        val lorebook = sampleLorebook(id = "lb-1")
        val entries = listOf(sampleEntry("lb-1", id = "e-1", insertionOrder = 1, enabled = true))
        val (repo, roster) = buildRepos(listOf(lorebook), mapOf("lb-1" to entries))
        val viewModel = WorldInfoEditorViewModel(repo, roster, "lb-1", language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.toggleEntryEnabled("e-1")
        advanceUntilIdle()
        assertFalse(repo.entries.value["lb-1"]!!.single().enabled)

        viewModel.toggleEntryEnabled("e-1")
        advanceUntilIdle()
        assertTrue(repo.entries.value["lb-1"]!!.single().enabled)
    }

    @Test
    fun `deleteEntry removes the entry from the lorebook`() = runTest(dispatcher) {
        val lorebook = sampleLorebook(id = "lb-1")
        val entries = listOf(
            sampleEntry("lb-1", id = "e-1", insertionOrder = 1),
            sampleEntry("lb-1", id = "e-2", insertionOrder = 2),
        )
        val (repo, roster) = buildRepos(listOf(lorebook), mapOf("lb-1" to entries))
        val viewModel = WorldInfoEditorViewModel(repo, roster, "lb-1", language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.deleteEntry("e-1")
        advanceUntilIdle()

        val remaining = repo.entries.value["lb-1"].orEmpty().map { it.id }
        assertEquals(listOf("e-2"), remaining)
    }

    @Test
    fun `bindings list resolves display names through the companion roster`() = runTest(dispatcher) {
        val lorebook = sampleLorebook(id = "lb-1")
        val bindings = listOf(
            LorebookBinding(lorebookId = "lb-1", characterId = "char-alpha", isPrimary = true),
        )
        val presets = listOf(sampleCharacter("char-alpha", "Alpha", "阿尔法"))
        val (repo, roster) = buildRepos(
            lorebooks = listOf(lorebook),
            bindings = mapOf("lb-1" to bindings),
            presetCharacters = presets,
        )
        val viewModel = WorldInfoEditorViewModel(repo, roster, "lb-1", language = { AppLanguage.English })
        advanceUntilIdle()

        val rows = viewModel.uiState.value.bindings
        assertEquals(1, rows.size)
        assertEquals("char-alpha", rows.single().characterId)
        assertEquals("Alpha", rows.single().displayName)
        assertTrue(rows.single().isPrimary)
    }

    @Test
    fun `bind CTA dispatches repository bind and leaves primary flag false by default`() = runTest(dispatcher) {
        val lorebook = sampleLorebook(id = "lb-1")
        val presets = listOf(sampleCharacter("char-alpha", "Alpha", "阿尔法"))
        val (repo, roster) = buildRepos(
            lorebooks = listOf(lorebook),
            presetCharacters = presets,
        )
        val viewModel = WorldInfoEditorViewModel(repo, roster, "lb-1", language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.bindCharacter("char-alpha")
        advanceUntilIdle()

        val stored = repo.bindings.value["lb-1"].orEmpty().single()
        assertEquals("char-alpha", stored.characterId)
        assertFalse(stored.isPrimary)
    }

    @Test
    fun `picker excludes already-bound characters`() = runTest(dispatcher) {
        val lorebook = sampleLorebook(id = "lb-1")
        val bindings = listOf(LorebookBinding(lorebookId = "lb-1", characterId = "char-alpha"))
        val presets = listOf(
            sampleCharacter("char-alpha", "Alpha", "阿尔法"),
            sampleCharacter("char-beta", "Beta", "贝塔"),
        )
        val (repo, roster) = buildRepos(
            lorebooks = listOf(lorebook),
            bindings = mapOf("lb-1" to bindings),
            presetCharacters = presets,
        )
        val viewModel = WorldInfoEditorViewModel(repo, roster, "lb-1", language = { AppLanguage.English })
        advanceUntilIdle()

        val pickerIds = viewModel.uiState.value.bindablePickerItems.map { it.characterId }
        assertEquals(listOf("char-beta"), pickerIds)
    }

    @Test
    fun `unbindCharacter removes the binding`() = runTest(dispatcher) {
        val lorebook = sampleLorebook(id = "lb-1")
        val bindings = listOf(LorebookBinding(lorebookId = "lb-1", characterId = "char-alpha"))
        val (repo, roster) = buildRepos(
            lorebooks = listOf(lorebook),
            bindings = mapOf("lb-1" to bindings),
            presetCharacters = listOf(sampleCharacter("char-alpha", "Alpha", "阿尔法")),
        )
        val viewModel = WorldInfoEditorViewModel(repo, roster, "lb-1", language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.unbindCharacter("char-alpha")
        advanceUntilIdle()

        assertTrue(repo.bindings.value["lb-1"].orEmpty().isEmpty())
    }

    @Test
    fun `togglePrimaryBinding flips the primary flag on the binding`() = runTest(dispatcher) {
        val lorebook = sampleLorebook(id = "lb-1")
        val bindings = listOf(
            LorebookBinding(lorebookId = "lb-1", characterId = "char-alpha", isPrimary = false),
        )
        val (repo, roster) = buildRepos(
            lorebooks = listOf(lorebook),
            bindings = mapOf("lb-1" to bindings),
            presetCharacters = listOf(sampleCharacter("char-alpha", "Alpha", "阿尔法")),
        )
        val viewModel = WorldInfoEditorViewModel(repo, roster, "lb-1", language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.togglePrimaryBinding("char-alpha")
        advanceUntilIdle()
        assertTrue(repo.bindings.value["lb-1"]!!.single().isPrimary)

        viewModel.togglePrimaryBinding("char-alpha")
        advanceUntilIdle()
        assertFalse(repo.bindings.value["lb-1"]!!.single().isPrimary)
    }

    @Test
    fun `header is null when the lorebookId does not exist`() = runTest(dispatcher) {
        val (repo, roster) = buildRepos(emptyList())
        val viewModel = WorldInfoEditorViewModel(repo, roster, "missing", language = { AppLanguage.English })
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.header)
        assertTrue(viewModel.uiState.value.entries.isEmpty())
        assertTrue(viewModel.uiState.value.bindings.isEmpty())
    }

    private fun sampleLorebook(
        id: String,
        english: String = "Lorebook $id",
        chinese: String = "世界书 $id",
        descriptionEn: String = "",
        descriptionZh: String = "",
        tokenBudget: Int = 1_024,
        isGlobal: Boolean = false,
        isBuiltIn: Boolean = false,
    ): Lorebook = Lorebook(
        id = id,
        ownerId = "owner",
        displayName = LocalizedText(english = english, chinese = chinese),
        description = LocalizedText(english = descriptionEn, chinese = descriptionZh),
        tokenBudget = tokenBudget,
        isGlobal = isGlobal,
        isBuiltIn = isBuiltIn,
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )

    private fun sampleEntry(
        lorebookId: String,
        id: String,
        insertionOrder: Int,
        enabled: Boolean = true,
    ): LorebookEntry = LorebookEntry(
        id = id,
        lorebookId = lorebookId,
        name = LocalizedText.of("entry-$id"),
        insertionOrder = insertionOrder,
        enabled = enabled,
    )

    private fun sampleCharacter(
        id: String,
        english: String,
        chinese: String,
    ): CompanionCharacterCard = CompanionCharacterCard(
        id = id,
        displayName = LocalizedText(english = english, chinese = chinese),
        roleLabel = LocalizedText.Empty,
        summary = LocalizedText.Empty,
        firstMes = LocalizedText.Empty,
        avatarText = english.take(2).uppercase(),
        accent = com.gkim.im.android.core.model.AccentTone.Primary,
        source = com.gkim.im.android.core.model.CompanionCharacterSource.Preset,
    )

    private fun buildRepos(
        lorebooks: List<Lorebook>,
        entries: Map<String, List<LorebookEntry>> = emptyMap(),
        bindings: Map<String, List<LorebookBinding>> = emptyMap(),
        presetCharacters: List<CompanionCharacterCard> = emptyList(),
    ): Pair<DefaultWorldInfoRepository, DefaultCompanionRosterRepository> {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = lorebooks,
            initialEntries = entries,
            initialBindings = bindings,
        )
        val roster = DefaultCompanionRosterRepository(
            presetCharacters = presetCharacters,
            drawPool = emptyList(),
        )
        return repo to roster
    }
}
