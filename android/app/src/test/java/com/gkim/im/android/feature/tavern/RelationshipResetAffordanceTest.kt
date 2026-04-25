package com.gkim.im.android.feature.tavern

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §6.1 verification — the "Reset relationship" affordance is two-step; calling it commits
 * `POST /api/relationships/{characterId}/reset`; on success the user-companion pair's
 * conversations, memory record, and last-selected alt-greeting are cleared while the
 * character card and the user's library are preserved.
 *
 * Tests drive the pure helpers ([RelationshipResetAffordanceState] + transition functions
 * [arm], [cancel], [confirm], [retry], [markCompleted], [markFailed], plus
 * [canSubmit], [callTargetCharacterId], [applyResetEffect]) that back the affordance's
 * Compose UI. The composable's first / second / cancel / retry click handlers each call
 * one of these transitions; the §6.2 instrumentation will lock the actual UI rendering.
 */
class RelationshipResetAffordanceTest {

    private fun idleState(characterId: String = "daylight-listener") =
        RelationshipResetAffordanceState(characterId = characterId)

    // -------------------------------------------------------------------------
    // Initial state — fresh affordance is Idle, no error
    // -------------------------------------------------------------------------

    @Test
    fun `initial state phase is Idle with no errorCode`() {
        val state = idleState()
        assertEquals(RelationshipResetPhase.Idle, state.phase)
        assertNull(state.errorCode)
        assertFalse(state.canSubmit())
        assertNull(state.callTargetCharacterId())
        assertNull(state.applyResetEffect())
    }

    // -------------------------------------------------------------------------
    // Two-step gate — arm + confirm vs cancel
    // -------------------------------------------------------------------------

    @Test
    fun `arm advances Idle to Armed`() {
        val state = idleState().arm()
        assertEquals(RelationshipResetPhase.Armed, state.phase)
        assertTrue(state.canSubmit())
    }

    @Test
    fun `arm is a no-op when already past Idle`() {
        val armedTwice = idleState().arm().arm()
        assertEquals(RelationshipResetPhase.Armed, armedTwice.phase)

        val submitting = idleState().arm().confirm()
        val noopArmed = submitting.arm()
        assertEquals(RelationshipResetPhase.Submitting, noopArmed.phase)
    }

    @Test
    fun `cancel from Armed returns to Idle without committing`() {
        val state = idleState().arm().cancel()
        assertEquals(RelationshipResetPhase.Idle, state.phase)
        assertFalse(state.canSubmit())
    }

    @Test
    fun `cancel from Idle is a no-op`() {
        val state = idleState().cancel()
        assertEquals(RelationshipResetPhase.Idle, state.phase)
    }

    @Test
    fun `confirm from Armed advances to Submitting`() {
        val state = idleState().arm().confirm()
        assertEquals(RelationshipResetPhase.Submitting, state.phase)
        assertNull(state.errorCode)
    }

    @Test
    fun `confirm from Idle does not skip the two-step gate`() {
        val state = idleState().confirm()
        // Still Idle — confirming without arming is a no-op, not a shortcut commit. The two-
        // step gate is the destructive-action protection the §6.1 spec requires.
        assertEquals(RelationshipResetPhase.Idle, state.phase)
    }

    @Test
    fun `canSubmit is true only when Armed`() {
        val idle = idleState()
        val armed = idle.arm()
        val submitting = armed.confirm()
        val completed = submitting.markCompleted()
        val failed = submitting.markFailed("server_busy")

        assertFalse(idle.canSubmit())
        assertTrue(armed.canSubmit())
        assertFalse(submitting.canSubmit())
        assertFalse(completed.canSubmit())
        assertFalse(failed.canSubmit())
    }

    // -------------------------------------------------------------------------
    // Endpoint call shape — characterId carried into the wire request
    // -------------------------------------------------------------------------

    @Test
    fun `callTargetCharacterId carries the characterId only while Submitting`() {
        val idle = idleState(characterId = "architect-oracle")
        assertNull(idle.callTargetCharacterId())

        val armed = idle.arm()
        assertNull(armed.callTargetCharacterId())

        val submitting = armed.confirm()
        assertEquals("architect-oracle", submitting.callTargetCharacterId())

        val completed = submitting.markCompleted()
        assertNull(completed.callTargetCharacterId())
    }

