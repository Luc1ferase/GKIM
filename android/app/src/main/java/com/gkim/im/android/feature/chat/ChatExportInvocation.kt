package com.gkim.im.android.feature.chat

import com.gkim.im.android.data.repository.CompanionTurnRepository
import com.gkim.im.android.data.repository.ExportedChatPayload

/**
 * §2.2 — Pure orchestrator that composes [CompanionTurnRepository.exportConversation] with a
 * platform-specific [ChatExportDispatcher] and projects the outcome into a
 * [ChatExportInvocationOutcome] the §5.1 dialog state machine consumes:
 * `Success(payload, target)` flips the dialog to completed (auto-dismiss); `Failed(code)`
 * flips it to errored with localized copy.
 *
 * Mirrors `invokeCardExport(...)` exactly so the test pattern from
 * `CardExportPresentationTest` transfers (fake repo + fake dispatcher in unit-test scope; no
 * Android imports required at the orchestrator layer).
 */
fun interface ChatExportDispatcher {
    suspend fun dispatch(payload: ExportedChatPayload, target: ChatExportTarget): Result<Unit>
}

sealed interface ChatExportInvocationOutcome {
    data class Success(
        val payload: ExportedChatPayload,
        val target: ChatExportTarget,
    ) : ChatExportInvocationOutcome

    data class Failed(val code: String) : ChatExportInvocationOutcome
}

suspend fun invokeChatExport(
    conversationId: String,
    state: ChatExportDialogState,
    repository: CompanionTurnRepository,
    dispatcher: ChatExportDispatcher,
): ChatExportInvocationOutcome {
    val exportResult = repository.exportConversation(
        conversationId = conversationId,
        format = "jsonl",
        pathOnly = state.pathOnly,
    )
    val payload = exportResult.getOrElse {
        return ChatExportInvocationOutcome.Failed(it.message ?: "export_failed")
    }
    val deliverResult = dispatcher.dispatch(payload, state.target)
    return deliverResult.fold(
        onSuccess = { ChatExportInvocationOutcome.Success(payload, state.target) },
        onFailure = { ChatExportInvocationOutcome.Failed(it.message ?: "dispatch_failed") },
    )
}
