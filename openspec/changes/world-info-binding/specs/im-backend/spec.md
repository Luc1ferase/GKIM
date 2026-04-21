## ADDED Requirements

### Requirement: Backend exposes authenticated CRUD for lorebooks, entries, and bindings

The system SHALL expose authenticated HTTP endpoints for lorebook lifecycle: `GET /api/lorebooks`, `POST /api/lorebooks`, `GET /api/lorebooks/{id}`, `PATCH /api/lorebooks/{id}`, `DELETE /api/lorebooks/{id}`, `POST /api/lorebooks/{id}/duplicate`, entry CRUD under `/api/lorebooks/{id}/entries/*`, and binding CRUD under `/api/lorebooks/{id}/bindings/*`. Every record MUST be owned by the authenticated user. Deleting a lorebook that carries at least one binding MUST fail with a typed error code `lorebook_has_bindings` until the bindings are removed.

#### Scenario: CRUD endpoints are scoped to the authenticated user

- **WHEN** an authenticated client calls `GET /api/lorebooks`
- **THEN** the response lists only lorebooks owned by the authenticated user

#### Scenario: Delete rejected when bindings exist

- **WHEN** a client calls `DELETE /api/lorebooks/{id}` on a lorebook that has at least one `LorebookBinding`
- **THEN** the backend rejects the request with `errorCode = "lorebook_has_bindings"` and the lorebook is not deleted

#### Scenario: Binding CRUD controls character attachment

- **WHEN** a client calls `POST /api/lorebooks/{id}/bindings` with `{ characterId, isPrimary }`
- **THEN** the backend creates a `LorebookBinding(lorebookId, characterId, isPrimary)` or rejects with `errorCode = "binding_exists"` if a binding for that pair already exists

#### Scenario: Setting isPrimary clears other primaries for the character

- **WHEN** a client updates a binding with `isPrimary = true` while another binding on the same character already has `isPrimary = true`
- **THEN** the backend demotes the prior primary to `isPrimary = false` so each character has at most one primary-bound lorebook

### Requirement: Backend executes a deterministic single-pass keyword scan at turn-assembly time

The system SHALL execute a deterministic single-pass keyword scan once per companion turn during prompt assembly. The scan MUST collect candidate lorebooks as the union of (a) lorebooks bound to the active character and (b) the authenticated user's lorebooks with `isGlobal = true`, de-duplicated by `lorebookId`. The scan text MUST be the current user turn body concatenated with up to `scanDepth` prior turn bodies, capped at a server-enforced maximum of 20 prior turns. Matching MUST be literal substring matching, case-sensitivity per entry. `constant = true` entries MUST be included unconditionally. The matched set MUST be totally ordered by `(insertionOrder asc, lorebookId asc, entryId asc)`.

#### Scenario: Candidate set is deduplicated across binding + global

- **WHEN** a lorebook has `isGlobal = true` AND is also bound to the active character
- **THEN** the lorebook appears exactly once in the scan's candidate set

#### Scenario: Scan window cap is enforced

- **WHEN** an entry declares `scanDepth = 50`
- **THEN** the scan reads at most 20 prior turns regardless of the requested `scanDepth`

#### Scenario: Total order is stable across runs with the same inputs

- **WHEN** the same scan input is evaluated twice
- **THEN** the matched set's order is identical across both runs, preserving determinism for debugging and replay

### Requirement: Backend injects matched entries as the `worldInfoEntries` allocator section with per-lorebook and per-section budgets

The system SHALL inject the matched entries as a single `worldInfoEntries` section in the deterministic token-budget allocator, placed between the `userPersonaDescription` section (above) and the `rollingSummary` section (below) in priority, and between `rollingSummary` (above) and the non-critical preset sections (below) in drop order. The allocator MUST enforce a per-lorebook `tokenBudget` cap by dropping that lorebook's lowest-priority entries first, then enforce the section's overall budget by dropping entries globally in reverse total-order until the bundle fits or is empty.

#### Scenario: Section priority sits between user persona description and rolling summary

- **WHEN** the allocator assembles a prompt containing both user persona description and rolling summary in addition to matched world-info entries
- **THEN** the emitted prompt orders them: `...`, `userPersonaDescription`, `worldInfoEntries`, `rollingSummary`, `...`

#### Scenario: Per-lorebook budget drops before section budget

- **WHEN** a single lorebook's matched entries exceed its own `tokenBudget`
- **THEN** entries from that lorebook are dropped in reverse `insertionOrder` until the lorebook's bundle fits, before any other lorebook's entries are considered for the section-level drop

#### Scenario: Section budget drops bundle tail last

- **WHEN** the cumulative matched bundle after per-lorebook dropping still exceeds the section-level allocator budget
- **THEN** entries are dropped globally in reverse `(insertionOrder, lorebookId, entryId)` order until the bundle fits or the section is empty

### Requirement: Backend auto-materializes a bound lorebook on import of ST `character_book` and round-trips on export

The system SHALL, at character-card import commit, detect whether the imported payload's V2/V3 JSON carries a non-empty `character_book` field, create a new `Lorebook` owned by the importing user, seed its entries from `character_book.entries` mapping modeled fields 1:1 and placing un-modeled fields under each entry's `extensions.st.*`, and create a `LorebookBinding(lorebookId, newCharacterId, isPrimary = true)`. The system SHALL, at character-card export, read the character's primary-bound lorebook and emit it into the export payload's `character_book` field, preserving `extensions.st.*` on each entry and preserving the other-language payload under `entry.extensions.stTranslationAlt.*`.

#### Scenario: Import commit materializes lorebook + primary binding

- **WHEN** an authenticated client commits an imported ST card whose JSON carries a non-empty `character_book`
- **THEN** the backend persists one new `Lorebook` seeded from `character_book.entries`, creates a `LorebookBinding` with `isPrimary = true` to the newly-imported character, and the commit response includes both the new card and the new lorebook record

#### Scenario: Export emits primary-bound lorebook into `character_book`

- **WHEN** an authenticated client exports a character whose primary-bound lorebook has entries
- **THEN** the exported payload's `character_book.entries` includes every entry with modeled fields reproduced and with `extensions.st.*` preserved

#### Scenario: Non-primary bindings surface as a warning on export

- **WHEN** a character has more than one binding at export time
- **THEN** the export response carries a typed warning listing the non-primary lorebook IDs and the exported `character_book` contains only the primary binding's entries
