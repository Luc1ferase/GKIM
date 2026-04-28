# companion-turn-client-retry-after-snackbar

## Why

The just-archived `companion-turn-client-retry-after-countdown` slice (2026-04-28) consumed F2-backend's `retryAfterMs` field by rendering a disabled-countdown Text on the Failed bubble's Retry affordance ("Retry in 12s / 12 秒后重试"). On review the visible countdown was rejected as visually noisy — the bubble's affordance row already carries Edit / Retry / Compose-new / Learn-more, and adding a live decrement-every-500ms timer reads as anxious / busy.

The corrected UX: keep the Retry button always clickable + visually unchanged, and only when the user actually taps it WITHIN the cooldown window do we surface an ephemeral, dismissable notice ("Retry available in `<N>`s / `<N>` 秒后才能重试"). If the user never taps during cooldown they never see the timer at all. The wire data (`retryAfterEpochMs` on `CompanionTurnMeta`) and the data-flow into `CompanionLifecyclePresentation` are unchanged from the prior slice; only the render strategy changes.

This is a tiny correction slice that supersedes the §3.2 + §4.1 paragraphs of the prior change — same data path, different presentation policy.

## What Changes

- **`ChatRoute.kt::ChatMessageRow`'s Retry render block** is reverted to a single clickable `Text("Retry / 重试")`, dropping the `LaunchedEffect`-driven `nowMs` tick state and the disabled-countdown Text branch. The `chat-companion-retry-countdown-${id}` testTag is retired.
- **`ChatRoute.kt::ChatScreen`** hoists a `cooldownNotice: String?` state with a `LaunchedEffect(cooldownNotice)` auto-dismiss (3 seconds). When non-null, renders a small `Surface`-wrapped Text banner near the top of the Column (just under the persona footer / above the messages list), styled as a soft pill with `OnSurfaceVariant` text. testTag `chat-retry-cooldown-notice`.
- **`ChatMessageRow`** gains a new `onTooEarlyRetry: (remainingSeconds: Long) -> Unit` callback parameter. The Retry button's `onClick` lambda computes `now vs presentation.retryAfterEpochMs` at click time:
  - If `deadline != null && now < deadline` → calls `onTooEarlyRetry(remainingSeconds)` (does NOT invoke the existing `onRetryCompanionTurn`).
  - Else → invokes `onRetryCompanionTurn()` exactly as today.
- **`ChatScreen`** binds `onTooEarlyRetry` to `{ secs -> cooldownNotice = formatRetryCooldownNotice(secs, language) }`, where the helper builds the bilingual "Retry available in `<secs>`s / `<secs>` 秒后才能重试" string.
- **`CompanionTurnMeta.retryAfterEpochMs` and `CompanionLifecyclePresentation.retryAfterEpochMs` are unchanged.** The data-flow tests in `ChatPresentationTest` keep passing.

## Impact

- Affected code:
  - `feature/chat/ChatRoute.kt` — touch the Retry render block in `ChatMessageRow`, hoist cooldownNotice state in `ChatScreen`, add a small banner Composable, thread the new callback.
- Test coverage:
  - The 3 §4.2 ChatPresentationTest cases from the prior slice keep passing — they pin data flow into presentation, which is unchanged.
  - One new presentation-layer helper test for `formatRetryCooldownNotice(secs: Long, language: AppLanguage)` (English + Chinese forms).
  - Compose instrumentation deferred (same rationale as the prior slice's §4.3 deferral).
- Spec delta: `llm-text-companion-chat` — 1 MODIFIED Requirement: rewrites the prior slice's "Failed bubble Retry affordance renders disabled countdown" scenario to the new "Retry tap during cooldown surfaces ephemeral notice" scenario. The retryAfterMs → retryAfterEpochMs translation invariant on the top description stays.
- Backwards compatibility:
  - Wire format unchanged.
  - Repository/state unchanged.
  - Only the render-time strategy flips. A user who already had a Failed bubble in-flight when the binary ships sees the same data, just no longer rendered as a visible countdown.
- Risks:
  - The 3-second auto-dismiss may feel too short for a slow reader. 3s matches the typical `Snackbar.Short` duration; if users complain we'd extend to 4-5s in a follow-up.
  - The cooldown banner's anchor (top of the Column under the persona footer) means it might not be visible if the user has scrolled deep into the timeline. Acceptable: the user just took an action that triggered it, so the screen is in focus.
