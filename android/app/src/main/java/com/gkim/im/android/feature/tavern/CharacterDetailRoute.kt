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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.gkim.im.android.core.model.CharacterSkin
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.core.model.ResolvedCompanionCharacterCard
import com.gkim.im.android.core.model.SkinRarity
import com.gkim.im.android.core.model.SkinTrait
import com.gkim.im.android.core.model.isEditable
import com.gkim.im.android.core.model.resolve
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.CardInteropRepository
import com.gkim.im.android.data.repository.CompanionSkinRepository
import com.gkim.im.android.data.repository.ExportedCardFormat
import com.gkim.im.android.feature.shared.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import java.time.Instant

private data class CharacterDetailUiState(
    val card: CompanionCharacterCard? = null,
    val skinsForCharacter: List<CharacterSkin> = emptyList(),
    val ownedSkinIds: Set<String> = emptySet(),
    val activeSkinId: String = CompanionCharacterCard.DEFAULT_SKIN_ID,
)

private class CharacterDetailViewModel(
    private val container: AppContainer,
    private val characterId: String,
) : ViewModel() {
    val uiState: StateFlow<CharacterDetailUiState> = combine(
        container.companionSkinRepository.catalog,
        container.companionSkinRepository.ownedSkinIds,
        container.companionSkinRepository.activeSkinByCharacter,
    ) { catalog, ownedSkinIds, activeByChar ->
        CharacterDetailUiState(
            card = container.companionRosterRepository.characterById(characterId),
            skinsForCharacter = catalog.filter { it.characterId == characterId },
            ownedSkinIds = ownedSkinIds,
            activeSkinId = activeByChar[characterId] ?: CompanionCharacterCard.DEFAULT_SKIN_ID,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CharacterDetailUiState(
            card = container.companionRosterRepository.characterById(characterId),
        ),
    )

    fun activateSkin(skinId: String) {
        container.companionSkinRepository.activateSkin(characterId, skinId)
    }
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

    var pendingExportFormat by remember { mutableStateOf<ExportedCardFormat?>(null) }
    val exportDispatcher = rememberCardExportDispatcher()

    CharacterDetailScreen(
        character = resolved,
        card = card,
        editable = card.isEditable,
        onBack = { navController.popBackStack() },
        onEdit = { navController.navigate("tavern/editor?mode=edit&id=${card.id}") },
        onActivate = {
            container.companionRosterRepository.activateCharacter(card.id)
            val conversation = container.messagingRepository.ensureConversation(
                contact = card.asCompanionContact(appLanguage),
                companionCardId = card.id,
            )
            navController.navigate("chat/${conversation.id}")
        },
        onExportPng = { pendingExportFormat = ExportedCardFormat.Png },
        onExportJson = { pendingExportFormat = ExportedCardFormat.Json },
        lorebookTab = {
            CharacterLorebookTabSection(
                container = container,
                characterId = card.id,
                onManageLorebook = { lorebookId ->
                    navController.navigate("settings?worldinfoLorebookId=$lorebookId")
                },
            )
        },
        relationshipResetSlot = {
            CharacterDetailRelationshipResetSection(
                container = container,
                characterId = card.id,
            )
        },
        skinGallerySlot = {
            CharacterDetailSkinGallerySection(
                characterId = card.id,
                skins = uiState.skinsForCharacter,
                ownedSkinIds = uiState.ownedSkinIds,
                activeSkinId = uiState.activeSkinId,
                appLanguage = appLanguage,
                onActivate = viewModel::activateSkin,
                onTryDrawing = {
                    // R3.3 — "Try drawing" CTA on locked-skin sheet pops back
                    // to the tavern home so the user lands on the draw entry.
                    navController.popBackStack(route = "tavern", inclusive = false)
                },
            )
        },
    )

    pendingExportFormat?.let { format ->
        CardExportDialog(
            cardId = card.id,
            initialFormat = format,
            repository = container.cardInteropRepository,
            dispatcher = exportDispatcher,
            onDismiss = { pendingExportFormat = null },
        )
    }
}

/**
 * §3.2 wire-up host — renders the [RelationshipResetButton] under the action row on the
 * character-detail screen. Public so [CharacterDetailRoute] above can compose it.
 */
@Composable
internal fun CharacterDetailRelationshipResetSection(
    container: AppContainer,
    characterId: String,
) {
    RelationshipResetButton(
        characterId = characterId,
        repository = container.messagingRepository,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .testTag("character-detail-relationship-reset"),
    )
}

/**
 * R3.3 — companion-skin-gacha skin gallery section on character detail.
 *
 * - tap-OwnedActive: no-op (already active).
 * - tap-OwnedInactive: confirm dialog → `companionSkinRepository.activateSkin(...)`
 *   (eventual `POST /users/me/skins/active` on the backend).
 * - tap-Locked: bottom sheet with rarity + trait descriptions + "Try drawing"
 *   CTA that pops back to the tavern home draw entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CharacterDetailSkinGallerySection(
    characterId: String,
    skins: List<CharacterSkin>,
    ownedSkinIds: Set<String>,
    activeSkinId: String,
    appLanguage: AppLanguage,
    onActivate: (skinId: String) -> Unit,
    onTryDrawing: () -> Unit,
) {
    if (skins.isEmpty()) return

    var pendingActivation by remember { mutableStateOf<CharacterSkin?>(null) }
    var pendingPreview by remember { mutableStateOf<CharacterSkin?>(null) }

    GlassCard(modifier = Modifier.testTag("character-detail-skin-gallery")) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = appLanguage.pick("Wardrobe", "衣橱"),
                style = MaterialTheme.typography.titleMedium,
                color = AetherColors.OnSurface,
            )
            Text(
                text = appLanguage.pick(
                    "Different evenings, same person. Tap a skin to swap, or peek at the locked ones.",
                    "同一个人，不同的夜。点选一件切换，或预览未解锁的款式。",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = AetherColors.OnSurfaceVariant,
            )
            SkinGallery(
                characterId = characterId,
                skins = skins,
                activeSkinId = activeSkinId,
                ownedSkinIds = ownedSkinIds,
                appLanguage = appLanguage,
                onActivate = { skinId ->
                    pendingActivation = skins.firstOrNull { it.skinId == skinId }
                },
                onPreviewLocked = { skinId ->
                    pendingPreview = skins.firstOrNull { it.skinId == skinId }
                },
            )
        }
    }

    pendingActivation?.let { skin ->
        AlertDialog(
            onDismissRequest = { pendingActivation = null },
            modifier = Modifier.testTag("skin-activate-dialog"),
            title = {
                Text(
                    text = appLanguage.pick("Wear this skin?", "切换为这件？"),
                )
            },
            text = {
                Text(
                    text = appLanguage.pick(
                        "Switch the active skin to ${skin.displayName.resolve(appLanguage)}.",
                        "把当前外观切换为「${skin.displayName.resolve(appLanguage)}」。",
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onActivate(skin.skinId)
                        pendingActivation = null
                    },
                    modifier = Modifier.testTag("skin-activate-confirm"),
                ) {
                    Text(text = appLanguage.pick("Confirm", "确认"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingActivation = null }) {
                    Text(text = appLanguage.pick("Cancel", "取消"))
                }
            },
        )
    }

    pendingPreview?.let { skin ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { pendingPreview = null },
            sheetState = sheetState,
            modifier = Modifier.testTag("skin-locked-preview-sheet"),
        ) {
            LockedSkinPreviewContent(
                skin = skin,
                appLanguage = appLanguage,
                onTryDrawing = {
                    pendingPreview = null
                    onTryDrawing()
                },
            )
        }
    }
}

@Composable
private fun LockedSkinPreviewContent(
    skin: CharacterSkin,
    appLanguage: AppLanguage,
    onTryDrawing: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = appLanguage.pick(skin.rarity.englishLabel(), skin.rarity.chineseLabel()),
            style = MaterialTheme.typography.labelLarge,
            color = AetherColors.Tertiary,
            modifier = Modifier.testTag("skin-locked-preview-rarity"),
        )
        Text(
            text = skin.displayName.resolve(appLanguage),
            style = MaterialTheme.typography.headlineSmall,
            color = AetherColors.OnSurface,
        )
        if (skin.traits.isEmpty()) {
            Text(
                text = appLanguage.pick(
                    "A different look — no persona shifts, just a new evening.",
                    "只是换了一身打扮，性格不会因此改变。",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = AetherColors.OnSurfaceVariant,
            )
        } else {
            skin.traits.forEach { trait ->
                LockedTraitRow(trait = trait, appLanguage = appLanguage)
            }
        }
        Text(
            text = appLanguage.pick("Try drawing", "去抽卡"),
            style = MaterialTheme.typography.labelLarge,
            color = AetherColors.OnSurface,
            modifier = Modifier
                .background(AetherColors.Primary, shape = androidx.compose.foundation.shape.CircleShape)
                .clickable(onClick = onTryDrawing)
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .testTag("skin-locked-preview-try-drawing"),
        )
    }
}

@Composable
private fun LockedTraitRow(
    trait: SkinTrait,
    appLanguage: AppLanguage,
) {
    Column(
        modifier = Modifier.testTag("skin-locked-preview-trait-${trait.traitId}"),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = trait.kind.label(appLanguage),
            style = MaterialTheme.typography.labelMedium,
            color = AetherColors.Primary,
        )
        Text(
            text = trait.description.resolve(appLanguage),
            style = MaterialTheme.typography.bodyMedium,
            color = AetherColors.OnSurfaceVariant,
        )
    }
}

private fun SkinRarity.englishLabel(): String = when (this) {
    SkinRarity.Common    -> "Common"
    SkinRarity.Rare      -> "Rare"
    SkinRarity.Epic      -> "Epic"
    SkinRarity.Legendary -> "Legendary"
}

private fun SkinRarity.chineseLabel(): String = when (this) {
    SkinRarity.Common    -> "普通"
    SkinRarity.Rare      -> "稀有"
    SkinRarity.Epic      -> "史诗"
    SkinRarity.Legendary -> "传说"
}

private fun com.gkim.im.android.core.model.SkinTraitKind.label(language: AppLanguage): String = when (this) {
    com.gkim.im.android.core.model.SkinTraitKind.PersonaMod        -> language.pick("Persona shift", "性格变化")
    com.gkim.im.android.core.model.SkinTraitKind.Greeting          -> language.pick("Opening line", "开场白")
    com.gkim.im.android.core.model.SkinTraitKind.VoiceTone         -> language.pick("Voice tone", "语气")
    com.gkim.im.android.core.model.SkinTraitKind.RelationshipBoost -> language.pick("Bond accelerator", "关系加成")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterDetailScreen(
    character: ResolvedCompanionCharacterCard,
    card: CompanionCharacterCard,
    editable: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onActivate: () -> Unit,
    onExportPng: () -> Unit,
    onExportJson: () -> Unit,
    lorebookTab: @Composable () -> Unit = {},
    relationshipResetSlot: @Composable () -> Unit = {},
    skinGallerySlot: @Composable () -> Unit = {},
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
        item {
            ActionRow(
                onActivate = onActivate,
                onExportPng = onExportPng,
                onExportJson = onExportJson,
            )
        }
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
        item { AboutCardSection(card) }
        item { lorebookTab() }
        // R3.3 — companion-skin-gacha skin gallery sits between description-side
        // sections (about / lorebook) and the relationship-reset trigger so the
        // user encounters wardrobe choices before considering a hard reset.
        item { skinGallerySlot() }
        item { relationshipResetSlot() }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionRow(
    onActivate: () -> Unit,
    onExportPng: () -> Unit,
    onExportJson: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
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
        Text(
            text = appLanguage.pick("Export PNG", "导出 PNG"),
            style = MaterialTheme.typography.labelLarge,
            color = AetherColors.OnSurface,
            modifier = Modifier
                .background(AetherColors.SurfaceContainerHigh, shape = androidx.compose.foundation.shape.CircleShape)
                .clickable(onClick = onExportPng)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("character-detail-export-png"),
        )
        Text(
            text = appLanguage.pick("Export JSON", "导出 JSON"),
            style = MaterialTheme.typography.labelLarge,
            color = AetherColors.OnSurface,
            modifier = Modifier
                .background(AetherColors.SurfaceContainerHigh, shape = androidx.compose.foundation.shape.CircleShape)
                .clickable(onClick = onExportJson)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("character-detail-export-json"),
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
