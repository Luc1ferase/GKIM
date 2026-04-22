package com.gkim.im.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.ResolvedUserPersona
import com.gkim.im.android.core.model.UserPersona
import com.gkim.im.android.core.model.resolve
import com.gkim.im.android.data.repository.UserPersonaMutationResult
import com.gkim.im.android.data.repository.UserPersonaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonaListItem(
    val persona: UserPersona,
    val resolved: ResolvedUserPersona,
    val isActive: Boolean,
    val canDelete: Boolean,
    val canActivate: Boolean,
    val canEdit: Boolean,
    val canDuplicate: Boolean,
)

data class PersonaLibraryUiState(
    val items: List<PersonaListItem> = emptyList(),
    val pendingOperation: PendingOperation? = null,
    val errorMessage: String? = null,
) {
    data class PendingOperation(val personaId: String, val kind: Kind) {
        enum class Kind { Activate, Delete, Duplicate }
    }
}

class PersonaLibraryViewModel(
    private val repository: UserPersonaRepository,
    private val language: () -> AppLanguage,
) : ViewModel() {

    private val pendingOperationState = MutableStateFlow<PersonaLibraryUiState.PendingOperation?>(null)
    private val errorMessageState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PersonaLibraryUiState> = combine(
        repository.observePersonas(),
        pendingOperationState,
        errorMessageState,
    ) { personas, pendingOperation, errorMessage ->
        PersonaLibraryUiState(
            items = sortForDisplay(personas).map { it.toListItem(language()) },
            pendingOperation = pendingOperation,
            errorMessage = errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PersonaLibraryUiState(),
    )

    private val _refreshCompletions = MutableStateFlow(0)
    val refreshCompletions: StateFlow<Int> = _refreshCompletions.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.refresh() }
            _refreshCompletions.value += 1
        }
    }

    fun activate(personaId: String) {
        pendingOperationState.value = PersonaLibraryUiState.PendingOperation(
            personaId = personaId,
            kind = PersonaLibraryUiState.PendingOperation.Kind.Activate,
        )
        viewModelScope.launch {
            val result = repository.activate(personaId)
            handleMutationOutcome(result)
        }
    }

    fun delete(personaId: String) {
        pendingOperationState.value = PersonaLibraryUiState.PendingOperation(
            personaId = personaId,
            kind = PersonaLibraryUiState.PendingOperation.Kind.Delete,
        )
        viewModelScope.launch {
            val result = repository.delete(personaId)
            handleMutationOutcome(result)
        }
    }

    fun duplicate(personaId: String) {
        pendingOperationState.value = PersonaLibraryUiState.PendingOperation(
            personaId = personaId,
            kind = PersonaLibraryUiState.PendingOperation.Kind.Duplicate,
        )
        viewModelScope.launch {
            val result = repository.duplicate(personaId)
            handleMutationOutcome(result)
        }
    }

    fun clearError() {
        errorMessageState.value = null
    }

    private fun handleMutationOutcome(result: UserPersonaMutationResult) {
        pendingOperationState.value = null
        errorMessageState.value = when (result) {
            is UserPersonaMutationResult.Success -> null
            is UserPersonaMutationResult.Rejected -> when (result.reason) {
                UserPersonaMutationResult.RejectionReason.UnknownPersona -> "Persona not found"
                UserPersonaMutationResult.RejectionReason.BuiltInPersonaImmutable ->
                    "Built-in persona cannot be deleted"
                UserPersonaMutationResult.RejectionReason.ActivePersonaNotDeletable ->
                    "Active persona cannot be deleted"
            }
            is UserPersonaMutationResult.Failed -> result.cause.message ?: "Operation failed"
        }
    }

    private fun sortForDisplay(personas: List<UserPersona>): List<UserPersona> {
        val builtIns = personas.filter { it.isBuiltIn }.sortedBy { it.createdAt }
        val userOwned = personas.filterNot { it.isBuiltIn }.sortedBy { it.createdAt }
        return builtIns + userOwned
    }

    private fun UserPersona.toListItem(language: AppLanguage): PersonaListItem =
        PersonaListItem(
            persona = this,
            resolved = resolve(language),
            isActive = isActive,
            canDelete = !isBuiltIn && !isActive,
            canActivate = !isActive,
            canEdit = true,
            canDuplicate = true,
        )
}
