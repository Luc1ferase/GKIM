## 1. Contract foundations

- [ ] 1.1 Add `android/app/src/main/java/com/gkim/im/android/core/model/UserPersonaModels.kt` with `UserPersona` (`id`, `displayName: LocalizedText`, `description: LocalizedText`, `isBuiltIn`, `isActive`, `createdAt`, `updatedAt`, `extensions: JsonObject`). Include a `UserPersonaValidation` helper that rejects blank display names or blank descriptions on either language side. Verification: `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon :app:compileDebugKotlin :app:testDebugUnitTest` stays green; new equality + JSON round-trip tests pass.

- [ ] 1.2 Add `android/app/src/main/java/com/gkim/im/android/core/model/MacroSubstitution.kt` exposing `substituteMacros(template: String, userDisplayName: String, charDisplayName: String): String`. It MUST handle the six forms `{{user}}`, `{user}`, `<user>`, `{{char}}`, `{char}`, `<char>`, and leave unknown macros untouched. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.core.model.MacroSubstitutionTest` covers every form, case sensitivity, and "no substitution when names are empty" edge cases.

- [ ] 1.3 Extend `android/app/src/main/java/com/gkim/im/android/data/remote/im/ImBackendModels.kt` with DTOs: `UserPersonaDto`, `UserPersonaListDto`, `UserPersonaActivateRequestDto`. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendPayloadsTest` covers round-trip serialization including the `extensions` passthrough and the bilingual display name.

- [ ] 1.4 Extend `android/app/src/main/java/com/gkim/im/android/data/remote/im/ImBackendClient.kt` with `listPersonas(baseUrl, token)`, `createPersona(...)`, `updatePersona(...)`, `deletePersona(...)`, `activatePersona(baseUrl, token, personaId)`, and `getActivePersona(baseUrl, token)`. Implement in `ImBackendHttpClient` with default `error("not implemented")` stubs for backward-compat. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendHttpClientTest` covers success, 404, and 409 (delete-active blocked) paths for every new endpoint.

- [ ] 1.5 Finalize `openspec/changes/user-persona/specs/im-backend/spec.md` so persistence, CRUD, active-singleton enforcement, built-in seeding, macro substitution semantics, and the allocator integration slot are captured as requirements with scenarios. Verification: `openspec validate user-persona --strict` passes.

## 2. UserPersona repository (Android)

- [ ] 2.1 Add `android/app/src/main/java/com/gkim/im/android/data/repository/UserPersonaRepository.kt` interface exposing `observePersonas(): Flow<List<UserPersona>>`, `observeActivePersona(): Flow<UserPersona?>`, `create(persona)`, `update(persona)`, `delete(personaId)`, `activate(personaId)`, `duplicate(personaId)`, and `refresh()`. `DefaultUserPersonaRepository` enforces invariants: exactly one persona has `isActive=true`; the built-in persona cannot be deleted; duplicate produces a user-owned persona with bilingual `" (copy)"` suffix. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.UserPersonaRepositoryTest` covers activate exclusivity, delete-built-in blocked, delete-active blocked, duplicate renaming.

- [ ] 2.2 Add `android/app/src/main/java/com/gkim/im/android/data/repository/LiveUserPersonaRepository.kt` that binds the default repository to `ImBackendClient` endpoints. On cold start `refresh()` loads the library and active-persona in parallel and merges them. Optimistic local update is rolled back on 4xx/5xx. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.LiveUserPersonaRepositoryTest` asserts merge semantics, activate-from-list behavior, and rollback on 409.

- [ ] 2.3 Register `userPersonaRepository` in `AppContainer` + `DefaultAppContainer` and expose it to the chat + settings view models that need active-persona state. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.*Test` stays green; `rg -n "userPersonaRepository" android/app/src/main/java` confirms wiring.

## 3. Android Settings: persona library

