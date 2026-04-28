# companion-settings-and-safety-reframe Specification

## Purpose
TBD - created by archiving change companion-settings-and-safety-reframe. Update Purpose after archive.
## Requirements
### Requirement: Companion chat safety blocks use a closed block-reason taxonomy with bilingual localized copy

The system SHALL represent every companion-turn safety block with one of a closed set of typed block-reason keys: `self_harm`, `illegal`, `nsfw_denied`, `minor_safety`, `provider_refusal`, and `other`. The system MUST supply bilingual localized user-facing copy for each reason, and it MUST map unknown future keys to `other` rather than surfacing a raw wire key to the user. The copy MUST be neutral — it MUST NOT echo the blocked content back to the user.

#### Scenario: Every closed-set reason renders bilingual copy

- **WHEN** a companion turn terminates with `companion_turn.blocked` carrying any of the six closed-set wire keys
- **THEN** the client resolves the reason to one of the six `BlockReason` enum values and renders the corresponding bilingual localized copy in the user's active `AppLanguage`

#### Scenario: Unknown wire keys fall back to `other`

- **WHEN** the backend emits a block event whose wire key is not one of the six closed-set keys
- **THEN** the client maps the reason to `BlockReason.Other` and renders the neutral fallback copy, and no raw wire key is shown to the user

#### Scenario: Block copy does not echo the blocked content

- **WHEN** a block bubble renders for any reason
- **THEN** the rendered copy is drawn from the localized table and does not quote, paraphrase, or summarize the user's prompt or the provider's refused response

### Requirement: Companion chat failures distinguish transient and permanent subtypes

The system SHALL represent every companion-turn failure with one of a closed set of typed failure-subtype keys: `transient`, `prompt_budget_exceeded`, `authentication_failed`, `provider_unavailable`, `network_error`, and `unknown`. The UI MUST render per-subtype localized copy and MUST expose a subtype-appropriate action set: retry for transient-like subtypes (`transient`, `provider_unavailable`, `network_error`, `unknown`); no retry for subtypes that require input change (`prompt_budget_exceeded`, `authentication_failed`), which instead surface an "edit and resend" or equivalent affordance.

#### Scenario: Transient subtypes surface Retry

- **WHEN** a companion turn terminates with a failure subtype in `{transient, provider_unavailable, network_error, unknown}`
- **THEN** the bubble renders per-subtype copy plus a primary Retry action that re-submits the user turn

#### Scenario: Permanent subtypes suppress Retry

- **WHEN** a companion turn terminates with a failure subtype in `{prompt_budget_exceeded, authentication_failed}`
- **THEN** the bubble renders per-subtype copy and does not expose a Retry action; the primary affordance instead guides the user to change input or reauthenticate

#### Scenario: Unknown subtypes fall back to `unknown`

- **WHEN** the backend emits a failure whose subtype key is not one of the six closed-set keys
- **THEN** the client maps the subtype to `unknown` and renders the generic fallback copy with a Retry action

### Requirement: Companion chat timeouts surface a dedicated state distinct from generic failure

The system SHALL render `Timeout` terminals with dedicated bilingual copy that explicitly identifies the timeout, a primary Retry-with-longer-wait affordance that the backend honors by extending the idle bound on the retried turn, and an optional secondary "switch preset" hint when the active preset's `maxReplyTokens` is above a heuristic threshold. Timeout bubbles MUST NOT reuse generic failure copy.

#### Scenario: Timeout bubble is distinct from failure

- **WHEN** a companion turn terminates with `Timeout`
- **THEN** the bubble renders timeout-specific bilingual copy, not generic failure copy, in the user's active `AppLanguage`

#### Scenario: Retry extends the idle bound

- **WHEN** the user taps the timeout-retry affordance
- **THEN** the re-submitted turn carries a hint that the backend extends the idle bound by 50%, so a marginally-slow provider has a better chance to complete

#### Scenario: Switch-preset hint is conditional

- **WHEN** a timeout occurs while the active preset has `maxReplyTokens` above the heuristic threshold
- **THEN** the bubble also surfaces a secondary "switch preset" hint linking to the preset library; otherwise the hint is absent

### Requirement: Content-policy acknowledgment is captured per account with version gating

The system SHALL require first-launch content-policy acknowledgment and MUST persist the acknowledgment (version + timestamp) per account. Subsequent launches MUST bypass the acknowledgment when the persisted version matches the current policy version. A version bump MUST re-trigger the acknowledgment route on next launch. Debug builds MAY skip acknowledgment.

#### Scenario: First launch routes to acknowledgment

- **WHEN** an account completes its first successful post-login session without a persisted content-policy acknowledgment
- **THEN** the app routes the user to the acknowledgment surface before entering the tavern

#### Scenario: Acknowledged session bypasses the route

- **WHEN** an account reopens the app and the persisted acknowledgment version matches the current policy version
- **THEN** the app bypasses the acknowledgment route and routes directly to the tavern

#### Scenario: Version bump re-triggers acknowledgment

- **WHEN** the policy version bumps between sessions
- **THEN** the next launch routes the user back through the acknowledgment surface and records a new acknowledgment on accept

### Requirement: Settings navigation is reorganized around companion-oriented sections

The system SHALL reorganize the Settings navigation into six sections: `Companion` (persona library, preset library, and a memory shortcut), `Appearance`, `Content & Safety`, `AIGC Image Provider` (scoped explicitly to image generation), `Developer & Connection` (developer flows gated behind debug builds where applicable), and `Account`. Section labels and item copy MUST use companion-oriented terminology, bilingual through the `LocalizedText` contract. The `AIGC Image Provider` section MUST preserve existing provider selection behavior; only the label and caption change.

#### Scenario: Settings renders the six sections in the documented order

- **WHEN** a user opens Settings
- **THEN** the menu lists the six sections (`Companion`, `Appearance`, `Content & Safety`, `AIGC Image Provider`, `Developer & Connection`, `Account`) in order, with companion-oriented labels in the active `AppLanguage`

#### Scenario: AIGC Image Provider caption clarifies scope

- **WHEN** a user opens the `AIGC Image Provider` section
- **THEN** the section header caption clarifies that this provider controls image generation and not companion chat, and the underlying provider selection behavior is unchanged from before the reframe

#### Scenario: Developer section is gated on debug builds where applicable

- **WHEN** a user opens Settings on a release build
- **THEN** the `Developer & Connection` section renders only the items that are safe for release (e.g., connection status), and debug-only items (IM backend override, dev user id) are absent

