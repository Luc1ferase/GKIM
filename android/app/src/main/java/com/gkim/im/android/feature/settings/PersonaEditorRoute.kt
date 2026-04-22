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
import com.gkim.im.android.core.model.UserPersonaValidationError
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.feature.shared.simpleViewModelFactory

@Composable
fun PersonaEditorRoute(
    container: AppContainer,
    personaId: String,
    onDone: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    val viewModel = viewModel<PersonaEditorViewModel>(
        key = "personaEditor-$personaId",
        factory = simpleViewModelFactory {
            PersonaEditorViewModel(
                repository = container.userPersonaRepository,
                personaId = personaId,
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedPersona?.id, uiState.savedPersona?.updatedAt) {
        if (uiState.savedPersona != null) {
            onDone()
        }
    }

    PageHeader(
        eyebrow = appLanguage.pick("Settings", "设置"),
        title = appLanguage.pick("Edit persona", "编辑角色"),
        description = appLanguage.pick(
            "Update the bilingual display name and description used for {{user}} injection.",
            "更新用于 {{user}} 注入的双语显示名和描述。",
        ),
        leadingLabel = appLanguage.pick("Back", "返回"),
        onLeading = onDone,
    )

    Column(
        modifier = Modifier.testTag("settings-detail-persona-editor"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (uiState.isBuiltIn) {
            GlassCard(modifier = Modifier.testTag("settings-persona-editor-builtin-notice")) {
                Text(
                    text = appLanguage.pick(
                        "This persona is built-in and cannot be modified. Duplicate it from the library if you need a customised copy.",
                        "内置角色不能被修改。如需定制，请在角色列表中复制后再编辑。",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
            }
        }

        uiState.saveError?.let { message ->
            GlassCard(modifier = Modifier.testTag("settings-persona-editor-error")) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
                OutlinedButton(
                    onClick = viewModel::clearSaveError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings-persona-editor-error-dismiss"),
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
                    .testTag("settings-persona-editor-display-name-en"),
                isError = uiState.validationErrors.contains(UserPersonaValidationError.DisplayNameEnglishBlank),
                label = { Text(appLanguage.pick("English", "英文")) },
            )
            OutlinedTextField(
                value = uiState.chineseDisplayName,
                onValueChange = viewModel::setChineseDisplayName,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-persona-editor-display-name-zh"),
                isError = uiState.validationErrors.contains(UserPersonaValidationError.DisplayNameChineseBlank),
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
                    .testTag("settings-persona-editor-description-en"),
                isError = uiState.validationErrors.contains(UserPersonaValidationError.DescriptionEnglishBlank),
                label = { Text(appLanguage.pick("English", "英文")) },
            )
            OutlinedTextField(
                value = uiState.chineseDescription,
                onValueChange = viewModel::setChineseDescription,
                enabled = !uiState.isBuiltIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-persona-editor-description-zh"),
                isError = uiState.validationErrors.contains(UserPersonaValidationError.DescriptionChineseBlank),
                label = { Text(appLanguage.pick("Chinese", "中文")) },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    viewModel.cancel()
                    onDone()
                },
                modifier = Modifier.testTag("settings-persona-editor-cancel"),
            ) {
                Text(appLanguage.pick("Cancel", "取消"))
            }
            Button(
                onClick = viewModel::save,
                enabled = uiState.canSave,
                modifier = Modifier.testTag("settings-persona-editor-save"),
            ) {
                Text(appLanguage.pick("Save", "保存"))
            }
        }
    }
}
