## Context

The tavern lobby already ships (`replace-space-with-character-roster-and-gacha` accepted) and bilingual companion copy is already contractual (`localize-companion-tavern-copy` accepted). Users see cards, can draw, and can activate a companion, but opening a chat with that companion still relies on the ordinary human-IM `Conversation` path because the card itself carries no persona instructions. The effective behavior is "the companion has a name and a one-line summary"; there is no authored system prompt, no scenario, no example dialogue to anchor style, no first-message greeting separate from summary, and no way for a user (or a future importer) to add a new card.

This slice is the data-model foundation for every later tavern feature on this branch. LLM chat (change 3), ST card interop (change 2), user personas (change 4), world info (change 5), and preset/memory (change 6) all assume this shape exists. We therefore have to get the schema and the editor UX right before the LLM wiring lands, not after.

Constraints:
- The app supports exactly two product languages (English, Chinese). Every authored prose field must be bilingual via `LocalizedText` to stay coherent with `localize-companion-tavern-copy`.
- The public repo hides backend source (`backend/README.md`). Spec deltas describe the backend contract; the Rust implementation lives in the maintainer-private checkout.
- Android service-boundary rules apply: no DB credentials in the APK, companion persistence flows over HTTPS roster APIs.
- The release automation (`scripts/release-android-tag.ps1`) must stay green; this slice is code-only and should not touch signing or CI.
- The seed-only `DefaultCompanionRosterRepository` is currently immutable. A mutation-capable repository has to keep returning stable `StateFlow` reads for existing `TavernUiState` consumers.

## Goals / Non-Goals

**Goals:**
- Model companion character cards as full tavern persona records on Android and in the backend contract.
- Let users open a character detail view from the tavern roster and author/edit user-created cards.
- Preserve the existing tavern tab, draw behavior, active-selection behavior, and bilingual contract without regressing.
- Position the repository and spec so the later LLM / interop / persona / world-info / preset / memory slices can plug in without reshaping the card schema again.

**Non-Goals:**
- Do not implement LLM text generation, streaming reply lifecycle, or swipe/regenerate. Those belong to change 3.
- Do not implement SillyTavern PNG or JSON import/export. Belongs to change 2. This slice only reserves the `extensions` bag so unknown fields survive future import.
- Do not introduce user personas, world info, preset modules, or memory summaries. Later changes.
- Do not redesign the tavern lobby layout, draw animation, or bottom navigation.
- Do not ship user-generated card publishing / sharing pipelines.

## Decisions

### 1. Expand `CompanionCharacterCard` in place instead of introducing a parallel "deep" type

Rationale:
- The existing type already owns id, accent, avatarText, source, and bilingual display fields; splitting into `ShallowCard` + `DeepCard` would force every tavern surface to pick between them and double the mapping surface.
- Future slices (LLM chat, ST interop, world info binding) all want one canonical card type to pass around.
- `resolve(language)` already exists; adding fields there preserves the pattern instead of inventing another projection.

Alternative considered:
- Keep `CompanionCharacterCard` as a display-only row and add a sibling `CompanionPersona` record keyed by card id. Rejected because every call site would need both, and the backend would have to maintain two aligned tables.

### 2. Every authored prose field is `LocalizedText`; authoring metadata is not

Fields that become `LocalizedText`: `systemPrompt`, `personality`, `scenario`, `exampleDialogue`, `firstMes`, each entry in `alternateGreetings`.
Fields that stay plain strings: `tags: List<String>`, `creator: String`, `creatorNotes: String`, `characterVersion: String`.

Rationale:
- Shell chrome and existing cards already pay the bilingual cost; extending it is strictly consistent.
- Tag search, creator handles, and version strings are identifiers rather than prose. Bilingualizing them invites drift (e.g., tag "fantasy" / "奇幻" would create two indexes for the same concept) and is not what ST ecosystem cards encode either.
- `LocalizedText` stays the single resolution point, so UI can continue calling `card.resolve(language)`.

Alternative considered:
- Make everything bilingual. Rejected because creator handles and version strings have no semantic translation.

### 3. `firstMes` replaces `openingLine`; alternate greetings live beside it

`openingLine` is renamed to `firstMes` (aligns with ST card JSON) and gains a sibling `alternateGreetings: List<LocalizedText>`. Call sites that currently read `openingLine` are migrated to `firstMes` in the same slice.

Rationale:
- ST ecosystem and future interop (change 2) use `first_mes` and `alternate_greetings`. Naming agreement now avoids a second rename when change 2 lands.
- Multiple greetings let the companion entry flow randomize or let the user pick the opening line — useful once LLM chat exists, cheap to model now.

Alternative considered:
- Keep `openingLine` and add `alternateGreetings` as a separate optional list. Rejected because the single/list split leaves an odd name for the primary greeting and complicates ST import mapping.

### 4. Repository becomes a mutation-capable CRUD while preserving preset immutability

