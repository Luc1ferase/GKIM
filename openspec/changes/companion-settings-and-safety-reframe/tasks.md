## 1. Contract foundations

- [x] 1.1 Add `android/app/src/main/java/com/gkim/im/android/core/model/BlockReason.kt` with the enum (`SelfHarm`, `Illegal`, `NsfwDenied`, `MinorSafety`, `ProviderRefusal`, `Other`) and a `wireKey: String` property, plus `BlockReason.fromWireKey(key)` that falls back to `Other` on unknown. Verification: `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon :app:compileDebugKotlin :app:testDebugUnitTest` stays green; `BlockReasonTest` covers wire-key round-trip and unknown fallback.

- [x] 1.2 Add `android/app/src/main/java/com/gkim/im/android/core/designsystem/BlockReasonCopy.kt` exposing `localizedCopy(reason: BlockReason, language: AppLanguage): LocalizedText` (or direct `String` resolution given an `AppLanguage`) backed by a bilingual table. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.core.designsystem.BlockReasonCopyTest` covers every enum value + both languages and rejects blank copy.

- [x] 1.3 Add `android/app/src/main/java/com/gkim/im/android/core/designsystem/SafetyCopy.kt` with bilingual tables for `Failed` subtypes (`Transient`, `PromptBudgetExceeded`, `AuthenticationFailed`, `ProviderUnavailable`, `NetworkError`, `Unknown`) and for `Timeout`. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.core.designsystem.SafetyCopyTest` covers every subtype + both languages and rejects blank copy.

- [x] 1.4 Extend `android/app/src/main/java/com/gkim/im/android/data/remote/im/ImBackendModels.kt` so `ImGatewayEvent.CompanionTurnBlocked` carries the wire-key reason and so the companion-turn failure event carries a typed subtype key (extending the failure payload from `llm-text-companion-chat`). Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendPayloadsTest` covers block-reason round-trip for every enum variant and failure-subtype round-trip.

- [x] 1.5 Finalize `openspec/changes/companion-settings-and-safety-reframe/specs/im-backend/spec.md` so the block-reason closed set, the failure-subtype closed set, and the content-policy acknowledgment endpoints are all captured as requirements with scenarios. Verification: `openspec validate companion-settings-and-safety-reframe --strict` passes.

## 2. Chat safety and failure bubbles

- [x] 2.1 Extend `feature/chat/ChatRoute.kt`'s `ChatMessageRow` so a `Blocked` terminal renders the `BlockReasonCopy.localizedCopy(reason, activeLanguage)` block and a "Compose a new message" CTA, with a small "Learn more" link to the content-policy surface. No retry affordance is rendered. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatBlockedBubbleTest` covers each reason rendering the correct copy, the absence of a retry affordance, and the "Compose new" action wiring.

- [x] 2.2 Extend `ChatMessageRow` so a `Failed` terminal renders per-subtype copy from `SafetyCopy`, with the correct action set per subtype: `Transient` → Retry; `PromptBudgetExceeded` / `AuthenticationFailed` → Edit user turn (no retry); `ProviderUnavailable` / `NetworkError` → Retry with a "check connection" hint; `Unknown` → generic Retry. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatFailedBubbleTest` covers each subtype's copy + action set.

- [x] 2.3 Extend `ChatMessageRow` so a `Timeout` terminal renders dedicated copy, a primary Retry-with-longer-wait affordance, and a secondary "Switch preset" hint when the active preset has `maxReplyTokens` above a heuristic cap. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatTimeoutBubbleTest` covers copy rendering, retry invocation pathway, and the conditional preset-hint visibility.

- [x] 2.4 Add instrumentation `ChatFailureAndSafetyBubbleInstrumentationTest` on `codex_api34` that injects a sequence of mocked `companion_turn.failed`, `companion_turn.blocked`, and `companion_turn.timeout` events through the realtime parser fake, and asserts each bubble renders the correct copy + correct actions. Verification: `.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.chat.ChatFailureAndSafetyBubbleInstrumentationTest` passes.

## 3. Settings reorganization

- [x] 3.1 Reorganize `feature/settings/SettingsRoute.kt`'s menu into the six sections: `Companion` (persona library + preset library + memory shortcut), `Appearance`, `Content & Safety`, `AIGC Image Provider` (renamed from `AiProvider` with clarifying caption), `Developer & Connection` (renamed from `ImValidation`, still gated behind `BuildConfig.DEBUG` where applicable), `Account`. Update all section labels + item descriptions to the new bilingual copy. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.SettingsMenuPresentationTest` asserts section order, labels, and the dev-gated visibility.

- [x] 3.2 Add the `Companion` section's memory-shortcut entry: tapping routes to a chooser listing recently active companions (with the current active companion at the top); selecting one opens the memory panel scoped to that companion (reusing the memory panel from `companion-memory-and-preset`). Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.CompanionMemoryShortcutTest` covers chooser population, active-companion ordering, and routing to the memory panel.

