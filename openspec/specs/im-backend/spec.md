# im-backend Specification

## Purpose
TBD - created by archiving change bootstrap-rust-im-backend. Update Purpose after archive.
## Requirements
### Requirement: Rust IM backend boots with secret-managed server configuration
The system SHALL provide a Rust backend service that runs as an HTTP and WebSocket server on the target Ubuntu host, and it MUST load PostgreSQL connection details, bind address, and other operational secrets from backend-only environment inputs instead of checked-in credentials. The published HTTP base URL used for Android emulator validation MUST resolve to a host-reachable service endpoint for auth and bootstrap traffic, and validation guidance MUST NOT assume that `127.0.0.1` inside the emulator/device runtime is automatically the backend host.

#### Scenario: Backend service boots on the Ubuntu host
- **WHEN** the backend process starts on the Ubuntu server at `124.222.15.128`
- **THEN** it reads its runtime configuration from secret-managed environment values, establishes PostgreSQL connectivity, and exposes a health-checkable service endpoint without requiring database or SSH passwords inside the repository

#### Scenario: Emulator-facing auth API target is host-reachable
- **WHEN** operators select the HTTP base URL used by the Android emulator for registration, login, and bootstrap validation
- **THEN** that URL points to a backend service endpoint that is reachable from the emulator/runtime boundary instead of relying on device-local `127.0.0.1` by assumption

### Requirement: Backend issues user-bound IM sessions for the first milestone
The system SHALL provide both a production auth flow and a development-safe session bootstrap flow for IM users, and it MUST return a stable user identity plus credentials that the same client can use for subsequent HTTP bootstrap requests and authenticated WebSocket connections.

#### Scenario: Production user obtains an IM session
- **WHEN** a registered user completes the backend credential-login flow
- **THEN** the backend returns a session or token response tied to that user identity so contacts, conversations, and WebSocket events can be scoped per user

#### Scenario: Development user obtains an IM session
- **WHEN** a supported development user completes the backend bootstrap flow
- **THEN** the backend returns a session or token response tied to that user identity so contacts, conversations, and WebSocket events can be scoped per user

### Requirement: Backend persists direct-message state in PostgreSQL
The system SHALL persist 1:1 IM state in PostgreSQL, and it MUST store enough data to reconstruct contacts, conversations, paginated message history, unread counts, delivery/read state, and optional direct-message attachment descriptors after a client disconnects. The backend MUST require that direct-message participants are mutual contacts before a new message is accepted.

#### Scenario: Client bootstraps direct-message history
- **WHEN** an authenticated user requests their conversation bootstrap or paginated history
- **THEN** the backend returns conversation metadata, unread counts, durable message records, and any associated attachment descriptors from PostgreSQL instead of transient in-memory-only state

#### Scenario: Offline recipient reconnects after missed messages
- **WHEN** a recipient reconnects after text or image messages were sent while they were offline
- **THEN** the backend can rebuild unread state, message history, and attachment descriptors for that user from PostgreSQL without message loss

#### Scenario: Message between non-contacts is rejected
- **WHEN** an authenticated user attempts to send a direct message to a user who is not a mutual contact
- **THEN** the backend rejects the message instead of silently auto-creating the contact relationship

### Requirement: Backend delivers low-latency direct messages over WebSocket
The system SHALL use authenticated WebSocket sessions for active IM delivery, and it MUST push text-message events, image-message events with attachment descriptors, delivery/read updates, and friend-request events to online clients without relying on polling for active conversations in the first single-node deployment.

#### Scenario: Online recipient receives a direct message
- **WHEN** an authenticated sender transmits a text direct message to an online direct-message recipient
- **THEN** the backend durably accepts the message and pushes the resulting event to the recipient's live WebSocket session on the same server node

#### Scenario: Online recipient receives an image direct message
- **WHEN** an authenticated sender creates an image direct message while the recipient is online
- **THEN** the backend durably accepts the image message and pushes `message.sent` / `message.received` events containing the attachment descriptor needed to render the same image on both clients

#### Scenario: Delivery/read updates reach the active conversation
- **WHEN** the recipient receives or reads a text or image message while both participants remain connected
- **THEN** the backend emits delivery or read updates through the active WebSocket sessions so the conversation state stays current without polling refreshes

#### Scenario: Friend-request events reach online users
- **WHEN** a friend request is sent, accepted, or rejected while the other party is online
- **THEN** the backend pushes the corresponding friend-request event through the active WebSocket session

### Requirement: Backend registers and authenticates credentialed IM accounts
The system SHALL provide production account registration and credential login endpoints for GKIM users in addition to the existing development-session bootstrap flow. `POST /api/auth/register` MUST validate username, password, and display-name input, persist a hashed password, and return a session token plus user profile. `POST /api/auth/login` MUST validate stored credentials and return a session token plus user profile for valid credentials.

#### Scenario: New user registers successfully
- **WHEN** a client submits `POST /api/auth/register` with a unique username, a valid password, and a valid display name
- **THEN** the backend creates the user record, stores a password hash, and returns an authenticated session payload for that user

#### Scenario: Duplicate username is rejected during registration
- **WHEN** a client submits `POST /api/auth/register` with a username that already exists
- **THEN** the backend returns a conflict response instead of creating a duplicate identity

#### Scenario: Credential login returns a user-bound IM session
- **WHEN** a registered user submits `POST /api/auth/login` with valid credentials
- **THEN** the backend returns a token tied to that user identity for subsequent bootstrap, history, and WebSocket requests

### Requirement: Backend supports authenticated user discovery and friend-request lifecycle
The system SHALL let authenticated users search for other users and manage a friend-request lifecycle before direct messaging access is granted. Search results MUST indicate relationship state, and accepted requests MUST create the reciprocal contact relationship required by direct messaging.

#### Scenario: Search returns users with relationship state
- **WHEN** an authenticated user queries `GET /api/users/search`
- **THEN** the backend returns matching users excluding the requester, along with the contact or pending-request state for each result

#### Scenario: Accepting a friend request creates mutual contacts
- **WHEN** an authenticated recipient accepts a pending friend request
- **THEN** the backend marks the request accepted and creates reciprocal contact records for both users

#### Scenario: Rejecting a friend request preserves non-contact state
- **WHEN** an authenticated recipient rejects a pending friend request
- **THEN** the backend marks the request rejected and does not create reciprocal contacts

### Requirement: Backend supports remote deployment and debugging on the target Ubuntu host
The system SHALL include a repeatable remote deployment/debug workflow for the current Ubuntu host behind `chat.lastxuans.sbs`, and it MUST let each accepted implementation slice be started, inspected, and smoke-tested on that server through SSH-accessible operational commands without committing the SSH password. That workflow MUST be satisfiable with a maintainer-held local-only or otherwise private backend checkout, and it MUST NOT require the published remote Git repository to carry backend source files. The deployed service MUST expose published HTTP and WebSocket endpoints that are suitable for Android end-to-end validation, and remote acceptance MUST include enough server-side checks to distinguish service-health problems, outdated backend binaries, and published-endpoint drift from Android client configuration problems.

