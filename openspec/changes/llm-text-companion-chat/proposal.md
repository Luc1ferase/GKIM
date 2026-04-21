## Why

The AI companion branch has a tavern with deep persona cards (`systemPrompt`, `personality`, `scenario`, `exampleDialogue`, `firstMes`, `alternateGreetings`), a bilingual contract, and a backend migration intent — but activating a companion still routes into `ChatRoute.kt` which has no LLM reply path, no persona prompt injection, and no conversation lifecycle beyond "send / receive text". The deep persona fields exist yet nothing consumes them, and the `ai-companion-experience` / `pivot-to-ai-companion-im` lifecycle obligations sit unimplemented. Without this slice, the tavern picks characters that cannot speak.

## What Changes

- **BREAKING** Companion conversations gain a reply lifecycle distinct from peer-to-peer IM messages, with explicit thinking, streaming, completed, failed, and blocked states.
- Introduce a dedicated LLM text chat capability covering turn submission, reply lifecycle events, swipe variant tree, regeneration, first-message / alternate greeting selection on entry, safety/timeout surfaces, and pending turn recovery.
- Modify the Android app contract so companion chat renders lifecycle bubbles, exposes swipe navigation between variants on companion bubbles, and supports user-initiated regeneration that appends variants to the active variant tree.
- Modify the backend contract so a companion turn is submitted over HTTP, lifecycle deltas are emitted over the existing WebSocket gateway, variants are persisted as a conversation tree keyed on `parentMessageId` + `variantGroupId`, and a pending-turn recovery endpoint allows reconnects to rehydrate in-flight turns.
- Assemble persona prompts server-side from deep card fields and substitute `{{user}}` / `<user>` macros with the active account's display name.
- Capture editable-any-bubble and arbitrary-layer regeneration as in-plan follow-ups sharing the same variant-tree data model, so Phase 2 does not require a model rewrite.

## Capabilities

### New Capabilities
- `llm-text-companion-chat`: Defines LLM text reply lifecycle, swipe variant tree, regeneration, first-message selection, pending turn recovery, and the service-boundary for persona prompt assembly.

### Modified Capabilities
- `core/im-app`: The Android app requirements change so companion chat renders reply lifecycle states, swipe controls, regenerate affordance, greeting picker on first entry, and explicit blocked / failed / timeout bubbles.
- `im-backend`: The backend requirements change so companion turns are submitted over HTTP, lifecycle events stream over WebSocket, variants persist as a tree, and pending turns rehydrate on reconnect.

## Impact

- Affected Android code: `core/model/ChatModels.kt` (add `MessageStatus`, `CompanionTurnMeta`, `parentMessageId`), `data/remote/im/ImBackendModels.kt` (new turn DTOs + lifecycle event types), `data/remote/im/ImBackendClient.kt` (submit/regenerate/list-pending endpoints), `data/remote/realtime/*` (parser extension), `data/repository/*` (new `CompanionTurnRepository`, wiring in `AppContainer`), `feature/chat/ChatRoute.kt` (lifecycle UI + swipe + greeting picker + regenerate), new feature route for the greeting picker if needed.
- Affected backend contract: new turn ingest + regenerate endpoints, new WS event shapes, new variant table, new pending-turn recovery endpoint, server-side persona prompt assembly with `{{user}}` substitution, language steering append, safety policy stub hook.
- Affected specs: new `llm-text-companion-chat`, plus deltas for `core/im-app` and `im-backend`.
- Affected UX: selecting a tavern companion and opening the conversation for the first time presents a greeting picker; sending a message shows a thinking → streaming companion bubble; the user can swipe between variants and regenerate the current variant on the most recent companion bubble; reconnect resumes in-progress turns; blocked/failed/timeout states surface explicitly instead of silently dropping.
- Non-goals (scoped out of this slice, already captured as downstream task IDs):
  - Arbitrary-layer regenerate + editable-any-bubble (follow-up phase within the same data model; no rewrite needed)
  - User persona beyond `{{user}}` display-name substitution (→ change `user-persona`)
  - Memory summaries beyond the recent-N-turns window (→ change `companion-memory-and-preset`)
  - World Info / lorebook injection (→ change `world-info-binding`)
  - Per-character provider / model / temperature override (→ change `tavern-experience-polish`)
  - SillyTavern PNG/JSON import-export (→ change `sillytavern-card-interop`)
  - Voice / TTS / STT, multi-agent rooms, tool use, function calling
  - Cost accounting / rate limiting / quota UI
