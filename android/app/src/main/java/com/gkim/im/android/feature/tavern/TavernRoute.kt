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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    LazyColumn(
        modifier = Modifier
            .background(AetherColors.Surface)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("tavern-screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            PageHeader(
                title = appLanguage.pick("Tavern", "酒馆"),
                actionLabel = appLanguage.pick("Create", "新建"),
                onAction = onCreateCharacter,
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
                HeaderPill(label = appLanguage.pick("Settings", "设置"), onClick = onOpenSettings)
                HeaderPill(label = appLanguage.pick("Draw", "抽卡"), onClick = onDraw)
                Text(
                    text = appLanguage.pick("Import card", "导入卡"),
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.OnSurface,
                    modifier = Modifier
                        .background(AetherColors.SurfaceContainerHigh, shape = androidx.compose.foundation.shape.CircleShape)
                        .clickable(onClick = onImportCard)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("tavern-import-entry"),
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
                Box(
                    modifier = Modifier
                        .padding(end = 14.dp)
                        .size(44.dp)
                        .background(AetherColors.Primary.copy(alpha = 0.18f), androidx.compose.foundation.shape.CircleShape)
                        .clickable(onClick = onAvatarTap)
                        .testTag("$testTag-avatar"),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text(
                        text = character.avatarText,
                        style = MaterialTheme.typography.labelLarge,
                        color = AetherColors.Primary,
                    )
                }
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
