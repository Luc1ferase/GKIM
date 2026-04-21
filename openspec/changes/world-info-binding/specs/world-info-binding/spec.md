## ADDED Requirements

### Requirement: A lorebook is a user-owned collection of bilingual entries bindable to characters or marked global

The system SHALL model a `Lorebook` as a user-owned record carrying bilingual `displayName` and `description` through the `LocalizedText` contract, a `tokenBudget` soft cap on total injected tokens per turn, an `isGlobal` flag that makes the lorebook participate in every turn for that user regardless of character binding, an `isBuiltIn` flag reserved for future seeded lorebooks, and an `extensions` bag that preserves un-modeled ST fields under the `st.*` namespace. Each lorebook MUST contain zero or more `LorebookEntry` records. A lorebook MUST be bindable to zero or more characters owned by the user via a `LorebookBinding` record that carries an `isPrimary` flag used at export time.

#### Scenario: Lorebook persists bilingual display name and description

- **WHEN** a user creates a lorebook with `displayName` and `description` populated on both language sides
- **THEN** subsequent reads of the lorebook return the same bilingual values and neither language side is silently dropped

#### Scenario: Deleting a bound lorebook is blocked until unbind

- **WHEN** a user invokes delete on a lorebook that carries at least one `LorebookBinding`
- **THEN** the delete is rejected with a typed error referencing the bindings, and the UI guides the user to unbind first

#### Scenario: Global flag makes a lorebook participate in every turn

