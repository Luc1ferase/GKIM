# Design: wire-companion-turn-runtime

## §1.1 Contract reconnaissance (recon note)

Reviewed `data/repository/CompanionTurnRepository.kt` (335 lines) and `data/repository/LiveCompanionTurnRepository.kt` (298 lines) on branch `feature/ai-companion-im` at HEAD=5420e65.

### Surface already present

**`CompanionTurnRepository` (interface)** — exposes state-reducer surface only:

| Member | Purpose |
|---|---|
| `val treeByConversation: StateFlow<Map<String, ConversationTurnTree>>` | Raw per-conversation tree state (orderedMessageIds, messagesById, variantGroups, turnToMessageId, messageIdToVariantGroupId, lastDeltaSeqByTurn). |
| `val activePathByConversation: StateFlow<Map<String, List<ChatMessage>>>` | Per-conversation active-path list (one `ChatMessage` per variant group, following `activeIndex`). **This is the flow `ChatViewModel.uiState` should consume for companion conversations.** |
| `fun recordUserTurn(userMessage, conversationId)` | Append optimistic outgoing user bubble. |
| `fun updateUserMessageStatus(conversationId, messageId, status)` | Flip user bubble status (Pending → Completed / Failed). |
| `fun handleTurnStarted / handleTurnDelta / handleTurnCompleted / handleTurnFailed / handleTurnBlocked` | Gateway-event handlers (called by Live from `RealtimeGateway.events`). Default also uses these directly for unit tests. |
| `fun selectVariant(turnId, variantIndex)` | Variant navigation for tavern-experience-polish §3 (future). |
| `fun applyRecord(record: CompanionTurnRecordDto)` / `applySnapshot` | Rehydrate from backend snapshot / pending-turn list. |

**`LiveCompanionTurnRepository` (concrete, wraps `DefaultCompanionTurnRepository`)** — adds the networking surface that is *not* on the interface:

| Member | Signature | Purpose |
|---|---|---|
| `val failedSubmissions: StateFlow<Map<String, FailedSubmission>>` | (public) | Drives Retry affordance on user bubbles whose submit failed. |
| `suspend fun submitUserTurn(conversationId, activeCompanionId, userTurnBody, activeLanguage, parentMessageId): Result<CompanionTurnRecordDto>` | (public) | Optimistic `recordUserTurn` → HTTP `submitCompanionTurn` → on success, flip bubble status to Completed + `applyRecord`; on failure, flip to Failed + record a `FailedSubmission`. |
| `suspend fun retrySubmitUserTurn(userMessageId): Result<CompanionTurnRecordDto>` | (public) | Re-submit using the stored `FailedSubmission`; flip back to Pending/Completed/Failed accordingly. |
| `suspend fun regenerateTurn(turnId): Result<CompanionTurnRecordDto>` | (public) | HTTP `regenerateCompanionTurn` → `applyRecord` (adds sibling under same `variantGroupId`). |

### Critical gap — blocks §3

`AppContainer` exposes `companionTurnRepository: CompanionTurnRepository` typed as the *interface*, which does **not** carry `submitUserTurn` / `retrySubmitUserTurn` / `regenerateTurn` / `failedSubmissions`. Any feature-layer consumer (including `ChatViewModel`) reaching through the container cannot invoke those four members without downcasting to `LiveCompanionTurnRepository`.

This is the root cause of the runtime gap: `llm-text-companion-chat` §3.4 said "call `CompanionTurnRepository.submitUserTurn(...)`", but the interface does not have that method and the ViewModel has no way to call it through the container.

### Minimal contract delta required in §3

`§3.1` (ViewModel wiring) MUST promote the four Live-only members onto the interface. Proposed shape:

```kotlin
interface CompanionTurnRepository {
    // ... existing state fields + reducer methods (unchanged)

    // Submission surface — added by wire-companion-turn-runtime
    val failedSubmissions: StateFlow<Map<String, FailedSubmission>>
    suspend fun submitUserTurn(
        conversationId: String,
        activeCompanionId: String,
        userTurnBody: String,
        activeLanguage: String,
        parentMessageId: String? = null,
    ): Result<CompanionTurnRecordDto>
    suspend fun retrySubmitUserTurn(userMessageId: String): Result<CompanionTurnRecordDto>
    suspend fun regenerateTurn(turnId: String): Result<CompanionTurnRecordDto>

    data class FailedSubmission(
        val userMessageId: String,
        val conversationId: String,
        val activeCompanionId: String,
        val userTurnBody: String,
        val activeLanguage: String,
        val parentMessageId: String?,
    )
}
```

Notes on impact:
- `FailedSubmission` currently lives as a nested class on `LiveCompanionTurnRepository`. It moves to the interface (or a top-level data class in the same file). A type alias or re-export can preserve source-compat for any existing test that references `LiveCompanionTurnRepository.FailedSubmission` — confirm during §3.1 whether such call sites exist.
- `DefaultCompanionTurnRepository` is currently used (a) as the delegate inside `Live`, and (b) as a standalone unit-test reducer (`CompanionTurnRepositoryTest`, `LiveCompanionTurnRepositoryTest`). Standalone unit-test use does not exercise the submit family, so the four new methods on `DefaultCompanionTurnRepository` can throw `NotImplementedError("submit path requires Live wiring")` with zero test impact. Verify by running `CompanionTurnRepositoryTest` after the promotion.
- `failedSubmissions` flow stays empty on `DefaultCompanionTurnRepository` (`MutableStateFlow(emptyMap())`) so ViewModel-level tests that use Default-only fakes do not crash.

