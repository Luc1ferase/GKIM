## ADDED Requirements

### Requirement: Backend supports friend request lifecycle
The system SHALL provide endpoints for sending, listing, accepting, and rejecting friend requests. A friend request creates a `friend_requests` row with status `pending`. Accepting a request creates a reciprocal contact pair and updates the status to `accepted`. Rejecting updates the status to `rejected`. The system MUST prevent duplicate pending requests between the same user pair. The system MUST prevent sending a friend request to an existing contact.

#### Scenario: User sends a friend request
- **WHEN** an authenticated user sends `POST /api/friends/request` with `toUserId`
- **THEN** the backend creates a `friend_requests` row with status `pending` and returns the request details

#### Scenario: Duplicate friend request is rejected
- **WHEN** an authenticated user sends a friend request to someone who already has a pending request from them
- **THEN** the backend returns HTTP 409 indicating a pending request already exists

#### Scenario: Friend request to existing contact is rejected
- **WHEN** an authenticated user sends a friend request to someone who is already a mutual contact
- **THEN** the backend returns HTTP 409 indicating the users are already contacts

#### Scenario: User lists pending friend requests
- **WHEN** an authenticated user sends `GET /api/friends/requests`
- **THEN** the backend returns all pending friend requests where the user is the recipient, including the sender's profile

#### Scenario: User accepts a friend request
- **WHEN** an authenticated user sends `POST /api/friends/requests/:id/accept` for a pending request addressed to them
- **THEN** the backend updates the request status to `accepted`, creates a reciprocal contact pair, and returns the updated request

#### Scenario: User rejects a friend request
- **WHEN** an authenticated user sends `POST /api/friends/requests/:id/reject` for a pending request addressed to them
- **THEN** the backend updates the request status to `rejected` and returns the updated request

#### Scenario: Accept/reject non-existent or already-handled request fails
- **WHEN** an authenticated user tries to accept or reject a request that does not exist, is not addressed to them, or is no longer pending
- **THEN** the backend returns HTTP 404 or HTTP 409 as appropriate

### Requirement: Android displays friend request UI in Contacts
The system SHALL show a pending friend request section at the top of the Contacts screen when there are pending incoming requests. Each pending request MUST show the sender's display name and avatar with accept/reject actions. The Contacts tab badge or indicator MUST reflect the count of pending incoming requests.

#### Scenario: Pending friend requests appear on Contacts screen
- **WHEN** the user opens the Contacts tab and has pending incoming friend requests
- **THEN** the screen shows a "Friend Requests" section above the contact list with each pending request and accept/reject actions

#### Scenario: User accepts a friend request from Contacts screen
- **WHEN** the user taps "Accept" on a pending friend request
- **THEN** the app calls the accept endpoint, the request disappears from the pending section, and the new contact appears in the contact list

#### Scenario: User rejects a friend request from Contacts screen
- **WHEN** the user taps "Reject" on a pending friend request
- **THEN** the app calls the reject endpoint and the request disappears from the pending section

### Requirement: Backend notifies online users of friend request events via WebSocket
The system SHALL push WebSocket events for friend request creation, acceptance, and rejection to online users who are party to the request. This ensures real-time updates without polling.

#### Scenario: Recipient receives friend request notification
- **WHEN** a user sends a friend request and the recipient is online
- **THEN** the backend pushes a `friend_request_received` event to the recipient's WebSocket session

#### Scenario: Sender receives acceptance notification
- **WHEN** a recipient accepts a friend request and the sender is online
- **THEN** the backend pushes a `friend_request_accepted` event to the sender's WebSocket session
