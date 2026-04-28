package com.gkim.im.android.core.designsystem

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.FailedSubtype
import com.gkim.im.android.core.model.LocalizedText

object SafetyCopy {

    fun localizedFailedCopy(subtype: FailedSubtype): LocalizedText = when (subtype) {
        FailedSubtype.Transient -> LocalizedText(
            english = "Something went wrong. Please try again.",
            chinese = "出了点问题,请重试。",
        )
        FailedSubtype.PromptBudgetExceeded -> LocalizedText(
            english = "Your message is longer than the model can handle. Please shorten it and try again.",
            chinese = "消息超出了模型可承载的长度,请缩短后再试。",
        )
        FailedSubtype.AuthenticationFailed -> LocalizedText(
            english = "We couldn't authenticate your session. Please sign in again.",
            chinese = "无法验证您的会话,请重新登录。",
        )
        FailedSubtype.ProviderUnavailable -> LocalizedText(
            english = "The AI provider is temporarily unavailable. Please try again in a moment.",
            chinese = "AI 服务商当前不可用,请稍后再试。",
        )
        FailedSubtype.NetworkError -> LocalizedText(
            english = "A network error interrupted the reply. Check your connection and try again.",
            chinese = "网络错误中断了回复,请检查网络后重试。",
        )
        FailedSubtype.Unknown -> LocalizedText(
            english = "The reply failed for an unknown reason. Please try again.",
            chinese = "回复因未知原因失败,请重试。",
        )
    }

    fun localizedFailedCopy(subtype: FailedSubtype, language: AppLanguage): String =
        localizedFailedCopy(subtype).resolve(language)

    val timeoutCopy: LocalizedText = LocalizedText(
        english = "The AI took too long to respond. Please try again.",
        chinese = "AI 响应超时,请重试。",
    )

    fun localizedTimeoutCopy(language: AppLanguage): String = timeoutCopy.resolve(language)

    val timeoutPresetHint: LocalizedText = LocalizedText(
        english = "Switching to a shorter preset may help.",
        chinese = "切换到更简短的预设可能有帮助。",
    )

    fun localizedTimeoutPresetHint(language: AppLanguage): String = timeoutPresetHint.resolve(language)
}
