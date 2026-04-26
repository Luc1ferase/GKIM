# Tasks — companion-turn-character-prompt-context

## §1 — OpenSpec scaffold

- [x] §1.1 — `proposal.md` / `tasks.md` / `specs/llm-text-companion-chat/spec.md` delta committed on branch `feature/companion-turn-character-prompt-context` off `feature/ai-companion-im` (HEAD `30eddaf`). Scaffold commit `6edc786`, pushed to `origin/feature/companion-turn-character-prompt-context`. `openspec validate companion-turn-character-prompt-context --strict` returns valid. Paired backend slice `companion-turn-backend-llm-bridge` lives at `669c927` on `origin/feature/companion-turn-backend-llm-bridge` in `GKIM-Backend`.

## §2 — DTO surface

- [ ] §2.1 — Add `@Serializable data class CharacterPromptContextDto(systemPrompt: String, personality: String, scenario: String, exampleDialogue: String, userPersonaName: String, companionDisplayName: String)` to `data/remote/im/ImBackendModels.kt` immediately above `CompanionTurnSubmitRequestDto`. No mapper to a domain type — the DTO carries already-resolved strings only. Verification: compiles; `kotlinx.serialization.json.Json.decodeFromString<CharacterPromptContextDto>` round-trips a hand-typed JSON literal containing the six fields.

- [ ] §2.2 — Extend `CompanionTurnSubmitRequestDto`, `EditUserTurnRequestDto`, `RegenerateAtRequestDto` each with `val characterPromptContext: CharacterPromptContextDto? = null` as the last constructor parameter. The default `null` keeps every existing call-site source-compatible. Verification: `:app:compileDebugKotlin` succeeds with no callsite changes; old serialized payloads (without the field) still deserialize because `null` is the default.

