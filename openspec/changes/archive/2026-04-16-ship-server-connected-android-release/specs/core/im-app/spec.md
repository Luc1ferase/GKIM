## ADDED Requirements

### Requirement: Tagged Android release behaves as a real server-connected IM client
The system SHALL ship tagged Android release builds with a bundled deployed IM backend origin that is sufficient to derive both HTTP and WebSocket endpoints for the authenticated IM experience, and it MUST NOT require the user to manually enter separate backend addresses before logging in to the published APK.

#### Scenario: Release build resolves IM transport from the bundled backend origin
- **WHEN** a user installs a tagged Android release APK built from GitHub
- **THEN** the app can resolve its IM HTTP and WebSocket targets from the bundled backend origin without first requiring operator-only endpoint entry in the UI

#### Scenario: Release build does not depend on placeholder service endpoints
- **WHEN** the tagged Android release APK initializes its core IM runtime wiring
- **THEN** the release path does not depend on placeholder `example.com` service endpoints to power login, contacts, conversations, or realtime messaging

### Requirement: Authenticated Android IM runtime prefers the real stored session over development bootstrap
The system SHALL initialize the authenticated Android IM experience from the stored credential session token and backend bootstrap payload when a user has logged in or registered successfully, and it MUST NOT silently replace that user session with a default development-session identity in shipped builds.

#### Scenario: Login leads into the same authenticated IM identity
- **WHEN** a user logs in or registers successfully in the shipped Android app
- **THEN** the subsequent Messages and Contacts experience hydrates backend state using that stored authenticated session rather than issuing an unrelated development-session bootstrap

#### Scenario: Unauthenticated release startup stays in the auth flow
- **WHEN** the shipped Android app launches without a valid stored authenticated session
- **THEN** it remains in the welcome/login/register flow instead of auto-entering the IM shell through a default development-session identity

### Requirement: Authenticated contacts and conversations avoid seed-first release behavior
The system SHALL render authenticated Contacts and Messages state from live backend bootstrap, search, friend-request, and conversation data for release users, and it MUST NOT treat seed contacts or seed conversations as the primary data source for the shipped authenticated IM experience.

#### Scenario: Authenticated Contacts uses live backend state
- **WHEN** an authenticated release user opens the Contacts tab
- **THEN** the visible contacts and pending request actions reflect backend-backed relationship state instead of a seed-only contact list

#### Scenario: Authenticated Messages uses live backend conversations
- **WHEN** an authenticated release user opens the Messages tab
- **THEN** the visible conversation list reflects backend bootstrap/history state instead of a seed-only in-memory conversation catalog
