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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.PageHeader
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.LorebookEntry
import com.gkim.im.android.core.model.SecondaryGate
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

data class WorldInfoEntryEditorUiState(
    val entryId: String = "",
    val lorebookId: String = "",
    val loaded: Boolean = false,
    val englishName: String = "",
    val chineseName: String = "",
    val englishKeys: List<String> = emptyList(),
    val chineseKeys: List<String> = emptyList(),
    val englishSecondaryKeys: List<String> = emptyList(),
    val chineseSecondaryKeys: List<String> = emptyList(),
    val secondaryGate: SecondaryGate = SecondaryGate.None,
    val englishContent: String = "",
    val chineseContent: String = "",
    val enabled: Boolean = true,
    val constant: Boolean = false,
    val caseSensitive: Boolean = false,
    val scanDepth: Int = LorebookEntry.DefaultScanDepth,
    val insertionOrder: Int = 0,
    val comment: String = "",
    val errorMessage: String? = null,
    val saveCompleted: Long = 0L,
)

class WorldInfoEntryEditorViewModel(
    private val repository: WorldInfoRepository,
    private val lorebookId: String,
    private val entryId: String,
) : ViewModel() {

    private val draftState = MutableStateFlow(WorldInfoEntryEditorUiState(entryId = entryId, lorebookId = lorebookId))
    private val errorMessageState = MutableStateFlow<String?>(null)
    private val saveCompletionState = MutableStateFlow(0L)

    val uiState: StateFlow<WorldInfoEntryEditorUiState> = combine(
        draftState,
        errorMessageState,
        saveCompletionState,
    ) { draft, error, savedAt ->
        draft.copy(errorMessage = error, saveCompleted = savedAt)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = WorldInfoEntryEditorUiState(entryId = entryId, lorebookId = lorebookId),
    )

    init {
        viewModelScope.launch {
            repository.observeEntries().collect { byLorebook ->
                if (draftState.value.loaded) return@collect
                val entry = byLorebook[lorebookId]?.firstOrNull { it.id == entryId } ?: return@collect
                draftState.value = entry.toDraft()
            }
        }
    }

    fun setEnglishName(value: String) = draftState.update { it.copy(englishName = value) }
    fun setChineseName(value: String) = draftState.update { it.copy(chineseName = value) }

    fun setEnglishKeys(values: List<String>) = draftState.update { it.copy(englishKeys = values) }
    fun setChineseKeys(values: List<String>) = draftState.update { it.copy(chineseKeys = values) }

    fun addKey(language: AppLanguage, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        draftState.update { state ->
            when (language) {
                AppLanguage.English -> state.copy(englishKeys = state.englishKeys + trimmed)
                AppLanguage.Chinese -> state.copy(chineseKeys = state.chineseKeys + trimmed)
            }
        }
    }

    fun removeKey(language: AppLanguage, index: Int) {
        draftState.update { state ->
            when (language) {
                AppLanguage.English -> state.copy(englishKeys = state.englishKeys.removeAt(index))
                AppLanguage.Chinese -> state.copy(chineseKeys = state.chineseKeys.removeAt(index))
            }
        }
    }

    fun addSecondaryKey(language: AppLanguage, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        draftState.update { state ->
            when (language) {
                AppLanguage.English -> state.copy(englishSecondaryKeys = state.englishSecondaryKeys + trimmed)
                AppLanguage.Chinese -> state.copy(chineseSecondaryKeys = state.chineseSecondaryKeys + trimmed)
            }
        }
    }

    fun removeSecondaryKey(language: AppLanguage, index: Int) {
        draftState.update { state ->
            when (language) {
                AppLanguage.English -> state.copy(englishSecondaryKeys = state.englishSecondaryKeys.removeAt(index))
                AppLanguage.Chinese -> state.copy(chineseSecondaryKeys = state.chineseSecondaryKeys.removeAt(index))
            }
        }
    }

    fun setSecondaryGate(gate: SecondaryGate) = draftState.update { it.copy(secondaryGate = gate) }

    fun setEnglishContent(value: String) = draftState.update { it.copy(englishContent = value) }
    fun setChineseContent(value: String) = draftState.update { it.copy(chineseContent = value) }

    fun setEnabled(value: Boolean) = draftState.update { it.copy(enabled = value) }
    fun setConstant(value: Boolean) = draftState.update { it.copy(constant = value) }
    fun setCaseSensitive(value: Boolean) = draftState.update { it.copy(caseSensitive = value) }

    fun setScanDepth(value: Int) {
        val clamped = value.coerceIn(0, LorebookEntry.MaxServerScanDepth)
        draftState.update { it.copy(scanDepth = clamped) }
    }

    fun setInsertionOrder(value: Int) = draftState.update { it.copy(insertionOrder = value) }

    fun setComment(value: String) = draftState.update { it.copy(comment = value) }

    fun clearError() {
        errorMessageState.value = null
    }

    fun save() {
        val draft = draftState.value
        if (!draft.loaded) {
            errorMessageState.value = "Entry not loaded yet"
            return
        }
        viewModelScope.launch {
            val result = repository.updateEntry(draft.toEntry())
            when (result) {
                is WorldInfoMutationResult.Success -> {
                    errorMessageState.value = null
                    saveCompletionState.value = saveCompletionState.value + 1
                }
                is WorldInfoMutationResult.Rejected -> {
                    errorMessageState.value = when (result.reason) {
                        WorldInfoMutationResult.RejectionReason.UnknownEntry -> "Entry not found"
                        WorldInfoMutationResult.RejectionReason.UnknownLorebook -> "Lorebook not found"
                        else -> "Operation rejected"
                    }
                }
                is WorldInfoMutationResult.Failed ->
                    errorMessageState.value = result.cause.message ?: "Operation failed"
            }
        }
    }

    private fun LorebookEntry.toDraft(): WorldInfoEntryEditorUiState = WorldInfoEntryEditorUiState(
        entryId = id,
        lorebookId = lorebookId,
        loaded = true,
        englishName = name.english,
        chineseName = name.chinese,
        englishKeys = keysByLang[AppLanguage.English].orEmpty(),
        chineseKeys = keysByLang[AppLanguage.Chinese].orEmpty(),
        englishSecondaryKeys = secondaryKeysByLang[AppLanguage.English].orEmpty(),
        chineseSecondaryKeys = secondaryKeysByLang[AppLanguage.Chinese].orEmpty(),
        secondaryGate = secondaryGate,
        englishContent = content.english,
        chineseContent = content.chinese,
        enabled = enabled,
        constant = constant,
        caseSensitive = caseSensitive,
        scanDepth = scanDepth,
        insertionOrder = insertionOrder,
        comment = comment,
    )

    private fun WorldInfoEntryEditorUiState.toEntry(): LorebookEntry = LorebookEntry(
        id = entryId,
        lorebookId = lorebookId,
        name = LocalizedText(english = englishName, chinese = chineseName),
        keysByLang = mapOf(
            AppLanguage.English to englishKeys,
            AppLanguage.Chinese to chineseKeys,
        ).filterValues { it.isNotEmpty() },
        secondaryKeysByLang = mapOf(
            AppLanguage.English to englishSecondaryKeys,
            AppLanguage.Chinese to chineseSecondaryKeys,
        ).filterValues { it.isNotEmpty() },
        secondaryGate = secondaryGate,
        content = LocalizedText(english = englishContent, chinese = chineseContent),
        enabled = enabled,
        constant = constant,
        caseSensitive = caseSensitive,
        scanDepth = scanDepth,
        insertionOrder = insertionOrder,
        comment = comment,
    )
}

