## ADDED Requirements

### Requirement: Backend provides user search by username or display name
The system SHALL provide a `GET /api/users/search?q=<query>` endpoint that returns users whose username or display name contains the query string (case-insensitive). The endpoint MUST require authentication, MUST exclude the requesting user from results, and MUST limit results to 20 entries. Each result MUST include the user's id, username, displayName, and avatarText, plus a `contactStatus` field indicating whether the user is already a contact, has a pending friend request, or is unrelated.

#### Scenario: Authenticated user searches for other users
- **WHEN** an authenticated user sends `GET /api/users/search?q=alice`
- **THEN** the backend returns up to 20 users whose username or display name contains "alice" (case-insensitive), excluding the requesting user, with contact status for each result

#### Scenario: Search returns empty for no matches
- **WHEN** an authenticated user searches with a query that matches no users
- **THEN** the backend returns an empty results array

### Requirement: Android provides a user search screen
The system SHALL provide a user search screen accessible from the Contacts tab that lets the user type a query, see matching users, and send friend requests to users who are not yet contacts. The screen MUST show each result's display name, username, and contact/request status.

#### Scenario: User searches and finds another user
- **WHEN** the user types a query in the search screen
- **THEN** the app displays matching users with their display name, username, and relationship status

#### Scenario: User sends a friend request from search results
- **WHEN** the user taps "Add friend" on a search result that has no existing contact or pending request
- **THEN** the app sends a friend request and updates the result status to "pending"
