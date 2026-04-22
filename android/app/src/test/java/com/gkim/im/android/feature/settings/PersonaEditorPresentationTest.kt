package com.gkim.im.android.feature.settings

import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.UserPersona
import com.gkim.im.android.core.model.UserPersonaValidationError
import com.gkim.im.android.data.repository.DefaultUserPersonaRepository
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
class PersonaEditorPresentationTest {

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
    fun `loads existing persona into editor state`() = runTest(dispatcher) {
        val (repo, personaId) = createRepoWithUserPersona()
        val vm = PersonaEditorViewModel(repo, personaId)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(personaId, state.personaId)
        assertEquals("Nova", state.englishDisplayName)
        assertEquals("新星", state.chineseDisplayName)
        assertEquals("A thoughtful traveller.", state.englishDescription)
        assertEquals("一位深思熟虑的旅人。", state.chineseDescription)
        assertFalse(state.isBuiltIn)
        assertFalse(state.isActive)
        assertFalse("no changes yet so save should be disabled", state.canSave)
        assertEquals(emptyList<UserPersonaValidationError>(), state.validationErrors)
    }

    @Test
    fun `validation surfaces blank english display name and blocks save`() = runTest(dispatcher) {
        val (repo, personaId) = createRepoWithUserPersona()
        val vm = PersonaEditorViewModel(repo, personaId)
        advanceUntilIdle()

        vm.setEnglishDisplayName("   ")
        assertTrue(vm.uiState.value.validationErrors.contains(UserPersonaValidationError.DisplayNameEnglishBlank))
        assertFalse("cannot save with blank english display name", vm.uiState.value.canSave)

        vm.setEnglishDisplayName("Restored")
        assertFalse(vm.uiState.value.validationErrors.contains(UserPersonaValidationError.DisplayNameEnglishBlank))
        assertTrue("edits without blanks should enable save", vm.uiState.value.canSave)
    }

    @Test
    fun `validation surfaces every blank field side`() = runTest(dispatcher) {
        val (repo, personaId) = createRepoWithUserPersona()
        val vm = PersonaEditorViewModel(repo, personaId)
        advanceUntilIdle()

        vm.setEnglishDisplayName("")
        vm.setChineseDisplayName("")
        vm.setEnglishDescription("")
        vm.setChineseDescription("")

        val errors = vm.uiState.value.validationErrors.toSet()
        assertEquals(
            setOf(
                UserPersonaValidationError.DisplayNameEnglishBlank,
                UserPersonaValidationError.DisplayNameChineseBlank,
                UserPersonaValidationError.DescriptionEnglishBlank,
                UserPersonaValidationError.DescriptionChineseBlank,
            ),
            errors,
        )
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `save success persists updated persona and updates baseline snapshot`() = runTest(dispatcher) {
        val (repo, personaId) = createRepoWithUserPersona()
        val vm = PersonaEditorViewModel(repo, personaId)
        advanceUntilIdle()

        vm.setEnglishDescription("A thoughtful traveller, now well-rested.")
        vm.setChineseDescription("一位休整后的深思旅人。")
        assertTrue("edits should mark canSave", vm.uiState.value.canSave)
        vm.save()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isSaving)
        assertNull(state.saveError)
        assertNotNull("savedPersona should be populated on success", state.savedPersona)
        assertEquals("A thoughtful traveller, now well-rested.", state.savedPersona!!.description.english)
        assertFalse("post-save baseline matches current fields so canSave flips false", state.canSave)
        assertFalse(state.hasUnsavedChanges)

        val persisted = repo.observePersonas().first().first { it.id == personaId }
        assertEquals("A thoughtful traveller, now well-rested.", persisted.description.english)
    }

    @Test
    fun `cancel discards unsaved changes and restores loaded snapshot`() = runTest(dispatcher) {
        val (repo, personaId) = createRepoWithUserPersona()
        val vm = PersonaEditorViewModel(repo, personaId)
        advanceUntilIdle()

        vm.setEnglishDisplayName("Nova Rewritten")
        vm.setChineseDescription("被改动过的描述。")
        assertTrue(vm.uiState.value.hasUnsavedChanges)

        vm.cancel()

        val state = vm.uiState.value
        assertEquals("Nova", state.englishDisplayName)
        assertEquals("一位深思熟虑的旅人。", state.chineseDescription)
        assertFalse("snapshot restore means canSave is false", state.canSave)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun `unknown persona id surfaces saveError and disables save`() = runTest(dispatcher) {
        val repo = DefaultUserPersonaRepository(initialPersonas = listOf(builtInPersona()))
        val vm = PersonaEditorViewModel(repo, personaId = "persona-missing")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Persona not found", state.saveError)
        assertFalse(state.canSave)
    }

    @Test
    fun `built-in persona cannot be saved even when fields are valid`() = runTest(dispatcher) {
        val builtIn = builtInPersona()
        val repo = DefaultUserPersonaRepository(initialPersonas = listOf(builtIn))
        val vm = PersonaEditorViewModel(repo, personaId = builtIn.id)
        advanceUntilIdle()

        vm.setEnglishDescription("Changed built-in description.")
        assertTrue(vm.uiState.value.hasUnsavedChanges)
        assertFalse("built-in persona can never be saved from this editor", vm.uiState.value.canSave)
    }

    private fun createRepoWithUserPersona(): Pair<DefaultUserPersonaRepository, String> {
        val now = 2_000L
        val builtIn = builtInPersona().copy(isActive = true, createdAt = 1_000L, updatedAt = 1_000L)
        val userPersona = UserPersona(
            id = "persona-nova",
            displayName = LocalizedText("Nova", "新星"),
            description = LocalizedText("A thoughtful traveller.", "一位深思熟虑的旅人。"),
            isBuiltIn = false,
            isActive = false,
            createdAt = now,
            updatedAt = now,
        )
        val repo = DefaultUserPersonaRepository(
            initialPersonas = listOf(builtIn, userPersona),
            clock = { 9_999L },
        )
        return repo to userPersona.id
    }

    private fun builtInPersona(): UserPersona = UserPersona(
        id = "persona-builtin-default",
        displayName = LocalizedText("You", "你"),
        description = LocalizedText("Default persona.", "默认角色资料。"),
        isBuiltIn = true,
        isActive = false,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
