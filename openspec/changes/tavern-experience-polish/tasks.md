## 1. Portrait large-view

- [ ] 1.1 Add `android/app/src/main/java/com/gkim/im/android/feature/tavern/PortraitLargeViewRoute.kt` with a full-screen composable backed by a `PortraitLargeViewViewModel`. Pinch-to-zoom + pan + double-tap-to-toggle-zoom. Fallback: avatar-less cards render a placeholder with the card's display name and an "Edit card" shortcut. Verification: `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon :app:compileDebugKotlin :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.PortraitLargeViewPresentationTest` covers render + zoom + fallback.

- [ ] 1.2 Wire avatar taps in tavern card rows, chat header, and chat bubble avatars to route to `PortraitLargeViewRoute` with the active card id. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.PortraitTapRoutingTest` covers the three surfaces.

## 2. Alt-greeting picker refinement

- [ ] 2.1 Extend the existing opener picker (introduced by `llm-text-companion-chat`) so each option renders a ~120-character localized preview and supports tap-to-preview the full greeting in a modal. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.AltGreetingPickerPresentationTest` covers preview rendering, modal invocation, and selection commit.

- [ ] 2.2 Persist the last-selected alt-greeting per companion; on subsequent opener renders (after relationship reset or on a fresh conversation), default the selection highlight to that greeting with a "Remembered from last time" caption. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.AltGreetingRememberedDefaultTest` covers the persistence + default-select behavior.

## 3. Chat branch tree navigation

- [ ] 3.1 Extend `feature/chat/ChatMessageRow.kt` so every companion bubble renders left/right swipe chevrons when its `variantGroupId`'s sibling count > 1, along with a `(n / total)` caption. Tapping a chevron mutates the conversation's `activePath` for that `variantGroupId` and the UI re-resolves the path. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatBranchChevronsTest` covers chevron visibility, caption, and active-path mutation.

- [ ] 3.2 Add an "Edit" overflow action on every **user** bubble that opens an edit sheet prefilled with the bubble's content. Submitting creates a new sibling under the same `parentMessageId` through `POST /api/companion-turns/{conversationId}/edit`; the new user-branch + its companion-turn become the active path. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatEditUserBubbleTest` covers edit-sheet prefill, backend call shape, and active-path switch.

- [ ] 3.3 Add a "Regenerate from here" overflow action on every **companion** bubble (not just the latest). Invoking the action calls the extended `POST /api/companion-turns/{conversationId}/regenerate` with `{ targetMessageId }`, producing a new sibling under the same `variantGroupId`; the UI switches the active path to the new sibling. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatRegenerateFromHereTest` covers the endpoint call shape for a mid-conversation bubble and the sibling creation + path switch.

- [ ] 3.4 Add instrumentation `ChatBranchNavigationInstrumentationTest` on `codex_api34` that seeds a conversation with 3 companion turns, edits turn 1's user message, regenerates turn 2, and asserts sibling navigation produces the expected 4-branch tree with each branch independently reachable. Verification: `.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.chat.ChatBranchNavigationInstrumentationTest` passes.

## 4. Per-character provider / preset override

- [x] 4.1 Extend `android/app/src/main/java/com/gkim/im/android/core/model/CompanionCharacterCard.kt` with `characterPresetId: String?` alongside the existing persona fields; map through the `sillytavern-card-interop` round-trip by preserving the field under `extensions.st.charPresetId`. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.core.model.CharacterCardPresetOverrideTest` covers round-trip and default-null behavior.

- [ ] 4.2 Extend the character-detail editor with an "Override preset" row that lets the user pick a preset from the library (or clear to default). Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.CharacterDetailPresetOverrideTest` covers picker invocation, selection persistence, and clear-to-default.

- [ ] 4.3 Update the chat chrome's preset pill so it surfaces the override (visually distinct "(card override)" suffix) when `characterPresetId` is non-null, and tapping routes to the character's detail surface where the override can be cleared. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatPresetPillOverrideTest` covers rendered label + route target.

## 5. JSONL chat export

- [ ] 5.1 Add `feature/chat/ChatExportDialog.kt` with a dialog offering active-path-only vs. full-tree toggle, target-language selector defaulted to active `AppLanguage`, and share-sheet vs. Downloads target. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatExportDialogPresentationTest` covers each control.

- [ ] 5.2 Wire the dialog to call `GET /api/conversations/{conversationId}/export?format=jsonl&pathOnly=...` and route the returned payload to the chosen target (share sheet or `DownloadManager`); filename default includes a `_<first8OfConversationId>` suffix for disambiguation. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatExportRoutingTest` covers both targets and the filename shape.

