## Context

The AI companion branch already switched the third tab from `Space` feed content to a tavern-style companion roster, and the shell chrome around that surface already follows `AppLanguage`. The remaining gap is in the companion content itself: Android seed cards, backend DTOs, and backend persistence all still model `displayName`, `roleLabel`, `summary`, and `openingLine` as single English strings.

Because those same fields are reused in preset cards, owned cards, draw results, and companion-chat identity labels, the problem is not limited to one screen. A Chinese user can enter a Chinese shell and still see English character names, role copy, and opening lines. The fix therefore has to be data-contract-first, not another round of UI-only `pick(...)` patches.

Constraints:
- The app currently supports exactly two product languages: English and Chinese.
- The public repo hides backend source, but the accepted change still has to define the backend contract that the private deployment implements.
- The existing tavern slice already depends on shared character-card models and backend-aware repository mapping, so the localization contract needs to fit both seed fallback and remote API flows.

## Goals / Non-Goals

**Goals:**
- Define companion character content as explicit English and Chinese authored copy instead of single-language strings.
- Ensure tavern cards, draw results, and companion-chat identity surfaces resolve the correct language from the active app language.
- Keep Android seed fallback and backend-driven roster APIs aligned to the same bilingual content shape.
- Make backend persistence and API migration explicit so legacy single-language rows do not leave the UI in a mixed-language state.

**Non-Goals:**
- Do not introduce general-purpose runtime machine translation for companion content.
- Do not expand the app's language system beyond the current English/Chinese pair in this slice.
- Do not redesign tavern layout, draw behavior, or persona orchestration beyond what is required for language-correct copy.

## Decisions

### 1. Model companion copy as structured bilingual fields instead of plain strings

Companion display name, role label, summary, and opening line should each become a structured bilingual value with explicit English and Chinese variants. Android domain models, seed data, backend DTOs, and backend companion models should all use that paired shape rather than storing one locale-resolved string.

Why this decision:
- It removes the root cause: single-language source data leaking through a localized shell.
- It keeps seed fallback and backend-driven flows aligned to one contract.
- It avoids scattering ad hoc `appLanguage.pick(...)` calls around every tavern field.

Alternatives considered:
- Keep single-string models and hardcode Chinese copies only in the Compose layer: rejected because backend-driven draw results and chat identity labels would still drift.
- Have the backend return only one locale-resolved string based on request context: rejected because the Android app already owns runtime language switching and benefits from stable cached bilingual payloads.

### 2. Persist backend companion copy in explicit per-language columns and expose it as a bilingual API object

The private backend should migrate companion storage from single-language text columns to explicit language-specific columns, then map those values into a bilingual API object for roster and draw responses. Existing seeded rows should be backfilled so both language variants exist before the bilingual response contract becomes the source of truth.

Why this decision:
- Explicit columns keep the database schema auditable and make incomplete localization easier to detect during migration.
- The API can stay ergonomic for Android by exposing a nested bilingual object instead of a large flat field list.
- This approach preserves deterministic authored copy and avoids opaque JSON blobs in persistence.

Alternatives considered:
- Store all localized copy in a JSON blob column: rejected because validation and migration become less transparent for a small fixed language set.
- Leave persistence single-language and bolt Chinese text on only in Android seed data: rejected because deployed backend responses would still return English-only characters.

### 3. Resolve language-specific companion projections in shared mapping helpers

The Android app should derive a language-resolved companion projection through shared helpers used by tavern cards, draw-result rendering, and `asCompanionContact()` conversion. UI code should consume that resolved projection instead of directly deciding field-by-field language.

Why this decision:
- It prevents tavern cards and companion chat headers from drifting apart.
- It keeps view code small and makes language-switch behavior easier to test.
- It reduces repeated conditional logic across preset, owned, and draw-result surfaces.

Alternatives considered:
- Let every UI surface resolve bilingual fields independently: rejected because the same language bug would likely recur in one downstream surface.

### 4. Treat bilingual authoring as required content, with compatibility only as migration scaffolding

The accepted product state should require both English and Chinese companion copy for shipped characters. Compatibility behavior exists only to bridge already-seeded single-language rows during rollout, not as a permanent excuse for partially localized companion catalogs.

Why this decision:
- The user's complaint is about incomplete shipped content, not merely missing fallback logic.
- It sets a clear bar for future preset or draw-pool additions.

Alternatives considered:
- Permit long-term single-language characters with fallback to English: rejected because it would preserve the mixed-language tavern problem in a softer form.

## Risks / Trade-offs

- [Bilingual fields add schema and model verbosity] → Mitigation: keep the language set fixed to English/Chinese and centralize mapping helpers so complexity does not spread through every screen.
- [Chinese and English persona copy can drift semantically] → Mitigation: require explicit authored pairs in seed data and backend fixtures, and add tests that verify both language paths instead of only one.
- [Legacy single-language rows could produce partially localized UI during rollout] → Mitigation: backfill backend seed rows as part of the migration and verify roster/draw responses before publishing the updated backend.
- [Language changes might leave stale active-companion labels in already-open chat state] → Mitigation: resolve companion-chat identity labels from the bilingual model on render or on language-change refresh instead of caching one locale permanently.

## Migration Plan

1. Introduce bilingual companion-copy types in Android models and backend API models.
2. Update Android seed data to provide authored English and Chinese content for every tavern character.
3. Add a backend migration that converts `companion_characters` from single-language fields to explicit English/Chinese fields and backfills existing rows.
4. Update backend repository/service/serialization code so roster, draw, and active-selection responses emit bilingual companion content.
5. Update Android repository mapping and tavern/chat rendering helpers to resolve the visible language from `AppLanguage`.
6. Add or update tests covering Chinese and English tavern content, draw results, and backend response serialization.

Rollback strategy:
- Revert the Android mapping/helpers to single-language resolution only if the backend contract has not yet been published.
- If the backend migration is already deployed, keep the expanded bilingual schema and temporarily fall back to English resolution in the client rather than rolling back data columns destructively.

## Open Questions

- Should Chinese companion `displayName` values stay stylistically close to the English proper names or adopt more interpretive Chinese naming? The change assumes each shipped character will have an explicitly reviewed Chinese display name instead of reusing English by default.
