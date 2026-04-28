package com.gkim.im.android.feature.settings

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.UserPersona
import com.gkim.im.android.data.repository.DefaultUserPersonaRepository
import com.gkim.im.android.data.repository.UserPersonaMutationResult
import com.gkim.im.android.data.repository.UserPersonaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
class PersonaListPresentationTest {

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
    fun `uiState lists built-in personas first then user personas each ordered by createdAt`() = runTest(dispatcher) {
        val personas = listOf(
            samplePersona(id = "user-b", createdAt = 4_000L, isBuiltIn = false),
            samplePersona(id = "built-a", createdAt = 1_000L, isBuiltIn = true, isActive = true),
            samplePersona(id = "user-a", createdAt = 3_000L, isBuiltIn = false),
            samplePersona(id = "built-b", createdAt = 2_000L, isBuiltIn = true),
        )
        val repo = DefaultUserPersonaRepository(initialPersonas = personas)
        val viewModel = PersonaLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        val ids = viewModel.uiState.value.items.map { it.persona.id }
        assertEquals(listOf("built-a", "built-b", "user-a", "user-b"), ids)
    }

    @Test
    fun `active persona flag is carried into PersonaListItem and surfaces as the active badge`() = runTest(dispatcher) {
        val personas = listOf(
            samplePersona(id = "built-a", createdAt = 1_000L, isBuiltIn = true, isActive = true),
            samplePersona(id = "user-a", createdAt = 2_000L, isBuiltIn = false),
        )
        val repo = DefaultUserPersonaRepository(initialPersonas = personas)
        val viewModel = PersonaLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        val activeItems = viewModel.uiState.value.items.filter { it.isActive }
        assertEquals(listOf("built-a"), activeItems.map { it.persona.id })
        val inactive = viewModel.uiState.value.items.first { it.persona.id == "user-a" }
        assertFalse(inactive.isActive)
    }

    @Test
    fun `built-in personas cannot be deleted even when inactive`() = runTest(dispatcher) {
        val personas = listOf(
            samplePersona(id = "built-a", createdAt = 1_000L, isBuiltIn = true, isActive = true),
            samplePersona(id = "built-b", createdAt = 2_000L, isBuiltIn = true, isActive = false),
            samplePersona(id = "user-a", createdAt = 3_000L, isBuiltIn = false, isActive = false),
        )
        val repo = DefaultUserPersonaRepository(initialPersonas = personas)
        val viewModel = PersonaLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        val byId = viewModel.uiState.value.items.associateBy { it.persona.id }
        assertFalse("built-a active built-in should not be deletable", byId.getValue("built-a").canDelete)
        assertFalse("built-b inactive built-in should still not be deletable", byId.getValue("built-b").canDelete)
        assertTrue("user-a inactive user persona should be deletable", byId.getValue("user-a").canDelete)
    }

    @Test
    fun `active persona cannot be deleted or activated but can still be edited and duplicated`() = runTest(dispatcher) {
        val personas = listOf(
            samplePersona(id = "built-a", createdAt = 1_000L, isBuiltIn = true, isActive = false),
            samplePersona(id = "user-a", createdAt = 2_000L, isBuiltIn = false, isActive = true),
        )
        val repo = DefaultUserPersonaRepository(initialPersonas = personas)
        val viewModel = PersonaLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        val active = viewModel.uiState.value.items.first { it.isActive }
        assertEquals("user-a", active.persona.id)
        assertFalse("active persona cannot be deleted", active.canDelete)
        assertFalse("active persona cannot be activated again", active.canActivate)
        assertTrue("active persona can be edited", active.canEdit)
        assertTrue("active persona can be duplicated", active.canDuplicate)

        val inactiveBuiltIn = viewModel.uiState.value.items.first { it.persona.id == "built-a" }
        assertTrue("inactive built-in can be activated", inactiveBuiltIn.canActivate)
        assertFalse("inactive built-in still cannot be deleted", inactiveBuiltIn.canDelete)
    }

