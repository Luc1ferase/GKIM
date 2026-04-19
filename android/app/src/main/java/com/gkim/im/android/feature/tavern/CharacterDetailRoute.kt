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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.PageHeader
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.core.model.ResolvedCompanionCharacterCard
import com.gkim.im.android.core.model.isEditable
import com.gkim.im.android.core.model.resolve
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.feature.shared.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

private data class CharacterDetailUiState(
    val card: CompanionCharacterCard? = null,
)

private class CharacterDetailViewModel(
    container: AppContainer,
    characterId: String,
) : ViewModel() {
    private val state = MutableStateFlow(
        CharacterDetailUiState(card = container.companionRosterRepository.characterById(characterId))
    )
    val uiState: StateFlow<CharacterDetailUiState> = state
}

@Composable
fun CharacterDetailRoute(
    navController: NavHostController,
    container: AppContainer,
    characterId: String,
) {
    val viewModel = viewModel<CharacterDetailViewModel>(factory = simpleViewModelFactory {
        CharacterDetailViewModel(container, characterId)
    })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appLanguage = LocalAppLanguage.current
    val card = uiState.card ?: run {
        MissingCharacterDetail(onBack = { navController.popBackStack() })
        return
    }
    val resolved = card.resolve(appLanguage)

    CharacterDetailScreen(
        character = resolved,
        editable = card.isEditable,
        onBack = { navController.popBackStack() },
        onEdit = { navController.navigate("tavern/editor?mode=edit&id=${card.id}") },
        onActivate = {
            container.companionRosterRepository.activateCharacter(card.id)
            val conversation = container.messagingRepository.ensureConversation(card.asCompanionContact(appLanguage))
            navController.navigate("chat/${conversation.id}")
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterDetailScreen(
    character: ResolvedCompanionCharacterCard,
    editable: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onActivate: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    LazyColumn(
        modifier = Modifier
            .background(AetherColors.Surface)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("character-detail-screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageHeader(
                title = character.displayName,
                description = character.roleLabel,
                leadingLabel = appLanguage.pick("Back", "返回"),
                onLeading = onBack,
                actionLabel = if (editable) appLanguage.pick("Edit", "编辑") else null,
                onAction = if (editable) onEdit else null,
            )
        }
        item { ActionRow(onActivate = onActivate) }
        item { SectionCard(appLanguage.pick("Summary", "摘要"), character.summary, "character-detail-summary") }
        item { SectionCard(appLanguage.pick("System prompt", "系统提示"), character.systemPrompt, "character-detail-system-prompt") }
        item { SectionCard(appLanguage.pick("Scenario", "场景"), character.scenario, "character-detail-scenario") }
        item { SectionCard(appLanguage.pick("Personality", "性格"), character.personality, "character-detail-personality") }
        item { SectionCard(appLanguage.pick("Example dialogue", "示例对话"), character.exampleDialogue, "character-detail-example") }
        item { SectionCard(appLanguage.pick("First message", "开场白"), character.firstMes, "character-detail-first-mes") }
        if (character.alternateGreetings.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.testTag("character-detail-alternate-greetings")) {
                    Text(
                        text = appLanguage.pick("Alternate greetings", "备选开场白"),
                        style = MaterialTheme.typography.titleMedium,
                        color = AetherColors.OnSurface,
                    )
                    character.alternateGreetings.forEachIndexed { index, greeting ->
                        Text(
                            text = "${index + 1}. $greeting",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AetherColors.OnSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            GlassCard(modifier = Modifier.testTag("character-detail-metadata")) {
                Text(text = appLanguage.pick("Metadata", "元数据"), style = MaterialTheme.typography.titleMedium, color = AetherColors.OnSurface)
                Text(text = appLanguage.pick("Creator", "作者") + ": " + character.creator, style = MaterialTheme.typography.bodyMedium, color = AetherColors.OnSurfaceVariant)
                Text(text = appLanguage.pick("Version", "版本") + ": " + character.characterVersion, style = MaterialTheme.typography.bodyMedium, color = AetherColors.OnSurfaceVariant)
                Text(text = appLanguage.pick("Notes", "备注") + ": " + character.creatorNotes, style = MaterialTheme.typography.bodyMedium, color = AetherColors.OnSurfaceVariant)
            }
        }
        if (character.tags.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.testTag("character-detail-tags")) {
                    Text(text = appLanguage.pick("Tags", "标签"), style = MaterialTheme.typography.titleMedium, color = AetherColors.OnSurface)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        character.tags.forEach { tag ->
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelLarge,
                                color = AetherColors.Primary,
                                modifier = Modifier
                                    .background(AetherColors.SurfaceContainerHigh, shape = androidx.compose.foundation.shape.CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(onActivate: () -> Unit) {
    val appLanguage = LocalAppLanguage.current
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = appLanguage.pick("Activate", "激活"),
            style = MaterialTheme.typography.labelLarge,
            color = AetherColors.OnSurface,
            modifier = Modifier
                .background(AetherColors.SurfaceContainerHigh, shape = androidx.compose.foundation.shape.CircleShape)
                .clickable(onClick = onActivate)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("character-detail-activate"),
        )
    }
}

@Composable
private fun SectionCard(title: String, body: String, tag: String) {
    GlassCard(modifier = Modifier.testTag(tag)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = AetherColors.OnSurface)
        Text(text = body, style = MaterialTheme.typography.bodyMedium, color = AetherColors.OnSurfaceVariant)
    }
}

@Composable
private fun MissingCharacterDetail(onBack: () -> Unit) {
    val appLanguage = LocalAppLanguage.current
    Column(
        modifier = Modifier
            .background(AetherColors.Surface)
            .padding(24.dp)
            .testTag("character-detail-missing"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader(
            title = appLanguage.pick("Character missing", "角色不存在"),
            leadingLabel = appLanguage.pick("Back", "返回"),
            onLeading = onBack,
        )
    }
}

private fun CompanionCharacterCard.asCompanionContact(appLanguage: AppLanguage): Contact =
    Contact(
        id = id,
        nickname = resolve(appLanguage).displayName,
        title = resolve(appLanguage).roleLabel,
        avatarText = avatarText,
        addedAt = Instant.now().toString(),
        isOnline = true,
    )
