package com.gkim.im.android.feature.settings

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Preset
import com.gkim.im.android.data.repository.CompanionPresetMutationResult
import com.gkim.im.android.data.repository.CompanionPresetRepository
import com.gkim.im.android.data.repository.DefaultCompanionPresetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PresetListPresentationTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `uiState lists built-in presets first then user presets each ordered by createdAt`() = runTest(dispatcher) {
        val presets = listOf(
            samplePreset(id = "user-b", createdAt = 4_000L, isBuiltIn = false),
            samplePreset(id = "built-a", createdAt = 1_000L, isBuiltIn = true, isActive = true),
            samplePreset(id = "user-a", createdAt = 3_000L, isBuiltIn = false),
            samplePreset(id = "built-b", createdAt = 2_000L, isBuiltIn = true),
        )
        val repo = DefaultCompanionPresetRepository(initialPresets = presets)
        val viewModel = PresetLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        val ids = viewModel.uiState.value.items.map { it.preset.id }
        assertEquals(listOf("built-a", "built-b", "user-a", "user-b"), ids)
    }

    @Test
    fun `active preset flag surfaces the active badge and suppresses reactivation`() = runTest(dispatcher) {
        val presets = listOf(
            samplePreset(id = "built-a", createdAt = 1_000L, isBuiltIn = true, isActive = true),
            samplePreset(id = "user-a", createdAt = 2_000L, isBuiltIn = false),
        )
        val repo = DefaultCompanionPresetRepository(initialPresets = presets)
        val viewModel = PresetLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        val activeItems = viewModel.uiState.value.items.filter { it.isActive }
        assertEquals(listOf("built-a"), activeItems.map { it.preset.id })
        val active = activeItems.first()
        assertFalse("active preset cannot be activated again", active.canActivate)
        val inactive = viewModel.uiState.value.items.first { it.preset.id == "user-a" }
        assertFalse(inactive.isActive)
        assertTrue(inactive.canActivate)
    }

    @Test
    fun `built-in presets are neither deletable nor editable but stay duplicable`() = runTest(dispatcher) {
        val presets = listOf(
            samplePreset(id = "built-a", createdAt = 1_000L, isBuiltIn = true, isActive = true),
            samplePreset(id = "built-b", createdAt = 2_000L, isBuiltIn = true, isActive = false),
            samplePreset(id = "user-a", createdAt = 3_000L, isBuiltIn = false, isActive = false),
        )
        val repo = DefaultCompanionPresetRepository(initialPresets = presets)
        val viewModel = PresetLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        val byId = viewModel.uiState.value.items.associateBy { it.preset.id }
        val builtA = byId.getValue("built-a")
        val builtB = byId.getValue("built-b")
        val userA = byId.getValue("user-a")
        assertFalse("active built-in not deletable", builtA.canDelete)
        assertFalse("active built-in not editable", builtA.canEdit)
        assertTrue("built-ins always duplicable", builtA.canDuplicate)
        assertFalse("inactive built-in still not deletable", builtB.canDelete)
        assertFalse("inactive built-in still not editable", builtB.canEdit)
        assertTrue("inactive built-in still duplicable", builtB.canDuplicate)
        assertTrue("user preset editable", userA.canEdit)
        assertTrue("user preset deletable when inactive", userA.canDelete)
    }

    @Test
    fun `active user preset cannot be deleted but can still be edited and duplicated`() = runTest(dispatcher) {
        val presets = listOf(
            samplePreset(id = "built-a", createdAt = 1_000L, isBuiltIn = true, isActive = false),
            samplePreset(id = "user-a", createdAt = 2_000L, isBuiltIn = false, isActive = true),
        )
        val repo = DefaultCompanionPresetRepository(initialPresets = presets)
        val viewModel = PresetLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        val active = viewModel.uiState.value.items.first { it.isActive }
        assertEquals("user-a", active.preset.id)
        assertFalse("active preset cannot be deleted", active.canDelete)
        assertFalse("active preset cannot be activated again", active.canActivate)
        assertTrue("active user preset can be edited", active.canEdit)
        assertTrue("active user preset can be duplicated", active.canDuplicate)
    }

    @Test
    fun `activate switches the active preset and clears pendingOperation on success`() = runTest(dispatcher) {
        val presets = listOf(
            samplePreset(id = "built-a", createdAt = 1_000L, isBuiltIn = true, isActive = true),
            samplePreset(id = "user-a", createdAt = 2_000L, isBuiltIn = false),
        )
        val repo = DefaultCompanionPresetRepository(initialPresets = presets)
        val viewModel = PresetLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.activate("user-a")
        advanceUntilIdle()

        val activeIds = viewModel.uiState.value.items.filter { it.isActive }.map { it.preset.id }
        assertEquals(listOf("user-a"), activeIds)
        assertNull("pendingOperation should clear after success", viewModel.uiState.value.pendingOperation)
        assertNull("no error expected", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `resolved preset fields honour the language provider`() = runTest(dispatcher) {
        val presets = listOf(
            Preset(
                id = "built-a",
                displayName = LocalizedText("Concise", "简洁"),
                description = LocalizedText("Short grounded replies.", "简短贴近的回复。"),
                isBuiltIn = true,
                isActive = true,
                createdAt = 1_000L,
                updatedAt = 1_000L,
            ),
        )
        val englishRepo = DefaultCompanionPresetRepository(initialPresets = presets)
        val englishVm = PresetLibraryViewModel(englishRepo, language = { AppLanguage.English })
        advanceUntilIdle()
        val englishItem = englishVm.uiState.value.items.first()
        assertEquals("Concise", englishItem.resolved.displayName)
        assertEquals("Short grounded replies.", englishItem.resolved.description)

        val chineseRepo = DefaultCompanionPresetRepository(initialPresets = presets)
        val chineseVm = PresetLibraryViewModel(chineseRepo, language = { AppLanguage.Chinese })
        advanceUntilIdle()
        val chineseItem = chineseVm.uiState.value.items.first()
        assertEquals("简洁", chineseItem.resolved.displayName)
        assertEquals("简短贴近的回复。", chineseItem.resolved.description)
    }

    @Test
    fun `delete on active preset surfaces rejection errorMessage`() = runTest(dispatcher) {
        val presets = listOf(
            samplePreset(id = "user-a", createdAt = 1_000L, isBuiltIn = false, isActive = true),
        )
        val repo = DefaultCompanionPresetRepository(initialPresets = presets)
        val viewModel = PresetLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.delete("user-a")
        advanceUntilIdle()

        assertEquals("Active preset cannot be deleted", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.pendingOperation)
        assertEquals(
            listOf("user-a"),
            viewModel.uiState.value.items.map { it.preset.id },
        )
    }

    @Test
    fun `delete on built-in preset surfaces immutable rejection message`() = runTest(dispatcher) {
        val presets = listOf(
            samplePreset(id = "built-a", createdAt = 1_000L, isBuiltIn = true, isActive = false),
            samplePreset(id = "user-a", createdAt = 2_000L, isBuiltIn = false, isActive = true),
        )
        val repo = DefaultCompanionPresetRepository(initialPresets = presets)
        val viewModel = PresetLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.delete("built-a")
        advanceUntilIdle()

        assertEquals("Built-in preset cannot be modified", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.pendingOperation)
        val ids = viewModel.uiState.value.items.map { it.preset.id }
        assertEquals(listOf("built-a", "user-a"), ids)
    }

    @Test
    fun `failed mutation surfaces cause message and keeps list consistent`() = runTest(dispatcher) {
        val presets = listOf(
            samplePreset(id = "built-a", createdAt = 1_000L, isBuiltIn = true, isActive = true),
            samplePreset(id = "user-a", createdAt = 2_000L, isBuiltIn = false),
        )
        val failing = FailingActivateRepository(presets, IllegalStateException("server_busy"))
        val viewModel = PresetLibraryViewModel(failing, language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.activate("user-a")
        advanceUntilIdle()

        assertEquals("server_busy", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.pendingOperation)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `init triggers repository refresh exactly once and notifies completion`() = runTest(dispatcher) {
        val repo = CountingRefreshRepository(
            initialPresets = listOf(samplePreset(id = "built-a", createdAt = 1_000L, isBuiltIn = true, isActive = true)),
        )
        val viewModel = PresetLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        assertEquals(1, repo.refreshCalls)
        assertEquals(1, viewModel.refreshCompletions.value)
    }

    private fun samplePreset(
        id: String,
        createdAt: Long,
        isBuiltIn: Boolean,
        isActive: Boolean = false,
    ): Preset = Preset(
        id = id,
        displayName = LocalizedText("Display-$id", "显示-$id"),
        description = LocalizedText("Description-$id", "描述-$id"),
        isBuiltIn = isBuiltIn,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private class FailingActivateRepository(
        initialPresets: List<Preset>,
        private val cause: Throwable,
    ) : CompanionPresetRepository {
        private val state = MutableStateFlow(initialPresets)
        override fun observePresets(): Flow<List<Preset>> = state
        override fun observeActivePreset(): Flow<Preset?> =
            state.map { list -> list.firstOrNull { it.isActive } }
        override suspend fun create(preset: Preset): CompanionPresetMutationResult =
            CompanionPresetMutationResult.Success(preset)
        override suspend fun update(preset: Preset): CompanionPresetMutationResult =
            CompanionPresetMutationResult.Success(preset)
        override suspend fun delete(presetId: String): CompanionPresetMutationResult =
            CompanionPresetMutationResult.Failed(cause)
        override suspend fun activate(presetId: String): CompanionPresetMutationResult =
            CompanionPresetMutationResult.Failed(cause)
        override suspend fun duplicate(presetId: String): CompanionPresetMutationResult =
            CompanionPresetMutationResult.Failed(cause)
        override suspend fun refresh() { /* no-op */ }
    }

    private class CountingRefreshRepository(
        initialPresets: List<Preset>,
    ) : CompanionPresetRepository {
        private val delegate = DefaultCompanionPresetRepository(initialPresets = initialPresets)
        var refreshCalls: Int = 0

        override fun observePresets(): Flow<List<Preset>> = delegate.observePresets()
        override fun observeActivePreset(): Flow<Preset?> = delegate.observeActivePreset()
        override suspend fun create(preset: Preset): CompanionPresetMutationResult = delegate.create(preset)
        override suspend fun update(preset: Preset): CompanionPresetMutationResult = delegate.update(preset)
        override suspend fun delete(presetId: String): CompanionPresetMutationResult = delegate.delete(presetId)
        override suspend fun activate(presetId: String): CompanionPresetMutationResult = delegate.activate(presetId)
        override suspend fun duplicate(presetId: String): CompanionPresetMutationResult = delegate.duplicate(presetId)
        override suspend fun refresh() {
            refreshCalls += 1
        }
    }

    @Suppress("unused")
    private val keepAliveJob: Job = Job()
}
