package com.gkim.im.android.data.remote.im

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks `CompanionTurnSubmitRequestDto` (and its new `CharacterPromptContextDto`
 * payload, `companion-turn-character-prompt-context` §2.3) against the paired
 * fixtures under `contract/fixtures/companion-turns/`:
 *
 *  - `submit-request-with-character-context.json`
 *  - `submit-request-without-character-context.json`
 *
 * Each fixture string in this file is a verbatim copy of the on-disk JSON; if
 * either side drifts, one of these round-trip assertions will turn red and the
 * `git diff --no-index GKIM/contract/fixtures GKIM-Backend/contract/fixtures`
 * canonical check will catch the cross-repo drift.
 *
 * Mirrors `CompanionTurnsContractFixturesTest`'s tone: assert the parsed shape,
 * then round-trip back through the encoder to catch field-name regressions.
 */
class CompanionTurnSubmitRequestDtoSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    // -------------------------------------------------------------------------
    // submit-request-with-character-context.json — full payload
    // -------------------------------------------------------------------------

    @Test
    fun `submit-request-with-character-context json deserializes with all six fields populated`() {
        val fixture = """
            {
              "conversationId": "conversation-daylight-listener-smoke",
              "activeCompanionId": "daylight-listener",
              "userTurnBody": "hello",
              "activeLanguage": "en",
              "clientTurnId": "client-turn-submit-smoke-with-context-01",
              "parentMessageId": null,
              "characterPromptContext": {
                "systemPrompt": "You are {{char}}, a calm listener in a quiet tavern. Speak plainly to {{user}}.",
                "personality": "Measured, warm under the surface, allergic to clichés.",
                "scenario": "A wood-paneled tavern near closing time. One lamp, two chairs, a notebook between you.",
                "exampleDialogue": "{{user}}: I don't know where to start.\n{{char}}: Then let's not start everywhere at once.",
                "userPersonaName": "Aria",
                "companionDisplayName": "Daylight Listener"
              }
            }
        """.trimIndent()

        val request = json.decodeFromString<CompanionTurnSubmitRequestDto>(fixture)

        assertEquals("conversation-daylight-listener-smoke", request.conversationId)
        assertEquals("daylight-listener", request.activeCompanionId)
        assertEquals("hello", request.userTurnBody)
        assertEquals("en", request.activeLanguage)
        assertEquals("client-turn-submit-smoke-with-context-01", request.clientTurnId)
        assertNull(request.parentMessageId)

        val ctx = request.characterPromptContext
            ?: throw AssertionError("characterPromptContext must not be null in the field-present fixture")

        assertEquals(
            "You are {{char}}, a calm listener in a quiet tavern. Speak plainly to {{user}}.",
            ctx.systemPrompt,
        )
        assertEquals("Measured, warm under the surface, allergic to clichés.", ctx.personality)
        assertEquals(
            "A wood-paneled tavern near closing time. One lamp, two chairs, a notebook between you.",
            ctx.scenario,
        )
        assertEquals(
            "{{user}}: I don't know where to start.\n{{char}}: Then let's not start everywhere at once.",
            ctx.exampleDialogue,
        )
        assertEquals("Aria", ctx.userPersonaName)
        assertEquals("Daylight Listener", ctx.companionDisplayName)
    }

    @Test
    fun `submit-request-with-character-context survives encode-decode round-trip`() {
        val source = CompanionTurnSubmitRequestDto(
            conversationId = "conversation-daylight-listener-smoke",
            activeCompanionId = "daylight-listener",
            userTurnBody = "hello",
            activeLanguage = "en",
            clientTurnId = "client-turn-submit-smoke-with-context-01",
            parentMessageId = null,
            characterPromptContext = CharacterPromptContextDto(
                systemPrompt = "You are {{char}}, a calm listener in a quiet tavern. Speak plainly to {{user}}.",
                personality = "Measured, warm under the surface, allergic to clichés.",
                scenario = "A wood-paneled tavern near closing time. One lamp, two chairs, a notebook between you.",
                exampleDialogue = "{{user}}: I don't know where to start.\n{{char}}: Then let's not start everywhere at once.",
                userPersonaName = "Aria",
                companionDisplayName = "Daylight Listener",
            ),
        )

        val encoded = json.encodeToString(CompanionTurnSubmitRequestDto.serializer(), source)
        val decoded = json.decodeFromString<CompanionTurnSubmitRequestDto>(encoded)

        assertEquals(source, decoded)
    }

    // -------------------------------------------------------------------------
    // submit-request-without-character-context.json — backwards-compat shape
    // -------------------------------------------------------------------------

    @Test
    fun `submit-request-without-character-context json deserializes with characterPromptContext null`() {
        val fixture = """
            {
              "conversationId": "conversation-daylight-listener-smoke",
              "activeCompanionId": "daylight-listener",
              "userTurnBody": "hello",
              "activeLanguage": "en",
              "clientTurnId": "client-turn-submit-smoke-without-context-01",
              "parentMessageId": null
            }
        """.trimIndent()

        val request = json.decodeFromString<CompanionTurnSubmitRequestDto>(fixture)

        assertEquals("conversation-daylight-listener-smoke", request.conversationId)
        assertEquals("daylight-listener", request.activeCompanionId)
        assertEquals("hello", request.userTurnBody)
        assertEquals("en", request.activeLanguage)
        assertEquals("client-turn-submit-smoke-without-context-01", request.clientTurnId)
        assertNull(request.parentMessageId)
        assertNull(request.characterPromptContext)
    }

    @Test
    fun `submit-request-without-character-context omits the field on encode with encodeDefaults disabled`() {
        val source = CompanionTurnSubmitRequestDto(
            conversationId = "conversation-daylight-listener-smoke",
            activeCompanionId = "daylight-listener",
            userTurnBody = "hello",
            activeLanguage = "en",
            clientTurnId = "client-turn-submit-smoke-without-context-01",
            parentMessageId = null,
        )

        val encoded = json.encodeToString(CompanionTurnSubmitRequestDto.serializer(), source)

        assertFalse(
            "field-absent payload must not carry characterPromptContext when default-encoding is disabled",
            encoded.contains("characterPromptContext"),
        )
        // parentMessageId default is also null — it should be elided too, proving the encoder
        // applies the same default-elision to the new field rather than treating it specially.
        assertFalse(
            "default-null parentMessageId must be elided too, confirming consistent default-elision",
            encoded.contains("parentMessageId"),
        )
        assertTrue("payload must still carry the conversationId key", encoded.contains("\"conversationId\""))
    }
}
