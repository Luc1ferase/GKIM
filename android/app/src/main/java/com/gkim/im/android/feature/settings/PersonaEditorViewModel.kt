package com.gkim.im.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.UserPersona
import com.gkim.im.android.core.model.UserPersonaValidation
import com.gkim.im.android.core.model.UserPersonaValidationError
import com.gkim.im.android.core.model.UserPersonaValidationResult
import com.gkim.im.android.data.repository.UserPersonaMutationResult
import com.gkim.im.android.data.repository.UserPersonaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PersonaEditorUiState(
    val personaId: String? = null,
    val isBuiltIn: Boolean = false,
    val isActive: Boolean = false,
    val englishDisplayName: String = "",
    val chineseDisplayName: String = "",
    val englishDescription: String = "",
    val chineseDescription: String = "",
    val validationErrors: List<UserPersonaValidationError> = emptyList(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedPersona: UserPersona? = null,
    val canSave: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
) {
    val hasError: Boolean get() = validationErrors.isNotEmpty()
}

class PersonaEditorViewModel(
    private val repository: UserPersonaRepository,
    private val personaId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonaEditorUiState())
    val uiState: StateFlow<PersonaEditorUiState> = _uiState.asStateFlow()

    private var initialSnapshot: PersonaEditorUiState? = null

    init {
        viewModelScope.launch { loadPersona() }
    }

    private suspend fun loadPersona() {
        val existing = repository.observePersonas().first().firstOrNull { it.id == personaId }
        if (existing == null) {
            _uiState.value = PersonaEditorUiState(
                personaId = personaId,
                saveError = "Persona not found",
            )
            return
        }
        val loaded = PersonaEditorUiState(
            personaId = existing.id,
            isBuiltIn = existing.isBuiltIn,
            isActive = existing.isActive,
            englishDisplayName = existing.displayName.english,
            chineseDisplayName = existing.displayName.chinese,
            englishDescription = existing.description.english,
            chineseDescription = existing.description.chinese,
        )
        initialSnapshot = loaded
        _uiState.value = recomputeDerived(loaded)
    }

    fun setEnglishDisplayName(value: String) {
        _uiState.value = recomputeDerived(_uiState.value.copy(englishDisplayName = value))
    }

    fun setChineseDisplayName(value: String) {
        _uiState.value = recomputeDerived(_uiState.value.copy(chineseDisplayName = value))
    }

    fun setEnglishDescription(value: String) {
        _uiState.value = recomputeDerived(_uiState.value.copy(englishDescription = value))
    }

    fun setChineseDescription(value: String) {
        _uiState.value = recomputeDerived(_uiState.value.copy(chineseDescription = value))
    }

    fun save() {
        val current = _uiState.value
        if (!current.canSave || current.isSaving) return
        val persona = current.toUserPersona() ?: return
        _uiState.value = current.copy(isSaving = true, saveError = null)
        viewModelScope.launch {
            val outcome = repository.update(persona)
            val next = when (outcome) {
                is UserPersonaMutationResult.Success -> current.copy(
                    isSaving = false,
                    saveError = null,
                    savedPersona = outcome.persona,
                    englishDisplayName = outcome.persona.displayName.english,
                    chineseDisplayName = outcome.persona.displayName.chinese,
                    englishDescription = outcome.persona.description.english,
                    chineseDescription = outcome.persona.description.chinese,
                )
                is UserPersonaMutationResult.Rejected -> current.copy(
                    isSaving = false,
                    saveError = when (outcome.reason) {
                        UserPersonaMutationResult.RejectionReason.UnknownPersona -> "Persona not found"
                        UserPersonaMutationResult.RejectionReason.BuiltInPersonaImmutable ->
                            "Built-in persona cannot be modified"
                        UserPersonaMutationResult.RejectionReason.ActivePersonaNotDeletable ->
                            "Active persona cannot be removed"
                    },
                )
                is UserPersonaMutationResult.Failed -> current.copy(
                    isSaving = false,
                    saveError = outcome.cause.message ?: "Save failed",
                )
            }
            if (outcome is UserPersonaMutationResult.Success) {
                initialSnapshot = next.copy(saveError = null, savedPersona = null)
            }
            _uiState.value = recomputeDerived(next)
        }
    }

    fun cancel() {
        val snapshot = initialSnapshot
        if (snapshot != null) {
            _uiState.value = recomputeDerived(snapshot.copy(saveError = null, savedPersona = null))
        }
    }

    fun clearSaveError() {
        _uiState.value = _uiState.value.copy(saveError = null)
    }

    private fun recomputeDerived(state: PersonaEditorUiState): PersonaEditorUiState {
        val persona = state.toUserPersona()
        val errors = if (persona != null) {
            when (val result = UserPersonaValidation.validate(persona)) {
                is UserPersonaValidationResult.Valid -> emptyList()
                is UserPersonaValidationResult.Invalid -> result.errors
            }
        } else {
            emptyList()
        }
        val hasChanges = initialSnapshot?.let { snapshot ->
            state.englishDisplayName != snapshot.englishDisplayName ||
                state.chineseDisplayName != snapshot.chineseDisplayName ||
                state.englishDescription != snapshot.englishDescription ||
                state.chineseDescription != snapshot.chineseDescription
        } ?: false
        return state.copy(
            validationErrors = errors,
            canSave = state.personaId != null && errors.isEmpty() && hasChanges && !state.isBuiltIn,
            hasUnsavedChanges = hasChanges,
        )
    }

    private fun PersonaEditorUiState.toUserPersona(): UserPersona? {
        val id = personaId ?: return null
        return UserPersona(
            id = id,
            displayName = LocalizedText(
                english = englishDisplayName,
                chinese = chineseDisplayName,
            ),
            description = LocalizedText(
                english = englishDescription,
                chinese = chineseDescription,
            ),
            isBuiltIn = isBuiltIn,
            isActive = isActive,
        )
    }
}

