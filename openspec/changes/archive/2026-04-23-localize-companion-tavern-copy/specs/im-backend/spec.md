## ADDED Requirements

### Requirement: Backend persists and returns bilingual companion card content
The system SHALL persist English and Chinese authored values for each companion character's `displayName`, `roleLabel`, `summary`, and `openingLine`, and it MUST expose both language variants through the companion roster and draw-result APIs instead of collapsing companion content into one locale-specific string.

#### Scenario: Authenticated roster load includes both language variants
- **WHEN** an authenticated user requests the companion roster
- **THEN** each preset and owned companion card in the response includes both English and Chinese values for every required user-facing character field

#### Scenario: Draw response includes both language variants
- **WHEN** an authenticated user performs a companion draw
- **THEN** the returned draw-result card includes both English and Chinese values for every required user-facing character field

### Requirement: Published companion catalogs are backfilled to the bilingual contract
The system SHALL migrate existing shipped companion catalog rows to the bilingual companion-copy contract before serving them from published APIs, and it MUST NOT leave a shipped preset or draw-pool character with only one authored language variant after the migration is accepted.

#### Scenario: Existing seeded companion rows are migrated to bilingual content
- **WHEN** the backend upgrades the existing companion catalog created from the original single-language tavern rollout
- **THEN** each shipped preset and draw-pool character becomes retrievable with complete English and Chinese values for the required companion fields