#### Scenario: Accepted backend slice is smoke-tested on the server
- **WHEN** a backend implementation slice is accepted for delivery
- **THEN** the maintainers' local/private backend materials provide the scripts, service shape, or documented commands needed to deploy that slice to the Ubuntu host, inspect logs or service status, confirm the backend health endpoint, and run a remote smoke test before the next slice begins

#### Scenario: Published backend is verified to run the image-message-capable version
- **WHEN** a backend slice that includes direct image-message behavior is deployed to the Ubuntu host
- **THEN** deployment acceptance proves host-local and published support for `POST /api/direct-messages/image` plus attachment fetch, instead of stopping at generic health or bootstrap checks

#### Scenario: Deployed backend endpoints are suitable for Android validation
- **WHEN** operators publish the backend HTTP and WebSocket endpoints for Android validation against the Ubuntu host
- **THEN** those endpoints are reachable for remote auth/bootstrap, realtime traffic, and the current accepted image-message API contract without relying on local Docker port publishing, adb reverse, or SSH-tunnel-only assumptions

### Requirement: Backend accepts authenticated direct-image message uploads
The system SHALL accept authenticated direct-image message creation through a backend API that can carry binary image payloads plus optional caption text, and it MUST persist the attachment before emitting the resulting direct-message events.

#### Scenario: Sender uploads an image direct message
- **WHEN** an authenticated user submits an image message with a recipient identity, optional `clientMessageId`, and optional caption text
- **THEN** the backend stores the image attachment, creates or resolves the direct conversation, persists the message, and returns or broadcasts a message record containing the resolved conversation identifier plus attachment descriptor

#### Scenario: Conversation member downloads a stored image attachment
- **WHEN** an authenticated user who belongs to the conversation requests the stored attachment for a direct image message
- **THEN** the backend authorizes the membership check and returns the persisted image payload in a format the Android client can render

### Requirement: Backend persists the full companion persona authoring record

The system SHALL persist companion character cards with every field of the persona authoring record, and it MUST expose those fields through the roster and active-selection APIs. The authored prose fields — system prompt, personality, scenario, example dialogue, first-message greeting, and each alternate greeting — MUST be stored and returned as bilingual English/Chinese text, consistent with the existing `localized-companion-copy` contract. Tags, creator attribution, creator notes, and character version MUST be stored as plain strings. The record MUST include a forward-compatible extensions object that preserves unknown structured fields across write and read roundtrips.

#### Scenario: Roster response carries the deep persona record

- **WHEN** an authenticated client requests the companion roster or active-selection endpoints
- **THEN** the backend response exposes every persona authoring field with the bilingual contract for prose fields and returns an `extensions` object that preserves unknown structured data

#### Scenario: Legacy single-field records are backfilled before serving the deep contract

- **WHEN** the backend migration runs against companion rows authored before this contract
- **THEN** the migration backfills the new prose fields for shipped preset and drawable cards so roster responses never expose a partially populated persona record

### Requirement: Backend supports user-authored companion cards with preset immutability

The system SHALL accept create and update operations for user-authored or draw-acquired companion cards belonging to the requesting user, and it MUST reject attempts to mutate preset-sourced cards. Deletion MAY be supported for user-authored cards; it MUST NOT be supported for preset cards and SHOULD NOT be supported for draw-acquired cards in the first-authored editor flow.

#### Scenario: User creates a new companion card through the backend

- **WHEN** an authenticated client submits a new companion card tied to the requesting user with all required persona authoring fields populated
- **THEN** the backend persists the card, associates it with the requesting user, and returns it with a stable id

#### Scenario: Preset cards reject mutation

- **WHEN** any client submits an update or delete request targeting a card whose source is `Preset`
- **THEN** the backend rejects the request with an explicit failure and leaves the preset record unchanged

#### Scenario: Draw-acquired card accepts edits but rejects delete in the first-authored flow

- **WHEN** the owner of a draw-acquired card submits an edit through the character editor flow
- **THEN** the backend persists the edit; if the same client attempts to delete the same card through that flow, the backend rejects the delete with an explicit failure

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

### Requirement: Backend persists user persona library and exposes CRUD endpoints

The system SHALL persist a user persona library per account server-side and MUST expose authenticated HTTP endpoints to list, create, update, duplicate, activate, and delete personas. Persona records MUST carry bilingual `displayName`, bilingual `description`, `isBuiltIn`, `isActive`, creation and update timestamps, and a forward-compatible `extensions` JSON object. Persona state MUST survive realtime gateway reconnect, backend restart, and client relaunch.

#### Scenario: Backend returns the persona library on authenticated read

- **WHEN** an authenticated client requests the persona library
- **THEN** the backend returns every persona owned by the account with `id`, bilingual `displayName`, bilingual `description`, `isBuiltIn`, `isActive`, timestamps, and `extensions`

#### Scenario: Persona CRUD operations round-trip through the backend

- **WHEN** an authenticated client creates, updates, duplicates, or deletes a persona
- **THEN** the backend persists the change, returns the updated record (or a success ack on delete), and subsequent list reads reflect the change

#### Scenario: Persona state survives reconnect and restart

- **WHEN** the realtime gateway disconnects, the client relaunches, or the backend process restarts
- **THEN** a subsequent persona library read returns the same records produced before the interruption, with no persona dropped and no field reset

### Requirement: Backend enforces exactly one active persona per user and blocks deletion of built-in or active personas

The system SHALL persist exactly one active persona per user, MUST expose the active persona id on bootstrap, and MUST enforce activation exclusivity atomically. The backend MUST reject deletion of a persona flagged `isBuiltIn` with a typed error, and MUST reject deletion of the currently active persona with a typed error. Deletion of an inactive user-owned persona MUST succeed.

#### Scenario: Activation is atomic and exclusive

- **WHEN** an authenticated client activates a persona id that is not currently active
- **THEN** the backend deactivates the previously active persona, marks the newly selected persona active, and returns the updated active persona id, all as a single atomic operation

#### Scenario: Bootstrap response exposes the active persona

- **WHEN** an authenticated client reads bootstrap state
- **THEN** the backend returns the active persona id (and optionally its record inline), so the client can label the chat chrome without an additional round trip

#### Scenario: Deleting a built-in persona is rejected

- **WHEN** an authenticated client attempts to delete a persona flagged `isBuiltIn`
- **THEN** the backend rejects the request with a typed error indicating the persona is built-in, and the record remains unchanged

#### Scenario: Deleting the active persona is rejected

- **WHEN** an authenticated client attempts to delete the currently active persona
- **THEN** the backend rejects the request with a typed error indicating the persona is active, and the record remains unchanged

### Requirement: Backend seeds a built-in persona on first bootstrap

