## ADDED Requirements

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
