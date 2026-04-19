package com.gkim.im.android.feature.tavern

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.PageHeader
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.CompanionCardMutationResult
import kotlinx.serialization.json.JsonObject

@Composable
fun CharacterEditorRoute(
    navController: NavHostController,
    container: AppContainer,
    mode: String,
    characterId: String?,
) {
    val isEdit = mode == "edit" && !characterId.isNullOrBlank()
    val original = if (isEdit) container.companionRosterRepository.characterById(characterId.orEmpty()) else null
    val seed = original ?: CompanionCharacterCard(
        id = "",
        displayName = LocalizedText("", ""),
        roleLabel = LocalizedText("", ""),
        summary = LocalizedText("", ""),
        firstMes = LocalizedText("", ""),
        alternateGreetings = emptyList(),
        systemPrompt = LocalizedText("", ""),
        personality = LocalizedText("", ""),
        scenario = LocalizedText("", ""),
        exampleDialogue = LocalizedText("", ""),
        tags = emptyList(),
        creator = "",
        creatorNotes = "",
        characterVersion = "1.0.0",
        avatarText = "CC",
        accent = com.gkim.im.android.core.model.AccentTone.Primary,
        source = CompanionCharacterSource.UserAuthored,
        extensions = JsonObject(emptyMap()),
    )

    var displayNameEn by remember { mutableStateOf(seed.displayName.english) }
    var displayNameZh by remember { mutableStateOf(seed.displayName.chinese) }
    var roleEn by remember { mutableStateOf(seed.roleLabel.english) }
    var roleZh by remember { mutableStateOf(seed.roleLabel.chinese) }
    var summaryEn by remember { mutableStateOf(seed.summary.english) }
    var summaryZh by remember { mutableStateOf(seed.summary.chinese) }
    var firstMesEn by remember { mutableStateOf(seed.firstMes.english) }
    var firstMesZh by remember { mutableStateOf(seed.firstMes.chinese) }
    var systemPromptEn by remember { mutableStateOf(seed.systemPrompt.english) }
    var systemPromptZh by remember { mutableStateOf(seed.systemPrompt.chinese) }
    var personalityEn by remember { mutableStateOf(seed.personality.english) }
    var personalityZh by remember { mutableStateOf(seed.personality.chinese) }
    var scenarioEn by remember { mutableStateOf(seed.scenario.english) }
    var scenarioZh by remember { mutableStateOf(seed.scenario.chinese) }
    var exampleEn by remember { mutableStateOf(seed.exampleDialogue.english) }
    var exampleZh by remember { mutableStateOf(seed.exampleDialogue.chinese) }
    var creator by remember { mutableStateOf(seed.creator) }
    var creatorNotes by remember { mutableStateOf(seed.creatorNotes) }
    var version by remember { mutableStateOf(seed.characterVersion) }
    var avatarText by remember { mutableStateOf(seed.avatarText) }
    var avatarUri by remember { mutableStateOf(seed.avatarUri.orEmpty()) }
    var tagDraft by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf<String>().apply { addAll(seed.tags) } }
    val alternateGreetings = remember {
        mutableStateListOf<LocalizedText>().apply {
            addAll(if (seed.alternateGreetings.isEmpty()) listOf(LocalizedText("", "")) else seed.alternateGreetings)
        }
    }

    CharacterEditorScreen(
        isEdit = isEdit,
        displayNameEn = displayNameEn,
        displayNameZh = displayNameZh,
        roleEn = roleEn,
        roleZh = roleZh,
        summaryEn = summaryEn,
        summaryZh = summaryZh,
        firstMesEn = firstMesEn,
        firstMesZh = firstMesZh,
        systemPromptEn = systemPromptEn,
        systemPromptZh = systemPromptZh,
        personalityEn = personalityEn,
        personalityZh = personalityZh,
        scenarioEn = scenarioEn,
        scenarioZh = scenarioZh,
        exampleEn = exampleEn,
        exampleZh = exampleZh,
        creator = creator,
        creatorNotes = creatorNotes,
        version = version,
        avatarText = avatarText,
        avatarUri = avatarUri,
        tags = tags,
        tagDraft = tagDraft,
        alternateGreetings = alternateGreetings,
        onDisplayNameEnChanged = { displayNameEn = it },
        onDisplayNameZhChanged = { displayNameZh = it },
        onRoleEnChanged = { roleEn = it },
        onRoleZhChanged = { roleZh = it },
        onSummaryEnChanged = { summaryEn = it },
        onSummaryZhChanged = { summaryZh = it },
        onFirstMesEnChanged = { firstMesEn = it },
        onFirstMesZhChanged = { firstMesZh = it },
        onSystemPromptEnChanged = { systemPromptEn = it },
        onSystemPromptZhChanged = { systemPromptZh = it },
        onPersonalityEnChanged = { personalityEn = it },
        onPersonalityZhChanged = { personalityZh = it },
        onScenarioEnChanged = { scenarioEn = it },
        onScenarioZhChanged = { scenarioZh = it },
        onExampleEnChanged = { exampleEn = it },
        onExampleZhChanged = { exampleZh = it },
        onCreatorChanged = { creator = it },
        onCreatorNotesChanged = { creatorNotes = it },
        onVersionChanged = { version = it },
        onAvatarTextChanged = { avatarText = it },
        onAvatarUriChanged = { avatarUri = it },
        onTagDraftChanged = { tagDraft = it },
        onAddTag = {
            val normalized = tagDraft.trim()
            if (normalized.isNotEmpty() && normalized !in tags) tags += normalized
            tagDraft = ""
        },
        onRemoveTag = { tags.remove(it) },
        onAlternateGreetingChanged = { index, updated -> alternateGreetings[index] = updated },
        onAddAlternateGreeting = { alternateGreetings += LocalizedText("", "") },
        onRemoveAlternateGreeting = { index -> if (alternateGreetings.size > 1) alternateGreetings.removeAt(index) },
        onCancel = { navController.popBackStack() },
        onSave = {
            val draft = seed.copy(
                displayName = LocalizedText(displayNameEn, displayNameZh),
                roleLabel = LocalizedText(roleEn, roleZh),
                summary = LocalizedText(summaryEn, summaryZh),
                firstMes = LocalizedText(firstMesEn, firstMesZh),
                alternateGreetings = alternateGreetings.filter { it.english.isNotBlank() || it.chinese.isNotBlank() },
                systemPrompt = LocalizedText(systemPromptEn, systemPromptZh),
                personality = LocalizedText(personalityEn, personalityZh),
                scenario = LocalizedText(scenarioEn, scenarioZh),
                exampleDialogue = LocalizedText(exampleEn, exampleZh),
                tags = tags.toList(),
                creator = creator,
                creatorNotes = creatorNotes,
                characterVersion = version,
                avatarText = avatarText,
                avatarUri = avatarUri.ifBlank { null },
                source = if (seed.source == CompanionCharacterSource.Preset) CompanionCharacterSource.UserAuthored else seed.source,
            )
            when (container.companionRosterRepository.upsertUserCharacter(draft)) {
                is CompanionCardMutationResult.Success -> navController.popBackStack()
                is CompanionCardMutationResult.Rejected -> Unit
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterEditorScreen(
    isEdit: Boolean,
    displayNameEn: String,
    displayNameZh: String,
    roleEn: String,
    roleZh: String,
    summaryEn: String,
    summaryZh: String,
    firstMesEn: String,
    firstMesZh: String,
    systemPromptEn: String,
    systemPromptZh: String,
    personalityEn: String,
    personalityZh: String,
    scenarioEn: String,
    scenarioZh: String,
    exampleEn: String,
    exampleZh: String,
    creator: String,
    creatorNotes: String,
    version: String,
    avatarText: String,
    avatarUri: String,
    tags: List<String>,
    tagDraft: String,
    alternateGreetings: List<LocalizedText>,
    onDisplayNameEnChanged: (String) -> Unit,
    onDisplayNameZhChanged: (String) -> Unit,
    onRoleEnChanged: (String) -> Unit,
    onRoleZhChanged: (String) -> Unit,
    onSummaryEnChanged: (String) -> Unit,
    onSummaryZhChanged: (String) -> Unit,
    onFirstMesEnChanged: (String) -> Unit,
    onFirstMesZhChanged: (String) -> Unit,
    onSystemPromptEnChanged: (String) -> Unit,
    onSystemPromptZhChanged: (String) -> Unit,
    onPersonalityEnChanged: (String) -> Unit,
    onPersonalityZhChanged: (String) -> Unit,
    onScenarioEnChanged: (String) -> Unit,
    onScenarioZhChanged: (String) -> Unit,
    onExampleEnChanged: (String) -> Unit,
    onExampleZhChanged: (String) -> Unit,
    onCreatorChanged: (String) -> Unit,
    onCreatorNotesChanged: (String) -> Unit,
    onVersionChanged: (String) -> Unit,
    onAvatarTextChanged: (String) -> Unit,
    onAvatarUriChanged: (String) -> Unit,
    onTagDraftChanged: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onAlternateGreetingChanged: (Int, LocalizedText) -> Unit,
    onAddAlternateGreeting: () -> Unit,
    onRemoveAlternateGreeting: (Int) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    LazyColumn(
        modifier = Modifier
            .background(AetherColors.Surface)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("character-editor-screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageHeader(
                title = if (isEdit) appLanguage.pick("Edit character", "编辑角色") else appLanguage.pick("Create character", "新建角色"),
                leadingLabel = appLanguage.pick("Cancel", "取消"),
                onLeading = onCancel,
                actionLabel = appLanguage.pick("Save", "保存"),
                onAction = onSave,
            )
        }
        item { BilingualField("Display name", "角色名", displayNameEn, displayNameZh, onDisplayNameEnChanged, onDisplayNameZhChanged, "character-editor-display-name") }
        item { BilingualField("Role label", "角色标签", roleEn, roleZh, onRoleEnChanged, onRoleZhChanged, "character-editor-role-label") }
        item { BilingualField("Summary", "摘要", summaryEn, summaryZh, onSummaryEnChanged, onSummaryZhChanged, "character-editor-summary", multiLine = true) }
        item { BilingualField("First message", "开场白", firstMesEn, firstMesZh, onFirstMesEnChanged, onFirstMesZhChanged, "character-editor-first-mes", multiLine = true) }
        item { BilingualField("System prompt", "系统提示", systemPromptEn, systemPromptZh, onSystemPromptEnChanged, onSystemPromptZhChanged, "character-editor-system-prompt", multiLine = true) }
        item { BilingualField("Personality", "性格", personalityEn, personalityZh, onPersonalityEnChanged, onPersonalityZhChanged, "character-editor-personality", multiLine = true) }
        item { BilingualField("Scenario", "场景", scenarioEn, scenarioZh, onScenarioEnChanged, onScenarioZhChanged, "character-editor-scenario", multiLine = true) }
        item { BilingualField("Example dialogue", "示例对话", exampleEn, exampleZh, onExampleEnChanged, onExampleZhChanged, "character-editor-example", multiLine = true) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Pill(appLanguage.pick("Add greeting", "添加开场白"), onAddAlternateGreeting, "character-editor-add-greeting")
                if (alternateGreetings.size > 1) {
                    Pill(appLanguage.pick("Remove last", "删除最后一条"), { onRemoveAlternateGreeting(alternateGreetings.lastIndex) }, "character-editor-remove-greeting")
                }
            }
        }
        itemsIndexed(alternateGreetings) { index, greeting ->
            BilingualField(
                englishLabel = "Greeting ${index + 1}",
                chineseLabel = "备选开场白 ${index + 1}",
                englishValue = greeting.english,
                chineseValue = greeting.chinese,
                onEnglishChanged = { onAlternateGreetingChanged(index, greeting.copy(english = it)) },
                onChineseChanged = { onAlternateGreetingChanged(index, greeting.copy(chinese = it)) },
                testTag = "character-editor-alt-$index",
                multiLine = true,
            )
        }
        item {
            GlassCard(modifier = Modifier.testTag("character-editor-tags")) {
                Text(text = appLanguage.pick("Tags", "标签"), style = MaterialTheme.typography.titleMedium, color = AetherColors.OnSurface)
                OutlinedTextField(
                    value = tagDraft,
                    onValueChange = onTagDraftChanged,
                    modifier = Modifier.fillMaxWidth().testTag("character-editor-tag-input"),
                    label = { Text(appLanguage.pick("New tag", "新标签")) },
                )
                Pill(appLanguage.pick("Add tag", "添加标签"), onAddTag, "character-editor-add-tag")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelLarge,
                            color = AetherColors.Primary,
                            modifier = Modifier
                                .background(AetherColors.SurfaceContainerHigh, shape = androidx.compose.foundation.shape.CircleShape)
                                .clickable { onRemoveTag(tag) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
        item { SimpleField(appLanguage.pick("Creator", "作者"), creator, onCreatorChanged, "character-editor-creator") }
        item { SimpleField(appLanguage.pick("Creator notes", "作者备注"), creatorNotes, onCreatorNotesChanged, "character-editor-creator-notes", multiLine = true) }
        item { SimpleField(appLanguage.pick("Version", "版本"), version, onVersionChanged, "character-editor-version") }
        item { SimpleField(appLanguage.pick("Avatar text", "头像文字"), avatarText, onAvatarTextChanged, "character-editor-avatar-text") }
        item { SimpleField(appLanguage.pick("Avatar URI", "头像 URI"), avatarUri, onAvatarUriChanged, "character-editor-avatar-uri") }
    }
}

@Composable
private fun BilingualField(
    englishLabel: String,
    chineseLabel: String,
    englishValue: String,
    chineseValue: String,
    onEnglishChanged: (String) -> Unit,
    onChineseChanged: (String) -> Unit,
    testTag: String,
    multiLine: Boolean = false,
) {
    GlassCard(modifier = Modifier.testTag(testTag)) {
        OutlinedTextField(
            value = englishValue,
            onValueChange = onEnglishChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(englishLabel) },
            maxLines = if (multiLine) 6 else 1,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        OutlinedTextField(
            value = chineseValue,
            onValueChange = onChineseChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(chineseLabel) },
            maxLines = if (multiLine) 6 else 1,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
    }
}

@Composable
private fun SimpleField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    testTag: String,
    multiLine: Boolean = false,
) {
    GlassCard(modifier = Modifier.testTag(testTag)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            maxLines = if (multiLine) 6 else 1,
        )
    }
}

@Composable
private fun Pill(label: String, onClick: () -> Unit, testTag: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = AetherColors.OnSurface,
        modifier = Modifier
            .background(AetherColors.SurfaceContainerHigh, shape = androidx.compose.foundation.shape.CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(testTag),
    )
}
