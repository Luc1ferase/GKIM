## Context

The repository now has a live Rust IM backend with working session issuance, bootstrap, history, and authenticated WebSocket flows, but the Android app still renders messaging from `InMemoryMessagingRepository`, `https://api.example.com/`, and `wss://example.com/realtime`. That means the product shell looks plausible, but the IM feature path is still disconnected from the real backend contract.

The user has already decided on the sequencing: first complete the smallest viable app-to-backend wiring, keep testing at each layer while wiring, and only then run full real-device validation. This change therefore needs to stop at "the app is genuinely connected" rather than also absorbing the later adb/SSH/device-evidence workflow. The design should preserve current UI contracts where possible, introduce a clean seam between placeholder and live messaging state, and keep the work reviewable in small accepted slices.

## Goals / Non-Goals

**Goals:**
- Replace the Android seed-only IM path with a backend-backed flow for session, bootstrap, history, and realtime message events.
- Add operator-managed IM runtime configuration for HTTP endpoint, WebSocket endpoint, and development user identity without rebuilding the APK.
- Keep Messages and Chat consuming repository state rather than pushing backend protocol concerns into Compose UI.
- Make each wiring layer independently testable so the change can be accepted incrementally before full device validation.

**Non-Goals:**
- Full adb reverse / SSH tunnel / screenshot evidence workflow for physical-device acceptance.
- Feed or AIGC backend integration in this same change.
- Production authentication, long-term public ingress work, or persistent offline sync storage.
- Broad refactors of unrelated Android screen structure or design system behavior.

## Decisions

### 1. Split "wiring" from "validation" into separate changes
This change will stop at functional app-to-backend wiring and layered verification. The later device-validation change can build on these seams instead of discovering transport and repository problems at the same time.

Why this decision:
- The current app is still disconnected at the repository and WebSocket layers, so full device validation now would mix integration bugs with unfinished plumbing.
- A narrower wiring-first change gives cleaner diffs, clearer verification, and lower rollback risk.
- The user explicitly prefers "wire first, validate after."

Alternatives considered:
- Keep wiring and real-device validation in one change: rejected because it makes task boundaries too wide and slows feedback.

### 2. Introduce a dedicated live messaging repository instead of mutating the seed repository in place
`MessagingRepository` will keep its interface, but the live path will move into a new backend-backed implementation that owns session bootstrap, conversation hydration, history loading, event reconciliation, and integration state. `InMemoryMessagingRepository` can remain available for tests or rollback.

Why this decision:
- The current in-memory repository is simple and useful as a fallback; preserving it keeps the seam explicit.
- A new implementation is easier to reason about than gradually turning the seed repository into a networked state machine.
- Existing ViewModels can stay largely unchanged if the interface contract is preserved.

Alternatives considered:
- Directly rewriting `InMemoryMessagingRepository`: rejected because it would blur fallback behavior and make review harder.

### 3. Keep HTTP and WebSocket responsibilities separate, and let the repository merge them
HTTP will provide the authoritative snapshot path:
- `POST /api/session/dev`
- `GET /api/bootstrap`
- `GET /api/conversations/:conversationId/messages`

WebSocket will provide incremental state changes:
- registration/connection state
- `message.sent`
- `message.received`
- `message.delivered`
- `message.read`
- gateway errors

The repository will merge HTTP snapshots and WebSocket deltas into the `Conversation` and `ChatMessage` UI models.

Why this decision:
- It matches the backend contract already implemented.
- It keeps transport concerns out of ViewModels and Compose UI.
- It creates a natural place for reconnect and rehydration behavior later.

Alternatives considered:
- Poll-only history refresh: rejected because it would fail to validate the backend's realtime path.
- Pushing raw WebSocket payloads into the UI: rejected because it would make state reconciliation brittle.

### 4. Use repository-owned integration state instead of silent fallback
The live messaging repository will expose explicit integration state such as authenticating, bootstrapping, realtime-connecting, ready, and error/degraded conditions. Settings can surface configuration validity, and Messages/Chat can surface real integration failures rather than silently showing seed success.

Why this decision:
- Silent fallback would make it hard to tell whether the app is actually connected.
- Later device-validation work needs visible state to know where failures happen.
- Explicit state improves both unit testing and UI instrumentation.

Alternatives considered:
- Falling back to seed conversations whenever backend access fails: rejected because it hides real integration problems.

### 5. Defer "new conversation from Contacts" if needed to keep the wiring path small
The first live wiring milestone should prioritize existing conversation bootstrap/history and live message flow. If initiating a brand-new conversation from Contacts requires more backend/UI contract work, it can remain staged behind later tasks or follow-up work as long as the existing IM path is live and testable.

Why this decision:
- The shortest proof that App and API are connected is existing conversation hydration plus live send/receive flows.
- Contact-initiated room creation can introduce extra state edges that are not required to prove the transport chain.

Alternatives considered:
- Require brand-new conversation creation in the first wiring slice: rejected because it can expand scope beyond the minimum integration proof.

## Risks / Trade-offs

- [Two active changes now touch similar IM areas] → Keep this change explicitly focused on wiring only, and avoid duplicating full device-validation tasks.
- [Existing UI tests assume fake-backed repositories] → Add HTTP/gateway contract tests plus targeted UI checks that assert backend-driven state surfaces without requiring full end-to-end runs yet.
- [Live repository state may become complex quickly] → Preserve a strict boundary: transport layer parses, repository reconciles, UI only consumes stable models.
- [The backend currently binds to server-localhost] → Do not solve public-access infrastructure here; keep the app configurable so a later validation workflow can supply forwarded endpoints.

## Migration Plan

1. Add or complete transport-layer DTOs, HTTP client contracts, and gateway event parsing with unit tests.
2. Add IM runtime configuration inputs and settings/debug surfaces for the live backend path.
3. Implement the authenticated realtime adapter and backend-backed messaging repository.
4. Switch `AppContainer` and the IM ViewModels onto the live repository path while preserving the existing UI model contract.
5. Add layered regression tests and minimal operator notes that prepare the later full device-validation change.

Rollback strategy:
- Keep `InMemoryMessagingRepository` available behind the same interface.
- Keep IM runtime configuration additive so the app can fall back to placeholder behavior temporarily if live wiring blocks progress.

## Open Questions

- Should the first live wiring cut keep Contacts on local `ensureConversation` behavior until backend-backed room initiation is ready?
- Do we want the live repository to hold a lightweight connection-state enum immediately, or defer richer degraded/retry state until the later validation-focused change?
