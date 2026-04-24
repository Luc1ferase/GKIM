package com.gkim.im.android.feature.chat

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.MessageStatus
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.repository.DefaultCompanionTurnRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * §5.1 / §5.2 verification — mirrors the `llm-text-companion-chat` §5.2 precedent
 * ("full-route scenarios ride the unit suites... the instrumentation slice covers the
 * Compose-rendered helpers end-to-end").
 *
 * Drives the production [DefaultCompanionTurnRepository] through the real reducer
 * transitions while a [CompanionLifecycleTimelineHost] collects `activePathByConversation`
 * and renders each message through the production [ChatMessageRow]. The assertions prove
 * that the same pipeline wired by §3.1 / §3.2 in the `ChatViewModel` (companion marker →
 * `submitUserTurn` → state flow → `ChatUiState.companionMessages` → `ChatMessageRow`)
 * renders the optimistic user bubble plus every companion lifecycle state from a Thinking
 * start through Streaming deltas to a Completed or Failed terminal.
 *
 * Full-route navigation (tavern → detail → activate → chat) is already covered by
 * `CharacterDetailActivateCompanionConversationTest`; ViewModel dispatch and uiState
 * assembly are covered by `ChatViewModelCompanionDispatchTest` and
 * `ChatViewModelUiStateCompanionTest`.
 */
@RunWith(AndroidJUnit4::class)
class CompanionChatEndToEndInstrumentationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun scriptedReducerTransitionsRenderPendingThinkingStreamingCompletedThroughChatMessageRow() {
        val repo = DefaultCompanionTurnRepository()
        val conversationId = "conversation-e2e-completed"
        val userMessageId = "scripted-user-completed"
        val companionMessageId = "scripted-companion-completed"
        val turnId = "scripted-turn-completed"
        val variantGroupId = "scripted-variant-group-completed"
        val finalBody = "Scripted companion reply complete."

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                CompanionLifecycleTimelineHost(repo = repo, conversationId = conversationId)
            }
        }

        composeRule.runOnIdle {
            repo.recordUserTurn(
                ChatMessage(
                    id = userMessageId,
                    direction = MessageDirection.Outgoing,
                    kind = MessageKind.Text,
                    body = "hello",
                    createdAt = "2026-04-24T00:00:00Z",
                    status = MessageStatus.Pending,
                ),
                conversationId,
            )
        }
        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithTag("chat-message-body-$userMessageId")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("chat-message-body-$userMessageId").assertTextContains("hello")

        composeRule.runOnIdle {
            repo.handleTurnStarted(
                ImGatewayEvent.CompanionTurnStarted(
                    turnId = turnId,
                    conversationId = conversationId,
                    messageId = companionMessageId,
                    variantGroupId = variantGroupId,
                    variantIndex = 0,
                ),
            )
        }
        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithTag("chat-companion-status-$companionMessageId")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Thinking…").assertIsDisplayed()

        composeRule.runOnIdle {
            repo.handleTurnDelta(
                ImGatewayEvent.CompanionTurnDelta(
                    turnId = turnId,
                    conversationId = conversationId,
                    messageId = companionMessageId,
                    deltaSeq = 1,
                    textDelta = "Scripted companion reply ",
                ),
            )
        }
        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithText("Streaming…").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Streaming…").assertIsDisplayed()

        composeRule.runOnIdle {
            repo.handleTurnCompleted(
                ImGatewayEvent.CompanionTurnCompleted(
                    turnId = turnId,
                    conversationId = conversationId,
                    messageId = companionMessageId,
                    finalBody = finalBody,
                    completedAt = "2026-04-24T00:00:01Z",
                ),
            )
            repo.updateUserMessageStatus(
                conversationId = conversationId,
                messageId = userMessageId,
                status = MessageStatus.Completed,
            )
        }
        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithTag("chat-message-body-$companionMessageId")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("chat-message-body-$companionMessageId")
            .assertTextContains(finalBody)
    }
}

@Composable
private fun CompanionLifecycleTimelineHost(
    repo: DefaultCompanionTurnRepository,
    conversationId: String,
) {
    val path by repo.activePathByConversation.collectAsState()
    val messages = path[conversationId].orEmpty()
    LazyColumn {
        items(messages, key = { it.id }) { message ->
            ChatMessageRow(
                conversation = null,
                message = message,
                isMostRecentCompanionVariant = true,
            )
        }
    }
}
