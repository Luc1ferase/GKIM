## ADDED Requirements

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
