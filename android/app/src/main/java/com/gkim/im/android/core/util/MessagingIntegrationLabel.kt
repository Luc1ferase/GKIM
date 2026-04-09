package com.gkim.im.android.core.util

import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.data.repository.MessagingIntegrationPhase
import com.gkim.im.android.data.repository.MessagingIntegrationState

fun messagingIntegrationStatusLabel(
    state: MessagingIntegrationState,
    appLanguage: AppLanguage,
): String {
    state.message?.let { return it }
    return when (state.phase) {
        MessagingIntegrationPhase.Idle -> appLanguage.pick("Waiting for live IM configuration", "等待 live IM 配置")
        MessagingIntegrationPhase.Authenticating -> appLanguage.pick("Authenticating live IM session", "正在认证 live IM 会话")
        MessagingIntegrationPhase.Bootstrapping -> appLanguage.pick("Hydrating live conversations", "正在拉取 live 会话")
        MessagingIntegrationPhase.RealtimeConnecting -> appLanguage.pick("Connecting realtime gateway", "正在连接实时网关")
        MessagingIntegrationPhase.Ready -> appLanguage.pick("Live IM connected", "live IM 已连接")
        MessagingIntegrationPhase.Error -> appLanguage.pick("Live IM error", "live IM 出错")
    }
}