The system SHALL create a built-in persona on first bootstrap for each account, seeding bilingual `displayName` from the account's `displayName` (both language sides initially identical) and bilingual `description` from a neutral placeholder. The seeded persona MUST have `isBuiltIn=true` and `isActive=true`. Re-bootstrapping MUST NOT create duplicates.

#### Scenario: First bootstrap seeds the built-in persona

- **WHEN** an account completes its first bootstrap
- **THEN** the persona library contains exactly one persona flagged `isBuiltIn=true` and `isActive=true`, whose bilingual `displayName` matches the account's `displayName` and whose bilingual `description` contains a neutral placeholder

#### Scenario: Subsequent bootstraps do not duplicate the built-in

- **WHEN** an already-bootstrapped account reconnects or restarts
- **THEN** the persona library still contains exactly one persona flagged `isBuiltIn=true`, and no duplicate built-in personas exist

### Requirement: Backend substitutes user and char macros in every assembled prompt

The system SHALL replace every occurrence of `{{user}}`, `{user}`, and `<user>` in an assembled companion turn prompt with the active persona's `displayName` in the active `AppLanguage`, and every occurrence of `{{char}}`, `{char}`, and `<char>` with the active companion card's `displayName` in the active `AppLanguage`. Substitution MUST happen before the prompt is sent to the provider. Stored message bodies MUST NOT be rewritten by this substitution step. Unknown macro-like tokens MUST be left as literal text.

#### Scenario: Assembled prompt substitutes user and char macros in all six forms

- **WHEN** the backend assembles a companion turn prompt containing any of the six macro forms
- **THEN** each macro occurrence is replaced with the active persona's display name (for user) or the active card's display name (for char) in the active `AppLanguage`, and the provider never receives a raw macro token of these six forms

#### Scenario: Stored message bodies retain raw macros

- **WHEN** a companion turn completes and the assembled prompt body that was sent to the provider contained substituted display names
- **THEN** the stored user message body and the stored companion reply body retain the raw macro tokens (if the user had included them); the substitution produced only the ephemeral prompt sent to the provider

#### Scenario: Unknown macros pass through

- **WHEN** an assembled prompt contains a macro-like token that is not one of the six supported forms
- **THEN** the backend leaves the token unchanged when sending to the provider

### Requirement: Backend injects the active persona description into the prompt allocator with documented priority and drop position

The system SHALL include the active persona's `description` as a dedicated section (`userPersonaDescription`) in the deterministic token-budget allocator introduced by `companion-memory-and-preset`. The section's priority MUST place it above the rolling summary and below pinned facts. The drop order MUST place it between the rolling summary and the non-critical preset sections (`formatInstructions`, `systemSuffix`). The `{{user}}` substitution MUST continue to resolve correctly even when the description section has been dropped.

#### Scenario: Persona description is included when budget allows

- **WHEN** the backend assembles a companion turn prompt with the active persona having a non-blank description and the allocator is not over budget
- **THEN** the prompt contains the `userPersonaDescription` section between the pinned facts (above) and the rolling summary (below)

#### Scenario: Persona description is dropped before non-critical preset sections

- **WHEN** the allocator must drop sections to fit the provider budget
- **THEN** the allocator drops `userPersonaDescription` after the rolling summary has already been dropped and before any non-critical preset section (`formatInstructions`, `systemSuffix`) is dropped

#### Scenario: User display name substitution survives description drop

- **WHEN** the allocator has dropped `userPersonaDescription` for budget reasons
- **THEN** the `{{user}}` substitution continues to resolve to the active persona's display name in the active `AppLanguage`, and the provider still receives the substituted name in any remaining section (including the preserved user turn)

### Requirement: Backend exposes authenticated CRUD for lorebooks, entries, and bindings

The system SHALL expose authenticated HTTP endpoints for lorebook lifecycle: `GET /api/lorebooks`, `POST /api/lorebooks`, `GET /api/lorebooks/{id}`, `PATCH /api/lorebooks/{id}`, `DELETE /api/lorebooks/{id}`, `POST /api/lorebooks/{id}/duplicate`, entry CRUD under `/api/lorebooks/{id}/entries/*`, and binding CRUD under `/api/lorebooks/{id}/bindings/*`. Every record MUST be owned by the authenticated user. Deleting a lorebook that carries at least one binding MUST fail with a typed error code `lorebook_has_bindings` until the bindings are removed.

#### Scenario: CRUD endpoints are scoped to the authenticated user

- **WHEN** an authenticated client calls `GET /api/lorebooks`
- **THEN** the response lists only lorebooks owned by the authenticated user

#### Scenario: Delete rejected when bindings exist

- **WHEN** a client calls `DELETE /api/lorebooks/{id}` on a lorebook that has at least one `LorebookBinding`
- **THEN** the backend rejects the request with `errorCode = "lorebook_has_bindings"` and the lorebook is not deleted

#### Scenario: Entry CRUD is scoped to the parent lorebook

- **WHEN** an authenticated client calls `GET /api/lorebooks/{id}/entries`, `POST /api/lorebooks/{id}/entries`, `PATCH /api/lorebooks/{id}/entries/{entryId}`, or `DELETE /api/lorebooks/{id}/entries/{entryId}` on a lorebook they own
- **THEN** the backend returns only entries whose `lorebookId` matches `{id}`, rewrites the created entry's `lorebookId` to `{id}` regardless of the request body's value, and rejects the request with a typed `not_found` error when the lorebook id does not belong to the authenticated user

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

### Requirement: Backend exposes a developer-only debug scan endpoint gated on a dev-access header

The system SHALL expose `POST /api/debug/worldinfo/scan` accepting a `{ characterId, scanText }` body and returning the set of entries that would match if the scan were executed against the authenticated user's lorebooks plus the character's bound lorebooks, with each match carrying `entryId`, `lorebookId`, `insertionOrder`, `constant`, and the literal `matchedKey` that triggered it (null for constant entries). The endpoint MUST require an `X-GKIM-Debug-Access` header in addition to the bearer token, MUST reject requests missing or carrying an incorrect dev-access header with HTTP 403, and SHALL only be enabled when the backend is configured to allow debug traffic. Clients SHOULD gate calls on `BuildConfig.DEBUG` so release builds never dispatch a request.

#### Scenario: Debug scan returns entries in the same total order the allocator would use

- **WHEN** a debug scan request is evaluated
- **THEN** the response matches are totally ordered by `(insertionOrder asc, lorebookId asc, entryId asc)` matching the production scan's total order so the debug output is a faithful preview

#### Scenario: Debug scan rejects callers without the dev-access header

- **WHEN** a client calls `POST /api/debug/worldinfo/scan` without the `X-GKIM-Debug-Access` header or with an incorrect value
- **THEN** the backend responds with HTTP 403 and does not expose any scan data

### Requirement: Backend accepts companion turn submission and regeneration over HTTP

