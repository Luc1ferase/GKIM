package com.gkim.im.android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.MacroSubstitution
import com.gkim.im.android.core.model.resolve

data class CompanionGreetingOption(
    val index: Int,
    val label: String,
    val body: String,
)

internal fun resolveCompanionGreetings(
    card: CompanionCharacterCard?,
    language: AppLanguage,
): List<CompanionGreetingOption> {
    if (card == null) return emptyList()
    val resolved = card.resolve(language)
    val options = mutableListOf<CompanionGreetingOption>()
    if (resolved.firstMes.isNotBlank()) {
        options += CompanionGreetingOption(
            index = options.size,
            label = "Greeting",
            body = resolved.firstMes,
        )
    }
    resolved.alternateGreetings.forEachIndexed { i, greeting ->
        if (greeting.isNotBlank()) {
            options += CompanionGreetingOption(
                index = options.size,
                label = "Alt ${i + 1}",
                body = greeting,
            )
        }
    }
    return options
}

internal fun shouldShowGreetingPicker(
    companionPathIsEmpty: Boolean,
    options: List<CompanionGreetingOption>,
): Boolean = companionPathIsEmpty && options.isNotEmpty()

internal fun applyPersonaMacros(
    options: List<CompanionGreetingOption>,
    userDisplayName: String,
    charDisplayName: String,
): List<CompanionGreetingOption> = options.map { option ->
    option.copy(
        body = MacroSubstitution.substituteMacros(
            template = option.body,
            userDisplayName = userDisplayName,
            charDisplayName = charDisplayName,
        ),
    )
}

/**
 * §2.1 — shorten a greeting body to a ~120-character preview. The returned preview is
 * suitable for in-line rendering; tapping an option opens a modal that shows the full body.
 *
 * Behavior:
 * - `body.length <= limit`: returned unchanged (including trailing whitespace — tests assert
 *   exact equality).
 * - `body.length > limit`: returned as the first `limit` characters with trailing whitespace
 *   trimmed and a single ellipsis character "…" appended. The ellipsis is one codepoint so
 *   the visible length is `<= limit + 1`.
 *
 * Character count is `String.length` (UTF-16 code units). For the common content we expect —
 * companion greetings in English and Chinese — every displayed character is a single UTF-16
 * code unit, so the count matches visual perception. Emoji + surrogate pairs are counted by
 * code unit, which can cut a pair; that is accepted for a preview.
 */
internal fun truncatePreview(body: String, limit: Int = 120): String {
    if (limit <= 0) return ""
    return if (body.length <= limit) {
        body
    } else {
        body.substring(0, limit).trimEnd() + "…"
    }
}

/**
 * §2.2 — pick the default-highlighted greeting for the opener picker.
 *
 * Returns:
 * - `null` when no options are available.
 * - `lastSelected` when it falls within `options.indices` (and options is non-empty).
 * - `0` otherwise (first option is the default when there is no recorded selection or the
 *   recorded selection is stale / out-of-range).
 *
 * The caller renders a "Remembered from last time" caption next to the default row when
 * `lastSelected != null && lastSelected == defaultSelectionIndex(options, lastSelected)`.
 */
internal fun defaultSelectionIndex(
    options: List<CompanionGreetingOption>,
    lastSelected: Int?,
): Int? {
    if (options.isEmpty()) return null
    if (lastSelected != null && lastSelected in options.indices) return lastSelected
    return 0
}

@Composable
fun CompanionGreetingPicker(
    options: List<CompanionGreetingOption>,
    onSelect: (CompanionGreetingOption) -> Unit,
    rememberedIndex: Int? = null,
    previewLimit: Int = 120,
) {
    if (options.isEmpty()) return
    val appLanguage = LocalAppLanguage.current
    var previewing by remember { mutableStateOf<CompanionGreetingOption?>(null) }
    val defaultIdx = defaultSelectionIndex(options, rememberedIndex)
    val rememberedMatches = rememberedIndex != null && defaultIdx == rememberedIndex

    GlassCard(modifier = Modifier.testTag("chat-companion-greeting-picker")) {
        Text(
            text = appLanguage.pick("Pick an opening line", "选一句开场"),
            style = MaterialTheme.typography.labelLarge,
            color = AetherColors.Primary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val isDefault = defaultIdx == option.index
                val showRemembered = rememberedMatches && isDefault
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AetherColors.SurfaceContainerHigh, RoundedCornerShape(18.dp))
                        .then(
                            if (isDefault) {
                                Modifier.border(1.dp, AetherColors.Primary, RoundedCornerShape(18.dp))
                            } else {
                                Modifier
                            }
                        )
                        .clickable { previewing = option }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("chat-companion-greeting-option-${option.index}"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = AetherColors.OnSurfaceVariant,
                    )
                    if (showRemembered) {
                        Text(
                            text = appLanguage.pick("Remembered from last time", "沿用上次选择"),
                            style = MaterialTheme.typography.labelSmall,
                            color = AetherColors.Primary,
                            modifier = Modifier.testTag("chat-companion-greeting-remembered-${option.index}"),
                        )
                    }
                    Text(
                        text = truncatePreview(option.body, previewLimit),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AetherColors.OnSurface,
                        modifier = Modifier.testTag("chat-companion-greeting-preview-${option.index}"),
                    )
                }
            }
        }
    }

    previewing?.let { option ->
        AltGreetingPreviewModal(
            option = option,
            onDismiss = { previewing = null },
            onCommit = {
                previewing = null
                onSelect(option)
            },
        )
    }
}

@Composable
private fun AltGreetingPreviewModal(
    option: CompanionGreetingOption,
    onDismiss: () -> Unit,
    onCommit: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AetherColors.Surface, RoundedCornerShape(24.dp))
                .padding(24.dp)
                .testTag("chat-companion-greeting-preview-modal"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = option.label,
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.OnSurfaceVariant,
            )
            Text(
                text = option.body,
                style = MaterialTheme.typography.bodyLarge,
                color = AetherColors.OnSurface,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .testTag("chat-companion-greeting-preview-modal-body"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = appLanguage.pick("Use this greeting", "使用此开场"),
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.OnSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AetherColors.Primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onCommit)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("chat-companion-greeting-preview-modal-commit"),
                )
                Text(
                    text = appLanguage.pick("Cancel", "取消"),
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.OnSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AetherColors.SurfaceContainerHigh, RoundedCornerShape(14.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("chat-companion-greeting-preview-modal-dismiss"),
                )
            }
        }
    }
}