- [x] 3.3 Add the `Content & Safety` section's two items: a read-only "Acknowledgment status" row (showing acceptance date or "Not accepted — read policy") and a "Block reason verbosity" toggle (default on). Tapping "Read policy" opens the content-policy route. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.ContentAndSafetySectionTest` covers acknowledgment-state rendering, verbosity toggle persistence, and the policy-route navigation.

- [ ] 3.4 Rewrite the `AIGC Image Provider` section caption so users understand this section is scoped to image generation and not companion chat. No functional change to provider selection logic. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.AigcImageProviderSectionTest` covers the new caption and preserved provider selection behavior.

## 4. Content-policy acknowledgment

- [ ] 4.1 Add `android/app/src/main/java/com/gkim/im/android/feature/settings/ContentPolicyAcknowledgmentRoute.kt` backed by a `ContentPolicyAcknowledgmentViewModel`. The route renders the policy copy (bilingual placeholder, loaded from `core/designsystem/ContentPolicyCopy.kt` via `LocalizedText`), a scrollable body, and an "I accept" CTA. Accepting calls the backend acknowledgment endpoint with the current policy version and persists the state. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.ContentPolicyAcknowledgmentPresentationTest` covers copy render, accept flow, and error fallback.

- [ ] 4.2 Wire the bootstrap flow so the first successful post-login session fetches the acknowledgment state via `GET /api/account/content-policy-acknowledgment`; if the state is missing or the policy version has bumped, the app routes to `ContentPolicyAcknowledgmentRoute` before entering the tavern. Skippable on `BuildConfig.DEBUG`. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.bootstrap.BootstrapAcknowledgmentGatingTest` covers first-launch routing, subsequent-launch bypass, and debug-build skip.

- [ ] 4.3 Add instrumentation `ContentPolicyAcknowledgmentInstrumentationTest` on `codex_api34` covering: fresh install → bootstrap prompts acknowledgment → tap accept → app enters tavern; subsequent launch → bootstrap skips acknowledgment. Verification: `.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.settings.ContentPolicyAcknowledgmentInstrumentationTest` passes.

## 5. Backend contract alignment

- [ ] 5.1 Finalize `openspec/changes/companion-settings-and-safety-reframe/specs/im-backend/spec.md` to cover: block-reason closed set (wire keys `self_harm`, `illegal`, `nsfw_denied`, `minor_safety`, `provider_refusal`, `other`); failure-subtype closed set (`transient`, `prompt_budget_exceeded`, `authentication_failed`, `provider_unavailable`, `network_error`, `unknown`); acknowledgment endpoints (`POST /api/account/content-policy-acknowledgment`, `GET /api/account/content-policy-acknowledgment`) with version gating and per-account persistence. Verification: `openspec validate companion-settings-and-safety-reframe --strict` passes.

- [ ] 5.2 Document the failure-subtype conventions in design.md § "Per-terminal bubble copy + actions" — already present — and cross-reference them from the spec delta so the contract between backend and client is unambiguous. Verification: design.md references the subtype list and the spec delta cites the same keys.

## 6. Purpose stub cleanup (archive-time step)

- [ ] 6.1 As part of this slice's archival, manually update `openspec/specs/companion-character-card-depth/spec.md` Purpose section from "TBD - created by archiving change deepen-companion-character-card. Update Purpose after archive." to the exact text defined in design.md § "Companion-character-card-depth Purpose rewrite". Commit the update as part of the archival commit. Verification: `rg -n "TBD" openspec/specs/companion-character-card-depth/spec.md` returns no matches after the archival commit.

## 7. Verification and delivery evidence

- [ ] 7.1 Add focused unit suites: `BlockReasonTest`, `BlockReasonCopyTest`, `SafetyCopyTest`, `ChatBlockedBubbleTest`, `ChatFailedBubbleTest`, `ChatTimeoutBubbleTest`, `SettingsMenuPresentationTest`, `CompanionMemoryShortcutTest`, `ContentAndSafetySectionTest`, `AigcImageProviderSectionTest`, `ContentPolicyAcknowledgmentPresentationTest`, `BootstrapAcknowledgmentGatingTest`, plus `ImBackendPayloadsTest` coverage for block-reason + failure-subtype round-trip. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest` fully green.

- [ ] 7.2 Add instrumentation coverage on `codex_api34`: `ChatFailureAndSafetyBubbleInstrumentationTest` plus `ContentPolicyAcknowledgmentInstrumentationTest`. Verification: `.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest` runs both suites green.

- [ ] 7.3 Record verification, review, score (≥95), and GitHub upload evidence in `docs/DELIVERY_WORKFLOW.md` for this slice, following the template used by the previous proposals. Include explicit pointers to the block-reason wire-key table, the failure-subtype table, and the Purpose-stub cleanup evidence. Verification: the delivery workflow entry exists with task rows 1.1 through 7.2 plus this recording task, and lists the `openspec archive` command + output (which also performs the Purpose stub rewrite in 6.1).
