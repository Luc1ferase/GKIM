## MODIFIED Requirements

### Requirement: Companion reply lifecycle is explicit and bounded

The system SHALL represent every companion reply as a bounded lifecycle with explicit `Thinking`, `Streaming`, `Completed`, `Failed`, `Blocked`, and `Timeout` states. The lifecycle MUST be driven by realtime delta events that carry a monotonic sequence number per turn, and it MUST end in exactly one terminal state (`Completed`, `Failed`, `Blocked`, or `Timeout`). A turn MUST reach a terminal state within a bounded total duration and a bounded idle interval; missing the bound MUST produce `Timeout` rather than an indefinitely pending reply. The wire-format parser MUST recognize the dotted event-type discriminators `companion_turn.failed`, `companion_turn.blocked`, and `companion_turn.timeout` and project each onto the corresponding `MessageStatus` terminal in the local model so the chat surface renders the matching presentation state.

When the `companion_turn.failed` event carries an optional `retryAfterMs` field (populated server-side when the upstream returns HTTP 429 with a `Retry-After` header), the local repository MUST translate it into an absolute `retryAfterEpochMs` (= reception-time + retryAfterMs) on the message's `CompanionTurnMeta` so a reconnecting client that re-receives the same event after a long gap still computes the correct remaining countdown without drift. The Failed bubble's Retry affordance MUST honor this deadline when present: the button MUST render as a disabled countdown ("Retry in `<seconds>`s / `<seconds>` 秒后重试") while `now < retryAfterEpochMs`, and MUST switch to the existing clickable Retry only when the deadline elapses or when the field is null.

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

#### Scenario: Failed bubble Retry affordance renders disabled countdown while retryAfterEpochMs is in the future

- **WHEN** a `CompanionLifecyclePresentation` carries a `MessageStatus.Failed` projection whose `companionTurnMeta.retryAfterEpochMs` is greater than the current wall-clock instant
- **THEN** the chat surface MUST render the Retry affordance as a non-clickable Text styled with the `OnSurfaceVariant` color, displaying bilingual "Retry in `<seconds>`s / `<seconds>` 秒后重试" copy that updates at least every 500ms via a Compose `LaunchedEffect`-driven tick
- **AND** when the wall-clock advances past `retryAfterEpochMs` (or when the field is null), the same render MUST switch to the existing clickable Retry affordance unchanged from the pre-F2c behavior; clients that never receive a `retryAfterMs` field continue to render Retry as immediately tappable
