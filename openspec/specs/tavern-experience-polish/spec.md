# tavern-experience-polish Specification

## Purpose
TBD - created by archiving change tavern-experience-polish. Update Purpose after archive.
## Requirements
### Requirement: The tavern exposes a full-screen portrait large-view for every companion's avatar

The system SHALL provide a full-screen portrait large-view surface reachable from every avatar tap in the tavern roster, the chat header, and the chat bubble avatar. The surface MUST render the card's highest-resolution avatar source with pinch-to-zoom, single-finger pan, and double-tap to toggle between 1× and 2× zoom. Cards without an avatar MUST render a placeholder carrying the card's display name and an "Edit card" shortcut.

#### Scenario: Avatar tap routes to the large-view

- **WHEN** a user taps a companion's avatar on the tavern roster, the chat header, or a chat bubble
- **THEN** the app navigates to the portrait large-view surface for that card

#### Scenario: Pinch gestures scale the portrait

- **WHEN** a user pinches outward on the portrait large-view
- **THEN** the image scales up smoothly with the gesture; releasing pinches back in if scale <1, or snaps to the last scale within bounds

#### Scenario: Avatar-less card renders the placeholder

- **WHEN** the card's avatar asset is missing
- **THEN** the surface renders a placeholder with the card's display name and an "Edit card" shortcut

### Requirement: The alt-greeting picker renders localized previews and remembers the last selection per companion

