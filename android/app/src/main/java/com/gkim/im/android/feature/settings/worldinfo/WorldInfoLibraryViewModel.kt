package com.gkim.im.android.feature.settings.worldinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Lorebook
import com.gkim.im.android.core.model.LorebookBinding
import com.gkim.im.android.core.model.LorebookEntry
import com.gkim.im.android.core.model.resolve
import com.gkim.im.android.data.repository.WorldInfoMutationResult
import com.gkim.im.android.data.repository.WorldInfoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorldInfoLibraryRow(
    val lorebookId: String,
    val displayName: String,
    val entryCount: Int,
    val isGlobal: Boolean,
    val isBuiltIn: Boolean,
    val boundCharacterCount: Int,
) {
    val hasBindings: Boolean get() = boundCharacterCount > 0
    val canDelete: Boolean get() = !isBuiltIn && !hasBindings
    val canDuplicate: Boolean get() = true
    val canToggleGlobal: Boolean get() = !isBuiltIn
}

data class WorldInfoLibraryUiState(
    val rows: List<WorldInfoLibraryRow> = emptyList(),
    val pendingOperation: PendingOperation? = null,
    val errorMessage: String? = null,
) {
    data class PendingOperation(val lorebookId: String, val kind: Kind) {
        enum class Kind { Create, Duplicate, Delete, ToggleGlobal }
    }
}

class WorldInfoLibraryViewModel(
    private val repository: WorldInfoRepository,
    private val language: () -> AppLanguage,
) : ViewModel() {

    private val pendingOperationState =
        MutableStateFlow<WorldInfoLibraryUiState.PendingOperation?>(null)
    private val errorMessageState = MutableStateFlow<String?>(null)
    private val lorebookSnapshot = MutableStateFlow<List<Lorebook>>(emptyList())

    val uiState: StateFlow<WorldInfoLibraryUiState> = combine(
        repository.observeLorebooks(),
        repository.observeEntries(),
        repository.observeBindings(),
        pendingOperationState,
        errorMessageState,
    ) { lorebooks, entries, bindings, pendingOperation, errorMessage ->
        lorebookSnapshot.value = lorebooks
        WorldInfoLibraryUiState(
            rows = sortForDisplay(lorebooks).map { lorebook ->
                lorebook.toRow(
                    entries = entries[lorebook.id].orEmpty(),
                    bindings = bindings[lorebook.id].orEmpty(),
                    language = language(),
                )
            },
            pendingOperation = pendingOperation,
            errorMessage = errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = WorldInfoLibraryUiState(),
    )

    private val _refreshCompletions = MutableStateFlow(0)
    val refreshCompletions: StateFlow<Int> = _refreshCompletions.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.refresh() }
            _refreshCompletions.value += 1
        }
    }

    fun createLorebook(ownerId: String) {
        pendingOperationState.value = WorldInfoLibraryUiState.PendingOperation(
            lorebookId = "",
            kind = WorldInfoLibraryUiState.PendingOperation.Kind.Create,
        )
        viewModelScope.launch {
            val draft = Lorebook(
                id = "",
                ownerId = ownerId,
                displayName = LocalizedText(english = "New lorebook", chinese = "新世界书"),
            )
            val result = repository.createLorebook(draft)
            handleMutationOutcome(result)
        }
    }

    fun duplicate(lorebookId: String) {
        pendingOperationState.value = WorldInfoLibraryUiState.PendingOperation(
            lorebookId = lorebookId,
            kind = WorldInfoLibraryUiState.PendingOperation.Kind.Duplicate,
        )
        viewModelScope.launch {
            val result = repository.duplicateLorebook(lorebookId)
            handleMutationOutcome(result)
        }
    }

    fun delete(lorebookId: String) {
        pendingOperationState.value = WorldInfoLibraryUiState.PendingOperation(
            lorebookId = lorebookId,
            kind = WorldInfoLibraryUiState.PendingOperation.Kind.Delete,
        )
        viewModelScope.launch {
            val result = repository.deleteLorebook(lorebookId)
            handleMutationOutcome(result)
        }
    }

    fun toggleGlobal(lorebookId: String) {
        val current = uiState.value.rows.firstOrNull { it.lorebookId == lorebookId }
        if (current == null || !current.canToggleGlobal) return

        pendingOperationState.value = WorldInfoLibraryUiState.PendingOperation(
            lorebookId = lorebookId,
            kind = WorldInfoLibraryUiState.PendingOperation.Kind.ToggleGlobal,
        )
        viewModelScope.launch {
            val existing = lorebookSnapshot.value.firstOrNull { it.id == lorebookId }
            if (existing == null) {
                handleMutationOutcome(
                    WorldInfoMutationResult.Rejected(
                        WorldInfoMutationResult.RejectionReason.UnknownLorebook,
                    ),
                )
                return@launch
            }
            val result = repository.updateLorebook(existing.copy(isGlobal = !existing.isGlobal))
            handleMutationOutcome(result)
        }
    }

    fun clearError() {
        errorMessageState.value = null
    }

    private fun handleMutationOutcome(result: WorldInfoMutationResult<*>) {
        pendingOperationState.value = null
        errorMessageState.value = when (result) {
            is WorldInfoMutationResult.Success -> null
            is WorldInfoMutationResult.Rejected -> when (result.reason) {
                WorldInfoMutationResult.RejectionReason.UnknownLorebook -> "Lorebook not found"
                WorldInfoMutationResult.RejectionReason.UnknownEntry -> "Entry not found"
                WorldInfoMutationResult.RejectionReason.UnknownBinding -> "Binding not found"
                WorldInfoMutationResult.RejectionReason.BuiltInLorebookImmutable ->
                    "Built-in lorebook cannot be modified"
                WorldInfoMutationResult.RejectionReason.LorebookHasBindings ->
                    "Lorebook still bound to characters"
                WorldInfoMutationResult.RejectionReason.BindingAlreadyExists ->
                    "Binding already exists"
            }
            is WorldInfoMutationResult.Failed -> result.cause.message ?: "Operation failed"
        }
    }

    private fun sortForDisplay(lorebooks: List<Lorebook>): List<Lorebook> {
        val builtIns = lorebooks.filter { it.isBuiltIn }.sortedBy { it.createdAt }
        val userOwned = lorebooks.filterNot { it.isBuiltIn }.sortedBy { it.createdAt }
        return builtIns + userOwned
    }

    private fun Lorebook.toRow(
        entries: List<LorebookEntry>,
        bindings: List<LorebookBinding>,
        language: AppLanguage,
    ): WorldInfoLibraryRow {
        val resolved = resolve(language)
        return WorldInfoLibraryRow(
            lorebookId = id,
            displayName = resolved.displayName.ifBlank {
                when (language) {
                    AppLanguage.English -> "Untitled lorebook"
                    AppLanguage.Chinese -> "未命名世界书"
                }
            },
            entryCount = entries.size,
            isGlobal = isGlobal,
            isBuiltIn = isBuiltIn,
            boundCharacterCount = bindings.map { it.characterId }.distinct().size,
        )
    }
}
