package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §5.2 verification — the §5.1 dialog state turns into:
 *
 * - a wire request to `GET /api/conversations/{conversationId}/export?format=jsonl&pathOnly=...`
 * - a default filename including the `_<first8OfConversationId>` disambiguation suffix
 * - a dispatch target (share sheet vs. Downloads) derived from the user's selector
 *
 * Tests drive the pure helpers ([toExportRequestParams], [chatExportFilename],
 * [dispatchTargetFor]) that back the §5.2 wire-up. The dispatcher is a fun-interface for
 * platform delivery (Android share intent / DownloadManager); this slice locks the
 * deterministic input → params / filename / target mapping that the dispatcher consumes.
 */
class ChatExportRoutingTest {

    // -------------------------------------------------------------------------
    // Wire request — toExportRequestParams threads the dialog state into query params
    // -------------------------------------------------------------------------

    @Test
    fun `toExportRequestParams uses jsonl format and the pathOnly flag from state`() {
        val state = initialChatExportDialogState(AppLanguage.English)  // pathOnly default true
        val params = state.toExportRequestParams(conversationId = "conv-1")
        assertEquals("conv-1", params.conversationId)
        assertEquals("jsonl", params.format)
        assertEquals(true, params.pathOnly)
    }

    @Test
    fun `toExportRequestParams forwards pathOnly false for full-tree exports`() {
        val state = initialChatExportDialogState(AppLanguage.English).withPathOnly(false)
        val params = state.toExportRequestParams(conversationId = "conv-2")
        assertEquals(false, params.pathOnly)
    }

    @Test
    fun `toExportRequestParams takes the conversationId from the caller, not from state`() {
        // The dialog state is conversation-agnostic by design — the same dialog can be
        // re-used for any conversation; the caller threads the active conversationId at
        // dispatch time.
        val state = initialChatExportDialogState(AppLanguage.English)
        val paramsA = state.toExportRequestParams(conversationId = "conv-a")
        val paramsB = state.toExportRequestParams(conversationId = "conv-b")
        assertEquals("conv-a", paramsA.conversationId)
        assertEquals("conv-b", paramsB.conversationId)
    }

    // -------------------------------------------------------------------------
    // Filename — `_<first8OfConversationId>` disambiguation suffix
    // -------------------------------------------------------------------------

    @Test
    fun `chatExportFilename includes the first 8 chars of conversationId as the disambiguator`() {
        val filename = chatExportFilename(
            conversationId = "conversation-daylight-listener-smoke",
            pathOnly = true,
        )
        assertEquals("chat-export-active-path_conversa.jsonl", filename)
    }

    @Test
    fun `chatExportFilename truncates a UUID-like conversationId to its first 8 characters`() {
        val filename = chatExportFilename(
            conversationId = "cdab29eb-f6d7-4204-8fcf-4877bd037f16",
            pathOnly = false,
        )
        assertEquals("chat-export-full-tree_cdab29eb.jsonl", filename)
    }

    @Test
    fun `chatExportFilename uses the whole id when conversationId is shorter than 8 chars`() {
        val filename = chatExportFilename(conversationId = "conv-1", pathOnly = true)
        assertEquals("chat-export-active-path_conv-1.jsonl", filename)
    }

    @Test
    fun `chatExportFilename labels active-path-only exports with active-path`() {
        val filename = chatExportFilename(conversationId = "any-id", pathOnly = true)
        assertTrue(filename.contains("active-path"))
        assertEquals(false, filename.contains("full-tree"))
    }

    @Test
    fun `chatExportFilename labels full-tree exports with full-tree`() {
        val filename = chatExportFilename(conversationId = "any-id", pathOnly = false)
        assertTrue(filename.contains("full-tree"))
        assertEquals(false, filename.contains("active-path"))
    }

    @Test
    fun `chatExportFilename ends with the jsonl extension`() {
        listOf(true, false).forEach { pathOnly ->
            val filename = chatExportFilename(conversationId = "any-id", pathOnly = pathOnly)
            assertTrue(filename.endsWith(".jsonl"))
        }
    }

    // -------------------------------------------------------------------------
    // Dispatch target — Share vs Downloads
    // -------------------------------------------------------------------------

    @Test
    fun `dispatchTargetFor returns ChatExportDispatchTarget Share when state target is Share`() {
        val state = initialChatExportDialogState(AppLanguage.English) // target default Share
        val target = dispatchTargetFor(state, conversationId = "conv-x")
        assertTrue(target is ChatExportDispatchTarget.Share)
        assertEquals(
            ChatExportDispatchTarget.MIME_TYPE_JSONL,
            (target as ChatExportDispatchTarget.Share).mimeType,
        )
    }

    @Test
    fun `dispatchTargetFor returns Downloads with the canonical filename as displayName`() {
        val state = initialChatExportDialogState(AppLanguage.English)
            .withTarget(ChatExportTarget.Downloads)
        val target = dispatchTargetFor(state, conversationId = "conversation-daylight-listener")
        assertTrue(target is ChatExportDispatchTarget.Downloads)
        val downloadsTarget = target as ChatExportDispatchTarget.Downloads
        assertEquals("chat-export-active-path_conversa.jsonl", downloadsTarget.displayName)
        assertEquals(ChatExportDispatchTarget.MIME_TYPE_JSONL, downloadsTarget.mimeType)
    }

    @Test
    fun `dispatchTargetFor reflects the pathOnly flag in the Downloads filename`() {
        val activePathState = initialChatExportDialogState(AppLanguage.English)
            .withTarget(ChatExportTarget.Downloads)
        val activeTarget = dispatchTargetFor(activePathState, conversationId = "abcdefghij")
        assertEquals(
            "chat-export-active-path_abcdefgh.jsonl",
            (activeTarget as ChatExportDispatchTarget.Downloads).displayName,
        )

        val fullTreeState = activePathState.withPathOnly(false)
        val fullTarget = dispatchTargetFor(fullTreeState, conversationId = "abcdefghij")
        assertEquals(
            "chat-export-full-tree_abcdefgh.jsonl",
            (fullTarget as ChatExportDispatchTarget.Downloads).displayName,
        )
    }

    // -------------------------------------------------------------------------
    // End-to-end happy path — state → params + filename + target consistency
    // -------------------------------------------------------------------------

    @Test
    fun `end-to-end happy path — Downloads target shares filename across params and dispatcher`() {
        val state = initialChatExportDialogState(AppLanguage.English)
            .withPathOnly(false)
            .withTarget(ChatExportTarget.Downloads)
        val conversationId = "cdab29eb-f6d7-4204-8fcf-4877bd037f16"

        val params = state.toExportRequestParams(conversationId)
        val standaloneFilename = chatExportFilename(conversationId, state.pathOnly)
        val target = dispatchTargetFor(state, conversationId) as ChatExportDispatchTarget.Downloads

        assertEquals("jsonl", params.format)
        assertEquals(false, params.pathOnly)
        assertEquals("chat-export-full-tree_cdab29eb.jsonl", standaloneFilename)
        // The Downloads target's displayName matches the standalone filename byte-for-byte —
        // a single source of truth for the file's identity, no drift risk between the
        // dispatcher's display-name and the file the user might browse to in the share-sheet
        // alternative path.
        assertEquals(standaloneFilename, target.displayName)
    }
}
