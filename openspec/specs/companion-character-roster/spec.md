# companion-character-roster Specification

## Purpose
TBD - created by archiving change replace-space-with-character-roster-and-gacha. Update Purpose after archive.
## Requirements
### Requirement: System exposes a tavern-style companion character roster
The system SHALL expose AI companion identities as tavern-style character cards, and it MUST let users browse a roster that clearly distinguishes preset available角色 from user-owned draw-acquired角色. Each character card MUST present enough persona-facing metadata for users to choose a companion intentionally instead of guessing from a bare name alone.

#### Scenario: User browses preset companion cards
- **WHEN** the user opens the tavern-style role-selection surface
- **THEN** the system presents preset character cards with persona-facing identity and enough visible metadata to support intentional selection

#### Scenario: User reviews owned draw-acquired cards
- **WHEN** the user has already obtained角色 through the draw flow
- **THEN** the system shows those owned cards in a durable roster instead of treating every draw result as disposable one-time output

### Requirement: Character draw behavior remains explicit and non-deceptive
The system SHALL support a draw flow that can grant companion character cards to the user, and it MUST represent the outcome clearly enough that users can tell whether they obtained a usable角色 card and whether it is now part of their owned roster.

#### Scenario: User performs a character draw
- **WHEN** the user triggers the draw flow from the tavern surface
- **THEN** the system returns an explicit draw outcome that identifies the resulting角色 card and whether it was added to the user’s owned roster

#### Scenario: User activates a roster card for companion chat
- **WHEN** the user selects a preset or owned角色 card as the active companion
- **THEN** the system records that active selection so subsequent companion conversation entry uses the chosen persona instead of a generic default

