package com.gkim.im.android.feature.settings

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
import androidx.compose.runtime.LaunchedEffect
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
import com.gkim.im.android.core.model.PresetValidationError
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.feature.shared.simpleViewModelFactory

@Composable
fun PresetEditorRoute(
    container: AppContainer,
    presetId: String,
    onDone: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    val viewModel = viewModel<PresetEditorViewModel>(
        key = "presetEditor-$presetId",
        factory = simpleViewModelFactory {
            PresetEditorViewModel(
                repository = container.companionPresetRepository,
                presetId = presetId,
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedPreset?.id, uiState.savedPreset?.updatedAt) {
        if (uiState.savedPreset != null) {
            onDone()
        }
    }

    PageHeader(
        eyebrow = appLanguage.pick("Settings", "设置"),
        title = appLanguage.pick("Edit preset", "编辑预设"),
        description = appLanguage.pick(
            "Tune the system prompt sections and reply parameters that shape companion replies.",
            "调整塑造伙伴回复的系统提示与回复参数。",
        ),
        leadingLabel = appLanguage.pick("Back", "返回"),
        onLeading = onDone,
    )

    Column(
        modifier = Modifier.testTag("settings-detail-preset-editor"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (uiState.isBuiltIn) {
            GlassCard(modifier = Modifier.testTag("settings-preset-editor-builtin-notice")) {
                Text(
                    text = appLanguage.pick(
                        "This preset is built-in and cannot be modified. Duplicate it from the library if you need a customised copy.",
                        "内置预设不能被修改。如需定制，请在预设列表中复制后再编辑。",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
            }
        }

        uiState.saveError?.let { message ->
            GlassCard(modifier = Modifier.testTag("settings-preset-editor-error")) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
                OutlinedButton(
                    onClick = viewModel::clearSaveError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings-preset-editor-error-dismiss"),
                ) {
                    Text(appLanguage.pick("Dismiss", "知道了"))
                }
            }
        }

        GlassCard {
            Text(
                text = appLanguage.pick("DISPLAY NAME", "显示名"),
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.Primary,
            )
            OutlinedTextField(
                value = uiState.englishDisplayName,
                onValueChange = viewModel::setEnglishDisplayName,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-preset-editor-display-name-en"),
                isError = uiState.validationErrors.contains(PresetValidationError.DisplayNameEnglishBlank),
                label = { Text(appLanguage.pick("English", "英文")) },
            )
            OutlinedTextField(
                value = uiState.chineseDisplayName,
                onValueChange = viewModel::setChineseDisplayName,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-preset-editor-display-name-zh"),
                isError = uiState.validationErrors.contains(PresetValidationError.DisplayNameChineseBlank),
                label = { Text(appLanguage.pick("Chinese", "中文")) },
            )
        }

        GlassCard {
            Text(
                text = appLanguage.pick("DESCRIPTION", "描述"),
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.Primary,
            )
            OutlinedTextField(
                value = uiState.englishDescription,
                onValueChange = viewModel::setEnglishDescription,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-preset-editor-description-en"),
                label = { Text(appLanguage.pick("English", "英文")) },
            )
            OutlinedTextField(
                value = uiState.chineseDescription,
                onValueChange = viewModel::setChineseDescription,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-preset-editor-description-zh"),
                label = { Text(appLanguage.pick("Chinese", "中文")) },
            )
        }

        GlassCard {
            Text(
                text = appLanguage.pick("SYSTEM PREFIX", "系统前缀"),
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.Primary,
            )
            OutlinedTextField(
                value = uiState.englishSystemPrefix,
                onValueChange = viewModel::setEnglishSystemPrefix,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-preset-editor-system-prefix-en"),
                label = { Text(appLanguage.pick("English", "英文")) },
            )
            OutlinedTextField(
                value = uiState.chineseSystemPrefix,
                onValueChange = viewModel::setChineseSystemPrefix,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-preset-editor-system-prefix-zh"),
                label = { Text(appLanguage.pick("Chinese", "中文")) },
            )
        }

        GlassCard {
            Text(
                text = appLanguage.pick("SYSTEM SUFFIX", "系统后缀"),
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.Primary,
            )
            OutlinedTextField(
                value = uiState.englishSystemSuffix,
                onValueChange = viewModel::setEnglishSystemSuffix,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-preset-editor-system-suffix-en"),
                label = { Text(appLanguage.pick("English", "英文")) },
            )
            OutlinedTextField(
                value = uiState.chineseSystemSuffix,
                onValueChange = viewModel::setChineseSystemSuffix,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-preset-editor-system-suffix-zh"),
                label = { Text(appLanguage.pick("Chinese", "中文")) },
            )
        }

        GlassCard {
            Text(
                text = appLanguage.pick("FORMAT INSTRUCTIONS", "格式说明"),
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.Primary,
            )
            OutlinedTextField(
                value = uiState.englishFormatInstructions,
                onValueChange = viewModel::setEnglishFormatInstructions,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-preset-editor-format-instructions-en"),
                label = { Text(appLanguage.pick("English", "英文")) },
            )
            OutlinedTextField(
                value = uiState.chineseFormatInstructions,
                onValueChange = viewModel::setChineseFormatInstructions,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-preset-editor-format-instructions-zh"),
                label = { Text(appLanguage.pick("Chinese", "中文")) },
            )
        }

        GlassCard {
            Text(
                text = appLanguage.pick("POST-HISTORY INSTRUCTIONS", "历史后说明"),
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.Primary,
            )
            OutlinedTextField(
                value = uiState.englishPostHistoryInstructions,
                onValueChange = viewModel::setEnglishPostHistoryInstructions,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-preset-editor-post-history-en"),
                label = { Text(appLanguage.pick("English", "英文")) },
            )
            OutlinedTextField(
                value = uiState.chinesePostHistoryInstructions,
                onValueChange = viewModel::setChinesePostHistoryInstructions,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-preset-editor-post-history-zh"),
                label = { Text(appLanguage.pick("Chinese", "中文")) },
            )
        }

        GlassCard {
            Text(
                text = appLanguage.pick("REPLY PARAMETERS", "回复参数"),
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.Primary,
            )
            OutlinedTextField(
                value = uiState.temperatureText,
                onValueChange = viewModel::setTemperatureText,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-preset-editor-temperature"),
                isError = uiState.validationErrors.contains(PresetValidationError.TemperatureOutOfRange),
                label = { Text(appLanguage.pick("Temperature (0.0–2.0)", "温度 (0.0–2.0)")) },
            )
            OutlinedTextField(
                value = uiState.topPText,
                onValueChange = viewModel::setTopPText,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-preset-editor-top-p"),
                isError = uiState.validationErrors.contains(PresetValidationError.TopPOutOfRange),
                label = { Text(appLanguage.pick("Top-p (0.0–1.0)", "Top-p (0.0–1.0)")) },
            )
            OutlinedTextField(
                value = uiState.maxReplyTokensText,
                onValueChange = viewModel::setMaxReplyTokensText,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-preset-editor-max-reply-tokens"),
                isError = uiState.validationErrors.contains(PresetValidationError.MaxReplyTokensOutOfRange),
                label = { Text(appLanguage.pick("Max reply tokens (1–32768)", "最大回复 token (1–32768)")) },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    viewModel.cancel()
                    onDone()
                },
                modifier = Modifier.testTag("settings-preset-editor-cancel"),
            ) {
                Text(appLanguage.pick("Cancel", "取消"))
            }
            Button(
                onClick = viewModel::save,
                enabled = uiState.canSave,
                modifier = Modifier.testTag("settings-preset-editor-save"),
            ) {
                Text(appLanguage.pick("Save", "保存"))
            }
        }
    }
}