    // -------------------------------------------------------------------------
    // Failure + retry — re-invoke without re-arming (§9.2 spec scenario)
    // -------------------------------------------------------------------------

    @Test
    fun `markFailed records the errorCode for the inline-error UI`() {
        val state = idleState().arm().confirm().markFailed("network_error")
        assertEquals(RelationshipResetPhase.Failed, state.phase)
        assertEquals("network_error", state.errorCode)
    }

    @Test
    fun `retry from Failed advances to Submitting without re-arming`() {
        val failed = idleState().arm().confirm().markFailed("network_error")
        val retried = failed.retry()
        assertEquals(RelationshipResetPhase.Submitting, retried.phase)
        assertNull(retried.errorCode)
    }

    @Test
    fun `retry from Idle Armed Submitting Completed is a no-op (only Failed retries)`() {
        listOf(
            idleState(),
            idleState().arm(),
            idleState().arm().confirm(),
            idleState().arm().confirm().markCompleted(),
        ).forEach { stateBeforeRetry ->
            val afterRetry = stateBeforeRetry.retry()
            assertEquals(stateBeforeRetry.phase, afterRetry.phase)
        }
    }

    // -------------------------------------------------------------------------
    // Post-reset effect — clear conversations + memory + last-selected greeting
    // -------------------------------------------------------------------------

    @Test
    fun `applyResetEffect returns null until the request completes`() {
        val notDoneStates = listOf(
            idleState(),
            idleState().arm(),
            idleState().arm().confirm(),
            idleState().arm().confirm().markFailed("network_error"),
        )
        notDoneStates.forEach { state ->
            assertNull(state.applyResetEffect())
        }
    }

    @Test
    fun `applyResetEffect on Completed clears conversations memory and last-greeting for the character`() {
        val effect = idleState(characterId = "daylight-listener")
            .arm()
            .confirm()
            .markCompleted()
            .applyResetEffect()
        assertEquals(
            RelationshipResetEffect(
                clearConversationsForCharacter = "daylight-listener",
                clearMemoryForCharacter = "daylight-listener",
                clearLastSelectedGreetingForCharacter = "daylight-listener",
            ),
            effect,
        )
    }

    @Test
    fun `applyResetEffect names only the targeted characterId (preserves others by omission)`() {
        // The effect carries the characterId for each clearable cache; the §9.1 spec's
        // "preserve" Scenario (character record / user library / lorebook bindings stay
        // untouched) is encoded by what the effect does NOT name — preset / persona /
        // lorebook caches don't appear in the effect, so the wire-up has nothing to clear
        // in those caches.
        val effect = idleState(characterId = "architect-oracle")
            .arm()
            .confirm()
            .markCompleted()
            .applyResetEffect()!!
        assertEquals("architect-oracle", effect.clearConversationsForCharacter)
        assertEquals("architect-oracle", effect.clearMemoryForCharacter)
        assertEquals("architect-oracle", effect.clearLastSelectedGreetingForCharacter)
    }

    // -------------------------------------------------------------------------
    // End-to-end happy paths
    // -------------------------------------------------------------------------

    @Test
    fun `end-to-end happy path — Idle to Armed to Submitting to Completed clears the pair state`() {
        val final = idleState(characterId = "daylight-listener")
            .arm()
            .confirm()
            .markCompleted()
        assertEquals(RelationshipResetPhase.Completed, final.phase)
        assertNotNull(final.applyResetEffect())
        assertEquals("daylight-listener", final.applyResetEffect()!!.clearConversationsForCharacter)
    }

    @Test
    fun `end-to-end retry path — failure surfaces error then retry succeeds`() {
        val firstAttempt = idleState(characterId = "daylight-listener")
            .arm()
            .confirm()
            .markFailed("network_error")
        assertEquals("network_error", firstAttempt.errorCode)

        val secondAttempt = firstAttempt.retry().markCompleted()
        assertEquals(RelationshipResetPhase.Completed, secondAttempt.phase)
        assertNull(secondAttempt.errorCode)
        // The retry path does NOT pass through Armed again — the two-step gate was already
        // cleared on the first attempt and the failure preserves that.
        val effect = secondAttempt.applyResetEffect()!!
        assertEquals("daylight-listener", effect.clearConversationsForCharacter)
    }

    private fun assertNotNull(value: Any?) {
        if (value == null) throw AssertionError("Expected non-null value, got null")
    }
}
