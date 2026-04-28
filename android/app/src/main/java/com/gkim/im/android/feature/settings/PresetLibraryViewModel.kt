package com.gkim.im.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.Preset
import com.gkim.im.android.core.model.ResolvedPreset
import com.gkim.im.android.core.model.resolve
import com.gkim.im.android.data.repository.CompanionPresetMutationResult
import com.gkim.im.android.data.repository.CompanionPresetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PresetListItem(
    val preset: Preset,
    val resolved: ResolvedPreset,
    val isActive: Boolean,
    val canDelete: Boolean,
    val canActivate: Boolean,
    val canEdit: Boolean,
    val canDuplicate: Boolean,
)

data class PresetLibraryUiState(
    val items: List<PresetListItem> = emptyList(),
    val pendingOperation: PendingOperation? = null,
    val errorMessage: String? = null,
) {
    data class PendingOperation(val presetId: String, val kind: Kind) {
        enum class Kind { Activate, Delete, Duplicate }
    }
}

class PresetLibraryViewModel(
    private val repository: CompanionPresetRepository,
    private val language: () -> AppLanguage,
) : ViewModel() {

    private val pendingOperationState = MutableStateFlow<PresetLibraryUiState.PendingOperation?>(null)
    private val errorMessageState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PresetLibraryUiState> = combine(
        repository.observePresets(),
        pendingOperationState,
        errorMessageState,
    ) { presets, pendingOperation, errorMessage ->
        PresetLibraryUiState(
            items = sortForDisplay(presets).map { it.toListItem(language()) },
            pendingOperation = pendingOperation,
            errorMessage = errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PresetLibraryUiState(),
    )

    private val _refreshCompletions = MutableStateFlow(0)
    val refreshCompletions: StateFlow<Int> = _refreshCompletions.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.refresh() }
            _refreshCompletions.value += 1
        }
    }

    fun activate(presetId: String) {
        pendingOperationState.value = PresetLibraryUiState.PendingOperation(
            presetId = presetId,
            kind = PresetLibraryUiState.PendingOperation.Kind.Activate,
        )
        viewModelScope.launch {
            val result = repository.activate(presetId)
            handleMutationOutcome(result)
        }
    }

    fun delete(presetId: String) {
        pendingOperationState.value = PresetLibraryUiState.PendingOperation(
            presetId = presetId,
            kind = PresetLibraryUiState.PendingOperation.Kind.Delete,
        )
        viewModelScope.launch {
            val result = repository.delete(presetId)
            handleMutationOutcome(result)
        }
    }

    fun duplicate(presetId: String) {
        pendingOperationState.value = PresetLibraryUiState.PendingOperation(
            presetId = presetId,
            kind = PresetLibraryUiState.PendingOperation.Kind.Duplicate,
        )
        viewModelScope.launch {
            val result = repository.duplicate(presetId)
            handleMutationOutcome(result)
        }
    }

    fun clearError() {
        errorMessageState.value = null
    }

    private fun handleMutationOutcome(result: CompanionPresetMutationResult) {
        pendingOperationState.value = null
        errorMessageState.value = when (result) {
            is CompanionPresetMutationResult.Success -> null
            is CompanionPresetMutationResult.Rejected -> when (result.reason) {
                CompanionPresetMutationResult.RejectionReason.UnknownPreset -> "Preset not found"
                CompanionPresetMutationResult.RejectionReason.BuiltInPresetImmutable ->
                    "Built-in preset cannot be modified"
                CompanionPresetMutationResult.RejectionReason.ActivePresetNotDeletable ->
                    "Active preset cannot be deleted"
            }
            is CompanionPresetMutationResult.Failed -> result.cause.message ?: "Operation failed"
        }
    }

    private fun sortForDisplay(presets: List<Preset>): List<Preset> {
        val builtIns = presets.filter { it.isBuiltIn }.sortedBy { it.createdAt }
        val userOwned = presets.filterNot { it.isBuiltIn }.sortedBy { it.createdAt }
        return builtIns + userOwned
    }

    private fun Preset.toListItem(language: AppLanguage): PresetListItem =
        PresetListItem(
            preset = this,
            resolved = resolve(language),
            isActive = isActive,
            canDelete = !isBuiltIn && !isActive,
            canActivate = !isActive,
            canEdit = !isBuiltIn,
            canDuplicate = true,
        )
}
