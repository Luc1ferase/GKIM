## ADDED Requirements

### Requirement: Android app exercises live IM backend flows during emulator validation
The system SHALL let the Android app authenticate through the backend development session flow, hydrate conversations and message history from the live IM backend, and reconcile authenticated WebSocket events into visible chat state during Android emulator validation runs against a locally containerized backend.

#### Scenario: Emulator validation bootstraps live conversation data
- **WHEN** a tester starts an IM validation session on the Android app in the emulator with a configured development user
- **THEN** the app issues the backend session/bootstrap requests and renders conversation state from live backend data instead of seed-only in-memory messaging rows

#### Scenario: Emulator validation drives realtime send and receipt updates
- **WHEN** the tester sends a message from the Android app in the emulator while the backend and counterpart session are online
- **THEN** the app updates the visible conversation from the live backend send, receive, delivered, and read flows instead of relying on local-only message append behavior

#### Scenario: Emulator validation recovers after reconnect or relaunch
- **WHEN** the Android app loses its realtime connection or is relaunched during an emulator validation session
- **THEN** it can recover current conversation state from backend bootstrap/history APIs and resume WebSocket synchronization without requiring a rebuild or seed reset

#### Scenario: Emulator validation surfaces backend failures explicitly
- **WHEN** session issuance, bootstrap loading, history retrieval, or realtime connection setup fails in the Android emulator
- **THEN** the app shows an explicit validation/debug failure state instead of silently falling back to placeholder success behavior

## MODIFIED Requirements

### Requirement: Android client accesses protected infrastructure through service boundaries
The system SHALL access IM, feed, and AIGC backend capabilities through HTTPS and WebSocket service endpoints, and it MUST NOT embed direct PostgreSQL credentials or database trust material in the Android client runtime. Backend-side PostgreSQL connectivity MUST be configurable through deployment-managed secrets for the current provider, so switching away from Aiven to the operations-provided PostgreSQL instance does not require shipping raw database inputs inside the repository or the mobile app. For IM emulator validation, the Android app MUST also support operator-managed HTTP and WebSocket endpoint inputs plus a development user selection so the Android emulator can target a host-published or deployed backend service without rebuilding the APK.

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

#### Scenario: Operator updates IM validation endpoints in the emulator
- **WHEN** a tester enters or selects IM backend endpoint values and a development user identity in the Android emulator
- **THEN** the app persists those service-boundary inputs locally for subsequent validation sessions without bundling backend credentials or database secrets into the APK
