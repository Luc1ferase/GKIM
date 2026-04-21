## ADDED Requirements

### Requirement: Backend persists the full companion persona authoring record

The system SHALL persist companion character cards with every field of the persona authoring record, and it MUST expose those fields through the roster and active-selection APIs. The authored prose fields — system prompt, personality, scenario, example dialogue, first-message greeting, and each alternate greeting — MUST be stored and returned as bilingual English/Chinese text, consistent with the existing `localized-companion-copy` contract. Tags, creator attribution, creator notes, and character version MUST be stored as plain strings. The record MUST include a forward-compatible extensions object that preserves unknown structured fields across write and read roundtrips.

#### Scenario: Roster response carries the deep persona record

- **WHEN** an authenticated client requests the companion roster or active-selection endpoints
- **THEN** the backend response exposes every persona authoring field with the bilingual contract for prose fields and returns an `extensions` object that preserves unknown structured data

#### Scenario: Legacy single-field records are backfilled before serving the deep contract

- **WHEN** the backend migration runs against companion rows authored before this contract
- **THEN** the migration backfills the new prose fields for shipped preset and drawable cards so roster responses never expose a partially populated persona record

### Requirement: Backend supports user-authored companion cards with preset immutability

The system SHALL accept create and update operations for user-authored or draw-acquired companion cards belonging to the requesting user, and it MUST reject attempts to mutate preset-sourced cards. Deletion MAY be supported for user-authored cards; it MUST NOT be supported for preset cards and SHOULD NOT be supported for draw-acquired cards in the first-authored editor flow.

#### Scenario: User creates a new companion card through the backend

- **WHEN** an authenticated client submits a new companion card tied to the requesting user with all required persona authoring fields populated
- **THEN** the backend persists the card, associates it with the requesting user, and returns it with a stable id

#### Scenario: Preset cards reject mutation

- **WHEN** any client submits an update or delete request targeting a card whose source is `Preset`
- **THEN** the backend rejects the request with an explicit failure and leaves the preset record unchanged

#### Scenario: Draw-acquired card accepts edits but rejects delete in the first-authored flow

- **WHEN** the owner of a draw-acquired card submits an edit through the character editor flow
- **THEN** the backend persists the edit; if the same client attempts to delete the same card through that flow, the backend rejects the delete with an explicit failure
