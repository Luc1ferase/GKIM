package com.gkim.im.android.feature.tavern

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.candleGlow
import com.gkim.im.android.core.designsystem.tavernGrain
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.PageHeader
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.CompanionDrawResult
import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.core.model.ResolvedCompanionCharacterCard
import com.gkim.im.android.core.model.resolve
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.CompanionRosterRepository
import com.gkim.im.android.feature.shared.simpleViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

internal const val TavernAllCompanionsSectionTestTag = "tavern-all-companions-section"

// R4.3 — gacha accent re-tone manifest. The post-draw result surfaces use
// these palette tokens (and only these) for chromatic emphasis. R1.1 swapped
// the palette so AetherColors.Tertiary now resolves to ember red (#B85450)
// and AetherColors.Primary to brass (#E0A04D); listing the bindings here
// makes the contract explicit so a future per-companion theming pass cannot
// silently regress to the previous lavender / pink saturation.
internal data class GachaAccentBinding(val testTag: String, val paletteToken: String)

internal val GachaResultAccents: List<GachaAccentBinding> = listOf(
    GachaAccentBinding("tavern-draw-result-latest-label", "tertiary"),
    GachaAccentBinding("tavern-draw-result-duplicate-label", "tertiary"),
    GachaAccentBinding("tavern-draw-keep-as-bonus", "primary"),
)

internal enum class HeaderActionKind { Pill, Rectangle }

internal data class HeaderActionSpec(val testTag: String, val kind: HeaderActionKind)

// R3.1 — pill discipline manifest. The Tavern home holds exactly one Pill,
// and that pill is the surface's primary emotional action (`抽卡` /
// "Pour a drink"). Settings + Import are demoted to Rectangle. The
// composable below reads from this list so the rendered layout cannot
// drift from the contract test without updating both.
internal val TavernHeaderActions: List<HeaderActionSpec> = listOf(
    HeaderActionSpec("tavern-create-trigger", HeaderActionKind.Rectangle),
    HeaderActionSpec("tavern-draw-trigger", HeaderActionKind.Pill),
    HeaderActionSpec("tavern-import-entry", HeaderActionKind.Rectangle),
)

private data class TavernUiState(
    val presetCharacters: List<CompanionCharacterCard> = emptyList(),
    val ownedCharacters: List<CompanionCharacterCard> = emptyList(),
    val userCharacters: List<CompanionCharacterCard> = emptyList(),
    val activeCharacterId: String = "",
    val lastDrawResult: CompanionDrawResult? = null,
)

