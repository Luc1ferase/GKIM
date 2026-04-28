package com.gkim.im.android.feature.tavern

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Preset

internal val DefaultPresetOverrideLabel: LocalizedText = LocalizedText(
    english = "Default (follow global active)",
    chinese = "默认（跟随全局活跃 preset）",
)

internal data class PresetOverrideOption(
    val presetId: String?,
    val displayName: LocalizedText,
)

internal fun presetOverrideOptions(presets: List<Preset>): List<PresetOverrideOption> =
    listOf(PresetOverrideOption(presetId = null, displayName = DefaultPresetOverrideLabel)) +
        presets.map { PresetOverrideOption(presetId = it.id, displayName = it.displayName) }

internal fun resolvePresetOverrideRowLabel(
    characterPresetId: String?,
    presets: List<Preset>,
    language: AppLanguage,
): String = when {
    characterPresetId == null -> DefaultPresetOverrideLabel.resolve(language)
    else -> presets.firstOrNull { it.id == characterPresetId }?.displayName?.resolve(language)
        ?: when (language) {
            AppLanguage.English -> "Override (preset removed)"
            AppLanguage.Chinese -> "覆盖（preset 已移除）"
        }
}

internal fun applyPresetSelection(
    card: CompanionCharacterCard,
    selectedPresetId: String?,
): CompanionCharacterCard = card.copy(characterPresetId = selectedPresetId)

@Composable
internal fun PresetOverrideRow(
    characterPresetId: String?,
    availablePresets: List<Preset>,
    onPresetSelected: (String?) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    var pickerOpen by remember { mutableStateOf(false) }
    val rowLabel = resolvePresetOverrideRowLabel(characterPresetId, availablePresets, appLanguage)

    GlassCard(
        modifier = Modifier
            .testTag("character-editor-preset-override")
            .clickable { pickerOpen = true },
    ) {
        Text(
            text = appLanguage.pick("Override preset", "覆盖 preset"),
            style = MaterialTheme.typography.titleMedium,
            color = AetherColors.OnSurface,
        )
        Text(
            text = rowLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = AetherColors.Primary,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .testTag("character-editor-preset-override-current"),
        )
        Text(
            text = appLanguage.pick(
                "Tap to choose a preset for this card, or pick Default to follow the global active preset.",
                "点击为当前卡片选择 preset，或选择「默认」跟随全局活跃 preset。",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = AetherColors.OnSurfaceVariant,
        )
    }

    if (pickerOpen) {
        AlertDialog(
            modifier = Modifier.testTag("character-editor-preset-override-picker"),
            onDismissRequest = { pickerOpen = false },
            confirmButton = {
                TextButton(onClick = { pickerOpen = false }) {
                    Text(appLanguage.pick("Cancel", "取消"))
                }
            },
            title = {
                Text(appLanguage.pick("Choose preset override", "选择 preset 覆盖"))
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presetOverrideOptions(availablePresets).forEach { option ->
                        val isSelected = option.presetId == characterPresetId
                        Text(
                            text = option.displayName.resolve(appLanguage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) AetherColors.Primary else AetherColors.OnSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    AetherColors.SurfaceContainerHigh,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable {
                                    pickerOpen = false
                                    onPresetSelected(option.presetId)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag(
                                    "character-editor-preset-override-option-${option.presetId ?: "default"}",
                                ),
                        )
                    }
                }
            },
        )
    }
}
