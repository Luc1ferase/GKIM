## ADDED Requirements

### Requirement: Backend exposes an edit-user-turn endpoint that creates a new user-message sibling

The system SHALL expose an authenticated endpoint `POST /api/companion-turns/{conversationId}/edit` accepting `{ parentMessageId, newUserText }`. The endpoint MUST create a new `ChatMessage` with `role = user`, `parentMessageId` matching the request, a newly-allocated `variantGroupId`, and `variantIndex = 0` inside that group. The endpoint MUST return the new user sibling and kick off a companion turn for that sibling through the existing turn lifecycle (`companion_turn.started` → `delta` → `completed`/`failed`/`blocked`/`timeout`). The original siblings MUST be preserved.

#### Scenario: Edit creates a new variant group under the parent

- **WHEN** an authenticated client `POST`s to `/api/companion-turns/{conversationId}/edit` with a valid `parentMessageId` and non-empty `newUserText`
- **THEN** the response body carries the new user sibling with a fresh `variantGroupId` and `variantIndex = 0`; the backend starts the companion turn lifecycle for that sibling

#### Scenario: Edit preserves the original variant group

- **WHEN** an edit is submitted for a parent that already has a user sibling
- **THEN** the prior sibling and its descendants remain persisted and addressable; the conversation's active-path map is updated to point at the new sibling

### Requirement: Backend extends the regenerate endpoint to accept `targetMessageId` for arbitrary-layer regenerate

The system SHALL extend `POST /api/companion-turns/{conversationId}/regenerate` to accept an optional `targetMessageId` field. When present, the endpoint MUST create a new companion-message sibling under the same `variantGroupId` as the target message and kick off the companion turn lifecycle for the new sibling. When absent, the endpoint MUST behave as before (regenerate the most recent companion turn). The new sibling MUST become the active index on its variant group; prior siblings MUST be preserved.

#### Scenario: Regenerate with targetMessageId appends a sibling at that layer

- **WHEN** an authenticated client `POST`s to the regenerate endpoint with `{ targetMessageId = "msg-42" }` for a mid-conversation companion message
- **THEN** the response carries a new sibling under `msg-42`'s `variantGroupId`; its `variantIndex` is the next available index in the group; the active-path map updates

#### Scenario: Regenerate without targetMessageId preserves legacy behavior

- **WHEN** an authenticated client `POST`s to the regenerate endpoint without a `targetMessageId` field
- **THEN** the endpoint regenerates the most recent companion turn, identical to the pre-extension behavior from `llm-text-companion-chat`

### Requirement: Backend persists `characterPresetId` on the character record and honors it during prompt assembly

The system SHALL persist an optional `characterPresetId: String?` field on every character record. When a conversation's active character carries a non-null `characterPresetId`, the deterministic allocator MUST use that preset (resolved against the user's preset library) instead of the user's globally-active preset for that conversation's turns. If the referenced preset has been deleted or is not owned by the user, the allocator MUST fall back to the globally-active preset and log a typed warning for that conversation.

#### Scenario: Override resolves against the user's preset library

- **WHEN** a conversation's active character has `characterPresetId = "preset-X"` and `preset-X` exists in the user's preset library
- **THEN** the allocator uses `preset-X`'s template + params for prompt assembly on that conversation's turns

#### Scenario: Dangling override falls back with a warning

- **WHEN** `characterPresetId = "preset-X"` references a preset that has been deleted
- **THEN** the allocator falls back to the user's globally-active preset and emits a typed warning naming the missing preset id

### Requirement: Backend exposes a conversation export endpoint returning JSONL with active-path or full-tree selection

The system SHALL expose `GET /api/conversations/{conversationId}/export?format=jsonl&pathOnly=true|false` returning the conversation serialized as JSON Lines. Each line MUST be a JSON object carrying `messageId`, `parentMessageId`, `variantGroupId`, `variantIndex`, `role`, `timestamp`, `content`, and the message's `extensions` bag. When `pathOnly=true`, the payload MUST include only messages along the active path. When `pathOnly=false`, the payload MUST include every message in the conversation regardless of active path.

#### Scenario: Active-path-only export emits ordered path messages

- **WHEN** an authenticated client calls the endpoint with `pathOnly=true` on a conversation whose active path contains five messages
- **THEN** the body carries five lines of JSON, one per path message, ordered from root to most-recent

#### Scenario: Full-tree export emits every message

- **WHEN** the same client calls the endpoint with `pathOnly=false` on the same conversation
- **THEN** the body carries one line per message in the conversation, including non-active siblings

### Requirement: Backend exposes an idempotent relationship-reset endpoint

The system SHALL expose `POST /api/relationships/{characterId}/reset` that deletes every `Conversation` between the authenticated user and the named character, clears the memory record (rolling summary + pinned facts) for the user-companion pair, and clears the last-selected alt-greeting. The endpoint MUST be idempotent (calling it on an already-reset relationship succeeds with no change). The character record, user library data (presets, personas, lorebooks), and lorebook bindings MUST NOT be affected.

#### Scenario: Reset clears conversations and memory

- **WHEN** an authenticated client `POST`s to the reset endpoint for a companion with existing conversations and memory
- **THEN** subsequent reads return zero conversations and an empty memory record for the user-companion pair; the character record and library data are unchanged

#### Scenario: Reset is idempotent

- **WHEN** an authenticated client `POST`s to the reset endpoint a second time on an already-reset relationship
- **THEN** the endpoint returns a success response with no state change

### Requirement: Backend emits gacha probabilities in the catalog response and records bonus events on duplicate draws

The system SHALL return the per-rarity probability breakdown inside the gacha catalog response so the Android client can render it in the pre-draw surface. The system SHALL accept `POST /api/gacha/bonus` with `{ drawnCardId }` recording a `bonusAwarded` event for the authenticated user. The response MUST carry a `bonusEventId` so the client can correlate the event with analytics or future reward resolution.

#### Scenario: Catalog response includes rarity probabilities

- **WHEN** an authenticated client fetches the gacha catalog
- **THEN** the response body carries a `rarities` field whose entries are `{ rarity, probability, cardIds }` shaped

#### Scenario: Bonus endpoint records the duplicate-acknowledgment

- **WHEN** an authenticated client `POST`s to `/api/gacha/bonus` with `{ drawnCardId = "card-42" }`
- **THEN** the backend persists `{ userId, drawnCardId, awardedAt }` and returns a `bonusEventId` that future reward-resolution paths can reference
