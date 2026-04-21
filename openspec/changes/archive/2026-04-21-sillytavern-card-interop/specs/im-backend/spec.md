## ADDED Requirements

### Requirement: Backend exposes a two-step card import endpoint that decodes V2 or V3 payloads and returns a structured preview

The system SHALL expose authenticated HTTP endpoints `POST /api/cards/import` (preview) and `POST /api/cards/import/commit` (persist). The preview endpoint MUST accept a PNG or JSON payload, validate the size, decode PNG tEXt chunks (`chara` base64 JSON and optional `ccv3`), validate the resulting JSON against the Tavern Card V2 or V3 schema, map ST fields to the deep persona record, and return a structured preview including the mapped record, a typed warnings list, the detected source language, and the `extensions.st.*` summary. The commit endpoint MUST accept the previewed record plus user overrides (selected language, field edits) and persist the record as a user-authored card.

#### Scenario: Preview decodes a PNG with a tEXt chara chunk

- **WHEN** an authenticated client uploads a PNG whose `tEXt` chunk `chara` carries a base64-encoded V2 or V3 Tavern Card JSON within bounded size limits
- **THEN** the backend returns a 200 preview response containing the mapped deep persona record, the typed warnings list, the detected source language, and the `extensions.st.*` summary

#### Scenario: Preview decodes a standalone JSON

- **WHEN** an authenticated client uploads a standalone V2 or V3 JSON file within bounded size limits
- **THEN** the backend returns the same preview shape as it does for a PNG import

#### Scenario: Commit persists the previewed record with user overrides

- **WHEN** an authenticated client posts a commit request referencing the previewed record plus any user overrides (selected primary language, field edits)
- **THEN** the backend persists the record as a user-authored card, assigns a new stable `id`, and returns the persisted card record

### Requirement: Backend enforces bounded safety limits on imports with typed error codes

The system SHALL enforce bounded safety limits on imported payloads. PNG files larger than 8 MiB and JSON files larger than 1 MiB MUST be rejected with error code `payload_too_large`. Avatar images larger than 4096×4096 pixels MUST be rejected with `avatar_too_large`. Payloads whose schema version is unsupported (legacy V1) MUST be rejected with `unsupported_schema_version`. Malformed PNG or JSON payloads MUST be rejected with `malformed_png` or `malformed_json` respectively. Over-length prose fields MUST be truncated and alt-greetings / tags above their caps MUST be trimmed, each surfaced as typed warning entries in the preview response.

#### Scenario: Typed error code accompanies rejections

- **WHEN** the backend rejects an import for any bounded-limit or schema-version reason
- **THEN** the response status is an error status (e.g., 413 or 422) with a JSON body whose `errorCode` field equals one of the documented typed codes (`payload_too_large`, `avatar_too_large`, `unsupported_schema_version`, `malformed_png`, `malformed_json`, `unsupported_format`)

#### Scenario: Truncation and trim surface as warnings, not errors

- **WHEN** the backend truncates a prose field, trims alt-greetings above 64, trims tags above 256, or drops an oversize `extensions.st.*` value
- **THEN** the preview response is 200 and carries a typed warning entry naming the field and the adjustment type, without failing the import

### Requirement: Backend preserves every imported ST field in the card's `extensions` bag under the `st.*` namespace

The system SHALL place every ST field that does not map onto a modeled deep persona field into the card's `extensions` bag under the `st.*` namespace, using stable keys: `stPostHistoryInstructions`, `stGroupOnlyGreetings`, `stDepthPrompt`, `stNickname`, `stSource`, `stCreationDate`, `stModificationDate`, `stAssets`, and `st.<otherKey>` for unknown subkeys inside the ST V3 `extensions` sub-object. The preserved values MUST round-trip on subsequent exports.

#### Scenario: Unknown ST fields land in `extensions.st.*`

- **WHEN** the backend decodes a card with known-unmapped fields (e.g., `post_history_instructions`, `depth_prompt`, `group_only_greetings`) or unknown V3 extension subkeys
- **THEN** the persisted record's `extensions` contains the corresponding `stPostHistoryInstructions`, `stDepthPrompt`, `stGroupOnlyGreetings`, or `st.<otherKey>` entries with the source values verbatim

#### Scenario: Round-trip export emits the same `st.*` fields

- **WHEN** an imported card is exported in the same format (PNG or JSON) with the same target language and no user edits
- **THEN** the exported payload's ST fields match the source — the `st.*` entries round-trip without loss

### Requirement: Backend exports cards as PNG with dual tEXt chunks or as JSON with version selection

The system SHALL expose `GET /api/cards/{cardId}/export?format=png|json&language=en|zh&includeTranslationAlt=<bool>`. PNG exports MUST embed both a `chara` tEXt chunk (V2-compatible subset) and a `ccv3` tEXt chunk (full V3) in the output image, and MUST include the card's avatar (or synthesize a placeholder from the card's initials if no avatar exists). JSON exports MUST default to V3 schema and MUST accept an explicit opt-in `v2_json` format mode that emits V2. The user MUST choose which `LocalizedText` side is emitted as the ST monolingual text. The user MAY opt into preserving the other-language side under `extensions.stTranslationAlt.<field>` in the exported payload; by default the other side is not emitted.

#### Scenario: PNG export embeds both tEXt chunks

- **WHEN** an authenticated client requests PNG export for a card
- **THEN** the returned PNG contains both a `chara` tEXt chunk (V2 subset) and a `ccv3` tEXt chunk (full V3), each carrying the deep persona record encoded as base64 JSON

#### Scenario: JSON export defaults to V3

- **WHEN** an authenticated client requests JSON export without specifying a version
- **THEN** the returned payload is a V3 document containing the full deep persona record

#### Scenario: V2-only JSON export omits V3-only fields

- **WHEN** an authenticated client requests JSON export with explicit `format=v2_json`
- **THEN** the returned payload conforms to the V2 schema, V3-only fields that do not round-trip are omitted, and the response includes a warning header listing the omitted fields

#### Scenario: Target language and other-language inclusion are honored

- **WHEN** an authenticated client requests export with `language=en` and `includeTranslationAlt=false`
- **THEN** the exported payload's ST monolingual prose fields contain the English side of each `LocalizedText`, and no `extensions.stTranslationAlt.*` keys are present

#### Scenario: Including translation alt adds a companion namespace

- **WHEN** an authenticated client requests export with `includeTranslationAlt=true`
- **THEN** the exported payload contains `extensions.stTranslationAlt.<field>` entries mirroring the non-target-language side of every bilingual prose field

### Requirement: Backend re-encodes imported PNGs to strip unknown chunks before storage

The system SHALL re-encode the avatar image from an imported PNG after extracting the `chara` / `ccv3` tEXt chunks, storing a clean PNG without unknown ancillary chunks. The re-encoded avatar MUST be stored as a server-side asset referenced by the card's `avatarUri`.

#### Scenario: Imported avatar is re-encoded

- **WHEN** the backend accepts a PNG import whose tEXt chunks have been decoded into a deep persona record
- **THEN** the PNG's image content is re-encoded (stripping unknown chunks and the decoded `tEXt` chunks themselves) and stored as the card's avatar asset, with a fresh `avatarUri` returned on the commit response

#### Scenario: Missing avatar yields a null avatarUri

- **WHEN** an imported card carries no avatar (e.g., a V3 card with `assets` only and no embedded PNG)
- **THEN** the persisted card has `avatarUri=null`, and exports synthesize a placeholder avatar from the card's initials when PNG format is requested
