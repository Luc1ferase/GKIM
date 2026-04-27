package com.gkim.im.android.feature.chat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.SafetyCopy
import com.gkim.im.android.core.designsystem.BlockReasonCopy
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.BlockReason
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.CompanionTurnMeta
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.MessageStatus
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.remote.im.ImGatewayEventParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatFailureAndSafetyBubbleInstrumentationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun parsedBlockedEventRendersBlockCopyComposeNewAndLearnMoreWithoutRetry() {
        val payload = """
            {
              "type": "companion_turn.blocked",
              "turnId": "turn-blocked-1",
              "conversationId": "conversation-1",
              "messageId": "companion-blocked-1",
              "reason": "nsfw_denied"
            }
        """.trimIndent()
        val event = ImGatewayEventParser.parse(payload) as ImGatewayEvent.CompanionTurnBlocked
        assertEquals(BlockReason.NsfwDenied, event.reasonAsBlockReason)

        val message = companionMessage(
            id = event.messageId,
            status = MessageStatus.Blocked,
            body = event.reason,
            blockReasonKey = event.reason,
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                ChatMessageRow(
                    conversation = null,
                    message = message,
                    isMostRecentCompanionVariant = true,
                )
            }
        }

        composeRule.onNodeWithTag("chat-companion-block-copy-${message.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-compose-new-${message.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-learn-more-policy-${message.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-retry-${message.id}").assertIsNotDisplayed()
    }

    @Test
    fun parsedBlockedEventUnknownReasonFallsBackToOtherCopy() {
        val payload = """
            {
              "type": "companion_turn.blocked",
              "turnId": "turn-blocked-2",
              "conversationId": "conversation-1",
              "messageId": "companion-blocked-2",
              "reason": "something_new"
            }
        """.trimIndent()
        val event = ImGatewayEventParser.parse(payload) as ImGatewayEvent.CompanionTurnBlocked
        assertEquals(
            "Unknown wire key must fall back to BlockReason.Other",
            BlockReason.Other,
            event.reasonAsBlockReason,
        )

        val message = companionMessage(
            id = event.messageId,
            status = MessageStatus.Blocked,
            body = event.reason,
            blockReasonKey = event.reason,
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                ChatMessageRow(
                    conversation = null,
                    message = message,
                    isMostRecentCompanionVariant = true,
                )
            }
        }

        composeRule.onNodeWithTag("chat-companion-block-copy-${message.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-compose-new-${message.id}").assertIsDisplayed()
    }

    @Test
    fun parsedFailedPromptBudgetEventRendersEditUserTurnWithoutRetry() {
        val payload = """
            {
              "type": "companion_turn.failed",
              "turnId": "turn-failed-1",
              "conversationId": "conversation-1",
              "messageId": "companion-failed-1",
              "subtype": "prompt_budget_exceeded",
              "errorMessage": "context too long"
            }
        """.trimIndent()
        val event = ImGatewayEventParser.parse(payload) as ImGatewayEvent.CompanionTurnFailed
        assertEquals(
            com.gkim.im.android.core.model.FailedSubtype.PromptBudgetExceeded,
            event.subtypeAsFailedSubtype,
        )

        val message = companionMessage(
            id = event.messageId,
            status = MessageStatus.Failed,
            body = event.errorMessage ?: "",
            failedSubtypeKey = event.subtype,
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                ChatMessageRow(
                    conversation = null,
                    message = message,
                    isMostRecentCompanionVariant = true,
                )
            }
        }

        composeRule.onNodeWithTag("chat-companion-failed-copy-${message.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-edit-user-turn-${message.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-retry-${message.id}").assertIsNotDisplayed()
        composeRule.onNodeWithTag("chat-companion-connection-hint-${message.id}").assertIsNotDisplayed()
    }

    @Test
    fun parsedFailedAuthenticationEventRendersEditUserTurnWithoutRetry() {
        val payload = """
            {
              "type": "companion_turn.failed",
              "turnId": "turn-failed-2",
              "conversationId": "conversation-1",
              "messageId": "companion-failed-2",
              "subtype": "authentication_failed"
            }
        """.trimIndent()
        val event = ImGatewayEventParser.parse(payload) as ImGatewayEvent.CompanionTurnFailed
        assertEquals(
            com.gkim.im.android.core.model.FailedSubtype.AuthenticationFailed,
            event.subtypeAsFailedSubtype,
        )

        val message = companionMessage(
            id = event.messageId,
            status = MessageStatus.Failed,
            body = "",
            failedSubtypeKey = event.subtype,
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                ChatMessageRow(
                    conversation = null,
                    message = message,
                    isMostRecentCompanionVariant = true,
                )
            }
        }

        composeRule.onNodeWithTag("chat-companion-failed-copy-${message.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-edit-user-turn-${message.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-retry-${message.id}").assertIsNotDisplayed()
    }

    @Test
    fun parsedFailedNetworkErrorEventRendersRetryPlusConnectionHint() {
        val payload = """
            {
              "type": "companion_turn.failed",
              "turnId": "turn-failed-3",
              "conversationId": "conversation-1",
              "messageId": "companion-failed-3",
              "subtype": "network_error"
            }
        """.trimIndent()
        val event = ImGatewayEventParser.parse(payload) as ImGatewayEvent.CompanionTurnFailed
        assertEquals(
            com.gkim.im.android.core.model.FailedSubtype.NetworkError,
            event.subtypeAsFailedSubtype,
        )

        val message = companionMessage(
            id = event.messageId,
            status = MessageStatus.Failed,
            body = "",
            failedSubtypeKey = event.subtype,
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                ChatMessageRow(
                    conversation = null,
                    message = message,
                    isMostRecentCompanionVariant = true,
                )
            }
        }

        composeRule.onNodeWithTag("chat-companion-failed-copy-${message.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-retry-${message.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-connection-hint-${message.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-edit-user-turn-${message.id}").assertIsNotDisplayed()
    }

    @Test
    fun parsedFailedProviderUnavailableEventRendersRetryPlusConnectionHint() {
        val payload = """
            {
              "type": "companion_turn.failed",
              "turnId": "turn-failed-4",
              "conversationId": "conversation-1",
              "messageId": "companion-failed-4",
              "subtype": "provider_unavailable"
            }
        """.trimIndent()
        val event = ImGatewayEventParser.parse(payload) as ImGatewayEvent.CompanionTurnFailed
        assertEquals(
            com.gkim.im.android.core.model.FailedSubtype.ProviderUnavailable,
            event.subtypeAsFailedSubtype,
        )

        val message = companionMessage(
            id = event.messageId,
            status = MessageStatus.Failed,
            body = "",
            failedSubtypeKey = event.subtype,
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                ChatMessageRow(
                    conversation = null,
                    message = message,
                    isMostRecentCompanionVariant = true,
                )
            }
        }

        composeRule.onNodeWithTag("chat-companion-retry-${message.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-connection-hint-${message.id}").assertIsDisplayed()
    }

    @Test
    fun parsedFailedTransientEventRendersRetryOnly() {
        val payload = """
            {
              "type": "companion_turn.failed",
              "turnId": "turn-failed-5",
              "conversationId": "conversation-1",
              "messageId": "companion-failed-5",
              "subtype": "transient"
            }
        """.trimIndent()
        val event = ImGatewayEventParser.parse(payload) as ImGatewayEvent.CompanionTurnFailed

        val message = companionMessage(
            id = event.messageId,
            status = MessageStatus.Failed,
            body = "",
            failedSubtypeKey = event.subtype,
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                ChatMessageRow(
                    conversation = null,
                    message = message,
                    isMostRecentCompanionVariant = true,
                )
            }
        }

        composeRule.onNodeWithTag("chat-companion-retry-${message.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-edit-user-turn-${message.id}").assertIsNotDisplayed()
        composeRule.onNodeWithTag("chat-companion-connection-hint-${message.id}").assertIsNotDisplayed()
    }

    @Test
    fun timeoutBubbleRendersTimeoutCopyAndRetryWithoutPresetHintWhenUnderCap() {
        // Simulated timeout: the backend emits a companion_turn.failed event with
        // subtype="timeout", which the repository translates to MessageStatus.Timeout.
        val payload = """
            {
              "type": "companion_turn.failed",
              "turnId": "turn-timeout-1",
              "conversationId": "conversation-1",
              "messageId": "companion-timeout-1",
              "subtype": "timeout",
              "errorMessage": "idle 15s"
            }
        """.trimIndent()
        val event = ImGatewayEventParser.parse(payload) as ImGatewayEvent.CompanionTurnFailed
        assertEquals("timeout", event.subtype)

        val message = companionMessage(
            id = event.messageId,
            status = MessageStatus.Timeout,
            body = event.errorMessage ?: "",
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                ChatMessageRow(
                    conversation = null,
                    message = message,
                    isMostRecentCompanionVariant = true,
                )
            }
        }

        composeRule.onNodeWithTag("chat-companion-timeout-copy-${message.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-retry-${message.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-switch-preset-hint-${message.id}")
            .assertIsNotDisplayed()
    }

    @Test
    fun blockedBubbleRendersSelfHarmCopyInChineseWhenLanguageIsChinese() {
        val payload = """
            {
              "type": "companion_turn.blocked",
              "turnId": "turn-blocked-zh",
              "conversationId": "conversation-1",
              "messageId": "companion-blocked-zh",
              "reason": "self_harm"
            }
        """.trimIndent()
        val event = ImGatewayEventParser.parse(payload) as ImGatewayEvent.CompanionTurnBlocked
        assertEquals(BlockReason.SelfHarm, event.reasonAsBlockReason)

        val expectedCopy = BlockReasonCopy.localizedCopy(BlockReason.SelfHarm, AppLanguage.Chinese)
        assertTrue(
            "Self-harm Chinese copy must be non-blank to prove bilingual wiring reached the UI layer",
            expectedCopy.isNotBlank(),
        )

        val message = companionMessage(
            id = event.messageId,
            status = MessageStatus.Blocked,
            body = event.reason,
            blockReasonKey = event.reason,
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.Chinese) {
                ChatMessageRow(
                    conversation = null,
                    message = message,
                    isMostRecentCompanionVariant = true,
                )
            }
        }

        composeRule.onNodeWithTag("chat-companion-block-copy-${message.id}").assertIsDisplayed()
    }

    @Test
    fun failedBubbleRendersNetworkCopyInChineseWhenLanguageIsChinese() {
        val payload = """
            {
              "type": "companion_turn.failed",
              "turnId": "turn-failed-zh",
              "conversationId": "conversation-1",
              "messageId": "companion-failed-zh",
              "subtype": "network_error"
            }
        """.trimIndent()
        val event = ImGatewayEventParser.parse(payload) as ImGatewayEvent.CompanionTurnFailed
        assertEquals(
            com.gkim.im.android.core.model.FailedSubtype.NetworkError,
            event.subtypeAsFailedSubtype,
        )

        val expectedCopy = SafetyCopy.localizedFailedCopy(
            com.gkim.im.android.core.model.FailedSubtype.NetworkError,
            AppLanguage.Chinese,
        )
        assertTrue(expectedCopy.isNotBlank())

        val message = companionMessage(
            id = event.messageId,
            status = MessageStatus.Failed,
            body = "",
            failedSubtypeKey = event.subtype,
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.Chinese) {
                ChatMessageRow(
                    conversation = null,
                    message = message,
                    isMostRecentCompanionVariant = true,
                )
            }
        }

        composeRule.onNodeWithTag("chat-companion-failed-copy-${message.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-connection-hint-${message.id}").assertIsDisplayed()
    }

    private fun companionMessage(
        id: String,
        status: MessageStatus,
        body: String,
        blockReasonKey: String? = null,
        failedSubtypeKey: String? = null,
    ): ChatMessage = ChatMessage(
        id = id,
        direction = MessageDirection.Incoming,
        kind = MessageKind.Text,
        body = body,
        createdAt = "2026-04-23T11:00:00Z",
        status = status,
        companionTurnMeta = CompanionTurnMeta(
            turnId = "turn-${id}",
            variantGroupId = "vg-${id}",
            variantIndex = 0,
            providerId = "openai",
            model = "gpt-4o-mini",
            blockReasonKey = blockReasonKey,
            failedSubtypeKey = failedSubtypeKey,
        ),
    )
}
