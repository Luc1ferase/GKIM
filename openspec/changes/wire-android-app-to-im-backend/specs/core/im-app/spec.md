## ADDED Requirements

### Requirement: Android messaging surfaces hydrate from the live IM backend
The system SHALL let the Android app authenticate against the live IM backend, hydrate conversation summaries and message history from backend HTTP APIs, and reconcile authenticated WebSocket events into the existing Messages and Chat UI state instead of relying only on seed-only in-memory messaging data.

#### Scenario: Messages screen bootstraps live conversation summaries
- **WHEN** the Android app starts an IM session with a configured development user
- **THEN** the app issues backend session/bootstrap requests and renders Messages conversation rows from backend conversation summaries instead of local seed-only data

#### Scenario: Chat screen loads backend history for the selected conversation
- **WHEN** the user opens a conversation that exists on the backend
- **THEN** the app loads message history from the backend history endpoint and renders those messages in Chat using the existing UI model contract

#### Scenario: Realtime events update visible chat state
- **WHEN** the backend emits authenticated WebSocket events for message send, receive, delivery, or read updates
- **THEN** the app reconciles those events into repository state and refreshes the visible Messages and Chat surfaces without treating the local repository as the source of truth

#### Scenario: Wiring failures remain visible to the user
- **WHEN** session, bootstrap, history, or realtime connection setup fails during live IM wiring
- **THEN** the app shows explicit integration failure state instead of silently falling back to seed-only success behavior

## MODIFIED Requirements

### Requirement: Android client accesses protected infrastructure through service boundaries
The system SHALL access IM, feed, and AIGC backend capabilities through HTTPS and WebSocket service endpoints, and it MUST NOT embed direct PostgreSQL credentials or database trust material in the Android client runtime. Backend-side PostgreSQL connectivity MUST be configurable through deployment-managed secrets for the current provider, so switching away from Aiven to the operations-provided PostgreSQL instance does not require shipping raw database inputs inside the repository or the mobile app. For the live IM path, the Android app MUST also support operator-managed IM HTTP and WebSocket endpoint inputs plus a development user selection so the app can connect to a configured backend environment without rebuilding the APK.

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

#### Scenario: Operator updates live IM runtime endpoints
- **WHEN** a tester or operator enters IM backend HTTP and WebSocket endpoint values plus a development user identity in the Android app
- **THEN** the app persists those service-boundary inputs locally and uses them for subsequent live IM sessions without bundling backend credentials or database secrets into the APK
