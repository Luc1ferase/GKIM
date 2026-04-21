## ADDED Requirements

### Requirement: Backend emits companion-turn block events with a closed set of typed wire-key reasons

The system SHALL tag every `companion_turn.blocked` event with a `reason` field whose value is drawn from the closed set `{"self_harm", "illegal", "nsfw_denied", "minor_safety", "provider_refusal", "other"}`. The backend MUST map every upstream provider safety signal, refusal, or policy-determined block into one of those six keys; it MUST NOT emit any other wire key for block reasons. Clients MAY expect the set to grow in future versions, but any new key MUST be added additively with a corresponding Android enum update, never introduced silently.

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

The system SHALL tag every `companion_turn.failed` event with a `subtype` field whose value is drawn from the closed set `{"transient", "prompt_budget_exceeded", "authentication_failed", "provider_unavailable", "network_error", "unknown"}`. The backend MUST choose the most specific subtype that applies to the failure cause and MUST NOT emit any other wire key; unclassifiable failures MUST emit `"unknown"`.

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
