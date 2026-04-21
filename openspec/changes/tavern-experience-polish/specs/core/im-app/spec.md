## ADDED Requirements

### Requirement: Android tavern + chat avatar taps route to a portrait large-view surface

The system SHALL wire every avatar tap on the tavern roster, chat header, and chat bubble avatar to navigate to a portrait large-view surface scoped to the tapped card. The surface MUST support pinch-to-zoom, single-finger pan, and double-tap to toggle between 1× and 2× zoom, with a graceful placeholder for cards without an avatar.

#### Scenario: Tavern roster avatar tap routes to large-view

- **WHEN** a user taps a card's avatar on the tavern roster
- **THEN** the app navigates to the portrait large-view surface for that card

#### Scenario: Chat header avatar tap routes to large-view

- **WHEN** a user taps the chat header's avatar for the active companion
- **THEN** the app navigates to the same portrait large-view surface as from the tavern roster

### Requirement: Android chat opener picker renders bilingual previews with remembered-default behavior

The system SHALL render the opener picker (introduced by `llm-text-companion-chat`) with ~120-character bilingual previews per option, tap-to-preview modals for the full greeting text, and a remembered-default highlight on the previously-selected greeting per companion. The remembered default MUST carry a localized "Remembered from last time" caption.

#### Scenario: Picker renders localized previews

- **WHEN** a user opens the opener picker for a card with three alt-greetings
- **THEN** each option card shows a ~120-character preview localized to the user's active `AppLanguage`

#### Scenario: Remembered default is highlighted

- **WHEN** a user had previously selected greeting B, then returns to the picker for the same companion after a relationship reset
- **THEN** greeting B is default-highlighted and carries the "Remembered from last time" caption

### Requirement: Android chat bubble rows support sibling-swipe, edit-user, and arbitrary-layer regenerate

The system SHALL extend `ChatMessageRow` so every bubble with `siblingCount > 1` renders left/right chevrons plus an `n / total` caption; every **user** bubble exposes an "Edit" overflow action; and every **companion** bubble exposes a "Regenerate from here" overflow action. All three affordances MUST operate on the variant-tree model from `llm-text-companion-chat` by creating new siblings under the appropriate `parentMessageId` or `variantGroupId`, never by overwriting existing messages.

#### Scenario: Sibling chevrons appear only on multi-sibling groups

- **WHEN** a bubble's variant group has a single sibling
- **THEN** no chevrons or caption render for that bubble

#### Scenario: Edit creates a user-turn sibling

- **WHEN** a user taps Edit on a user bubble, modifies the text, and submits
- **THEN** the client calls the edit endpoint with the new text and the bubble's `parentMessageId`; the returned new user sibling becomes the active path alongside its generated companion turn

#### Scenario: Regenerate from here creates a companion-turn sibling at that layer

- **WHEN** a user taps "Regenerate from here" on a mid-conversation companion bubble
- **THEN** the client calls the regenerate endpoint with that bubble's `messageId` as the target; the returned new sibling becomes the active path

### Requirement: Android chat chrome surfaces the per-character preset override

The system SHALL render the chat chrome's preset pill with the overridden preset's display name and a localized "(card override)" suffix when the active character carries `characterPresetId` non-null. Tapping the pill MUST route to the character-detail surface where the override can be cleared.

#### Scenario: Overridden pill renders the override label

- **WHEN** the active character has `characterPresetId = "preset-X"` and the user's globally-active preset is `preset-Y`
- **THEN** the pill renders `preset-X`'s display name with the "(card override)" suffix

#### Scenario: Tapping the pill routes to the character detail

- **WHEN** a user taps the overridden preset pill
- **THEN** the app routes to the active character's detail surface with the "Override preset" row focused for clearing

### Requirement: Android chat exposes a JSONL export dialog

The system SHALL add a "Export as JSONL" action to the chat overflow menu that opens an export dialog offering: active-path-only vs. full-tree toggle, a target-language selector defaulted to the user's active `AppLanguage`, and a share-sheet vs. Downloads destination. Invoking the dialog MUST call the backend export endpoint and route the returned payload to the chosen destination; the default filename MUST include the `_<first8OfConversationId>` suffix.

#### Scenario: Dialog defaults to active path + share sheet + active language

- **WHEN** a user opens the export dialog for the first time
- **THEN** active-path-only is pre-selected, the target is the share sheet, and the language defaults to the user's active `AppLanguage`

#### Scenario: Downloads destination writes the file

- **WHEN** a user selects the Downloads destination and confirms the export
- **THEN** the returned JSONL payload is written through `DownloadManager` with the conversation-hash-suffixed filename

### Requirement: Android character detail exposes the full-relationship-reset affordance

The system SHALL render a "Reset relationship" affordance on the character-detail surface behind a two-step confirmation dialog. The affordance MUST call the relationship-reset endpoint on confirm, show an inline error with a retry on failure, and update the in-memory caches (conversations list, memory record) to reflect the reset state on success.

#### Scenario: Two-step confirmation arms and commits

- **WHEN** a user taps "Reset relationship" once
- **THEN** the affordance enters the armed state with a destructive-action warning; a second tap commits the reset; a cancel affordance dismisses without change

#### Scenario: Successful reset refreshes in-memory caches

- **WHEN** the backend acknowledges the reset
- **THEN** the tavern surface shows zero conversations for this companion, the memory panel renders the empty-memory state, and no stale data lingers

### Requirement: Android gacha roster renders rarity probabilities and a duplicate-animation branch

The system SHALL extend the gacha roster flow so the pre-draw surface renders the rarity / probability breakdown from the backend catalog response. After a draw whose resulting card id appears in the user's owned roster, the result-animation MUST play the "Already owned" variant with a "Keep as bonus" CTA that records a `bonusAwarded` event.

#### Scenario: Pre-draw surface renders rarity breakdown

- **WHEN** the user opens the gacha pre-draw surface
- **THEN** a rarity breakdown derived from the backend catalog probabilities renders in the user's active `AppLanguage`

#### Scenario: Owned-card draw plays the duplicate variant

- **WHEN** a draw returns a card id that is already in the user's owned roster
- **THEN** the result animation plays the "Already owned" variant; tapping the "Keep as bonus" CTA records the bonus event

### Requirement: Android character detail renders an About-this-card sub-section for creator attribution

The system SHALL add an "About this card" sub-section to the character-detail surface rendering `creator`, `creatorNotes`, `characterVersion`, the linkified `stSource` from `extensions.st.stSource`, and formatted `stCreationDate` / `stModificationDate` values. Missing fields MUST be hidden (no empty-row placeholder). The `stSource` link MUST open in the system browser.

#### Scenario: Populated fields render labeled rows

- **WHEN** a card's `creator` and `characterVersion` are populated and `creatorNotes` is empty
- **THEN** the sub-section renders two rows (creator, characterVersion) and omits the creatorNotes row

#### Scenario: stSource opens the system browser

- **WHEN** a user taps a linkified `stSource` URL
- **THEN** the system browser opens the URL; the app remains in the background so returning resumes the character-detail surface
