## ADDED Requirements

### Requirement: Tavern surface exposes character detail and editor flows

The system SHALL let authenticated users open a character detail surface for any roster card and open a character editor surface for user-authored or draw-acquired cards. The detail surface MUST show the resolved persona authoring record and an activation action. The editor MUST support creating new cards and updating editable cards with bilingual prose input and optional avatar selection.

#### Scenario: User navigates from tavern to character detail

- **WHEN** the user taps a companion card on the tavern surface
- **THEN** the app opens a character detail route that renders the card's persona authoring record in the active language and shows an activation action

#### Scenario: User enters the editor to author a new card

- **WHEN** the user invokes the create-card entry point from the tavern surface
- **THEN** the app opens a character editor route that accepts bilingual entries for system prompt, personality, scenario, example dialogue, first-message greeting, alternate greetings, tags, creator metadata, and avatar image selection

#### Scenario: User saves or cancels the editor

- **WHEN** the user confirms Save or Cancel from the editor route
- **THEN** the app either persists the new or updated card and returns to the prior surface, or discards edits and returns without modifying roster state

### Requirement: Companion chat entry uses the full persona authoring record

The system SHALL propagate the full persona authoring record (system prompt, personality, scenario, example dialogue, greetings) along with the selected card's bilingual display metadata when the user activates a companion and routes into chat. Companion chat entry MUST NOT rely on only the card's display summary to identify persona context.

#### Scenario: User activates a card and enters chat

- **WHEN** the user activates a card from the tavern detail surface and opens the companion conversation
- **THEN** the conversation entry path carries the full persona authoring record associated with the active card so downstream chat flows can use persona instructions rather than only the summary string