The system SHALL accept authenticated companion turn submission over HTTP and MUST return a stable `turnId` plus the conversation-tree anchor (`parentMessageId`, `variantGroupId`, `variantIndex`) for the newly created turn. A matching HTTP endpoint MUST accept regeneration requests that append a new sibling variant under an existing `variantGroupId`. Both endpoints MUST honor a client-supplied idempotency key so retries do not produce duplicate variants.

#### Scenario: Client submits a companion turn

- **WHEN** an authenticated client submits a companion turn with `conversationId`, `activeCompanionId`, `userTurnBody`, `activeLanguage`, `clientTurnId`, and `parentMessageId`
- **THEN** the backend persists the user message, starts the companion reply lifecycle, and returns `turnId` plus the created variant's tree anchor in the response

#### Scenario: Regenerate appends a sibling variant

- **WHEN** an authenticated client posts a regenerate request referencing an existing `turnId` with a fresh `clientTurnId`
- **THEN** the backend appends a new sibling variant under the same `variantGroupId`, starts its lifecycle, and returns the new variant's identifier

#### Scenario: Duplicate submit with same idempotency key is absorbed

- **WHEN** an authenticated client retries a submit or regenerate with the same `clientTurnId` that was already accepted
- **THEN** the backend returns the previously created turn identifier instead of creating a duplicate variant

### Requirement: Backend emits companion reply lifecycle over WebSocket with monotonic deltas

The system SHALL emit companion turn lifecycle events over the existing authenticated WebSocket gateway, and it MUST cover `companion_turn.started`, `companion_turn.delta`, `companion_turn.completed`, `companion_turn.failed`, and `companion_turn.blocked` event types. Each `delta` event MUST carry a monotonic `deltaSeq` per `turnId` and a `textDelta` chunk, and the backend MUST NOT emit additional lifecycle events for a turn after a terminal event.

#### Scenario: Online client receives lifecycle deltas in monotonic order

- **WHEN** an authenticated client is connected and a companion turn is streaming for its user
- **THEN** the gateway emits `companion_turn.delta` events whose `deltaSeq` values increase monotonically per `turnId` until the terminal event fires

#### Scenario: Terminal events stop further deltas

- **WHEN** the backend emits `companion_turn.completed`, `companion_turn.failed`, or `companion_turn.blocked` for a given `turnId`
- **THEN** no subsequent lifecycle events are emitted for that `turnId`

### Requirement: Backend persists companion turns as a variant tree

The system SHALL persist each companion turn as a sibling in a variant group keyed by `variantGroupId`, and it MUST preserve every prior variant when a regeneration request appends a new one. The persisted history MUST support resolving an active path through the tree (one selected sibling per variant group) and MUST return variants alongside user messages when the client requests conversation history.

#### Scenario: Variants persist across regenerations

- **WHEN** a user regenerates a companion reply one or more times
- **THEN** the persisted conversation history retains every variant under the same `variantGroupId`, and none of the prior variants is destroyed

#### Scenario: History response exposes the variant tree

- **WHEN** an authenticated client loads conversation history for a companion conversation
- **THEN** the response includes every variant with its `variantGroupId`, `variantIndex`, `parentMessageId`, and lifecycle status so the client can reconstruct the tree and the active path

### Requirement: Backend assembles persona prompts with user substitution and language steering

The system SHALL build the LLM prompt for a companion turn server-side from the active card's `systemPrompt`, `personality`, `scenario`, `exampleDialogue`, and chosen greeting, using the variant matching the user's active `AppLanguage`. The backend MUST substitute `{{user}}`, `{user}`, and `<user>` macros with the authenticated user's display name before invoking the provider, and it MUST apply soft language steering toward the requested `activeLanguage`.

#### Scenario: Deep persona fields flow into the assembled prompt

- **WHEN** the backend builds a companion turn prompt
- **THEN** the prompt includes the active card's bilingual `systemPrompt`, `personality`, `scenario`, and `exampleDialogue` using the variant that matches the submitted `activeLanguage`

#### Scenario: Macro substitution replaces user tokens before provider call

- **WHEN** an assembled prompt contains `{{user}}`, `{user}`, or `<user>`
- **THEN** the backend replaces those tokens with the authenticated user's display name before calling the provider

#### Scenario: Language steering accompanies every turn

- **WHEN** the client submits a companion turn with `activeLanguage` set
- **THEN** the assembled prompt includes an instruction that steers the companion reply toward that language

### Requirement: Backend exposes pending companion turn recovery

The system SHALL expose an authenticated HTTP endpoint that returns all currently pending companion turns for the requesting user, and it MUST expose a per-turn snapshot endpoint that returns the turn's current text and lifecycle status for reconnect refresh. A pending turn MUST remain durable enough to survive a realtime gateway reconnect.

#### Scenario: Client retrieves the list of pending turns

- **WHEN** an authenticated client requests the pending companion turns list
- **THEN** the backend returns every companion turn in `Thinking` or `Streaming` state that belongs to the requesting user, each with its current lifecycle status and the last `deltaSeq` the backend emitted

#### Scenario: Client snapshots a single turn after a delta gap

- **WHEN** an authenticated client requests a per-turn snapshot during a delta gap
- **THEN** the backend returns the turn's current accumulated text, lifecycle status, and latest `deltaSeq` so the client can replay into its reducer without silent corruption

### Requirement: Backend bounds companion turn lifetime and surfaces typed safety blocks

The system SHALL bound every companion turn with both a total-duration and an idle-duration limit, terminating the turn with `Timeout` when either bound fires. The system SHALL return a typed block reason with every `Blocked` terminal event and MUST keep the neutral product-level reason distinguishable from arbitrary provider error strings.

#### Scenario: Total duration or idle interval triggers timeout

- **WHEN** a companion turn exceeds the total-duration bound or the idle-delta bound
- **THEN** the backend terminates the turn with `Timeout`, emits the corresponding event, and ceases further deltas

#### Scenario: Safety block carries a typed reason

- **WHEN** safety policy or provider refusal prevents a companion reply from completing
- **THEN** the backend emits `companion_turn.blocked` with a typed reason from a known reason set, and the reason is carried alongside the event for client rendering

### Requirement: Backend persists and returns bilingual companion card content
The system SHALL persist English and Chinese authored values for each companion character's `displayName`, `roleLabel`, `summary`, and `openingLine`, and it MUST expose both language variants through the companion roster and draw-result APIs instead of collapsing companion content into one locale-specific string.

#### Scenario: Authenticated roster load includes both language variants
- **WHEN** an authenticated user requests the companion roster
- **THEN** each preset and owned companion card in the response includes both English and Chinese values for every required user-facing character field

#### Scenario: Draw response includes both language variants
- **WHEN** an authenticated user performs a companion draw
- **THEN** the returned draw-result card includes both English and Chinese values for every required user-facing character field

### Requirement: Published companion catalogs are backfilled to the bilingual contract
The system SHALL migrate existing shipped companion catalog rows to the bilingual companion-copy contract before serving them from published APIs, and it MUST NOT leave a shipped preset or draw-pool character with only one authored language variant after the migration is accepted.

