package com.gkim.im.android.feature.tavern

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.data.repository.MessagingRepository
import kotlinx.coroutines.launch

/**
 * §3.1 — Compose composable that renders the §6.1 [RelationshipResetAffordanceState]
 * state machine through five visible UI phases:
 *  - Idle: trigger button only.
 *  - Armed: confirmation banner + Cancel/Confirm.
 *  - Submitting: disabled in-flight indicator.
 *  - Completed: auto-dismiss back to Idle (parent observes via `onCompleted` callback).
 *  - Failed: inline error copy + retry button (retry advances Failed → Submitting directly,
 *    bypassing the two-step gate per §6.1 / §9.2 spec).
 *
 * Localized error copy maps the two known failure codes (`character_not_available` /
 * `network_failure`) to EN/ZH messages; unknown codes fall through to a generic copy.
 */
@Composable
fun RelationshipResetButton(
    characterId: String,
    repository: MessagingRepository,
    onCompleted: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val appLanguage = LocalAppLanguage.current
    val englishLocale = appLanguage == AppLanguage.English
    val coroutineScope = rememberCoroutineScope()
    var state by remember(characterId) {
        mutableStateOf(RelationshipResetAffordanceState(characterId = characterId))
    }

    fun submit() {
        coroutineScope.launch {
            val result = repository.resetRelationship(characterId)
            state = result.fold(
                onSuccess = { state.markCompleted() },
                onFailure = { t -> state.markFailed(t.message ?: "network_failure") },
            )
        }
    }

    LaunchedEffect(state.phase) {
        if (state.phase == RelationshipResetPhase.Submitting) {
            submit()
        }
        if (state.phase == RelationshipResetPhase.Completed) {
            onCompleted()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("relationship-reset-affordance"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (state.phase) {
            RelationshipResetPhase.Idle -> {
                ResetTriggerButton(englishLocale = englishLocale, enabled = true) {
                    state = state.arm()
                }
            }

            RelationshipResetPhase.Completed -> {
                Text(
                    text = com.gkim.im.android.core.strings.CompanionStrings.PostRelationshipReset.pick(appLanguage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurfaceVariant,
                    modifier = Modifier.testTag("relationship-reset-completed-copy"),
                )
                ResetTriggerButton(englishLocale = englishLocale, enabled = true) {
                    state = state.arm()
                }
            }

            RelationshipResetPhase.Armed -> {
                ConfirmationBanner(
                    englishLocale = englishLocale,
                    onCancel = { state = state.cancel() },
                    onConfirm = { state = state.confirm() },
                )
            }

            RelationshipResetPhase.Submitting -> {
                DisabledTriggerLabel(
                    englishLocale = englishLocale,
                    label = if (englishLocale) "Resetting…" else "重置中…",
                )
            }

            RelationshipResetPhase.Failed -> {
                FailedRow(
                    englishLocale = englishLocale,
                    code = state.errorCode.orEmpty(),
                    onRetry = { state = state.retry() },
                )
            }
        }
    }
}

@Composable
private fun ResetTriggerButton(
    englishLocale: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = if (englishLocale) "Reset relationship" else "重置关系"
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = AetherColors.Danger,
        modifier = Modifier
            .background(AetherColors.SurfaceContainerHigh, shape = RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("relationship-reset-trigger"),
    )
}

@Composable
private fun ConfirmationBanner(
    englishLocale: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val message = if (englishLocale) {
        "Reset will delete all conversations, memory, and the last selected greeting for this companion. This cannot be undone."
    } else {
        "重置将删除该伙伴的所有对话、记忆和上次选择的开场白，此操作不可撤销。"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AetherColors.SurfaceContainerLow, shape = RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("relationship-reset-confirmation-banner"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = AetherColors.OnSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (englishLocale) "Cancel" else "取消",
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.OnSurfaceVariant,
                modifier = Modifier
                    .background(AetherColors.SurfaceContainerHigh, shape = CircleShape)
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("relationship-reset-cancel"),
            )
            Text(
                text = if (englishLocale) "Confirm reset" else "确认重置",
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.OnSurface,
                modifier = Modifier
                    .background(AetherColors.Danger, shape = CircleShape)
                    .clickable(onClick = onConfirm)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("relationship-reset-confirm"),
            )
        }
    }
}

@Composable
private fun DisabledTriggerLabel(@Suppress("UNUSED_PARAMETER") englishLocale: Boolean, label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = AetherColors.OnSurfaceVariant,
        modifier = Modifier
            .background(AetherColors.SurfaceContainerHigh, shape = RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("relationship-reset-submitting"),
    )
}

@Composable
private fun FailedRow(
    englishLocale: Boolean,
    code: String,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = relationshipResetErrorCopy(code = code, englishLocale = englishLocale),
            style = MaterialTheme.typography.bodySmall,
            color = AetherColors.Danger,
            modifier = Modifier.testTag("relationship-reset-error"),
        )
        Text(
            text = if (englishLocale) "Retry" else "重试",
            style = MaterialTheme.typography.labelLarge,
            color = AetherColors.OnSurface,
            modifier = Modifier
                .background(AetherColors.PrimaryContainer, shape = CircleShape)
                .clickable(onClick = onRetry)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("relationship-reset-retry"),
        )
    }
}

internal fun relationshipResetErrorCopy(code: String, englishLocale: Boolean): String = when (code) {
    "character_not_available" -> if (englishLocale) {
        "This companion is no longer available."
    } else {
        "该伙伴已不可用。"
    }
    "network_failure" -> if (englishLocale) {
        "Network error during reset. Please try again."
    } else {
        "重置过程中发生网络错误，请稍后重试。"
    }
    else -> if (englishLocale) "Reset failed: $code" else "重置失败：$code"
}
