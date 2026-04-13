# im-backend Specification

## Purpose
TBD - created by archiving change bootstrap-rust-im-backend. Update Purpose after archive.
## Requirements
### Requirement: Rust IM backend boots with secret-managed server configuration
The system SHALL provide a Rust backend service that runs as an HTTP and WebSocket server on the target Ubuntu host, and it MUST load PostgreSQL connection details, bind address, and other operational secrets from backend-only environment inputs instead of checked-in credentials. The published HTTP base URL used for Android emulator validation MUST resolve to a host-reachable service endpoint for auth and bootstrap traffic, and validation guidance MUST NOT assume that `127.0.0.1` inside the emulator/device runtime is automatically the backend host.

#### Scenario: Backend service boots on the Ubuntu host
- **WHEN** the backend process starts on the Ubuntu server at `124.222.15.128`
- **THEN** it reads its runtime configuration from secret-managed environment values, establishes PostgreSQL connectivity, and exposes a health-checkable service endpoint without requiring database or SSH passwords inside the repository

#### Scenario: Emulator-facing auth API target is host-reachable
- **WHEN** operators select the HTTP base URL used by the Android emulator for registration, login, and bootstrap validation
- **THEN** that URL points to a backend service endpoint that is reachable from the emulator/runtime boundary instead of relying on device-local `127.0.0.1` by assumption

### Requirement: Backend issues user-bound IM sessions for the first milestone
The system SHALL provide both a production auth flow and a development-safe session bootstrap flow for IM users, and it MUST return a stable user identity plus credentials that the same client can use for subsequent HTTP bootstrap requests and authenticated WebSocket connections.

#### Scenario: Production user obtains an IM session
- **WHEN** a registered user completes the backend credential-login flow
- **THEN** the backend returns a session or token response tied to that user identity so contacts, conversations, and WebSocket events can be scoped per user

#### Scenario: Development user obtains an IM session
- **WHEN** a supported development user completes the backend bootstrap flow
- **THEN** the backend returns a session or token response tied to that user identity so contacts, conversations, and WebSocket events can be scoped per user

### Requirement: Backend persists direct-message state in PostgreSQL
The system SHALL persist 1:1 IM state in PostgreSQL, and it MUST store enough data to reconstruct contacts, conversations, paginated message history, unread counts, and delivery/read state after a client disconnects. The backend MUST require that direct-message participants are mutual contacts before a new message is accepted.

#### Scenario: Client bootstraps direct-message history
- **WHEN** an authenticated user requests their conversation bootstrap or paginated history
- **THEN** the backend returns conversation metadata, unread counts, and durable message records from PostgreSQL instead of transient in-memory-only state

#### Scenario: Offline recipient reconnects after missed messages
- **WHEN** a recipient reconnects after messages were sent while they were offline
- **THEN** the backend can rebuild unread state and message history for that user from PostgreSQL without message loss

#### Scenario: Message between non-contacts is rejected
- **WHEN** an authenticated user attempts to send a direct message to a user who is not a mutual contact
- **THEN** the backend rejects the message instead of silently auto-creating the contact relationship

### Requirement: Backend delivers low-latency direct messages over WebSocket
The system SHALL use authenticated WebSocket sessions for active IM delivery, and it MUST push message, presence, delivery, read, and friend-request events to online clients without relying on polling for active conversations in the first single-node deployment.

#### Scenario: Online recipient receives a direct message
- **WHEN** an authenticated sender transmits a message to an online direct-message recipient
- **THEN** the backend durably accepts the message and pushes the resulting event to the recipient's live WebSocket session on the same server node

#### Scenario: Delivery/read updates reach the active conversation
- **WHEN** the recipient receives or reads a message while both participants remain connected
- **THEN** the backend emits delivery or read updates through the active WebSocket sessions so the conversation state stays current without polling refreshes

#### Scenario: Friend-request events reach online users
- **WHEN** a friend request is sent, accepted, or rejected while the other party is online
- **THEN** the backend pushes the corresponding friend-request event through the active WebSocket session

### Requirement: Backend registers and authenticates credentialed IM accounts
The system SHALL provide production account registration and credential login endpoints for GKIM users in addition to the existing development-session bootstrap flow. `POST /api/auth/register` MUST validate username, password, and display-name input, persist a hashed password, and return a session token plus user profile. `POST /api/auth/login` MUST validate stored credentials and return a session token plus user profile for valid credentials.

#### Scenario: New user registers successfully
- **WHEN** a client submits `POST /api/auth/register` with a unique username, a valid password, and a valid display name
- **THEN** the backend creates the user record, stores a password hash, and returns an authenticated session payload for that user

#### Scenario: Duplicate username is rejected during registration
- **WHEN** a client submits `POST /api/auth/register` with a username that already exists
- **THEN** the backend returns a conflict response instead of creating a duplicate identity

#### Scenario: Credential login returns a user-bound IM session
- **WHEN** a registered user submits `POST /api/auth/login` with valid credentials
- **THEN** the backend returns a token tied to that user identity for subsequent bootstrap, history, and WebSocket requests

### Requirement: Backend supports authenticated user discovery and friend-request lifecycle
The system SHALL let authenticated users search for other users and manage a friend-request lifecycle before direct messaging access is granted. Search results MUST indicate relationship state, and accepted requests MUST create the reciprocal contact relationship required by direct messaging.

#### Scenario: Search returns users with relationship state
- **WHEN** an authenticated user queries `GET /api/users/search`
- **THEN** the backend returns matching users excluding the requester, along with the contact or pending-request state for each result

#### Scenario: Accepting a friend request creates mutual contacts
- **WHEN** an authenticated recipient accepts a pending friend request
- **THEN** the backend marks the request accepted and creates reciprocal contact records for both users

#### Scenario: Rejecting a friend request preserves non-contact state
- **WHEN** an authenticated recipient rejects a pending friend request
- **THEN** the backend marks the request rejected and does not create reciprocal contacts

### Requirement: Backend supports remote deployment and debugging on the target Ubuntu host
The system SHALL include a repeatable remote deployment/debug workflow for the Ubuntu host at `124.222.15.128`, and it MUST let each accepted implementation slice be started, inspected, and smoke-tested on that server through SSH-accessible operational commands without committing the SSH password.

#### Scenario: Accepted backend slice is smoke-tested on the server
- **WHEN** a backend implementation slice is accepted for delivery
- **THEN** the repository provides the scripts, service shape, or documented commands needed to deploy that slice to `124.222.15.128`, inspect logs or service status, and run a remote smoke test before the next slice begins
