package com.gkim.im.android.feature.settings.worldinfo

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Lorebook
import com.gkim.im.android.core.model.LorebookBinding
import com.gkim.im.android.core.model.LorebookEntry
import com.gkim.im.android.data.repository.DefaultWorldInfoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
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
class WorldInfoLibraryPresentationTest {

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
    fun `rows expose resolved display name and entry count per lorebook`() = runTest(dispatcher) {
        val lorebookA = sampleLorebook(id = "lb-a", createdAt = 2_000L, english = "Alpha", chinese = "阿尔法")
        val lorebookB = sampleLorebook(id = "lb-b", createdAt = 1_000L, english = "Bravo", chinese = "布拉沃")
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(lorebookA, lorebookB),
            initialEntries = mapOf(
                "lb-a" to listOf(sampleEntry("lb-a", id = "e1"), sampleEntry("lb-a", id = "e2")),
                "lb-b" to listOf(sampleEntry("lb-b", id = "e3")),
            ),
        )
        val viewModel = WorldInfoLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        val ids = viewModel.uiState.value.rows.map { it.lorebookId }
        assertEquals(listOf("lb-b", "lb-a"), ids)
        val a = viewModel.uiState.value.rows.first { it.lorebookId == "lb-a" }
        assertEquals("Alpha", a.displayName)
        assertEquals(2, a.entryCount)
        val b = viewModel.uiState.value.rows.first { it.lorebookId == "lb-b" }
        assertEquals("Bravo", b.displayName)
        assertEquals(1, b.entryCount)
    }

    @Test
    fun `display name falls back to untitled when both languages are blank`() = runTest(dispatcher) {
        val blank = sampleLorebook(id = "lb-blank", english = "", chinese = "")
        val repoEn = DefaultWorldInfoRepository(initialLorebooks = listOf(blank))
        val vmEn = WorldInfoLibraryViewModel(repoEn, language = { AppLanguage.English })
        advanceUntilIdle()
        assertEquals("Untitled lorebook", vmEn.uiState.value.rows.single().displayName)

        val repoZh = DefaultWorldInfoRepository(initialLorebooks = listOf(blank))
        val vmZh = WorldInfoLibraryViewModel(repoZh, language = { AppLanguage.Chinese })
        advanceUntilIdle()
        assertEquals("未命名世界书", vmZh.uiState.value.rows.single().displayName)
    }

    @Test
    fun `global badge flag reflects lorebook isGlobal`() = runTest(dispatcher) {
        val global = sampleLorebook(id = "lb-global", isGlobal = true)
        val local = sampleLorebook(id = "lb-local", isGlobal = false)
        val repo = DefaultWorldInfoRepository(initialLorebooks = listOf(global, local))
        val viewModel = WorldInfoLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        val globalRow = viewModel.uiState.value.rows.first { it.lorebookId == "lb-global" }
        val localRow = viewModel.uiState.value.rows.first { it.lorebookId == "lb-local" }
        assertTrue(globalRow.isGlobal)
        assertFalse(localRow.isGlobal)
    }

    @Test
    fun `delete is disabled when the lorebook has bindings`() = runTest(dispatcher) {
        val bound = sampleLorebook(id = "lb-bound")
        val free = sampleLorebook(id = "lb-free")
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(bound, free),
            initialBindings = mapOf(
                "lb-bound" to listOf(LorebookBinding(lorebookId = "lb-bound", characterId = "char-1")),
                "lb-free" to emptyList(),
            ),
        )
        val viewModel = WorldInfoLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        val boundRow = viewModel.uiState.value.rows.first { it.lorebookId == "lb-bound" }
        val freeRow = viewModel.uiState.value.rows.first { it.lorebookId == "lb-free" }
        assertTrue(boundRow.hasBindings)
        assertFalse(boundRow.canDelete)
        assertEquals(1, boundRow.boundCharacterCount)
        assertFalse(freeRow.hasBindings)
        assertTrue(freeRow.canDelete)
    }

    @Test
    fun `delete is disabled for built-in lorebooks even when unbound`() = runTest(dispatcher) {
        val builtIn = sampleLorebook(id = "lb-builtin", isBuiltIn = true)
        val repo = DefaultWorldInfoRepository(initialLorebooks = listOf(builtIn))
        val viewModel = WorldInfoLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        val row = viewModel.uiState.value.rows.single()
        assertTrue(row.isBuiltIn)
        assertFalse(row.canDelete)
        assertFalse(row.canToggleGlobal)
    }

    @Test
    fun `create CTA produces a new user-owned lorebook with bilingual default name`() = runTest(dispatcher) {
        val repo = DefaultWorldInfoRepository()
        val viewModel = WorldInfoLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.rows.isEmpty())

        viewModel.createLorebook(ownerId = "user-1")
        advanceUntilIdle()

        val created = repo.lorebooks.value.single()
        assertEquals("user-1", created.ownerId)
        assertEquals("New lorebook", created.displayName.english)
        assertEquals("新世界书", created.displayName.chinese)
        assertFalse(created.isBuiltIn)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `duplicate dispatches to repository and produces a sibling lorebook`() = runTest(dispatcher) {
        val original = sampleLorebook(id = "lb-origin", english = "Origin", chinese = "起源")
        val repo = DefaultWorldInfoRepository(initialLorebooks = listOf(original))
        val viewModel = WorldInfoLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.duplicate("lb-origin")
        advanceUntilIdle()

        val ids = repo.lorebooks.value.map { it.id }
        assertEquals(2, ids.size)
        assertTrue("original preserved", ids.contains("lb-origin"))
        val copy = repo.lorebooks.value.first { it.id != "lb-origin" }
        assertNotNull(copy.displayName.english)
        assertTrue(
            "copy name flags the duplication",
            copy.displayName.english.contains("copy", ignoreCase = true) || copy.displayName.chinese.contains("副本"),
        )
    }

    @Test
    fun `delete removes an unbound lorebook`() = runTest(dispatcher) {
        val free = sampleLorebook(id = "lb-free")
        val repo = DefaultWorldInfoRepository(initialLorebooks = listOf(free))
        val viewModel = WorldInfoLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.delete("lb-free")
        advanceUntilIdle()

        assertTrue(repo.lorebooks.value.isEmpty())
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `delete surfaces an error when the lorebook is still bound`() = runTest(dispatcher) {
        val bound = sampleLorebook(id = "lb-bound")
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(bound),
            initialBindings = mapOf(
                "lb-bound" to listOf(LorebookBinding(lorebookId = "lb-bound", characterId = "char-1")),
            ),
        )
        val viewModel = WorldInfoLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.delete("lb-bound")
        advanceUntilIdle()

        assertEquals(listOf("lb-bound"), repo.lorebooks.value.map { it.id })
        assertEquals("Lorebook still bound to characters", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `toggleGlobal flips the lorebook isGlobal flag`() = runTest(dispatcher) {
        val target = sampleLorebook(id = "lb-toggle", isGlobal = false)
        val repo = DefaultWorldInfoRepository(initialLorebooks = listOf(target))
        val viewModel = WorldInfoLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.toggleGlobal("lb-toggle")
        advanceUntilIdle()
        assertTrue(repo.lorebooks.value.single().isGlobal)

        viewModel.toggleGlobal("lb-toggle")
        advanceUntilIdle()
        assertFalse(repo.lorebooks.value.single().isGlobal)
    }

    @Test
    fun `toggleGlobal is a no-op when the lorebook is built-in`() = runTest(dispatcher) {
        val builtIn = sampleLorebook(id = "lb-builtin", isBuiltIn = true, isGlobal = false)
        val repo = DefaultWorldInfoRepository(initialLorebooks = listOf(builtIn))
        val viewModel = WorldInfoLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.toggleGlobal("lb-builtin")
        advanceUntilIdle()

        assertFalse("built-in stays local", repo.lorebooks.value.single().isGlobal)
        assertNull("no error surfaced since the gate is client-side", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `clearError resets the error banner`() = runTest(dispatcher) {
        val bound = sampleLorebook(id = "lb-bound")
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(bound),
            initialBindings = mapOf(
                "lb-bound" to listOf(LorebookBinding(lorebookId = "lb-bound", characterId = "char-1")),
            ),
        )
        val viewModel = WorldInfoLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()
        viewModel.delete("lb-bound")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `built-in lorebooks sort before user-owned lorebooks`() = runTest(dispatcher) {
        val userOld = sampleLorebook(id = "user-old", createdAt = 1_000L)
        val builtNew = sampleLorebook(id = "built-new", createdAt = 3_000L, isBuiltIn = true)
        val builtOld = sampleLorebook(id = "built-old", createdAt = 2_000L, isBuiltIn = true)
        val userNew = sampleLorebook(id = "user-new", createdAt = 4_000L)
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(userOld, builtNew, builtOld, userNew),
        )
        val viewModel = WorldInfoLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        val ids = viewModel.uiState.value.rows.map { it.lorebookId }
        assertEquals(listOf("built-old", "built-new", "user-old", "user-new"), ids)
    }

    private fun sampleLorebook(
        id: String,
        createdAt: Long = 1_000L,
        english: String = "Lorebook $id",
        chinese: String = "世界书 $id",
        isGlobal: Boolean = false,
        isBuiltIn: Boolean = false,
    ): Lorebook = Lorebook(
        id = id,
        ownerId = "owner",
        displayName = LocalizedText(english = english, chinese = chinese),
        isGlobal = isGlobal,
        isBuiltIn = isBuiltIn,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun sampleEntry(lorebookId: String, id: String): LorebookEntry = LorebookEntry(
        id = id,
        lorebookId = lorebookId,
        name = LocalizedText.of("entry-$id"),
    )
}
