package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §5.1 verification — the JSONL chat export dialog exposes three controls, each with a well-
 * defined initial value and a pure transition helper:
 *
 * 1. active-path-only vs. full-tree toggle (`pathOnly`, default true)
 * 2. target-language selector defaulted to the active [AppLanguage] (`language`, default
 *    activeLanguage.toChatExportWireLanguage())
 * 3. share-sheet vs. Downloads target selector (`target`, default ChatExportTarget.Share)
 *
 * Plus the in-flight / completed / failed state machine that the §5.2 wire-up will drive when
 * the export request is in flight.
 *
 * Tests exercise the pure helpers directly. The composable's button taps each delegate to one
 * of these helpers (e.g., share-tab tap → withTarget(Share); language radio → withLanguage("zh")),
 * so asserting the helpers' contracts implicitly covers the rendering path without standing up
 * Compose.
 */
class ChatExportDialogPresentationTest {

    // -------------------------------------------------------------------------
    // Initial state — defaults track each of the three control surfaces
    // -------------------------------------------------------------------------

    @Test
    fun `initialChatExportDialogState defaults pathOnly to true (active-path is the lighter export)`() {
        val state = initialChatExportDialogState(AppLanguage.English)
        assertTrue(state.pathOnly)
    }

    @Test
    fun `initialChatExportDialogState defaults target to Share`() {
        val state = initialChatExportDialogState(AppLanguage.English)
        assertEquals(ChatExportTarget.Share, state.target)
    }

    @Test
    fun `initialChatExportDialogState defaults language to active AppLanguage in wire form`() {
        val englishState = initialChatExportDialogState(AppLanguage.English)
        assertEquals("en", englishState.language)

        val chineseState = initialChatExportDialogState(AppLanguage.Chinese)
        assertEquals("zh", chineseState.language)
    }

    @Test
    fun `initialChatExportDialogState lifecycle flags start clean`() {
        val state = initialChatExportDialogState(AppLanguage.English)
        assertFalse(state.inFlight)
        assertFalse(state.completed)
        assertNull(state.errorCode)
    }

    // -------------------------------------------------------------------------
    // pathOnly toggle — the active-path-only vs. full-tree control
    // -------------------------------------------------------------------------

    @Test
    fun `withPathOnly false flips active-path-only to full-tree without touching other controls`() {
        val state = initialChatExportDialogState(AppLanguage.English).withPathOnly(false)
        assertFalse(state.pathOnly)
        assertEquals("en", state.language)
        assertEquals(ChatExportTarget.Share, state.target)
    }

    @Test
    fun `withPathOnly true restores active-path-only after a full-tree selection`() {
        val state = initialChatExportDialogState(AppLanguage.Chinese)
            .withPathOnly(false)
            .withPathOnly(true)
        assertTrue(state.pathOnly)
        assertEquals("zh", state.language)
    }

    // -------------------------------------------------------------------------
    // language selector — overrides the active-language default
    // -------------------------------------------------------------------------

    @Test
    fun `withLanguage overrides language but keeps other controls`() {
        val state = initialChatExportDialogState(AppLanguage.English).withLanguage("zh")
        assertEquals("zh", state.language)
        assertTrue(state.pathOnly)
        assertEquals(ChatExportTarget.Share, state.target)
    }

    @Test
    fun `withLanguage accepts arbitrary wire codes the selector might offer`() {
        val state = initialChatExportDialogState(AppLanguage.English).withLanguage("ja")
        assertEquals("ja", state.language)
    }

    // -------------------------------------------------------------------------
    // target selector — share-sheet vs. Downloads
    // -------------------------------------------------------------------------

    @Test
    fun `withTarget switches share to Downloads without touching other controls`() {
        val state = initialChatExportDialogState(AppLanguage.English)
            .withTarget(ChatExportTarget.Downloads)
        assertEquals(ChatExportTarget.Downloads, state.target)
        assertTrue(state.pathOnly)
        assertEquals("en", state.language)
    }

    @Test
    fun `withTarget switches Downloads back to share`() {
        val state = initialChatExportDialogState(AppLanguage.English)
            .withTarget(ChatExportTarget.Downloads)
            .withTarget(ChatExportTarget.Share)
        assertEquals(ChatExportTarget.Share, state.target)
    }

    // -------------------------------------------------------------------------
    // lifecycle state machine — inFlight / completed / failed
    // -------------------------------------------------------------------------

    @Test
    fun `markInFlight clears prior error and completed flags`() {
        val prior = initialChatExportDialogState(AppLanguage.English)
            .markFailed("export_failed")
        val running = prior.markInFlight()
        assertTrue(running.inFlight)
        assertFalse(running.completed)
        assertNull(running.errorCode)
    }

    @Test
    fun `markCompleted clears inFlight and flips completed on`() {
        val running = initialChatExportDialogState(AppLanguage.English).markInFlight()
        val done = running.markCompleted()
        assertFalse(done.inFlight)
        assertTrue(done.completed)
        assertNull(done.errorCode)
    }

    @Test
    fun `markFailed stores error code and clears inFlight and completed`() {
        val running = initialChatExportDialogState(AppLanguage.English).markInFlight()
        val failed = running.markFailed("server_busy")
        assertEquals("server_busy", failed.errorCode)
        assertFalse(failed.inFlight)
        assertFalse(failed.completed)
    }

    @Test
    fun `lifecycle transitions preserve user control selections`() {
        val state = initialChatExportDialogState(AppLanguage.Chinese)
            .withPathOnly(false)
            .withLanguage("en")
            .withTarget(ChatExportTarget.Downloads)
            .markInFlight()
        assertFalse(state.pathOnly)
        assertEquals("en", state.language)
        assertEquals(ChatExportTarget.Downloads, state.target)
        assertTrue(state.inFlight)
    }

    // -------------------------------------------------------------------------
    // wire language mapping — mirrors AppLanguage.toChatExportWireLanguage()
    // -------------------------------------------------------------------------

    @Test
    fun `AppLanguage maps to canonical wire language codes`() {
        assertEquals("en", AppLanguage.English.toChatExportWireLanguage())
        assertEquals("zh", AppLanguage.Chinese.toChatExportWireLanguage())
    }
}
