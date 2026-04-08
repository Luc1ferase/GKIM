## MODIFIED Requirements

### Requirement: Android client accesses protected infrastructure through service boundaries
The system SHALL access IM, feed, and AIGC backend capabilities through HTTPS and WebSocket service endpoints, and it MUST NOT embed direct PostgreSQL credentials or database trust material in the Android client runtime. Backend-side PostgreSQL connectivity MUST be configurable through deployment-managed secrets for the current provider, so switching away from Aiven to the operations-provided PostgreSQL instance does not require shipping raw database inputs inside the repository or the mobile app.

#### Scenario: Android app initializes remote connectivity
- **WHEN** the app configures its connected services
- **THEN** it uses API base URLs and WebSocket endpoints instead of a direct PostgreSQL DSN

#### Scenario: Protected infrastructure material stays outside the APK
- **WHEN** database credentials or database CA trust material are required
- **THEN** they are held by backend infrastructure and are not packaged into the Android app

#### Scenario: Backend Postgres provider changes without mobile database coupling
- **WHEN** operations switches backend infrastructure from the previous Aiven database to a replacement PostgreSQL host such as `124.222.15.128:5432`
- **THEN** the checked-in app contract remains centered on backend service endpoints and treats the database target as backend runtime configuration only

#### Scenario: Raw database secrets are not committed as product configuration
- **WHEN** backend deployment needs the PostgreSQL role, password, optional database name, or optional TLS inputs for the replacement database
- **THEN** those values are supplied through untracked environment or secret-management inputs rather than committed to OpenSpec artifacts, Android assets, or versioned default configuration files

#### Scenario: Old Aiven certificate assumptions do not remain the default trust story
- **WHEN** active repository guidance documents backend PostgreSQL trust inputs after the provider switch
- **THEN** it no longer treats the previous Aiven `ca.pem` flow as the standing default and only documents replacement trust material if the current backend actually requires it