private fun <T> List<T>.removeAt(index: Int): List<T> =
    if (index in indices) toMutableList().also { it.removeAt(index) } else this

private fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
    value = transform(value)
}

@Composable
fun WorldInfoEntryEditorRoute(
    container: AppContainer,
    lorebookId: String,
    entryId: String,
    onBack: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    val viewModel = viewModel<WorldInfoEntryEditorViewModel>(
        key = "worldInfoEntryEditor-$lorebookId-$entryId",
        factory = simpleViewModelFactory {
            WorldInfoEntryEditorViewModel(
                repository = container.worldInfoRepository,
                lorebookId = lorebookId,
                entryId = entryId,
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted > 0L) onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("worldinfo-entry-editor-screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PageHeader(
            eyebrow = appLanguage.pick("World Info", "世界信息"),
            title = appLanguage.pick("Edit entry", "编辑条目"),
            description = appLanguage.pick(
                "Configure keys, content, and scan behavior for this lorebook entry.",
                "配置条目的关键词、内容与扫描行为。",
            ),
            leadingLabel = appLanguage.pick("Back", "返回"),
            onLeading = onBack,
        )

        uiState.errorMessage?.let { message ->
            GlassCard(modifier = Modifier.testTag("worldinfo-entry-editor-error")) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
                TextButton(
                    modifier = Modifier.testTag("worldinfo-entry-editor-error-dismiss"),
                    onClick = viewModel::clearError,
                ) {
                    Text(appLanguage.pick("Dismiss", "忽略"))
                }
            }
        }

        if (!uiState.loaded) {
            GlassCard(modifier = Modifier.testTag("worldinfo-entry-editor-loading")) {
                Text(
                    text = appLanguage.pick(
                        "Loading entry…",
                        "正在加载条目…",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
            }
            return@Column
        }

        WorldInfoEntryNameCard(
            englishName = uiState.englishName,
            chineseName = uiState.chineseName,
            onEnglishNameChange = viewModel::setEnglishName,
            onChineseNameChange = viewModel::setChineseName,
        )

        WorldInfoEntryKeyTabsCard(
            englishKeys = uiState.englishKeys,
            chineseKeys = uiState.chineseKeys,
            onAddKey = viewModel::addKey,
            onRemoveKey = viewModel::removeKey,
        )

        WorldInfoEntrySecondaryKeyTabsCard(
            englishKeys = uiState.englishSecondaryKeys,
            chineseKeys = uiState.chineseSecondaryKeys,
            gate = uiState.secondaryGate,
            onAddKey = viewModel::addSecondaryKey,
            onRemoveKey = viewModel::removeSecondaryKey,
            onGateChange = viewModel::setSecondaryGate,
        )

        WorldInfoEntryContentCard(
            englishContent = uiState.englishContent,
            chineseContent = uiState.chineseContent,
            onEnglishContentChange = viewModel::setEnglishContent,
            onChineseContentChange = viewModel::setChineseContent,
        )

        WorldInfoEntryFlagsCard(
            enabled = uiState.enabled,
            constant = uiState.constant,
            caseSensitive = uiState.caseSensitive,
            onEnabledChange = viewModel::setEnabled,
            onConstantChange = viewModel::setConstant,
            onCaseSensitiveChange = viewModel::setCaseSensitive,
        )

        WorldInfoEntryNumbersCard(
            scanDepth = uiState.scanDepth,
            insertionOrder = uiState.insertionOrder,
            onScanDepthChange = viewModel::setScanDepth,
            onInsertionOrderChange = viewModel::setInsertionOrder,
        )

        WorldInfoEntryCommentCard(
            comment = uiState.comment,
            onCommentChange = viewModel::setComment,
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("worldinfo-entry-editor-save"),
            onClick = viewModel::save,
        ) {
            Text(appLanguage.pick("Save entry", "保存条目"))
        }
    }
}

@Composable
private fun WorldInfoEntryNameCard(
    englishName: String,
    chineseName: String,
    onEnglishNameChange: (String) -> Unit,
    onChineseNameChange: (String) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    GlassCard(modifier = Modifier.testTag("worldinfo-entry-editor-name-card")) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = appLanguage.pick("Name", "名称"),
                style = MaterialTheme.typography.titleSmall,
                color = AetherColors.OnSurface,
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worldinfo-entry-editor-name-english"),
                value = englishName,
                onValueChange = onEnglishNameChange,
                label = { Text(appLanguage.pick("Name (English)", "名称 (英文)")) },
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worldinfo-entry-editor-name-chinese"),
                value = chineseName,
                onValueChange = onChineseNameChange,
                label = { Text(appLanguage.pick("Name (Chinese)", "名称 (中文)")) },
            )
        }
    }
}

