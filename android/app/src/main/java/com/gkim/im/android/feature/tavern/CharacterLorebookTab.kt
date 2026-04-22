package com.gkim.im.android.feature.tavern

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.resolve
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.WorldInfoMutationResult
import com.gkim.im.android.data.repository.WorldInfoRepository
import com.gkim.im.android.feature.shared.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CharacterLorebookRow(
    val lorebookId: String,
    val displayName: String,
    val entryCount: Int,
    val isPrimary: Boolean,
)

data class CharacterLorebookPickerItem(
    val lorebookId: String,
    val displayName: String,
)

data class CharacterLorebookTabUiState(
    val characterId: String,
    val rows: List<CharacterLorebookRow> = emptyList(),
    val pickerItems: List<CharacterLorebookPickerItem> = emptyList(),
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean get() = rows.isEmpty()
    val canBind: Boolean get() = pickerItems.isNotEmpty()
}

class CharacterLorebookTabViewModel(
    private val repository: WorldInfoRepository,
    private val characterId: String,
    private val language: () -> AppLanguage,
) : ViewModel() {

    private val errorMessageState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CharacterLorebookTabUiState> = combine(
        repository.observeLorebooks(),
        repository.observeEntries(),
        repository.observeBindings(),
        errorMessageState,
    ) { lorebooks, entries, bindings, errorMessage ->
        val lorebookById = lorebooks.associateBy { it.id }
        val characterBindings = bindings.values.flatten().filter { it.characterId == characterId }
        val boundLorebookIds = characterBindings.map { it.lorebookId }.toSet()
        val rows = characterBindings
            .mapNotNull { binding ->
                val lorebook = lorebookById[binding.lorebookId] ?: return@mapNotNull null
                CharacterLorebookRow(
                    lorebookId = lorebook.id,
                    displayName = displayNameOf(lorebook.displayName.resolve(language())),
                    entryCount = entries[lorebook.id].orEmpty().size,
                    isPrimary = binding.isPrimary,
                )
            }
            .sortedWith(compareByDescending<CharacterLorebookRow> { it.isPrimary }.thenBy { it.displayName })
        val pickerItems = lorebooks
            .filterNot { it.id in boundLorebookIds }
            .map {
                CharacterLorebookPickerItem(
                    lorebookId = it.id,
                    displayName = displayNameOf(it.displayName.resolve(language())),
                )
            }
            .sortedBy { it.displayName }
        CharacterLorebookTabUiState(
            characterId = characterId,
            rows = rows,
            pickerItems = pickerItems,
            errorMessage = errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CharacterLorebookTabUiState(characterId = characterId),
    )

    fun bind(lorebookId: String) {
        viewModelScope.launch {
            val outcome = repository.bind(
                lorebookId = lorebookId,
                characterId = characterId,
                isPrimary = false,
            )
            when (outcome) {
                is WorldInfoMutationResult.Success -> errorMessageState.value = null
                is WorldInfoMutationResult.Rejected -> errorMessageState.value = rejectionMessage(outcome.reason)
                is WorldInfoMutationResult.Failed -> errorMessageState.value = when (language()) {
                    AppLanguage.English -> "Bind failed"
                    AppLanguage.Chinese -> "绑定失败"
                }
            }
        }
    }

    fun clearError() {
        errorMessageState.value = null
    }

    private fun displayNameOf(resolved: String): String = resolved.ifBlank {
        when (language()) {
            AppLanguage.English -> "Untitled lorebook"
            AppLanguage.Chinese -> "未命名世界书"
        }
    }

    private fun rejectionMessage(reason: WorldInfoMutationResult.RejectionReason): String = when (language()) {
        AppLanguage.English -> when (reason) {
            WorldInfoMutationResult.RejectionReason.UnknownLorebook -> "Lorebook not found"
            WorldInfoMutationResult.RejectionReason.BindingAlreadyExists -> "Lorebook already bound"
            else -> "Operation rejected"
        }
        AppLanguage.Chinese -> when (reason) {
            WorldInfoMutationResult.RejectionReason.UnknownLorebook -> "未找到世界书"
            WorldInfoMutationResult.RejectionReason.BindingAlreadyExists -> "世界书已绑定"
            else -> "操作被拒绝"
        }
    }
}

@Composable
fun CharacterLorebookTabSection(
    container: AppContainer,
    characterId: String,
    onManageLorebook: (String) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    val viewModel = viewModel<CharacterLorebookTabViewModel>(
        key = "characterLorebookTab-$characterId",
        factory = simpleViewModelFactory {
            CharacterLorebookTabViewModel(
                repository = container.worldInfoRepository,
                characterId = characterId,
                language = { appLanguage },
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GlassCard(modifier = Modifier.testTag("character-detail-lorebook-tab")) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = appLanguage.pick("Bound lorebooks", "已绑定世界书"),
                style = MaterialTheme.typography.titleMedium,
                color = AetherColors.OnSurface,
            )
            uiState.errorMessage?.let { message ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("character-detail-lorebook-error"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = AetherColors.Primary,
                    )
                    TextButton(
                        modifier = Modifier.testTag("character-detail-lorebook-error-dismiss"),
                        onClick = { viewModel.clearError() },
                    ) {
                        Text(appLanguage.pick("Dismiss", "关闭"))
                    }
                }
            }
            if (uiState.isEmpty) {
                Text(
                    modifier = Modifier.testTag("character-detail-lorebook-empty"),
                    text = appLanguage.pick(
                        "No lorebooks bound yet.",
                        "尚未绑定任何世界书。",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurfaceVariant,
                )
            } else {
                uiState.rows.forEach { row ->
                    CharacterLorebookRowView(row = row, onManage = onManageLorebook)
                }
            }
            BindLorebookControl(
                pickerItems = uiState.pickerItems,
                onBind = viewModel::bind,
                emptyLabelTag = if (uiState.isEmpty) "character-detail-lorebook-bind-cta" else "character-detail-lorebook-bind-another",
            )
        }
    }
}

@Composable
private fun BindLorebookControl(
    pickerItems: List<CharacterLorebookPickerItem>,
    onBind: (String) -> Unit,
    emptyLabelTag: String,
) {
    val appLanguage = LocalAppLanguage.current
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(
            modifier = Modifier.testTag(emptyLabelTag),
            enabled = pickerItems.isNotEmpty(),
            onClick = { expanded = true },
        ) {
            Text(appLanguage.pick("Bind a lorebook", "绑定世界书"))
        }
        DropdownMenu(
            modifier = Modifier.testTag("character-detail-lorebook-picker"),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (pickerItems.isEmpty()) {
                DropdownMenuItem(
                    modifier = Modifier.testTag("character-detail-lorebook-picker-empty"),
                    text = {
                        Text(
                            appLanguage.pick(
                                "No lorebooks available",
                                "暂无可绑定世界书",
                            ),
                        )
                    },
                    onClick = { expanded = false },
                    enabled = false,
                )
            } else {
                pickerItems.forEach { item ->
                    DropdownMenuItem(
                        modifier = Modifier.testTag("character-detail-lorebook-picker-${item.lorebookId}"),
                        text = { Text(item.displayName) },
                        onClick = {
                            expanded = false
                            onBind(item.lorebookId)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterLorebookRowView(
    row: CharacterLorebookRow,
    onManage: (String) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    val tag = "character-detail-lorebook-row-${row.lorebookId}"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                modifier = Modifier.testTag("$tag-name"),
                text = row.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = AetherColors.OnSurface,
            )
            Text(
                modifier = Modifier.testTag("$tag-entry-count"),
                text = appLanguage.pick(
                    "${row.entryCount} entries",
                    "${row.entryCount} 条",
                ),
                style = MaterialTheme.typography.labelSmall,
                color = AetherColors.OnSurfaceVariant,
            )
            if (row.isPrimary) {
                Text(
                    modifier = Modifier.testTag("$tag-primary-badge"),
                    text = appLanguage.pick("Primary", "主要"),
                    style = MaterialTheme.typography.labelSmall,
                    color = AetherColors.Primary,
                )
            }
        }
        TextButton(
            modifier = Modifier.testTag("$tag-manage"),
            onClick = { onManage(row.lorebookId) },
        ) {
            Text(appLanguage.pick("Manage in library", "前往管理"))
        }
    }
}
