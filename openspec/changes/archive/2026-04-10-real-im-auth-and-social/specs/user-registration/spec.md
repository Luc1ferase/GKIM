## ADDED Requirements

### Requirement: Backend registers new users with username and password
The system SHALL provide a `POST /api/auth/register` endpoint that accepts a JSON body with `username`, `password`, and `displayName`, creates a new user with an Argon2id-hashed password, and returns a session token. The username MUST be unique (case-insensitive) and between 3–20 alphanumeric/underscore characters. The password MUST be at least 8 characters. The `displayName` MUST be between 1–30 characters. The endpoint MUST return a 409 Conflict if the username is already taken.

#### Scenario: New user registers successfully
- **WHEN** a client sends `POST /api/auth/register` with a valid unique username, password (>= 8 chars), and displayName
- **THEN** the backend creates a `users` row with the hashed password, issues a session token, and returns the token plus user profile in the response

#### Scenario: Registration fails for duplicate username
- **WHEN** a client sends `POST /api/auth/register` with a username that already exists (case-insensitive)
- **THEN** the backend returns HTTP 409 with an error indicating the username is taken

#### Scenario: Registration fails for invalid input
- **WHEN** a client sends `POST /api/auth/register` with a username shorter than 3 characters, a password shorter than 8 characters, or an empty displayName
- **THEN** the backend returns HTTP 400 with a validation error describing the constraint violation

### Requirement: Android provides a registration screen
The system SHALL provide a registration screen accessible from the Welcome page that collects username, password, and display name, calls the backend register endpoint, persists the returned session token, and navigates to the authenticated shell on success. The screen MUST display backend validation errors (duplicate username, invalid input) inline.

#### Scenario: User completes registration on Android
- **WHEN** the user fills in valid username, password, and display name on the registration screen and taps register
- **THEN** the app calls the register endpoint, stores the session token, and navigates to the main shell

#### Scenario: Registration screen shows validation error
- **WHEN** the backend returns a 409 or 400 error during registration
- **THEN** the registration screen displays the error message near the relevant input field