- [ ] §2.3 — Add `contract/fixtures/companion-turns/submit-request-with-character-context.json` and `submit-request-without-character-context.json` byte-mirroring the paired backend repo's fixtures. Add a new `feature` test `data/remote/im/CompanionTurnSubmitRequestDtoSerializationTest.kt` that round-trips both fixtures through the Kotlin DTO via the project's `Json` instance and asserts (a) field-present fixture deserializes with all six `CharacterPromptContextDto` fields populated, (b) field-absent fixture deserializes with `characterPromptContext == null`, (c) re-serializing the field-absent DTO produces JSON without the `characterPromptContext` key (kotlinx.serialization's encode-default behavior; verify whichever output our `Json` instance produces). Verification: `:app:testDebugUnitTest --tests *CompanionTurnSubmitRequestDtoSerializationTest*` green; `git diff --no-index contract/fixtures/companion-turns/ ../GKIM-Backend/contract/fixtures/companion-turns/` empty for the two new files.

## §3 — Resolution helper

- [ ] §3.1 — New file `feature/chat/CharacterPromptContextResolver.kt` exposing `internal fun resolveCharacterPromptContext(card: CompanionCharacterCard?, persona: UserPersona?, language: AppLanguage): CharacterPromptContextDto?` and a private `defaultPersonaName(language: AppLanguage): String` (returns `"User"` for English, `"用户"` for Chinese, matching the convention used elsewhere). The helper:
  - Returns `null` when `card == null` so callers can bail before the wire call (matches `ChatViewModel.sendMessage`'s existing companion-only gate).
  - Otherwise computes `resolved = card.resolve(language)` and packs `systemPrompt = resolved.systemPrompt`, `personality = resolved.personality`, `scenario = resolved.scenario`, `exampleDialogue = resolved.exampleDialogue`, `userPersonaName = persona?.displayName?.resolve(language) ?: defaultPersonaName(language)`, `companionDisplayName = resolved.displayName`.
  - Does NOT macro-substitute. The backend prompt-assembly module is the single substitution point per the paired backend slice's `design.md` §2.

  Verification: new `feature/chat/CharacterPromptContextResolverTest.kt` exercises (a) `card == null → null`, (b) full card + active persona → all six fields populated and macros un-substituted (assert `"{{user}}"` survives in the output when present in `systemPrompt`), (c) full card + `persona == null` → `userPersonaName == "User"` for `AppLanguage.English` and `"用户"` for `AppLanguage.Chinese`, (d) language switch swaps which `LocalizedText` branch is picked. 5 tests minimum.

## §4 — Repository surface

- [ ] §4.1 — Extend `CompanionTurnRepository` interface methods with the new optional parameter:
  - `suspend fun submitUserTurn(conversationId: String, activeCompanionId: String, userTurnBody: String, activeLanguage: String, parentMessageId: String? = null, characterPromptContext: CharacterPromptContextDto? = null): Result<CompanionTurnRecordDto>`
  - `suspend fun editUserTurn(conversationId: String, parentMessageId: String, newUserText: String, activeCompanionId: String, activeLanguage: String, characterPromptContext: CharacterPromptContextDto? = null): Result<EditUserTurnResponseDto>`
  - `suspend fun regenerateCompanionTurnAtTarget(conversationId: String, targetMessageId: String, characterPromptContext: CharacterPromptContextDto? = null): Result<CompanionTurnRecordDto>`

  All three default to `null`, preserving every existing internal call-site. `DefaultCompanionTurnRepository` accepts the parameter and ignores it (no behavior change). Verification: `:app:compileDebugKotlin` succeeds with no other code changes; existing `LiveCompanionTurnRepository*Test.kt` continues to compile.

- [ ] §4.2 — `LiveCompanionTurnRepository.submitUserTurn`, `.editUserTurn`, `.regenerateCompanionTurnAtTarget` each thread the new parameter into the corresponding outbound DTO (`CompanionTurnSubmitRequestDto`, `EditUserTurnRequestDto`, `RegenerateAtRequestDto`). The retry path in `retrySubmitUserTurn` carries the captured `characterPromptContext` from the original `FailedCompanionSubmission` so the retry sees the same context the original send carried — extend `FailedCompanionSubmission` with a `characterPromptContext: CharacterPromptContextDto? = null` field. Verification: extend `LiveCompanionTurnRepositorySubmitTest`, `LiveCompanionTurnRepositoryEditUserTurnTest`, `LiveCompanionTurnRepositoryRegenerateAtTest` each with one test that supplies a non-null `CharacterPromptContextDto` and asserts (via the existing fake `ImBackendClient`'s captured-request assertion) that the outbound DTO carries the same six field values; one test per method that supplies `null` and asserts the outbound DTO carries `null`.

## §5 — Call-site wiring

- [ ] §5.1 — `ChatViewModel.sendMessage` resolves the active companion card via `currentConversationSnapshot()?.companionCardId?.let { container.companionRosterRepository.characterById(it) }` and the active persona via the user-persona repository (already in scope through `AppContainer`); calls `resolveCharacterPromptContext(card, persona, activeLanguage)`; passes the result into `companionTurnRepository.submitUserTurn(..., characterPromptContext = ctx)`. Verification: extend the existing `ChatViewModel` unit test suite with one test that asserts `submitUserTurn` is called with a non-null `characterPromptContext` whose six fields equal `card.resolve(language)`'s + `persona.displayName.resolve(language)`'s projections.

- [ ] §5.2 — Same wire-up in `ChatViewModel.editUserTurn` and `ChatViewModel.regenerateFromHere`. Both already have `companionId` and the conversation snapshot in scope; both gain the same one-line `resolveCharacterPromptContext(...)` call before the repository invocation. Verification: extend the corresponding unit tests with the same equality assertion as §5.1.

## §6 — Verification + archive

- [ ] §6.1 — Verification roll-up: §2-§5 evidence rows recorded in `docs/DELIVERY_WORKFLOW.md` (companion-turn-character-prompt-context section). All unit tests green; the new `CompanionTurnSubmitRequestDtoSerializationTest` (3 tests), `CharacterPromptContextResolverTest` (5 tests), and the three extended repository tests (6 new tests) all pass. Confirm the paired backend slice has been merged or is in-flight; if backend has not yet shipped, the client change is harmless on the wire (forward-compatible; the deployed S1 backend ignores unknown fields).

- [ ] §6.2 — Archive — move `openspec/changes/companion-turn-character-prompt-context` → `openspec/changes/archive/<YYYY-MM-DD>-companion-turn-character-prompt-context/`, apply the spec delta to `openspec/specs/llm-text-companion-chat/spec.md`. Verification: `openspec validate companion-turn-character-prompt-context --strict` passes pre-archive; `openspec archive companion-turn-character-prompt-context --yes` succeeds; final spec file carries the new requirement text under `llm-text-companion-chat`.
