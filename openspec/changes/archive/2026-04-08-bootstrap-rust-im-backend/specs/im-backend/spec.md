## ADDED Requirements

### Requirement: Rust IM backend boots with secret-managed server configuration
The system SHALL provide a Rust backend service that runs as an HTTP and WebSocket server on the target Ubuntu host, and it MUST load PostgreSQL connection details, bind address, and other operational secrets from backend-only environment inputs instead of checked-in credentials.

#### Scenario: Backend service boots on the Ubuntu host
- **WHEN** the backend process starts on the Ubuntu server at `124.222.15.128`
- **THEN** it reads its runtime configuration from secret-managed environment values, establishes PostgreSQL connectivity, and exposes a health-checkable service endpoint without requiring database or SSH passwords inside the repository

### Requirement: Backend issues user-bound IM sessions for the first milestone
The system SHALL provide a development-safe session bootstrap flow for IM users, and it MUST return a stable user identity plus credentials that the same client can use for subsequent HTTP bootstrap requests and authenticated WebSocket connections.

#### Scenario: Development user obtains an IM session
- **WHEN** a supported development user completes the backend bootstrap flow
- **THEN** the backend returns a session or token response tied to that user identity so contacts, conversations, and WebSocket events can be scoped per user

### Requirement: Backend persists direct-message state in PostgreSQL
The system SHALL persist 1:1 IM state in PostgreSQL, and it MUST store enough data to reconstruct contacts, conversations, paginated message history, unread counts, and delivery/read state after a client disconnects.

#### Scenario: Client bootstraps direct-message history
- **WHEN** an authenticated user requests their conversation bootstrap or paginated history
- **THEN** the backend returns conversation metadata, unread counts, and durable message records from PostgreSQL instead of transient in-memory-only state

#### Scenario: Offline recipient reconnects after missed messages
- **WHEN** a recipient reconnects after messages were sent while they were offline
- **THEN** the backend can rebuild unread state and message history for that user from PostgreSQL without message loss

### Requirement: Backend delivers low-latency direct messages over WebSocket
The system SHALL use authenticated WebSocket sessions for active IM delivery, and it MUST push message, presence, delivery, and read events to online clients without relying on polling for active conversations in the first single-node deployment.

#### Scenario: Online recipient receives a direct message
- **WHEN** an authenticated sender transmits a message to an online direct-message recipient
- **THEN** the backend durably accepts the message and pushes the resulting event to the recipient's live WebSocket session on the same server node

#### Scenario: Delivery/read updates reach the active conversation
- **WHEN** the recipient receives or reads a message while both participants remain connected
- **THEN** the backend emits delivery or read updates through the active WebSocket sessions so the conversation state stays current without polling refreshes

### Requirement: Backend supports remote deployment and debugging on the target Ubuntu host
The system SHALL include a repeatable remote deployment/debug workflow for the Ubuntu host at `124.222.15.128`, and it MUST let each accepted implementation slice be started, inspected, and smoke-tested on that server through SSH-accessible operational commands without committing the SSH password.

#### Scenario: Accepted backend slice is smoke-tested on the server
- **WHEN** a backend implementation slice is accepted for delivery
- **THEN** the repository provides the scripts, service shape, or documented commands needed to deploy that slice to `124.222.15.128`, inspect logs or service status, and run a remote smoke test before the next slice begins
