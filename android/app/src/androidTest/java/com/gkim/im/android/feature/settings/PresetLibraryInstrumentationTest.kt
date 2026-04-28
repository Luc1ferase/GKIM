package com.gkim.im.android.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Preset
import com.gkim.im.android.core.model.PresetParams
import com.gkim.im.android.core.model.PresetTemplate
import com.gkim.im.android.core.model.resolve
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PresetLibraryInstrumentationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun builtInPresetIsMarkedActiveOnOpen() {
        composeRule.setContent {
            TestablePresetsScreen(initialLibrary = sampleLibrary())
        }

        composeRule.onNodeWithTag("settings-detail-presets").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-presets-active-preset-builtin-default").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-presets-active-preset-warm").assertIsNotDisplayed()
    }

    @Test
    fun builtInPresetEditButtonIsDisabled() {
        composeRule.setContent {
            TestablePresetsScreen(initialLibrary = sampleLibrary())
        }

        composeRule.onNodeWithTag("settings-presets-edit-preset-builtin-default").assertIsNotEnabled()
        composeRule.onNodeWithTag("settings-presets-edit-preset-warm").assertIsEnabled()
    }

    @Test
    fun builtInPresetDeleteButtonIsDisabled() {
        composeRule.setContent {
            composeRule.mainClock.autoAdvance = true
            TestablePresetsScreen(initialLibrary = sampleLibrary())
        }

        composeRule.onNodeWithTag("settings-presets-delete-preset-builtin-default").assertIsNotEnabled()
    }

    @Test
    fun newPresetButtonOpensEditorInCreateMode() {
        composeRule.setContent {
            TestablePresetsScreen(initialLibrary = sampleLibrary())
        }

        composeRule.onNodeWithTag("settings-presets-new").performClick()
        composeRule.onNodeWithTag("preset-editor-create-mode").assertIsDisplayed()
        composeRule.onNodeWithTag("preset-editor-displayname-en").performTextInput("Brisk")
        composeRule.onNodeWithTag("preset-editor-displayname-zh").performTextInput("轻快")
        composeRule.onNodeWithTag("preset-editor-description-en").performTextInput("Punchy replies.")
        composeRule.onNodeWithTag("preset-editor-description-zh").performTextInput("有力的回复。")
        composeRule.onNodeWithTag("preset-editor-save").performClick()

        composeRule.onNodeWithTag("settings-presets-card-preset-brisk").assertIsDisplayed()
    }

    @Test
    fun duplicateBuiltInCreatesCopyInLibrary() {
        composeRule.setContent {
            TestablePresetsScreen(initialLibrary = sampleLibrary())
        }

        composeRule.onNodeWithTag("settings-presets-duplicate-preset-builtin-default").performClick()
        composeRule.onNodeWithTag("settings-presets-card-preset-builtin-default-copy").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-presets-builtin-preset-builtin-default-copy").assertIsNotDisplayed()
    }

    @Test
    fun activateNewPresetMovesActiveBadge() {
        composeRule.setContent {
            TestablePresetsScreen(initialLibrary = sampleLibrary())
        }

        composeRule.onNodeWithTag("settings-presets-active-preset-builtin-default").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-presets-activate-preset-warm").performClick()

        composeRule.onNodeWithTag("settings-presets-active-preset-warm").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-presets-active-preset-builtin-default").assertIsNotDisplayed()
    }

    @Test
    fun deleteButtonIsDisabledForActivePreset() {
        composeRule.setContent {
            TestablePresetsScreen(initialLibrary = sampleLibrary())
        }

        composeRule.onNodeWithTag("settings-presets-delete-preset-builtin-default").assertIsNotEnabled()
        composeRule.onNodeWithTag("settings-presets-delete-preset-warm").assertIsEnabled()
    }

    @Test
    fun deleteInactiveUserPresetRemovesItFromList() {
        composeRule.setContent {
            TestablePresetsScreen(initialLibrary = sampleLibrary())
        }

        composeRule.onNodeWithTag("settings-presets-card-preset-warm").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-presets-delete-preset-warm").performClick()
        composeRule.onNodeWithTag("settings-presets-card-preset-warm").assertIsNotDisplayed()
        composeRule.onNodeWithTag("settings-presets-card-preset-builtin-default").assertIsDisplayed()
    }

    @Test
    fun builtInBadgeRendersForSeedPreset() {
        composeRule.setContent {
            TestablePresetsScreen(initialLibrary = sampleLibrary())
        }

        composeRule.onNodeWithTag("settings-presets-builtin-preset-builtin-default").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-presets-builtin-preset-warm").assertIsNotDisplayed()
    }

    @Test
    fun editUserPresetPersistsAfterSave() {
        composeRule.setContent {
            TestablePresetsScreen(initialLibrary = sampleLibrary())
        }

        composeRule.onNodeWithTag("settings-presets-edit-preset-warm").performClick()
        composeRule.onNodeWithTag("preset-editor-edit-mode").assertIsDisplayed()
        composeRule.onNodeWithTag("preset-editor-description-en").performTextClearance()
        composeRule.onNodeWithTag("preset-editor-description-en").performTextInput("Renamed warmth")
        composeRule.onNodeWithTag("preset-editor-description-zh").performTextClearance()
        composeRule.onNodeWithTag("preset-editor-description-zh").performTextInput("改写过的温暖")
        composeRule.onNodeWithTag("preset-editor-save").performClick()

        composeRule.onNodeWithTag("settings-presets-description-preview-preset-warm-en").assertIsDisplayed()
    }

    private fun sampleLibrary(): List<Preset> = listOf(
        Preset(
            id = "preset-builtin-default",
            displayName = LocalizedText("Default", "默认"),
            description = LocalizedText("Neutral companion voice.", "中立的伙伴口吻。"),
            template = PresetTemplate(
                systemPrefix = LocalizedText("You are a helpful companion.", "你是乐于助人的伙伴。"),
                systemSuffix = LocalizedText("", ""),
                formatInstructions = LocalizedText("", ""),
                postHistoryInstructions = LocalizedText("", ""),
            ),
            params = PresetParams(temperature = 0.7, topP = 0.9, maxReplyTokens = 512),
            isBuiltIn = true,
            isActive = true,
            createdAt = 0L,
            updatedAt = 0L,
        ),
        Preset(
            id = "preset-warm",
            displayName = LocalizedText("Warm", "温暖"),
            description = LocalizedText("A warmer tone.", "更温暖的口吻。"),
            template = PresetTemplate(
                systemPrefix = LocalizedText("Be warm and encouraging.", "要温暖并鼓励对方。"),
                systemSuffix = LocalizedText("", ""),
                formatInstructions = LocalizedText("", ""),
                postHistoryInstructions = LocalizedText("", ""),
            ),
            params = PresetParams(temperature = 0.8, topP = 0.95, maxReplyTokens = 768),
            isBuiltIn = false,
            isActive = false,
            createdAt = 10L,
            updatedAt = 10L,
        ),
    )
}