## 6. Full relationship reset

- [ ] 6.1 Add a "Reset relationship" affordance on the character-detail surface behind a two-step confirmation dialog. Confirming calls `POST /api/relationships/{characterId}/reset` which clears all conversations, the memory record, and the last-selected alt-greeting for the user-companion pair; the card itself is not deleted. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.RelationshipResetAffordanceTest` covers the two-step gate, the endpoint call shape, and the post-reset state (empty conversations + empty memory + cleared last-greeting).

- [ ] 6.2 Add instrumentation `RelationshipResetInstrumentationTest` on `codex_api34` that seeds a companion with 2 conversations + memory + pinned facts, invokes relationship reset, and asserts the tavern surface shows zero conversations for the companion and memory is empty. Verification: `.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.tavern.RelationshipResetInstrumentationTest` passes.

## 7. Gacha probability + duplicate-handling animation

- [ ] 7.1 Extend the gacha pre-draw UI (from `replace-space-with-character-roster-and-gacha`) to surface the rarity / probability breakdown, drawn from the existing backend catalog response. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.GachaProbabilitySurfacingTest` covers breakdown rendering with several fixture probability distributions.

- [ ] 7.2 Extend the gacha result animation so a drawn card whose id already appears in the user's owned roster branches into the "Already owned" variant; the variant renders a "Keep as bonus" CTA that records a `bonusAwarded` event. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.GachaDuplicateAnimationTest` covers duplicate detection, variant-animation selection, and the bonus-event recording call.

## 8. Creator attribution sub-surface

- [ ] 8.1 Extend `feature/tavern/CharacterDetailRoute.kt` with an "About this card" sub-section rendering `creator`, `creatorNotes`, `characterVersion`, linkified `stSource` from `extensions.st.stSource`, and formatted `stCreationDate` / `stModificationDate`. Missing fields are hidden. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.CharacterDetailCreatorSubSectionTest` covers populated fields, missing fields (not rendered), and `stSource` link tap routing to the system browser.

## 9. Backend contract alignment

- [ ] 9.1 Finalize `openspec/changes/tavern-experience-polish/specs/im-backend/spec.md` to cover: edit-user-turn endpoint (`POST /api/companion-turns/{conversationId}/edit`), arbitrary-layer regenerate extension (`POST /api/companion-turns/{conversationId}/regenerate` with `{ targetMessageId }`), relationship-reset endpoint (`POST /api/relationships/{characterId}/reset`), JSONL export endpoint (`GET /api/conversations/{conversationId}/export?format=jsonl&pathOnly=...`), `characterPresetId` card-record field, and the gacha duplicate / bonusAwarded contract. Verification: `openspec validate tavern-experience-polish --strict` passes.

- [ ] 9.2 Finalize `openspec/changes/tavern-experience-polish/specs/core/im-app/spec.md` to cover all eight Android-side polish items (portrait view, alt-greeting picker refinement, branch-tree navigation, per-character override, JSONL export, relationship reset, gacha surfacing, creator attribution). Verification: `openspec validate tavern-experience-polish --strict` passes.

## 10. Verification and delivery evidence

- [ ] 10.1 Add focused unit suites as named in each section (e.g., `PortraitLargeViewPresentationTest`, `PortraitTapRoutingTest`, `AltGreetingPickerPresentationTest`, `AltGreetingRememberedDefaultTest`, `ChatBranchChevronsTest`, `ChatEditUserBubbleTest`, `ChatRegenerateFromHereTest`, `CharacterCardPresetOverrideTest`, `CharacterDetailPresetOverrideTest`, `ChatPresetPillOverrideTest`, `ChatExportDialogPresentationTest`, `ChatExportRoutingTest`, `RelationshipResetAffordanceTest`, `GachaProbabilitySurfacingTest`, `GachaDuplicateAnimationTest`, `CharacterDetailCreatorSubSectionTest`). Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest` fully green.

- [ ] 10.2 Add instrumentation coverage on `codex_api34`: `ChatBranchNavigationInstrumentationTest`, `RelationshipResetInstrumentationTest`. Verification: `.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest` runs both suites green.

- [ ] 10.3 Record verification, review, score (≥95), and GitHub upload evidence in `docs/DELIVERY_WORKFLOW.md` for this slice following prior slices' template. Include explicit pointers to each of the eight items, the branch-tree contract extensions, and the JSONL export format description. Verification: the delivery workflow entry exists with task rows 1.1 through 10.2 plus this recording task, and lists the `openspec archive` command + output.
