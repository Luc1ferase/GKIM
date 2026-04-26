# Pass resolved character prompt context with every companion-turn submit / edit / regenerate-at

## Why

The sibling backend repo's S2 slice `companion-turn-backend-llm-bridge` (proposed 2026-04-27) replaces S1's `CannedTextProvider` (which streams a fixed bilingual greeting `"Hello, traveler. I am the Daylight Listener. 你好,旅行者。"` regardless of input) with a real OpenAI-compatible streaming bridge plus a single-language prompt-assembly pipeline. To produce a coherent in-character reply the bridge needs the resolved system prompt, persona, scenario, and example-dialogue blocks for the active companion card. The S4 slice (`companion-backend-persona-memory-preset`) will eventually move card storage to the backend, but until then the backend has no copy of these fields — its `companion_character_roster` table only carries display-name level metadata.

The client already resolves these fields in the active language for greeting macro substitution (`docs/DELIVERY_WORKFLOW.md` task 1725 — `applyPersonaMacros(resolveCompanionGreetings(card, language), activePersona.displayName.resolve(language), card?.resolve(language)?.name.orEmpty())`). The same `ResolvedCompanionCharacterCard.systemPrompt` / `personality` / `scenario` / `exampleDialogue` fields and the active persona's display name are already in scope at `ChatViewModel.sendMessage` / `editUserTurn` / `regenerateFromHere`. This slice forwards those values as a new optional `characterPromptContext` field on the existing companion-turn request DTOs so the paired backend slice can assemble a coherent system message.

This is a passthrough change — the client doesn't change behavior visibly. Without it, the paired S2 backend falls back to a degraded bland system message that names the companion display name only. With it, the backend gets the full character voice and the user sees in-character replies. The two repos ship as a paired PR pair so neither side regresses.

## What Changes

- **New shared DTO**: `CharacterPromptContextDto` in `data/remote/im/ImBackendModels.kt` carrying six `String` fields — `systemPrompt`, `personality`, `scenario`, `exampleDialogue`, `userPersonaName`, `companionDisplayName`. All `@Serializable` with the implicit camelCase keys the existing wire model uses. No mapping helper to a domain type — the DTO exists to ferry already-resolved strings across the wire.

- **Three request DTOs gain the optional field**: `CompanionTurnSubmitRequestDto`, `EditUserTurnRequestDto`, `RegenerateAtRequestDto` each gain `characterPromptContext: CharacterPromptContextDto? = null`. The default is `null` so existing call sites compile unchanged; old serialized payloads remain valid; the field is optional on the wire.

- **Resolution helper**: new pure function `resolveCharacterPromptContext(card: CompanionCharacterCard?, persona: UserPersona?, language: AppLanguage): CharacterPromptContextDto?` in a new file `feature/chat/CharacterPromptContextResolver.kt`. Returns `null` when `card` is `null` (the same gate `ChatViewModel.sendMessage` already uses to decide whether to dispatch through `companionTurnRepository` at all). Otherwise resolves all four `LocalizedText` fields via the existing `card.resolve(language)` projection, picks the active persona's display name (falling back to the literal `"User"` / `"用户"` when `persona == null`), and packs the result. Macros (`{{user}}`, `{{char}}`) are NOT pre-substituted on the client — the backend's prompt-assembly module is the single substitution point, mirroring the design decision recorded in the backend slice's `design.md` §2.

- **Wire-up at three call sites**: `ChatViewModel.sendMessage` / `.editUserTurn` / `.regenerateFromHere` each resolve the active companion card and active persona (both already in scope via `currentConversationSnapshot()` + `personaRepository.activePersona`), call `resolveCharacterPromptContext`, and pass the resulting `CharacterPromptContextDto?` into the corresponding `LiveCompanionTurnRepository` method. The repository methods gain an extra `characterPromptContext: CharacterPromptContextDto? = null` parameter, threaded through to the DTO carried by the underlying `ImBackendClient` HTTP call. Default-`null` keeps the parameter list backward-compatible at every internal call site.

