# Tasks — relationship-reset-runtime-wireup

## §1 — OpenSpec scaffold

- [ ] §1.1 — proposal.md / tasks.md / specs deltas / .openspec.yaml committed; slice opened on a fresh worktree off `feature/ai-companion-im` (HEAD `204febf`).

## §2 — Repository runtime

- [ ] §2.1 — `MessagingRepository.resetRelationship(characterId): Result<Unit>` interface method + `LiveMessagingRepository` impl that calls `ImBackendClient.resetRelationship` and removes matching conversations from the local cache on success; `InMemoryMessagingRepository` keeps a no-op default for tests / non-live impls. Wire-failure remap: HTTP 403 `character_not_available` body → `character_not_available`, otherwise → `network_failure`.

## §3 — UI

- [ ] §3.1 — `RelationshipResetUi.kt` Compose composable rendering the §6.1 `RelationshipResetAffordanceState` state-machine through five visible UI states: Idle (trigger button), Armed (confirmation banner with Cancel + Confirm), Submitting (disabled pending state), Completed (auto-dismiss), Failed (inline error + retry). Localized error copy maps `character_not_available` / `network_failure` to EN/ZH copy.
- [ ] §3.2 — `CharacterDetailRoute` integration — host the affordance state in `mutableStateOf<RelationshipResetAffordanceState>` keyed on `characterId`; render `RelationshipResetButton` after the `ActionRow` on companion cards (gated on `card.companionCardId != null`). On Confirm, launch coroutine that calls `messagingRepository.resetRelationship(characterId)` and projects the result through `markCompleted` / `markFailed`. Apply `applyResetEffect()` on Completed (the cache clear already happened in repo, this is the testTag-aligned spec-mapped record of the local effect).

## §4 — Tests

- [ ] §4.1 — `LiveMessagingRepositoryResetRelationshipTest` — exercises `resetRelationship` with a fake `ImBackendClient`: success removes conversations whose `companionCardId == characterId` from the StateFlow + leaves other conversations intact, HTTP 403 `character_not_available` body maps to error code, IO failure maps to `network_failure`, missing baseUrl/token short-circuits without calling backend.

## §5 — Instrumentation

- [ ] §5.1 — `RelationshipResetButtonInstrumentationTest` — composes `RelationshipResetButton` against a fake `MessagingRepository`; drives the state machine through (a) Idle → Armed → Cancel → Idle (no backend call), (b) Idle → Armed → Confirm → Completed (one backend call, conversations cleared), (c) Confirm → Failed → Retry → Completed (two backend calls, no re-arming gate).

## §6 — Verification + archive

- [ ] §6.1 — Verification roll-up: unit tests green; instrumentation green on `codex_api34`; `openspec validate --specs --strict` passes; manual smoke against `chat.lastxuans.sbs` (open character detail → reset → confirm conversations cleared) — record evidence + scores in `docs/DELIVERY_WORKFLOW.md`.
- [ ] §6.2 — Archive — move `openspec/changes/relationship-reset-runtime-wireup` → `openspec/changes/archive/2026-04-26-relationship-reset-runtime-wireup/`, apply spec deltas to `openspec/specs/core/im-app` + `openspec/specs/llm-text-companion-chat`, run `openspec archive` (manual core/im-app delta merge if openspec CLI nested-capability bug recurs), commit + push.
