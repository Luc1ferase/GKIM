## ADDED Requirements

### Requirement: Android Settings reorganizes into six companion-oriented sections

The system SHALL reorganize the Android Settings root menu into six sections, rendered in this order: `Companion` (persona library, preset library, and a memory shortcut), `Appearance`, `Content & Safety`, `AIGC Image Provider` (scoped explicitly to image generation, not companion chat), `Developer & Connection` (renamed from `ImValidation`, gated behind `BuildConfig.DEBUG` for items that are not safe on release), and `Account`. Section labels and item copy MUST be bilingual through the `LocalizedText` contract and MUST use companion-oriented terminology instead of the prior IM/AIGC terminology.

#### Scenario: Settings renders the six sections in the documented order

- **WHEN** a user opens the Android Settings screen
- **THEN** the top-level menu lists the six sections (`Companion`, `Appearance`, `Content & Safety`, `AIGC Image Provider`, `Developer & Connection`, `Account`) in that order, each labeled in the user's active `AppLanguage`

#### Scenario: AIGC Image Provider section clarifies it does not control companion chat

- **WHEN** a user opens the `AIGC Image Provider` section
- **THEN** the section's header caption makes clear that the provider selection controls image generation only and does not apply to companion chat, and the underlying provider-selection behavior is preserved from the pre-reframe implementation

#### Scenario: Developer & Connection section hides debug-only items on release builds

- **WHEN** a user opens Settings on a release build (`BuildConfig.DEBUG == false`)
- **THEN** the `Developer & Connection` section renders only items that are safe on release (e.g., connection status); IM backend override, dev user id, and other debug-only entries are absent

### Requirement: Android Settings `Companion` section exposes a memory shortcut scoped per companion

The system SHALL expose a memory shortcut entry inside the `Companion` section that, when tapped, opens a chooser listing the user's recently active companions with the currently active companion at the top. Selecting a companion from the chooser MUST open the memory panel (from `companion-memory-and-preset`) scoped to that companion.

#### Scenario: Memory shortcut lists recently active companions

- **WHEN** a user taps the memory shortcut in the `Companion` section
- **THEN** a chooser appears listing the recently active companions, with the currently active companion at the top of the list

#### Scenario: Selecting a companion routes to that companion's memory panel

- **WHEN** a user selects a companion from the memory chooser
- **THEN** the app routes to the memory panel scoped to the selected companion, letting the user review summary, manage pinned facts, and invoke the three reset granularities

### Requirement: Android Settings `Content & Safety` section renders acknowledgment status and block-reason verbosity

The system SHALL populate the `Content & Safety` section with: (a) a read-only acknowledgment-status row that surfaces either the acceptance date or a "Not accepted — read policy" state with a tappable entry into the content-policy route, and (b) a block-reason verbosity toggle (default on) that persists per account and gates whether block bubbles show the bilingual reason copy in addition to the generic block label.

#### Scenario: Acknowledgment-status row reflects persisted state

- **WHEN** a user opens the `Content & Safety` section
- **THEN** the acknowledgment-status row renders either the persisted acceptance date (localized) or a "Not accepted — read policy" call-to-action, based on the persisted per-account acknowledgment state

#### Scenario: Verbosity toggle persists and affects block bubble rendering

- **WHEN** a user toggles the block-reason verbosity setting
- **THEN** the setting is persisted per account and, on subsequent block bubble renders, the bilingual reason copy is shown when the toggle is on and suppressed (falling back to a generic block label) when the toggle is off

### Requirement: Android chat `Blocked` bubble renders typed bilingual reason copy and a new-message path

The system SHALL render the `Blocked` terminal as a bubble whose body is drawn from the bilingual block-reason copy table keyed by the resolved `BlockReason` enum and whose primary affordance is a "Compose a new message" CTA that clears the input and focuses the composer. The bubble MUST NOT expose a retry affordance, MUST NOT echo or paraphrase the user's prompt, and MAY expose a secondary "Read content policy" link that opens the content-policy route. Unknown wire-key reasons MUST map to `BlockReason.Other` with neutral fallback copy.

#### Scenario: Each closed-set reason renders the mapped bilingual copy

- **WHEN** a companion turn terminates with `Blocked` carrying any of the six closed-set wire keys
- **THEN** the bubble renders the copy mapped to that `BlockReason` enum variant in the user's active `AppLanguage`, with no retry affordance

#### Scenario: Unknown wire-key reasons fall back to neutral copy

- **WHEN** a companion turn terminates with `Blocked` carrying a wire key that is not in the closed set
- **THEN** the bubble maps to `BlockReason.Other`, renders the neutral "Other" copy from the localized table, and does not display any raw wire key to the user

#### Scenario: Bubble surfaces compose-new affordance instead of retry

- **WHEN** the user taps the primary affordance on a `Blocked` bubble
- **THEN** the composer is focused and any prior draft is cleared so the user can enter a new message; no retry is submitted to the backend

### Requirement: Android chat `Failed` bubble renders per-subtype copy and a subtype-appropriate action set

The system SHALL render the `Failed` terminal with copy and actions selected by the resolved failure subtype. For subtypes `Transient`, `ProviderUnavailable`, `NetworkError`, and `Unknown`, the bubble MUST expose a primary `Retry` action that re-submits the user turn. For subtypes `PromptBudgetExceeded` and `AuthenticationFailed`, the bubble MUST NOT expose `Retry`; instead, it MUST expose an affordance that guides the user to edit the user turn or reauthenticate respectively. Unknown wire-key subtypes MUST map to `Unknown` with generic fallback copy.

