package com.gkim.im.android.feature.tavern

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.PageHeader
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.resolve
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.CardImportPreview
import com.gkim.im.android.data.repository.CardInteropRepository
import com.gkim.im.android.feature.shared.simpleViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ImportCardPreviewUiState {
    data object Idle : ImportCardPreviewUiState
    data object Loading : ImportCardPreviewUiState
    data class Loaded(
        val preview: CardImportPreview,
        val selectedLanguage: String,
        val committing: Boolean = false,
    ) : ImportCardPreviewUiState

    data class Failed(val code: String) : ImportCardPreviewUiState
    data class Committed(val card: CompanionCharacterCard) : ImportCardPreviewUiState
}

internal object PendingImportBytes {
    private var bytes: ByteArray? = null
    private var filename: String? = null

    fun set(bytes: ByteArray, filename: String) {
        this.bytes = bytes
        this.filename = filename
    }

    fun take(): Pair<ByteArray, String>? {
        val b = bytes ?: return null
        val f = filename ?: return null
        bytes = null
        filename = null
        return b to f
    }
}

class ImportCardPreviewViewModel(
    private val repository: CardInteropRepository,
    scopeOverride: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope = scopeOverride ?: viewModelScope

    private val _state: MutableStateFlow<ImportCardPreviewUiState> =
        MutableStateFlow(ImportCardPreviewUiState.Idle)
    val state: StateFlow<ImportCardPreviewUiState> = _state.asStateFlow()

    fun submit(bytes: ByteArray, filename: String) {
        _state.value = ImportCardPreviewUiState.Loading
        scope.launch {
            repository.previewImport(bytes, filename)
                .onSuccess { preview ->
                    _state.value = ImportCardPreviewUiState.Loaded(
                        preview = preview,
                        selectedLanguage = preview.detectedLanguage,
                    )
                }
                .onFailure { throwable ->
                    _state.value = ImportCardPreviewUiState.Failed(throwable.message ?: "unknown")
                }
        }
    }

    fun selectLanguage(language: String) {
        val current = _state.value as? ImportCardPreviewUiState.Loaded ?: return
        _state.value = current.copy(selectedLanguage = language)
    }

    fun commit() {
        val current = _state.value as? ImportCardPreviewUiState.Loaded ?: return
        if (current.committing) return
        _state.value = current.copy(committing = true)
        scope.launch {
            repository.commitImport(
                preview = current.preview,
                overrides = null,
                languageOverride = current.selectedLanguage,
            )
                .onSuccess { card -> _state.value = ImportCardPreviewUiState.Committed(card) }
                .onFailure { throwable ->
                    _state.value =
                        ImportCardPreviewUiState.Failed(throwable.message ?: "commit_failed")
                }
        }
    }

    fun reset() {
        _state.value = ImportCardPreviewUiState.Idle
    }
}

@Composable
fun ImportCardPreviewRoute(
    navController: NavHostController,
    container: AppContainer,
) {
    val viewModel = viewModel<ImportCardPreviewViewModel>(factory = simpleViewModelFactory {
        ImportCardPreviewViewModel(container.cardInteropRepository)
    })
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        PendingImportBytes.take()?.let { (bytes, filename) ->
            viewModel.submit(bytes, filename)
        }
    }

    LaunchedEffect(state) {
        if (state is ImportCardPreviewUiState.Committed) {
            navController.popBackStack("space", inclusive = false)
        }
    }

    ImportCardPreviewScreen(
        state = state,
        onSelectLanguage = viewModel::selectLanguage,
        onCommit = viewModel::commit,
        onCancel = {
            viewModel.reset()
            navController.popBackStack()
        },
    )
}

