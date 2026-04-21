## 1. Contract foundations

- [ ] 1.1 Add `android/app/src/main/java/com/gkim/im/android/core/model/Lorebook.kt` carrying `id`, `ownerId`, `displayName: LocalizedText`, `description: LocalizedText`, `isGlobal`, `isBuiltIn`, `tokenBudget`, `extensions: JsonObject`, `createdAt`, `updatedAt`. Verification: `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon :app:compileDebugKotlin :app:testDebugUnitTest` stays green; `LorebookModelTest` covers copy / equality / JSON round-trip (Kotlinx Serialization).

- [ ] 1.2 Add `android/app/src/main/java/com/gkim/im/android/core/model/LorebookEntry.kt` carrying `id`, `lorebookId`, `name: LocalizedText`, `keysByLang: Map<AppLanguage, List<String>>`, `secondaryKeysByLang: Map<AppLanguage, List<String>>`, `secondaryGate: SecondaryGate`, `content: LocalizedText`, `enabled`, `constant`, `caseSensitive`, `scanDepth`, `insertionOrder`, `comment`, `extensions: JsonObject`. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.core.model.LorebookEntryTest` covers JSON round-trip for each `SecondaryGate` variant and for empty/populated per-language key lists.

- [ ] 1.3 Add `android/app/src/main/java/com/gkim/im/android/core/model/LorebookBinding.kt` tying a `lorebookId` to a `characterId` with a `isPrimary: Boolean` flag used at export time. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.core.model.LorebookBindingTest` covers serialization and the primary flag semantics.

- [ ] 1.4 Extend `android/app/src/main/java/com/gkim/im/android/data/remote/im/ImBackendModels.kt` with DTOs for lorebook CRUD and binding CRUD, plus the bootstrap response extension that carries the user's lorebook summary for the Settings entry's "X lorebooks" badge. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendPayloadsTest` covers every new payload variant.

## 2. Lorebook repository and API wiring

- [ ] 2.1 Add `android/app/src/main/java/com/gkim/im/android/data/remote/im/ImWorldInfoClient.kt` with `list()`, `create(request)`, `update(id, request)`, `delete(id)`, `duplicate(id)`, `listEntries(lorebookId)`, entry CRUD, `listBindings(lorebookId)`, `bind(lorebookId, characterId, isPrimary)`, `unbind(lorebookId, characterId)`. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImWorldInfoClientTest` covers happy-path and error-path handling with MockWebServer.

- [ ] 2.2 Add `android/app/src/main/java/com/gkim/im/android/data/repository/WorldInfoRepository.kt` with `StateFlow<List<Lorebook>>`, `StateFlow<Map<LorebookId, List<LorebookEntry>>>`, and CRUD methods that optimistically apply the mutation and reconcile with the server response. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.WorldInfoRepositoryTest` covers optimistic CRUD + reconciliation + binding update propagation.

- [ ] 2.3 Wire `WorldInfoRepository` into the same DI graph as `PresetRepository` / `MemoryRepository`. Refresh on bootstrap and on authenticated login. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.RepositoryBootstrapTest` covers the load order.

## 3. Settings → Companion → World Info library UI

- [ ] 3.1 Add Settings entry under `Companion` (already defined in `companion-settings-and-safety-reframe`) labeled "World Info" that routes to `feature/settings/worldinfo/WorldInfoLibraryRoute.kt`. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.SettingsMenuPresentationTest` asserts the new entry's presence, label, and route wiring.

- [ ] 3.2 Add `feature/settings/worldinfo/WorldInfoLibraryRoute.kt` that renders the user's lorebook list with per-row (name, entry count, `Global` badge), the primary Create CTA, per-row overflow (Duplicate, Delete disabled when bound, Toggle global), and row-tap routing into the editor. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.worldinfo.WorldInfoLibraryPresentationTest` covers each affordance + the disabled-Delete guard.

- [ ] 3.3 Add `feature/settings/worldinfo/WorldInfoEditorRoute.kt` that renders the lorebook header editor (displayName, description, tokenBudget, isGlobal), the entry list with move-up/down + enabled toggle + overflow, and the bindings sub-surface (list bound characters + bind picker + unbind). Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.worldinfo.WorldInfoEditorPresentationTest` covers header editing, entry reordering, and the bind/unbind flow.

- [ ] 3.4 Add `feature/settings/worldinfo/WorldInfoEntryEditor.kt` (modal or deep route) exposing the full entry field set: `name` bilingual, two per-language key tabs (en/zh) with inline add/remove, secondary keys + gate dropdown, bilingual content, constant/caseSensitive toggles, scanDepth/insertionOrder numbers, comment textarea. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.worldinfo.WorldInfoEntryEditorPresentationTest` covers each field and the per-language key tab isolation.

## 4. Character card-detail lorebook tab