#### Scenario: Transient-family subtypes expose Retry

- **WHEN** a companion turn terminates with a failure subtype in `{Transient, ProviderUnavailable, NetworkError, Unknown}`
- **THEN** the bubble renders per-subtype bilingual copy and exposes a primary `Retry` action that re-submits the user turn

#### Scenario: Permanent subtypes suppress Retry

- **WHEN** a companion turn terminates with a failure subtype in `{PromptBudgetExceeded, AuthenticationFailed}`
- **THEN** the bubble renders per-subtype bilingual copy without a `Retry` action; the primary affordance instead routes to edit-user-turn (for `PromptBudgetExceeded`) or to the reauthentication flow (for `AuthenticationFailed`)

#### Scenario: Unknown wire-key subtype falls back to the `Unknown` rendering

- **WHEN** a companion turn terminates with a failure whose subtype wire key is not in the closed set
- **THEN** the bubble maps to the `Unknown` variant, renders the generic fallback copy, and exposes the standard `Retry` action

### Requirement: Android chat `Timeout` bubble is visually distinct from `Failed` and offers a longer-wait retry

The system SHALL render the `Timeout` terminal with dedicated bilingual copy, explicitly identifying the timeout, a primary `Retry-with-longer-wait` action that the backend honors by extending the idle bound by 50% on the retried turn, and an optional secondary "Switch preset" hint. The secondary hint MUST appear only when the active preset's `maxReplyTokens` is above the heuristic threshold. Timeout bubbles MUST NOT reuse the generic `Failed` copy or action set.

#### Scenario: Timeout bubble uses dedicated copy distinct from Failed

- **WHEN** a companion turn terminates with `Timeout`
- **THEN** the bubble renders timeout-specific bilingual copy in the user's active `AppLanguage`, not the generic `Failed` copy, and the primary action is labeled as a retry-with-longer-wait

#### Scenario: Retry extends the idle bound on the retried turn

- **WHEN** the user taps the timeout bubble's primary retry affordance
- **THEN** the re-submitted turn carries a hint the backend honors by extending the idle bound by 50% relative to the default

#### Scenario: Switch-preset hint appears only above the heuristic token threshold

- **WHEN** a `Timeout` bubble is rendered while the active preset's `maxReplyTokens` is above the heuristic threshold
- **THEN** the bubble exposes a secondary "Switch preset" hint that routes to the preset library; when the preset's `maxReplyTokens` is at or below the threshold, the hint is absent

### Requirement: Android bootstrap gates the tavern behind content-policy acknowledgment with version awareness

The system SHALL, on the first successful post-login session, fetch the per-account content-policy acknowledgment state from the backend. When the state is absent, or when the persisted acknowledgment version is lower than the current policy version, the app MUST route the user to the `ContentPolicyAcknowledgmentRoute` before entering the tavern. When the persisted acknowledgment version matches the current policy version, the app MUST bypass the acknowledgment route and route directly to the tavern. Debug builds MAY skip the gate.

#### Scenario: Missing acknowledgment routes the user to the acknowledgment surface

- **WHEN** a user completes login successfully and no per-account acknowledgment is persisted
- **THEN** the app routes to `ContentPolicyAcknowledgmentRoute` before entering the tavern

#### Scenario: Acknowledged session bypasses the gate

- **WHEN** a user opens the app and the persisted acknowledgment version matches the current policy version
- **THEN** the app bypasses `ContentPolicyAcknowledgmentRoute` and routes directly to the tavern

#### Scenario: Policy version bump re-triggers the gate

- **WHEN** the current policy version is higher than the persisted acknowledgment version on session start
- **THEN** the app routes the user to `ContentPolicyAcknowledgmentRoute` and records a new acknowledgment when the user accepts

#### Scenario: Debug builds may skip the gate

- **WHEN** the app is running under `BuildConfig.DEBUG` with the skip flag enabled
- **THEN** the bootstrap flow does not block entry to the tavern on acknowledgment state

### Requirement: `ContentPolicyAcknowledgmentRoute` renders bilingual policy copy and records acceptance

The system SHALL provide a `ContentPolicyAcknowledgmentRoute` that renders the current policy body from a bilingual localized copy table, exposes a scrollable reading area, and exposes a single primary "I accept" CTA. Accepting MUST call the backend acknowledgment endpoint with the current policy version, persist the acknowledgment locally, and route the user to the tavern. On error, the route MUST surface a retry affordance without granting entry to the tavern.

#### Scenario: Route renders bilingual policy body and accept CTA

- **WHEN** the user lands on `ContentPolicyAcknowledgmentRoute`
- **THEN** the screen renders the policy body copy in the user's active `AppLanguage` and exposes a single primary "I accept" CTA

#### Scenario: Accept records the acknowledgment and enters the tavern

- **WHEN** the user taps "I accept" and the backend acknowledges the request successfully
- **THEN** the client persists the acknowledgment (version + timestamp), and the app navigates into the tavern

#### Scenario: Accept failure surfaces retry without granting entry

- **WHEN** the acknowledgment call fails
- **THEN** the route renders an inline error with a retry affordance and does not route the user into the tavern
