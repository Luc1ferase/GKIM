# llm-text-companion-chat Specification

## Purpose
TBD - created by archiving change llm-text-companion-chat. Update Purpose after archive.
## Requirements
### Requirement: Companion reply lifecycle is explicit and bounded

The system SHALL represent every companion reply as a bounded lifecycle with explicit `Thinking`, `Streaming`, `Completed`, `Failed`, `Blocked`, and `Timeout` states. The lifecycle MUST be driven by realtime delta events that carry a monotonic sequence number per turn, and it MUST end in exactly one terminal state (`Completed`, `Failed`, `Blocked`, or `Timeout`). A turn MUST reach a terminal state within a bounded total duration and a bounded idle interval; missing the bound MUST produce `Timeout` rather than an indefinitely pending reply. The wire-format parser MUST recognize the dotted event-type discriminators `companion_turn.failed`, `companion_turn.blocked`, and `companion_turn.timeout` and project each onto the corresponding `MessageStatus` terminal in the local model so the chat surface renders the matching presentation state.

When the `companion_turn.failed` event carries an optional `retryAfterMs` field (populated server-side when the upstream returns HTTP 429 with a `Retry-After` header), the local repository MUST translate it into an absolute `retryAfterEpochMs` (= reception-time + retryAfterMs) on the message's `CompanionTurnMeta` so a reconnecting client that re-receives the same event after a long gap still computes the correct remaining countdown without drift. The Failed bubble's Retry affordance MUST honor this deadline at click time without surfacing a visible countdown on the button itself: the Retry text MUST always render as a clickable affordance, but tapping it while `now < retryAfterEpochMs` MUST suppress the actual retry dispatch and surface an ephemeral, auto-dismissing notice (3-second duration) carrying bilingual "Retry available in `<seconds>`s / `<seconds>` 秒后才能重试" copy. When the field is null or the deadline has elapsed, the tap dispatches the existing retry path unchanged.

#### Scenario: Companion turn transitions through thinking and streaming to completed

- **WHEN** an authenticated user submits a companion turn and the provider begins producing text normally
- **THEN** the turn transitions `Pending → Thinking → Streaming → Completed`, each transition surfaces as a typed realtime event, and the final completed state carries the full reply text

#### Scenario: Companion turn emits explicit failure and safety terminals

- **WHEN** provider orchestration, transport, or safety policy prevents a companion reply from completing
- **THEN** the turn terminates in `Failed`, `Blocked`, or `Timeout` with a typed reason, and no subsequent lifecycle deltas are emitted for that turn

#### Scenario: Idle or total-duration bound fires timeout

- **WHEN** a companion turn exceeds the total-duration bound or stays idle beyond the idle bound without producing new deltas
- **THEN** the lifecycle terminates with `Timeout`, distinct from generic `Failed`, so the client can surface timeout-specific retry guidance

#### Scenario: companion_turn.failed wire event parses into MessageStatus.Failed with subtype

- **WHEN** the realtime gateway emits an event whose `type` discriminator equals `"companion_turn.failed"` carrying the fields `{ turnId, conversationId, messageId, subtype, errorMessage, completedAt }`
- **THEN** the local parser MUST construct an `ImGatewayEvent.CompanionTurnFailed` value and the repository handler MUST project it into a `ChatMessage` whose `status = MessageStatus.Failed` and whose `companionTurnMeta.failedSubtypeKey` equals the event's `subtype`
- **AND** the `subtype` MUST round-trip through the `FailedSubtype.fromWireKey` enum without falling through to `Unknown` for any of the six taxonomy values (`transient`, `prompt_budget_exceeded`, `authentication_failed`, `provider_unavailable`, `network_error`, `unknown`); only an unrecognized future value MUST land on `Unknown`
- **AND** the byte-equivalent fixture `contract/fixtures/companion-turns/event-failed.json` (mirrored from the paired backend slice) MUST round-trip through the local serde model without field drift

#### Scenario: companion_turn.blocked wire event parses into MessageStatus.Blocked with reason

- **WHEN** the realtime gateway emits an event whose `type` discriminator equals `"companion_turn.blocked"` carrying the fields `{ turnId, conversationId, messageId, reason, completedAt }`
- **THEN** the local parser MUST construct an `ImGatewayEvent.CompanionTurnBlocked` value and the repository handler MUST project it into a `ChatMessage` whose `status = MessageStatus.Blocked`; the bubble's lifecycle render MUST carry the reason for the locale-keyed Blocked copy
- **AND** the byte-equivalent fixture `contract/fixtures/companion-turns/event-blocked.json` MUST round-trip through the local serde model without field drift

