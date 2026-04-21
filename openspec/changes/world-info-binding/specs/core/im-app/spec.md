## ADDED Requirements

### Requirement: Android Settings Companion section exposes a World Info library entry

The system SHALL expose a "World Info" entry inside the Settings `Companion` section that routes to a lorebook library surface. The entry's label and caption MUST be bilingual through `LocalizedText` and render in the user's active `AppLanguage`. The library surface MUST list the user's lorebooks with per-row (displayName, entry count, `Global` badge when `isGlobal = true`), expose a primary Create CTA, a per-row overflow with Duplicate, Delete (disabled when the lorebook has any binding), and Toggle-global, and MUST route each row-tap into the lorebook editor.

#### Scenario: Entry renders inside the Companion section

- **WHEN** a user opens Settings and taps into the `Companion` section
- **THEN** a "World Info" entry appears alongside the persona library, preset library, and memory shortcut, labeled in the user's active `AppLanguage`

#### Scenario: Delete affordance is disabled for bound lorebooks

- **WHEN** a user opens the row overflow for a lorebook that has at least one `LorebookBinding`
- **THEN** the Delete affordance is visibly disabled and tapping it does not issue a delete request; the UI surfaces a hint pointing to the bindings surface

### Requirement: Android lorebook editor exposes full header + entry + binding editing with per-language key isolation

The system SHALL provide a lorebook editor route that exposes: editable header fields (`displayName`, `description`, `tokenBudget`, `isGlobal`); an entry list where each row surfaces the entry's `name`, `insertionOrder`, an `enabled` toggle, and an overflow with Duplicate, Delete, Move-up, and Move-down; a per-entry detail surface exposing `name` (bilingual), `keysByLang` with distinct per-language tabs (en/zh), `secondaryKeysByLang`, `secondaryGate` selector, `content` (bilingual), `constant` toggle, `caseSensitive` toggle, `scanDepth` and `insertionOrder` numeric fields, and a `comment` textarea; and a bindings sub-surface listing the characters this lorebook is bound to with per-character Unbind and a "Bind to character" picker.

#### Scenario: Per-language key tabs are isolated

- **WHEN** a user adds keys on the English tab of the entry editor
- **THEN** the added keys appear only in `keysByLang[English]`, and the Chinese tab's list is not mutated

#### Scenario: Move-up / Move-down adjusts `insertionOrder`

- **WHEN** a user taps Move-up on the second-highest entry
- **THEN** the entry's `insertionOrder` is adjusted so the entry now sorts above the entry formerly first, and the change is persisted via the backend

#### Scenario: Bind picker adds a binding

- **WHEN** a user opens the Bind picker, selects a character, and confirms
- **THEN** a `LorebookBinding` is created with `isPrimary = false` by default, and the characters list in the bindings surface updates to include the new binding

### Requirement: Android character card-detail exposes a Lorebook tab scoped to that character's bindings

The system SHALL add a "Lorebook" tab to the character card-detail surface that renders a read-only preview of every lorebook currently bound to the character. Each lorebook row MUST display its `displayName` and entry count and MUST expose a "Manage in library" affordance that routes to the lorebook editor for that lorebook. When a character has zero bound lorebooks, the tab MUST render a zero-state with a "Bind a lorebook" CTA that opens a picker of the user's lorebooks.

#### Scenario: Character with bound lorebooks shows a read-only preview

- **WHEN** a user opens the Lorebook tab for a character that has at least one binding
- **THEN** the tab lists each bound lorebook with its `displayName` and entry count, and each row exposes a "Manage in library" link that deep-routes into the editor

#### Scenario: Character with zero bindings shows the zero-state CTA

- **WHEN** a user opens the Lorebook tab for a character that has no bindings
- **THEN** the tab renders a zero-state message and a "Bind a lorebook" CTA that opens the lorebook picker

### Requirement: Android card-import preview surfaces an embedded lorebook summary

The system SHALL extend the character-card import preview so that, when the imported payload's JSON carries a `character_book` field, the preview response includes a lorebook-import summary block listing the entry count, a total token estimate, and a boolean flag indicating whether any entry has `constant = true`. The Android preview UI MUST render this summary inline so the user sees the lorebook side effect before tapping commit.

#### Scenario: Preview includes lorebook summary when `character_book` is present

- **WHEN** an imported card's JSON carries a non-empty `character_book`
- **THEN** the Android preview surface renders a lorebook summary block showing entry count, total token estimate, and the presence of any `constant` entries

#### Scenario: Preview omits the summary block when `character_book` is absent

- **WHEN** an imported card's JSON does not carry a `character_book` field, or the field is an empty object
- **THEN** the Android preview surface does not render the lorebook summary block
