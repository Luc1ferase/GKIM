# companion-character-card-depth Specification

## Purpose
Captures the companion character card as a full persona authoring record — system prompt, personality, scenario framing, example dialogue, first-message greeting, alternate greetings, tags, creator attribution, character version, and a forward-compatible extensions bag — using the bilingual `LocalizedText` contract for every prose field. Defines the authoring surfaces (tavern character detail, character editor) and the ownership + mutability rules for user-authored, preset, and draw-acquired cards.
## Requirements
### Requirement: Companion character cards carry a full persona authoring record

The system SHALL represent each companion character card as a persona authoring record that includes system prompt, personality, scenario framing, example dialogue, first-message greeting, alternate greetings, tags, creator attribution, character version, optional local avatar URI, and a forward-compatible extensions bag. Authored prose fields (`systemPrompt`, `personality`, `scenario`, `exampleDialogue`, `firstMes`, each `alternateGreetings` entry) MUST be bilingual via the existing `LocalizedText` contract. Non-prose metadata (`tags`, `creator`, `creatorNotes`, `characterVersion`) MUST stay as plain strings. The record MUST keep the existing `id`, `avatarText`, `accent`, and `source` fields unchanged so prior tavern rendering, draw, and active-selection flows continue to work. The `extensions` bag MUST reserve the `st.*` namespace for SillyTavern interop payloads: known stable keys (`stPostHistoryInstructions`, `stGroupOnlyGreetings`, `stDepthPrompt`, `stNickname`, `stSource`, `stCreationDate`, `stModificationDate`, `stAssets`) and the open `st.<otherKey>` space for unknown-to-us V3 extension subkeys. The `avatarUri` field MUST accept both user-captured images and server-stored re-encoded images resulting from PNG imports.

#### Scenario: Card exposes persona instructions

- **WHEN** the app resolves a companion card for any tavern or chat surface
- **THEN** the resolved projection includes the active-language system prompt, personality, scenario, example dialogue, first-message greeting, and alternate greetings list drawn from the card's `LocalizedText` fields

#### Scenario: Forward-compatible extensions bag round-trips

- **WHEN** a card is persisted or transferred through the companion roster path
- **THEN** unknown structured fields are preserved in the card's `extensions` object instead of being dropped silently

#### Scenario: Reserved st.* namespace preserves imported ST fields across persist and export

- **WHEN** a card is imported from a SillyTavern payload and then re-exported in the same format
- **THEN** every ST field originally present in `extensions.st.*` (stable keys and `st.<otherKey>` entries) is preserved verbatim on persist and emitted verbatim on export, so the round-trip does not lose data

#### Scenario: avatarUri accepts both user captures and re-encoded imports

- **WHEN** a card's avatar originates either from a user-selected local image or from the re-encoded PNG produced by SillyTavern import
- **THEN** the card's `avatarUri` field points to the corresponding asset through a uniform reference, and downstream renderers do not distinguish between the two sources

### Requirement: Companion roster supports user-authored cards while preserving preset content

The system SHALL let authenticated users create new companion character cards and update cards they authored, and it MUST prevent modification or deletion of preset-sourced cards. Draw-acquired cards MAY be edited by their owner but MUST NOT be deletable through the first-authored editor flow.

#### Scenario: User creates a new companion card

- **WHEN** an authenticated user finalizes the character editor form with required fields populated
- **THEN** the system persists the new card as a user-authored companion, exposes it in the owned roster, and makes it activatable for companion chat

#### Scenario: User edits a draw-acquired card

- **WHEN** an authenticated user opens the editor for a draw-acquired card they own
- **THEN** the system accepts edits to the persona authoring record and keeps the card in the owned roster

#### Scenario: Preset cards remain canonical

- **WHEN** a caller attempts to update or delete a card whose source is `Preset`
- **THEN** the system rejects the mutation with an explicit failure and leaves the preset card unchanged

### Requirement: Character detail and editor surfaces expose the deeper persona record

The system SHALL provide a tavern character detail surface that renders the resolved persona authoring record for any card, and it SHALL provide a character editor surface that can create new user-authored cards and update editable cards. The detail surface MUST expose an activation action that sets the card as the active companion. The editor MUST accept bilingual entry for every prose field and MUST let the user select a local avatar image.

#### Scenario: User opens character detail from the tavern roster

- **WHEN** the user taps a companion card in the tavern roster
- **THEN** the app navigates to the character detail surface and shows the resolved persona authoring record, including authoring metadata and opening-greeting information

#### Scenario: User creates a new card from the tavern

- **WHEN** the user opens the create-card entry point from the tavern surface and saves a filled editor form
- **THEN** the app persists a new user-authored companion card and returns the user to the tavern surface with the new card visible in the owned roster

#### Scenario: User edits an editable card

- **WHEN** the user opens the editor for a non-preset card and saves updated content
- **THEN** the app persists the update and the detail surface reflects the new content on return

