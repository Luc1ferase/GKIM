package com.gkim.im.android.core.designsystem

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText

object ContentPolicyCopy {

    const val currentVersion: String = "2026-04-23-v1"

    val title: LocalizedText = LocalizedText(
        english = "Content policy",
        chinese = "内容政策",
    )

    val body: LocalizedText = LocalizedText(
        english = """
            This companion experience is intended for adults who enjoy roleplay and creative writing with AI.

            You agree that:
            • You will not attempt to generate content depicting self-harm, suicide, illegal activity, or sexual material involving minors.
            • You understand that AI replies may be filtered or blocked by provider policy; the app will surface a reason when that happens.
            • You understand that companion memory and chat logs are stored on your account so that conversations stay consistent across sessions.
            • You are responsible for the content you send. If you encounter material that violates this policy, stop the conversation and report the incident.

            Accepting this policy unlocks the tavern and companion chat. You can revisit this acknowledgment anytime from Settings → Content & Safety.
        """.trimIndent(),
        chinese = """
            陪伴功能面向喜欢与 AI 进行角色扮演和创意写作的成年用户。

            你同意：
            • 不会尝试生成涉及自伤、自杀、违法活动或涉及未成年人的色情内容。
            • 了解 AI 回复可能会被服务商策略过滤或拦截；遇到拦截时,应用会展示原因。
            • 了解陪伴记忆和聊天记录会保存在你的账号中,以便对话在多次会话间保持连贯。
            • 对你发送的内容负责。如遇到违反本政策的内容,请停止对话并上报事件。

            接受本政策即可开启酒馆和陪伴聊天。你可随时在「设置 → 内容与安全」中查看此确认。
        """.trimIndent(),
    )

    val acceptCta: LocalizedText = LocalizedText(
        english = "I accept",
        chinese = "我接受",
    )

    val accepting: LocalizedText = LocalizedText(
        english = "Sending acknowledgment…",
        chinese = "正在发送确认…",
    )

    val acceptedCopy: LocalizedText = LocalizedText(
        english = "Thanks — your acknowledgment was saved.",
        chinese = "感谢确认 — 已保存你的选择。",
    )

    val errorFallback: LocalizedText = LocalizedText(
        english = "Couldn't send acknowledgment. Check your connection and try again.",
        chinese = "无法发送确认。请检查网络连接后重试。",
    )

    fun localizedTitle(language: AppLanguage): String = title.resolve(language)

    fun localizedBody(language: AppLanguage): String = body.resolve(language)
}
