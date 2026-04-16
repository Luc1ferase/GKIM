## ADDED Requirements

### Requirement: Backend accepts authenticated direct-image message uploads
The system SHALL accept authenticated direct-image message creation through a backend API that can carry binary image payloads plus optional caption text, and it MUST persist the attachment before emitting the resulting direct-message events.

#### Scenario: Sender uploads an image direct message
- **WHEN** an authenticated user submits an image message with a recipient identity, optional `clientMessageId`, and optional caption text
- **THEN** the backend stores the image attachment, creates or resolves the direct conversation, persists the message, and returns or broadcasts a message record containing the resolved conversation identifier plus attachment descriptor

#### Scenario: Conversation member downloads a stored image attachment
- **WHEN** an authenticated user who belongs to the conversation requests the stored attachment for a direct image message
- **THEN** the backend authorizes the membership check and returns the persisted image payload in a format the Android client can render

## MODIFIED Requirements

### Requirement: Backend persists direct-message state in PostgreSQL
The system SHALL persist 1:1 IM state in PostgreSQL, and it MUST store enough data to reconstruct contacts, conversations, paginated message history, unread counts, delivery/read state, and optional direct-message attachment descriptors after a client disconnects. The backend MUST require that direct-message participants are mutual contacts before a new message is accepted.

#### Scenario: Client bootstraps direct-message history
- **WHEN** an authenticated user requests their conversation bootstrap or paginated history
- **THEN** the backend returns conversation metadata, unread counts, durable message records, and any associated attachment descriptors from PostgreSQL instead of transient in-memory-only state

#### Scenario: Offline recipient reconnects after missed messages
- **WHEN** a recipient reconnects after text or image messages were sent while they were offline
- **THEN** the backend can rebuild unread state, message history, and attachment descriptors for that user from PostgreSQL without message loss

#### Scenario: Message between non-contacts is rejected
- **WHEN** an authenticated user attempts to send a direct message to a user who is not a mutual contact
- **THEN** the backend rejects the message instead of silently auto-creating the contact relationship

### Requirement: Backend delivers low-latency direct messages over WebSocket
The system SHALL use authenticated WebSocket sessions for active IM delivery, and it MUST push text-message events, image-message events with attachment descriptors, delivery/read updates, and friend-request events to online clients without relying on polling for active conversations in the first single-node deployment.

#### Scenario: Online recipient receives a direct message
- **WHEN** an authenticated sender transmits a text direct message to an online direct-message recipient
- **THEN** the backend durably accepts the message and pushes the resulting event to the recipient's live WebSocket session on the same server node

#### Scenario: Online recipient receives an image direct message
- **WHEN** an authenticated sender creates an image direct message while the recipient is online
- **THEN** the backend durably accepts the image message and pushes `message.sent` / `message.received` events containing the attachment descriptor needed to render the same image on both clients

#### Scenario: Delivery/read updates reach the active conversation
- **WHEN** the recipient receives or reads a text or image message while both participants remain connected
- **THEN** the backend emits delivery or read updates through the active WebSocket sessions so the conversation state stays current without polling refreshes

#### Scenario: Friend-request events reach online users
- **WHEN** a friend request is sent, accepted, or rejected while the other party is online
- **THEN** the backend pushes the corresponding friend-request event through the active WebSocket session
