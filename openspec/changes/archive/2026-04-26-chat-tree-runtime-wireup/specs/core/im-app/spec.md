## ADDED Requirements

### Requirement: Android chat surface renders the production Edit-user and Regenerate-from-here affordances on every applicable bubble

The system SHALL render an Edit overflow on every user (Outgoing) bubble that carries a `parentMessageId`, opening an edit sheet prefilled with the bubble's body text. The system SHALL render a Regenerate-from-here overflow on every companion (Incoming + `companionTurnMeta`) bubble — not only the most recent — that on tap invokes the `regenerateCompanionTurnAtTarget` repository call. The §3.1 sibling chevrons MUST stay visible on every variantGroup with `siblingCount > 1` regardless of how the siblings were created (submit, regenerate, regenerate-at, or edit).

#### Scenario: User bubble with a parent renders the Edit overflow

- **WHEN** the chat timeline includes a user bubble with a non-null `parentMessageId` and the conversation has an active companion id
- **THEN** the bubble renders an Edit overflow whose tap opens a Compose `ModalBottomSheet` prefilled with the bubble's body text and a Submit button gated on the §3.2 `canSubmit` rule (non-blank + differs-from-original)

#### Scenario: Root user bubble suppresses the Edit overflow

- **WHEN** the chat timeline includes a user bubble with a null `parentMessageId` (the conversation's root user message)
- **THEN** the bubble does not render the Edit overflow because the §3.2 endpoint contractually rejects edits with no parent

#### Scenario: Mid-conversation companion bubble renders Regenerate-from-here

- **WHEN** the chat timeline includes a companion bubble in the middle of the conversation (not the most recent)
- **THEN** the bubble renders a Regenerate-from-here overflow whose tap invokes the repository's `regenerateCompanionTurnAtTarget` call with the bubble's `messageId` as the target

#### Scenario: Most-recent companion bubble also renders Regenerate-from-here

- **WHEN** the chat timeline's most recent message is a companion bubble
- **THEN** the same Regenerate-from-here overflow appears (the §3.3 spec wording requires it on every companion bubble, not only mid-conversation)

### Requirement: ChatViewModel maintains a per-conversation active-path map and projects it back into bubble metadata

The system SHALL maintain a `Map<conversationId, Map<variantGroupId, activeIndex>>` in `ChatViewModel` state. On every recomposition, each rendered `ChatMessage.companionTurnMeta` MUST carry the `siblingActiveIndex` corresponding to that bubble's `variantGroupId`'s entry in the map (or 0 when the group has only one sibling). The map MUST be mutable via `selectVariantAt(conversationId, variantGroupId, newIndex)`, idempotent (no-op when newIndex equals the current active), and clamped to `[0, siblingCount - 1]`.

#### Scenario: Single-sibling group projects active index 0

- **WHEN** a conversation has a `variantGroupId` with one sibling (the original submit response)
- **THEN** the rendered bubble's `companionTurnMeta.siblingActiveIndex` is 0 and no chevron renders (per the §3.1 chevron-suppression rule)

#### Scenario: Multi-sibling group projects the map's active index

- **WHEN** a conversation has a `variantGroupId` with three siblings and the map records active index 2
- **THEN** the rendered bubble's `companionTurnMeta.siblingActiveIndex` is 2, the §3.1 chevrons render with a "3/3" caption, and the prev chevron is enabled while the next chevron is disabled (terminal-disable rule)

#### Scenario: selectVariantAt mutates the map and clamps out-of-bounds inputs

- **WHEN** the user taps the next chevron on a "2/3" sibling group
- **THEN** `selectVariantAt(conversationId, variantGroupId, 2)` flips the map entry from 1 → 2 and the bubble re-renders at the new active variant

- **WHEN** `selectVariantAt(conversationId, variantGroupId, 5)` is called on a 3-sibling group
- **THEN** the value clamps to 2 (the maximum valid index) without throwing

### Requirement: ChatViewModel exposes editUserTurn and regenerateFromHere handlers with lifecycle state

The system SHALL expose two handler entry-points on `ChatViewModel`: `editUserTurn(messageId, newDraftText)` and `regenerateFromHere(messageId)`. Each handler MUST surface in-flight + failed lifecycle state on the ViewModel so the affordance UI can render an in-flight indicator and an inline error with a retry. On success, the corresponding §3.2 / §3.3 active-path effect MUST be applied so the new variant becomes the active path.

#### Scenario: editUserTurn happy path applies the active-path effect on success

- **WHEN** `editUserTurn("user-msg-7", "rewritten text")` is called and the repository call returns the new user-message + companion-turn variants
- **THEN** the lifecycle state transitions in-flight → completed and the §3.2 `editUserBubbleActivePathEffect` flips the active-path map entries for the user-message's parent variantGroup and the companion-turn's parent variantGroup to the new variants

#### Scenario: editUserTurn failure surfaces an inline error with retry

- **WHEN** the repository call rejects the request (transport failure or non-2xx response)
- **THEN** the lifecycle state transitions in-flight → failed with the failure reason; the affordance UI renders the inline error and a retry button; tapping retry re-invokes the same `editUserTurn` without re-arming any user gesture

#### Scenario: regenerateFromHere supports mid-conversation invocation

- **WHEN** `regenerateFromHere("companion-msg-3")` is called on a companion bubble that is not the most recent
- **THEN** the repository call's `targetMessageId` is "companion-msg-3" and the resulting new sibling becomes the active variant for that group, leaving every later companion turn's variant tree untouched
