package com.gkim.im.android.feature.chat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.designsystem.LocalAppLanguage
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * §5.1 verification — Compose `ChatExportDialog` with a fake repository + fake dispatcher,
 * exercise the three controls (path-only / language / target), submit, and assert the
 * dispatcher saw the right payload + target and the dialog auto-dismissed. Mirrors the
 * `CardExportInstrumentationTest` shape.
 */
@RunWith(AndroidJUnit4::class)
class ChatExportDialogInstrumentationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val conversationId = "conversation-export-instr-12345678"

    @Test
    fun dialogRendersAllControlSlots() {
        val repository = RecordingExportRepository(
            payload = ExportedChatPayload(
                filename = "chat-export-active-path_conversa.jsonl",
                bytes = "{}\n".toByteArray(Charsets.UTF_8),
                contentType = "application/x-ndjson",
            ),
        )
        val dispatcher = ChatExportDispatcher { _, _ -> Result.success(Unit) }

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                ChatExportDialog(
                    conversationId = conversationId,
                    repository = repository,
                    dispatcher = dispatcher,
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithTag("chat-export-dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-export-dialog-title").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-export-dialog-path-only-active").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-export-dialog-path-only-full").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-export-dialog-language-en").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-export-dialog-language-zh").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-export-dialog-target-share").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-export-dialog-target-downloads").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-export-dialog-cancel").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-export-dialog-submit").assertIsDisplayed()
    }

    @Test
    fun submitWithDefaultsRoutesActivePathShareAndAutoDismisses() {
        val payload = ExportedChatPayload(
            filename = "chat-export-active-path_conversa.jsonl",
            bytes = "{}\n".toByteArray(Charsets.UTF_8),
            contentType = "application/x-ndjson",
        )
        val repository = RecordingExportRepository(payload = payload)
        val dispatched = mutableListOf<Pair<ExportedChatPayload, ChatExportTarget>>()
        val dispatcher = ChatExportDispatcher { p, t ->
            dispatched += p to t
            Result.success(Unit)
        }
        var dismissed = false

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                ChatExportDialog(
                    conversationId = conversationId,
                    repository = repository,
                    dispatcher = dispatcher,
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithTag("chat-export-dialog-submit").performClick()
        composeRule.waitUntil(5_000) { dismissed }

        assertTrue(dismissed)
        val call = repository.exportCalls.single()
        assertEquals(conversationId, call.conversationId)
        assertEquals("jsonl", call.format)
        assertEquals(true, call.pathOnly)
        assertEquals(ChatExportTarget.Share, dispatched.single().second)
        assertEquals(payload, dispatched.single().first)
    }

    @Test
    fun toggleFullTreeAndDownloadsTargetFlowToRepoAndDispatcher() {
        val payload = ExportedChatPayload(
            filename = "chat-export-full-tree_conversa.jsonl",
            bytes = "{\"role\":\"u\"}\n".toByteArray(Charsets.UTF_8),
            contentType = "application/x-ndjson",
        )
        val repository = RecordingExportRepository(payload = payload)
        val dispatched = mutableListOf<Pair<ExportedChatPayload, ChatExportTarget>>()
        val dispatcher = ChatExportDispatcher { p, t ->
            dispatched += p to t
            Result.success(Unit)
        }
        var dismissed = false

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.Chinese) {
                ChatExportDialog(
                    conversationId = conversationId,
                    repository = repository,
                    dispatcher = dispatcher,
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithTag("chat-export-dialog-path-only-full").performClick()
        composeRule.onNodeWithTag("chat-export-dialog-language-zh").performClick()
        composeRule.onNodeWithTag("chat-export-dialog-target-downloads").performClick()
        composeRule.onNodeWithTag("chat-export-dialog-submit").performClick()
        composeRule.waitUntil(5_000) { dismissed }

        val call = repository.exportCalls.single()
        assertEquals(false, call.pathOnly)
        assertEquals(ChatExportTarget.Downloads, dispatched.single().second)
    }

    @Test
    fun repositoryFailureRendersErrorAndKeepsDialogOpen() {
        val repository = RecordingExportRepository(
            failure = RuntimeException("404_unknown_conversation"),
        )
        val dispatcher = ChatExportDispatcher { _, _ -> Result.success(Unit) }
        var dismissed = false

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                ChatExportDialog(
                    conversationId = conversationId,
                    repository = repository,
                    dispatcher = dispatcher,
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithTag("chat-export-dialog-submit").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("chat-export-dialog-error")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("chat-export-dialog-error").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-export-dialog").assertIsDisplayed()
        assertFalse(dismissed)
    }

    private data class CapturedExport(
        val conversationId: String,
        val format: String,
        val pathOnly: Boolean,
    )

    private class RecordingExportRepository(
        private val payload: ExportedChatPayload? = null,
        private val failure: Throwable? = null,
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
            failure?.let { return Result.failure(it) }
            return Result.success(payload!!)
        }
    }
}
