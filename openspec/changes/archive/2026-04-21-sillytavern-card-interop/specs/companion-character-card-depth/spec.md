## MODIFIED Requirements

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
