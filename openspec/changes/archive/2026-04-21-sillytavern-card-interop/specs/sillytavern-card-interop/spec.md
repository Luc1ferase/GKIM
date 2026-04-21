## ADDED Requirements

### Requirement: SillyTavern character cards are imported as PNG with tEXt chunks or standalone JSON

The system SHALL accept imported character cards in two formats: PNG files whose `tEXt` chunk `chara` contains a base64-encoded JSON payload matching the SillyTavern Tavern Card V2 or V3 schema, and standalone JSON files matching the same V2 or V3 schema. The system MUST reject unsupported formats (legacy V1, WebP, GIF, or otherwise unrecognized payloads) with a typed error, and MUST reject payloads that exceed bounded safety limits with a typed error.

#### Scenario: PNG import with tEXt chunk succeeds

- **WHEN** a user uploads a PNG whose `tEXt` chunk `chara` carries a base64-encoded V2 or V3 Tavern Card JSON payload within bounded size limits
- **THEN** the system decodes the chunk, validates the payload against the V2 or V3 schema, and returns a structured import preview containing the decoded deep persona record, any warnings, and a language-detection result

#### Scenario: JSON import succeeds

- **WHEN** a user uploads a standalone JSON file matching the V2 or V3 schema within bounded size limits
- **THEN** the system validates the payload and returns the same structured import preview as it would for a PNG

#### Scenario: Legacy V1 and unsupported formats are rejected

- **WHEN** a user uploads a file whose schema version is unsupported (legacy V1) or whose format is unsupported (WebP, GIF, unrecognized binary)
- **THEN** the system rejects the upload with a typed error (`unsupported_schema_version` or `unsupported_format`) and does not persist or pre-commit any record

#### Scenario: Malformed PNG or JSON payloads are rejected with typed codes

- **WHEN** a user uploads a PNG whose tEXt chunks are missing, corrupt, or fail CRC validation, or a JSON file that fails to parse or fails V2/V3 schema validation
- **THEN** the system rejects the upload with `malformed_png` for the PNG case and `malformed_json` for the JSON case, and does not persist or pre-commit any record

### Requirement: Import is a two-step flow: preview and commit

The system SHALL expose an import preview operation that returns the decoded record without persisting, and a separate commit operation that persists the previewed record after user confirmation. The preview MUST surface warnings for every field truncated, trimmed, dropped, or parked in the `extensions` bag, and MUST let the user override the detected source language before committing.

#### Scenario: Preview does not persist the imported card

- **WHEN** a user completes the preview step but does not commit
- **THEN** the system has not added any new card to the user's roster, and the upload's server-side temporary state is eligible for expiry

#### Scenario: Preview warnings enumerate every adjustment

- **WHEN** the system decodes a card that triggers field truncation, alt-greetings trim, avatar resize, or unknown-field parking
- **THEN** the preview response includes a structured warnings list with an entry for each adjustment, naming the field and the adjustment type

#### Scenario: User overrides the detected source language before committing

- **WHEN** the preview's detected source language differs from the user's intended language
- **THEN** the user can select a different language in the preview UI, and the subsequent commit applies the user-selected language as the primary side of every `LocalizedText` field

### Requirement: Import preserves unknown and not-yet-modeled ST fields in the `st.*` extensions namespace

The system SHALL preserve every ST field that does not map onto a modeled deep persona field by placing it in the card's `extensions` bag under the `st.*` namespace, using stable keys (`stPostHistoryInstructions`, `stGroupOnlyGreetings`, `stDepthPrompt`, `stNickname`, `stSource`, `stCreationDate`, `stModificationDate`, `stAssets`, and `st.<otherKey>` for the ST V3 `extensions` sub-object). The preserved values MUST round-trip on subsequent exports.

#### Scenario: Unknown ST fields round-trip through import and export

- **WHEN** a user imports a card with unknown-to-us ST fields, previews, commits, then exports the card in the same format
- **THEN** the exported payload contains the same set of ST fields with the same values, preserved through the `extensions.st.*` namespace

#### Scenario: Reserved namespace keys are stable

- **WHEN** the system maps a V3 `post_history_instructions` or `depth_prompt` or similar known-unmapped field
- **THEN** the key inside `extensions` is exactly one of the reserved stable keys listed in this capability's design, and the value matches the source