private enum class PresetEditorMode { Hidden, Edit, Create }

@Composable
private fun TestablePresetsScreen(initialLibrary: List<Preset>) {
    var library by remember { mutableStateOf(initialLibrary) }
    var editorMode by remember { mutableStateOf(PresetEditorMode.Hidden) }
    var editingPresetId by remember { mutableStateOf<String?>(null) }

    when (editorMode) {
        PresetEditorMode.Hidden -> TestablePresetsList(
            library = library,
            onEdit = { id ->
                editingPresetId = id
                editorMode = PresetEditorMode.Edit
            },
            onNew = {
                editingPresetId = null
                editorMode = PresetEditorMode.Create
            },
            onActivate = { id ->
                library = library.map { it.copy(isActive = it.id == id) }
            },
            onDuplicate = { id ->
                val source = library.first { it.id == id }
                val copyId = "${source.id}-copy"
                library = library + source.copy(
                    id = copyId,
                    displayName = LocalizedText(
                        "${source.displayName.english} (copy)",
                        "${source.displayName.chinese} (副本)",
                    ),
                    isBuiltIn = false,
                    isActive = false,
                )
            },
            onDelete = { id ->
                library = library.filterNot { it.id == id }
            },
        )
        PresetEditorMode.Edit -> {
            val existing = library.first { it.id == editingPresetId }
            TestablePresetEditor(
                mode = PresetEditorMode.Edit,
                initialDisplayNameEn = existing.displayName.english,
                initialDisplayNameZh = existing.displayName.chinese,
                initialDescriptionEn = existing.description.english,
                initialDescriptionZh = existing.description.chinese,
                onSave = { dnEn, dnZh, dcEn, dcZh ->
                    library = library.map { preset ->
                        if (preset.id == existing.id) {
                            preset.copy(
                                displayName = LocalizedText(dnEn, dnZh),
                                description = LocalizedText(dcEn, dcZh),
                            )
                        } else {
                            preset
                        }
                    }
                    editorMode = PresetEditorMode.Hidden
                },
                onCancel = { editorMode = PresetEditorMode.Hidden },
            )
        }
        PresetEditorMode.Create -> TestablePresetEditor(
            mode = PresetEditorMode.Create,
            initialDisplayNameEn = "",
            initialDisplayNameZh = "",
            initialDescriptionEn = "",
            initialDescriptionZh = "",
            onSave = { dnEn, dnZh, dcEn, dcZh ->
                val id = "preset-${dnEn.lowercase()}"
                library = library + Preset(
                    id = id,
                    displayName = LocalizedText(dnEn, dnZh),
                    description = LocalizedText(dcEn, dcZh),
                    isBuiltIn = false,
                    isActive = false,
                )
                editorMode = PresetEditorMode.Hidden
            },
            onCancel = { editorMode = PresetEditorMode.Hidden },
        )
    }
}

