package com.gkim.im.android.data.remote.im

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the Kotlin wire DTO `RelationshipResetResponseDto` against the canonical
 * `contract/fixtures/relationships/relationship-reset-response.json` payload paired with
 * `GKIM-Backend`'s `tavern-experience-polish-reset-backend` slice.
 *
 * The request fixture is `{}` — no body parameter; the matching Retrofit method
 * (`resetRelationship`) sends a POST with no body, matching the existing
 * `deleteCompanionCharacter` no-body POST pattern. The response carries `{ "ok": true }`
 * on every success (including idempotent repeat resets), which the
 * `RelationshipResetResponseDto.ok: Boolean` field decodes.
 */
class RelationshipsContractFixturesTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // -------------------------------------------------------------------------
    // relationship-reset-request.json — empty body sentinel
    // -------------------------------------------------------------------------

    @Test
    fun `relationship-reset-request fixture decodes as an empty JSON object`() {
        // The request body has no fields — backends accept either an empty body or `{}`. The
        // fixture canonicalises `{}` so the wire reader sees a well-formed JSON value either
        // way. The Kotlin client sends a no-body POST (matching the deleteCompanionCharacter
        // pattern), and the backend's request DTO accepts either Content-Length: 0 or `{}`.
        val parsed = json.decodeFromString<JsonObject>(REQUEST_FIXTURE)
        assertTrue(parsed.isEmpty())
    }

    // -------------------------------------------------------------------------
    // relationship-reset-response.json — { "ok": true }
    // -------------------------------------------------------------------------

    @Test
    fun `relationship-reset-response fixture decodes into RelationshipResetResponseDto`() {
        val response = json.decodeFromString<RelationshipResetResponseDto>(RESPONSE_FIXTURE)
        assertEquals(true, response.ok)
    }

    @Test
    fun `RelationshipResetResponseDto round-trip preserves the ok flag`() {
        val source = RelationshipResetResponseDto(ok = true)
        val encoded = json.encodeToString(RelationshipResetResponseDto.serializer(), source)
        val decoded = json.decodeFromString<RelationshipResetResponseDto>(encoded)
        assertEquals(source, decoded)
    }

    @Test
    fun `RelationshipResetResponseDto with ok=false decodes for completeness`() {
        // The §5 spec scenario "Reset is idempotent" says the endpoint always returns success
        // on idempotent repeat — but the wire DTO is general enough to carry an `ok=false`
        // outcome if a future server ever decided to surface a partial-failure code that way.
        // Defending the wire shape's flexibility is cheap.
        val source = RelationshipResetResponseDto(ok = false)
        val encoded = json.encodeToString(RelationshipResetResponseDto.serializer(), source)
        val decoded = json.decodeFromString<RelationshipResetResponseDto>(encoded)
        assertEquals(false, decoded.ok)
    }

    private companion object {
        // Verbatim copy of `contract/fixtures/relationships/relationship-reset-request.json`.
        const val REQUEST_FIXTURE: String = "{}"

        // Verbatim copy of `contract/fixtures/relationships/relationship-reset-response.json`.
        const val RESPONSE_FIXTURE: String = """
{
  "ok": true
}
"""
    }
}