    @Test
    fun `activate switches the active persona and clears pendingOperation on success`() = runTest(dispatcher) {
        val personas = listOf(
            samplePersona(id = "built-a", createdAt = 1_000L, isBuiltIn = true, isActive = true),
            samplePersona(id = "user-a", createdAt = 2_000L, isBuiltIn = false),
        )
        val repo = DefaultUserPersonaRepository(initialPersonas = personas)
        val viewModel = PersonaLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.activate("user-a")
        advanceUntilIdle()

        val activeIds = viewModel.uiState.value.items.filter { it.isActive }.map { it.persona.id }
        assertEquals(listOf("user-a"), activeIds)
        assertNull("pendingOperation should clear after success", viewModel.uiState.value.pendingOperation)
        assertNull("no error expected", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `resolved persona fields honour the language provider`() = runTest(dispatcher) {
        val personas = listOf(
            UserPersona(
                id = "built-a",
                displayName = LocalizedText("You", "你"),
                description = LocalizedText("A curious traveller.", "一位好奇的旅人。"),
                isBuiltIn = true,
                isActive = true,
                createdAt = 1_000L,
                updatedAt = 1_000L,
            ),
        )
        val englishRepo = DefaultUserPersonaRepository(initialPersonas = personas)
        val englishVm = PersonaLibraryViewModel(englishRepo, language = { AppLanguage.English })
        advanceUntilIdle()
        val englishItem = englishVm.uiState.value.items.first()
        assertEquals("You", englishItem.resolved.displayName)
        assertEquals("A curious traveller.", englishItem.resolved.description)

        val chineseRepo = DefaultUserPersonaRepository(initialPersonas = personas)
        val chineseVm = PersonaLibraryViewModel(chineseRepo, language = { AppLanguage.Chinese })
        advanceUntilIdle()
        val chineseItem = chineseVm.uiState.value.items.first()
        assertEquals("你", chineseItem.resolved.displayName)
        assertEquals("一位好奇的旅人。", chineseItem.resolved.description)
    }

    @Test
    fun `delete on active persona surfaces rejection errorMessage`() = runTest(dispatcher) {
        val personas = listOf(
            samplePersona(id = "user-a", createdAt = 1_000L, isBuiltIn = false, isActive = true),
        )
        val repo = DefaultUserPersonaRepository(initialPersonas = personas)
        val viewModel = PersonaLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        viewModel.delete("user-a")
        advanceUntilIdle()

        assertEquals("Active persona cannot be deleted", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.pendingOperation)
        // Persona remains.
        assertEquals(
            listOf("user-a"),
            viewModel.uiState.value.items.map { it.persona.id },
        )
    }

    @Test
    fun `failed mutation surfaces cause message and keeps list consistent`() = runTest(dispatcher) {
        val personas = listOf(
            samplePersona(id = "built-a", createdAt = 1_000L, isBuiltIn = true, isActive = true),
            samplePersona(id = "user-a", createdAt = 2_000L, isBuiltIn = false),
        )
        val failing = FailingActivateRepository(personas, IllegalStateException("server_busy"))
        val viewModel = PersonaLibraryViewModel(failing, language = { AppLanguage.English })
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
            initialPersonas = listOf(samplePersona(id = "built-a", createdAt = 1_000L, isBuiltIn = true, isActive = true)),
        )
        val viewModel = PersonaLibraryViewModel(repo, language = { AppLanguage.English })
        advanceUntilIdle()

        assertEquals(1, repo.refreshCalls)
        assertEquals(1, viewModel.refreshCompletions.value)
    }

    private fun samplePersona(
        id: String,
        createdAt: Long,
        isBuiltIn: Boolean,
        isActive: Boolean = false,
    ): UserPersona = UserPersona(
        id = id,
        displayName = LocalizedText("Display-$id", "显示-$id"),
        description = LocalizedText("Description-$id", "描述-$id"),
        isBuiltIn = isBuiltIn,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private class FailingActivateRepository(
        initialPersonas: List<UserPersona>,
        private val cause: Throwable,
    ) : UserPersonaRepository {
        private val state = MutableStateFlow(initialPersonas)
        override fun observePersonas(): Flow<List<UserPersona>> = state
        override fun observeActivePersona(): Flow<UserPersona?> =
            state.map { list -> list.firstOrNull { it.isActive } }
        override suspend fun create(persona: UserPersona): UserPersonaMutationResult =
            UserPersonaMutationResult.Success(persona)
        override suspend fun update(persona: UserPersona): UserPersonaMutationResult =
            UserPersonaMutationResult.Success(persona)
        override suspend fun delete(personaId: String): UserPersonaMutationResult =
            UserPersonaMutationResult.Failed(cause)
        override suspend fun activate(personaId: String): UserPersonaMutationResult =
            UserPersonaMutationResult.Failed(cause)
        override suspend fun duplicate(personaId: String): UserPersonaMutationResult =
            UserPersonaMutationResult.Failed(cause)
        override suspend fun refresh() { /* no-op */ }
    }

    private class CountingRefreshRepository(
        initialPersonas: List<UserPersona>,
    ) : UserPersonaRepository {
        private val delegate = DefaultUserPersonaRepository(initialPersonas = initialPersonas)
        var refreshCalls: Int = 0

        override fun observePersonas(): Flow<List<UserPersona>> = delegate.observePersonas()
        override fun observeActivePersona(): Flow<UserPersona?> = delegate.observeActivePersona()
        override suspend fun create(persona: UserPersona): UserPersonaMutationResult = delegate.create(persona)
        override suspend fun update(persona: UserPersona): UserPersonaMutationResult = delegate.update(persona)
        override suspend fun delete(personaId: String): UserPersonaMutationResult = delegate.delete(personaId)
        override suspend fun activate(personaId: String): UserPersonaMutationResult = delegate.activate(personaId)
        override suspend fun duplicate(personaId: String): UserPersonaMutationResult = delegate.duplicate(personaId)
        override suspend fun refresh() {
            refreshCalls += 1
        }
    }

    @Suppress("unused")
    private val keepAliveJob: Job = Job()
}
