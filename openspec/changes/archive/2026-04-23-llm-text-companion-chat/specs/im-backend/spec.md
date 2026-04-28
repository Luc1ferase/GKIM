## ADDED Requirements

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
