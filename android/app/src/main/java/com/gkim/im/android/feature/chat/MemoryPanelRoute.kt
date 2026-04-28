package com.gkim.im.android.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.PageHeader
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionMemory
import com.gkim.im.android.core.model.CompanionMemoryPin
import com.gkim.im.android.core.model.CompanionMemoryResetScope
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.feature.shared.simpleViewModelFactory

@Composable
fun MemoryPanelRoute(
    container: AppContainer,
    cardId: String,
    currentTurnProvider: () -> Int = { 0 },
    onDone: () -> Unit,
) {
    val viewModel = viewModel<MemoryPanelViewModel>(
        key = "memoryPanel-$cardId",
        factory = simpleViewModelFactory {
            MemoryPanelViewModel(
                repository = container.companionMemoryRepository,
                cardId = cardId,
                currentTurnProvider = currentTurnProvider,
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current

    MemoryPanelContent(
        state = uiState,
        language = language,
        onBack = onDone,
        onNewPin = { viewModel.openPinEditorForNew() },
        onEditPin = viewModel::openPinEditorForEdit,
        onDeletePin = viewModel::deletePin,
        onSetPinEnglish = viewModel::setPinEnglish,
        onSetPinChinese = viewModel::setPinChinese,
        onSavePin = viewModel::savePinEditor,
        onCancelPin = viewModel::cancelPinEditor,
        onRequestReset = viewModel::requestReset,
        onConfirmReset = viewModel::confirmReset,
        onCancelReset = viewModel::cancelResetConfirmation,
        onClearError = viewModel::clearError,
    )
}

@Composable
fun MemoryPanelContent(
    state: MemoryPanelUiState,
    language: AppLanguage,
    onBack: () -> Unit,
    onNewPin: () -> Unit,
    onEditPin: (String) -> Unit,
    onDeletePin: (String) -> Unit,
    onSetPinEnglish: (String) -> Unit,
    onSetPinChinese: (String) -> Unit,
    onSavePin: () -> Unit,
    onCancelPin: () -> Unit,
    onRequestReset: (CompanionMemoryResetScope) -> Unit,
    onConfirmReset: () -> Unit,
    onCancelReset: () -> Unit,
    onClearError: () -> Unit,
) {
    PageHeader(
        eyebrow = language.pick("Companion", "伙伴"),
        title = language.pick("Memory", "记忆"),
        description = language.pick(
            "Review what the companion remembers, pin facts so they persist, and reset scopes if needed.",
            "查看伙伴记得的内容，固定需要保留的事实，必要时重置相应范围。",
        ),
        leadingLabel = language.pick("Back", "返回"),
        onLeading = onBack,
    )

    Column(
        modifier = Modifier.testTag("memory-panel-root"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        state.operationError?.let { message ->
            GlassCard(modifier = Modifier.testTag("memory-panel-error")) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
                OutlinedButton(
                    onClick = onClearError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("memory-panel-error-dismiss"),
                ) {
                    Text(language.pick("Dismiss", "知道了"))
                }
            }
        }

        SummaryCard(state = state, language = language)

        PinsSection(
            state = state,
            language = language,
            onNewPin = onNewPin,
            onEditPin = onEditPin,
            onDeletePin = onDeletePin,
        )

        state.pinEditor?.let { editor ->
            PinEditorCard(
                editor = editor,
                language = language,
                onSetEnglish = onSetPinEnglish,
                onSetChinese = onSetPinChinese,
                onSave = onSavePin,
                onCancel = onCancelPin,
            )
        }

        ResetControls(
            pendingScope = state.resetConfirmation,
            language = language,
            onRequestReset = onRequestReset,
            onConfirmReset = onConfirmReset,
            onCancelReset = onCancelReset,
        )
    }
}

@Composable
private fun SummaryCard(
    state: MemoryPanelUiState,
    language: AppLanguage,
) {
    GlassCard(modifier = Modifier.testTag("memory-panel-summary")) {
        Text(
            text = language.pick("SUMMARY", "摘要"),
            style = MaterialTheme.typography.labelLarge,
            color = AetherColors.Primary,
        )
        val summary = state.memory?.summary
        val summaryText = summary?.resolve(language).orEmpty().trim()
        if (summaryText.isEmpty()) {
            Text(
                text = language.pick(
                    "No summary yet — keep chatting and the companion will start remembering key moments.",
                    "还没有摘要——继续对话，伙伴会开始记住关键时刻。",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = AetherColors.OnSurfaceVariant,
                modifier = Modifier.testTag("memory-panel-summary-empty"),
            )
        } else {
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyLarge,
                color = AetherColors.OnSurface,
                modifier = Modifier.testTag("memory-panel-summary-body"),
            )
        }
        val turnsSince = state.turnsSinceSummaryUpdate
        val subtitle = when {
            state.memory == null -> null
            turnsSince == null || turnsSince == 0 -> language.pick(
                "Updated this turn",
                "本回合已更新",
            )
            turnsSince == 1 -> language.pick(
                "Updated 1 turn ago",
                "1 回合前更新",
            )
            else -> language.pick(
                "Updated $turnsSince turns ago",
                "$turnsSince 回合前更新",
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = AetherColors.OnSurfaceVariant,
                modifier = Modifier.testTag("memory-panel-summary-subtitle"),
            )
        }
    }
}

@Composable
private fun PinsSection(
    state: MemoryPanelUiState,
    language: AppLanguage,
    onNewPin: () -> Unit,
    onEditPin: (String) -> Unit,
    onDeletePin: (String) -> Unit,
) {
    GlassCard(modifier = Modifier.testTag("memory-panel-pins")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = language.pick("PINNED FACTS", "固定事实"),
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.Primary,
            )
            OutlinedButton(
                onClick = onNewPin,
                modifier = Modifier.testTag("memory-panel-pins-new"),
            ) {
                Text(language.pick("New pin", "新增"))
            }
        }
        if (state.pins.isEmpty()) {
            Text(
                text = language.pick(
                    "Nothing pinned yet. Pin facts you want the companion to always remember.",
                    "暂无固定内容。把希望伙伴始终记住的事实固定下来。",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = AetherColors.OnSurfaceVariant,
                modifier = Modifier.testTag("memory-panel-pins-empty"),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.pins.forEach { pin ->
                    PinRow(
                        pin = pin,
                        language = language,
                        onEdit = { onEditPin(pin.id) },
                        onDelete = { onDeletePin(pin.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PinRow(
    pin: CompanionMemoryPin,
    language: AppLanguage,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("memory-panel-pin-${pin.id}"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = pin.text.resolve(language),
            style = MaterialTheme.typography.bodyLarge,
            color = AetherColors.OnSurface,
            modifier = Modifier.testTag("memory-panel-pin-text-${pin.id}"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.testTag("memory-panel-pin-edit-${pin.id}"),
            ) {
                Text(language.pick("Edit", "编辑"))
            }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.testTag("memory-panel-pin-delete-${pin.id}"),
            ) {
                Text(language.pick("Delete", "删除"))
            }
        }
    }
}

@Composable
private fun PinEditorCard(
    editor: PinEditorState,
    language: AppLanguage,
    onSetEnglish: (String) -> Unit,
    onSetChinese: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val editorTag = if (editor.isCreate) {
        "memory-panel-pin-editor-create"
    } else {
        "memory-panel-pin-editor-edit"
    }
    GlassCard(modifier = Modifier.testTag(editorTag)) {
        Text(
            text = if (editor.isCreate) {
                language.pick("NEW PIN", "新增固定")
            } else {
                language.pick("EDIT PIN", "编辑固定")
            },
            style = MaterialTheme.typography.labelLarge,
            color = AetherColors.Primary,
        )
        OutlinedTextField(
            value = editor.englishText,
            onValueChange = onSetEnglish,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("memory-panel-pin-editor-en"),
            label = { Text(language.pick("English", "英文")) },
        )
        OutlinedTextField(
            value = editor.chineseText,
            onValueChange = onSetChinese,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("memory-panel-pin-editor-zh"),
            label = { Text(language.pick("Chinese", "中文")) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.testTag("memory-panel-pin-editor-cancel"),
            ) {
                Text(language.pick("Cancel", "取消"))
            }
            Button(
                onClick = onSave,
                enabled = editor.canSave,
                modifier = Modifier.testTag("memory-panel-pin-editor-save"),
            ) {
                Text(language.pick("Save", "保存"))
            }
        }
    }
}

@Composable
private fun ResetControls(
    pendingScope: CompanionMemoryResetScope?,
    language: AppLanguage,
    onRequestReset: (CompanionMemoryResetScope) -> Unit,
    onConfirmReset: () -> Unit,
    onCancelReset: () -> Unit,
) {
    GlassCard(modifier = Modifier.testTag("memory-panel-reset")) {
        Text(
            text = language.pick("RESET", "重置"),
            style = MaterialTheme.typography.labelLarge,
            color = AetherColors.Primary,
        )
        Text(
            text = language.pick(
                "Resetting cannot be undone. The companion will lose the chosen memory scope for this card.",
                "重置无法撤销。伙伴将失去对应范围的记忆。",
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = AetherColors.OnSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { onRequestReset(CompanionMemoryResetScope.Pins) },
                modifier = Modifier.testTag("memory-panel-reset-pins"),
            ) {
                Text(language.pick("Clear pinned", "清除固定"))
            }
            OutlinedButton(
                onClick = { onRequestReset(CompanionMemoryResetScope.Summary) },
                modifier = Modifier.testTag("memory-panel-reset-summary"),
            ) {
                Text(language.pick("Clear summary", "清除摘要"))
            }
            OutlinedButton(
                onClick = { onRequestReset(CompanionMemoryResetScope.All) },
                modifier = Modifier.testTag("memory-panel-reset-all"),
            ) {
                Text(language.pick("Clear all", "全部清除"))
            }
        }
        pendingScope?.let { scope ->
            val prompt = when (scope) {
                CompanionMemoryResetScope.Pins -> language.pick(
                    "Remove every pinned fact for this companion?",
                    "确定清除该伙伴的全部固定事实吗？",
                )
                CompanionMemoryResetScope.Summary -> language.pick(
                    "Clear the companion's summary? It will rebuild from future turns.",
                    "确定清除伙伴的摘要吗？未来对话会重新建立。",
                )
                CompanionMemoryResetScope.All -> language.pick(
                    "Clear both pinned facts and summary?",
                    "确定同时清除固定事实与摘要吗？",
                )
            }
            val confirmTag = "memory-panel-reset-confirm-${scope.name.lowercase()}"
            val cancelTag = "memory-panel-reset-cancel-${scope.name.lowercase()}"
            GlassCard(modifier = Modifier.testTag("memory-panel-reset-confirmation")) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onCancelReset,
                        modifier = Modifier.testTag(cancelTag),
                    ) {
                        Text(language.pick("Cancel", "取消"))
                    }
                    Button(
                        onClick = onConfirmReset,
                        modifier = Modifier.testTag(confirmTag),
                    ) {
                        Text(language.pick("Confirm reset", "确认重置"))
                    }
                }
            }
        }
    }
}
