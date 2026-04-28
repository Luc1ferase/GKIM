# companion-turn-client-retry-after-countdown

## Why

The paired backend slice `companion-turn-backend-recovery-followups` (archived 2026-04-28) parses upstream `Retry-After` headers on HTTP 429 and surfaces the parsed millisecond delay as a `retryAfterMs: Long?` field on the `companion_turn.failed` wire event. The Kotlin client's `ImGatewayEvent.CompanionTurnFailed` parser currently ignores the new field — kotlinx-serialization silently drops unknown JSON keys — so the user-visible behavior is unchanged: a `Failed{transient}` bubble renders an always-clickable "Retry / 重试" button, the user taps it within the rate-limit window, and the upstream rejects again with 429.

The user-perceived effect is a system that looks broken: tap → fail → tap → fail → tap → fail. The actionable wire data the backend already ships gets thrown away.

This slice closes the loop by:
1. Parsing `retryAfterMs` into the Kotlin event data class.
2. Computing an absolute `retryAfterEpochMs` (= reception-time + retryAfterMs) and persisting it onto the message's `CompanionTurnMeta` so it survives across reconnects + bootstrap replay (the relative ms field would drift; the absolute timestamp is reconnect-stable).
3. Rendering the Retry affordance as a disabled countdown ("Retry in 12s / 12 秒后重试") while `now < retryAfterEpochMs`, transitioning to a clickable Retry button when the deadline elapses.

## What Changes

- **`ImGatewayEvent.CompanionTurnFailed` data class** gains `retryAfterMs: Long? = null`. Existing fixtures that omit the key continue to decode (kotlinx-serialization treats absent + default-valued as the same case). The `subtypeAsFailedSubtype` accessor is unchanged.
- **`CompanionTurnMeta`** gains `retryAfterEpochMs: Long? = null`. Stored as an absolute epoch-millis timestamp (NOT relative) so a reconnecting client that re-receives the same event after a long gap still computes the correct remaining countdown.
- **`DefaultCompanionTurnRepository.handleTurnFailed`** computes `clockMillis() + event.retryAfterMs` when the event carries the new field, and stores it onto the meta. The repository constructor gains an optional `clockMillis: () -> Long = ::defaultClockMillis` so tests can pin behavior without scheduling delays.
- **`CompanionLifecyclePresentation`** gains `retryAfterEpochMs: Long? = null`; the `MessageStatus.Failed` branch propagates `meta.retryAfterEpochMs` into the presentation. Other branches (Completed / Streaming / Blocked / Timeout) leave the field null.
- **Failed bubble Retry render** in `ChatRoute.kt` gains a countdown branch: when `presentation.retryAfterEpochMs != null && now < retryAfterEpochMs`, the bubble shows "Retry in `<seconds>`s / `<seconds>` 秒后重试" with `OnSurfaceVariant` text color (visually disabled), no `clickable` modifier, and a `LaunchedEffect` that ticks every 500ms to drive recomposition. When the deadline elapses (or the field was never set), the bubble renders the existing clickable Retry button unchanged.

## Impact

- Affected code:
  - `core/model/ChatModels.kt::CompanionTurnMeta` — add `retryAfterEpochMs: Long? = null`.
  - `data/remote/im/ImBackendModels.kt::ImGatewayEvent.CompanionTurnFailed` — add `retryAfterMs: Long? = null`.
  - `data/repository/CompanionTurnRepository.kt::DefaultCompanionTurnRepository` — accept an optional `clockMillis` callback in the constructor (default `System::currentTimeMillis`); thread the parsed `retryAfterMs` into `retryAfterEpochMs` on `handleTurnFailed`.
  - `feature/chat/ChatRoute.kt::CompanionLifecyclePresentation` — add the field + propagate; `companionLifecyclePresentation()`'s Failed branch reads `meta.retryAfterEpochMs`.
  - `feature/chat/ChatRoute.kt::ChatMessageRow`'s Failed-affordance render — branch on the active-countdown predicate; emit either the disabled-countdown Text or the existing clickable Retry Text.
- Test coverage:
  - `ChatPresentationTest` adds three Failed-branch cases:
    - `companion lifecycle failed state propagates retryAfterEpochMs onto presentation when meta carries it`
    - `companion lifecycle failed state has null retryAfterEpochMs when meta does not carry it`
    - `companion lifecycle failed state still computes showRetry independently of retryAfterEpochMs presence` (regression guard for the existing taxonomy logic)
  - `CompanionTurnRepositoryRecoveryEventTest` (the existing unit-level repository test from S3 §2.3) adds one case asserting the repository plus a frozen-clock injection produces a deterministic `retryAfterEpochMs`.
  - One Compose instrumentation test (`ChatFailureRetryCountdownInstrumentationTest`) asserts the disabled countdown renders ahead of the deadline and switches to the clickable Retry once the deadline elapses (driven by a synthetic clock advancing past the deadline).
- Spec delta: `llm-text-companion-chat` capability gains 1 MODIFIED Requirement (the typed-Failed-terminal Requirement narrows from "Retry button is always clickable on a transient Failed bubble" to "Retry button respects the optional retryAfterEpochMs deadline if present").
- Backwards compatibility:
  - Older WS payloads that omit `retryAfterMs` continue to deserialize; the field defaults to null and the Retry button renders as today.
  - The new `CompanionTurnMeta.retryAfterEpochMs` field defaults to null; any code that copies `CompanionTurnMeta` via `.copy()` without naming the new field gets null automatically.
- Risks:
  - The 500ms tick in `LaunchedEffect` runs only while the Failed bubble is composed — backgrounding the app pauses the recomposition. When the user returns, the next composition reads `now()` again and either continues the countdown or jumps to "Retry" if the deadline passed. No sticky state.
  - Wall-clock skew between client and backend isn't compensated. If a client clock drifts by 30s+, a 12s countdown may render slightly off. The Retry-After contract is wall-clock-relative, not server-time-relative; a small drift is acceptable since the user's tap precision exceeds the drift.
