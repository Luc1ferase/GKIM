package com.gkim.im.android.feature.settings.worldinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Lorebook
import com.gkim.im.android.core.model.LorebookBinding
import com.gkim.im.android.core.model.LorebookEntry
import com.gkim.im.android.core.model.resolve
import com.gkim.im.android.data.repository.CompanionRosterRepository
import com.gkim.im.android.data.repository.WorldInfoMutationResult
import com.gkim.im.android.data.repository.WorldInfoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorldInfoEditorHeader(
    val lorebookId: String,
    val ownerId: String,
    val englishName: String,
    val chineseName: String,
    val englishDescription: String,
    val chineseDescription: String,
    val tokenBudget: Int,
    val isGlobal: Boolean,
    val isBuiltIn: Boolean,
)

data class WorldInfoEditorEntryRow(
    val entryId: String,
    val displayName: String,
    val enabled: Boolean,
    val insertionOrder: Int,
    val position: Int,
    val total: Int,
) {
    val canMoveUp: Boolean get() = position > 0
    val canMoveDown: Boolean get() = position < total - 1
}

data class WorldInfoEditorBindingRow(
    val characterId: String,
    val displayName: String,
    val isPrimary: Boolean,
)

data class WorldInfoEditorPickerItem(
    val characterId: String,
    val displayName: String,
)

data class WorldInfoEditorUiState(
    val header: WorldInfoEditorHeader? = null,
    val entries: List<WorldInfoEditorEntryRow> = emptyList(),
    val bindings: List<WorldInfoEditorBindingRow> = emptyList(),
    val bindablePickerItems: List<WorldInfoEditorPickerItem> = emptyList(),
    val pendingOperation: PendingOperation? = null,
    val errorMessage: String? = null,
) {
    data class PendingOperation(val targetId: String, val kind: Kind) {
        enum class Kind { SaveHeader, AddEntry, MoveEntry, ToggleEntry, DeleteEntry, Bind, Unbind, TogglePrimary }
    }
}

