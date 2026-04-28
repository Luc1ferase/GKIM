package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.MessageStatus
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.remote.im.ImGatewayEventParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §2.3 verification — end-to-end assertions exercising the full
 * `parser → handler → ChatMessage` flow for each of the three new typed
 * terminal events. Each test parses a stub gateway-event payload via
 * `ImGatewayEventParser`, dispatches the resulting variant through the
 * matching `DefaultCompanionTurnRepository.handleTurnXxx(event)`, and
 * asserts the produced `ChatMessage` carries the expected `MessageStatus`
 * plus the variant-specific projection field
 * (`failedSubtypeKey` / `blockReasonKey` / `timeoutElapsedMs`).
 */
class CompanionTurnRepositoryRecoveryEventTest {

    private val conversationId = "conversation-recovery-smoke"

    private fun userMessage(id: String): ChatMessage = ChatMessage(
        id = id,
        direction = MessageDirection.Outgoing,
        kind = MessageKind.Text,
        body = "Hello",
        createdAt = "2026-04-27T11:59:59Z",
    )

    private fun seedStartedTurn(repo: DefaultCompanionTurnRepository, turnId: String, messageId: String) {
        repo.recordUserTurn(userMessage("user-1"), conversationId)
        repo.handleTurnStarted(
            ImGatewayEvent.CompanionTurnStarted(
                turnId = turnId,
                conversationId = conversationId,
                messageId = messageId,
                variantGroupId = "vg-recovery-smoke",
                variantIndex = 0,
                parentMessageId = "user-1",
            )
        )
    }

    @Test
    fun `parsed event-failed promotes the active companion message to MessageStatus Failed with subtype`() {
        val repo = DefaultCompanionTurnRepository()
        seedStartedTurn(repo, "turn-failed-recovery-smoke-01", "companion-turn-failed-recovery-smoke-01")

        val payload = """
            {
              "type": "companion_turn.failed",
              "turnId": "turn-failed-recovery-smoke-01",
              "conversationId": "conversation-recovery-smoke",
              "messageId": "companion-turn-failed-recovery-smoke-01",
              "subtype": "provider_unavailable",
              "errorMessage": "upstream provider returned 503",
              "completedAt": "2026-04-27T12:00:01.500Z"
            }
        """.trimIndent()
        val event = ImGatewayEventParser.parse(payload)
        repo.handleTurnFailed(event as ImGatewayEvent.CompanionTurnFailed)

        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        val terminal = path.last()
        assertEquals(MessageStatus.Failed, terminal.status)
        assertEquals("provider_unavailable", terminal.companionTurnMeta?.failedSubtypeKey)
        assertEquals("upstream provider returned 503", terminal.body)
        assertTrue(terminal.companionTurnMeta?.canRegenerate == true)
    }

    @Test
    fun `parsed event-blocked promotes the active companion message to MessageStatus Blocked with reason`() {
        val repo = DefaultCompanionTurnRepository()
        seedStartedTurn(repo, "turn-blocked-recovery-smoke-01", "companion-turn-blocked-recovery-smoke-01")

        val payload = """
            {
              "type": "companion_turn.blocked",
              "turnId": "turn-blocked-recovery-smoke-01",
              "conversationId": "conversation-recovery-smoke",
              "messageId": "companion-turn-blocked-recovery-smoke-01",
              "reason": "self_harm",
              "completedAt": "2026-04-27T12:00:01.500Z"
            }
        """.trimIndent()
        val event = ImGatewayEventParser.parse(payload)
        repo.handleTurnBlocked(event as ImGatewayEvent.CompanionTurnBlocked)

        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        val terminal = path.last()
        assertEquals(MessageStatus.Blocked, terminal.status)
        assertEquals("self_harm", terminal.companionTurnMeta?.blockReasonKey)
        assertEquals("self_harm", terminal.body)
        assertTrue(terminal.companionTurnMeta?.canRegenerate == false)
    }

