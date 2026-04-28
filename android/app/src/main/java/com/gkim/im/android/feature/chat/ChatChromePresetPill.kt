package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.Preset

data class ChatChromePresetPill(
    val label: String,
    val destinationRoute: String,
    val activePresetId: String?,
    val isOverride: Boolean = false,
)

object ChatChromePresetPillDefaults {
    const val DestinationRoute: String = "settings"
    const val FallbackLabelEnglish: String = "Choose preset"
    const val FallbackLabelChinese: String = "选择预设"

    /**
     * Suffix appended to the pill label when the active companion card carries a
     * `characterPresetId` override. The English form has a leading space so it follows the
     * preset name with normal Western punctuation; the Chinese form uses full-width
     * parentheses with no leading space, matching the convention used elsewhere in the
     * tavern surfaces ("覆盖" rather than "(card override)").
     */
    const val OverrideSuffixEnglish: String = " (card override)"
    const val OverrideSuffixChinese: String = "（卡片覆盖）"

    /**
     * Fallback base label when an override id is set but the matching preset has been
     * removed from the library (rare but possible if the preset was deleted after the card
     * captured the override). The user still sees that an override is active, distinct
     * from the no-override default-label case.
     */
    const val OverrideRemovedFallbackEnglish: String = "Override (preset removed)"
    const val OverrideRemovedFallbackChinese: String = "覆盖（preset 已移除）"

    /**
     * Route prefix for the character-detail surface where the override can be cleared.
     * Matches the existing `tavern/detail/{characterId}` composable in the nav graph
     * (no new route created).
     */
    const val OverrideDestinationRoutePrefix: String = "tavern/detail"
}

/**
 * Builds the chat-chrome preset pill data shape.
 *
 * - When `activeCardCharacterPresetId` is null (or no card is active), the pill behaves as
 *   it did before §4.3 landed: the label resolves the global `activePreset`'s display name
 *   (or the bilingual fallback when no preset is active), the route points at `"settings"`,
 *   and `isOverride` is false. No suffix is appended.
 *
 * - When `activeCardCharacterPresetId` is non-null, the pill renders the per-character
 *   override: the label resolves the override preset's display name (or the bilingual
 *   "Override (preset removed)" fallback when the id no longer matches a known preset)
 *   plus a localized "(card override)" suffix; the route points at the active card's
 *   `tavern/detail/<id>` surface (where §4.2's editor row can clear the override); and
 *   `isOverride` is true so the renderer can apply a visually distinct treatment.
 *   When `activeCardId` is omitted, the route degrades to `"settings"` rather than
 *   producing a malformed `tavern/detail/` link.
 */
fun chatChromePresetPill(
    activePreset: Preset?,
    language: AppLanguage,
    activeCardCharacterPresetId: String? = null,
    activeCardId: String? = null,
    presets: List<Preset> = emptyList(),
): ChatChromePresetPill {
    val isOverride = activeCardCharacterPresetId != null
    return if (isOverride) {
        val overridePreset = presets.firstOrNull { it.id == activeCardCharacterPresetId }
        val baseLabel = overridePreset?.displayName?.resolve(language)
            ?: language.pick(
                ChatChromePresetPillDefaults.OverrideRemovedFallbackEnglish,
                ChatChromePresetPillDefaults.OverrideRemovedFallbackChinese,
            )
        val suffix = language.pick(
            ChatChromePresetPillDefaults.OverrideSuffixEnglish,
            ChatChromePresetPillDefaults.OverrideSuffixChinese,
        )
        val destination = activeCardId?.let {
            "${ChatChromePresetPillDefaults.OverrideDestinationRoutePrefix}/$it"
        } ?: ChatChromePresetPillDefaults.DestinationRoute
        ChatChromePresetPill(
            label = baseLabel + suffix,
            destinationRoute = destination,
            activePresetId = activeCardCharacterPresetId,
            isOverride = true,
        )
    } else {
        val label = activePreset?.displayName?.resolve(language)
            ?: language.pick(
                ChatChromePresetPillDefaults.FallbackLabelEnglish,
                ChatChromePresetPillDefaults.FallbackLabelChinese,
            )
        ChatChromePresetPill(
            label = label,
            destinationRoute = ChatChromePresetPillDefaults.DestinationRoute,
            activePresetId = activePreset?.id,
            isOverride = false,
        )
    }
}
