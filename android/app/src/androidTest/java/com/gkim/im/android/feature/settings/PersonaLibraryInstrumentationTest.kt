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
import com.gkim.im.android.core.model.UserPersona
import com.gkim.im.android.core.model.resolve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonaLibraryInstrumentationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun builtInPersonaIsMarkedActiveOnOpen() {
        composeRule.setContent {
            TestablePersonasScreen(initialLibrary = sampleLibrary())
        }

        composeRule.onNodeWithTag("settings-detail-personas").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-personas-active-persona-builtin-default").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-personas-active-persona-auric").assertIsNotDisplayed()
    }

    @Test
    fun editBuiltInDescriptionPersistsAfterSave() {
        val library = sampleLibrary()

        composeRule.setContent {
            TestablePersonasScreen(initialLibrary = library)
        }

        composeRule.onNodeWithTag("settings-personas-edit-persona-builtin-default").performClick()
        composeRule.onNodeWithTag("persona-editor-description-en").performTextClearance()
        composeRule.onNodeWithTag("persona-editor-description-en").performTextInput("Renamed traveller")
        composeRule.onNodeWithTag("persona-editor-description-zh").performTextClearance()
        composeRule.onNodeWithTag("persona-editor-description-zh").performTextInput("重命名的旅人")
        composeRule.onNodeWithTag("persona-editor-save").performClick()

        composeRule.onNodeWithTag("settings-personas-description-persona-builtin-default")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("persona-description-preview-persona-builtin-default-en")
            .assertIsDisplayed()
    }

    @Test
    fun newPersonaButtonOpensEditorInCreateMode() {
        composeRule.setContent {
            TestablePersonasScreen(initialLibrary = sampleLibrary())
        }

        composeRule.onNodeWithTag("settings-personas-new").performClick()
        composeRule.onNodeWithTag("persona-editor-create-mode").assertIsDisplayed()
        composeRule.onNodeWithTag("persona-editor-displayname-en").performTextInput("Kestrel")
        composeRule.onNodeWithTag("persona-editor-displayname-zh").performTextInput("红隼")
        composeRule.onNodeWithTag("persona-editor-description-en").performTextInput("A watcher.")
        composeRule.onNodeWithTag("persona-editor-description-zh").performTextInput("一名观察者。")
        composeRule.onNodeWithTag("persona-editor-save").performClick()

        composeRule.onNodeWithTag("settings-personas-card-persona-kestrel").assertIsDisplayed()
    }

    @Test
    fun activateNewPersonaMovesActiveBadge() {
        composeRule.setContent {
            TestablePersonasScreen(initialLibrary = sampleLibrary())
        }

        composeRule.onNodeWithTag("settings-personas-active-persona-builtin-default")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("settings-personas-activate-persona-auric").performClick()

        composeRule.onNodeWithTag("settings-personas-active-persona-auric").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-personas-active-persona-builtin-default")
            .assertIsNotDisplayed()
    }

    @Test
    fun deleteButtonIsDisabledForActivePersona() {
        composeRule.setContent {
            TestablePersonasScreen(initialLibrary = sampleLibrary())
        }

        composeRule.onNodeWithTag("settings-personas-delete-persona-builtin-default")
            .assertIsNotEnabled()
        composeRule.onNodeWithTag("settings-personas-delete-persona-auric").assertIsEnabled()
    }

    @Test
    fun deleteInactiveUserPersonaRemovesItFromList() {
        composeRule.setContent {
            TestablePersonasScreen(initialLibrary = sampleLibrary())
        }

        composeRule.onNodeWithTag("settings-personas-card-persona-auric").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-personas-delete-persona-auric").performClick()
        composeRule.onNodeWithTag("settings-personas-card-persona-auric").assertIsNotDisplayed()
        composeRule.onNodeWithTag("settings-personas-card-persona-builtin-default")
            .assertIsDisplayed()
    }

    @Test
    fun builtInBadgeRendersForSeedPersona() {
        composeRule.setContent {
            TestablePersonasScreen(initialLibrary = sampleLibrary())
        }

        composeRule.onNodeWithTag("settings-personas-builtin-persona-builtin-default")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("settings-personas-builtin-persona-auric").assertIsNotDisplayed()
    }

    private fun sampleLibrary(): List<UserPersona> = listOf(
        UserPersona(
            id = "persona-builtin-default",
            displayName = LocalizedText("You", "你"),
            description = LocalizedText("A neutral traveller.", "一位中立的旅人。"),
            isBuiltIn = true,
            isActive = true,
        ),
        UserPersona(
            id = "persona-auric",
            displayName = LocalizedText("Auric", "金辉"),
            description = LocalizedText("A wandering archivist.", "一位游走的档案员。"),
            isBuiltIn = false,
            isActive = false,
        ),
    )
}

private enum class EditorMode { Hidden, Edit, Create }

