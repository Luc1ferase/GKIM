package com.gkim.im.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Preset
import com.gkim.im.android.core.model.PresetParams
import com.gkim.im.android.core.model.PresetTemplate
import com.gkim.im.android.core.model.PresetValidation
import com.gkim.im.android.core.model.PresetValidationError
import com.gkim.im.android.core.model.PresetValidationResult
import com.gkim.im.android.data.repository.CompanionPresetMutationResult
import com.gkim.im.android.data.repository.CompanionPresetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PresetEditorUiState(
    val presetId: String? = null,
    val isBuiltIn: Boolean = false,
    val isActive: Boolean = false,
    val englishDisplayName: String = "",
    val chineseDisplayName: String = "",
    val englishDescription: String = "",
    val chineseDescription: String = "",
    val englishSystemPrefix: String = "",
    val chineseSystemPrefix: String = "",
    val englishSystemSuffix: String = "",
    val chineseSystemSuffix: String = "",
    val englishFormatInstructions: String = "",
    val chineseFormatInstructions: String = "",
    val englishPostHistoryInstructions: String = "",
    val chinesePostHistoryInstructions: String = "",
    val temperatureText: String = "",
    val topPText: String = "",
    val maxReplyTokensText: String = "",
    val validationErrors: List<PresetValidationError> = emptyList(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedPreset: Preset? = null,
    val canSave: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
) {
    val hasError: Boolean get() = validationErrors.isNotEmpty()
}

class PresetEditorViewModel(
    private val repository: CompanionPresetRepository,
    private val presetId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PresetEditorUiState())
    val uiState: StateFlow<PresetEditorUiState> = _uiState.asStateFlow()

    private var initialSnapshot: PresetEditorUiState? = null

    init {
        viewModelScope.launch { loadPreset() }
    }

    private suspend fun loadPreset() {
        val existing = repository.observePresets().first().firstOrNull { it.id == presetId }
        if (existing == null) {
            _uiState.value = PresetEditorUiState(
                presetId = presetId,
                saveError = "Preset not found",
            )
            return
        }
        val loaded = PresetEditorUiState(
            presetId = existing.id,
            isBuiltIn = existing.isBuiltIn,
            isActive = existing.isActive,
            englishDisplayName = existing.displayName.english,
            chineseDisplayName = existing.displayName.chinese,
            englishDescription = existing.description.english,
            chineseDescription = existing.description.chinese,
            englishSystemPrefix = existing.template.systemPrefix.english,
            chineseSystemPrefix = existing.template.systemPrefix.chinese,
            englishSystemSuffix = existing.template.systemSuffix.english,
            chineseSystemSuffix = existing.template.systemSuffix.chinese,
            englishFormatInstructions = existing.template.formatInstructions.english,
            chineseFormatInstructions = existing.template.formatInstructions.chinese,
            englishPostHistoryInstructions = existing.template.postHistoryInstructions.english,
            chinesePostHistoryInstructions = existing.template.postHistoryInstructions.chinese,
            temperatureText = existing.params.temperature?.toString().orEmpty(),
            topPText = existing.params.topP?.toString().orEmpty(),
            maxReplyTokensText = existing.params.maxReplyTokens?.toString().orEmpty(),
        )
        initialSnapshot = loaded
        _uiState.value = recomputeDerived(loaded)
    }

    fun setEnglishDisplayName(value: String) = update { it.copy(englishDisplayName = value) }
    fun setChineseDisplayName(value: String) = update { it.copy(chineseDisplayName = value) }
    fun setEnglishDescription(value: String) = update { it.copy(englishDescription = value) }
    fun setChineseDescription(value: String) = update { it.copy(chineseDescription = value) }
    fun setEnglishSystemPrefix(value: String) = update { it.copy(englishSystemPrefix = value) }
    fun setChineseSystemPrefix(value: String) = update { it.copy(chineseSystemPrefix = value) }
    fun setEnglishSystemSuffix(value: String) = update { it.copy(englishSystemSuffix = value) }
    fun setChineseSystemSuffix(value: String) = update { it.copy(chineseSystemSuffix = value) }
    fun setEnglishFormatInstructions(value: String) = update { it.copy(englishFormatInstructions = value) }
    fun setChineseFormatInstructions(value: String) = update { it.copy(chineseFormatInstructions = value) }
    fun setEnglishPostHistoryInstructions(value: String) = update { it.copy(englishPostHistoryInstructions = value) }
    fun setChinesePostHistoryInstructions(value: String) = update { it.copy(chinesePostHistoryInstructions = value) }
    fun setTemperatureText(value: String) = update { it.copy(temperatureText = value) }
    fun setTopPText(value: String) = update { it.copy(topPText = value) }
    fun setMaxReplyTokensText(value: String) = update { it.copy(maxReplyTokensText = value) }

    fun save() {
        val current = _uiState.value
        if (!current.canSave || current.isSaving) return
        val preset = current.toPreset() ?: return
        _uiState.value = current.copy(isSaving = true, saveError = null)
        viewModelScope.launch {
            val outcome = repository.update(preset)
            val next = when (outcome) {
                is CompanionPresetMutationResult.Success -> current.copy(
                    isSaving = false,
                    saveError = null,
                    savedPreset = outcome.preset,
                ).withPersistedPreset(outcome.preset)
                is CompanionPresetMutationResult.Rejected -> current.copy(
                    isSaving = false,
                    saveError = when (outcome.reason) {
                        CompanionPresetMutationResult.RejectionReason.UnknownPreset -> "Preset not found"
                        CompanionPresetMutationResult.RejectionReason.BuiltInPresetImmutable ->
                            "Built-in preset cannot be modified"
                        CompanionPresetMutationResult.RejectionReason.ActivePresetNotDeletable ->
                            "Active preset cannot be deleted"
                    },
                )
                is CompanionPresetMutationResult.Failed -> current.copy(
                    isSaving = false,
                    saveError = outcome.cause.message ?: "Save failed",
                )
            }
            if (outcome is CompanionPresetMutationResult.Success) {
                initialSnapshot = next.copy(saveError = null, savedPreset = null)
            }
            _uiState.value = recomputeDerived(next)
        }
    }

    fun cancel() {
        val snapshot = initialSnapshot
        if (snapshot != null) {
            _uiState.value = recomputeDerived(snapshot.copy(saveError = null, savedPreset = null))
        }
    }

    fun clearSaveError() {
        _uiState.value = _uiState.value.copy(saveError = null)
    }

    private fun update(transform: (PresetEditorUiState) -> PresetEditorUiState) {
        _uiState.value = recomputeDerived(transform(_uiState.value))
    }

    private fun recomputeDerived(state: PresetEditorUiState): PresetEditorUiState {
        val preset = state.toPreset()
        val errors = if (preset != null) {
            when (val result = PresetValidation.validate(preset)) {
                is PresetValidationResult.Valid -> emptyList()
                is PresetValidationResult.Invalid -> result.errors
            }
        } else {
            emptyList()
        }
        val parseErrors = buildList {
            if (state.temperatureText.isNotBlank() && state.temperatureText.toDoubleOrNull() == null) {
                add(PresetValidationError.TemperatureOutOfRange)
            }
            if (state.topPText.isNotBlank() && state.topPText.toDoubleOrNull() == null) {
                add(PresetValidationError.TopPOutOfRange)
            }
            if (state.maxReplyTokensText.isNotBlank() && state.maxReplyTokensText.toIntOrNull() == null) {
                add(PresetValidationError.MaxReplyTokensOutOfRange)
            }
        }
        val mergedErrors = (errors + parseErrors).distinct()
        val hasChanges = initialSnapshot?.let { snapshot -> state.hasDifferencesFrom(snapshot) } ?: false
        return state.copy(
            validationErrors = mergedErrors,
            canSave = state.presetId != null && mergedErrors.isEmpty() && hasChanges && !state.isBuiltIn,
            hasUnsavedChanges = hasChanges,
        )
    }

    private fun PresetEditorUiState.toPreset(): Preset? {
        val id = presetId ?: return null
        return Preset(
            id = id,
            displayName = LocalizedText(englishDisplayName, chineseDisplayName),
            description = LocalizedText(englishDescription, chineseDescription),
            template = PresetTemplate(
                systemPrefix = LocalizedText(englishSystemPrefix, chineseSystemPrefix),
                systemSuffix = LocalizedText(englishSystemSuffix, chineseSystemSuffix),
                formatInstructions = LocalizedText(englishFormatInstructions, chineseFormatInstructions),
                postHistoryInstructions = LocalizedText(englishPostHistoryInstructions, chinesePostHistoryInstructions),
            ),
            params = PresetParams(
                temperature = temperatureText.takeIf { it.isNotBlank() }?.toDoubleOrNull(),
                topP = topPText.takeIf { it.isNotBlank() }?.toDoubleOrNull(),
                maxReplyTokens = maxReplyTokensText.takeIf { it.isNotBlank() }?.toIntOrNull(),
            ),
            isBuiltIn = isBuiltIn,
            isActive = isActive,
        )
    }

    private fun PresetEditorUiState.hasDifferencesFrom(snapshot: PresetEditorUiState): Boolean {
        return englishDisplayName != snapshot.englishDisplayName ||
            chineseDisplayName != snapshot.chineseDisplayName ||
            englishDescription != snapshot.englishDescription ||
            chineseDescription != snapshot.chineseDescription ||
            englishSystemPrefix != snapshot.englishSystemPrefix ||
            chineseSystemPrefix != snapshot.chineseSystemPrefix ||
            englishSystemSuffix != snapshot.englishSystemSuffix ||
            chineseSystemSuffix != snapshot.chineseSystemSuffix ||
            englishFormatInstructions != snapshot.englishFormatInstructions ||
            chineseFormatInstructions != snapshot.chineseFormatInstructions ||
            englishPostHistoryInstructions != snapshot.englishPostHistoryInstructions ||
            chinesePostHistoryInstructions != snapshot.chinesePostHistoryInstructions ||
            temperatureText != snapshot.temperatureText ||
            topPText != snapshot.topPText ||
            maxReplyTokensText != snapshot.maxReplyTokensText
    }

    private fun PresetEditorUiState.withPersistedPreset(preset: Preset): PresetEditorUiState = copy(
        englishDisplayName = preset.displayName.english,
        chineseDisplayName = preset.displayName.chinese,
        englishDescription = preset.description.english,
        chineseDescription = preset.description.chinese,
        englishSystemPrefix = preset.template.systemPrefix.english,
        chineseSystemPrefix = preset.template.systemPrefix.chinese,
        englishSystemSuffix = preset.template.systemSuffix.english,
        chineseSystemSuffix = preset.template.systemSuffix.chinese,
        englishFormatInstructions = preset.template.formatInstructions.english,
        chineseFormatInstructions = preset.template.formatInstructions.chinese,
        englishPostHistoryInstructions = preset.template.postHistoryInstructions.english,
        chinesePostHistoryInstructions = preset.template.postHistoryInstructions.chinese,
        temperatureText = preset.params.temperature?.toString().orEmpty(),
        topPText = preset.params.topP?.toString().orEmpty(),
        maxReplyTokensText = preset.params.maxReplyTokens?.toString().orEmpty(),
    )
}