    @Test
    fun `parsed event-timeout promotes the active companion message to MessageStatus Timeout with elapsedMs`() {
        val repo = DefaultCompanionTurnRepository()
        seedStartedTurn(repo, "turn-timeout-recovery-smoke-01", "companion-turn-timeout-recovery-smoke-01")

        val payload = """
            {
              "type": "companion_turn.timeout",
              "turnId": "turn-timeout-recovery-smoke-01",
              "conversationId": "conversation-recovery-smoke",
              "messageId": "companion-turn-timeout-recovery-smoke-01",
              "elapsedMs": 30000,
              "completedAt": "2026-04-27T12:00:30.000Z"
            }
        """.trimIndent()
        val event = ImGatewayEventParser.parse(payload)
        repo.handleTurnTimeout(event as ImGatewayEvent.CompanionTurnTimeout)

        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        val terminal = path.last()
        assertEquals(MessageStatus.Timeout, terminal.status)
        assertEquals(30_000L, terminal.companionTurnMeta?.timeoutElapsedMs)
        // Timeout retains the in-flight body (no error string overrides it) and
        // canRegenerate stays true so the bubble can offer Retry.
        assertTrue(terminal.companionTurnMeta?.canRegenerate == true)
    }

    @Test
    fun `parsed event-failed with retryAfterMs is persisted as retryAfterEpochMs against fixed clock`() {
        // Pinned wall-clock so the absolute deadline arithmetic is deterministic.
        // Reference: 2026-04-28 12:00:00 UTC = 1_777_377_600_000 ms.
        val frozenClockMs = 1_777_377_600_000L
        val repo = DefaultCompanionTurnRepository(clockMillis = { frozenClockMs })
        seedStartedTurn(repo, "turn-rate-limited-smoke-01", "companion-turn-rate-limited-smoke-01")

        val payload = """
            {
              "type": "companion_turn.failed",
              "turnId": "turn-rate-limited-smoke-01",
              "conversationId": "conversation-recovery-smoke",
              "messageId": "companion-turn-rate-limited-smoke-01",
              "subtype": "transient",
              "errorMessage": "upstream returned HTTP 429",
              "retryAfterMs": 12000,
              "completedAt": "2026-04-28T12:00:00.000Z"
            }
        """.trimIndent()
        val event = ImGatewayEventParser.parse(payload)
        repo.handleTurnFailed(event as ImGatewayEvent.CompanionTurnFailed)

        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        val terminal = path.last()
        assertEquals(MessageStatus.Failed, terminal.status)
        assertEquals("transient", terminal.companionTurnMeta?.failedSubtypeKey)
        // Absolute deadline = clock + retryAfterMs (= 1_777_377_612_000 ms).
        assertEquals(frozenClockMs + 12_000L, terminal.companionTurnMeta?.retryAfterEpochMs)
    }

    @Test
    fun `parsed event-failed without retryAfterMs leaves retryAfterEpochMs null`() {
        val repo = DefaultCompanionTurnRepository(clockMillis = { 1_777_377_600_000L })
        seedStartedTurn(repo, "turn-failed-no-retry-after-smoke-01", "companion-turn-failed-no-retry-after-smoke-01")

        val payload = """
            {
              "type": "companion_turn.failed",
              "turnId": "turn-failed-no-retry-after-smoke-01",
              "conversationId": "conversation-recovery-smoke",
              "messageId": "companion-turn-failed-no-retry-after-smoke-01",
              "subtype": "provider_unavailable",
              "errorMessage": "upstream returned HTTP 503"
            }
        """.trimIndent()
        val event = ImGatewayEventParser.parse(payload)
        repo.handleTurnFailed(event as ImGatewayEvent.CompanionTurnFailed)

        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        val terminal = path.last()
        assertEquals(MessageStatus.Failed, terminal.status)
        assertEquals("provider_unavailable", terminal.companionTurnMeta?.failedSubtypeKey)
        assertEquals(null, terminal.companionTurnMeta?.retryAfterEpochMs)
    }
}