- [ ] 4.1 Extend `feature/tavern/CharacterDetailRoute.kt` (or the equivalent card-detail surface) with a **Lorebook** tab that renders a read-only preview of every lorebook currently bound to this character. Each lorebook shows its displayName and entry count; tapping "Manage in library" routes to the editor. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.CharacterDetailLorebookTabTest` covers the tab's render and the deep-link into the editor.

- [ ] 4.2 When a character has zero bound lorebooks, the tab renders a zero-state with a "Bind a lorebook" CTA that opens a picker of the user's lorebooks. Verification: the same test covers the zero-state and the picker selection flow.

## 5. Backend contract alignment

- [ ] 5.1 Finalize `openspec/changes/world-info-binding/specs/im-backend/spec.md` to cover: lorebook CRUD endpoints, entry CRUD endpoints, binding CRUD endpoints, scan algorithm behavior, the allocator integration (placing `worldInfoEntries` between `userPersonaDescription` and `rollingSummary`), and the import/export round-trip with ST `character_book`. Verification: `openspec validate world-info-binding --strict` passes.

- [ ] 5.2 Cross-reference the allocator integration from design.md §4 in the `world-info-binding` new-capability spec (scan ordering) and in the `im-backend` delta (allocator clause), ensuring both name `userPersonaDescription` (above) and `rollingSummary` (below) as the neighboring sections in priority. Verification: `rg -n "userPersonaDescription" openspec/changes/world-info-binding/specs/` returns hits in both spec files.

- [ ] 5.3 Cross-reference the ST `character_book` round-trip contract in the new-capability spec and the `im-backend` delta so the canonical lossless slot name is unambiguous. Verification: `rg -n "character_book" openspec/changes/world-info-binding/specs/` returns hits in both spec files.

## 6. Import / export integration

- [ ] 6.1 Extend the character-card import preview (from `sillytavern-card-interop`) so an imported V2/V3 card carrying `character_book` renders a lorebook-import summary (entry count + token estimate + flag whether `constant` entries exist) alongside the existing preview fields. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.CardImportLorebookPreviewTest` covers the summary rendering.

- [ ] 6.2 Extend the character-card import commit path so a new `Lorebook` is created per imported `character_book`, its entries seeded from `character_book.entries`, the new lorebook bound to the imported character (with `isPrimary = true`), and un-modeled fields preserved under `entry.extensions.st.*`. Verification: instrumentation test `CardImportLorebookMaterializationInstrumentationTest` on `codex_api34` imports a fixture ST card with a `character_book`, walks through preview → commit, then asserts the lorebook exists in the library and the binding is present on the character detail surface.

- [ ] 6.3 Extend the character-card export endpoint so the character's primary-bound lorebook is emitted into `character_book` losslessly (modeled fields + `extensions.st.*`), monolingual key/content selected by the exporting user's primary-language choice with the other-language payload preserved under `entry.extensions.stTranslationAlt.*`. When multiple lorebooks are bound, non-primary bindings surface as a warning in the export response. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.CardExportLorebookRoundTripTest` covers: (a) export → re-import round-trip preserves entry set and `extensions.st.*`, (b) multiple-binding warning surfaces.

## 7. Runtime injection smoke check

- [ ] 7.1 Add a developer-only debug endpoint (gated on `BuildConfig.DEBUG` + dev-header) that accepts a candidate character id + a simulated scan text and returns the entries that would match, sorted by `insertionOrder`. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.WorldInfoDebugScanTest` covers the request/response shape.

- [ ] 7.2 Add an instrumentation smoke test on `codex_api34`: seed a lorebook with three entries (one `constant`, two keyword-gated with distinct keys), bind it to the active character, submit a turn whose body matches one of the keyword keys, and assert via the debug endpoint that exactly the `constant` entry plus the matched entry fire. Verification: `.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.tavern.WorldInfoRuntimeSmokeInstrumentationTest` passes.

## 8. Verification and delivery evidence

- [ ] 8.1 Add focused unit suites: `LorebookModelTest`, `LorebookEntryTest`, `LorebookBindingTest`, `ImWorldInfoClientTest`, `WorldInfoRepositoryTest`, `WorldInfoLibraryPresentationTest`, `WorldInfoEditorPresentationTest`, `WorldInfoEntryEditorPresentationTest`, `CharacterDetailLorebookTabTest`, `CardImportLorebookPreviewTest`, `CardExportLorebookRoundTripTest`, `WorldInfoDebugScanTest`. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest` fully green.

- [ ] 8.2 Add instrumentation coverage on `codex_api34`: `CardImportLorebookMaterializationInstrumentationTest` + `WorldInfoRuntimeSmokeInstrumentationTest`. Verification: `.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest` runs both suites green.

- [ ] 8.3 Record verification, review, score (≥95), and GitHub upload evidence in `docs/DELIVERY_WORKFLOW.md` for this slice following the template used by prior proposals. Include explicit pointers to the scan-algorithm table, the allocator slot, and the round-trip mapping with `character_book`. Verification: the delivery workflow entry exists with task rows 1.1 through 8.2 plus this recording task, and lists the `openspec archive` command + output.
