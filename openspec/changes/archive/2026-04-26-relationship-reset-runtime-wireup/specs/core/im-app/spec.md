## ADDED Requirements

### Requirement: Reset relationship affordance on the production character-detail surface

The Android character-detail screen SHALL render a "Reset relationship" affordance below the action row on companion-card details (gated on `card.companionCardId != null`). The affordance SHALL drive the §6.1 `RelationshipResetAffordanceState` state machine through five visible UI states (Idle / Armed / Submitting / Completed / Failed) and surface the destructive-action confirmation as an inline banner (not a modal), per the §6.1 presentation contract. On Completed, the local conversations list SHALL no longer contain entries whose `companionCardId` matches the reset character; on Failed, the inline error copy SHALL render and the user SHALL be able to retry the call without re-arming the two-step gate.

#### Scenario: Idle state shows the trigger button

- **WHEN** the user opens character detail for a card with `companionCardId != null` and the affordance is in `Idle` phase
- **THEN** the screen renders the "Reset relationship" trigger button (testTag `relationship-reset-trigger`)
- **AND** the confirmation banner is NOT rendered (testTag `relationship-reset-confirmation-banner` does not exist)

#### Scenario: Tap arms the destructive-action gate

- **WHEN** the user taps the trigger button
- **THEN** the affordance transitions to `Armed`, the confirmation banner becomes visible (testTag `relationship-reset-confirmation-banner`), and the banner offers Cancel + Confirm controls (testTags `relationship-reset-cancel` / `relationship-reset-confirm`)
- **AND** the conversations list is NOT yet mutated

#### Scenario: Cancel from Armed returns to Idle without the call

- **WHEN** the user taps Cancel from the Armed banner
- **THEN** the affordance returns to `Idle`, the banner disappears, no `MessagingRepository.resetRelationship` call is made, and the conversations list is unchanged

#### Scenario: Confirm from Armed dispatches and clears the local conversations on success

- **WHEN** the user taps Confirm from the Armed banner
- **THEN** the affordance transitions to `Submitting`, calls `MessagingRepository.resetRelationship(characterId)`, and on `Result.success` transitions to `Completed`, removing every `Conversation` whose `companionCardId == characterId` from the local cache

#### Scenario: Failed reset surfaces an inline error with a retry that does not re-arm the gate

- **WHEN** `MessagingRepository.resetRelationship(characterId)` returns `Result.failure` whose throwable message is `character_not_available` or `network_failure`
- **THEN** the affordance transitions to `Failed`, the inline error copy renders the localized message for the failure code (testTag `relationship-reset-error`), and the retry button is enabled (testTag `relationship-reset-retry`)
- **AND** tapping retry transitions the affordance directly from `Failed` to `Submitting` (NOT back through Armed); a successful retry then advances to `Completed` with the same local-cache mutation