The `CompanionRosterRepository` interface gains `upsertUserCharacter(card: CompanionCharacterCard): CompanionCharacterCard` and `deleteUserCharacter(characterId: String): Boolean`. Preset characters (those with `source == Preset`) cannot be modified or deleted; attempts fail explicitly. Owned-from-draw characters (`source == Drawn`) can be edited but not deleted unless explicitly allowed (out of scope for this slice; default: edit allowed, delete blocked to avoid accidental loss).

Rationale:
- Authoring requires mutation.
- Presets are product content and must stay canonical.
- Drawn characters are rewards the user expects to keep.

Alternative considered:
- Allow full delete on everything. Rejected because it invites accidental loss of drawn roster entries and clashes with the "owned roster" contract from `replace-space-with-character-roster-and-gacha`.

### 5. Rename `feature/space` to `feature/tavern`; keep the `space` route id stable

Files move to `feature/tavern/` and file-level composables are renamed (`SpaceRoute` → `TavernRoute`). The navigation destination string remains `"space"` to avoid rewriting every navigation call site in this slice; a later cosmetic rename slice can flip the route id if desired.

Rationale:
- File naming should match the product surface (tavern) to reduce confusion for later slices.
- Route string rename is a separate cross-cutting concern with test impact disproportionate to its value.

### 6. Add an `extensions: Map<String, JsonElement>` bag on the card

The card carries a forward-compatible `extensions` map that stores unknown fields from future import flows (change 2) without dropping data. UI does not render it; repository and backend serializer must round-trip it.

Rationale:
- Change 2 will import ST V2/V3 cards whose `data.extensions` object contains arbitrary third-party keys.
- Dropping those silently on import makes round-trip export lossy.
- Modeling the bag now means change 2 does not retroactively alter the schema.

### 7. Backend contract gains bilingual columns and an `extensions` JSON column; migration/backfill happens in the backend slice

The spec delta for `im-backend` requires: per-field English/Chinese columns for the new prose fields (mirroring `localize-companion-tavern-copy`'s approach) and a JSONB `extensions` column for forward compatibility. Backfill for existing preset rows uses the shipped Android seed data as the authoritative source.

## Risks / Trade-offs

- [Schema surface grows substantially, raising the cost of later migrations]
  → Mitigation: model all new prose fields through the existing `LocalizedText` pattern and funnel change 2's unknown data through `extensions` rather than more ad-hoc columns.
- [Backend migration can diverge from Android model if the two are edited separately]
  → Mitigation: lock both sides in this same OpenSpec change; implement Android first, then backend contract; add a companion-roster API contract test on Android that exercises all new fields.
- [Character editor UX could grow beyond the slice if users request previewing, validation, etc.]
  → Mitigation: ship a flat form that maps 1:1 to model fields and defer preview/validation affordances to later slices.
- [The existing tests reference `openingLine`]
  → Mitigation: rename in model, route data, and test fixtures in the same commit; the rename is cheap and localized.
- [SAF-provided avatar URIs can expire or the underlying file can be deleted]
  → Mitigation: the card stores a `Uri` string plus a fallback `avatarText`; UI renders avatar if the URI resolves, else falls back to `avatarText`.

## Migration Plan

1. Update Android domain models and the seed constants to the new field shape. Keep the existing preset seeded copy (already bilingual) and seed the new prose fields with authored English/Chinese content for each shipped preset.
2. Update `CompanionRosterRepository` interface + `DefaultCompanionRosterRepository` + `BackendAwareCompanionRosterRepository` to expose CRUD semantics while preserving preset immutability.
3. Update `feature/space/SpaceRoute.kt` consumers to new field names, then move the file to `feature/tavern/TavernRoute.kt` and update package + imports. Leave the navigation route id `"space"` untouched for now.
4. Add `feature/tavern/CharacterDetailRoute.kt` and `feature/tavern/CharacterEditorRoute.kt`. Wire from tavern card row → detail; add a `+` action in the tavern top bar → editor (create); add "Edit" action in detail → editor (update) for non-preset cards.
5. Update the backend spec delta so the remote contract mirrors the Android shape. The private backend implements the schema migration + API serializer + tests in its own checkout; the public repo only records the contract.
6. Update existing Android unit tests (`RepositoriesTest.kt`, tavern view-model tests) and add tests for the new CRUD behavior and field resolution.

Rollback strategy:
- If the backend contract cannot land in the same delivery window, the Android client can run purely on the seed/default repository; the roster API continues to serve the old shallow shape and the client tolerates missing new fields by falling back to empty `LocalizedText`. We would then ship the backend contract in a follow-up slice. The Android schema itself does not need to be reverted.

## Open Questions

- Should authored user characters be discoverable via the draw pool? Default: no — draw pool stays preset-authored content only; user-created cards sit only in the owned roster.
- Should a user be allowed to edit a preset card locally as an "override"? Default: no in this slice; cloning a preset into an editable user card is a later convenience.
- Should `firstMes` be chosen from `alternateGreetings` at chat entry, and if so by whom (user picks / random)? Default in this slice: first entry is `firstMes`; change 3 decides the picker.
- Avatar storage: keep on local SAF URI only, or also upload to backend? Default: local URI now; backend upload can be layered later without schema change.
