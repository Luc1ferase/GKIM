# companion-turn-client-recovery-and-safety

## Why

The Kotlin client has been wired since `companion-settings-and-safety-reframe` (archived 2026-04-24) to render typed `Failed | Blocked | Timeout` terminals with the right Retry / Edit-user-turn / Check-connection-hint affordances. The wire shape parser (`ImBackendModels.kt:1128-1155`) already accepts `companion_turn.failed | .blocked | .timeout` discriminator strings and the `FailedSubtype` enum (`core/model/FailedSubtype.kt`) carries the six-value taxonomy. What's been missing is the BACKEND emitting these typed terminals — until now every upstream LLM error flowed through the §6 safety-net Delta + Completed path.

The paired backend slice `companion-turn-backend-recovery-and-safety` (in `GKIM-Backend`) starts emitting the typed terminals. This client slice closes the loop:

1. **Verifies the wire shape against the new backend fixtures.** The backend authors three new event fixtures (`event-failed.json`, `event-blocked.json`, `event-timeout.json`) and mirrors them byte-equivalent into this repo's `contract/fixtures/companion-turns/`. The serde test suite asserts each fixture parses into the existing `ImGatewayEvent.CompanionTurn{Failed,Blocked,Timeout}` types without field drift.
2. **Bundles three small UX papercuts the user flagged on 2026-04-27**, all of which are localized to the chat surface and naturally land in the same client touchpoint as the typed-terminal verification work:
   - **Duplicate Regenerate affordance**: the latest companion bubble currently renders both an English-only "Regenerate" Text button (legacy S0 mvp single-turn regenerate, callback `onRegenerate` is no longer bound at the call site → the click does nothing) AND the bilingual "Regenerate from here / 从这里重新生成" Text button (wired to the §3.3 regenerate-at endpoint). Two buttons, same function, the dead one is English-only — which is jarring when the app language is Chinese.
   - **Settings entry hard to find from chat**: the only Settings entry is a `HeaderPill` buried in the Tavern tab body (line 192 of `TavernRoute.kt`). Users who entered chat from a deep-link or via a previously-saved conversation can't navigate to Settings without exiting the chat surface entirely.
   - **"Ready" status-line literal under tavern characters**: when a companion turn completes successfully and no provider/model badge is available, the status line falls through to the literal English string `"Ready"` (`ChatRoute.kt:1499`). On a tavern character bubble this reads as a "system status badge" — confusing, untranslated, and contributes nothing useful.

These three are too small to warrant their own slice and they touch exactly the surfaces the typed-terminal rendering work also touches (the chat top bar + the `ChatMessageRow` lifecycle presentation), so bundling them avoids touching the same files in two different commits.

## What Changes

### Backend wire-shape verification (paired with `companion-turn-backend-recovery-and-safety`)

- **Mirror three new event fixtures byte-equivalent** from the backend repo: `contract/fixtures/companion-turns/event-failed.json`, `event-blocked.json`, `event-timeout.json`.
- **Add three round-trip serde tests** in `data/remote/im/CompanionTurnEventSerializationTest.kt` (or extend the existing test) asserting each fixture deserializes into the existing `ImGatewayEvent.CompanionTurnFailed { turnId, conversationId, messageId, subtype, errorMessage, completedAt }`, `.CompanionTurnBlocked { turnId, conversationId, messageId, reason, completedAt }`, `.CompanionTurnTimeout { turnId, conversationId, messageId, elapsedMs, completedAt }`.
- **Verify the existing `MessageStatus.Failed | Blocked | Timeout` parser path** in `CompanionTurnRepository`'s gateway-event handler correctly converts each event into the matching `MessageStatus`. The handler code already exists; the verification is end-to-end.

### Three UX papercuts

- **Remove the legacy English-only "Regenerate" affordance** on the most-recent companion bubble (`ChatRoute.kt:1152-1161`). The bilingual "Regenerate from here / 从这里重新生成" entry handles the same case via the §3.3 regenerate-at endpoint — no functionality is lost. The `showRegenerate` boolean on `CompanionLifecyclePresentation` is RETAINED (existing presentation tests assert it's false on Failed/Blocked/Timeout bubbles, which is independent of the UI render); only the rendering of that boolean is removed. The `onRegenerate: () -> Unit` callback parameter on `ChatMessageRow` is dropped since no caller binds it (it was dead since chat-tree shipped).
- **Add a "Settings / 设置" entry to the chat top-bar overflow dropdown** (`ChatRoute.kt::ChatTopBar`, after the existing "Export chat / 导出对话" entry). Tapping it pops the chat back to the messages tab and navigates to the `"settings"` route — consistent with the way the Tavern tab's HeaderPill jumps to settings. The bilingual copy reuses `appLanguage.pick("Settings", "设置")`.
- **Drop the "Ready" status-line literal** on completed companion bubbles (`ChatRoute.kt:1499`). The replacement: when `modelBadge` is non-null, render `"Model · $modelBadge"` (unchanged); when null, render `null` (the status-line row hides when the value is null). The §6 `MessageStatus.Completed` lifecycle row's `statusLine` becomes `modelBadge?.let { "Model · $it" }`. No fallback string.

### Spec deltas

- **Modify the `core/im-app` capability spec's "Companion bubbles render typed terminals..." Requirement** to add three Scenarios pinning the chat-surface contract: (a) only one Regenerate affordance per companion bubble, bilingual; (b) Settings reachable from the chat top-bar overflow when the active conversation is a companion conversation; (c) Completed bubbles show `Model · <badge>` when a badge is available and hide the status-line row when not.
- **Modify the `llm-text-companion-chat` capability spec's "Wire shape envelope..." Requirement** to add three Scenarios pinning the new event-type discriminators (`companion_turn.failed | .blocked | .timeout`) parse into the matching `ImGatewayEvent` variants without field drift, citing the three new contract fixtures as the wire-shape source of truth.

## Impact

- Affected code:
  - `contract/fixtures/companion-turns/event-{failed,blocked,timeout}.json` — three new fixtures (mirrored from backend).
  - `android/app/src/test/java/com/gkim/im/android/data/remote/im/CompanionTurnEventSerializationTest.kt` (or equivalent) — three new round-trip assertions.
  - `android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt` — three small surgical edits (delete the dead "Regenerate" Text + `onRegenerate` parameter; add the Settings overflow item; drop the "Ready" fallback).
  - `android/app/src/test/java/com/gkim/im/android/feature/chat/ChatRowSnapshotTest.kt` (or instrumentation) — adjust assertions for the dropped UI element if any test was pinning the literal "Regenerate" text.
- Spec deltas: 1 MODIFIED Requirement on `core/im-app` (3 Scenarios added) + 1 MODIFIED Requirement on `llm-text-companion-chat` (3 Scenarios added).
- Backwards compatibility:
  - Older backend (S2 / current prod): no behavior change. Existing `Started`/`Delta`/`Completed` parsing path is untouched. Older backends never emit `failed`/`blocked`/`timeout` events, so the new test paths are no-ops at runtime.
  - Older client (pre-this-slice): renders the duplicate Regenerate + the "Ready" badge + no chat-overflow Settings as before. No wire-format change forces an upgrade.
- Risks:
  - One existing instrumentation test may pin the literal `"Regenerate"` text on the bubble; we'll adjust that to assert against the bilingual "Regenerate from here / 从这里重新生成" instead.
  - The chat top-bar overflow currently has only one item (Export chat); adding a second item may reflow the dropdown layout — visual smoke needed on the emulator.
- Paired backend slice: `companion-turn-backend-recovery-and-safety` (in GKIM-Backend repo) — must land first so the live wire shape exists.
