package com.gkim.im.android.data.remote.im

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Locks the Kotlin wire DTOs against the canonical
 * `contract/fixtures/companion-turns/{edit-request,edit-response,regenerate-at-request}.json`
 * payloads paired with `GKIM-Backend`. Every fixture string in this file is a verbatim copy of
 * the JSON in the repo's `contract/fixtures/companion-turns/` directory; if either side drifts,
 * one of these round-trip assertions will turn red and the
 * `git diff --no-index GKIM/contract/fixtures GKIM-Backend/contract/fixtures` canonical check
 * will catch it.
 *
 * The three new fixtures cover the tavern-experience-polish §3.2 (edit-user-turn) and §3.3
 * (regenerate-at) backend slice's wire shape. Pre-existing fixtures (`submit-*`,
 * `regenerate-request`, `event-*`) are covered by `ImBackendPayloadsTest` and the WS event
 * parser tests under `LiveCompanionTurnRepositoryTest`.
 */
class CompanionTurnsContractFixturesTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // -------------------------------------------------------------------------
    // edit-request.json — POST /api/companion-turns/{conversationId}/edit body
    // -------------------------------------------------------------------------

    @Test
    fun `edit-request json deserializes into EditUserTurnRequestDto`() {
        val fixture = """
            {
              "parentMessageId": "msg-original-user-7",
              "newUserText": "rewritten greeting from user",
              "clientTurnId": "client-turn-edit-smoke-01",
              "activeCompanionId": "daylight-listener",
              "activeLanguage": "en"
            }
        """.trimIndent()

        val request = json.decodeFromString<EditUserTurnRequestDto>(fixture)

        assertEquals("msg-original-user-7", request.parentMessageId)
        assertEquals("rewritten greeting from user", request.newUserText)
        assertEquals("client-turn-edit-smoke-01", request.clientTurnId)
        assertEquals("daylight-listener", request.activeCompanionId)
        assertEquals("en", request.activeLanguage)
    }

    @Test
    fun `edit-request round-trip produces semantically equivalent JSON`() {
        val source = EditUserTurnRequestDto(
            parentMessageId = "msg-original-user-7",
            newUserText = "rewritten greeting from user",
            clientTurnId = "client-turn-edit-smoke-01",
            activeCompanionId = "daylight-listener",
            activeLanguage = "en",
        )
        val encoded = json.encodeToString(EditUserTurnRequestDto.serializer(), source)
        val decoded = json.decodeFromString<EditUserTurnRequestDto>(encoded)
        assertEquals(source, decoded)
    }

    // -------------------------------------------------------------------------
    // edit-response.json — 200 response carrying { userMessage, companionTurn }
    // -------------------------------------------------------------------------

    @Test
    fun `edit-response json deserializes into EditUserTurnResponseDto`() {
        val fixture = """
            {
              "userMessage": {
                "messageId": "user-message-edit-smoke-01",
                "variantGroupId": "variant-group-edit-user-smoke-01",
                "variantIndex": 0,
                "parentMessageId": "msg-original-user-7",
                "role": "user"
              },
              "companionTurn": {
                "turnId": "turn-edit-companion-smoke-01",
                "conversationId": "conversation-daylight-listener-smoke",
                "messageId": "turn-edit-companion-smoke-01",
                "variantGroupId": "variant-group-edit-companion-smoke-01",
                "variantIndex": 0,
                "parentMessageId": "user-message-edit-smoke-01",
                "status": "thinking",
                "accumulatedBody": "",
                "lastDeltaSeq": 0,
                "providerId": null,
                "model": null,
                "startedAt": "2026-04-25T12:00:00Z",
                "completedAt": null,
                "failureSubtype": null,
                "errorMessage": null
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<EditUserTurnResponseDto>(fixture)

        // userMessage — the new user-side variant
        assertEquals("user-message-edit-smoke-01", response.userMessage.messageId)
        assertEquals("variant-group-edit-user-smoke-01", response.userMessage.variantGroupId)
        assertEquals(0, response.userMessage.variantIndex)
        assertEquals("msg-original-user-7", response.userMessage.parentMessageId)
        assertEquals("user", response.userMessage.role)

        // companionTurn — the new companion-side variant under the user-message parent
        val turn = response.companionTurn
        assertEquals("turn-edit-companion-smoke-01", turn.turnId)
        assertEquals("conversation-daylight-listener-smoke", turn.conversationId)
        assertEquals("turn-edit-companion-smoke-01", turn.messageId)
        assertEquals("variant-group-edit-companion-smoke-01", turn.variantGroupId)
        assertEquals(0, turn.variantIndex)
        assertEquals("user-message-edit-smoke-01", turn.parentMessageId)
        assertEquals("thinking", turn.status)
        assertEquals("", turn.accumulatedBody)
        assertEquals(0, turn.lastDeltaSeq)
        assertNull(turn.providerId)
        assertNull(turn.model)
        assertEquals("2026-04-25T12:00:00Z", turn.startedAt)
        assertNull(turn.completedAt)
        assertNull(turn.failureSubtype)
        assertNull(turn.errorMessage)
    }

    @Test
    fun `edit-response NewUserMessageRecord with absent parentMessageId decodes as null`() {
        // Defends the optional-parent variant (root user message under a fresh conversation)
        // so the field stays nullable even though the canonical fixture is non-null.
        val fixture = """
            {
              "messageId": "root-user-message-x",
              "variantGroupId": "root-variant-group-x",
              "variantIndex": 0,
              "role": "user"
            }
        """.trimIndent()

        val record = json.decodeFromString<NewUserMessageRecordDto>(fixture)

        assertEquals("root-user-message-x", record.messageId)
        assertEquals("root-variant-group-x", record.variantGroupId)
        assertEquals(0, record.variantIndex)
        assertNull(record.parentMessageId)
        assertEquals("user", record.role)
    }

    // -------------------------------------------------------------------------
    // regenerate-at-request.json — POST /api/companion-turns/{conversationId}/regenerate-at body
    // -------------------------------------------------------------------------

    @Test
    fun `regenerate-at-request with explicit targetMessageId deserializes into RegenerateAtRequestDto`() {
        val fixture = """
            {
              "clientTurnId": "client-turn-regenerate-at-smoke-01",
              "targetMessageId": "turn-edit-companion-smoke-01"
            }
        """.trimIndent()

        val request = json.decodeFromString<RegenerateAtRequestDto>(fixture)

        assertEquals("client-turn-regenerate-at-smoke-01", request.clientTurnId)
        assertEquals("turn-edit-companion-smoke-01", request.targetMessageId)
    }

    @Test
    fun `regenerate-at-request without targetMessageId preserves the legacy regenerate-latest semantic`() {
        // The §3.3 contract says: when targetMessageId is absent, behave as legacy regenerate
        // (most-recent companion turn). Defends the optional-field deserialization so a
        // request body that omits the key still parses cleanly into the DTO with null target.
        val fixture = """
            {
              "clientTurnId": "client-turn-regenerate-at-omitted-target"
            }
        """.trimIndent()

        val request = json.decodeFromString<RegenerateAtRequestDto>(fixture)

        assertEquals("client-turn-regenerate-at-omitted-target", request.clientTurnId)
        assertNull(request.targetMessageId)
    }

    @Test
    fun `regenerate-at-request round-trip preserves both fields`() {
        val withTarget = RegenerateAtRequestDto(
            clientTurnId = "client-turn-regenerate-at-smoke-01",
            targetMessageId = "turn-edit-companion-smoke-01",
        )
        val withoutTarget = RegenerateAtRequestDto(
            clientTurnId = "client-turn-regenerate-at-omitted-target",
            targetMessageId = null,
        )
        assertEquals(
            withTarget,
            json.decodeFromString<RegenerateAtRequestDto>(
                json.encodeToString(RegenerateAtRequestDto.serializer(), withTarget),
            ),
        )
        assertEquals(
            withoutTarget,
            json.decodeFromString<RegenerateAtRequestDto>(
                json.encodeToString(RegenerateAtRequestDto.serializer(), withoutTarget),
            ),
        )
    }
}
