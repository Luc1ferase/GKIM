package com.gkim.im.android.feature.tavern

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.gkim.im.android.data.repository.WorldInfoRepository
import com.gkim.im.android.feature.shared.simpleViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CharacterLorebookRow(
    val lorebookId: String,
    val displayName: String,
    val entryCount: Int,
    val isPrimary: Boolean,
)

data class CharacterLorebookTabUiState(
    val characterId: String,
    val rows: List<CharacterLorebookRow> = emptyList(),
) {
    val isEmpty: Boolean get() = rows.isEmpty()
}

class CharacterLorebookTabViewModel(
    repository: WorldInfoRepository,
    private val characterId: String,
    private val language: () -> AppLanguage,
) : ViewModel() {

    val uiState: StateFlow<CharacterLorebookTabUiState> = combine(
        repository.observeLorebooks(),
        repository.observeEntries(),
        repository.observeBindings(),
    ) { lorebooks, entries, bindings ->
        val lorebookById = lorebooks.associateBy { it.id }
        val rows = bindings.values
            .flatten()
            .filter { it.characterId == characterId }
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
        CharacterLorebookTabUiState(characterId = characterId, rows = rows)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CharacterLorebookTabUiState(characterId = characterId),
    )

    private fun displayNameOf(resolved: String): String = resolved.ifBlank {
        when (language()) {
            AppLanguage.English -> "Untitled lorebook"
            AppLanguage.Chinese -> "未命名世界书"
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
