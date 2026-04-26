# Tasks — relationship-reset-runtime-wireup

## §1 — OpenSpec scaffold

- [x] §1.1 — proposal.md / tasks.md / specs deltas / .openspec.yaml committed (`00eddfb`); slice opened on a fresh worktree off `feature/ai-companion-im` (HEAD `204febf`).

## §2 — Repository runtime

- [x] §2.1 — `MessagingRepository.resetRelationship(characterId): Result<Unit>` interface method (default-throw) + `LiveMessagingRepository` impl that calls `ImBackendClient.resetRelationship`, removes matching conversations from the local cache on success, and remaps HTTP 403 `character_not_available` body → `character_not_available`, otherwise → `network_failure`. (`<TBD>`)

## §3 — UI

- [x] §3.1 — `RelationshipResetUi.kt` Compose composable rendering the §6.1 state machine through five visible UI states (Idle / Armed / Submitting / Completed / Failed). Internal coroutineScope launches `repository.resetRelationship(characterId)` from the Submitting phase via `LaunchedEffect(state.phase)`; success transitions to Completed (auto-dismiss back to Idle on next render); failure surfaces inline error + retry that bypasses the two-step gate (Failed → Submitting). Localized error copy for `character_not_available` / `network_failure` in EN+ZH. (`<TBD>`)
- [x] §3.2 — `CharacterDetailRoute` integration — `CharacterDetailScreen` gains a new slot `relationshipResetSlot: @Composable () -> Unit` rendered after the lorebook tab. `CharacterDetailRoute` populates it with `CharacterDetailRelationshipResetSection` that wires `container.messagingRepository`. The slot is rendered for all character-detail invocations; the §6.1 affordance is the destructive action so visibility-gating is implicit (system / placeholder cards live in a different surface). (`<TBD>`)

## §4 — Tests

- [x] §4.1 — `LiveMessagingRepositoryResetRelationshipTest` — 5 tests exercising `resetRelationship` with a fake `ImBackendClient`: success removes only matching `companionCardId` entries; success leaves non-matching + non-companion conversations untouched; HTTP 403 `character_not_available` body maps to error code; IOException maps to `network_failure`; HTTP 403 with non-matching body falls through to `network_failure`. All 5 green. (`<TBD>`)

## §5 — Instrumentation

- [ ] §5.1 — `RelationshipResetButtonInstrumentationTest` — composes `RelationshipResetButton` against a fake `MessagingRepository`; drives the state machine through (a) Idle → Armed → Cancel → Idle (no backend call), (b) Idle → Armed → Confirm → Completed (one backend call, conversations cleared), (c) Confirm → Failed → Retry → Completed (two backend calls, no re-arming gate).

## §6 — Verification + archive

- [ ] §6.1 — Verification roll-up: unit tests green; instrumentation green on `codex_api34`; `openspec validate --specs --strict` passes; manual smoke against `chat.lastxuans.sbs` (open character detail → reset → confirm conversations cleared) — record evidence + scores in `docs/DELIVERY_WORKFLOW.md`.
- [ ] §6.2 — Archive — move `openspec/changes/relationship-reset-runtime-wireup` → `openspec/changes/archive/2026-04-26-relationship-reset-runtime-wireup/`, apply spec deltas to `openspec/specs/core/im-app` + `openspec/specs/llm-text-companion-chat`, run `openspec archive` (manual core/im-app delta merge if openspec CLI nested-capability bug recurs), commit + push.