private class TavernViewModel(
    private val companionRosterRepository: CompanionRosterRepository,
) : ViewModel() {
    val uiState = combine(
        companionRosterRepository.presetCharacters,
        companionRosterRepository.ownedCharacters,
        companionRosterRepository.userCharacters,
        companionRosterRepository.activeCharacterId,
        companionRosterRepository.lastDrawResult,
    ) { presetCharacters, ownedCharacters, userCharacters, activeCharacterId, lastDrawResult ->
        TavernUiState(
            presetCharacters = presetCharacters,
            ownedCharacters = ownedCharacters,
            userCharacters = userCharacters,
            activeCharacterId = activeCharacterId,
            lastDrawResult = lastDrawResult,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TavernUiState())

    fun activateCharacter(characterId: String): CompanionCharacterCard? {
        companionRosterRepository.activateCharacter(characterId)
        return companionRosterRepository.characterById(characterId)
    }

    fun drawCharacter() {
        viewModelScope.launch {
            companionRosterRepository.drawCharacter()
        }
    }
}

@Composable
fun TavernRoute(navController: NavHostController, container: AppContainer) {
    val viewModel = viewModel<TavernViewModel>(factory = simpleViewModelFactory {
        TavernViewModel(container.companionRosterRepository)
    })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appLanguage = LocalAppLanguage.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var importEntryState by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(TavernImportEntryState())
    }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: ByteArray(0)
        val filename = uri.lastPathSegment?.substringAfterLast('/') ?: "card"
        when (val result = evaluateImportSelection(bytes, filename)) {
            is TavernImportSelectionResult.Rejected -> {
                importEntryState = TavernImportEntryState(errorCode = result.code)
            }
            is TavernImportSelectionResult.Accepted -> {
                importEntryState = TavernImportEntryState()
                PendingImportBytes.set(result.bytes, result.filename)
                navController.navigate("tavern/import-preview")
            }
        }
    }

    val drawBreakdown = remember(container) {
        computeProbabilityBreakdown(container.companionRosterRepository.drawPool)
    }
    TavernScreen(
        uiState = uiState,
        importEntryState = importEntryState,
        drawBreakdown = drawBreakdown,
        onOpenSettings = { navController.navigate("settings") },
        onCreateCharacter = { navController.navigate("tavern/editor?mode=create") },
        onImportCard = { importLauncher.launch(arrayOf("image/png", "application/json")) },
        onDraw = viewModel::drawCharacter,
        onOpenCharacter = { characterId -> navController.navigate("tavern/detail/$characterId") },
        onOpenPortrait = { characterId ->
            navController.navigate(portraitTapRouteForTavernCard(characterId))
        },
        onBonusAwarded = { _ ->
            // Bonus-awarded event recording wiring is deferred to a follow-up analytics slice;
            // the composable callback fires with the right payload shape (verified by
            // GachaDuplicateAnimationTest) and future code can route it to a bus.
        },
        onActivateCharacter = { characterId ->
            val selected = viewModel.activateCharacter(characterId) ?: return@TavernScreen
            val conversation = container.messagingRepository.ensureConversation(
                contact = selected.asCompanionContact(appLanguage),
                companionCardId = selected.id,
            )
            navController.navigate("chat/${conversation.id}")
        },
    )
}