@Composable
private fun WorldInfoEntryKeyTabsCard(
    englishKeys: List<String>,
    chineseKeys: List<String>,
    onAddKey: (AppLanguage, String) -> Unit,
    onRemoveKey: (AppLanguage, Int) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    GlassCard(modifier = Modifier.testTag("worldinfo-entry-editor-keys-card")) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = appLanguage.pick("Keys", "关键词"),
                style = MaterialTheme.typography.titleSmall,
                color = AetherColors.OnSurface,
            )
            LanguageKeyTabs(
                tagPrefix = "worldinfo-entry-editor-keys",
                englishKeys = englishKeys,
                chineseKeys = chineseKeys,
                onAdd = onAddKey,
                onRemove = onRemoveKey,
            )
        }
    }
}

@Composable
private fun WorldInfoEntrySecondaryKeyTabsCard(
    englishKeys: List<String>,
    chineseKeys: List<String>,
    gate: SecondaryGate,
    onAddKey: (AppLanguage, String) -> Unit,
    onRemoveKey: (AppLanguage, Int) -> Unit,
    onGateChange: (SecondaryGate) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    var gateExpanded by rememberSaveable { mutableStateOf(false) }
    GlassCard(modifier = Modifier.testTag("worldinfo-entry-editor-secondary-card")) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = appLanguage.pick("Secondary keys", "次要关键词"),
                    style = MaterialTheme.typography.titleSmall,
                    color = AetherColors.OnSurface,
                )
                Box {
                    OutlinedButton(
                        modifier = Modifier.testTag("worldinfo-entry-editor-secondary-gate"),
                        onClick = { gateExpanded = true },
                    ) {
                        Text(gate.label(appLanguage))
                    }
                    DropdownMenu(
                        modifier = Modifier.testTag("worldinfo-entry-editor-secondary-gate-menu"),
                        expanded = gateExpanded,
                        onDismissRequest = { gateExpanded = false },
                    ) {
                        SecondaryGate.values().forEach { value ->
                            DropdownMenuItem(
                                modifier = Modifier.testTag(
                                    "worldinfo-entry-editor-secondary-gate-${value.name.lowercase()}",
                                ),
                                text = { Text(value.label(appLanguage)) },
                                onClick = {
                                    gateExpanded = false
                                    onGateChange(value)
                                },
                            )
                        }
                    }
                }
            }
            LanguageKeyTabs(
                tagPrefix = "worldinfo-entry-editor-secondary",
                englishKeys = englishKeys,
                chineseKeys = chineseKeys,
                onAdd = onAddKey,
                onRemove = onRemoveKey,
            )
        }
    }
}