- [ ] 3.1 Add a "Personas" section to Settings (in the existing Settings route or a sibling route) listing every persona from `UserPersonaRepository.observePersonas()`, showing the active persona with a visible badge, and exposing entry points for "New persona", "Duplicate", "Edit", "Activate", and "Delete" (disabled for built-ins and the currently active persona). Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.PersonaListPresentationTest` asserts list ordering, disabled actions, and active badge.

- [ ] 3.2 Add `android/app/src/main/java/com/gkim/im/android/feature/settings/PersonaEditorRoute.kt` backed by a `PersonaEditorViewModel` that edits bilingual `displayName` and bilingual `description`. Save calls `UserPersonaRepository.update`; cancel discards. Validation rejects blank fields on either language side, using `UserPersonaValidation`. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.PersonaEditorPresentationTest` covers validation, save success, cancel-discards-changes.

- [ ] 3.3 Add instrumentation `PersonaLibraryInstrumentationTest` on `codex_api34` covering: open Settings → Personas, see the built-in persona marked active, edit its description and confirm the change persists, create a new user-owned persona, activate it, observe the active badge moves, attempt to delete the active persona and observe the action disabled, delete an inactive user persona. Verification: `.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.settings.PersonaLibraryInstrumentationTest` passes.

## 4. Chat surface integration

- [ ] 4.1 Extend the companion chat chrome with an active-persona pill next to the active-preset pill. Tapping the persona pill routes to Settings → Personas. The pill's label resolves to the active persona's `displayName` in the active `AppLanguage`. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatChromePersonaPillTest` covers label selection, tap routing, and update on active-persona change.

- [ ] 4.2 Wire the greeting picker and any chat preview renderer that shows macros to use `MacroSubstitution.substituteMacros` with the active persona's display name and the active companion card's display name. The stored greeting body is NOT rewritten; only the rendered preview is substituted. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.GreetingPickerMacroSubstitutionTest` asserts preview text reflects substitution while the stored variant body remains raw.

- [ ] 4.3 Add a subtle footer line under the chrome pills showing "Talking as {personaName}" in the active `AppLanguage`, with typography that does not compete with the conversation content. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatChromePersonaFooterTest` asserts rendering and accessibility semantics (content description).

## 5. Backend contract alignment

- [ ] 5.1 Finalize `openspec/changes/user-persona/specs/im-backend/spec.md` to cover: HTTP endpoints for persona library CRUD + activate + active getter, built-in persona seeding on first bootstrap, active-singleton enforcement, macro substitution rules (`{{user}}`/`{user}`/`<user>` → active persona display name; `{{char}}`/`{char}`/`<char>` → active companion display name), and the persona-description section in the token-budget allocator with its priority and drop position. Verification: `openspec validate user-persona --strict` passes.

- [ ] 5.2 Document in design.md § "Persona description injection in the token-budget allocator" the exact priority slot and drop step inserted into the `companion-memory-and-preset` allocator. Also document the `MacroSubstitution` form list (six forms) so the backend and the Android client stay in sync. Verification: design.md references both sections and the spec delta for `companion-memory-and-preset` reflects the added section.

## 6. Verification and delivery evidence

- [ ] 6.1 Add focused unit suites: `UserPersonaModelsTest`, `MacroSubstitutionTest`, `UserPersonaRepositoryTest`, `LiveUserPersonaRepositoryTest`, `PersonaListPresentationTest`, `PersonaEditorPresentationTest`, `ChatChromePersonaPillTest`, `GreetingPickerMacroSubstitutionTest`, `ChatChromePersonaFooterTest`, plus `ImBackendPayloadsTest` (persona DTOs) and `ImBackendHttpClientTest` (new endpoints). Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest` fully green.

- [ ] 6.2 Add instrumentation coverage on `codex_api34`: `PersonaLibraryInstrumentationTest` plus a `PersonaIntegrationChatTest` that switches the active persona mid-session and confirms the next turn reflects the new display name in chrome + footer + greeting preview. Verification: `.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest` runs both suites green.

- [ ] 6.3 Record verification, review, score (≥95), and GitHub upload evidence in `docs/DELIVERY_WORKFLOW.md` for this slice, following the template used by `llm-text-companion-chat` and `companion-memory-and-preset`. Include explicit pointers to the new spec delta files and the macro-form table in design.md. Verification: the delivery workflow entry exists with task rows 1.1 through 6.2 plus this recording task, and lists the `openspec archive` command + output.
