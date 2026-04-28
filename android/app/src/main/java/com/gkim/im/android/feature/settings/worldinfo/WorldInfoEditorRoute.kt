package com.gkim.im.android.feature.settings.worldinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.PageHeader
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.feature.shared.simpleViewModelFactory

@Composable
fun WorldInfoEditorRoute(
    container: AppContainer,
    lorebookId: String,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit = {},
) {
    val appLanguage = LocalAppLanguage.current
    val viewModel = viewModel<WorldInfoEditorViewModel>(
        key = "worldInfoEditor-$lorebookId",
        factory = simpleViewModelFactory {
            WorldInfoEditorViewModel(
                repository = container.worldInfoRepository,
                rosterRepository = container.companionRosterRepository,
                lorebookId = lorebookId,
                language = { appLanguage },
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val header = uiState.header

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("worldinfo-editor-screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PageHeader(
            eyebrow = appLanguage.pick("World Info", "世界信息"),
            title = appLanguage.pick("Edit lorebook", "编辑世界书"),
            description = appLanguage.pick(
                "Update the lorebook header, entries, and character bindings.",
                "更新世界书的基础信息、条目与角色绑定。",
            ),
            leadingLabel = appLanguage.pick("Back", "返回"),
            onLeading = onBack,
        )

        uiState.errorMessage?.let { message ->
            GlassCard(modifier = Modifier.testTag("worldinfo-editor-error")) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
                TextButton(
                    modifier = Modifier.testTag("worldinfo-editor-error-dismiss"),
                    onClick = viewModel::clearError,
                ) {
                    Text(appLanguage.pick("Dismiss", "忽略"))
                }
            }
        }

        if (header == null) {
            GlassCard(modifier = Modifier.testTag("worldinfo-editor-missing")) {
                Text(
                    text = appLanguage.pick(
                        "Lorebook not found. It may have been deleted.",
                        "未找到世界书。可能已被删除。",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
            }
        } else {
            WorldInfoEditorHeaderCard(
                header = header,
                onSave = viewModel::saveHeader,
            )
            WorldInfoEditorEntriesCard(
                entries = uiState.entries,
                onAddEntry = { viewModel.addEntry() },
                onMoveUp = viewModel::moveEntryUp,
                onMoveDown = viewModel::moveEntryDown,
                onToggleEnabled = viewModel::toggleEntryEnabled,
                onDelete = viewModel::deleteEntry,
                onOpen = onOpenEntry,
            )
            WorldInfoEditorBindingsCard(
                bindings = uiState.bindings,
                pickerItems = uiState.bindablePickerItems,
                onBind = { viewModel.bindCharacter(it) },
                onUnbind = viewModel::unbindCharacter,
                onTogglePrimary = viewModel::togglePrimaryBinding,
            )
        }
    }
}

@Composable
private fun WorldInfoEditorHeaderCard(
    header: WorldInfoEditorHeader,
    onSave: (String, String, String, String, Int, Boolean) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    var englishName by rememberSaveable(header.lorebookId) { mutableStateOf(header.englishName) }
    var chineseName by rememberSaveable(header.lorebookId) { mutableStateOf(header.chineseName) }
    var englishDescription by rememberSaveable(header.lorebookId) { mutableStateOf(header.englishDescription) }
    var chineseDescription by rememberSaveable(header.lorebookId) { mutableStateOf(header.chineseDescription) }
    var tokenBudgetText by rememberSaveable(header.lorebookId) { mutableStateOf(header.tokenBudget.toString()) }
    var isGlobal by rememberSaveable(header.lorebookId) { mutableStateOf(header.isGlobal) }

    GlassCard(modifier = Modifier.testTag("worldinfo-editor-header-card")) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = appLanguage.pick("Header", "基础信息"),
                style = MaterialTheme.typography.titleSmall,
                color = AetherColors.OnSurface,
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worldinfo-editor-header-english-name"),
                value = englishName,
                onValueChange = { englishName = it },
                label = { Text(appLanguage.pick("Name (English)", "名称 (英文)")) },
                enabled = !header.isBuiltIn,
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worldinfo-editor-header-chinese-name"),
                value = chineseName,
                onValueChange = { chineseName = it },
                label = { Text(appLanguage.pick("Name (Chinese)", "名称 (中文)")) },
                enabled = !header.isBuiltIn,
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worldinfo-editor-header-english-description"),
                value = englishDescription,
                onValueChange = { englishDescription = it },
                label = { Text(appLanguage.pick("Description (English)", "描述 (英文)")) },
                enabled = !header.isBuiltIn,
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worldinfo-editor-header-chinese-description"),
                value = chineseDescription,
                onValueChange = { chineseDescription = it },
                label = { Text(appLanguage.pick("Description (Chinese)", "描述 (中文)")) },
                enabled = !header.isBuiltIn,
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worldinfo-editor-header-token-budget"),
                value = tokenBudgetText,
                onValueChange = { input -> tokenBudgetText = input.filter { it.isDigit() } },
                label = { Text(appLanguage.pick("Token budget", "Token 预算")) },
                enabled = !header.isBuiltIn,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worldinfo-editor-header-global-row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = appLanguage.pick("Global (applies to every character)", "全局 (对所有角色生效)"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
                Switch(
                    modifier = Modifier.testTag("worldinfo-editor-header-global-switch"),
                    checked = isGlobal,
                    onCheckedChange = { isGlobal = it },
                    enabled = !header.isBuiltIn,
                )
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worldinfo-editor-header-save"),
                enabled = !header.isBuiltIn,
                onClick = {
                    onSave(
                        englishName,
                        chineseName,
                        englishDescription,
                        chineseDescription,
                        tokenBudgetText.toIntOrNull() ?: header.tokenBudget,
                        isGlobal,
                    )
                },
            ) {
                Text(appLanguage.pick("Save header", "保存基础信息"))
            }
        }
    }
}

@Composable
private fun WorldInfoEditorEntriesCard(
    entries: List<WorldInfoEditorEntryRow>,
    onAddEntry: () -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onToggleEnabled: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    GlassCard(modifier = Modifier.testTag("worldinfo-editor-entries-card")) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = appLanguage.pick("Entries", "条目"),
                    style = MaterialTheme.typography.titleSmall,
                    color = AetherColors.OnSurface,
                )
                OutlinedButton(
                    modifier = Modifier.testTag("worldinfo-editor-add-entry"),
                    onClick = onAddEntry,
                ) {
                    Text(appLanguage.pick("Add entry", "新增条目"))
                }
            }
            if (entries.isEmpty()) {
                Text(
                    modifier = Modifier.testTag("worldinfo-editor-entries-empty"),
                    text = appLanguage.pick("No entries yet.", "暂无条目。"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
            } else {
                entries.forEach { entry ->
                    WorldInfoEditorEntryRowCard(
                        entry = entry,
                        onMoveUp = { onMoveUp(entry.entryId) },
                        onMoveDown = { onMoveDown(entry.entryId) },
                        onToggleEnabled = { onToggleEnabled(entry.entryId) },
                        onDelete = { onDelete(entry.entryId) },
                        onOpen = { onOpen(entry.entryId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorldInfoEditorEntryRowCard(
    entry: WorldInfoEditorEntryRow,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    var overflowExpanded by rememberSaveable(entry.entryId) { mutableStateOf(false) }
    val rowTag = "worldinfo-editor-entry-${entry.entryId}"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .testTag(rowTag),
        color = AetherColors.SurfaceContainerLow,
        onClick = onOpen,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.testTag("$rowTag-name"),
                    text = entry.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AetherColors.OnSurface,
                )
                Text(
                    modifier = Modifier.testTag("$rowTag-order"),
                    text = appLanguage.pick(
                        "Order ${entry.insertionOrder}",
                        "顺序 ${entry.insertionOrder}",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = AetherColors.OnSurface,
                )
            }
            Switch(
                modifier = Modifier.testTag("$rowTag-enabled"),
                checked = entry.enabled,
                onCheckedChange = { onToggleEnabled() },
            )
            TextButton(
                modifier = Modifier.testTag("$rowTag-move-up"),
                enabled = entry.canMoveUp,
                onClick = onMoveUp,
            ) { Text("↑") }
            TextButton(
                modifier = Modifier.testTag("$rowTag-move-down"),
                enabled = entry.canMoveDown,
                onClick = onMoveDown,
            ) { Text("↓") }
            Box {
                TextButton(
                    modifier = Modifier.testTag("$rowTag-overflow"),
                    onClick = { overflowExpanded = true },
                ) {
                    Text(appLanguage.pick("More", "更多"))
                }
                DropdownMenu(
                    modifier = Modifier.testTag("$rowTag-overflow-menu"),
                    expanded = overflowExpanded,
                    onDismissRequest = { overflowExpanded = false },
                ) {
                    DropdownMenuItem(
                        modifier = Modifier.testTag("$rowTag-overflow-delete"),
                        text = { Text(appLanguage.pick("Delete", "删除")) },
                        onClick = {
                            overflowExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorldInfoEditorBindingsCard(
    bindings: List<WorldInfoEditorBindingRow>,
    pickerItems: List<WorldInfoEditorPickerItem>,
    onBind: (String) -> Unit,
    onUnbind: (String) -> Unit,
    onTogglePrimary: (String) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    var pickerExpanded by rememberSaveable { mutableStateOf(false) }
    GlassCard(modifier = Modifier.testTag("worldinfo-editor-bindings-card")) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = appLanguage.pick("Character bindings", "角色绑定"),
                    style = MaterialTheme.typography.titleSmall,
                    color = AetherColors.OnSurface,
                )
                Box {
                    OutlinedButton(
                        modifier = Modifier.testTag("worldinfo-editor-bind-button"),
                        onClick = { pickerExpanded = true },
                        enabled = pickerItems.isNotEmpty(),
                    ) {
                        Text(appLanguage.pick("Bind character", "绑定角色"))
                    }
                    DropdownMenu(
                        modifier = Modifier.testTag("worldinfo-editor-bind-menu"),
                        expanded = pickerExpanded,
                        onDismissRequest = { pickerExpanded = false },
                    ) {
                        pickerItems.forEach { item ->
                            DropdownMenuItem(
                                modifier = Modifier.testTag("worldinfo-editor-bind-option-${item.characterId}"),
                                text = { Text(item.displayName) },
                                onClick = {
                                    pickerExpanded = false
                                    onBind(item.characterId)
                                },
                            )
                        }
                    }
                }
            }
            if (bindings.isEmpty()) {
                Text(
                    modifier = Modifier.testTag("worldinfo-editor-bindings-empty"),
                    text = appLanguage.pick(
                        "No characters bound yet.",
                        "尚未绑定角色。",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
            } else {
                bindings.forEach { row ->
                    val tag = "worldinfo-editor-binding-${row.characterId}"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(tag),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                modifier = Modifier.testTag("$tag-name"),
                                text = row.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = AetherColors.OnSurface,
                            )
                            if (row.isPrimary) {
                                Text(
                                    modifier = Modifier.testTag("$tag-primary-badge"),
                                    text = appLanguage.pick("Primary (used on export)", "主要（导出时使用）"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AetherColors.OnSurface,
                                )
                            }
                        }
                        TextButton(
                            modifier = Modifier.testTag("$tag-toggle-primary"),
                            onClick = { onTogglePrimary(row.characterId) },
                        ) {
                            Text(
                                if (row.isPrimary) {
                                    appLanguage.pick("Unset primary", "取消主要")
                                } else {
                                    appLanguage.pick("Set primary", "设为主要")
                                },
                            )
                        }
                        TextButton(
                            modifier = Modifier.testTag("$tag-unbind"),
                            onClick = { onUnbind(row.characterId) },
                        ) {
                            Text(appLanguage.pick("Unbind", "解绑"))
                        }
                    }
                }
            }
        }
    }
}
