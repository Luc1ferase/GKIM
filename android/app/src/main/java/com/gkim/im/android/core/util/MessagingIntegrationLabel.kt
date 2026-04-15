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
        MessagingIntegrationPhase.Idle -> appLanguage.pick("Waiting for connection", "等待连接")
        MessagingIntegrationPhase.Authenticating -> appLanguage.pick("Signing in", "正在登录")
        MessagingIntegrationPhase.Bootstrapping -> appLanguage.pick("Syncing conversations", "正在同步会话")
        MessagingIntegrationPhase.RealtimeConnecting -> appLanguage.pick("Connecting", "正在连接")
        MessagingIntegrationPhase.Ready -> appLanguage.pick("Connected", "已连接")
        MessagingIntegrationPhase.Error -> appLanguage.pick("Connection issue", "连接异常")
    }
}
