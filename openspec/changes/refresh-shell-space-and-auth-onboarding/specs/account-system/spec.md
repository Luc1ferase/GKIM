## ADDED Requirements

### Requirement: Users MUST register or log in before accessing authenticated app features
The system SHALL require an unauthenticated user to register or log in before entering the main GKIM application shell, and it MUST restore previously authenticated users directly into the app until their session expires or they log out.

#### Scenario: First launch shows welcome auth entry
- **WHEN** the app starts and no active authenticated session is present
- **THEN** the system shows the welcome experience with explicit `注册` and `登录` actions instead of opening the main shell

#### Scenario: Returning user bypasses welcome gate
- **WHEN** the app starts and a valid authenticated session is already stored
- **THEN** the system skips the welcome gate and opens the authenticated shell directly

### Requirement: Every account MUST have a unique persistent account ID
The system SHALL create a unique persistent account ID for each registered user, and it MUST use that account ID as the canonical identifier for login and user-to-user adding instead of relying on display names.

#### Scenario: Registration creates a unique account ID
- **WHEN** a new user completes registration with an unused account ID
- **THEN** the system persists the account, rejects future duplicates of that ID, and returns the created identity for authenticated use

#### Scenario: Duplicate account ID is rejected
- **WHEN** a user attempts to register with an account ID that already exists
- **THEN** the system rejects the registration and reports that the requested account ID is unavailable

### Requirement: Authenticated users MUST be able to add contacts by account ID
The system SHALL let an authenticated user add another user by exact account ID, and it MUST create a contact relationship that can be used by messaging and discovery surfaces.

#### Scenario: Valid account ID adds a contact
- **WHEN** an authenticated user enters another existing user's account ID
- **THEN** the system creates or reveals the contact relationship so the added user becomes reachable in the app

#### Scenario: Invalid or duplicate add is handled safely
- **WHEN** an authenticated user enters a nonexistent account ID, their own account ID, or an account they already added
- **THEN** the system rejects the action with an explicit validation result and does not create a duplicate contact relationship
