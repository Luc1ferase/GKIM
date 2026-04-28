package com.gkim.im.android.feature.settings

import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Preset
import com.gkim.im.android.core.model.PresetParams
import com.gkim.im.android.core.model.PresetTemplate
import com.gkim.im.android.core.model.PresetValidationError
import com.gkim.im.android.data.repository.DefaultCompanionPresetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class PresetEditorPresentationTest {

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
    fun `loads existing preset into editor state including template and params`() = runTest(dispatcher) {
        val (repo, presetId) = createRepoWithUserPreset()
        val vm = PresetEditorViewModel(repo, presetId)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(presetId, state.presetId)
        assertEquals("Concise", state.englishDisplayName)
        assertEquals("简洁", state.chineseDisplayName)
        assertEquals("Short replies.", state.englishDescription)
        assertEquals("简短回复。", state.chineseDescription)
        assertEquals("You are Nova.", state.englishSystemPrefix)
        assertEquals("你是 Nova。", state.chineseSystemPrefix)
        assertEquals("End with a question.", state.englishSystemSuffix)
        assertEquals("以问题结束。", state.chineseSystemSuffix)
        assertEquals("Reply in prose.", state.englishFormatInstructions)
        assertEquals("用散文回复。", state.chineseFormatInstructions)
        assertEquals("Stay in character.", state.englishPostHistoryInstructions)
        assertEquals("保持角色一致。", state.chinesePostHistoryInstructions)
        assertEquals("0.7", state.temperatureText)
        assertEquals("0.9", state.topPText)
        assertEquals("512", state.maxReplyTokensText)
        assertFalse(state.isBuiltIn)
        assertFalse(state.isActive)
        assertFalse("no changes yet so save should be disabled", state.canSave)
        assertEquals(emptyList<PresetValidationError>(), state.validationErrors)
    }

    @Test
    fun `validation surfaces blank english display name and blocks save`() = runTest(dispatcher) {
        val (repo, presetId) = createRepoWithUserPreset()
        val vm = PresetEditorViewModel(repo, presetId)
        advanceUntilIdle()

        vm.setEnglishDisplayName("   ")
        assertTrue(vm.uiState.value.validationErrors.contains(PresetValidationError.DisplayNameEnglishBlank))
        assertFalse("cannot save with blank english display name", vm.uiState.value.canSave)

        vm.setEnglishDisplayName("Restored")
        assertFalse(vm.uiState.value.validationErrors.contains(PresetValidationError.DisplayNameEnglishBlank))
        assertTrue("edits without blanks should enable save", vm.uiState.value.canSave)
    }

    @Test
    fun `validation surfaces blank chinese display name and blocks save`() = runTest(dispatcher) {
        val (repo, presetId) = createRepoWithUserPreset()
        val vm = PresetEditorViewModel(repo, presetId)
        advanceUntilIdle()

        vm.setChineseDisplayName("")
        assertTrue(vm.uiState.value.validationErrors.contains(PresetValidationError.DisplayNameChineseBlank))
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `validation surfaces out-of-range numeric params and blocks save`() = runTest(dispatcher) {
        val (repo, presetId) = createRepoWithUserPreset()
        val vm = PresetEditorViewModel(repo, presetId)
        advanceUntilIdle()

        vm.setTemperatureText("5.0")
        vm.setTopPText("-0.1")
        vm.setMaxReplyTokensText("0")

        val errors = vm.uiState.value.validationErrors.toSet()
        assertTrue(errors.contains(PresetValidationError.TemperatureOutOfRange))
        assertTrue(errors.contains(PresetValidationError.TopPOutOfRange))
        assertTrue(errors.contains(PresetValidationError.MaxReplyTokensOutOfRange))
        assertFalse("out-of-range params should block save", vm.uiState.value.canSave)
    }

    @Test
    fun `non-numeric param text flags as out-of-range and blocks save`() = runTest(dispatcher) {
        val (repo, presetId) = createRepoWithUserPreset()
        val vm = PresetEditorViewModel(repo, presetId)
        advanceUntilIdle()

        vm.setTemperatureText("abc")
        assertTrue(
            vm.uiState.value.validationErrors.contains(PresetValidationError.TemperatureOutOfRange),
        )
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `save success persists updated preset and updates baseline snapshot`() = runTest(dispatcher) {
        val (repo, presetId) = createRepoWithUserPreset()
        val vm = PresetEditorViewModel(repo, presetId)
        advanceUntilIdle()

        vm.setEnglishDisplayName("Concise v2")
        vm.setEnglishSystemPrefix("You are Nova (refined).")
        vm.setTemperatureText("0.5")
        assertTrue("edits should mark canSave", vm.uiState.value.canSave)

        vm.save()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isSaving)
        assertNull(state.saveError)
        val saved = state.savedPreset
        assertNotNull("savedPreset should be populated on success", saved)
        assertEquals("Concise v2", saved!!.displayName.english)
        assertEquals("You are Nova (refined).", saved.template.systemPrefix.english)
        assertEquals(0.5, saved.params.temperature!!, 0.0001)
        assertFalse("post-save baseline matches current fields so canSave flips false", state.canSave)
        assertFalse(state.hasUnsavedChanges)

        val persisted = repo.observePresets().first().first { it.id == presetId }
        assertEquals("Concise v2", persisted.displayName.english)
        assertEquals("You are Nova (refined).", persisted.template.systemPrefix.english)
        assertEquals(0.5, persisted.params.temperature!!, 0.0001)
    }

    @Test
    fun `cancel discards unsaved changes and restores loaded snapshot`() = runTest(dispatcher) {
        val (repo, presetId) = createRepoWithUserPreset()
        val vm = PresetEditorViewModel(repo, presetId)
        advanceUntilIdle()

        vm.setEnglishDisplayName("Rewritten")
        vm.setChineseSystemPrefix("改写过的前缀。")
        vm.setMaxReplyTokensText("2048")
        assertTrue(vm.uiState.value.hasUnsavedChanges)

        vm.cancel()

        val state = vm.uiState.value
        assertEquals("Concise", state.englishDisplayName)
        assertEquals("你是 Nova。", state.chineseSystemPrefix)
        assertEquals("512", state.maxReplyTokensText)
        assertFalse("snapshot restore means canSave is false", state.canSave)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun `unknown preset id surfaces saveError and disables save`() = runTest(dispatcher) {
        val repo = DefaultCompanionPresetRepository(initialPresets = listOf(builtInPreset()))
        val vm = PresetEditorViewModel(repo, presetId = "preset-missing")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Preset not found", state.saveError)
        assertFalse(state.canSave)
    }

    @Test
    fun `built-in preset cannot be saved even when fields are valid`() = runTest(dispatcher) {
        val builtIn = builtInPreset()
        val repo = DefaultCompanionPresetRepository(initialPresets = listOf(builtIn))
        val vm = PresetEditorViewModel(repo, presetId = builtIn.id)
        advanceUntilIdle()

        vm.setEnglishDescription("Changed built-in description.")
        assertTrue(vm.uiState.value.hasUnsavedChanges)
        assertFalse("built-in preset can never be saved from this editor", vm.uiState.value.canSave)
    }

    @Test
    fun `clearing all param text keeps canSave enabled when other fields changed`() = runTest(dispatcher) {
        val (repo, presetId) = createRepoWithUserPreset()
        val vm = PresetEditorViewModel(repo, presetId)
        advanceUntilIdle()

        vm.setTemperatureText("")
        vm.setTopPText("")
        vm.setMaxReplyTokensText("")
        assertTrue("blank params are valid (treated as null) so save is enabled", vm.uiState.value.canSave)
        assertEquals(emptyList<PresetValidationError>(), vm.uiState.value.validationErrors)
    }

    private fun createRepoWithUserPreset(): Pair<DefaultCompanionPresetRepository, String> {
        val now = 2_000L
        val builtIn = builtInPreset().copy(isActive = true, createdAt = 1_000L, updatedAt = 1_000L)
        val userPreset = Preset(
            id = "preset-concise",
            displayName = LocalizedText("Concise", "简洁"),
            description = LocalizedText("Short replies.", "简短回复。"),
            template = PresetTemplate(
                systemPrefix = LocalizedText("You are Nova.", "你是 Nova。"),
                systemSuffix = LocalizedText("End with a question.", "以问题结束。"),
                formatInstructions = LocalizedText("Reply in prose.", "用散文回复。"),
                postHistoryInstructions = LocalizedText("Stay in character.", "保持角色一致。"),
            ),
            params = PresetParams(
                temperature = 0.7,
                topP = 0.9,
                maxReplyTokens = 512,
            ),
            isBuiltIn = false,
            isActive = false,
            createdAt = now,
            updatedAt = now,
        )
        val repo = DefaultCompanionPresetRepository(
            initialPresets = listOf(builtIn, userPreset),
            clock = { 9_999L },
        )
        return repo to userPreset.id
    }

    private fun builtInPreset(): Preset = Preset(
        id = "preset-builtin-default",
        displayName = LocalizedText("Default", "默认"),
        description = LocalizedText("Default preset.", "默认预设。"),
        isBuiltIn = true,
        isActive = false,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
