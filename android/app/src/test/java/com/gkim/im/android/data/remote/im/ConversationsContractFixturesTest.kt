package com.gkim.im.android.data.remote.im

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Locks the Kotlin wire DTO `ConversationExportLineDto` against the canonical
 * `contract/fixtures/conversations/export-{active-path,full-tree}-jsonl-response.txt`
 * payloads paired with `GKIM-Backend`'s `tavern-experience-polish-export-backend` slice.
 * Each fixture is a JSON Lines payload: one JSON object per line, parsed independently.
 *
 * The active-path-only export carries the conversation's currently-active variant chain
 * (5 lines for the §1.1 backend smoke fixture); the full-tree export carries every
 * message including non-active siblings (6 lines — adds the `turn-export-companion-smoke-01a`
 * sibling that the active path drops in favour of the regenerated `01b`).
 *
 * Per the §9.1 spec scenario "Each JSONL line carries the documented field shape", every
 * line MUST expose the eight named keys (`messageId`, `parentMessageId`, `variantGroupId`,
 * `variantIndex`, `role`, `timestamp`, `content`, `extensions`). Absent optional fields
 * MUST decode as `null` rather than missing the key — the kotlinx.serialization
 * `ignoreUnknownKeys = true` mode preserves that contract because the wire keys are always
 * present even when null-valued.
 */
class ConversationsContractFixturesTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // -------------------------------------------------------------------------
    // export-active-path-jsonl-response.txt — 5 ordered path messages
    // -------------------------------------------------------------------------

    @Test
    fun `active-path JSONL fixture decodes into 5 ConversationExportLineDto entries`() {
        val lines = ACTIVE_PATH_FIXTURE.trim().split("\n")
        assertEquals(5, lines.size)

        val records = lines.map { json.decodeFromString<ConversationExportLineDto>(it) }
        assertEquals("user-message-export-smoke-01", records[0].messageId)
        assertEquals(null, records[0].parentMessageId)
        assertEquals("user", records[0].role)
        assertEquals("variant-group-export-user-01", records[0].variantGroupId)
        assertEquals(0, records[0].variantIndex)

        // The second line is the regenerated companion sibling (variantIndex=1) — the active
        // path picks it because the §3.3 regenerate-at promoted it to the active variant.
        assertEquals("turn-export-companion-smoke-01b", records[1].messageId)
        assertEquals("user-message-export-smoke-01", records[1].parentMessageId)
        assertEquals("variant-group-export-companion-01", records[1].variantGroupId)
        assertEquals(1, records[1].variantIndex)
        assertEquals("assistant", records[1].role)

        assertEquals("user-message-export-smoke-02", records[2].messageId)
        assertEquals("turn-export-companion-smoke-01b", records[2].parentMessageId)

        assertEquals("turn-export-companion-smoke-02", records[3].messageId)
        assertEquals(0, records[3].variantIndex)

        assertEquals("user-message-export-smoke-03", records[4].messageId)
        assertEquals("turn-export-companion-smoke-02", records[4].parentMessageId)
    }

    @Test
    fun `active-path lines all expose the documented 8 keys with null for absent extensions`() {
        val lines = ACTIVE_PATH_FIXTURE.trim().split("\n")
        for (line in lines) {
            val record = json.decodeFromString<ConversationExportLineDto>(line)
            assertNotNull(record.messageId)
            assertNotNull(record.variantGroupId)
            assertNotNull(record.role)
            assertNotNull(record.timestamp)
            assertNotNull(record.content)
            // §9.1 scenario: absent optional fields render as null (not omitted). The fixture
            // sets extensions to JSON null on every line; the DTO surfaces it as Kotlin null.
            assertNull(record.extensions)
        }
    }

    // -------------------------------------------------------------------------
    // export-full-tree-jsonl-response.txt — 6 messages incl non-active sibling
    // -------------------------------------------------------------------------

    @Test
    fun `full-tree JSONL fixture decodes into 6 entries with both companion siblings present`() {
        val lines = FULL_TREE_FIXTURE.trim().split("\n")
        assertEquals(6, lines.size)

        val records = lines.map { json.decodeFromString<ConversationExportLineDto>(it) }
        // Both `01a` (variantIndex=0) and `01b` (variantIndex=1) appear under the same
        // variantGroupId — the full-tree export emits non-active siblings.
        val companion01Group = records.filter { it.variantGroupId == "variant-group-export-companion-01" }
        assertEquals(2, companion01Group.size)
        assertEquals(setOf(0, 1), companion01Group.map { it.variantIndex }.toSet())
        assertEquals(
            setOf("turn-export-companion-smoke-01a", "turn-export-companion-smoke-01b"),
            companion01Group.map { it.messageId }.toSet(),
        )
    }

    @Test
    fun `full-tree fixture is a strict superset of active-path fixture (every active-path line appears verbatim)`() {
        val activeLines = ACTIVE_PATH_FIXTURE.trim().split("\n").toSet()
        val fullLines = FULL_TREE_FIXTURE.trim().split("\n").toSet()
        // Every active-path line is byte-equal to a line in the full-tree payload.
        assertEquals(emptyList<String>(), (activeLines - fullLines).toList())
        // The full-tree carries exactly one extra line beyond the active path: the inactive
        // sibling `01a` (the §3.3 regenerate-at's "prior sibling preserved" semantic).
        assertEquals(1, (fullLines - activeLines).size)
    }

    // -------------------------------------------------------------------------
    // Round-trip via the Kotlin DTO
    // -------------------------------------------------------------------------

    @Test
    fun `ConversationExportLineDto round-trip preserves nullable parentMessageId and extensions`() {
        val source = ConversationExportLineDto(
            messageId = "user-message-export-smoke-01",
            parentMessageId = null,
            variantGroupId = "variant-group-export-user-01",
            variantIndex = 0,
            role = "user",
            timestamp = "2026-04-25T12:00:00Z",
            content = "hello daylight listener",
            extensions = null,
        )
        val encoded = json.encodeToString(ConversationExportLineDto.serializer(), source)
        val decoded = json.decodeFromString<ConversationExportLineDto>(encoded)
        assertEquals(source, decoded)
    }

    @Test
    fun `ConversationExportLineDto with non-null parentMessageId encodes the parent edge`() {
        val source = ConversationExportLineDto(
            messageId = "turn-export-companion-smoke-01b",
            parentMessageId = "user-message-export-smoke-01",
            variantGroupId = "variant-group-export-companion-01",
            variantIndex = 1,
            role = "assistant",
            timestamp = "2026-04-25T12:00:02Z",
            content = "Hello traveler — I am the Daylight Listener (regenerated).",
            extensions = null,
        )
        val encoded = json.encodeToString(ConversationExportLineDto.serializer(), source)
        val decoded = json.decodeFromString<ConversationExportLineDto>(encoded)
        assertEquals(source, decoded)
        assertEquals("user-message-export-smoke-01", decoded.parentMessageId)
    }

    private companion object {
        // Verbatim copy of `contract/fixtures/conversations/export-active-path-jsonl-response.txt`.
        // If this string drifts from the file, the cross-repo
        // `git diff --no-index GKIM/contract GKIM-Backend/contract` canonical check breaks.
        const val ACTIVE_PATH_FIXTURE: String = """
{"messageId":"user-message-export-smoke-01","parentMessageId":null,"variantGroupId":"variant-group-export-user-01","variantIndex":0,"role":"user","timestamp":"2026-04-25T12:00:00Z","content":"hello daylight listener","extensions":null}
{"messageId":"turn-export-companion-smoke-01b","parentMessageId":"user-message-export-smoke-01","variantGroupId":"variant-group-export-companion-01","variantIndex":1,"role":"assistant","timestamp":"2026-04-25T12:00:02Z","content":"Hello traveler — I am the Daylight Listener (regenerated).","extensions":null}
{"messageId":"user-message-export-smoke-02","parentMessageId":"turn-export-companion-smoke-01b","variantGroupId":"variant-group-export-user-02","variantIndex":0,"role":"user","timestamp":"2026-04-25T12:00:03Z","content":"tell me a story","extensions":null}
{"messageId":"turn-export-companion-smoke-02","parentMessageId":"user-message-export-smoke-02","variantGroupId":"variant-group-export-companion-02","variantIndex":0,"role":"assistant","timestamp":"2026-04-25T12:00:04Z","content":"Once upon a time, a traveler met the Daylight Listener.","extensions":null}
{"messageId":"user-message-export-smoke-03","parentMessageId":"turn-export-companion-smoke-02","variantGroupId":"variant-group-export-user-03","variantIndex":0,"role":"user","timestamp":"2026-04-25T12:00:05Z","content":"go on","extensions":null}
"""

        // Verbatim copy of `contract/fixtures/conversations/export-full-tree-jsonl-response.txt`.
        const val FULL_TREE_FIXTURE: String = """
{"messageId":"user-message-export-smoke-01","parentMessageId":null,"variantGroupId":"variant-group-export-user-01","variantIndex":0,"role":"user","timestamp":"2026-04-25T12:00:00Z","content":"hello daylight listener","extensions":null}
{"messageId":"turn-export-companion-smoke-01a","parentMessageId":"user-message-export-smoke-01","variantGroupId":"variant-group-export-companion-01","variantIndex":0,"role":"assistant","timestamp":"2026-04-25T12:00:01Z","content":"Hello, traveler.","extensions":null}
{"messageId":"turn-export-companion-smoke-01b","parentMessageId":"user-message-export-smoke-01","variantGroupId":"variant-group-export-companion-01","variantIndex":1,"role":"assistant","timestamp":"2026-04-25T12:00:02Z","content":"Hello traveler — I am the Daylight Listener (regenerated).","extensions":null}
{"messageId":"user-message-export-smoke-02","parentMessageId":"turn-export-companion-smoke-01b","variantGroupId":"variant-group-export-user-02","variantIndex":0,"role":"user","timestamp":"2026-04-25T12:00:03Z","content":"tell me a story","extensions":null}
{"messageId":"turn-export-companion-smoke-02","parentMessageId":"user-message-export-smoke-02","variantGroupId":"variant-group-export-companion-02","variantIndex":0,"role":"assistant","timestamp":"2026-04-25T12:00:04Z","content":"Once upon a time, a traveler met the Daylight Listener.","extensions":null}
{"messageId":"user-message-export-smoke-03","parentMessageId":"turn-export-companion-smoke-02","variantGroupId":"variant-group-export-user-03","variantIndex":0,"role":"user","timestamp":"2026-04-25T12:00:05Z","content":"go on","extensions":null}
"""
    }
}
