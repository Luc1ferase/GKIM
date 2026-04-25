package com.gkim.im.android.feature.tavern

/**
 * §6.1 — presentation contract for the "Reset relationship" affordance on the character-detail
 * surface.
 *
 * The affordance is two-step: the first tap arms the destructive-action confirmation
 * ([RelationshipResetPhase.Armed]); the second tap commits the request
 * ([RelationshipResetPhase.Submitting]) which calls
 * `POST /api/relationships/{characterId}/reset` and on success transitions to
 * [RelationshipResetPhase.Completed]. Cancel from Armed returns to Idle without making the
 * call. A failed call surfaces an inline error ([RelationshipResetPhase.Failed]) with a
 * retry that re-invokes the endpoint without re-arming the two-step gate (per the §9.2 spec
 * scenario "Failed reset surfaces an inline error with a retry affordance").
 *
 * Successful reset clears the user-companion pair's conversations, memory record, and
 * last-selected alt-greeting; the character card, the user's preset / persona / lorebook
 * library, and lorebook bindings are preserved (per the §9.1 spec scenario "Reset preserves
 * the character record and the user's library data"). The exact local-cache mutation lives
 * in the next-layer wire-up's repository / ViewModel; this helper describes the effect's
 * shape so the wire-up can mechanically apply it.
 */
internal enum class RelationshipResetPhase {
    Idle,
    Armed,
    Submitting,
    Completed,
    Failed,
}

internal data class RelationshipResetAffordanceState(
    val characterId: String,
    val phase: RelationshipResetPhase = RelationshipResetPhase.Idle,
    val errorCode: String? = null,
)

internal fun RelationshipResetAffordanceState.arm(): RelationshipResetAffordanceState =
    if (phase == RelationshipResetPhase.Idle) {
        copy(phase = RelationshipResetPhase.Armed, errorCode = null)
    } else {
        this
    }

internal fun RelationshipResetAffordanceState.cancel(): RelationshipResetAffordanceState =
    if (phase == RelationshipResetPhase.Armed) {
        copy(phase = RelationshipResetPhase.Idle, errorCode = null)
    } else {
        this
    }

internal fun RelationshipResetAffordanceState.confirm(): RelationshipResetAffordanceState =
    if (phase == RelationshipResetPhase.Armed) {
        copy(phase = RelationshipResetPhase.Submitting, errorCode = null)
    } else {
        this
    }

internal fun RelationshipResetAffordanceState.markCompleted(): RelationshipResetAffordanceState =
    copy(phase = RelationshipResetPhase.Completed, errorCode = null)

internal fun RelationshipResetAffordanceState.markFailed(code: String): RelationshipResetAffordanceState =
    copy(phase = RelationshipResetPhase.Failed, errorCode = code)

/**
 * Retry the call from [RelationshipResetPhase.Failed] without re-arming the two-step gate.
 * The §9.2 spec scenario commits to "tapping retry re-invokes the endpoint without
 * re-arming"; this helper enforces that — Idle / Armed / Submitting / Completed all stay
 * unchanged, only Failed advances to Submitting.
 */
internal fun RelationshipResetAffordanceState.retry(): RelationshipResetAffordanceState =
    if (phase == RelationshipResetPhase.Failed) {
        copy(phase = RelationshipResetPhase.Submitting, errorCode = null)
    } else {
        this
    }

internal fun RelationshipResetAffordanceState.canSubmit(): Boolean =
    phase == RelationshipResetPhase.Armed

internal fun RelationshipResetAffordanceState.callTargetCharacterId(): String? =
    if (phase == RelationshipResetPhase.Submitting) characterId else null

/**
 * Describes the local-cache mutation the §6.1 wire-up should apply on successful reset:
 * the user-companion pair's conversations, memory record, and last-selected alt-greeting
 * preference should all be cleared for the named character. The character card itself plus
 * the user's library and lorebook bindings stay untouched — they're not represented in this
 * effect, which is the positive shape of the §9.1 "preserve" Scenario.
 */
internal data class RelationshipResetEffect(
    val clearConversationsForCharacter: String,
    val clearMemoryForCharacter: String,
    val clearLastSelectedGreetingForCharacter: String,
)

internal fun RelationshipResetAffordanceState.applyResetEffect(): RelationshipResetEffect? {
    if (phase != RelationshipResetPhase.Completed) return null
    return RelationshipResetEffect(
        clearConversationsForCharacter = characterId,
        clearMemoryForCharacter = characterId,
        clearLastSelectedGreetingForCharacter = characterId,
    )
}