## §2 Conversation companion marker

`core/model/ChatModels.kt:64` `Conversation` has no companion marker. `Contact` (extension site) also has no kind. Both activation sites (`feature/tavern/CharacterDetailRoute.kt:87` and `feature/tavern/TavernRoute.kt:130`) call `messagingRepository.ensureConversation(card.asCompanionContact(appLanguage))` with no signal that the conversation is companion-kind.

Proposed delta (in §2.1):
- Add `companionCardId: String? = null` to `Conversation` (default null ⇒ source-compat for all peer-IM sites).
- Extend `MessagingRepository.ensureConversation(contact: Contact, companionCardId: String? = null)` with an optional second argument. Legacy call sites (`ContactsRoute`, peer chat bootstraps) keep passing no second argument; the two tavern sites pass `card.id`.
- Both `InMemoryMessagingRepository.ensureConversation` and `LiveMessagingRepository.ensureConversation` propagate the value onto the returned `Conversation`.

This keeps the marker additive and avoids touching the `Contact` type.

## §3 ViewModel wiring — downstream implications

With the §1.1 contract delta in place, `ChatViewModel.sendMessage(body)` becomes:

```kotlin
fun sendMessage(body: String, attachmentInput: MediaInput? = null) {
    if (body.isBlank() && attachmentInput == null) return
    val conversation = uiState.value.conversation
    val companionCardId = conversation?.companionCardId
    if (companionCardId != null) {
        viewModelScope.launch {
            companionTurnRepository.submitUserTurn(
                conversationId = resolvedConversationId,
                activeCompanionId = companionCardId,
                userTurnBody = body,
                activeLanguage = activeLanguage.wireKey,   // to be resolved from AppLanguage
                parentMessageId = null,  // first-turn; tavern-experience-polish §3 extends this
            )
        }
    } else {
        messagingRepository.sendMessage(
            conversationId = resolvedConversationId,
            body = body,
            attachment = attachmentInput?.asMessageAttachment(),
        )
    }
    generationActionFeedback.value = null
}
```

`ChatUiState` gains a field `companionMessages: List<ChatMessage>?` fed from `companionTurnRepository.activePathByConversation.map { it[resolvedConversationId] }` when the conversation carries a `companionCardId`. `ChatRoute` renders `companionMessages` when non-null (via existing `ChatMessageRow`), else falls back to `conversation.messages`.

## §4 Activate handler

Both activation sites update identically:

```kotlin
onActivate = {
    container.companionRosterRepository.activateCharacter(card.id)
    val conversation = container.messagingRepository.ensureConversation(
        card.asCompanionContact(appLanguage),
        companionCardId = card.id,
    )
    navController.navigate("chat/${conversation.id}")
}
```

Both `CharacterDetailRoute.kt:85-89` and `TavernRoute.kt:~130` need this change. Unit test target: both handlers.

## §5 Instrumentation fake

The new `CompanionChatEndToEndInstrumentationTest` needs a `FakeCompanionTurnRepository` that:
- Implements the full interface (including the promoted submit surface from §1.1 delta).
- Scripts `submitUserTurn` to immediately emit scripted events via the reducer helpers (`handleTurnStarted` → `handleTurnDelta` → `handleTurnCompleted`).
- Returns `Result.success(...)` after scripting.
- For the Failed scenario, skips the Completed step and emits `handleTurnFailed` instead.

The fake can be defined inline in the test file and hoisted into a test-scoped `AppContainer` constructor. No production code change for the fake.

## Open questions deferred to §3

- **`activeLanguage.wireKey` resolution** — the ViewModel currently has `AppLanguage` available via `UserPersonaRepository.observeActivePersona()` and the composition-local. `submitUserTurn` expects a `String`; confirm the mapping (`"en"` / `"zh"`) during §3.1 and note in its commit message if a helper is added.
- **`parentMessageId` on first turn** — for a brand-new conversation, null is correct; for subsequent turns the last companion message id may be needed depending on backend contract. `tavern-experience-polish` §3 will refine this. For §3 scope, pass null and cover only the first-turn path in the new instrumentation test.

## Risk register

| Risk | Mitigation |
|---|---|
| Promoting submit to interface breaks existing unit tests that construct `DefaultCompanionTurnRepository` directly. | Make Default throw `NotImplementedError` for the four new methods; verify `CompanionTurnRepositoryTest` + `LiveCompanionTurnRepositoryTest` remain green before committing §3.1. |
| Runtime crash if a companion conversation is persisted without a `companionCardId` (legacy users). | §2.1 default is null ⇒ ViewModel falls back to `messagingRepository.sendMessage`. Non-breaking for legacy. |
| Instrumentation fake emits events faster than Compose can render, racing assertions. | Use `composeRule.waitUntil(timeoutMillis = 2_000) { ... }` for each lifecycle step; the existing `LlmCompanionChatTest` template already uses this idiom. |
