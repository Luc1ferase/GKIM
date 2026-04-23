# llm-text-companion-chat Specification

## Purpose
TBD - created by archiving change llm-text-companion-chat. Update Purpose after archive.
## Requirements
### Requirement: Companion reply lifecycle is explicit and bounded

The system SHALL represent every companion reply as a bounded lifecycle with explicit `Thinking`, `Streaming`, `Completed`, `Failed`, `Blocked`, and `Timeout` states. The lifecycle MUST be driven by realtime delta events that carry a monotonic sequence number per turn, and it MUST end in exactly one terminal state (`Completed`, `Failed`, `Blocked`, or `Timeout`). A turn MUST reach a terminal state within a bounded total duration and a bounded idle interval; missing the bound MUST produce `Timeout` rather than an indefinitely pending reply.

#### Scenario: Companion turn transitions through thinking and streaming to completed

- **WHEN** an authenticated user submits a companion turn and the provider begins producing text normally
- **THEN** the turn transitions `Pending → Thinking → Streaming → Completed`, each transition surfaces as a typed realtime event, and the final completed state carries the full reply text

#### Scenario: Companion turn emits explicit failure and safety terminals

- **WHEN** provider orchestration, transport, or safety policy prevents a companion reply from completing
- **THEN** the turn terminates in `Failed`, `Blocked`, or `Timeout` with a typed reason, and no subsequent lifecycle deltas are emitted for that turn

#### Scenario: Idle or total-duration bound fires timeout

- **WHEN** a companion turn exceeds the total-duration bound or stays idle beyond the idle bound without producing new deltas
- **THEN** the lifecycle terminates with `Timeout`, distinct from generic `Failed`, so the client can surface timeout-specific retry guidance

### Requirement: Companion replies support a variant tree with swipe navigation and regeneration

The system SHALL persist companion replies as siblings in a variant tree keyed on a shared `variantGroupId` per user turn, and it MUST allow the user to navigate between variants on the same turn without losing prior variants. Regeneration MUST append a new sibling to the existing variant group rather than overwriting or deleting prior variants. Exactly one variant per group is the active-path selection at any moment; the active selection MUST persist across relaunch.

#### Scenario: Multiple variants coexist under the same user turn

- **WHEN** a user regenerates a companion reply to the same user turn one or more times
- **THEN** the conversation history retains every variant as a sibling in the same `variantGroupId`, each variant is individually addressable, and none of the prior variants are destroyed

#### Scenario: User swipes between variants

- **WHEN** a user cycles through variants on a companion bubble via swipe navigation
- **THEN** only the active variant selection changes; the underlying variant records, their order, and their metadata remain untouched

#### Scenario: Regenerate appends a sibling variant

- **WHEN** the user requests regeneration on the most recent companion variant
- **THEN** the system creates a new variant under the same `variantGroupId` and transitions it through the normal reply lifecycle, starting from `Thinking`

### Requirement: Companion conversations use explicit first-message selection on first entry

The system SHALL present an explicit selection of the companion's `firstMes` plus every entry in `alternateGreetings` when a user enters a companion conversation with no message history, and it MUST skip the selector once any companion or user message exists in the conversation. The selected greeting MUST be persisted through the same variant contract as regular companion replies, at `variantIndex=0` of its `variantGroupId`.

#### Scenario: Empty companion conversation shows greeting picker

- **WHEN** a user opens a companion conversation whose message history is empty
- **THEN** the system presents a picker listing the resolved `firstMes` and every `alternateGreetings` entry in the active `AppLanguage`, and the conversation does not auto-insert a greeting until the user chooses one

#### Scenario: Non-empty companion conversation skips picker

- **WHEN** a user opens a companion conversation whose message history already contains at least one message
- **THEN** the system does not render the greeting picker and routes the user directly into the timeline

#### Scenario: Selected greeting persists as a companion variant

- **WHEN** the user selects a greeting from the picker
- **THEN** the selected greeting is persisted as a companion message with `variantIndex=0` in its `variantGroupId` and is visible in subsequent conversation history reads

### Requirement: Companion prompt assembly carries the deep persona record and substitutes the user display name

The system SHALL assemble each companion turn prompt server-side from the active card's deep persona fields — `systemPrompt`, `personality`, `scenario`, `exampleDialogue`, and the chosen `firstMes` or `alternateGreetings` entry — and it MUST apply soft language steering toward the user's active `AppLanguage`. The system MUST substitute `{{user}}`, `{user}`, and `<user>` macros in assembled prompts with the authenticated user's display name before sending the prompt to the LLM provider.

#### Scenario: Deep persona fields feed prompt assembly

- **WHEN** the backend assembles a companion turn prompt
- **THEN** the prompt incorporates the active card's `systemPrompt`, `personality`, `scenario`, and `exampleDialogue` in the language matching the user's active `AppLanguage`, drawn from the bilingual `LocalizedText` record

#### Scenario: {{user}} macro substitution happens before provider call

- **WHEN** an assembled prompt contains `{{user}}`, `{user}`, or `<user>` macros
- **THEN** the backend replaces each occurrence with the authenticated account's display name, and the provider never receives a raw macro token

#### Scenario: Language steering aligns reply language with active AppLanguage

- **WHEN** the backend assembles a companion turn prompt for a user whose active `AppLanguage` is Chinese or English
- **THEN** the prompt includes an instruction that steers the companion reply toward that language, and the client-submitted turn request carries `activeLanguage` so the server can apply the steering consistently

### Requirement: Pending companion turns recover across reconnect and relaunch

The system SHALL let a client rehydrate any in-flight companion turn after reconnect or relaunch, and it MUST NOT drop a pending turn silently. The client MUST be able to list pending turns for the authenticated user and to snapshot an individual turn's current state when realtime deltas arrive with gaps.

#### Scenario: Client lists pending turns on startup or reconnect

- **WHEN** an authenticated client starts up or reconnects while one or more companion turns remain in `Thinking` or `Streaming` state
- **THEN** the client can request the current list of pending turns, rehydrate their lifecycle state, and subscribe to continuing deltas over the realtime gateway

#### Scenario: Delta gap triggers snapshot refresh

- **WHEN** a client detects a monotonic `deltaSeq` gap for a turn that is still in a non-terminal state
- **THEN** the client can fetch a server snapshot of that turn's current text and status, replay into the reducer, and resume consuming deltas without silent corruption