#### Scenario: companion_turn.timeout wire event parses into MessageStatus.Timeout with elapsedMs

- **WHEN** the realtime gateway emits an event whose `type` discriminator equals `"companion_turn.timeout"` carrying the fields `{ turnId, conversationId, messageId, elapsedMs, completedAt }`
- **THEN** the local parser MUST construct an `ImGatewayEvent.CompanionTurnTimeout` value and the repository handler MUST project it into a `ChatMessage` whose `status = MessageStatus.Timeout`; the `elapsedMs` value MUST be exposed on the local model so the bubble can surface "no response after Xs" copy
- **AND** the byte-equivalent fixture `contract/fixtures/companion-turns/event-timeout.json` MUST round-trip through the local serde model without field drift

#### Scenario: companion_turn.failed event with retryAfterMs is persisted as absolute retryAfterEpochMs

- **WHEN** the realtime gateway emits a `companion_turn.failed` event whose payload carries `retryAfterMs: 12000` (the backend's parsed `Retry-After` header value from an upstream 429 response)
- **THEN** the local parser MUST construct `ImGatewayEvent.CompanionTurnFailed` with `retryAfterMs = 12000L`, and the repository handler MUST set `companionTurnMeta.retryAfterEpochMs = clockMillis() + 12_000L` on the projected `ChatMessage` so the deadline is wall-clock-absolute and survives subsequent reconnects without re-computation drift
- **AND** when the wire payload omits the `retryAfterMs` key, the parsed event MUST decode to `retryAfterMs = null` and the repository handler MUST leave `companionTurnMeta.retryAfterEpochMs` as null

#### Scenario: Retry tap during cooldown surfaces ephemeral notice without dispatching the retry

- **WHEN** the user taps the Retry affordance on a Failed bubble whose `companionTurnMeta.retryAfterEpochMs` is greater than the current wall-clock instant
- **THEN** the chat surface MUST suppress the underlying `onRetryCompanionTurn` dispatch AND surface a small auto-dismissing notice (3-second duration, anchored near the top of the chat column under the persona footer) carrying bilingual "Retry available in `<seconds>`s / `<seconds>` 秒后才能重试" copy keyed to the active `LocalAppLanguage`
- **AND** the notice MUST NOT block the user from tapping again immediately — repeated taps within the cooldown re-surface the notice with the freshly-computed remaining seconds without ever invoking the retry path
- **AND** when the wall-clock advances past `retryAfterEpochMs` (or when the field is null), the next tap MUST dispatch the existing retry path unchanged from the pre-F2c behavior
- **AND** the Retry button itself MUST always render as a clickable affordance regardless of the cooldown state — the visible button never carries a countdown or disabled styling

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

### Requirement: Companion chat Send path invokes CompanionTurnRepository end-to-end

The system SHALL dispatch the chat Send affordance on a companion conversation through `CompanionTurnRepository.submitUserTurn(...)` rather than the legacy `MessagingRepository.sendMessage(...)` path. The chat surface MUST render an outgoing user bubble at `MessageStatus.Pending` optimistically on submit, then transition per the companion turn state flow through the terminal states (`Thinking`, `Streaming`, `Completed`, `Failed`, `Blocked`, `Timeout`). The integration MUST be covered by a layered test pyramid: (1) unit tests for ViewModel dispatch branching on the conversation's companion marker and for uiState assembly from the companion state flow, (2) a unit test for the activate handler populating the companion marker on conversation creation, and (3) a Compose-rendered instrumentation test on `codex_api34` that drives the production `DefaultCompanionTurnRepository` reducer transitions through `CompanionLifecycleTimelineHost` and asserts every lifecycle state rendered by `ChatMessageRow`.

#### Scenario: Send on a companion conversation dispatches to CompanionTurnRepository

- **WHEN** a user taps Send on a chat surface whose active conversation is a companion conversation
- **THEN** the `ChatViewModel` invokes `CompanionTurnRepository.submitUserTurn(...)` exactly once for that submit and does not invoke `MessagingRepository.sendMessage(...)` for the same submit

#### Scenario: Send on a peer-IM conversation still uses MessagingRepository

- **WHEN** a user taps Send on a chat surface whose active conversation is a peer-IM conversation (no companion marker)
- **THEN** the `ChatViewModel` invokes `MessagingRepository.sendMessage(...)` exactly once for that submit and does not invoke `CompanionTurnRepository.submitUserTurn(...)` for the same submit

#### Scenario: Companion user bubble renders optimistically on submit

- **WHEN** a user taps Send on a companion conversation
- **THEN** the chat surface renders an outgoing user bubble at `MessageStatus.Pending` before any backend acknowledgment has been received

#### Scenario: ChatMessageRow consumes the companion state flow

- **WHEN** the active conversation is a companion conversation and the companion turn state flow emits successive lifecycle events
- **THEN** `ChatMessageRow` renders the corresponding state (`Thinking`, `Streaming`, `Completed`, `Failed`, `Blocked`, or `Timeout`) sourced from `ChatViewModel.uiState` and driven by `CompanionTurnRepository`'s state flow for that conversation

#### Scenario: Compose-rendered instrumentation gates the lifecycle render

- **WHEN** the companion-chat instrumentation runs on `codex_api34(AVD) - 14` driving a real `DefaultCompanionTurnRepository` through `recordUserTurn` → `handleTurnStarted` → `handleTurnDelta` → `handleTurnCompleted`
- **THEN** the test collects `activePathByConversation` through `CompanionLifecycleTimelineHost` and asserts that `ChatMessageRow` renders (a) `chat-message-body-<userMessageId>` for the Pending user bubble, (b) `chat-companion-status-<companionMessageId>` with `"Thinking…"` after the started event, (c) `"Streaming…"` after the first delta, and (d) `chat-message-body-<companionMessageId>` with the final body after the completed event

#### Scenario: Failed-terminal scenario is covered in instrumentation

- **WHEN** the same instrumentation test runs its Failed scenario driving `handleTurnFailed(subtype="transient")` on the companion message and `updateUserMessageStatus(Failed)` on the user message
- **THEN** the test asserts `chat-companion-failed-copy-<companionMessageId>` is displayed (companion bubble failed copy from `SafetyCopy.localizedFailedCopy`) and `chat-user-submission-retry-<userMessageId>` is displayed (outgoing-user Retry affordance from `outgoingSubmissionFailureLine`)

### Requirement: LiveCompanionTurnRepository exposes editUserTurn and regenerateCompanionTurnAtTarget runtime methods

The system SHALL expose two runtime methods on `LiveCompanionTurnRepository`: `editUserTurn(conversationId, parentMessageId, newUserText, activeCompanionId, activeLanguage)` returning `Result<EditUserTurnResponseDto>` and `regenerateCompanionTurnAtTarget(conversationId, targetMessageId)` returning `Result<CompanionTurnRecordDto>`. Each method MUST call the corresponding `ImBackendClient` HTTP method, project the response into the local sibling tree, advance the active-path map for the affected variant groups, and emit the resulting `ChatMessage` updates through the existing `companionMessages` flow consumed by `ChatViewModel`.

#### Scenario: editUserTurn projects both new variants into the local tree

- **WHEN** `editUserTurn` is called and the backend response carries a new user-message variant under parentMessageId `msg-original-user-7` plus a new companion-turn variant under the new user message
- **THEN** the local sibling tree gains both variants, the active-path map records the new user-message's `messageId` as active under the user variantGroup and the new companion-turn's `messageId` as active under the companion variantGroup, and the next `companionMessages` emission carries both new bubbles ordered correctly in the timeline

#### Scenario: regenerateCompanionTurnAtTarget appends a sibling at the target's variantGroup

- **WHEN** `regenerateCompanionTurnAtTarget` is called with `targetMessageId = "companion-msg-3"` and the backend returns a new companion-turn at `variantIndex = 1` of that message's variantGroup
- **THEN** the local sibling tree's `siblingsFor(conversationId, variantGroupId)` accessor returns both the original (index 0) and the new (index 1) variant; the active-path map advances to index 1; downstream bubbles under the original sibling remain addressable but inactive

#### Scenario: HTTP failure on either method surfaces a Result.failure without mutating local state

- **WHEN** the underlying `ImBackendClient` call fails (transport / non-2xx)
- **THEN** the method returns `Result.failure` and the local sibling tree + active-path map are unchanged, so the `ChatViewModel` retry path can re-invoke without compounding state

### Requirement: LiveCompanionTurnRepository tracks per-variant-group sibling lists for active-path lookup

The system SHALL maintain a `Map<conversationId, Map<variantGroupId, List<ChatMessage>>>` of siblings indexed by their variant group, exposing a read-only `siblingsFor(conversationId, variantGroupId): List<ChatMessage>` accessor. The list MUST preserve insertion order (the original sibling at index 0, regenerate / regenerate-at / edit additions appended). The accessor backs the `ChatViewModel`'s active-path projection and the §3.1 chevron caption's "n / total" denominator.

#### Scenario: siblingsFor returns the canonical sibling order

- **WHEN** a variant group has been touched by submit (creating sibling 0), regenerate-at (appending sibling 1), and another regenerate-at (appending sibling 2)
- **THEN** `siblingsFor` returns the three siblings in insertion order with their `variantIndex` matching their position in the list

#### Scenario: siblingsFor returns an empty list for unknown groups

- **WHEN** `siblingsFor` is called with a `variantGroupId` that the conversation has never touched
- **THEN** the accessor returns an empty list (not null), so the §3.1 chevron rendering path can call `siblingCount = list.size` without a null check

### Requirement: CompanionTurnRepository exposes a JSONL conversation-export method

The `CompanionTurnRepository` SHALL expose `exportConversation(conversationId, format, pathOnly): Result<ExportedChatPayload>`. The `LiveCompanionTurnRepository` impl SHALL delegate to `ImBackendClient.exportConversation(...)` (which calls `GET /api/conversations/:conversationId/export?format=...&pathOnly=...`), wrap the response body string + the canonical `chat-export-<active-path|full-tree>_<first8OfConversationId>.jsonl` filename + the `application/x-ndjson` content-type into `ExportedChatPayload(filename, bytes, contentType)`, and surface wire failures as `Result.failure(throwable)` whose message is a stable error code so the UI can render localized copy without parsing exception types.

#### Scenario: Active-path JSONL export returns a wrapped payload

- **WHEN** `LiveCompanionTurnRepository.exportConversation(conversationId, format="jsonl", pathOnly=true)` is invoked and `ImBackendClient.exportConversation` succeeds with a non-empty JSONL string
- **THEN** the method returns `Result.success(ExportedChatPayload)` whose `filename` is `chat-export-active-path_<first8OfConversationId>.jsonl`, whose `bytes` matches the JSONL string encoded as UTF-8, and whose `contentType` is `application/x-ndjson`

#### Scenario: Full-tree JSONL export uses the full-tree filename

- **WHEN** the same method is invoked with `pathOnly=false`
- **THEN** the returned payload's filename is `chat-export-full-tree_<first8OfConversationId>.jsonl` and the wire query carries `pathOnly=false`

#### Scenario: 404 maps to `404_unknown_conversation`

- **WHEN** `ImBackendClient.exportConversation` throws an HTTP exception with status 404
- **THEN** the method returns `Result.failure` whose throwable message is `404_unknown_conversation`

#### Scenario: 400 unsupported_format maps to `unsupported_format`

- **WHEN** `ImBackendClient.exportConversation` throws an HTTP exception with status 400 carrying the backend's `unsupported_format` error body
- **THEN** the method returns `Result.failure` whose throwable message is `unsupported_format`

### Requirement: MessagingRepository exposes a relationship-reset method that delegates to the backend and reconciles local conversations

The `MessagingRepository` SHALL expose `resetRelationship(characterId): Result<Unit>`. The `LiveMessagingRepository` impl SHALL delegate to `ImBackendClient.resetRelationship(characterId)` (which calls `POST /api/relationships/{characterId}/reset`), and on success SHALL remove every `Conversation` whose `companionCardId == characterId` from the local `conversations` StateFlow before returning. Wire failures SHALL be surfaced as `Result.failure(throwable)` whose message is a stable error code so the UI can render localized copy without parsing exception types.

#### Scenario: Successful reset removes the matching conversations from the local cache

- **WHEN** the local `conversations` StateFlow contains entries with `companionCardId` values `["c1", "c2", "c3"]` and `LiveMessagingRepository.resetRelationship("c2")` returns `Result.success`
- **THEN** the StateFlow's next emission contains exactly the conversations with `companionCardId` in `["c1", "c3"]`; the conversation with `companionCardId == "c2"` is removed

#### Scenario: HTTP 403 character_not_available maps to error code `character_not_available`

- **WHEN** `ImBackendClient.resetRelationship` throws an HTTP exception with status 403 carrying the backend's `character_not_available` error body
- **THEN** the method returns `Result.failure` whose throwable message is `character_not_available` and the local conversations cache is NOT mutated

#### Scenario: Network failure maps to `network_failure` and leaves the cache intact

- **WHEN** `ImBackendClient.resetRelationship` throws an `IOException`
- **THEN** the method returns `Result.failure` whose throwable message is `network_failure` and the local conversations cache is NOT mutated

#### Scenario: Missing base URL or token short-circuits without calling backend

- **WHEN** `baseUrlProvider()` or `tokenProvider()` returns null
- **THEN** the method returns `Result.failure` immediately and `ImBackendClient.resetRelationship` is NOT invoked