@Composable
private fun TestablePresetsList(
    library: List<Preset>,
    onEdit: (String) -> Unit,
    onNew: () -> Unit,
    onActivate: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("settings-detail-presets"),
    ) {
        Text(
            text = "New preset",
            modifier = Modifier
                .testTag("settings-presets-new")
                .clickable { onNew() },
        )
        library.forEach { preset ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-presets-card-${preset.id}"),
            ) {
                Row {
                    Text(text = preset.displayName.resolve(AppLanguage.English))
                    if (preset.isActive) {
                        Text(
                            text = "ACTIVE",
                            modifier = Modifier.testTag("settings-presets-active-${preset.id}"),
                        )
                    }
                    if (preset.isBuiltIn) {
                        Text(
                            text = "BUILT-IN",
                            modifier = Modifier.testTag("settings-presets-builtin-${preset.id}"),
                        )
                    }
                }
                Text(
                    text = preset.description.resolve(AppLanguage.English),
                    modifier = Modifier.testTag("settings-presets-description-${preset.id}"),
                )
                Text(
                    text = preset.description.english,
                    modifier = Modifier.testTag("settings-presets-description-preview-${preset.id}-en"),
                )
                Row {
                    OutlinedButton(
                        onClick = { onActivate(preset.id) },
                        enabled = !preset.isActive,
                        modifier = Modifier.testTag("settings-presets-activate-${preset.id}"),
                    ) { Text("Activate") }
                    OutlinedButton(
                        onClick = { onEdit(preset.id) },
                        enabled = !preset.isBuiltIn,
                        modifier = Modifier.testTag("settings-presets-edit-${preset.id}"),
                    ) { Text("Edit") }
                    OutlinedButton(
                        onClick = { onDuplicate(preset.id) },
                        modifier = Modifier.testTag("settings-presets-duplicate-${preset.id}"),
                    ) { Text("Duplicate") }
                    OutlinedButton(
                        onClick = { onDelete(preset.id) },
                        enabled = !preset.isBuiltIn && !preset.isActive,
                        modifier = Modifier.testTag("settings-presets-delete-${preset.id}"),
                    ) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun TestablePresetEditor(
    mode: PresetEditorMode,
    initialDisplayNameEn: String,
    initialDisplayNameZh: String,
    initialDescriptionEn: String,
    initialDescriptionZh: String,
    onSave: (String, String, String, String) -> Unit,
    onCancel: () -> Unit,
) {
    var displayNameEn by remember { mutableStateOf(initialDisplayNameEn) }
    var displayNameZh by remember { mutableStateOf(initialDisplayNameZh) }
    var descriptionEn by remember { mutableStateOf(initialDescriptionEn) }
    var descriptionZh by remember { mutableStateOf(initialDescriptionZh) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("preset-editor-${if (mode == PresetEditorMode.Create) "create" else "edit"}-mode"),
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = displayNameEn,
            onValueChange = { displayNameEn = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("preset-editor-displayname-en"),
        )
        androidx.compose.material3.OutlinedTextField(
            value = displayNameZh,
            onValueChange = { displayNameZh = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("preset-editor-displayname-zh"),
        )
        androidx.compose.material3.OutlinedTextField(
            value = descriptionEn,
            onValueChange = { descriptionEn = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("preset-editor-description-en"),
        )
        androidx.compose.material3.OutlinedTextField(
            value = descriptionZh,
            onValueChange = { descriptionZh = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("preset-editor-description-zh"),
        )
        Row {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.testTag("preset-editor-cancel"),
            ) { Text("Cancel") }
            OutlinedButton(
                onClick = { onSave(displayNameEn, displayNameZh, descriptionEn, descriptionZh) },
                enabled = displayNameEn.isNotBlank() && displayNameZh.isNotBlank(),
                modifier = Modifier.testTag("preset-editor-save"),
            ) { Text("Save") }
        }
    }
}
