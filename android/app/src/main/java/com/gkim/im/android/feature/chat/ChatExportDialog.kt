package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AppLanguage

/**
 * §5.1 — presentation contract for the JSONL chat export dialog.
 *
 * The dialog offers three controls: an active-path-only / full-tree toggle, a target-language
 * selector (defaulted to the active [AppLanguage] when the dialog opens), and a share-sheet /
 * Downloads target selector. It also carries the in-flight / completed / failed lifecycle so the
 * dialog can disable controls while the export is running and surface the outcome on close.
 *
 * The §5.2 wire-up (`GET /api/conversations/{conversationId}/export?format=jsonl&pathOnly=...`
 * dispatcher + share-sheet vs. DownloadManager target routing) consumes this state via an
 * `invokeChatExport(...)` pipeline that mirrors the card export's dispatcher / outcome shape.
 */
enum class ChatExportTarget { Share, Downloads }

data class ChatExportDialogState(
    val pathOnly: Boolean = true,
    val language: String,
    val target: ChatExportTarget = ChatExportTarget.Share,
    val inFlight: Boolean = false,
    val errorCode: String? = null,
    val completed: Boolean = false,
)

internal fun initialChatExportDialogState(
    activeLanguage: AppLanguage,
): ChatExportDialogState = ChatExportDialogState(
    language = activeLanguage.toChatExportWireLanguage(),
)

internal fun AppLanguage.toChatExportWireLanguage(): String = when (this) {
    AppLanguage.English -> "en"
    AppLanguage.Chinese -> "zh"
}

internal fun ChatExportDialogState.withPathOnly(pathOnly: Boolean): ChatExportDialogState =
    copy(pathOnly = pathOnly)

internal fun ChatExportDialogState.withLanguage(language: String): ChatExportDialogState =
    copy(language = language)

internal fun ChatExportDialogState.withTarget(target: ChatExportTarget): ChatExportDialogState =
    copy(target = target)

internal fun ChatExportDialogState.markInFlight(): ChatExportDialogState =
    copy(inFlight = true, errorCode = null, completed = false)

internal fun ChatExportDialogState.markCompleted(): ChatExportDialogState =
    copy(inFlight = false, completed = true)

internal fun ChatExportDialogState.markFailed(code: String): ChatExportDialogState =
    copy(inFlight = false, errorCode = code, completed = false)