#### Scenario: Existing seeded companion rows are migrated to bilingual content
- **WHEN** the backend upgrades the existing companion catalog created from the original single-language tavern rollout
- **THEN** each shipped preset and draw-pool character becomes retrievable with complete English and Chinese values for the required companion fields

### Requirement: Backend persists character catalogs, owned roster state, and active selection
The system SHALL persist the preset companion catalog, per-user draw-acquired角色 ownership, and the user’s active selected角色 on the backend, and it MUST keep that state durable across reconnects and session recovery.

#### Scenario: User loads the tavern roster
- **WHEN** an authenticated user opens the tavern-style character surface
- **THEN** the backend can provide the preset catalog, the user’s owned roster, and the currently active selected角色 for that account

#### Scenario: User changes the active角色
- **WHEN** an authenticated user selects a preset or owned角色 as the active companion
- **THEN** the backend records that active selection durably so subsequent companion conversation startup uses the chosen persona

### Requirement: Backend records explicit draw outcomes for companion角色 cards
The system SHALL support a draw operation that yields a companion角色 outcome for the authenticated user, and it MUST record the result explicitly enough that the client can represent the draw result and the updated owned-roster state truthfully.

#### Scenario: User performs a角色 draw
- **WHEN** an authenticated user triggers the character draw flow
- **THEN** the backend returns the draw result together with the resulting ownership outcome needed to update the user’s roster

#### Scenario: Drawn角色 becomes available to the conversation system
- **WHEN** a user receives a角色 card through the draw flow and activates it
- **THEN** the backend makes that persona available as a valid companion identity for the ensuing conversation lifecycle

### Requirement: Backend persists per-companion memory as rolling summary plus pinned facts

The system SHALL persist companion memory state server-side, keyed on the `(userId, companionCardId)` pair, and it MUST model that state as a rolling natural-language summary plus an ordered list of pinned facts. Memory state MUST survive realtime gateway reconnect, backend restart, and client relaunch. The backend MUST expose an authenticated HTTP endpoint that returns the full memory record for a given companion.

#### Scenario: Backend returns memory state on authenticated read

- **WHEN** an authenticated client requests the memory record for a companion card id the user has interacted with
- **THEN** the backend returns the rolling summary (bilingual prose), its last-updated timestamp and turn cursor, and the ordered list of pinned facts with their ids, bilingual text bodies, creation timestamps, and optional `sourceMessageId` references

#### Scenario: Memory state survives reconnect and restart

- **WHEN** the realtime gateway disconnects, the client relaunches, or the backend process restarts
- **THEN** a subsequent memory read returns the same record produced before the interruption, with no loss of the summary or any pinned fact

### Requirement: Backend exposes pin CRUD scoped per companion

The system SHALL expose authenticated HTTP endpoints for creating, listing, updating, and deleting pinned facts scoped to a `(userId, companionCardId)` pair. Pin creation MUST accept a nullable `sourceMessageId` referencing any message within the user's companion conversation, including non-active-path variants. Pin updates MUST accept a new bilingual text body; pin deletes MUST remove only the target pin and MUST NOT touch the rolling summary or the transcript.

#### Scenario: Client creates a pin from a specific message

- **WHEN** an authenticated client creates a pin with a `sourceMessageId` referencing one of its messages and a bilingual text body
- **THEN** the backend persists the pin with a fresh id and creation timestamp, returns the stored record, and makes the pin visible on subsequent memory reads

#### Scenario: Client creates a pin manually

- **WHEN** an authenticated client creates a pin with no `sourceMessageId` and a bilingual text body
- **THEN** the backend persists the pin with `sourceMessageId` null, returns the stored record, and makes the pin visible on subsequent memory reads

#### Scenario: Pin update and delete affect only the target pin

- **WHEN** an authenticated client updates or deletes a pin
- **THEN** the backend applies the change to that pin id only, returns the updated record (or a success ack on delete), and leaves the rolling summary, other pins, and the transcript unchanged

### Requirement: Backend exposes three memory reset scopes

The system SHALL expose an authenticated HTTP endpoint that resets memory for a `(userId, companionCardId)` pair at one of three scopes: pinned facts only, summary only, or all memory. Every reset MUST leave the conversation transcript unchanged. A reset of the summary scope MUST also reset the `summaryTurnCursor` so the next completed turn retriggers summarization.

#### Scenario: Pins-only reset leaves summary intact

- **WHEN** an authenticated client invokes memory reset with scope "pins"
- **THEN** the backend deletes every pinned fact for that `(userId, companionCardId)` pair, leaves the rolling summary and its turn cursor unchanged, leaves the transcript unchanged, and returns the updated memory record

#### Scenario: Summary-only reset leaves pins intact

- **WHEN** an authenticated client invokes memory reset with scope "summary"
- **THEN** the backend wipes the rolling summary, resets `summaryTurnCursor` to zero, leaves every pinned fact unchanged, leaves the transcript unchanged, and returns the updated memory record

#### Scenario: All-memory reset leaves transcript intact

- **WHEN** an authenticated client invokes memory reset with scope "all"
- **THEN** the backend deletes every pinned fact, wipes the rolling summary, resets `summaryTurnCursor` to zero, leaves the transcript unchanged, and returns the updated memory record

### Requirement: Backend persists preset library with built-in seeding and user-owned CRUD

The system SHALL persist a preset library server-side, seeded idempotently with at least three built-in presets (a neutral default, a roleplay-immersive preset, and a concise-companion preset) on every boot. The backend MUST expose authenticated HTTP endpoints to list, create, update, duplicate, delete, and activate presets, and it MUST reject mutation of built-in presets (edit and delete) with a typed error. Each preset record MUST carry four bilingual template sections (`systemPrefix`, `systemSuffix`, `formatInstructions`, `postHistoryInstructions`), three nullable provider parameters (`temperature`, `topP`, `maxReplyTokens`), an `extensions` object for forward-compatible fields, and the `isBuiltIn` flag.

#### Scenario: Built-in presets are seeded idempotently

- **WHEN** the backend process boots or restarts
- **THEN** the preset library contains exactly the three built-in presets at their canonical shapes, and repeated boots do not produce duplicate built-in records

#### Scenario: Built-in preset mutation is rejected

- **WHEN** an authenticated client attempts to edit or delete a preset flagged `isBuiltIn`
- **THEN** the backend rejects the request with a typed error indicating the preset is built-in, and the preset record is unchanged

#### Scenario: User-owned preset CRUD succeeds

- **WHEN** an authenticated client creates, updates, duplicates, or deletes a user-owned preset
- **THEN** the backend applies the change, returns the updated record (or a success ack on delete), and subsequent list reads reflect the change

### Requirement: Backend enforces exactly one active preset per user