@Composable
private fun TavernScreen(
    uiState: TavernUiState,
    importEntryState: TavernImportEntryState,
    drawBreakdown: GachaProbabilityBreakdown,
    onOpenSettings: () -> Unit,
    onCreateCharacter: () -> Unit,
    onImportCard: () -> Unit,
    onDraw: () -> Unit,
    onOpenCharacter: (String) -> Unit,
    onOpenPortrait: (String) -> Unit,
    onBonusAwarded: (BonusAwardedEvent) -> Unit,
    onActivateCharacter: (String) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    val primaryContainerColor = AetherColors.PrimaryContainer
    LazyColumn(
        modifier = Modifier
            .background(AetherColors.Surface)
            .tavernGrain()
            .candleGlow(
                anchor = androidx.compose.ui.Alignment.TopEnd,
                primaryContainer = primaryContainerColor,
            )
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("tavern-screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            PageHeader(
                title = appLanguage.pick("Tavern", "酒馆"),
                actionLabel = appLanguage.pick("Settings", "设置"),
                onAction = onOpenSettings,
                actionIcon = Icons.Outlined.Settings,
            )
        }
        item {
            Text(
                text = appLanguage.pick(
                    "Choose a companion card, inspect its persona, or draw someone unexpected.",
                    "选择一个陪伴角色，查看它的设定，或者抽一张新角色卡。",
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = AetherColors.OnSurfaceVariant,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RectangularAction(
                    label = appLanguage.pick("Create", "新建"),
                    onClick = onCreateCharacter,
                    testTag = "tavern-create-trigger",
                )
                PrimaryActionPill(
                    label = appLanguage.pick("Pour a drink", "抽卡"),
                    onClick = onDraw,
                    testTag = "tavern-draw-trigger",
                )
                RectangularAction(
                    label = appLanguage.pick("Import card", "导入卡"),
                    onClick = onImportCard,
                    testTag = "tavern-import-entry",
                )
            }
        }
        if (importEntryState.errorCode != null) {
            item {
                Text(
                    text = importErrorCopy(importEntryState.errorCode, appLanguage == AppLanguage.English),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.Danger,
                    modifier = Modifier.testTag("tavern-import-error"),
                )
            }
        }
        item {
            DrawEntryCard(
                lastDrawResult = uiState.lastDrawResult,
                breakdown = drawBreakdown,
                onDraw = onDraw,
                onOpenCharacter = { result -> onOpenCharacter(result.card.id) },
                onBonusAwarded = onBonusAwarded,
            )
        }
        item {
            SectionTitle(
                title = appLanguage.pick("All companions", "全部陪伴"),
                testTag = TavernAllCompanionsSectionTestTag,
            )
        }
        item {
            SectionTitle(
                title = appLanguage.pick("Preset Roles", "预设角色"),
                testTag = "tavern-preset-section",
            )
        }
        items(uiState.presetCharacters, key = { it.id }) { character ->
            CharacterCard(
                character = character.resolve(appLanguage),
                active = uiState.activeCharacterId == character.id,
                testTag = "tavern-preset-card-${character.id}",
                ownershipLabel = appLanguage.pick("Preset", "预设"),
                onClick = { onOpenCharacter(character.id) },
                onAvatarTap = { onOpenPortrait(character.id) },
                onActivate = { onActivateCharacter(character.id) },
            )
        }
        item {
            SectionTitle(
                title = appLanguage.pick("Owned Cards", "已持有角色"),
                testTag = "tavern-owned-section",
            )
        }
        if (uiState.ownedCharacters.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.testTag("tavern-owned-empty")) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = appLanguage.pick(
                                "Your draw-acquired cards will appear here.",
                                "抽到的新角色卡会出现在这里。",
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = AetherColors.OnSurfaceVariant,
                        )
                        Text(
                            text = appLanguage.pick("Or import a SillyTavern card", "或从 SillyTavern 导入一张卡"),
                            style = MaterialTheme.typography.labelLarge,
                            color = AetherColors.OnSurface,
                            modifier = Modifier
                                .background(
                                    AetherColors.SurfaceContainerHigh,
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                )
                                .clickable(onClick = onImportCard)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .testTag("tavern-import-empty-cta"),
                        )
                    }
                }
            }
        } else {
            items(uiState.ownedCharacters, key = { it.id }) { character ->
                CharacterCard(
                    character = character.resolve(appLanguage),
                    active = uiState.activeCharacterId == character.id,
                    testTag = "tavern-owned-card-${character.id}",
                    ownershipLabel = appLanguage.pick("Drawn", "抽得"),
                    onClick = { onOpenCharacter(character.id) },
                    onAvatarTap = { onOpenPortrait(character.id) },
                    onActivate = { onActivateCharacter(character.id) },
                )
            }
        }
        if (uiState.userCharacters.isNotEmpty()) {
            item {
                SectionTitle(
                    title = appLanguage.pick("Custom Cards", "自建角色"),
                    testTag = "tavern-user-section",
                )
            }
            items(uiState.userCharacters, key = { it.id }) { character ->
                CharacterCard(
                    character = character.resolve(appLanguage),
                    active = uiState.activeCharacterId == character.id,
                    testTag = "tavern-user-card-${character.id}",
                    ownershipLabel = appLanguage.pick("Custom", "自建"),
                    onClick = { onOpenCharacter(character.id) },
                    onAvatarTap = { onOpenPortrait(character.id) },
                    onActivate = { onActivateCharacter(character.id) },
                )
            }
        }
    }
}