### Requirement: Monolingual ST sources are promoted to bilingual records with user-selectable primary language

The system SHALL promote every monolingual ST prose field into the bilingual `LocalizedText` record, placing the imported text on the user-selected primary side and mirroring the same text onto the other side with an `extensions.stTranslationPending.<field>` flag. The primary side defaults to a heuristic detection result; the user MAY override the detection at preview time. The system MUST NOT auto-translate the other side via any provider.

#### Scenario: Heuristic detection sets the default primary language

- **WHEN** the system decodes a monolingual ST card
- **THEN** the import preview's detected-source-language field is populated by a heuristic based on character-range analysis, and is presented as the default selection in the language picker

#### Scenario: Other-language side is mirrored with a pending flag

- **WHEN** a monolingual card is committed with primary language set
- **THEN** every bilingual prose field on the persisted record has the imported text on the primary side and an identical mirrored value on the other side, and the `extensions.stTranslationPending` bag lists each mirrored field

#### Scenario: Auto-translation does not occur

- **WHEN** a monolingual card is imported
- **THEN** the other-language side is never produced via a provider call; the mirrored placeholder is the only default, and the user must manually edit to produce a real translation

### Requirement: Export emits PNG or JSON with user-selected target language and optional other-language preservation

The system SHALL export a card as either a PNG with dual `chara` (V2-compatible subset) and `ccv3` (full V3) tEXt chunks, or as a JSON file emitting V3 by default with an opt-in V2-only mode. The user MUST choose which `LocalizedText` side (`en` or `zh`) is emitted as the ST monolingual text. The user MAY opt into preserving the other-language side under `extensions.stTranslationAlt.<field>` in the exported payload; the default is off.

#### Scenario: PNG export embeds both V2 and V3 chunks

- **WHEN** a user exports a card as PNG
- **THEN** the exported image carries both a `chara` tEXt chunk (V2-compatible subset payload) and a `ccv3` tEXt chunk (full V3 payload), so both generations of downstream tools can read the card

#### Scenario: JSON export defaults to V3 with V2-only opt-in

- **WHEN** a user exports a card as JSON without specifying a version
- **THEN** the exported file contains the full V3 schema; if the user explicitly selects V2-only, the exported file contains a V2 payload and the preview notifies the user that V3-only fields will be omitted

#### Scenario: Other-language side is preserved only when opted in

- **WHEN** a user exports a card with the "include other-language text" toggle off
- **THEN** the exported payload contains only the target-language text in ST monolingual fields and does not carry `extensions.stTranslationAlt.*` keys

#### Scenario: Opting in adds stTranslationAlt entries

- **WHEN** a user exports a card with the "include other-language text" toggle on
- **THEN** the exported payload carries an `extensions.stTranslationAlt.<field>` entry for every bilingual prose field, containing the non-target-language text verbatim

### Requirement: Bounded safety limits reject or adjust oversize payloads with typed error codes

The system SHALL enforce bounded safety limits on imported payloads: PNG files > 8 MiB MUST be rejected with `payload_too_large`; JSON files > 1 MiB MUST be rejected with `payload_too_large`; avatar images > 4096×4096 pixels MUST be rejected with `avatar_too_large`; any prose field > 32 KiB MUST be truncated with a warning; alt-greetings beyond 64 entries MUST be trimmed with a warning; tags beyond 256 entries MUST be trimmed with a warning; any `extensions.st.*` value > 64 KiB MUST be dropped with a warning.

#### Scenario: Oversize payload is rejected before decoding

- **WHEN** a user uploads a payload that exceeds the file-size or avatar-dimension limits
- **THEN** the system rejects the upload with the corresponding typed error code, and no decoded preview is produced

#### Scenario: Oversize field is truncated with a warning

- **WHEN** a prose field exceeds the per-field size limit (e.g., 32 KiB)
- **THEN** the system truncates the field to the limit and adds a typed warning entry to the preview, naming the field and the adjustment

#### Scenario: Oversize alt-greetings or tags are trimmed with a warning

- **WHEN** alt-greetings exceed 64 entries or tags exceed 256 entries
- **THEN** the system trims to the limit and adds a typed warning entry to the preview