- **WHEN** a user marks a lorebook `isGlobal = true`
- **THEN** every companion turn for that user (regardless of the active character's bindings) includes the global lorebook in the scan's candidate set

### Requirement: A lorebook entry is bilingual with per-language trigger keys and a secondary-key gate

The system SHALL model a `LorebookEntry` with: bilingual `name` and `content` through `LocalizedText`; per-language key lists `keysByLang` (one list per `AppLanguage`) and per-language `secondaryKeysByLang`; a `secondaryGate` enum (`AND`, `OR`, `NONE`); independent `enabled`, `constant`, `caseSensitive` flags; a bounded `scanDepth` (default 3, server-capped at 20); an `insertionOrder` integer where lower = higher priority; a non-injected `comment`; and an `extensions` bag that preserves un-modeled ST fields. An entry MUST NOT be matched against a language's scan text unless its `keysByLang` for that language contains at least one key, OR its `constant` flag is `true`.

#### Scenario: Entry with empty active-language keys and `constant = false` never matches

- **WHEN** an entry's `keysByLang[activeLanguage]` is empty and `constant` is `false`
- **THEN** the entry is not selected in that turn's scan regardless of scan text content

#### Scenario: Entry with `constant = true` always matches

- **WHEN** an entry's `constant` flag is `true` and the entry is `enabled`
- **THEN** the entry is included in the matched set unconditionally, regardless of keys or scan text

#### Scenario: Secondary gate AND requires all secondary keys to match

- **WHEN** an entry's `secondaryGate = AND` and its `secondaryKeysByLang[activeLanguage]` has at least one key
- **THEN** the entry matches only when its primary key matches AND every secondary key for the active language appears in the scan text

#### Scenario: Secondary gate OR requires any secondary key to match

- **WHEN** an entry's `secondaryGate = OR` and its `secondaryKeysByLang[activeLanguage]` has at least one key
- **THEN** the entry matches when its primary key matches AND any one secondary key for the active language appears in the scan text

#### Scenario: Secondary gate NONE ignores secondary keys

- **WHEN** an entry's `secondaryGate = NONE`
- **THEN** the entry matches based on primary keys alone and any populated secondary keys are ignored for the scan decision

### Requirement: The lorebook scan runs server-side, once per companion turn, over a bounded scan window

The system SHALL execute a single-pass keyword scan once per companion turn on the server. The scan text MUST be the concatenation of the current user turn and the bodies of up to `scanDepth` prior turns, capped by a server-enforced maximum of 20 prior turns regardless of the entry-level `scanDepth`. Keyword matching MUST be literal substring matching with case sensitivity per entry, and MUST use only the active language's `keysByLang` and `secondaryKeysByLang` values. The scan MUST NOT recursively rescan after injecting matched entries.

#### Scenario: Scan text includes the current user turn plus the last scanDepth turns

- **WHEN** the scan runs with an entry carrying `scanDepth = 3`
- **THEN** the scan text is the current user turn's body plus the bodies of the three most recent prior turns, in order

#### Scenario: Server cap overrides entry-level scanDepth

- **WHEN** an entry declares `scanDepth = 50`
- **THEN** the scan treats the effective window as the server-enforced maximum (20) and does not read more than 20 prior turns

#### Scenario: Scan is single-pass

- **WHEN** an injected entry's `content` contains text that matches another entry's keyword
- **THEN** the second entry is not re-triggered; the scan does not run again after injection

### Requirement: Matched entries are sorted by a deterministic total order and budget-dropped by `insertionOrder`

The system SHALL produce a deterministic total order over matched entries using `(insertionOrder ascending, lorebookId ascending, entryId ascending)`. When the bundle token count exceeds the allocator's assigned budget for the `worldInfoEntries` section, the system MUST drop entries in reverse order of that total ordering (highest `insertionOrder` first) until the bundle fits or is empty. When a single lorebook's contribution exceeds its own `tokenBudget`, the system MUST drop that lorebook's remaining entries before dropping any other lorebook's entries.

#### Scenario: Equal `insertionOrder` values resolve deterministically

- **WHEN** two entries across different lorebooks share the same `insertionOrder`
- **THEN** they are ordered by `lorebookId` then by `entryId` so the order is stable across turns with the same inputs

#### Scenario: Per-lorebook budget drops before per-section budget

- **WHEN** a single lorebook's matched entries collectively exceed its own `tokenBudget`
- **THEN** that lorebook's lowest-priority entries are dropped first, before the allocator considers dropping entries from other lorebooks for the section-level budget

#### Scenario: Section budget drops the lowest-priority entries

- **WHEN** the total matched bundle exceeds the section's allocator budget after per-lorebook dropping
- **THEN** entries are dropped in reverse total-order (highest `insertionOrder` first) until the bundle fits or is empty

### Requirement: Import from ST `character_book` auto-materializes a bound lorebook; export round-trips it back

The system SHALL, when committing an imported SillyTavern V2 or V3 card that carries a `character_book` field, create a new `Lorebook` owned by the importing user, seed its entries from `character_book.entries` mapping modeled fields 1:1 and placing un-modeled fields under each entry's `extensions.st.*`, and create a `LorebookBinding(lorebookId, characterId, isPrimary = true)` for the newly-imported character. The system SHALL, when exporting a character to ST JSON or PNG, read the character's primary-bound lorebook and emit it into the target's `character_book` field with modeled fields mapped back 1:1 and the lorebook's + entries' `extensions.st.*` merged into the target. The other-language content MUST be preserved under `entry.extensions.stTranslationAlt.*` so re-import is lossless.

#### Scenario: Import creates a new lorebook bound to the imported character

- **WHEN** a user commits an imported ST card whose JSON carries a `character_book`
- **THEN** the backend creates one new `Lorebook` owned by the user, seeds its entries from `character_book.entries`, and creates a `LorebookBinding` to the new character with `isPrimary = true`

#### Scenario: Export round-trips the bound lorebook into `character_book`

- **WHEN** a user exports a character whose primary-bound lorebook has entries
- **THEN** the exported V2/V3 payload carries a `character_book` field whose entries reproduce every modeled field and preserve `extensions.st.*`; a subsequent import of that payload recreates the same entry set

#### Scenario: Other-language payload survives monolingual export via `extensions.stTranslationAlt.*`

- **WHEN** the exporting user picks English as the primary export language on an entry whose `content` and `keysByLang` are populated for both English and Chinese
- **THEN** the exported entry's `content` and `keys` carry the English side, and the Chinese side is preserved under `entry.extensions.stTranslationAlt.content` and `entry.extensions.stTranslationAlt.keys`

#### Scenario: Multiple bindings on export surface a warning

- **WHEN** a user exports a character bound to more than one lorebook
- **THEN** the exporter emits only the `isPrimary = true` binding into `character_book`; the export response carries a typed warning naming the non-primary bindings so the user can merge or re-bind before re-export
