package com.gkim.im.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gkim.im.android.core.designsystem.ContentPolicyCopy
import com.gkim.im.android.data.local.PreferencesStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContentPolicyAcknowledgmentUiState(
    val version: String = ContentPolicyCopy.currentVersion,
    val isSubmitting: Boolean = false,
    val isAcknowledged: Boolean = false,
    val errorMessage: String? = null,
)

fun interface ContentPolicyAcknowledgmentSubmitter {
    suspend fun submit(version: String): Long
}

class ContentPolicyAcknowledgmentViewModel(
    private val submitter: ContentPolicyAcknowledgmentSubmitter,
    private val preferencesStore: PreferencesStore,
    private val version: String = ContentPolicyCopy.currentVersion,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContentPolicyAcknowledgmentUiState(version = version))
    val uiState: StateFlow<ContentPolicyAcknowledgmentUiState> = _uiState.asStateFlow()

    fun accept() {
        if (_uiState.value.isSubmitting || _uiState.value.isAcknowledged) return
        _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            val result = runCatching { submitter.submit(version) }
            result.fold(
                onSuccess = { acceptedAtMillis ->
                    val stamped = if (acceptedAtMillis > 0L) acceptedAtMillis else clock()
                    preferencesStore.setContentPolicyAcknowledgment(stamped, version)
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        isAcknowledged = true,
                        errorMessage = null,
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        isAcknowledged = false,
                        errorMessage = throwable.message ?: "acknowledgment failed",
                    )
                },
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
