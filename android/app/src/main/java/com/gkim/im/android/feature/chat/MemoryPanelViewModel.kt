package com.gkim.im.android.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gkim.im.android.core.model.CompanionMemory
import com.gkim.im.android.core.model.CompanionMemoryPin
import com.gkim.im.android.core.model.CompanionMemoryResetScope
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.data.repository.CompanionMemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class MemoryPanelUiState(
    val cardId: String,
    val memory: CompanionMemory? = null,
    val pins: List<CompanionMemoryPin> = emptyList(),
    val currentTurn: Int = 0,
    val pinEditor: PinEditorState? = null,
    val resetConfirmation: CompanionMemoryResetScope? = null,
    val operationError: String? = null,
) {
    val turnsSinceSummaryUpdate: Int?
        get() = memory?.let { (currentTurn - it.summaryTurnCursor).coerceAtLeast(0) }
}

data class PinEditorState(
    val pinId: String? = null,
    val sourceMessageId: String? = null,
    val englishText: String = "",
    val chineseText: String = "",
) {
    val isCreate: Boolean get() = pinId == null
    val canSave: Boolean get() = englishText.isNotBlank() && chineseText.isNotBlank()
}

class MemoryPanelViewModel(
    private val repository: CompanionMemoryRepository,
    private val cardId: String,
    private val currentTurnProvider: () -> Int = { 0 },
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryPanelUiState(cardId = cardId))
    val uiState: StateFlow<MemoryPanelUiState> = _uiState.asStateFlow()

    init {
        combine(
            repository.observeMemory(cardId),
            repository.observePins(cardId),
        ) { memory, pins -> memory to pins }
            .onEach { (memory, pins) ->
                _uiState.value = _uiState.value.copy(
                    memory = memory,
                    pins = pins.sortedBy { it.createdAt },
                    currentTurn = currentTurnProvider(),
                )
            }
            .launchIn(viewModelScope)

        viewModelScope.launch { repository.refresh(cardId) }
    }

    fun openPinEditorForNew(sourceMessageId: String? = null) {
        _uiState.value = _uiState.value.copy(
            pinEditor = PinEditorState(sourceMessageId = sourceMessageId),
        )
    }

    fun openPinEditorForEdit(pinId: String) {
        val pin = _uiState.value.pins.firstOrNull { it.id == pinId } ?: return
        _uiState.value = _uiState.value.copy(
            pinEditor = PinEditorState(
                pinId = pin.id,
                sourceMessageId = pin.sourceMessageId,
                englishText = pin.text.english,
                chineseText = pin.text.chinese,
            ),
        )
    }

    fun setPinEnglish(text: String) {
        val editor = _uiState.value.pinEditor ?: return
        _uiState.value = _uiState.value.copy(pinEditor = editor.copy(englishText = text))
    }

    fun setPinChinese(text: String) {
        val editor = _uiState.value.pinEditor ?: return
        _uiState.value = _uiState.value.copy(pinEditor = editor.copy(chineseText = text))
    }

    fun savePinEditor() {
        val editor = _uiState.value.pinEditor ?: return
        if (!editor.canSave) return
        val text = LocalizedText(editor.englishText, editor.chineseText)
        viewModelScope.launch {
            val result = if (editor.isCreate) {
                repository.createPin(cardId, editor.sourceMessageId, text)
            } else {
                repository.updatePin(editor.pinId!!, text)
            }
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(pinEditor = null, operationError = null)
            } else {
                _uiState.value = _uiState.value.copy(
                    operationError = result.exceptionOrNull()?.message ?: "Pin save failed",
                )
            }
        }
    }

    fun cancelPinEditor() {
        _uiState.value = _uiState.value.copy(pinEditor = null)
    }

    fun deletePin(pinId: String) {
        viewModelScope.launch {
            val result = repository.deletePin(pinId)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    operationError = result.exceptionOrNull()?.message ?: "Pin delete failed",
                )
            }
        }
    }

    fun requestReset(scope: CompanionMemoryResetScope) {
        _uiState.value = _uiState.value.copy(resetConfirmation = scope)
    }

    fun confirmReset() {
        val scope = _uiState.value.resetConfirmation ?: return
        _uiState.value = _uiState.value.copy(resetConfirmation = null)
        viewModelScope.launch { repository.reset(cardId, scope) }
    }

    fun cancelResetConfirmation() {
        _uiState.value = _uiState.value.copy(resetConfirmation = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(operationError = null)
    }
}