The system SHALL render each opener option (the card's `firstMes` plus every `alternateGreetings` entry) as a card carrying a ~120-character localized preview in the user's active `AppLanguage`. A tap-to-preview interaction MUST open a modal showing the full greeting. The system MUST persist the last-selected alt-greeting per companion, and subsequent opener picker surfaces MUST default-highlight that selection with a "Remembered from last time" caption.

#### Scenario: Each option renders a 120-character preview

- **WHEN** the opener picker surface renders for a card with a `firstMes` plus two `alternateGreetings`
- **THEN** the picker lists three option cards, each carrying a ~120-character preview truncated with an ellipsis if the full greeting is longer, localized to the user's active `AppLanguage`

#### Scenario: Tap-to-preview opens the full greeting modal

- **WHEN** a user taps an option card's body (not its primary CTA)
- **THEN** a modal appears rendering the full greeting text, scrollable if long, with a Close affordance that returns the user to the picker

#### Scenario: Remembered default on subsequent opener render

- **WHEN** a user has previously selected an alt-greeting for this companion, then returns to the opener picker for the same companion
- **THEN** the previously-selected greeting carries a visual "Remembered from last time" caption and is default-highlighted

### Requirement: Chat exposes sibling-swipe navigation at every depth in the variant tree

The system SHALL render left/right sibling-swipe chevrons on every chat bubble whose variant group has more than one sibling, regardless of the bubble's depth in the conversation tree. A `(n / total)` caption MUST appear beside the chevrons. Tapping a chevron MUST persist the new active path for the bubble's variant group so that the entire conversation re-resolves to the newly-active downstream messages.

#### Scenario: Chevrons appear only on variant groups with siblings

- **WHEN** a bubble's variant group has exactly one sibling (the bubble itself)
- **THEN** neither chevron nor caption is rendered for that bubble

#### Scenario: Caption reflects current index

- **WHEN** a bubble is the second of five siblings in its variant group
- **THEN** the caption reads "2 / 5" beside the chevrons

#### Scenario: Active-path mutation re-resolves downstream

- **WHEN** a user taps the right chevron on a mid-conversation variant group
- **THEN** the conversation's active path shifts to the next sibling for that variant group and the downstream messages re-render according to the new sibling's descendants

### Requirement: Chat exposes an "Edit" action on every user bubble that creates a new sibling variant

The system SHALL expose an "Edit" action in the overflow of every user bubble. Invoking the action MUST open an edit sheet prefilled with the bubble's content; submitting the edit MUST create a new sibling under the same `parentMessageId` with a new `variantGroupId`, then kick off a companion turn for the edited sibling. The new user-branch plus its companion turn MUST become the active path; the original branch MUST remain persisted as a non-active sibling.

#### Scenario: Edit creates a sibling, not an overwrite

- **WHEN** a user edits a user bubble whose variant group originally had one sibling
- **THEN** after edit commit, the variant group has two siblings; the new sibling is active; the original sibling is preserved

#### Scenario: Edit kicks off a companion turn for the new branch

- **WHEN** the edit is committed and the companion turn stream begins
- **THEN** the new user-branch sibling shows the `Pending → Thinking → Streaming → Completed/Failed/Blocked/Timeout` lifecycle exactly as for a fresh turn

### Requirement: Chat exposes "Regenerate from here" on every companion bubble, not only the most recent

The system SHALL expose a "Regenerate from here" action in the overflow of every companion bubble (not just the most recent). Invoking the action MUST create a new sibling under the selected bubble's `variantGroupId`, preserving the prior sibling. The new sibling MUST become the active path; downstream messages that existed under the previously-active sibling MUST remain persisted as non-active descendants.

#### Scenario: Arbitrary-layer regenerate appends a sibling

- **WHEN** a user invokes "Regenerate from here" on a mid-conversation companion bubble whose variant group has one sibling
- **THEN** a new sibling is appended to the variant group; the new sibling becomes active; the original is preserved

#### Scenario: Downstream history survives under the inactive sibling

- **WHEN** regenerating from a bubble whose original sibling had downstream messages
- **THEN** those downstream messages remain addressable under the (now non-active) original sibling and are reachable by sibling-swiping back

### Requirement: A companion card optionally overrides the globally-active preset via `characterPresetId`

The system SHALL allow a companion card to carry an optional `characterPresetId` field. When a card with non-null `characterPresetId` is the active conversation's character, the allocator MUST substitute that preset for the user's globally-active preset in that conversation only. The chat chrome MUST visually surface the override through a "(card override)" suffix on the preset pill, and tapping the pill MUST route to the character's detail surface where the override can be cleared.

#### Scenario: Active conversation uses the card's preset when override is set

- **WHEN** a conversation's active card carries `characterPresetId = "preset-X"` and the user's globally-active preset is `preset-Y`
- **THEN** the allocator uses `preset-X` for that conversation's turns; other conversations continue to use `preset-Y`

#### Scenario: Override is visible in the chat chrome

- **WHEN** the active card has `characterPresetId` non-null
- **THEN** the chat chrome's preset pill renders the overridden preset's display name suffixed with a localized "(card override)" indicator

#### Scenario: Clearing the override reverts to the global preset

- **WHEN** a user clears the `characterPresetId` field on the card
- **THEN** the conversation's allocator reverts to the globally-active preset on the next turn; the pill's "(card override)" suffix disappears

### Requirement: A conversation can be exported as JSONL with active-path and full-tree variants

The system SHALL expose a conversation export as JSONL. The Android client MUST render an export dialog offering: an active-path-only vs. full-tree toggle, a target-language selector defaulted to the active `AppLanguage`, and a share-sheet vs. Downloads destination. The returned payload MUST emit one JSON object per line, each line containing `messageId`, `parentMessageId`, `variantGroupId`, `variantIndex`, `role`, `timestamp`, `content`, and the message's `extensions` bag.

#### Scenario: Active-path-only emits only the active-path messages

- **WHEN** a user exports a conversation with active-path-only selected
- **THEN** the payload contains one JSON object per message in the active path, ordered from root to the most recent active message

#### Scenario: Full-tree emits every message regardless of active path

- **WHEN** a user exports the same conversation with full-tree selected
- **THEN** the payload contains one JSON object per message in the conversation, including non-active branches

#### Scenario: Target routing delivers the payload

- **WHEN** a user selects Share sheet as the target
- **THEN** the system share sheet opens with the JSONL payload and a filename ending in `_<first8OfConversationId>.jsonl`

### Requirement: Relationship reset clears conversations plus memory per user-companion pair

The system SHALL provide a "Reset relationship" affordance on the character-detail surface behind a two-step confirmation dialog. Confirming MUST delete every `Conversation` between the user and this companion, clear the memory record (rolling summary + pinned facts) for the user-companion pair, and clear the last-selected alt-greeting. The card itself, the user's library data (presets, personas, lorebooks), and any lorebook bindings MUST NOT be affected. The operation MUST be idempotent.

#### Scenario: Two-step confirmation gate

- **WHEN** a user taps "Reset relationship" the first time
- **THEN** the affordance arms (enters a confirm-or-cancel state) with a localized destructive-action warning; a second tap commits the reset and a cancel dismisses without change

#### Scenario: Reset clears conversations and memory, preserves card

- **WHEN** a user confirms relationship reset on a companion with existing conversations and memory
- **THEN** subsequent reads return zero conversations for that companion, an empty memory record for that user-companion pair, and a cleared last-selected alt-greeting; the card record, preset library, persona library, and lorebook bindings are unchanged

#### Scenario: Reset is idempotent

- **WHEN** a user invokes relationship reset a second time on a companion that already has no conversations or memory
- **THEN** the operation succeeds with no error and no change to state

### Requirement: Gacha surfaces rarity probability and a duplicate-handling animation branch

The system SHALL, before a gacha draw, render the rarity / probability breakdown drawn from the backend catalog response. After a draw whose resulting card is already in the user's owned roster, the result animation MUST branch into a dedicated "Already owned" variant that renders a "Keep as bonus" CTA. Tapping the CTA MUST record a `bonusAwarded` event against the user's account; the consolation reward itself is deferred to a future slice (the event is a durable hook).

#### Scenario: Pre-draw UI shows rarity breakdown

- **WHEN** a user opens the gacha pre-draw surface
- **THEN** the UI renders a rarity breakdown (e.g., "Rare 5%, Epic 1%, Common 94%") sourced from the backend catalog response

#### Scenario: Duplicate triggers the "Already owned" variant

- **WHEN** a draw's resulting card id already appears in the user's owned roster
- **THEN** the result animation plays the "Already owned" variant and renders a "Keep as bonus" CTA

#### Scenario: Keep-as-bonus records the event

- **WHEN** a user taps "Keep as bonus" on the duplicate variant
- **THEN** the client calls the bonus-event endpoint, persisting a `bonusAwarded` event for the user + drawn card id

### Requirement: Character detail surfaces creator attribution when present

The system SHALL render an "About this card" sub-section on the character-detail surface exposing `creator`, `creatorNotes`, `characterVersion`, a linkified `stSource` from `extensions.st.stSource`, and formatted `stCreationDate` + `stModificationDate` values from `extensions.st.*`. Missing fields MUST be hidden (not rendered as empty rows). The `stSource` link MUST open in the system browser.

#### Scenario: Populated fields render in the sub-section

- **WHEN** a card carries non-empty `creator`, `creatorNotes`, and `characterVersion`
- **THEN** the sub-section renders each as a labeled row in the card-detail scroll

#### Scenario: Missing fields are hidden

- **WHEN** a card has an empty `creator` field
- **THEN** the sub-section omits the creator row entirely; no empty-row placeholder renders

#### Scenario: stSource link opens in the system browser

- **WHEN** a user taps the linkified `stSource` URL
- **THEN** the system browser opens the URL; the character-detail surface remains visible (under the browser) so returning feels seamless

