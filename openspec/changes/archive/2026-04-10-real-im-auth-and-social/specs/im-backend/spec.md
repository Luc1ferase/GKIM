## MODIFIED Requirements

### Requirement: Backend issues user-bound IM sessions for the first milestone
The system SHALL provide both a production auth flow (register/login endpoints) and a development-safe session bootstrap flow for IM users. The production auth endpoints MUST validate credentials and return a stable user identity plus session token. The dev-session endpoint MUST remain available for testing. All session tokens MUST work for subsequent HTTP bootstrap requests and authenticated WebSocket connections.

#### Scenario: Production user obtains an IM session via login
- **WHEN** a registered user authenticates via `POST /api/auth/login` with valid credentials
- **THEN** the backend returns a session token tied to that user identity so contacts, conversations, and WebSocket events can be scoped per user

#### Scenario: Development user obtains an IM session
- **WHEN** a supported development user completes the backend dev-session bootstrap flow
- **THEN** the backend returns a session or token response tied to that user identity so contacts, conversations, and WebSocket events can be scoped per user

### Requirement: Backend persists direct-message state in PostgreSQL
The system SHALL persist 1:1 IM state in PostgreSQL, and it MUST store enough data to reconstruct contacts, conversations, paginated message history, unread counts, and delivery/read state after a client disconnects. The system MUST enforce that both sender and recipient are mutual contacts before accepting a direct message. The system MUST also store the `friend_requests` table for friend request lifecycle and a `username`/`password_hash` pair on the `users` table for credential authentication.

#### Scenario: Client bootstraps direct-message history
- **WHEN** an authenticated user requests their conversation bootstrap or paginated history
- **THEN** the backend returns conversation metadata, unread counts, and durable message records from PostgreSQL instead of transient in-memory-only state

#### Scenario: Offline recipient reconnects after missed messages
- **WHEN** a recipient reconnects after messages were sent while they were offline
- **THEN** the backend can rebuild unread state and message history for that user from PostgreSQL without message loss

#### Scenario: Message between non-contacts is rejected
- **WHEN** an authenticated user attempts to send a message to a user who is not a mutual contact
- **THEN** the backend rejects the message with an error indicating that the users must be contacts first

### Requirement: Backend delivers low-latency direct messages over WebSocket
The system SHALL use authenticated WebSocket sessions for active IM delivery, and it MUST push message, presence, delivery, read, and friend-request events to online clients without relying on polling for active conversations in the first single-node deployment.

#### Scenario: Online recipient receives a direct message
- **WHEN** an authenticated sender transmits a message to an online direct-message recipient
- **THEN** the backend durably accepts the message and pushes the resulting event to the recipient's live WebSocket session on the same server node

#### Scenario: Delivery/read updates reach the active conversation
- **WHEN** the recipient receives or reads a message while both participants remain connected
- **THEN** the backend emits delivery or read updates through the active WebSocket sessions so the conversation state stays current without polling refreshes

#### Scenario: Friend request events are pushed to online users
- **WHEN** a friend request is sent, accepted, or rejected while the other party is online
- **THEN** the backend pushes the corresponding friend-request event through the active WebSocket session
