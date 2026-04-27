package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.MessageStatus
import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.repository.CompanionTurnRepository
import com.gkim.im.android.data.repository.ConversationTurnTree
import com.gkim.im.android.data.repository.ExportedChatPayload
import com.gkim.im.android.data.repository.FailedCompanionSubmission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §4.2 verification — `invokeChatExport(...)` composes the repository's export call with a
 * platform dispatcher and projects the outcome into [ChatExportInvocationOutcome]. Pure
 * Kotlin test (no Android imports) using fake repository + fake dispatcher.
 */
class ChatExportInvocationTest {

    private val conversationId = "conversation-export-orchestrator"

    private val samplePayload = ExportedChatPayload(
        filename = "chat-export-active-path_conversa.jsonl",
        bytes = "{\"role\":\"u\"}\n".toByteArray(Charsets.UTF_8),
        contentType = "application/x-ndjson",
    )

    @Test
    fun `success path returns Success carrying payload and dialog target`() = runTest {
        val repo = StubRepository(exportResult = Result.success(samplePayload))
        val dispatcher = StubDispatcher(deliverResult = Result.success(Unit))
        val state = initialChatExportDialogState(AppLanguage.English)
            .withTarget(ChatExportTarget.Share)

        val outcome = invokeChatExport(conversationId, state, repo, dispatcher)

        outcome as ChatExportInvocationOutcome.Success
        assertEquals(samplePayload, outcome.payload)
        assertEquals(ChatExportTarget.Share, outcome.target)
    }

    @Test
    fun `repository failure short-circuits with Failed(code) and dispatcher is never called`() =
        runTest {
            val repo = StubRepository(
                exportResult = Result.failure(RuntimeException("404_unknown_conversation")),
            )
            val dispatcher = StubDispatcher(deliverResult = Result.success(Unit))
            val state = initialChatExportDialogState(AppLanguage.English)

            val outcome = invokeChatExport(conversationId, state, repo, dispatcher)

            outcome as ChatExportInvocationOutcome.Failed
            assertEquals("404_unknown_conversation", outcome.code)
            assertTrue("dispatcher must not run when repo fails", dispatcher.dispatchCalls.isEmpty())
        }

    @Test
    fun `dispatcher failure surfaces the dispatcher's error code`() = runTest {
        val repo = StubRepository(exportResult = Result.success(samplePayload))
        val dispatcher = StubDispatcher(
            deliverResult = Result.failure(RuntimeException("no_share_target")),
        )
        val state = initialChatExportDialogState(AppLanguage.English)
            .withTarget(ChatExportTarget.Share)

        val outcome = invokeChatExport(conversationId, state, repo, dispatcher)

        outcome as ChatExportInvocationOutcome.Failed
        assertEquals("no_share_target", outcome.code)
    }

    @Test
    fun `pathOnly toggle flows to the repository call`() = runTest {
        val repo = StubRepository(exportResult = Result.success(samplePayload))
        val dispatcher = StubDispatcher(deliverResult = Result.success(Unit))
        val state = initialChatExportDialogState(AppLanguage.English).withPathOnly(false)

        invokeChatExport(conversationId, state, repo, dispatcher)

        assertEquals(false, repo.exportCalls.single().pathOnly)
        assertEquals("jsonl", repo.exportCalls.single().format)
    }

    @Test
    fun `target choice flows to the dispatcher call`() = runTest {
        val repo = StubRepository(exportResult = Result.success(samplePayload))
        val dispatcher = StubDispatcher(deliverResult = Result.success(Unit))
        val state = initialChatExportDialogState(AppLanguage.English)
            .withTarget(ChatExportTarget.Downloads)

        invokeChatExport(conversationId, state, repo, dispatcher)

        assertEquals(ChatExportTarget.Downloads, dispatcher.dispatchCalls.single().target)
        assertEquals(samplePayload, dispatcher.dispatchCalls.single().payload)
    }

    @Test
    fun `repository failure with null message falls back to export_failed sentinel`() = runTest {
        val repo = StubRepository(exportResult = Result.failure(StubException()))
        val dispatcher = StubDispatcher(deliverResult = Result.success(Unit))
        val state = initialChatExportDialogState(AppLanguage.English)

        val outcome = invokeChatExport(conversationId, state, repo, dispatcher)

        outcome as ChatExportInvocationOutcome.Failed
        assertEquals("export_failed", outcome.code)
    }

    private data class CapturedExport(
        val conversationId: String,
        val format: String,
        val pathOnly: Boolean,
    )

    private class StubRepository(
        private val exportResult: Result<ExportedChatPayload>,
    ) : CompanionTurnRepository {
        val exportCalls = mutableListOf<CapturedExport>()
        override val treeByConversation: StateFlow<Map<String, ConversationTurnTree>> =
            MutableStateFlow(emptyMap())
        override val activePathByConversation: StateFlow<Map<String, List<ChatMessage>>> =
            MutableStateFlow(emptyMap())
        override val failedSubmissions: StateFlow<Map<String, FailedCompanionSubmission>> =
            MutableStateFlow(emptyMap())
        override fun recordUserTurn(userMessage: ChatMessage, conversationId: String) = Unit
        override fun updateUserMessageStatus(conversationId: String, messageId: String, status: MessageStatus) = Unit
        override fun handleTurnStarted(event: ImGatewayEvent.CompanionTurnStarted) = Unit
        override fun handleTurnDelta(event: ImGatewayEvent.CompanionTurnDelta) = Unit
        override fun handleTurnCompleted(event: ImGatewayEvent.CompanionTurnCompleted) = Unit
        override fun handleTurnFailed(event: ImGatewayEvent.CompanionTurnFailed) = Unit
        override fun handleTurnBlocked(event: ImGatewayEvent.CompanionTurnBlocked) = Unit
        override fun handleTurnTimeout(event: ImGatewayEvent.CompanionTurnTimeout) = Unit
        override fun selectVariant(turnId: String, variantIndex: Int) = Unit
        override fun selectVariantByGroup(conversationId: String, variantGroupId: String, newIndex: Int) = Unit
        override fun applyRecord(record: CompanionTurnRecordDto) = Unit
        override suspend fun exportConversation(
            conversationId: String,
            format: String,
            pathOnly: Boolean,
        ): Result<ExportedChatPayload> {
            exportCalls += CapturedExport(conversationId, format, pathOnly)
            return exportResult
        }
    }

    private data class CapturedDispatch(
        val payload: ExportedChatPayload,
        val target: ChatExportTarget,
    )

    private class StubDispatcher(
        private val deliverResult: Result<Unit>,
    ) : ChatExportDispatcher {
        val dispatchCalls = mutableListOf<CapturedDispatch>()
        override suspend fun dispatch(
            payload: ExportedChatPayload,
            target: ChatExportTarget,
        ): Result<Unit> {
            dispatchCalls += CapturedDispatch(payload, target)
            return deliverResult
        }
    }

    private class StubException : RuntimeException() {
        override val message: String? = null
    }
}