The system SHALL persist exactly one active preset per user and MUST expose the active preset id on bootstrap. Activation of a different preset MUST deactivate the previously active preset atomically. Deletion of the currently active preset MUST be rejected with a typed error; the client must switch active first.

#### Scenario: Activation is atomic and exclusive

- **WHEN** an authenticated client activates a preset id that is not currently active
- **THEN** the backend deactivates the previously active preset, marks the newly selected preset active, and returns the updated active-preset id, all as a single atomic operation

#### Scenario: Bootstrap exposes the active preset id

- **WHEN** an authenticated client reads the active preset on bootstrap
- **THEN** the backend returns the active preset record (or a default id when no user-owned active has been selected yet), so the client can label the chat chrome accordingly

#### Scenario: Deleting the active preset is blocked

- **WHEN** an authenticated client attempts to delete the currently active preset
- **THEN** the backend rejects the request with a typed error indicating the preset is active, and the preset record is unchanged

### Requirement: Backend assembles companion turn prompts with the active preset plus memory under a deterministic token budget

The system SHALL integrate memory state and the user's active preset into the companion turn prompt assembler introduced by `llm-text-companion-chat`. The assembler MUST compose the active preset's four template sections, the persona fields from the active card, pinned facts, the rolling summary, the recent-N turns, and the current user turn into a single prompt, respecting a deterministic priority order. When the provider budget would be exceeded, the assembler MUST drop sections in a fixed drop order (example dialogue first, then older recent turns, then the rolling summary, then non-critical preset sections) and MUST NOT drop pinned facts, persona `systemPrompt`, the preset `systemPrefix`, or the current user turn. If the current user turn alone exceeds the budget, the assembler MUST terminate the turn with `Failed` and reason `prompt_budget_exceeded`.

#### Scenario: Assembled prompt incorporates active preset and memory alongside persona

- **WHEN** the backend assembles a companion turn prompt for a user with a non-default active preset and a non-empty memory record
- **THEN** the prompt contains the active preset's four template sections at their priority-ordered slots, the persona fields from the active card, the pinned facts, and the rolling summary, in addition to the recent-N turns and the current user turn

#### Scenario: Over-budget assembly drops sections in fixed order

- **WHEN** the running prompt would exceed the provider budget during assembly
- **THEN** the assembler drops sections in a fixed documented order (starting with persona `exampleDialogue`, then older recent turns, then the rolling summary, then non-critical preset sections), and pinned facts, persona `systemPrompt`, preset `systemPrefix`, and the current user turn are preserved

#### Scenario: User turn alone exceeds the budget terminates with a typed reason

- **WHEN** the current user turn alone cannot fit the provider budget even after the full drop order has been applied
- **THEN** the backend terminates the turn with `Failed` and a typed reason `prompt_budget_exceeded` rather than silently truncating the user's input

### Requirement: Backend regenerates the rolling summary asynchronously on a deterministic trigger

The system SHALL regenerate the rolling summary on either of two triggers: the number of completed turns since the last regen has reached a configured threshold, or the assembler projected that the current prompt would exceed a configured soft cap of the provider budget. Summarization MUST run asynchronously and MUST NOT block the in-flight companion turn. If the summarizer call fails, the system MUST retain the prior summary and MUST NOT surface the failure as a user-visible error on the turn in flight.

#### Scenario: Summary regen is triggered by turn threshold

- **WHEN** a companion turn completes and the completed-turns-since-last-regen count reaches the configured threshold
- **THEN** the backend enqueues an asynchronous summary regeneration for that `(userId, companionCardId)` pair, and the turn's response is not delayed by the enqueue

#### Scenario: Summary regen is triggered by budget pressure

- **WHEN** the assembler projects that the running prompt will exceed the configured soft cap
- **THEN** the backend enqueues an asynchronous summary regeneration and proceeds with the in-flight turn using the current summary

#### Scenario: Summarizer failure preserves prior memory

- **WHEN** the asynchronous summarizer call fails for any reason
- **THEN** the prior rolling summary and `summaryTurnCursor` remain unchanged, the failure is logged at warning level, and no user-visible error is produced for the in-flight or next companion turn

### Requirement: Backend emits companion-turn block events with a closed set of typed wire-key reasons

The system SHALL tag every `companion_turn.blocked` event with a `reason` field whose value is drawn from the closed set `{"self_harm", "illegal", "nsfw_denied", "minor_safety", "provider_refusal", "other"}`. The backend MUST map every upstream provider safety signal, refusal, or policy-determined block into one of those six keys; it MUST NOT emit any other wire key for block reasons. Clients MAY expect the set to grow in future versions, but any new key MUST be added additively with a corresponding Android enum update, never introduced silently. The Android client maps each key to a bilingual localized copy and the "Compose a new message" action (no retry) per the matrix in `openspec/changes/companion-settings-and-safety-reframe/design.md` § 3 "Per-terminal bubble copy + actions"; that document is the authoritative cross-reference for UI-side interpretation of these wire keys.

#### Scenario: Provider refusal maps to `provider_refusal`

- **WHEN** the upstream provider emits a refusal response (for any reason the provider does not further classify)
- **THEN** the backend emits `companion_turn.blocked` with `reason = "provider_refusal"`

#### Scenario: Policy-determined self-harm signal maps to `self_harm`

- **WHEN** the backend's safety policy classifies the user turn or the provider's candidate reply as self-harm-related
- **THEN** the backend emits `companion_turn.blocked` with `reason = "self_harm"`

#### Scenario: Unclassified blocks land on `other`

- **WHEN** a block occurs that the backend cannot confidently map to any of the first five keys
- **THEN** the backend emits `companion_turn.blocked` with `reason = "other"` rather than inventing a new wire key

### Requirement: Backend emits companion-turn failure events with a closed set of typed subtype wire keys

The system SHALL tag every `companion_turn.failed` event with a `subtype` field whose value is drawn from the closed set `{"transient", "prompt_budget_exceeded", "authentication_failed", "provider_unavailable", "network_error", "unknown"}`. The backend MUST choose the most specific subtype that applies to the failure cause and MUST NOT emit any other wire key; unclassifiable failures MUST emit `"unknown"`. The Android client maps each subtype to per-subtype bilingual copy and a typed action set — `transient`/`unknown` → Retry, `prompt_budget_exceeded`/`authentication_failed` → Edit user turn (no retry), `provider_unavailable`/`network_error` → Retry-with-connection-hint — as captured in `openspec/changes/companion-settings-and-safety-reframe/design.md` § 3 "Per-terminal bubble copy + actions", which is the authoritative cross-reference for the UI behavior driven by these wire keys.

#### Scenario: Prompt budget exhaustion maps to `prompt_budget_exceeded`

- **WHEN** the deterministic token allocator cannot fit the required prompt sections under the configured budget
- **THEN** the backend emits `companion_turn.failed` with `subtype = "prompt_budget_exceeded"`

#### Scenario: Provider auth error maps to `authentication_failed`

