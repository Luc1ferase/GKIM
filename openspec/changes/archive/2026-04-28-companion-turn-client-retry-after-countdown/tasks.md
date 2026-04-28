# Tasks — companion-turn-client-retry-after-countdown

## §0 — OpenSpec scaffold

- [x] §0.1 — `proposal.md` / `tasks.md` / `specs/llm-text-companion-chat/spec.md` deltas committed on branch `feature/companion-turn-client-retry-after-countdown` off `master` HEAD `5092f95`. Scaffold commit `47d4803`. `openspec validate companion-turn-client-retry-after-countdown --strict` returns `Change 'companion-turn-client-retry-after-countdown' is valid`.

## §1 — Wire model: parse `retryAfterMs` from the WS event

- [x] §1.1 — Added `retryAfterMs: Long? = null` to `ImGatewayEvent.CompanionTurnFailed` data class (camelCase matches backend wire shape). kotlinx-serialization treats absent + default-valued as the same case → existing fixtures and pre-F2 payloads continue to decode unchanged. Evidence: commit `75eea08`. Verified by two new round-trip cases in `CompanionTurnEventSerializationTest`: `companion_turn failed payload with retryAfterMs round-trips into CompanionTurnFailed retryAfterMs` (asserts 12_000L parses correctly) + `companion_turn failed payload without retryAfterMs decodes to null retryAfterMs` (back-compat).

## §2 — Repository: persist `retryAfterEpochMs` on `CompanionTurnMeta`

- [x] §2.1 — Added `retryAfterEpochMs: Long? = null` to `CompanionTurnMeta` in `core/model/ChatModels.kt`. Default null preserves all existing call sites (positional/named args + `.copy()`). Evidence: commit `f90d9c0`.

- [x] §2.2 — Extended `DefaultCompanionTurnRepository`'s constructor with `clockMillis: () -> Long = ::defaultClockMillis` (the new internal top-level `defaultClockMillis()` returns `System.currentTimeMillis()`). In `handleTurnFailed`, computed `event.retryAfterMs?.let { clockMillis() + it }` is threaded into the meta. Constructor change is back-compat — existing `DefaultCompanionTurnRepository()` no-arg construction still compiles. Evidence: commit `f90d9c0`. Verified by two new cases in `CompanionTurnRepositoryRecoveryEventTest`: frozen-clock `1_777_377_600_000L` + retryAfterMs=12_000 → asserts persisted `retryAfterEpochMs == 1_777_377_612_000L`; absent-field path leaves it null.

## §3 — ChatRoute Failed-bubble countdown render

- [x] §3.1 — Added `retryAfterEpochMs: Long? = null` to `CompanionLifecyclePresentation`. `companionLifecyclePresentation()`'s `MessageStatus.Failed` branch reads `meta.retryAfterEpochMs` and propagates it. Other branches (Thinking/Streaming/Completed/Timeout/Blocked) leave it null. Evidence: commit `84e2cc2`.

- [x] §3.2 — Replaced the single-line Retry render block in `ChatMessageRow` with a branched render: when `deadline != null && now < deadline`, emits `Text("Retry in Ns / N 秒后重试", color = OnSurfaceVariant)` with no `clickable` modifier and testTag `chat-companion-retry-countdown-${message.id}`; a `LaunchedEffect(deadline)` ticks every 500ms to drive recomposition; the loop self-terminates when `now >= deadline`. Otherwise (deadline null OR elapsed): renders the existing clickable Retry Text exactly as today. The remaining seconds are `ceil((deadline - now) / 1000).coerceAtLeast(1L)` so the visible countdown never blips to "0s" before flipping to clickable Retry. Evidence: commit `84e2cc2`. `:app:compileDebugKotlin` BUILD SUCCESSFUL.

## §4 — Tests + spec + archive

- [x] §4.1 — Spec delta `specs/llm-text-companion-chat/spec.md` modifies the existing "Companion reply lifecycle is explicit and bounded" Requirement: extended its top description to cover the retryAfterMs → retryAfterEpochMs translation invariant, and added two new Scenarios — `companion_turn.failed event with retryAfterMs is persisted as absolute retryAfterEpochMs` (parse + persist) and `Failed bubble Retry affordance renders disabled countdown while retryAfterEpochMs is in the future` (render). Authored at scaffold time `47d4803`; `openspec validate --strict` returns valid.

- [x] §4.2 — Extended `ChatPresentationTest` with three Failed-branch cases (commit `c15852f`):
  - `companion lifecycle failed state propagates retryAfterEpochMs onto presentation when meta carries it` (asserts `retryAfterEpochMs == 1_777_377_612_000L` flows through + `showRetry == true`)
  - `companion lifecycle failed state has null retryAfterEpochMs when meta does not carry it` (back-compat)
  - `non-failed lifecycle states never carry retryAfterEpochMs even when meta is set` (defensive — only Failed branch propagates; Streaming/Timeout explicitly drop)
  Drive-by: `companionMessage()` helper gains `failedSubtypeKey` + `retryAfterEpochMs` optional params. `:app:testDebugUnitTest` full sweep BUILD SUCCESSFUL (no other test broke).

- [ ] §4.3 — DEFERRED — Compose instrumentation test (`ChatFailureRetryCountdownInstrumentationTest`) needs an emulator and would advance `composeRule.mainClock` past the deadline to verify the live tag transition. Deferred because the wire→repository→presentation chain is fully pinned by §1.1 / §2.2 / §4.2 unit tests, and the §3.2 render branch is mechanical (one if-else over `now < deadline`); the marginal value of an instrumentation test is small relative to the emulator setup cost. Re-open when the next emulator-driven sweep runs and add the case alongside the existing `Chat*InstrumentationTest` files.

- [x] §4.4 — Tasks ticked above with commit SHAs. Delivery evidence rows recorded in `docs/DELIVERY_WORKFLOW.md` under `companion-turn-client-retry-after-countdown delivery evidence`. `openspec archive companion-turn-client-retry-after-countdown --yes` moves the slice to `openspec/changes/archive/2026-04-28-companion-turn-client-retry-after-countdown/` and applies the spec delta onto `openspec/specs/llm-text-companion-chat/spec.md`. Post-archive `openspec validate --specs --strict` returns clean across all 16 client capabilities.
