package com.gkim.im.android.feature.chat

/**
 * §5.2 — routing + filename contract for the JSONL chat export.
 *
 * The §5.1 [ChatExportDialogState] presentation contract carries the user's three control
 * choices (pathOnly toggle, language wire code, share-sheet vs. Downloads target) and the
 * lifecycle flags. §5.2 turns those choices into:
 *
 * 1. a [ChatExportRequestParams] for the wire call —
 *    `GET /api/conversations/{conversationId}/export?format=jsonl&pathOnly=true|false`;
 * 2. a [ChatExportFilename] including a `_<first8OfConversationId>` disambiguation suffix
 *    plus a path-only / full-tree marker and a `.jsonl` extension; and
 * 3. a [ChatExportDispatchTarget] describing how the resulting payload should be delivered
 *    (share sheet's intent vs. DownloadManager's display-name).
 *
 * The actual HTTP call (via `ImBackendClient.exportConversation`) and the platform-specific
 * dispatcher (Android share intent or `DownloadManager.enqueue`) are layered on top of these
 * helpers in the §5.2 wire-up; this slice locks the contract that the wire's `format` /
 * `pathOnly` query parameters and the filename's structure are deterministic given the
 * dialog state.
 */
internal data class ChatExportRequestParams(
    val conversationId: String,
    val format: String,
    val pathOnly: Boolean,
)

internal fun ChatExportDialogState.toExportRequestParams(
    conversationId: String,
): ChatExportRequestParams = ChatExportRequestParams(
    conversationId = conversationId,
    format = "jsonl",
    pathOnly = pathOnly,
)

/**
 * The canonical filename for a chat-export payload. The `_<first8OfConversationId>`
 * suffix disambiguates exports of the same conversation across multiple invocations
 * (e.g., one active-path and one full-tree export of the same chat).
 *
 * Format: `chat-export-<pathLabel>_<first8OfConversationId>.jsonl` where `pathLabel` is
 * `active-path` or `full-tree` per the §5.1 toggle. If the conversation id is shorter
 * than 8 characters, the whole id is used; if longer, only the first 8 are kept.
 */
internal fun chatExportFilename(
    conversationId: String,
    pathOnly: Boolean,
): String {
    val pathLabel = if (pathOnly) "active-path" else "full-tree"
    val disambiguator = conversationId.take(8)
    return "chat-export-${pathLabel}_${disambiguator}.jsonl"
}

internal sealed interface ChatExportDispatchTarget {
    data class Share(val mimeType: String = MIME_TYPE_JSONL) : ChatExportDispatchTarget
    data class Downloads(val displayName: String, val mimeType: String = MIME_TYPE_JSONL) : ChatExportDispatchTarget

    companion object {
        const val MIME_TYPE_JSONL: String = "application/x-ndjson"
    }
}

internal fun dispatchTargetFor(
    state: ChatExportDialogState,
    conversationId: String,
): ChatExportDispatchTarget {
    val filename = chatExportFilename(conversationId = conversationId, pathOnly = state.pathOnly)
    return when (state.target) {
        ChatExportTarget.Share -> ChatExportDispatchTarget.Share()
        ChatExportTarget.Downloads -> ChatExportDispatchTarget.Downloads(displayName = filename)
    }
}