@Composable
private fun LanguageKeyTabs(
    tagPrefix: String,
    englishKeys: List<String>,
    chineseKeys: List<String>,
    onAdd: (AppLanguage, String) -> Unit,
    onRemove: (AppLanguage, Int) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    var selectedLanguage by rememberSaveable(tagPrefix) { mutableStateOf(AppLanguage.English) }
    var draft by rememberSaveable(tagPrefix, selectedLanguage) { mutableStateOf("") }
    val selectedIndex = if (selectedLanguage == AppLanguage.English) 0 else 1

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TabRow(
            modifier = Modifier.testTag("$tagPrefix-tabrow"),
            selectedTabIndex = selectedIndex,
        ) {
            Tab(
                modifier = Modifier.testTag("$tagPrefix-tab-english"),
                selected = selectedLanguage == AppLanguage.English,
                onClick = { selectedLanguage = AppLanguage.English },
                text = { Text(appLanguage.pick("English", "英文")) },
            )
            Tab(
                modifier = Modifier.testTag("$tagPrefix-tab-chinese"),
                selected = selectedLanguage == AppLanguage.Chinese,
                onClick = { selectedLanguage = AppLanguage.Chinese },
                text = { Text(appLanguage.pick("Chinese", "中文")) },
            )
        }
        val keys = if (selectedLanguage == AppLanguage.English) englishKeys else chineseKeys
        val listTag = "$tagPrefix-list-${if (selectedLanguage == AppLanguage.English) "english" else "chinese"}"
        Column(
            modifier = Modifier.testTag(listTag),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (keys.isEmpty()) {
                Text(
                    modifier = Modifier.testTag("$listTag-empty"),
                    text = appLanguage.pick("No keys yet.", "暂无关键词。"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
            } else {
                keys.forEachIndexed { index, key ->
                    val rowTag = "$listTag-$index"
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .testTag(rowTag),
                        color = AetherColors.SurfaceContainerLow,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.testTag("$rowTag-value"),
                                text = key,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AetherColors.OnSurface,
                            )
                            TextButton(
                                modifier = Modifier.testTag("$rowTag-remove"),
                                onClick = { onRemove(selectedLanguage, index) },
                            ) {
                                Text(appLanguage.pick("Remove", "移除"))
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .testTag("$tagPrefix-add-input"),
                value = draft,
                onValueChange = { draft = it },
                label = { Text(appLanguage.pick("New key", "新关键词")) },
                singleLine = true,
            )
            OutlinedButton(
                modifier = Modifier.testTag("$tagPrefix-add-button"),
                onClick = {
                    onAdd(selectedLanguage, draft)
                    draft = ""
                },
            ) {
                Text(appLanguage.pick("Add", "添加"))
            }
        }
    }
}

@Composable
private fun WorldInfoEntryContentCard(
    englishContent: String,
    chineseContent: String,
    onEnglishContentChange: (String) -> Unit,
    onChineseContentChange: (String) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    GlassCard(modifier = Modifier.testTag("worldinfo-entry-editor-content-card")) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = appLanguage.pick("Content", "内容"),
                style = MaterialTheme.typography.titleSmall,
                color = AetherColors.OnSurface,
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worldinfo-entry-editor-content-english"),
                value = englishContent,
                onValueChange = onEnglishContentChange,
                label = { Text(appLanguage.pick("Content (English)", "内容 (英文)")) },
                minLines = 3,
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worldinfo-entry-editor-content-chinese"),
                value = chineseContent,
                onValueChange = onChineseContentChange,
                label = { Text(appLanguage.pick("Content (Chinese)", "内容 (中文)")) },
                minLines = 3,
            )
        }
    }
}