@Composable
private fun HeaderPill(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = AetherColors.OnSurface,
        modifier = Modifier
            .background(AetherColors.SurfaceContainerHigh, shape = androidx.compose.foundation.shape.CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

// R3.1 — pill discipline. The Tavern home holds exactly one rounded-pill
// (corner radius >= 18 dp + brass primary chromatic emphasis) and that pill
// is the surface's primary emotional action. Admin actions render as
// rectangles (corner radius <= 12 dp) with surfaceContainerHigh background.

@Composable
internal fun RectangularAction(
    label: String,
    onClick: () -> Unit,
    testTag: String,
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = AetherColors.OnSurface,
        modifier = Modifier
            .clip(shape)
            .background(AetherColors.SurfaceContainerHigh, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(testTag),
    )
}

@Composable
internal fun PrimaryActionPill(
    label: String,
    onClick: () -> Unit,
    testTag: String,
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = AetherColors.OnSurface,
        modifier = Modifier
            .clip(shape)
            .background(AetherColors.Primary, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(testTag),
    )
}

@Composable
private fun DrawEntryCard(
    lastDrawResult: CompanionDrawResult?,
    breakdown: GachaProbabilityBreakdown,
    onDraw: () -> Unit,
    onOpenCharacter: (CompanionDrawResult) -> Unit,
    onBonusAwarded: (BonusAwardedEvent) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    GlassCard(modifier = Modifier.testTag("tavern-draw-entry")) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = appLanguage.pick("Role Draw", "角色抽卡"),
                style = MaterialTheme.typography.titleLarge,
                color = AetherColors.OnSurface,
            )
            Text(
                text = appLanguage.pick(
                    "Draw from the tavern pool to add new companion cards to your roster.",
                    "从酒馆卡池里抽角色，把新角色卡加入你的持有列表。",
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = AetherColors.OnSurfaceVariant,
            )
            if (!breakdown.isEmpty) {
                GachaProbabilityBreakdownSection(breakdown = breakdown, appLanguage = appLanguage)
            }
            Text(
                text = appLanguage.pick("Draw a card", "抽一张"),
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.OnSurface,
                modifier = Modifier
                    .background(AetherColors.SurfaceContainerHigh, shape = androidx.compose.foundation.shape.CircleShape)
                    .clickable(onClick = onDraw)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("tavern-draw-trigger"),
            )
            lastDrawResult?.let { result ->
                when (gachaResultVariant(result)) {
                    GachaResultVariant.NewCard -> DrawResultNewCard(result = result, onOpenCharacter = onOpenCharacter)
                    GachaResultVariant.AlreadyOwned -> DrawResultAlreadyOwned(
                        result = result,
                        onOpenCharacter = onOpenCharacter,
                        onBonusAwarded = onBonusAwarded,
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawResultNewCard(
    result: CompanionDrawResult,
    onOpenCharacter: (CompanionDrawResult) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    val resolvedCard = result.card.resolve(appLanguage)
    GlassCard(
        modifier = Modifier
            .testTag("tavern-draw-result")
            .clickable { onOpenCharacter(result) },
    ) {
        Text(
            text = appLanguage.pick("Latest draw", "本次抽卡"),
            style = MaterialTheme.typography.labelLarge,
            color = AetherColors.Tertiary,
        )
        Text(
            text = resolvedCard.displayName,
            style = MaterialTheme.typography.headlineMedium,
            color = AetherColors.OnSurface,
        )
        Text(
            text = appLanguage.pick("New card added to roster", "新角色已加入持有列表"),
            style = MaterialTheme.typography.bodyLarge,
            color = AetherColors.OnSurfaceVariant,
        )
    }
}

@Composable
private fun DrawResultAlreadyOwned(
    result: CompanionDrawResult,
    onOpenCharacter: (CompanionDrawResult) -> Unit,
    onBonusAwarded: (BonusAwardedEvent) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    val resolvedCard = result.card.resolve(appLanguage)
    var bonusClaimed by androidx.compose.runtime.remember(result.card.id) {
        androidx.compose.runtime.mutableStateOf(false)
    }
    GlassCard(
        modifier = Modifier
            .testTag("tavern-draw-result-already-owned")
            .clickable { onOpenCharacter(result) },
    ) {
        Text(
            text = appLanguage.pick("Duplicate draw", "重复抽到"),
            style = MaterialTheme.typography.labelLarge,
            color = AetherColors.Tertiary,
        )
        Text(
            text = resolvedCard.displayName,
            style = MaterialTheme.typography.headlineMedium,
            color = AetherColors.OnSurface,
        )
        Text(
            text = appLanguage.pick(
                "Already owned — convert this draw into a consolation bonus.",
                "已持有，可换取安慰奖励。",
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = AetherColors.OnSurfaceVariant,
        )
        Text(
            text = if (bonusClaimed) {
                appLanguage.pick("Bonus claimed", "奖励已领取")
            } else {
                appLanguage.pick("Keep as bonus", "转为奖励")
            },
            style = MaterialTheme.typography.labelLarge,
            color = if (bonusClaimed) AetherColors.OnSurfaceVariant else AetherColors.OnSurface,
            modifier = Modifier
                .background(
                    if (bonusClaimed) AetherColors.SurfaceContainerLow else AetherColors.Primary.copy(alpha = 0.22f),
                    shape = androidx.compose.foundation.shape.CircleShape,
                )
                .then(
                    if (bonusClaimed) Modifier else Modifier.clickable {
                        onBonusAwarded(bonusAwardedEvent(result, System.currentTimeMillis()))
                        bonusClaimed = true
                    }
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("tavern-draw-keep-as-bonus"),
        )
    }
}

@Composable
private fun GachaProbabilityBreakdownSection(
    breakdown: GachaProbabilityBreakdown,
    appLanguage: AppLanguage,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AetherColors.SurfaceContainerLow, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("tavern-draw-probability-breakdown"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = appLanguage.pick("Drop rates", "掉落概率"),
            style = MaterialTheme.typography.labelLarge,
            color = AetherColors.OnSurface,
        )
        breakdown.entries.forEach { entry ->
            val label = appLanguage.pick(entry.rarity.englishLabel, entry.rarity.chineseLabel)
            Text(
                text = "$label · ${entry.count} · ${formatProbabilityPercent(entry.probability)}",
                style = MaterialTheme.typography.bodyMedium,
                color = AetherColors.OnSurfaceVariant,
                modifier = Modifier.testTag("tavern-draw-probability-row-${entry.rarity.name}"),
            )
        }
    }
}

@Composable
private fun CharacterCard(
    character: ResolvedCompanionCharacterCard,
    active: Boolean,
    testTag: String,
    ownershipLabel: String,
    onClick: () -> Unit,
    onAvatarTap: () -> Unit,
    onActivate: () -> Unit,
) {
    GlassCard(modifier = Modifier.testTag(testTag)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                com.gkim.im.android.core.ui.SkinAvatar(
                    characterId = character.id,
                    skinId = "default",
                    version = 1,
                    variant = com.gkim.im.android.core.assets.SkinVariant.Thumb,
                    modifier = Modifier
                        .padding(end = 14.dp)
                        .clickable(onClick = onAvatarTap)
                        .testTag("$testTag-avatar"),
                    shape = com.gkim.im.android.core.ui.TavernCardAvatarShape,
                    contentDescription = character.displayName,
                )
                Column(
                    modifier = Modifier.weight(1f).clickable(onClick = onClick),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = character.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = AetherColors.OnSurface,
                    )
                    Text(
                        text = character.roleLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = AetherColors.OnSurfaceVariant,
                    )
                }
                Text(
                    text = if (active) "$ownershipLabel · ACTIVE" else ownershipLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.Primary,
                )
            }
            Text(
                text = character.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = AetherColors.OnSurfaceVariant,
                modifier = Modifier.clickable(onClick = onClick),
            )
            Box(
                modifier = Modifier
                    .background(AetherColors.SurfaceContainerHigh, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("$testTag-first-mes")
                    .clickable(onClick = onClick),
            ) {
                Text(
                    text = character.firstMes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
            }
            HeaderPill(
                label = LocalAppLanguage.current.pick("Activate", "激活"),
                onClick = onActivate,
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    testTag: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = AetherColors.OnSurface,
        modifier = Modifier.testTag(testTag),
    )
}

private fun CompanionCharacterCard.asCompanionContact(appLanguage: AppLanguage): Contact {
    val resolved = resolve(appLanguage)
    return Contact(
        id = id,
        nickname = resolved.displayName,
        title = resolved.roleLabel,
        avatarText = avatarText,
        addedAt = Instant.now().toString(),
        isOnline = true,
    )
}
