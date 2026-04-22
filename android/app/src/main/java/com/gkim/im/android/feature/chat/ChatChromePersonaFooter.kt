package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.UserPersona

data class ChatChromePersonaFooter(
    val text: String,
    val contentDescription: String,
    val activePersonaId: String,
)

object ChatChromePersonaFooterDefaults {
    const val EnglishPrefix: String = "Talking as "
    const val ChinesePrefix: String = "以 "
    const val ChineseSuffix: String = " 的身份对话"

    fun formatEnglish(name: String): String = "$EnglishPrefix$name"
    fun formatChinese(name: String): String = "$ChinesePrefix$name$ChineseSuffix"
}

fun chatChromePersonaFooter(
    activePersona: UserPersona?,
    language: AppLanguage,
): ChatChromePersonaFooter? {
    if (activePersona == null) return null
    val name = activePersona.displayName.resolve(language)
    val text = language.pick(
        english = ChatChromePersonaFooterDefaults.formatEnglish(name),
        chinese = ChatChromePersonaFooterDefaults.formatChinese(name),
    )
    return ChatChromePersonaFooter(
        text = text,
        contentDescription = text,
        activePersonaId = activePersona.id,
    )
}
