package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage

data class ChatChromeMemoryEntry(
    val label: String,
    val destinationRoute: String,
    val cardId: String?,
    val isEnabled: Boolean,
)

object ChatChromeMemoryEntryDefaults {
    const val DestinationRoutePrefix: String = "memory-panel"
    const val LabelEnglish: String = "Memory"
    const val LabelChinese: String = "记忆"
}

fun chatChromeMemoryEntry(
    cardId: String?,
    language: AppLanguage,
): ChatChromeMemoryEntry {
    val resolvedLabel = language.pick(
        ChatChromeMemoryEntryDefaults.LabelEnglish,
        ChatChromeMemoryEntryDefaults.LabelChinese,
    )
    val route = if (cardId != null) {
        "${ChatChromeMemoryEntryDefaults.DestinationRoutePrefix}/$cardId"
    } else {
        ChatChromeMemoryEntryDefaults.DestinationRoutePrefix
    }
    return ChatChromeMemoryEntry(
        label = resolvedLabel,
        destinationRoute = route,
        cardId = cardId,
        isEnabled = cardId != null,
    )
}
