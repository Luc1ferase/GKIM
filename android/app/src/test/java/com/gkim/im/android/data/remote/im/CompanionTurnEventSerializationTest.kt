package com.gkim.im.android.data.remote.im

import com.gkim.im.android.core.model.BlockReason
import com.gkim.im.android.core.model.FailedSubtype
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * §2.2 verification — the three new wire-event fixtures
 * (`event-failed.json`, `event-blocked.json`, `event-timeout.json` under
 * `contract/fixtures/companion-turns/`, mirrored byte-equivalent from the
 * paired backend slice's `contract/fixtures/companion-turns/`) round-trip
 * through `ImGatewayEventParser` into the matching `ImGatewayEvent`
 * variants without field drift.
 *
 * Each fixture string here is a verbatim copy of the JSON file under
 * `contract/fixtures/companion-turns/`; if either side drifts, one of
 * these round-trip assertions will turn red and the
 * `git diff --no-index GKIM/contract/fixtures GKIM-Backend/contract/fixtures`
 * canonical check will catch it.
 */
class CompanionTurnEventSerializationTest {

    @Test
    fun `event-failed json parses into CompanionTurnFailed with provider_unavailable subtype`() {
        val fixture = """
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

        val event = ImGatewayEventParser.parse(fixture)

        assertEquals(ImGatewayEvent.CompanionTurnFailed::class.java, event.javaClass)
        val failed = event as ImGatewayEvent.CompanionTurnFailed
        assertEquals("turn-failed-recovery-smoke-01", failed.turnId)
        assertEquals("conversation-recovery-smoke", failed.conversationId)
        assertEquals("companion-turn-failed-recovery-smoke-01", failed.messageId)
        assertEquals("provider_unavailable", failed.subtype)
        assertEquals("upstream provider returned 503", failed.errorMessage)
        assertEquals("2026-04-27T12:00:01.500Z", failed.completedAt)
        // The subtype MUST round-trip through FailedSubtype.fromWireKey without
        // falling through to Unknown for the six taxonomy values.
        assertEquals(FailedSubtype.ProviderUnavailable, failed.subtypeAsFailedSubtype)
    }

    @Test
    fun `event-failed FailedSubtype taxonomy round-trips for all six wire keys`() {
        // The six wire-keys in the FailedSubtype taxonomy must NOT fall through
        // to Unknown — only an unrecognized future value lands on Unknown.
        listOf(
            "transient" to FailedSubtype.Transient,
            "prompt_budget_exceeded" to FailedSubtype.PromptBudgetExceeded,
            "authentication_failed" to FailedSubtype.AuthenticationFailed,
            "provider_unavailable" to FailedSubtype.ProviderUnavailable,
            "network_error" to FailedSubtype.NetworkError,
            "unknown" to FailedSubtype.Unknown,
        ).forEach { (wireKey, expected) ->
            assertEquals(
                "FailedSubtype.fromWireKey($wireKey) MUST resolve to $expected, not Unknown fallback",
                expected,
                FailedSubtype.fromWireKey(wireKey),
            )
        }
        // Only an unrecognized future value lands on Unknown.
        assertEquals(FailedSubtype.Unknown, FailedSubtype.fromWireKey("future_unknown_subtype"))
    }

    @Test
    fun `event-blocked json parses into CompanionTurnBlocked with self_harm reason`() {
        val fixture = """
            {
              "type": "companion_turn.blocked",
              "turnId": "turn-blocked-recovery-smoke-01",
              "conversationId": "conversation-recovery-smoke",
              "messageId": "companion-turn-blocked-recovery-smoke-01",
              "reason": "self_harm",
              "completedAt": "2026-04-27T12:00:01.500Z"
            }
        """.trimIndent()

        val event = ImGatewayEventParser.parse(fixture)

        assertEquals(ImGatewayEvent.CompanionTurnBlocked::class.java, event.javaClass)
        val blocked = event as ImGatewayEvent.CompanionTurnBlocked
        assertEquals("turn-blocked-recovery-smoke-01", blocked.turnId)
        assertEquals("conversation-recovery-smoke", blocked.conversationId)
        assertEquals("companion-turn-blocked-recovery-smoke-01", blocked.messageId)
        assertEquals("self_harm", blocked.reason)
        assertEquals("2026-04-27T12:00:01.500Z", blocked.completedAt)
        assertEquals(BlockReason.SelfHarm, blocked.reasonAsBlockReason)
    }

    @Test
    fun `event-timeout json parses into CompanionTurnTimeout with elapsedMs`() {
        val fixture = """
            {
              "type": "companion_turn.timeout",
              "turnId": "turn-timeout-recovery-smoke-01",
              "conversationId": "conversation-recovery-smoke",
              "messageId": "companion-turn-timeout-recovery-smoke-01",
              "elapsedMs": 30000,
              "completedAt": "2026-04-27T12:00:30.000Z"
            }
        """.trimIndent()

        val event = ImGatewayEventParser.parse(fixture)

        assertEquals(ImGatewayEvent.CompanionTurnTimeout::class.java, event.javaClass)
        val timeout = event as ImGatewayEvent.CompanionTurnTimeout
        assertEquals("turn-timeout-recovery-smoke-01", timeout.turnId)
        assertEquals("conversation-recovery-smoke", timeout.conversationId)
        assertEquals("companion-turn-timeout-recovery-smoke-01", timeout.messageId)
        assertEquals(30_000L, timeout.elapsedMs)
        assertEquals("2026-04-27T12:00:30.000Z", timeout.completedAt)
    }

    @Test
    fun `parser dispatch routes companion_turn dotted discriminators to typed terminals`() {
        // Lightweight smoke: each new type discriminator is recognized by the
        // dispatch table and produces a non-null event of the expected variant.
        val failed = ImGatewayEventParser.parse(
            """{"type":"companion_turn.failed","turnId":"t","conversationId":"c","messageId":"m","subtype":"transient"}"""
        )
        val blocked = ImGatewayEventParser.parse(
            """{"type":"companion_turn.blocked","turnId":"t","conversationId":"c","messageId":"m","reason":"other"}"""
        )
        val timeout = ImGatewayEventParser.parse(
            """{"type":"companion_turn.timeout","turnId":"t","conversationId":"c","messageId":"m","elapsedMs":1000}"""
        )
        assertNotNull(failed as? ImGatewayEvent.CompanionTurnFailed)
        assertNotNull(blocked as? ImGatewayEvent.CompanionTurnBlocked)
        assertNotNull(timeout as? ImGatewayEvent.CompanionTurnTimeout)
    }
}
