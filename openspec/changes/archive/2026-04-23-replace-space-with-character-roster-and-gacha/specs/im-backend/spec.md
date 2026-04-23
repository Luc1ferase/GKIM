## ADDED Requirements

### Requirement: Backend persists character catalogs, owned roster state, and active selection
The system SHALL persist the preset companion catalog, per-user draw-acquired角色 ownership, and the user’s active selected角色 on the backend, and it MUST keep that state durable across reconnects and session recovery.

#### Scenario: User loads the tavern roster
- **WHEN** an authenticated user opens the tavern-style character surface
- **THEN** the backend can provide the preset catalog, the user’s owned roster, and the currently active selected角色 for that account

#### Scenario: User changes the active角色
- **WHEN** an authenticated user selects a preset or owned角色 as the active companion
- **THEN** the backend records that active selection durably so subsequent companion conversation startup uses the chosen persona

### Requirement: Backend records explicit draw outcomes for companion角色 cards
The system SHALL support a draw operation that yields a companion角色 outcome for the authenticated user, and it MUST record the result explicitly enough that the client can represent the draw result and the updated owned-roster state truthfully.

#### Scenario: User performs a角色 draw
- **WHEN** an authenticated user triggers the character draw flow
- **THEN** the backend returns the draw result together with the resulting ownership outcome needed to update the user’s roster

#### Scenario: Drawn角色 becomes available to the conversation system
- **WHEN** a user receives a角色 card through the draw flow and activates it
- **THEN** the backend makes that persona available as a valid companion identity for the ensuing conversation lifecycle