- **Repository wire path**: `LiveCompanionTurnRepository.submitUserTurn`, `.editUserTurn`, and `.regenerateCompanionTurnAtTarget` each accept the new optional parameter and pack it into the corresponding outbound DTO. `DefaultCompanionTurnRepository` (used by tests + non-live builds) keeps its existing no-op behavior; the parameter is accepted at the trait boundary but ignored by the default.

- **Contract fixtures**: new `contract/fixtures/companion-turns/submit-request-with-character-context.json` and `submit-request-without-character-context.json` mirror the paired backend repo's fixtures byte-for-byte. The existing `ConversationsContractFixturesTest` (or a new sibling) round-trips both shapes through the Kotlin DTO to verify camelCase keys + nullability.

## Capabilities

### Modified Capabilities

- `llm-text-companion-chat`: the existing companion-turn submit / edit / regenerate-at request DTOs gain an optional `characterPromptContext` field; the existing `LiveCompanionTurnRepository` methods gain an optional parameter that threads the resolved context onto the wire. Behavior is unchanged when the new field is `null` (older builds, builds without an active card). When non-null, it is the client's authoritative snapshot of the companion's voice for the duration of one turn.

### New Capabilities

- None. This slice extends `llm-text-companion-chat`. The active-persona / active-card resolution machinery already lives in `core/im-app` and `companion-character-card-depth`; this slice just plumbs them onto an existing wire path.

## Impact

- **Affected Android code**: `data/remote/im/ImBackendModels.kt` (one new DTO + three field additions), `feature/chat/CharacterPromptContextResolver.kt` (new file), `feature/chat/ChatRoute.kt` (three call-site changes in `ChatViewModel`), `data/repository/CompanionTurnRepository.kt` + `LiveCompanionTurnRepository.kt` + `DefaultCompanionTurnRepository.kt` (three method-signature extensions, one threading change in the live impl), `data/remote/im/ImBackendHttpClient.kt` (no change — the DTO is serialized verbatim; no helper renaming). No Compose / UI changes; the affordance surface is unchanged.

- **Affected backend contract**: paired with `companion-turn-backend-llm-bridge` in the sibling backend repo. The shared `contract/fixtures/companion-turns/` directory mirrors the new fixtures across both repos. Without the paired backend, the new field is sent on the wire and the old backend ignores it (forward-compatible per the existing JSON contract — the backend's `serde::Deserialize` already tolerates unknown fields).

- **Affected specs**: delta on `llm-text-companion-chat` (new optional DTO field on the three request shapes + new `LiveCompanionTurnRepository` parameter contract). No delta on `core/im-app` (no UI change) or `companion-character-card-depth` (no card-model change).

- **Affected tests**: new `feature/chat/CharacterPromptContextResolverTest.kt` (pure-function tests of the resolution helper), new `data/remote/im/CompanionTurnSubmitRequestDtoSerializationTest.kt` (round-trips both fixtures), update of `LiveCompanionTurnRepositorySubmitTest.kt` (and edit / regenerate-at variants) to assert the new parameter is forwarded onto the outbound DTO when supplied. Existing tests that don't pass the new parameter continue to work via the default `null`.

- **Affected UX**: none directly. The user-visible win lands when the paired backend slice deploys: real, in-character, single-language replies replace the canned bilingual greeting. Without the paired backend the user-visible behavior is identical to today.

- **Non-goals** (deferred):
  - Client-side macro substitution: still done at the existing greeting-picker call sites for the `firstMes` / `alternateGreetings` preview surface; NOT done for the new `characterPromptContext` payload (the backend is the single point of substitution for prompt-assembly so the audit log has one source of truth).
  - Persona-store backend persistence: S4 (`companion-backend-persona-memory-preset`).
  - Per-character preset / temperature / model overrides: S4.
  - Removal of `CharacterPromptContextDto` once the backend adopts persistent card storage: a clean-up slice after S4 lands.
