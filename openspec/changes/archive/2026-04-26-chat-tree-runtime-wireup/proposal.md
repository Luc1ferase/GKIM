## Why

The tavern-experience-polish slice's §3.1 / §3.2 / §3.3 / §3.4 commits (HEAD `b6ce30e` on `feature/ai-companion-im`) landed the **presentation contracts** for chat branch-tree navigation: sibling-swipe chevrons (`ChatBranchChevronsTest`), Edit-user-bubble overflow (`ChatEditUserBubbleTest`), Regenerate-from-here on every companion bubble (`ChatRegenerateFromHereTest`), and the 4-branch-tree instrumentation (`ChatBranchNavigationInstrumentationTest`). Each landing commit explicitly deferred the production wire-up to a follow-up: the Compose composables that render the affordances, the `ChatViewModel` handlers that mutate state, and the `LiveCompanionTurnRepository` calls that hit the backend's already-deployed `POST /api/companion-turns/:conversationId/edit` and `POST /api/companion-turns/:conversationId/regenerate-at` endpoints.

Without that wire-up, none of the affordances appear in the production app even though every helper, DTO, and HTTP method is reachable. This slice closes the gap.

## What Changes

- **Active-path state in `ChatViewModel`**: introduce a `Map<variantGroupId, activeIndex>` carried alongside the existing `ChatUiState.companionMessages`, persisted across recompositions, and projected back into `ChatMessage.companionTurnMeta.siblingActiveIndex` so the §3.1 chevron rendering path picks it up at every layer of the tree.
- **`LiveCompanionTurnRepository` runtime methods**: add `editUserTurn(conversationId, parentMessageId, newUserText, ...)` and `regenerateCompanionTurnAtTarget(conversationId, targetMessageId)` that call the existing `ImBackendClient` methods, project the response into the in-memory branch tree, and emit the resulting `ChatMessage` updates through the same `activePathByConversation` flow the §3.1 mvp slice already uses for submit + regenerate. Maintain `Map<variantGroupId, List<ChatMessage>>` so siblings can be navigated locally without re-fetching from the backend.
- **`ChatRoute` + `ChatMessageRow` UI**: add the user-bubble Edit overflow + Compose `ModalBottomSheet` edit sheet (prefilled via §3.2 `editUserBubbleSheetState` helper); add the companion-bubble Regenerate-from-here overflow; wire the §3.1 chevron's `onSelectVariantAt` callback to the new `ChatViewModel.selectVariantAt(...)` mutation. The instrumentation test already pins the testTag matrix; this slice produces the production composables that match those tags.
- **`ChatViewModel` handler entry-points**: `editUserTurn(messageId, newText)` (delegates to repository, applies §3.2 `editUserBubbleActivePathEffect` on success) and `regenerateFromHere(messageId)` (delegates, applies §3.3 `regenerateFromHereActivePathEffect`). Both expose lifecycle state (in-flight / failed) on the ViewModel for the affordance UI to read.

## Capabilities

### Modified Capabilities

- `core/im-app`: Android chat surface gains the production-rendered Edit and Regenerate-from-here overflows, the production active-path state in `ChatViewModel`, and the `LiveCompanionTurnRepository` calls that bring the §3.x affordances to life. The presentation contract from `tavern-experience-polish` §3 stays unchanged; this slice fills in the runtime layer beneath it.
- `llm-text-companion-chat`: the existing `LiveCompanionTurnRepository` gains two new public methods (`editUserTurn`, `regenerateCompanionTurnAtTarget`) that compose with the existing submit / regenerate lifecycle. The repository's idempotency contract from §3.2 of the mvp slice extends transparently to the new endpoints (clientTurnId-keyed dedup).

## Impact

- Affected Android code: `feature/chat/ChatRoute.kt` (overflow affordances + edit sheet + variant-tap wiring), `feature/chat/ChatMessageRow.kt` (Edit / Regenerate-from-here render paths on the bubble overflow), `feature/chat/ChatViewModel.kt` (active-path state + handler methods), `data/repository/LiveCompanionTurnRepository.kt` (HTTP wiring + sibling tracking + active-path projection). Compose dependencies extend to `material3.ModalBottomSheet` for the edit sheet.
- Affected backend contract: none new — this slice consumes the already-deployed `tavern-experience-polish-branch-tree-backend` endpoints.
- Affected specs: delta on `core/im-app` (production runtime requirements for the chat-tree affordances) + delta on `llm-text-companion-chat` (new repository methods + active-path projection).
- Affected UX: chat surface gains the Edit + Regenerate-from-here + per-layer chevron affordances visible to users for the first time. The §3.4 instrumentation's testTag matrix becomes hit-able on the production composable, not only on the test's BranchTreeHost.
- Non-goals (scoped out of this slice):
  - JSONL export wire-up (deferred to `chat-export-runtime-wireup` slice)
  - Relationship reset wire-up (deferred to `relationship-reset-runtime-wireup` slice)
  - Per-character preset override prompt-time application (backend-side; deferred to a separate backend slice)
  - Tree-graph visualization surface (proposal §3 explicitly scoped this out)
  - Multi-conversation simultaneous edits (one in-flight edit / regenerate per conversation)
