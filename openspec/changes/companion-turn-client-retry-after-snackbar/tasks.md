# Tasks — companion-turn-client-retry-after-snackbar

## §0 — OpenSpec scaffold

- [ ] §0.1 — `proposal.md` / `tasks.md` / `specs/llm-text-companion-chat/spec.md` deltas committed on branch `feature/companion-turn-client-retry-after-snackbar` off `master` HEAD `3e355f2`. Verification: `openspec validate companion-turn-client-retry-after-snackbar --strict` returns valid.

## §1 — Strip countdown render; wire ephemeral on-tap notice

- [ ] §1.1 — In `ChatRoute.kt::ChatMessageRow`, revert the Retry render block to a single clickable `Text("Retry / 重试")`. Remove the `LaunchedEffect(deadline)` tick, the `nowMs` state, and the `chat-companion-retry-countdown-${id}` testTag. The `companionPresentation.retryAfterEpochMs` field STAYS (the click handler reads it).

- [ ] §1.2 — Add `onTooEarlyRetry: (remainingSeconds: Long) -> Unit = {}` to the `ChatMessageRow` parameter list. The Retry `onClick` lambda computes `now = System.currentTimeMillis()`, `deadline = companionPresentation.retryAfterEpochMs`:
  - If `deadline != null && now < deadline`: compute `remainingSeconds = ((deadline - now + 999) / 1000).coerceAtLeast(1L)` and call `onTooEarlyRetry(remainingSeconds)`. MUST NOT invoke `onRetryCompanionTurn`.
  - Else: invoke `onRetryCompanionTurn()` unchanged.

- [ ] §1.3 — In `ChatRoute.kt::ChatScreen`, hoist `var cooldownNotice by remember { mutableStateOf<String?>(null) }`. Bind `onTooEarlyRetry` on each `ChatMessageRow` to `{ secs -> cooldownNotice = formatRetryCooldownNotice(secs, appLanguage) }`. Add a `LaunchedEffect(cooldownNotice)` that delays 3000ms and clears the state when non-null.

- [ ] §1.4 — Render the cooldownNotice as a small `Surface` (rounded-corner, `SurfaceVariant` background, 12dp horizontal + 8dp vertical padding) just under the persona footer / above the in-flight tree-affordance row. Hidden when `cooldownNotice == null`. testTag `chat-retry-cooldown-notice`.

- [ ] §1.5 — Add a top-level helper `internal fun formatRetryCooldownNotice(remainingSeconds: Long, language: AppLanguage): String`:
  - English: `"Retry available in ${remainingSeconds}s"`
  - Chinese: `"${remainingSeconds} 秒后才能重试"`
  Pinned by a small unit test in a new `ChatRetryCooldownNoticeTest` file (or appended to `ChatPresentationTest`).

## §2 — Tests + spec + archive

- [ ] §2.1 — Author `specs/llm-text-companion-chat/spec.md` delta: 1 MODIFIED Requirement on the existing "Companion reply lifecycle is explicit and bounded" Requirement, replacing the F2c "Failed bubble Retry affordance renders disabled countdown" scenario with a new "Retry tap during cooldown surfaces ephemeral notice" scenario. The retryAfterMs → retryAfterEpochMs translation invariant stays.

- [ ] §2.2 — Add `formatRetryCooldownNotice` helper test (English + Chinese forms) and any minor adjustments needed to existing presentation tests. Run `:app:testDebugUnitTest --tests "*chat.*"` for the focused sweep.

- [ ] §2.3 — Tick §0.1–§2.2. Append delivery evidence rows in `docs/DELIVERY_WORKFLOW.md`. Run `openspec archive companion-turn-client-retry-after-snackbar --yes`. Post-archive `openspec validate --specs --strict` returns clean.

- [ ] §2.4 — Merge `feature/companion-turn-client-retry-after-snackbar` → `master` via `--no-ff`; push.