class WorldInfoEditorViewModel(
    private val repository: WorldInfoRepository,
    private val rosterRepository: CompanionRosterRepository,
    private val lorebookId: String,
    private val language: () -> AppLanguage,
) : ViewModel() {

    private val pendingOperationState = MutableStateFlow<WorldInfoEditorUiState.PendingOperation?>(null)
    private val errorMessageState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<WorldInfoEditorUiState> = combine(
        repository.observeLorebooks(),
        repository.observeEntries(),
        repository.observeBindings(),
        rosterRepository.presetCharacters,
        combine(pendingOperationState, errorMessageState, rosterRepository.userCharacters) { pending, error, user ->
            Triple(pending, error, user)
        },
    ) { lorebooks, entriesByLorebook, bindingsByLorebook, presetCharacters, pendingTriple ->
        val (pendingOperation, errorMessage, userCharacters) = pendingTriple
        val lorebook = lorebooks.firstOrNull { it.id == lorebookId }
        val appLanguage = language()
        val entries = entriesByLorebook[lorebookId].orEmpty()
        val bindings = bindingsByLorebook[lorebookId].orEmpty()
        val allCharacters = (presetCharacters + userCharacters).distinctBy { it.id }
        val charactersById = allCharacters.associateBy { it.id }
        val boundCharacterIds = bindings.map { it.characterId }.toSet()

        WorldInfoEditorUiState(
            header = lorebook?.toHeader(),
            entries = entries
                .sortedWith(compareBy({ it.insertionOrder }, { it.id }))
                .mapIndexed { index, entry ->
                    entry.toRow(
                        position = index,
                        total = entries.size,
                        language = appLanguage,
                    )
                },
            bindings = bindings.map { binding ->
                binding.toRow(
                    character = charactersById[binding.characterId],
                    language = appLanguage,
                )
            },
            bindablePickerItems = allCharacters
                .filterNot { it.id in boundCharacterIds }
                .map { it.toPickerItem(appLanguage) },
            pendingOperation = pendingOperation,
            errorMessage = errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = WorldInfoEditorUiState(),
    )

    fun saveHeader(
        englishName: String,
        chineseName: String,
        englishDescription: String,
        chineseDescription: String,
        tokenBudget: Int,
        isGlobal: Boolean,
    ) {
        val current = uiState.value.header ?: return
        pendingOperationState.value = WorldInfoEditorUiState.PendingOperation(
            targetId = current.lorebookId,
            kind = WorldInfoEditorUiState.PendingOperation.Kind.SaveHeader,
        )
        viewModelScope.launch {
            val source = currentLorebook() ?: run {
                handleMutationOutcome(
                    WorldInfoMutationResult.Rejected(
                        WorldInfoMutationResult.RejectionReason.UnknownLorebook,
                    ),
                )
                return@launch
            }
            val updated = source.copy(
                displayName = LocalizedText(english = englishName, chinese = chineseName),
                description = LocalizedText(english = englishDescription, chinese = chineseDescription),
                tokenBudget = tokenBudget,
                isGlobal = isGlobal,
            )
            handleMutationOutcome(repository.updateLorebook(updated))
        }
    }

    fun addEntry(
        englishName: String = "New entry",
        chineseName: String = "新条目",
    ) {
        pendingOperationState.value = WorldInfoEditorUiState.PendingOperation(
            targetId = lorebookId,
            kind = WorldInfoEditorUiState.PendingOperation.Kind.AddEntry,
        )
        viewModelScope.launch {
            val currentEntries = currentEntries()
            val nextOrder = (currentEntries.maxOfOrNull { it.insertionOrder } ?: -1) + 1
            val draft = LorebookEntry(
                id = "",
                lorebookId = lorebookId,
                name = LocalizedText(english = englishName, chinese = chineseName),
                insertionOrder = nextOrder,
            )
            handleMutationOutcome(repository.createEntry(lorebookId, draft))
        }
    }

    fun moveEntryUp(entryId: String) = moveEntry(entryId, delta = -1)

    fun moveEntryDown(entryId: String) = moveEntry(entryId, delta = +1)

    private fun moveEntry(entryId: String, delta: Int) {
        pendingOperationState.value = WorldInfoEditorUiState.PendingOperation(
            targetId = entryId,
            kind = WorldInfoEditorUiState.PendingOperation.Kind.MoveEntry,
        )
        viewModelScope.launch {
            val sorted = currentEntries().sortedWith(compareBy({ it.insertionOrder }, { it.id }))
            val position = sorted.indexOfFirst { it.id == entryId }
            val targetPosition = position + delta
            if (position == -1 || targetPosition !in sorted.indices) {
                pendingOperationState.value = null
                return@launch
            }
            val moved = sorted[position]
            val neighbor = sorted[targetPosition]
            val movedOrder = moved.insertionOrder
            val neighborOrder = neighbor.insertionOrder
            val swappedOrders = if (movedOrder == neighborOrder) {
                movedOrder + delta to movedOrder
            } else {
                neighborOrder to movedOrder
            }
            val first = repository.updateEntry(moved.copy(insertionOrder = swappedOrders.first))
            if (first is WorldInfoMutationResult.Success) {
                handleMutationOutcome(
                    repository.updateEntry(neighbor.copy(insertionOrder = swappedOrders.second)),
                )
            } else {
                handleMutationOutcome(first)
            }
        }
    }

    fun toggleEntryEnabled(entryId: String) {
        val target = currentEntries().firstOrNull { it.id == entryId } ?: return
        pendingOperationState.value = WorldInfoEditorUiState.PendingOperation(
            targetId = entryId,
            kind = WorldInfoEditorUiState.PendingOperation.Kind.ToggleEntry,
        )
        viewModelScope.launch {
            handleMutationOutcome(repository.updateEntry(target.copy(enabled = !target.enabled)))
        }
    }

    fun deleteEntry(entryId: String) {
        pendingOperationState.value = WorldInfoEditorUiState.PendingOperation(
            targetId = entryId,
            kind = WorldInfoEditorUiState.PendingOperation.Kind.DeleteEntry,
        )
        viewModelScope.launch {
            handleMutationOutcome(repository.deleteEntry(lorebookId, entryId))
        }
    }

    fun bindCharacter(characterId: String, asPrimary: Boolean = false) {
        pendingOperationState.value = WorldInfoEditorUiState.PendingOperation(
            targetId = characterId,
            kind = WorldInfoEditorUiState.PendingOperation.Kind.Bind,
        )
        viewModelScope.launch {
            handleMutationOutcome(
                repository.bind(lorebookId = lorebookId, characterId = characterId, isPrimary = asPrimary),
            )
        }
    }

    fun unbindCharacter(characterId: String) {
        pendingOperationState.value = WorldInfoEditorUiState.PendingOperation(
            targetId = characterId,
            kind = WorldInfoEditorUiState.PendingOperation.Kind.Unbind,
        )
        viewModelScope.launch {
            handleMutationOutcome(repository.unbind(lorebookId = lorebookId, characterId = characterId))
        }
    }

    fun togglePrimaryBinding(characterId: String) {
        val binding = currentBindings().firstOrNull { it.characterId == characterId } ?: return
        pendingOperationState.value = WorldInfoEditorUiState.PendingOperation(
            targetId = characterId,
            kind = WorldInfoEditorUiState.PendingOperation.Kind.TogglePrimary,
        )
        viewModelScope.launch {
            handleMutationOutcome(
                repository.updateBinding(
                    lorebookId = lorebookId,
                    characterId = characterId,
                    isPrimary = !binding.isPrimary,
                ),
            )
        }
    }

    fun clearError() {
        errorMessageState.value = null
    }

    private fun currentLorebook(): Lorebook? {
        val flow = repository.observeLorebooks() as? StateFlow<List<Lorebook>>
        return flow?.value?.firstOrNull { it.id == lorebookId }
    }

    private fun currentEntries(): List<LorebookEntry> {
        val flow = repository.observeEntries() as? StateFlow<Map<String, List<LorebookEntry>>>
        return flow?.value?.get(lorebookId).orEmpty()
    }

    private fun currentBindings(): List<LorebookBinding> {
        val flow = repository.observeBindings() as? StateFlow<Map<String, List<LorebookBinding>>>
        return flow?.value?.get(lorebookId).orEmpty()
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

    private fun Lorebook.toHeader(): WorldInfoEditorHeader = WorldInfoEditorHeader(
        lorebookId = id,
        ownerId = ownerId,
        englishName = displayName.english,
        chineseName = displayName.chinese,
        englishDescription = description.english,
        chineseDescription = description.chinese,
        tokenBudget = tokenBudget,
        isGlobal = isGlobal,
        isBuiltIn = isBuiltIn,
    )

    private fun LorebookEntry.toRow(
        position: Int,
        total: Int,
        language: AppLanguage,
    ): WorldInfoEditorEntryRow {
        val resolved = name.resolve(language).ifBlank {
            when (language) {
                AppLanguage.English -> "Untitled entry"
                AppLanguage.Chinese -> "未命名条目"
            }
        }
        return WorldInfoEditorEntryRow(
            entryId = id,
            displayName = resolved,
            enabled = enabled,
            insertionOrder = insertionOrder,
            position = position,
            total = total,
        )
    }

    private fun LorebookBinding.toRow(
        character: CompanionCharacterCard?,
        language: AppLanguage,
    ): WorldInfoEditorBindingRow = WorldInfoEditorBindingRow(
        characterId = characterId,
        displayName = character?.displayName?.resolve(language).orEmpty().ifBlank {
            when (language) {
                AppLanguage.English -> "Unknown character ($characterId)"
                AppLanguage.Chinese -> "未知角色 ($characterId)"
            }
        },
        isPrimary = isPrimary,
    )

    private fun CompanionCharacterCard.toPickerItem(language: AppLanguage): WorldInfoEditorPickerItem =
        WorldInfoEditorPickerItem(
            characterId = id,
            displayName = displayName.resolve(language).ifBlank {
                when (language) {
                    AppLanguage.English -> "Unnamed"
                    AppLanguage.Chinese -> "未命名"
                }
            },
        )
}
