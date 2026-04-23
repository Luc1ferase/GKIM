package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.Preset

data class ChatChromePresetPill(
    val label: String,
    val destinationRoute: String,
    val activePresetId: String?,
)

object ChatChromePresetPillDefaults {
    const val DestinationRoute: String = "settings"
    const val FallbackLabelEnglish: String = "Choose preset"
    const val FallbackLabelChinese: String = "选择预设"
}

fun chatChromePresetPill(
    activePreset: Preset?,
    language: AppLanguage,
): ChatChromePresetPill {
    val label = activePreset?.displayName?.resolve(language)
        ?: language.pick(
            ChatChromePresetPillDefaults.FallbackLabelEnglish,
            ChatChromePresetPillDefaults.FallbackLabelChinese,
        )
    return ChatChromePresetPill(
        label = label,
        destinationRoute = ChatChromePresetPillDefaults.DestinationRoute,
        activePresetId = activePreset?.id,
    )
}
