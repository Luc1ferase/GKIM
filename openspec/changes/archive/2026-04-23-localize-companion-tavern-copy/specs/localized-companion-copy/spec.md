## ADDED Requirements

### Requirement: Companion character copy is defined as paired English and Chinese authored content
The system SHALL define each shipped companion character's user-facing copy as explicit English and Chinese authored values, and it MUST apply that bilingual contract to `displayName`, `roleLabel`, `summary`, and `openingLine` instead of treating any single-language string as the canonical source.

#### Scenario: New companion definition includes both language variants
- **WHEN** a preset or draw-pool companion character is authored for shipment
- **THEN** its definition includes English and Chinese values for each required user-facing field

#### Scenario: Consumer resolves copy deterministically by language
- **WHEN** a client or service needs to render companion content for English or Chinese UI
- **THEN** it can resolve the matching authored variant from the shared bilingual companion-copy contract without guessing from a single-language fallback string

### Requirement: Shipped companion copy uses authored bilingual content instead of runtime translation
The system SHALL ship reviewed English and Chinese companion copy as part of the character definition, and it MUST NOT depend on runtime machine translation or online translation services to synthesize tavern-facing shipped content.

#### Scenario: Seeded or cached companion card renders offline
- **WHEN** the app renders a seeded or cached companion character card while offline
- **THEN** the required English and Chinese copy is already present in the companion data needed by the client
