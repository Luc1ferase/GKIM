package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.UserPersona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

sealed interface UserPersonaMutationResult {
    data class Success(val persona: UserPersona) : UserPersonaMutationResult
    data class Rejected(val reason: RejectionReason) : UserPersonaMutationResult
    data class Failed(val cause: Throwable) : UserPersonaMutationResult

    enum class RejectionReason {
        UnknownPersona,
        BuiltInPersonaImmutable,
        ActivePersonaNotDeletable,
    }
}

interface UserPersonaRepository {
    fun observePersonas(): Flow<List<UserPersona>>
    fun observeActivePersona(): Flow<UserPersona?>
    suspend fun create(persona: UserPersona): UserPersonaMutationResult
    suspend fun update(persona: UserPersona): UserPersonaMutationResult
    suspend fun delete(personaId: String): UserPersonaMutationResult
    suspend fun activate(personaId: String): UserPersonaMutationResult
    suspend fun duplicate(personaId: String): UserPersonaMutationResult
    suspend fun refresh()
}

class DefaultUserPersonaRepository(
    initialPersonas: List<UserPersona>,
    private val idGenerator: () -> String = { "persona-${UUID.randomUUID()}" },
    private val clock: () -> Long = { System.currentTimeMillis() },
) : UserPersonaRepository {

    private val personasState = MutableStateFlow(enforceSingleActive(initialPersonas))

    val personas: StateFlow<List<UserPersona>> = personasState

    override fun observePersonas(): Flow<List<UserPersona>> = personasState

    override fun observeActivePersona(): Flow<UserPersona?> =
        personasState.map { list -> list.firstOrNull { it.isActive } }

    override suspend fun create(persona: UserPersona): UserPersonaMutationResult {
        val assignedId = if (persona.id.isBlank()) idGenerator() else persona.id
        val now = clock()
        val normalized = persona.copy(
            id = assignedId,
            isBuiltIn = false,
            isActive = false,
            createdAt = if (persona.createdAt == 0L) now else persona.createdAt,
            updatedAt = now,
        )
        personasState.value = personasState.value + normalized
        return UserPersonaMutationResult.Success(normalized)
    }

    override suspend fun update(persona: UserPersona): UserPersonaMutationResult {
        val existing = personasState.value.firstOrNull { it.id == persona.id }
            ?: return UserPersonaMutationResult.Rejected(
                UserPersonaMutationResult.RejectionReason.UnknownPersona,
            )
        val updated = persona.copy(
            isBuiltIn = existing.isBuiltIn,
            isActive = existing.isActive,
            createdAt = existing.createdAt,
            updatedAt = clock(),
        )
        personasState.value = personasState.value.map {
            if (it.id == existing.id) updated else it
        }
        return UserPersonaMutationResult.Success(updated)
    }

    override suspend fun delete(personaId: String): UserPersonaMutationResult {
        val existing = personasState.value.firstOrNull { it.id == personaId }
            ?: return UserPersonaMutationResult.Rejected(
                UserPersonaMutationResult.RejectionReason.UnknownPersona,
            )
        if (existing.isBuiltIn) {
            return UserPersonaMutationResult.Rejected(
                UserPersonaMutationResult.RejectionReason.BuiltInPersonaImmutable,
            )
        }
        if (existing.isActive) {
            return UserPersonaMutationResult.Rejected(
                UserPersonaMutationResult.RejectionReason.ActivePersonaNotDeletable,
            )
        }
        personasState.value = personasState.value.filterNot { it.id == personaId }
        return UserPersonaMutationResult.Success(existing)
    }

    override suspend fun activate(personaId: String): UserPersonaMutationResult {
        if (personasState.value.none { it.id == personaId }) {
            return UserPersonaMutationResult.Rejected(
                UserPersonaMutationResult.RejectionReason.UnknownPersona,
            )
        }
        val now = clock()
        val nextList = personasState.value.map {
            if (it.id == personaId) {
                it.copy(isActive = true, updatedAt = now)
            } else if (it.isActive) {
                it.copy(isActive = false, updatedAt = now)
            } else {
                it
            }
        }
        personasState.value = nextList
        val activated = nextList.first { it.id == personaId }
        return UserPersonaMutationResult.Success(activated)
    }

    override suspend fun duplicate(personaId: String): UserPersonaMutationResult {
        val source = personasState.value.firstOrNull { it.id == personaId }
            ?: return UserPersonaMutationResult.Rejected(
                UserPersonaMutationResult.RejectionReason.UnknownPersona,
            )
        val now = clock()
        val copy = source.copy(
            id = idGenerator(),
            displayName = LocalizedText(
                english = source.displayName.english + CopySuffix.English,
                chinese = source.displayName.chinese + CopySuffix.Chinese,
            ),
            isBuiltIn = false,
            isActive = false,
            createdAt = now,
            updatedAt = now,
        )
        personasState.value = personasState.value + copy
        return UserPersonaMutationResult.Success(copy)
    }

    override suspend fun refresh() {
        // No-op for in-memory repository; LiveUserPersonaRepository overrides to pull from backend.
    }

    fun setSnapshot(personas: List<UserPersona>) {
        personasState.value = enforceSingleActive(personas)
    }

    private fun enforceSingleActive(list: List<UserPersona>): List<UserPersona> {
        val firstActive = list.indexOfFirst { it.isActive }
        if (firstActive < 0) return list
        return list.mapIndexed { index, persona ->
            if (index == firstActive) persona
            else if (persona.isActive) persona.copy(isActive = false)
            else persona
        }
    }

    object CopySuffix {
        const val English: String = " (copy)"
        const val Chinese: String = "（副本）"
    }
}
