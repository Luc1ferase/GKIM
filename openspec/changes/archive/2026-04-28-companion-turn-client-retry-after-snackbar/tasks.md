# Tasks — companion-turn-client-retry-after-snackbar

## §0 — OpenSpec scaffold

- [x] §0.1 — `proposal.md` / `tasks.md` / `specs/llm-text-companion-chat/spec.md` deltas committed on branch `feature/companion-turn-client-retry-after-snackbar` off `master` HEAD `3e355f2`. Scaffold commit `cac3581`. `openspec validate companion-turn-client-retry-after-snackbar --strict` returns valid.

## §1 — Strip countdown render; wire ephemeral on-tap notice

- [x] §1.1 — Reverted the Retry render block in `ChatMessageRow` to a single clickable `Text("Retry / 重试")`. Removed the `LaunchedEffect(deadline)` tick, the `nowMs` state, and the `chat-companion-retry-countdown-${id}` testTag (no consumers). `CompanionLifecyclePresentation.retryAfterEpochMs` stays — read at click time.

- [x] §1.2 — Added `onTooEarlyRetry: (remainingSeconds: Long) -> Unit = {}` parameter to `ChatMessageRow`. The Retry `onClick` lambda computes `now` + reads `companionPresentation.retryAfterEpochMs`: if `deadline != null && now < deadline`, calls `onTooEarlyRetry(remainingSeconds)` with `((deadline - now + 999) / 1000).coerceAtLeast(1L)` (suppressing the retry dispatch); else dispatches `onRetryCompanionTurn()` unchanged.

- [x] §1.3 — In `ChatScreen`: hoisted `var cooldownNotice by remember { mutableStateOf<String?>(null) }` plus `LaunchedEffect(cooldownNotice)` that delays 3000ms and clears non-null state. The single `ChatMessageRow` call site now passes `onTooEarlyRetry = { secs -> cooldownNotice = formatRetryCooldownNotice(secs, appLanguage) }`.

- [x] §1.4 — Rendered the cooldownNotice as a `Surface` (rounded 12dp, `SurfaceContainerLow` background — no `SurfaceVariant` token in the AetherColors palette) anchored just under the persona footer with 8dp top-padding, 12dp horizontal + 8dp vertical inner padding, `OnSurfaceVariant` text color, testTag `chat-retry-cooldown-notice`. Hidden via `cooldownNotice?.let { ... }` when null.

- [x] §1.5 — Added `internal fun formatRetryCooldownNotice(remainingSeconds: Long, language: AppLanguage): String` near `TIMEOUT_PRESET_HINT_MAX_REPLY_TOKENS_CAP`. English form: `"Retry available in ${remainingSeconds}s"`; Chinese: `"${remainingSeconds} 秒后才能重试"`. Pinned by 3 new unit tests in `ChatPresentationTest`.

§1 commit: `ba2aa75`. `:app:compileDebugKotlin` + `:app:testDebugUnitTest` full sweep BUILD SUCCESSFUL.

## §2 — Tests + spec + archive

- [x] §2.1 — Spec delta authored at scaffold time `cac3581`. Modifies the existing "Companion reply lifecycle is explicit and bounded" Requirement: rewrote the top description's render-policy paragraph to specify "always-clickable button + on-tap ephemeral notice" instead of the F2c "disabled-countdown render". Replaced the F2c scenario "Failed bubble Retry affordance renders disabled countdown while retryAfterEpochMs is in the future" with the new scenario "Retry tap during cooldown surfaces ephemeral notice without dispatching the retry". The retryAfterMs → retryAfterEpochMs persistence scenario stays unchanged. `openspec validate --strict` returns valid.

- [x] §2.2 — Added 3 new unit tests in `ChatPresentationTest`: `formatRetryCooldownNotice English form`, `formatRetryCooldownNotice Chinese form`, `formatRetryCooldownNotice clamps the rendered second to the helper's input regardless of size` (defensive split between caller-side clamping and helper-side passthrough). The 3 F2c presentation cases (data flow into `retryAfterEpochMs`) keep passing — they pin the data path which this slice doesn't touch. `:app:testDebugUnitTest` full sweep BUILD SUCCESSFUL.

- [x] §2.3 — Tasks ticked above with commit SHAs. Delivery evidence rows recorded in `docs/DELIVERY_WORKFLOW.md` under `companion-turn-client-retry-after-snackbar delivery evidence`. `openspec archive companion-turn-client-retry-after-snackbar --yes` moves the slice to `openspec/changes/archive/2026-04-28-companion-turn-client-retry-after-snackbar/` and applies the spec delta onto `openspec/specs/llm-text-companion-chat/spec.md` (1 Requirement modified). Post-archive `openspec validate --specs --strict` returns 16 passed, 0 failed.

- [x] §2.4 — Merged `feature/companion-turn-client-retry-after-snackbar` → `master` via `--no-ff`; pushed to origin.