- **WHEN** the upstream provider returns an authentication error (missing, invalid, or revoked credentials)
- **THEN** the backend emits `companion_turn.failed` with `subtype = "authentication_failed"`

#### Scenario: Upstream provider outage maps to `provider_unavailable`

- **WHEN** the upstream provider is unreachable or returns a 5xx availability error outside the network layer
- **THEN** the backend emits `companion_turn.failed` with `subtype = "provider_unavailable"`

#### Scenario: Network-layer failure maps to `network_error`

- **WHEN** the failure originates at the TCP/TLS/DNS/proxy layer between the backend and the upstream provider
- **THEN** the backend emits `companion_turn.failed` with `subtype = "network_error"`

#### Scenario: Generic retryable failure maps to `transient`

- **WHEN** the failure is classified as retryable but cannot be attributed to a more specific subtype
- **THEN** the backend emits `companion_turn.failed` with `subtype = "transient"`

#### Scenario: Unclassifiable failure maps to `unknown`

- **WHEN** the failure cannot be mapped to any of the first five subtypes with confidence
- **THEN** the backend emits `companion_turn.failed` with `subtype = "unknown"` rather than inventing a new wire key

### Requirement: Backend honors a retry hint that extends the idle bound on timed-out companion turns

The system SHALL accept a retry hint on a re-submitted companion turn that carries `retryReason = "timeout"`. When present, the backend MUST extend the idle bound used to detect no-streaming timeouts by 50% relative to the default for that turn. The hint MUST apply only to the single retried turn and MUST NOT persist across subsequent turns.

#### Scenario: Timeout-retry hint extends the idle bound by 50%

- **WHEN** the client re-submits a turn with `retryReason = "timeout"`
- **THEN** the backend uses an idle bound equal to 1.5× the default idle bound for that single turn

#### Scenario: The extended bound does not leak into later turns

- **WHEN** the timeout-retried turn terminates (Completed, Failed, Blocked, or Timeout)
- **THEN** subsequent companion turns for the same session use the default idle bound, not the extended one

### Requirement: Backend persists per-account content-policy acknowledgment with version gating

The system SHALL expose an authenticated HTTP endpoint pair for content-policy acknowledgment: `GET /api/account/content-policy-acknowledgment` returning the current acknowledgment state (`{ version, acceptedAt }` or the empty state) and `POST /api/account/content-policy-acknowledgment` accepting `{ version }` and persisting the acknowledgment keyed on the authenticated account. The backend MUST also expose the current required policy version in a field clients can compare against the persisted acknowledgment version.

#### Scenario: GET returns the persisted acknowledgment or the empty state

- **WHEN** a client calls `GET /api/account/content-policy-acknowledgment` on an authenticated account
- **THEN** the response body carries `{ version, acceptedAt }` when an acknowledgment is persisted, or an empty-state indicator when no acknowledgment has been recorded, alongside the current required policy version

#### Scenario: POST records acknowledgment keyed per account

- **WHEN** an authenticated client calls `POST /api/account/content-policy-acknowledgment` with `{ version }` set to the current required policy version
- **THEN** the backend persists `{ accountId, version, acceptedAt }`, overwriting any prior acknowledgment for that account, and returns the persisted record

#### Scenario: Version bump invalidates prior acknowledgment

- **WHEN** the current required policy version is higher than the persisted `version` for an account
- **THEN** the `GET` response surfaces the mismatch so clients can re-gate the tavern entry until a fresh `POST` is recorded

#### Scenario: Rejected acknowledgment version

- **WHEN** a client `POST`s an acknowledgment with a `version` field that is not the current required policy version
- **THEN** the backend rejects the request with a typed error code so the client can refresh and re-prompt the user against the current policy

### Requirement: Backend orchestrates durable AI companion conversations
The system SHALL orchestrate AI companion dialogue on the backend, and it MUST persist companion identity, per-user conversation state, bounded memory summaries, and pending turn lifecycle in durable storage. The backend MUST keep companion conversation state isolated per user so one user’s relationship context or memory does not leak into another user’s thread.

#### Scenario: User sends a turn to an AI companion
- **WHEN** an authenticated user submits a message to an AI companion conversation
- **THEN** the backend records the user turn, resolves the companion identity and current memory context, and starts the companion reply lifecycle as durable server-side work

#### Scenario: User resumes a companion thread after interruption
- **WHEN** the user reconnects while a companion turn or memory update was previously in progress
- **THEN** the backend can restore the pending or completed turn lifecycle and current memory summary from durable state

#### Scenario: Companion memory remains isolated per user
- **WHEN** multiple users converse with the same companion persona
- **THEN** the backend keeps memory and relationship state scoped to each user-companion pair instead of sharing private conversation context across accounts

### Requirement: Backend emits realtime companion response lifecycle events
The system SHALL deliver AI companion reply lifecycle over the existing realtime IM boundary, and it MUST emit enough event states for the Android client to represent thinking, partial or progressive reply state, final response completion, and explicit failure or blocked outcomes.

#### Scenario: Online client receives in-progress and final companion reply states
- **WHEN** a companion response is generated for an online user
- **THEN** the backend emits the reply lifecycle events needed for the client to render both in-progress and completed companion response states

#### Scenario: Companion generation fails or is blocked
- **WHEN** provider execution, safety policy, or orchestration logic prevents a companion reply from completing
- **THEN** the backend emits an explicit failure or blocked outcome instead of leaving the client waiting on an apparently healthy reply that will never arrive

### Requirement: Backend exposes an edit-user-turn endpoint that creates a new user-message sibling

The system SHALL expose an authenticated endpoint `POST /api/companion-turns/{conversationId}/edit` accepting `{ parentMessageId, newUserText }`. The endpoint MUST create a new `ChatMessage` with `role = user`, `parentMessageId` matching the request, a newly-allocated `variantGroupId`, and `variantIndex = 0` inside that group. The endpoint MUST return the new user sibling and kick off a companion turn for that sibling through the existing turn lifecycle (`companion_turn.started` → `delta` → `completed`/`failed`/`blocked`/`timeout`). The original siblings MUST be preserved.

#### Scenario: Edit creates a new variant group under the parent

- **WHEN** an authenticated client `POST`s to `/api/companion-turns/{conversationId}/edit` with a valid `parentMessageId` and non-empty `newUserText`
- **THEN** the response body carries the new user sibling with a fresh `variantGroupId` and `variantIndex = 0`; the backend starts the companion turn lifecycle for that sibling

#### Scenario: Edit preserves the original variant group

- **WHEN** an edit is submitted for a parent that already has a user sibling
- **THEN** the prior sibling and its descendants remain persisted and addressable; the conversation's active-path map is updated to point at the new sibling

#### Scenario: Edit drives the companion turn lifecycle for the new sibling

- **WHEN** an edit succeeds and the backend kicks off the companion turn for the new user sibling
- **THEN** the WS gateway emits `companion_turn.started` for the new sibling's `messageId`, followed by zero or more `companion_turn.delta` events, ending in exactly one terminal event (`completed` / `failed` / `blocked` / `timeout`); siblings under the original variant group continue to play any in-flight events without interruption