@Composable
private fun ImportCardPreviewScreen(
    state: ImportCardPreviewUiState,
    onSelectLanguage: (String) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    LazyColumn(
        modifier = Modifier
            .background(AetherColors.Surface)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("tavern-import-preview"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            PageHeader(
                title = appLanguage.pick("Import Preview", "导入预览"),
                actionLabel = appLanguage.pick("Cancel", "取消"),
                onAction = onCancel,
            )
        }
        when (state) {
            ImportCardPreviewUiState.Idle -> item {
                Text(
                    text = appLanguage.pick(
                        "Pick a card file from the tavern to begin.",
                        "从酒馆选择一个角色卡文件以开始。",
                    ),
                    color = AetherColors.OnSurfaceVariant,
                    modifier = Modifier.testTag("tavern-import-preview-idle"),
                )
            }
            ImportCardPreviewUiState.Loading -> item {
                Text(
                    text = appLanguage.pick("Decoding the card…", "正在解析角色卡…"),
                    color = AetherColors.OnSurfaceVariant,
                    modifier = Modifier.testTag("tavern-import-preview-loading"),
                )
            }
            is ImportCardPreviewUiState.Loaded -> {
                val resolved = state.preview.card.resolve(appLanguage)
                item {
                    GlassCard(modifier = Modifier.testTag("tavern-import-preview-card")) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = resolved.displayName,
                                style = MaterialTheme.typography.headlineMedium,
                                color = AetherColors.OnSurface,
                            )
                            Text(
                                text = resolved.roleLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = AetherColors.OnSurfaceVariant,
                            )
                            Text(
                                text = resolved.firstMes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AetherColors.OnSurface,
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LanguagePill(
                            label = "EN",
                            active = state.selectedLanguage == "en",
                            onClick = { onSelectLanguage("en") },
                            modifier = Modifier.testTag("tavern-import-preview-language-en"),
                        )
                        LanguagePill(
                            label = "ZH",
                            active = state.selectedLanguage == "zh",
                            onClick = { onSelectLanguage("zh") },
                            modifier = Modifier.testTag("tavern-import-preview-language-zh"),
                        )
                    }
                }
                if (state.preview.warnings.isNotEmpty()) {
                    item {
                        Text(
                            text = appLanguage.pick("Warnings", "提示"),
                            style = MaterialTheme.typography.titleMedium,
                            color = AetherColors.OnSurface,
                        )
                    }
                    items(state.preview.warnings, key = { it.code + (it.field ?: "") }) { warning ->
                        Text(
                            text = warning.field?.let { "${warning.code} · $it" } ?: warning.code,
                            style = MaterialTheme.typography.bodySmall,
                            color = AetherColors.OnSurfaceVariant,
                            modifier = Modifier.testTag("tavern-import-preview-warning-${warning.code}"),
                        )
                    }
                }
                if (state.preview.stExtensionKeys.isNotEmpty()) {
                    item {
                        Text(
                            text = appLanguage.pick(
                                "Preserved extensions: ${state.preview.stExtensionKeys.joinToString(", ")}",
                                "保留的扩展字段：${state.preview.stExtensionKeys.joinToString("、")}",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = AetherColors.OnSurfaceVariant,
                            modifier = Modifier.testTag("tavern-import-preview-st-extensions"),
                        )
                    }
                }
                item {
                    Text(
                        text = if (state.committing) {
                            appLanguage.pick("Saving…", "保存中…")
                        } else {
                            appLanguage.pick("Commit", "保存")
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = AetherColors.OnSurface,
                        modifier = Modifier
                            .background(AetherColors.SurfaceContainerHigh, shape = CircleShape)
                            .clickable(enabled = !state.committing, onClick = onCommit)
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .testTag("tavern-import-preview-commit"),
                    )
                }
            }
            is ImportCardPreviewUiState.Failed -> item {
                Text(
                    text = importErrorCopy(state.code, appLanguage == com.gkim.im.android.core.model.AppLanguage.English),
                    color = AetherColors.Danger,
                    modifier = Modifier.testTag("tavern-import-preview-failed"),
                )
            }
            is ImportCardPreviewUiState.Committed -> item {
                Text(
                    text = appLanguage.pick("Added to your roster.", "已加入你的持有列表。"),
                    color = AetherColors.OnSurfaceVariant,
                    modifier = Modifier.testTag("tavern-import-preview-committed"),
                )
            }
        }
    }
}

@Composable
private fun LanguagePill(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = if (active) "[$label]" else label,
        style = MaterialTheme.typography.labelLarge,
        color = if (active) AetherColors.Primary else AetherColors.OnSurface,
        modifier = modifier
            .background(AetherColors.SurfaceContainerHigh, shape = CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
