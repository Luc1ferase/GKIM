package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.UserPersona

data class ChatChromePersonaPill(
    val label: String,
    val destinationRoute: String,
    val activePersonaId: String?,
)

object ChatChromePersonaPillDefaults {
    const val DestinationRoute: String = "settings"
    const val FallbackLabelEnglish: String = "Choose persona"
    const val FallbackLabelChinese: String = "选择角色"
}

fun chatChromePersonaPill(
    activePersona: UserPersona?,
    language: AppLanguage,
): ChatChromePersonaPill {
    val label = activePersona?.displayName?.resolve(language)
        ?: language.pick(
            ChatChromePersonaPillDefaults.FallbackLabelEnglish,
            ChatChromePersonaPillDefaults.FallbackLabelChinese,
        )
    return ChatChromePersonaPill(
        label = label,
        destinationRoute = ChatChromePersonaPillDefaults.DestinationRoute,
        activePersonaId = activePersona?.id,
    )
}
