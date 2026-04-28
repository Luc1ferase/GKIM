# Tasks — companion-turn-client-retry-after-countdown

## §0 — OpenSpec scaffold

- [ ] §0.1 — `proposal.md` / `tasks.md` / `specs/llm-text-companion-chat/spec.md` deltas committed on branch `feature/companion-turn-client-retry-after-countdown` off `master` HEAD `5092f95`. Verification: `openspec validate companion-turn-client-retry-after-countdown --strict` returns valid.

## §1 — Wire model: parse `retryAfterMs` from the WS event

- [ ] §1.1 — Add `retryAfterMs: Long? = null` to `ImGatewayEvent.CompanionTurnFailed` (camelCase serialName matches the backend's wire shape). Confirm `ImGatewayEventParser` round-trips: a payload with `"retryAfterMs": 12000` decodes to `Some(12000)`; a payload omitting the key decodes to `null`. Verification: extend `CompanionTurnEventSerializationTest` with a `retry_after_ms_round_trips_when_present` and a `retry_after_ms_decodes_to_null_when_absent` case.

## §2 — Repository: persist `retryAfterEpochMs` on `CompanionTurnMeta`

- [ ] §2.1 — Add `retryAfterEpochMs: Long? = null` to `CompanionTurnMeta` (`core/model/ChatModels.kt`). Default null preserves all existing call sites that build the meta via positional/named args or `.copy()`.

- [ ] §2.2 — Extend `DefaultCompanionTurnRepository`'s constructor with an optional `clockMillis: () -> Long = ::defaultClockMillis` (a private top-level `defaultClockMillis()` returning `System.currentTimeMillis()`). In `handleTurnFailed`, compute `event.retryAfterMs?.let { clockMillis() + it }` and pass it through to the meta's `.copy(retryAfterEpochMs = ...)`. Verification: extend `CompanionTurnRepositoryRecoveryEventTest` with `parsed event-failed with retryAfterMs is persisted as retryAfterEpochMs against a fixed clock` — uses a frozen `clockMillis = { 1_777_377_600_000L }` and a `retryAfterMs = 12_000` payload, asserting the persisted meta carries `retryAfterEpochMs == 1_777_377_612_000L`.

## §3 — ChatRoute Failed-bubble countdown render

- [ ] §3.1 — Add `retryAfterEpochMs: Long? = null` to `CompanionLifecyclePresentation`. `companionLifecyclePresentation()`'s `MessageStatus.Failed` branch reads `meta.retryAfterEpochMs` and propagates it. All other branches (Thinking/Streaming/Completed/Timeout/Blocked) leave it null.

- [ ] §3.2 — Replace the existing single-line Retry render block in `ChatMessageRow` with a branched render:
  - Compute `now` via a `Long` state seeded with `System.currentTimeMillis()` and refreshed by a `LaunchedEffect(presentation.retryAfterEpochMs)` that ticks every 500ms while `now < presentation.retryAfterEpochMs ?: 0L`.
  - When `presentation.retryAfterEpochMs != null && now < presentation.retryAfterEpochMs!!`: render `Text("Retry in <s>s / <s> 秒后重试", color = AetherColors.OnSurfaceVariant)` with NO `clickable` modifier; testTag `chat-companion-retry-countdown-${message.id}`.
  - Otherwise: render the existing clickable Retry exactly as today (no behavioral change for the absent-field path).

## §4 — Tests + spec + archive

- [ ] §4.1 — Author `specs/llm-text-companion-chat/spec.md` delta: 1 MODIFIED Requirement on the typed-Failed terminal Requirement, narrowing the Retry-affordance scenario to honor `retryAfterEpochMs`. Verification: `openspec validate companion-turn-client-retry-after-countdown --strict` passes.

- [ ] §4.2 — `ChatPresentationTest` gets three new cases:
  - `companion lifecycle failed state propagates retryAfterEpochMs onto presentation when meta carries it`
  - `companion lifecycle failed state has null retryAfterEpochMs when meta does not carry it`
  - `companion lifecycle failed state still computes showRetry independently of retryAfterEpochMs presence`
  Run `:app:testDebugUnitTest --tests "*chat.*"` for the focused sweep; confirm the existing 6 Failed-bubble tests + the 3 new ones all pass.

- [ ] §4.3 — One Compose instrumentation test `ChatFailureRetryCountdownInstrumentationTest`:
  - Composes the Failed bubble with `retryAfterEpochMs = now + 1500ms`.
  - Asserts the `chat-companion-retry-countdown-${message.id}` tag is displayed and the bubble's clickable Retry tag is NOT.
  - `composeRule.mainClock.advanceTimeBy(2000)` past the deadline.
  - Asserts the countdown tag is gone and the clickable Retry tag is now displayed.

- [ ] §4.4 — Tick §0.1–§4.3 with commit SHAs in tasks.md. Append delivery evidence rows in `docs/DELIVERY_WORKFLOW.md`. Run `openspec archive companion-turn-client-retry-after-countdown --yes`. Post-archive `openspec validate --specs --strict` returns clean.
