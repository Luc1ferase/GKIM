## ADDED Requirements

### Requirement: Backend authenticates users with username and password
The system SHALL provide a `POST /api/auth/login` endpoint that accepts `username` and `password`, validates the credentials against the stored Argon2id hash, and returns a session token with user profile on success. The endpoint MUST return HTTP 401 for invalid credentials without revealing whether the username exists.

#### Scenario: User logs in with correct credentials
- **WHEN** a client sends `POST /api/auth/login` with a valid username and matching password
- **THEN** the backend returns a session token and user profile

#### Scenario: Login fails with wrong password
- **WHEN** a client sends `POST /api/auth/login` with a valid username but incorrect password
- **THEN** the backend returns HTTP 401 with a generic "invalid credentials" message

#### Scenario: Login fails with nonexistent username
- **WHEN** a client sends `POST /api/auth/login` with a username that does not exist
- **THEN** the backend returns HTTP 401 with the same generic "invalid credentials" message (no user enumeration)

### Requirement: Android provides a credential login screen
The system SHALL provide a login screen accessible from the Welcome page that collects username and password, calls the backend login endpoint, persists the returned session token in encrypted storage, and navigates to the authenticated shell on success.

#### Scenario: User logs in on Android
- **WHEN** the user enters valid credentials on the login screen and taps login
- **THEN** the app calls the login endpoint, stores the session token in EncryptedSharedPreferences, and navigates to the main shell

#### Scenario: Login screen shows error for invalid credentials
- **WHEN** the backend returns HTTP 401 during login
- **THEN** the login screen displays an "invalid credentials" error message

### Requirement: Android persists session token for auto-login
The system SHALL store the session token in EncryptedSharedPreferences after successful login or registration. On app launch, the system MUST check for a stored token, attempt bootstrap with it, and navigate directly to the authenticated shell if the token is still valid. If the token is expired or invalid, the system MUST clear the stored token and show the Welcome screen.

#### Scenario: App auto-logs in with stored token
- **WHEN** the app launches with a valid stored session token
- **THEN** the app performs bootstrap and navigates to the authenticated shell without showing the Welcome screen

#### Scenario: App clears expired token on launch
- **WHEN** the app launches with a stored session token that is expired or revoked
- **THEN** the app clears the stored token and navigates to the Welcome screen