@Composable
private fun WorldInfoEntryFlagsCard(
    enabled: Boolean,
    constant: Boolean,
    caseSensitive: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onConstantChange: (Boolean) -> Unit,
    onCaseSensitiveChange: (Boolean) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    GlassCard(modifier = Modifier.testTag("worldinfo-entry-editor-flags-card")) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = appLanguage.pick("Flags", "开关"),
                style = MaterialTheme.typography.titleSmall,
                color = AetherColors.OnSurface,
            )
            EntryFlagRow(
                testTag = "worldinfo-entry-editor-flag-enabled",
                englishLabel = "Enabled",
                chineseLabel = "启用",
                checked = enabled,
                onChange = onEnabledChange,
            )
            EntryFlagRow(
                testTag = "worldinfo-entry-editor-flag-constant",
                englishLabel = "Constant (always inject)",
                chineseLabel = "始终注入（Constant）",
                checked = constant,
                onChange = onConstantChange,
            )
            EntryFlagRow(
                testTag = "worldinfo-entry-editor-flag-case-sensitive",
                englishLabel = "Case sensitive",
                chineseLabel = "区分大小写",
                checked = caseSensitive,
                onChange = onCaseSensitiveChange,
            )
        }
    }
}

@Composable
private fun EntryFlagRow(
    testTag: String,
    englishLabel: String,
    chineseLabel: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("$testTag-row"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = appLanguage.pick(englishLabel, chineseLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = AetherColors.OnSurface,
        )
        Switch(
            modifier = Modifier.testTag(testTag),
            checked = checked,
            onCheckedChange = onChange,
        )
    }
}

@Composable
private fun WorldInfoEntryNumbersCard(
    scanDepth: Int,
    insertionOrder: Int,
    onScanDepthChange: (Int) -> Unit,
    onInsertionOrderChange: (Int) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    var scanDepthText by rememberSaveable(scanDepth) { mutableStateOf(scanDepth.toString()) }
    var insertionOrderText by rememberSaveable(insertionOrder) { mutableStateOf(insertionOrder.toString()) }
    GlassCard(modifier = Modifier.testTag("worldinfo-entry-editor-numbers-card")) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = appLanguage.pick("Scan behavior", "扫描行为"),
                style = MaterialTheme.typography.titleSmall,
                color = AetherColors.OnSurface,
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worldinfo-entry-editor-scan-depth"),
                value = scanDepthText,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    scanDepthText = digits
                    digits.toIntOrNull()?.let(onScanDepthChange)
                },
                label = {
                    Text(
                        appLanguage.pick(
                            "Scan depth (0–${LorebookEntry.MaxServerScanDepth})",
                            "扫描深度（0–${LorebookEntry.MaxServerScanDepth}）",
                        ),
                    )
                },
                singleLine = true,
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worldinfo-entry-editor-insertion-order"),
                value = insertionOrderText,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    insertionOrderText = digits
                    digits.toIntOrNull()?.let(onInsertionOrderChange)
                },
                label = { Text(appLanguage.pick("Insertion order", "插入顺序")) },
                singleLine = true,
            )
        }
    }
}

@Composable
private fun WorldInfoEntryCommentCard(
    comment: String,
    onCommentChange: (String) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    GlassCard(modifier = Modifier.testTag("worldinfo-entry-editor-comment-card")) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = appLanguage.pick("Comment", "备注"),
                style = MaterialTheme.typography.titleSmall,
                color = AetherColors.OnSurface,
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worldinfo-entry-editor-comment"),
                value = comment,
                onValueChange = onCommentChange,
                label = { Text(appLanguage.pick("Author comment", "作者备注")) },
                minLines = 2,
            )
        }
    }
}

private fun SecondaryGate.label(appLanguage: AppLanguage): String = when (this) {
    SecondaryGate.None -> appLanguage.pick("Gate: none", "门控：无")
    SecondaryGate.And -> appLanguage.pick("Gate: AND", "门控：AND")
    SecondaryGate.Or -> appLanguage.pick("Gate: OR", "门控：OR")
}
