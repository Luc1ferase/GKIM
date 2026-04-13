## ADDED Requirements

### Requirement: Backend supports account registration and login for app users
The system SHALL provide backend-owned registration and login endpoints for GKIM accounts, and it MUST authenticate users with persistent account identities instead of relying only on development-session shortcuts.

#### Scenario: User registers a new account
- **WHEN** a client submits a valid new account ID and password for registration
- **THEN** the backend creates the account, persists the credentialed identity, and returns an authenticated session or token response

#### Scenario: User logs in with an existing account
- **WHEN** a client submits valid credentials for an existing account
- **THEN** the backend authenticates the account and returns an authenticated session or token response usable by subsequent app and IM APIs

### Requirement: Backend supports adding contacts by account ID
The system SHALL let an authenticated account locate another user by exact account ID, and it MUST persist the resulting contact relationship for later messaging and bootstrap queries.

#### Scenario: Authenticated user adds another account by ID
- **WHEN** an authenticated user submits another existing account's ID
- **THEN** the backend creates or returns the contact relationship and makes that user available to downstream IM/contact queries

#### Scenario: Invalid account add is rejected
- **WHEN** an authenticated user submits a nonexistent, duplicate, or self-targeting account ID
- **THEN** the backend rejects the request without creating a broken or duplicate contact record

## MODIFIED Requirements

### Requirement: Backend issues user-bound IM sessions for the first milestone
The system SHALL provide account-backed authenticated session flows for IM users, and it MUST return a stable authenticated identity plus credentials that the same client can use for subsequent HTTP bootstrap requests and authenticated WebSocket connections.

#### Scenario: Authenticated account obtains an IM session
- **WHEN** a registered account successfully completes login or registration
- **THEN** the backend returns a session or token response tied to that persistent account identity so contacts, conversations, and WebSocket events can be scoped per account

#### Scenario: Unauthenticated client cannot bootstrap IM
- **WHEN** a client attempts to access IM bootstrap or authenticated WebSocket flows without a valid account-backed session
- **THEN** the backend rejects the request instead of falling back to a development-only identity shortcut