@Composable
private fun TestablePersonasScreen(initialLibrary: List<UserPersona>) {
    var library by remember { mutableStateOf(initialLibrary) }
    var editorMode by remember { mutableStateOf(EditorMode.Hidden) }
    var editingPersonaId by remember { mutableStateOf<String?>(null) }

    when (editorMode) {
        EditorMode.Hidden -> TestablePersonasList(
            library = library,
            onEdit = { id ->
                editingPersonaId = id
                editorMode = EditorMode.Edit
            },
            onNew = {
                editingPersonaId = null
                editorMode = EditorMode.Create
            },
            onActivate = { id ->
                library = library.map { it.copy(isActive = it.id == id) }
            },
            onDelete = { id ->
                library = library.filterNot { it.id == id }
            },
        )
        EditorMode.Edit -> {
            val existing = library.first { it.id == editingPersonaId }
            TestablePersonaEditor(
                mode = EditorMode.Edit,
                initialDisplayNameEn = existing.displayName.english,
                initialDisplayNameZh = existing.displayName.chinese,
                initialDescriptionEn = existing.description.english,
                initialDescriptionZh = existing.description.chinese,
                onSave = { dnEn, dnZh, dcEn, dcZh ->
                    library = library.map { persona ->
                        if (persona.id == existing.id) {
                            persona.copy(
                                displayName = LocalizedText(dnEn, dnZh),
                                description = LocalizedText(dcEn, dcZh),
                            )
                        } else {
                            persona
                        }
                    }
                    editorMode = EditorMode.Hidden
                },
                onCancel = { editorMode = EditorMode.Hidden },
            )
        }
        EditorMode.Create -> TestablePersonaEditor(
            mode = EditorMode.Create,
            initialDisplayNameEn = "",
            initialDisplayNameZh = "",
            initialDescriptionEn = "",
            initialDescriptionZh = "",
            onSave = { dnEn, dnZh, dcEn, dcZh ->
                val id = "persona-${dnEn.lowercase()}"
                library = library + UserPersona(
                    id = id,
                    displayName = LocalizedText(dnEn, dnZh),
                    description = LocalizedText(dcEn, dcZh),
                    isBuiltIn = false,
                    isActive = false,
                )
                editorMode = EditorMode.Hidden
            },
            onCancel = { editorMode = EditorMode.Hidden },
        )
    }
}

@Composable
private fun TestablePersonasList(
    library: List<UserPersona>,
    onEdit: (String) -> Unit,
    onNew: () -> Unit,
    onActivate: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("settings-detail-personas"),
    ) {
        Text(
            text = "New persona",
            modifier = Modifier
                .testTag("settings-personas-new")
                .clickable { onNew() },
        )
        library.forEach { persona ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-personas-card-${persona.id}"),
            ) {
                Row {
                    Text(text = persona.displayName.resolve(AppLanguage.English))
                    if (persona.isActive) {
                        Text(
                            text = "ACTIVE",
                            modifier = Modifier.testTag("settings-personas-active-${persona.id}"),
                        )
                    }
                    if (persona.isBuiltIn) {
                        Text(
                            text = "BUILT-IN",
                            modifier = Modifier.testTag("settings-personas-builtin-${persona.id}"),
                        )
                    }
                }
                Text(
                    text = persona.description.resolve(AppLanguage.English),
                    modifier = Modifier.testTag("settings-personas-description-${persona.id}"),
                )
                Text(
                    text = persona.description.english,
                    modifier = Modifier.testTag("persona-description-preview-${persona.id}-en"),
                )
                Row {
                    OutlinedButton(
                        onClick = { onActivate(persona.id) },
                        enabled = !persona.isActive,
                        modifier = Modifier.testTag("settings-personas-activate-${persona.id}"),
                    ) { Text("Activate") }
                    OutlinedButton(
                        onClick = { onEdit(persona.id) },
                        modifier = Modifier.testTag("settings-personas-edit-${persona.id}"),
                    ) { Text("Edit") }
                    OutlinedButton(
                        onClick = { onDelete(persona.id) },
                        enabled = !persona.isBuiltIn && !persona.isActive,
                        modifier = Modifier.testTag("settings-personas-delete-${persona.id}"),
                    ) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun TestablePersonaEditor(
    mode: EditorMode,
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
            .testTag("persona-editor-${if (mode == EditorMode.Create) "create" else "edit"}-mode"),
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = displayNameEn,
            onValueChange = { displayNameEn = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("persona-editor-displayname-en"),
        )
        androidx.compose.material3.OutlinedTextField(
            value = displayNameZh,
            onValueChange = { displayNameZh = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("persona-editor-displayname-zh"),
        )
        androidx.compose.material3.OutlinedTextField(
            value = descriptionEn,
            onValueChange = { descriptionEn = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("persona-editor-description-en"),
        )
        androidx.compose.material3.OutlinedTextField(
            value = descriptionZh,
            onValueChange = { descriptionZh = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("persona-editor-description-zh"),
        )
        Row {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.testTag("persona-editor-cancel"),
            ) { Text("Cancel") }
            OutlinedButton(
                onClick = { onSave(displayNameEn, displayNameZh, descriptionEn, descriptionZh) },
                enabled = displayNameEn.isNotBlank() &&
                    displayNameZh.isNotBlank() &&
                    descriptionEn.isNotBlank() &&
                    descriptionZh.isNotBlank(),
                modifier = Modifier.testTag("persona-editor-save"),
            ) { Text("Save") }
        }
    }
}
