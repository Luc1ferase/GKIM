## MODIFIED Requirements

### Requirement: Android client accesses protected infrastructure through service boundaries
The system SHALL access IM, feed, and AIGC backend capabilities through HTTPS and WebSocket service endpoints, and it MUST NOT embed direct PostgreSQL credentials or database trust material in the Android client runtime. Backend-side PostgreSQL connectivity MUST be configurable through deployment-managed secrets for the current provider, so switching away from Aiven to the operations-provided PostgreSQL instance does not require shipping raw database inputs inside the repository or the mobile app. For normal Android runtime, the app MUST resolve IM HTTP and realtime connectivity from a single trusted backend origin instead of asking end users to enter separate HTTP and WebSocket addresses manually. The WebSocket endpoint MUST be derived from that same backend origin by preserving authority and converting the transport scheme appropriately. Credential login, registration, startup bootstrap, contacts, and user-search flows MUST resolve their HTTP API target from the resolved backend origin or an already-authenticated session URL, rather than silently falling back to an opaque emulator-local `127.0.0.1` default when no session has been established yet. If manual IM endpoint override remains for emulator or deployment validation, it MUST live in a guarded developer-validation path and MUST NOT be required for ordinary users to use the app.

#### Scenario: Android app initializes IM connectivity from one backend origin
- **WHEN** the app configures IM connectivity for a normal user session
- **THEN** it resolves HTTP API traffic and derives the matching WebSocket endpoint from the same backend origin instead of requiring two separately entered endpoint values

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

#### Scenario: Ordinary users are not asked to type raw IM endpoints
- **WHEN** a normal user opens the production-facing app settings or onboarding flow
- **THEN** the app does not require that user to enter separate IM HTTP and WebSocket addresses before messaging can work

#### Scenario: Developer override remains available without changing the normal product contract
- **WHEN** a tester enters a guarded developer-validation flow for emulator or deployment verification
- **THEN** the app may accept a developer-managed backend origin override plus validation identity without exposing raw IM endpoint entry as a requirement for ordinary users

#### Scenario: Auth routes honor the resolved backend origin before session bootstrap
- **WHEN** an unauthenticated user opens login or registration before any authenticated session URL has been stored
- **THEN** the app sends auth requests to the resolved trusted backend origin or guarded developer override origin instead of silently assuming `http://127.0.0.1:18080/`

### Requirement: IM validation status is surfaced from Settings
The system SHALL present live IM connection and validation status inside `Settings > IM Validation` or an equivalent guarded validation surface, and it MUST place that status alongside the resolved backend-origin information used for validation and troubleshooting. Production-facing settings MUST NOT require separate raw HTTP and WebSocket endpoint fields for ordinary users. If a guarded developer-validation flow is provided, any manual override controls there MUST center on one backend-origin value rather than separate HTTP and WebSocket inputs.

#### Scenario: User opens the production-facing IM settings surface
- **WHEN** a normal user navigates to the IM connectivity or settings surface
- **THEN** the screen shows IM connection state and the active backend environment summary without asking that user to provide separate HTTP and WebSocket endpoint values

#### Scenario: Tester opens the guarded validation surface
- **WHEN** a tester navigates to the guarded IM validation workflow
- **THEN** the screen shows the current live IM status near the resolved backend-origin value and any allowed single-origin override controls needed for troubleshooting
