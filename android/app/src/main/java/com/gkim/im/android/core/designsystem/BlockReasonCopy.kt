package com.gkim.im.android.core.designsystem

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.BlockReason
import com.gkim.im.android.core.model.LocalizedText

object BlockReasonCopy {

    fun localizedCopy(reason: BlockReason): LocalizedText = when (reason) {
        BlockReason.SelfHarm -> LocalizedText(
            english = "This conversation involves self-harm or suicide-related content. If you need real support, please reach out to a local helpline.",
            chinese = "此对话涉及自伤或自杀相关内容。如需现实支持,请联系当地心理援助热线。",
        )
        BlockReason.Illegal -> LocalizedText(
            english = "This content isn't allowed because it touches on illegal activity.",
            chinese = "此内容涉及违法活动,无法生成。",
        )
        BlockReason.NsfwDenied -> LocalizedText(
            english = "The current provider or policy doesn't allow this content.",
            chinese = "当前服务商或策略不允许此类内容。",
        )
        BlockReason.MinorSafety -> LocalizedText(
            english = "This content is restricted to protect minors.",
            chinese = "为保护未成年人,此内容受到限制。",
        )
        BlockReason.ProviderRefusal -> LocalizedText(
            english = "The AI provider declined to generate this reply.",
            chinese = "AI 服务商拒绝生成此回复。",
        )
        BlockReason.Other -> LocalizedText(
            english = "This reply was blocked. Try rephrasing or choosing a different direction.",
            chinese = "此回复已被拦截。请尝试换种说法或换个方向。",
        )
    }

    fun localizedCopy(reason: BlockReason, language: AppLanguage): String =
        localizedCopy(reason).resolve(language)
}
