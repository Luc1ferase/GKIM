package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Lorebook
import com.gkim.im.android.core.model.LorebookBinding
import com.gkim.im.android.core.model.LorebookEntry
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterDetailLorebookTabTest {

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
    fun `empty state exposes no rows when character has no bindings`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(sampleLorebook("lb-1", "Alpha", "阿尔法")),
            initialEntries = mapOf("lb-1" to listOf(sampleEntry("lb-1", "e-1"))),
            initialBindings = emptyMap(),
        )
        val viewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CHARACTER_ID, state.characterId)
        assertTrue(state.isEmpty)
    }

    @Test
    fun `rows expose displayName and entry count per bound lorebook`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(
                sampleLorebook("lb-1", "Alpha", "阿尔法"),
                sampleLorebook("lb-2", "Beta", "贝塔"),
            ),
            initialEntries = mapOf(
                "lb-1" to listOf(
                    sampleEntry("lb-1", "e-1"),
                    sampleEntry("lb-1", "e-2"),
                ),
                "lb-2" to listOf(sampleEntry("lb-2", "e-1")),
            ),
            initialBindings = mapOf(
                "lb-1" to listOf(LorebookBinding(lorebookId = "lb-1", characterId = CHARACTER_ID)),
                "lb-2" to listOf(LorebookBinding(lorebookId = "lb-2", characterId = CHARACTER_ID)),
            ),
        )
        val viewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()

        val rows = viewModel.uiState.value.rows
        assertEquals(2, rows.size)
        val alpha = rows.single { it.lorebookId == "lb-1" }
        assertEquals("Alpha", alpha.displayName)
        assertEquals(2, alpha.entryCount)
        val beta = rows.single { it.lorebookId == "lb-2" }
        assertEquals("Beta", beta.displayName)
        assertEquals(1, beta.entryCount)
    }

    @Test
    fun `rows resolve displayName in the active language`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(sampleLorebook("lb-1", "Alpha", "阿尔法")),
            initialBindings = mapOf(
                "lb-1" to listOf(LorebookBinding(lorebookId = "lb-1", characterId = CHARACTER_ID)),
            ),
        )
        val chineseViewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.Chinese }
        advanceUntilIdle()
        assertEquals("阿尔法", chineseViewModel.uiState.value.rows.single().displayName)
    }

    @Test
    fun `rows fall back to Untitled lorebook when both languages are blank`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(sampleLorebook("lb-1", english = "", chinese = "")),
            initialBindings = mapOf(
                "lb-1" to listOf(LorebookBinding(lorebookId = "lb-1", characterId = CHARACTER_ID)),
            ),
        )
        val englishViewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()
        assertEquals("Untitled lorebook", englishViewModel.uiState.value.rows.single().displayName)

        val chineseViewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.Chinese }
        advanceUntilIdle()
        assertEquals("未命名世界书", chineseViewModel.uiState.value.rows.single().displayName)
    }

    @Test
    fun `rows only include bindings for the target character`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(
                sampleLorebook("lb-1", "Alpha", "阿尔法"),
                sampleLorebook("lb-2", "Beta", "贝塔"),
            ),
            initialBindings = mapOf(
                "lb-1" to listOf(LorebookBinding(lorebookId = "lb-1", characterId = CHARACTER_ID)),
                "lb-2" to listOf(LorebookBinding(lorebookId = "lb-2", characterId = "someone-else")),
            ),
        )
        val viewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()

        val rows = viewModel.uiState.value.rows
        assertEquals(listOf("lb-1"), rows.map { it.lorebookId })
    }

    @Test
    fun `primary binding sorts before non-primary and isPrimary flag is exposed`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(
                sampleLorebook("lb-1", "Alpha", "阿尔法"),
                sampleLorebook("lb-2", "Beta", "贝塔"),
            ),
            initialBindings = mapOf(
                "lb-1" to listOf(LorebookBinding(lorebookId = "lb-1", characterId = CHARACTER_ID, isPrimary = false)),
                "lb-2" to listOf(LorebookBinding(lorebookId = "lb-2", characterId = CHARACTER_ID, isPrimary = true)),
            ),
        )
        val viewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()

        val rows = viewModel.uiState.value.rows
        assertEquals("lb-2", rows.first().lorebookId)
        assertTrue(rows.first().isPrimary)
        assertEquals("lb-1", rows.last().lorebookId)
        assertFalse(rows.last().isPrimary)
    }

    @Test
    fun `rows sort alphabetically inside each primary bucket`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(
                sampleLorebook("lb-1", "Gamma", "伽马"),
                sampleLorebook("lb-2", "Alpha", "阿尔法"),
                sampleLorebook("lb-3", "Beta", "贝塔"),
            ),
            initialBindings = mapOf(
                "lb-1" to listOf(LorebookBinding(lorebookId = "lb-1", characterId = CHARACTER_ID)),
                "lb-2" to listOf(LorebookBinding(lorebookId = "lb-2", characterId = CHARACTER_ID)),
                "lb-3" to listOf(LorebookBinding(lorebookId = "lb-3", characterId = CHARACTER_ID)),
            ),
        )
        val viewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()

        val rows = viewModel.uiState.value.rows
        assertEquals(listOf("Alpha", "Beta", "Gamma"), rows.map { it.displayName })
    }

    @Test
    fun `missing lorebook referenced by a binding is filtered out`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(sampleLorebook("lb-1", "Alpha", "阿尔法")),
            initialBindings = mapOf(
                "lb-1" to listOf(LorebookBinding(lorebookId = "lb-1", characterId = CHARACTER_ID)),
                "ghost" to listOf(LorebookBinding(lorebookId = "ghost", characterId = CHARACTER_ID)),
            ),
        )
        val viewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()

        val rows = viewModel.uiState.value.rows
        assertEquals(listOf("lb-1"), rows.map { it.lorebookId })
    }

    @Test
    fun `manage callback fires with the tapped lorebookId for deep-link into the editor`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(sampleLorebook("lb-1", "Alpha", "阿尔法")),
            initialBindings = mapOf(
                "lb-1" to listOf(LorebookBinding(lorebookId = "lb-1", characterId = CHARACTER_ID)),
            ),
        )
        val viewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()

        var requested: String? = null
        val onManage: (String) -> Unit = { requested = it }
        val row = viewModel.uiState.value.rows.single()
        assertNotNull(row)

        onManage(row.lorebookId)
        assertEquals("lb-1", requested)
    }

    @Test
    fun `rows update live when a new binding is added`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(sampleLorebook("lb-1", "Alpha", "阿尔法")),
            initialBindings = emptyMap(),
        )
        val viewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isEmpty)

        repo.bind(lorebookId = "lb-1", characterId = CHARACTER_ID, isPrimary = true)
        advanceUntilIdle()

        val rows = viewModel.uiState.value.rows
        assertEquals(1, rows.size)
        assertTrue(rows.single().isPrimary)
    }

    @Test
    fun `pickerItems expose unbound lorebooks sorted alphabetically`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(
                sampleLorebook("lb-1", "Gamma", "伽马"),
                sampleLorebook("lb-2", "Alpha", "阿尔法"),
                sampleLorebook("lb-3", "Beta", "贝塔"),
            ),
            initialBindings = emptyMap(),
        )
        val viewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()

        val picker = viewModel.uiState.value.pickerItems
        assertEquals(listOf("Alpha", "Beta", "Gamma"), picker.map { it.displayName })
        assertTrue(viewModel.uiState.value.canBind)
    }

    @Test
    fun `pickerItems exclude lorebooks already bound to this character`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(
                sampleLorebook("lb-1", "Alpha", "阿尔法"),
                sampleLorebook("lb-2", "Beta", "贝塔"),
                sampleLorebook("lb-3", "Gamma", "伽马"),
            ),
            initialBindings = mapOf(
                "lb-1" to listOf(LorebookBinding(lorebookId = "lb-1", characterId = CHARACTER_ID)),
            ),
        )
        val viewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()

        val picker = viewModel.uiState.value.pickerItems
        assertEquals(listOf("lb-2", "lb-3"), picker.map { it.lorebookId }.sorted())
    }

    @Test
    fun `pickerItems still include lorebooks bound only to other characters`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(
                sampleLorebook("lb-1", "Alpha", "阿尔法"),
                sampleLorebook("lb-2", "Beta", "贝塔"),
            ),
            initialBindings = mapOf(
                "lb-1" to listOf(LorebookBinding(lorebookId = "lb-1", characterId = "someone-else")),
            ),
        )
        val viewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()

        val picker = viewModel.uiState.value.pickerItems
        assertEquals(listOf("lb-1", "lb-2"), picker.map { it.lorebookId }.sorted())
    }

    @Test
    fun `canBind is false when there are no pickerItems`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(sampleLorebook("lb-1", "Alpha", "阿尔法")),
            initialBindings = mapOf(
                "lb-1" to listOf(LorebookBinding(lorebookId = "lb-1", characterId = CHARACTER_ID)),
            ),
        )
        val viewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canBind)
        assertTrue(viewModel.uiState.value.pickerItems.isEmpty())
    }

    @Test
    fun `pickerItems fall back to Untitled when displayName is blank`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(sampleLorebook("lb-1", english = "", chinese = "")),
            initialBindings = emptyMap(),
        )
        val englishViewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()
        assertEquals("Untitled lorebook", englishViewModel.uiState.value.pickerItems.single().displayName)

        val chineseViewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.Chinese }
        advanceUntilIdle()
        assertEquals("未命名世界书", chineseViewModel.uiState.value.pickerItems.single().displayName)
    }

    @Test
    fun `bind creates a non-primary binding for this character`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(sampleLorebook("lb-1", "Alpha", "阿尔法")),
            initialBindings = emptyMap(),
        )
        val viewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isEmpty)

        viewModel.bind("lb-1")
        advanceUntilIdle()

        val rows = viewModel.uiState.value.rows
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("lb-1", row.lorebookId)
        assertFalse(row.isPrimary)
        assertEquals(null, viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.pickerItems.isEmpty())
    }

    @Test
    fun `bind surfaces bilingual error when lorebook is already bound`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(sampleLorebook("lb-1", "Alpha", "阿尔法")),
            initialBindings = mapOf(
                "lb-1" to listOf(LorebookBinding(lorebookId = "lb-1", characterId = CHARACTER_ID)),
            ),
        )
        val viewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()

        viewModel.bind("lb-1")
        advanceUntilIdle()
        assertEquals("Lorebook already bound", viewModel.uiState.value.errorMessage)

        val chineseViewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.Chinese }
        advanceUntilIdle()
        chineseViewModel.bind("lb-1")
        advanceUntilIdle()
        assertEquals("世界书已绑定", chineseViewModel.uiState.value.errorMessage)
    }

    @Test
    fun `bind surfaces bilingual error when lorebook is unknown`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(sampleLorebook("lb-1", "Alpha", "阿尔法")),
            initialBindings = emptyMap(),
        )
        val viewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()

        viewModel.bind("ghost")
        advanceUntilIdle()
        assertEquals("Lorebook not found", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `clearError resets the error banner`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(sampleLorebook("lb-1", "Alpha", "阿尔法")),
            initialBindings = mapOf(
                "lb-1" to listOf(LorebookBinding(lorebookId = "lb-1", characterId = CHARACTER_ID)),
            ),
        )
        val viewModel = CharacterLorebookTabViewModel(repo, CHARACTER_ID) { AppLanguage.English }
        advanceUntilIdle()
        viewModel.bind("lb-1")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    companion object {
        private const val CHARACTER_ID = "char-alpha"
    }

    private fun sampleLorebook(
        id: String,
        english: String = "Lorebook $id",
        chinese: String = "世界书 $id",
    ): Lorebook = Lorebook(
        id = id,
        ownerId = "owner",
        displayName = LocalizedText(english = english, chinese = chinese),
        description = LocalizedText.Empty,
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )

    private fun sampleEntry(lorebookId: String, id: String): LorebookEntry = LorebookEntry(
        id = id,
        lorebookId = lorebookId,
        name = LocalizedText.of("entry-$id"),
    )
}
