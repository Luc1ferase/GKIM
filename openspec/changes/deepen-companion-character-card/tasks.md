## 1. Domain model depth

- [ ] 1.1 Expand `CompanionCharacterCard` in `android/app/src/main/java/com/gkim/im/android/core/model/CompanionModels.kt` with `systemPrompt`, `personality`, `scenario`, `exampleDialogue`, `firstMes` (replacing `openingLine`), `alternateGreetings: List<LocalizedText>`, `tags: List<String>`, `creator`, `creatorNotes`, `characterVersion`, `avatarUri: String?`, `extensions: Map<String, kotlinx.serialization.json.JsonElement>`, and update `ResolvedCompanionCharacterCard` + `resolve()` to project the new fields. Verification: `./gradlew :app:compileDebugKotlin` passes and existing unit tests compile after field rename.

- [ ] 1.2 Update `android/app/src/main/java/com/gkim/im/android/data/repository/SeedData.kt` so every shipped preset and every drawable pool entry carries authored English+Chinese content for all new prose fields plus reasonable default `tags`, `creator`, `characterVersion`. Verification: snapshot-style unit test asserts every seeded card has non-empty bilingual `systemPrompt`, `scenario`, `personality`, `firstMes`.

## 2. Repository CRUD

- [ ] 2.1 Extend `CompanionRosterRepository` interface in `android/app/src/main/java/com/gkim/im/android/data/repository/CompanionRosterRepository.kt` with `upsertUserCharacter(card)` and `deleteUserCharacter(characterId)`. `DefaultCompanionRosterRepository` implements both, blocking mutation on `source == Preset` with an explicit failure. A new flow `userCharacters: StateFlow<List<CompanionCharacterCard>>` exposes cards created through `upsertUserCharacter`. Verification: unit tests cover create, update, delete, preset-immutability, and draw-acquired edit-but-not-delete behavior.

- [ ] 2.2 Update `android/app/src/main/java/com/gkim/im/android/data/repository/BackendAwareCompanionRosterRepository.kt` so CRUD operations either forward to the backend roster API (new endpoints are stubbed in this slice) or fall back to the in-memory default when the backend contract is unavailable. Document the fallback behavior with a TODO referencing the backend implementation slice. Verification: unit test that exercises fallback path when the backend adapter is not configured.

## 3. Tavern UI surfaces

- [ ] 3.1 Move `android/app/src/main/java/com/gkim/im/android/feature/space/SpaceRoute.kt` to `android/app/src/main/java/com/gkim/im/android/feature/tavern/TavernRoute.kt`, rename `SpaceRoute` to `TavernRoute` and its view model accordingly, update package and imports across call sites (`feature/navigation/GkimRootApp.kt`, tests) while keeping the navigation destination id `"space"` unchanged. Verification: `./gradlew :app:assembleDebug` succeeds; existing navigation tests still resolve the Tavern tab.

- [ ] 3.2 Add `android/app/src/main/java/com/gkim/im/android/feature/tavern/CharacterDetailRoute.kt` showing resolved card fields (header, summary, tags, creator, version, system prompt preview, scenario, personality, example dialogue, firstMes + alternateGreetings count). Non-preset cards expose an "Edit" action; all cards expose "Activate as current companion". Verification: Compose UI test asserts the detail route renders each bilingual field section and exposes the activation CTA.

- [ ] 3.3 Add `android/app/src/main/java/com/gkim/im/android/feature/tavern/CharacterEditorRoute.kt` supporting both create (from Tavern `+` action) and update (from detail "Edit") for non-preset cards. Form covers every new authored field with English/Chinese inputs, tag chip entry, avatar picker (SAF `OpenDocument`), Cancel + Save actions. Save calls `upsertUserCharacter`. Verification: Compose UI test drives create flow, asserts a new card appears in the tavern owned roster with the edited content.

- [ ] 3.4 Wire tavern card rows to route to detail (`tavern/detail/{id}`) and the `+` action to editor (`tavern/editor?mode=create` / `mode=edit&id=<id>`). Update `feature/navigation/GkimRootApp.kt` with the new composables; destinations are nested under the authenticated shell. Verification: instrumentation test taps a roster card, sees detail route, taps Edit, sees editor, saves, returns, sees updated label.

## 4. Backend contract alignment

- [ ] 4.1 Finalize the spec delta in `openspec/changes/deepen-companion-character-card/specs/im-backend/spec.md` so companion roster APIs expose every new field as bilingual JSON plus an `extensions` object, and active-selection/draw responses include the full deep card. Verification: spec review passes OpenSpec lint / review gate.

- [ ] 4.2 Record the backend migration intent (add per-language columns for the new prose fields; add JSONB `extensions`; backfill preset rows from shipped Android seed content) in this slice's design/spec without committing Rust source. Verification: maintainer handoff note in the delivery evidence points to the private backend migration PR.

## 5. Verification and delivery evidence

- [ ] 5.1 Update `android/app/src/test/java/com/gkim/im/android/data/repository/RepositoriesTest.kt` and add new tests covering: deep field resolution, preset immutability on upsert/delete, user-created card lifecycle, backend fallback behavior. Verification: `./gradlew :app:testDebugUnitTest` passes.

- [ ] 5.2 Add Compose UI tests under `android/app/src/androidTest/java/com/gkim/im/android/feature/tavern/` for TavernRoute rename, detail rendering, editor create/update round-trip. Verification: instrumentation tests pass on the same emulator profile used by `pivot-to-ai-companion-im` validation.

- [ ] 5.3 Record verification, review, score (≥95), and evidence in `docs/DELIVERY_WORKFLOW.md` for this slice. Verification: delivery workflow entry present and linked from the change folder.
