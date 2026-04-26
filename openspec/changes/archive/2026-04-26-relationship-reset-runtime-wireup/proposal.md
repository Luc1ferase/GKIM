## Why

The tavern-experience-polish slice's §6.1 commit landed the **presentation contract** for the relationship-reset affordance (`RelationshipResetAffordanceState` two-step state machine in `feature/tavern/RelationshipReset.kt`, plus `RelationshipResetEffect` describing the local-cache mutation). The `tavern-experience-polish-reset-backend` slice then shipped `POST /api/relationships/{characterId}/reset` (deployed 2026-04-26 to `chat.lastxuans.sbs`), and the Retrofit method `ImBackendClient.resetRelationship(...)` is already in place from the polish slice.

What is missing is the **production wire-up**: the Compose UI surface that renders the §6.1 state-machine on `CharacterDetailScreen`, the entry-point trigger button on the action row, the repository method that calls `ImBackendClient.resetRelationship` and projects the success into a local-cache mutation (clear conversations for the user×character pair), and the failure-path retry that re-invokes the endpoint without re-arming the two-step gate.

## What Changes

- **`MessagingRepository.resetRelationship`**: new method on `MessagingRepository` (interface + `LiveMessagingRepository` impl + `InMemoryMessagingRepository` no-op) that calls `ImBackendClient.resetRelationship(characterId)`, on success removes any `Conversation` whose `companionCardId == characterId` from the local `conversations` StateFlow, and surfaces wire-failure error codes (`character_not_available` / `network_failure`) back to the caller. The local cache mutation matches the §6.1 `RelationshipResetEffect.clearConversationsForCharacter` shape.
- **`RelationshipResetButton` Compose composable** in `feature/tavern/RelationshipResetUi.kt`: renders the §6.1 affordance state-machine — Idle shows the "Reset relationship" trigger; Armed shows the destructive-action confirmation banner with Cancel + Confirm buttons; Submitting shows a "Resetting…" disabled state; Failed shows an inline error + retry button (retry re-invokes without re-arming per §9.2 spec); Completed auto-dismisses back to Idle after the cache mutation runs.
- **`CharacterDetailRoute` integration**: gate the affordance on `card.companionCardId != null` (i.e., not the system / built-in placeholder cards) and host the composable + state inside the route. The action row gains a third button below Activate / Export. Tapping Reset opens the affordance flow inline (not a modal) — destructive-action confirmation is rendered as an inline banner per the §6.1 contract, not a separate dialog.

## Capabilities

### Modified Capabilities

- `core/im-app`: Android character-detail screen gains the production-rendered "Reset relationship" affordance, with the two-step destructive-action gate, inline error + retry, and auto-dismiss on success. The §6.1 state-machine contract from `tavern-experience-polish` stays unchanged; this slice fills in the visible Compose surface and the local-cache mutation that runs on success.
- `llm-text-companion-chat`: `MessagingRepository` gains a `resetRelationship(characterId)` method that delegates to `ImBackendClient.resetRelationship`. Mirrors the existing `editUserTurn` / `regenerateCompanionTurnAtTarget` shape from `chat-tree-runtime-wireup` — Result-typed, error-coded, server-authoritative on memory + greeting state with client-side cache reconciliation for conversations.

## Impact

- Affected Android code: new file `feature/tavern/RelationshipResetUi.kt` (Compose composable rendering the affordance + retry path), modified `feature/tavern/CharacterDetailRoute.kt` (action-row entry + inline affordance host), modified `data/repository/Repositories.kt` (new method on `MessagingRepository` interface + `LiveMessagingRepository` impl + `InMemoryMessagingRepository` no-op).
- Affected backend contract: none new — this slice consumes the already-deployed `tavern-experience-polish-reset-backend` endpoint.
- Affected specs: delta on `core/im-app` (production runtime requirement for the reset affordance + retry) + delta on `llm-text-companion-chat` (new repository method).
- Affected UX: character-detail screen for companion cards gains a "Reset relationship" button. Tap shows a destructive-action confirmation banner; confirm clears the conversations list for that character pair locally (memory + greeting state are cleared server-side, picked up on next bootstrap).
- Non-goals (scoped out of this slice):
  - Multi-character batch reset (one character per affordance invocation).
  - Local memory-pin cache invalidation (memory pins aren't currently surfaced in a client-side cache that needs explicit clearing — backend reset + next bootstrap handles them).
  - Re-greeting / first-message replay after reset (the next time the user opens the conversation, the standard companion-conversation bootstrap path runs; this slice does not pre-stage the new greeting).
  - Reset undo (the destructive action is irreversible per the §6.1 spec; the two-step gate is the safety mechanism).
  - Reset history / audit log (the user can see whether a conversation exists; no separate "reset performed at" record).