### Requirement: Backend extends the regenerate endpoint to accept `targetMessageId` for arbitrary-layer regenerate

The system SHALL extend `POST /api/companion-turns/{conversationId}/regenerate` to accept an optional `targetMessageId` field. When present, the endpoint MUST create a new companion-message sibling under the same `variantGroupId` as the target message and kick off the companion turn lifecycle for the new sibling. When absent, the endpoint MUST behave as before (regenerate the most recent companion turn). The new sibling MUST become the active index on its variant group; prior siblings MUST be preserved.

#### Scenario: Regenerate with targetMessageId appends a sibling at that layer

- **WHEN** an authenticated client `POST`s to the regenerate endpoint with `{ targetMessageId = "msg-42" }` for a mid-conversation companion message
- **THEN** the response carries a new sibling under `msg-42`'s `variantGroupId`; its `variantIndex` is the next available index in the group; the active-path map updates

#### Scenario: Regenerate without targetMessageId preserves legacy behavior

- **WHEN** an authenticated client `POST`s to the regenerate endpoint without a `targetMessageId` field
- **THEN** the endpoint regenerates the most recent companion turn, identical to the pre-extension behavior from `llm-text-companion-chat`

#### Scenario: Prior siblings under the target's variant group remain addressable

- **WHEN** a regenerate appends a new sibling to a variant group that already has two siblings
- **THEN** all three siblings remain persisted and addressable by `messageId`; only the active-path index moves to the new sibling

### Requirement: Backend persists `characterPresetId` on the character record and honors it during prompt assembly

The system SHALL persist an optional `characterPresetId: String?` field on every character record. When a conversation's active character carries a non-null `characterPresetId`, the deterministic allocator MUST use that preset (resolved against the user's preset library) instead of the user's globally-active preset for that conversation's turns. If the referenced preset has been deleted or is not owned by the user, the allocator MUST fall back to the globally-active preset and log a typed warning for that conversation.

#### Scenario: Override resolves against the user's preset library

- **WHEN** a conversation's active character has `characterPresetId = "preset-X"` and `preset-X` exists in the user's preset library
- **THEN** the allocator uses `preset-X`'s template + params for prompt assembly on that conversation's turns

#### Scenario: Dangling override falls back with a warning

- **WHEN** `characterPresetId = "preset-X"` references a preset that has been deleted
- **THEN** the allocator falls back to the user's globally-active preset and emits a typed warning naming the missing preset id

#### Scenario: Null override leaves the global preset in effect

- **WHEN** a conversation's active character has `characterPresetId = null`
- **THEN** the allocator uses the user's globally-active preset for that conversation's turns, with no warning emitted

### Requirement: Backend exposes a conversation export endpoint returning JSONL with active-path or full-tree selection

The system SHALL expose `GET /api/conversations/{conversationId}/export?format=jsonl&pathOnly=true|false` returning the conversation serialized as JSON Lines. Each line MUST be a JSON object carrying `messageId`, `parentMessageId`, `variantGroupId`, `variantIndex`, `role`, `timestamp`, `content`, and the message's `extensions` bag. When `pathOnly=true`, the payload MUST include only messages along the active path. When `pathOnly=false`, the payload MUST include every message in the conversation regardless of active path.

#### Scenario: Active-path-only export emits ordered path messages

- **WHEN** an authenticated client calls the endpoint with `pathOnly=true` on a conversation whose active path contains five messages
- **THEN** the body carries five lines of JSON, one per path message, ordered from root to most-recent

#### Scenario: Full-tree export emits every message

- **WHEN** the same client calls the endpoint with `pathOnly=false` on the same conversation
- **THEN** the body carries one line per message in the conversation, including non-active siblings

#### Scenario: Each JSONL line carries the documented field shape

- **WHEN** any export response line is parsed
- **THEN** the JSON object exposes `messageId`, `parentMessageId`, `variantGroupId`, `variantIndex`, `role`, `timestamp`, `content`, and `extensions` keys; absent optional fields render as `null` rather than being omitted

### Requirement: Backend exposes an idempotent relationship-reset endpoint

The system SHALL expose `POST /api/relationships/{characterId}/reset` that deletes every `Conversation` between the authenticated user and the named character, clears the memory record (rolling summary + pinned facts) for the user-companion pair, and clears the last-selected alt-greeting. The endpoint MUST be idempotent (calling it on an already-reset relationship succeeds with no change). The character record, user library data (presets, personas, lorebooks), and lorebook bindings MUST NOT be affected.

#### Scenario: Reset clears conversations and memory

- **WHEN** an authenticated client `POST`s to the reset endpoint for a companion with existing conversations and memory
- **THEN** subsequent reads return zero conversations and an empty memory record for the user-companion pair; the character record and library data are unchanged

#### Scenario: Reset is idempotent

- **WHEN** an authenticated client `POST`s to the reset endpoint a second time on an already-reset relationship
- **THEN** the endpoint returns a success response with no state change

#### Scenario: Reset preserves the character record and the user's library data

- **WHEN** a reset is committed for a user-companion pair whose user owns presets, personas, and lorebooks (some bound to the companion)
- **THEN** the character record (card definition, alt-greetings, `characterPresetId`) is unchanged; the user's preset / persona / lorebook library is unchanged; lorebook bindings on the character are unchanged; only the conversations + memory + last-selected alt-greeting for this user-companion pair are cleared

### Requirement: Backend emits gacha probabilities in the catalog response and records bonus events on duplicate draws

The system SHALL return the per-rarity probability breakdown inside the gacha catalog response so the Android client can render it in the pre-draw surface. The system SHALL accept `POST /api/gacha/bonus` with `{ drawnCardId }` recording a `bonusAwarded` event for the authenticated user. The response MUST carry a `bonusEventId` so the client can correlate the event with analytics or future reward resolution.

#### Scenario: Catalog response includes rarity probabilities

- **WHEN** an authenticated client fetches the gacha catalog
- **THEN** the response body carries a `rarities` field whose entries are `{ rarity, probability, cardIds }` shaped

#### Scenario: Bonus endpoint records the duplicate-acknowledgment

- **WHEN** an authenticated client `POST`s to `/api/gacha/bonus` with `{ drawnCardId = "card-42" }`
- **THEN** the backend persists `{ userId, drawnCardId, awardedAt }` and returns a `bonusEventId` that future reward-resolution paths can reference

#### Scenario: Catalog rarity entry shape

- **WHEN** a client parses the `rarities` array on the catalog response
- **THEN** each entry exposes `rarity` (canonical lowercase string — e.g., `legendary` / `epic` / `rare` / `common`), `probability` (a decimal in `[0, 1]`), and `cardIds` (the list of catalog card ids in that rarity tier); the sum of `probability` across entries equals `1.0` ± a small floating-point tolerance

