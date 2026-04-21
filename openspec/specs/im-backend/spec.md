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

