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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
fun WorldInfoLibraryRoute(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenLorebook: (String) -> Unit = {},
) {
    val appLanguage = LocalAppLanguage.current
    val viewModel = viewModel<WorldInfoLibraryViewModel>(
        key = "worldInfoLibrary",
        factory = simpleViewModelFactory {
            WorldInfoLibraryViewModel(
                repository = container.worldInfoRepository,
                language = { appLanguage },
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOwnerId = container.sessionStore.username.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("worldinfo-library-screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PageHeader(
            eyebrow = appLanguage.pick("Settings", "设置"),
            title = appLanguage.pick("World Info", "世界信息"),
            description = appLanguage.pick(
                "Manage lorebooks bound to companion characters.",
                "管理绑定到伙伴角色的世界书。",
            ),
            leadingLabel = appLanguage.pick("Back", "返回"),
            onLeading = onBack,
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("worldinfo-library-create"),
            onClick = { viewModel.createLorebook(currentOwnerId) },
        ) {
            Text(appLanguage.pick("Create lorebook", "新建世界书"))
        }

        uiState.errorMessage?.let { message ->
            GlassCard(modifier = Modifier.testTag("worldinfo-library-error")) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
                TextButton(
                    modifier = Modifier.testTag("worldinfo-library-error-dismiss"),
                    onClick = viewModel::clearError,
                ) {
                    Text(appLanguage.pick("Dismiss", "忽略"))
                }
            }
        }

        if (uiState.rows.isEmpty()) {
            GlassCard(modifier = Modifier.testTag("worldinfo-library-empty")) {
                Text(
                    text = appLanguage.pick(
                        "No lorebooks yet. Create one to start binding world info to characters.",
                        "暂无世界书。创建世界书后即可绑定到角色。",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.rows.forEach { row ->
                    WorldInfoLibraryRowCard(
                        row = row,
                        onOpen = { onOpenLorebook(row.lorebookId) },
                        onDuplicate = { viewModel.duplicate(row.lorebookId) },
                        onDelete = { viewModel.delete(row.lorebookId) },
                        onToggleGlobal = { viewModel.toggleGlobal(row.lorebookId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorldInfoLibraryRowCard(
    row: WorldInfoLibraryRow,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onToggleGlobal: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    var overflowExpanded by rememberSaveable(row.lorebookId) { mutableStateOf(false) }
    val rowTestTag = "worldinfo-library-row-${row.lorebookId}"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .testTag(rowTestTag),
        color = AetherColors.SurfaceContainerLow,
        onClick = onOpen,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.7f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    modifier = Modifier.testTag("$rowTestTag-name"),
                    text = row.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = AetherColors.OnSurface,
                )
                Text(
                    modifier = Modifier.testTag("$rowTestTag-entry-count"),
                    text = appLanguage.pick(
                        "${row.entryCount} entries",
                        "${row.entryCount} 条目",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = AetherColors.OnSurface,
                )
                if (row.isGlobal) {
                    Surface(
                        modifier = Modifier.testTag("$rowTestTag-global-badge"),
                        color = AetherColors.SurfaceContainerHigh,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            text = appLanguage.pick("Global", "全局"),
                            style = MaterialTheme.typography.labelSmall,
                            color = AetherColors.OnSurface,
                        )
                    }
                }
                if (row.hasBindings) {
                    Text(
                        modifier = Modifier.testTag("$rowTestTag-bound-count"),
                        text = appLanguage.pick(
                            "Bound to ${row.boundCharacterCount} character(s)",
                            "已绑定 ${row.boundCharacterCount} 个角色",
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = AetherColors.OnSurface,
                    )
                }
            }
            Box {
                TextButton(
                    modifier = Modifier.testTag("$rowTestTag-overflow"),
                    onClick = { overflowExpanded = true },
                ) {
                    Text(appLanguage.pick("More", "更多"))
                }
                DropdownMenu(
                    modifier = Modifier.testTag("$rowTestTag-overflow-menu"),
                    expanded = overflowExpanded,
                    onDismissRequest = { overflowExpanded = false },
                ) {
                    DropdownMenuItem(
                        modifier = Modifier.testTag("$rowTestTag-overflow-duplicate"),
                        text = { Text(appLanguage.pick("Duplicate", "复制")) },
                        enabled = row.canDuplicate,
                        onClick = {
                            overflowExpanded = false
                            onDuplicate()
                        },
                    )
                    DropdownMenuItem(
                        modifier = Modifier.testTag("$rowTestTag-overflow-toggle-global"),
                        text = {
                            Text(
                                if (row.isGlobal) {
                                    appLanguage.pick("Unset global", "取消全局")
                                } else {
                                    appLanguage.pick("Set global", "设为全局")
                                },
                            )
                        },
                        enabled = row.canToggleGlobal,
                        onClick = {
                            overflowExpanded = false
                            onToggleGlobal()
                        },
                    )
                    DropdownMenuItem(
                        modifier = Modifier.testTag("$rowTestTag-overflow-delete"),
                        text = { Text(appLanguage.pick("Delete", "删除")) },
                        enabled = row.canDelete,
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
