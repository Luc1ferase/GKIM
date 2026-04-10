## Why

GKIM currently operates on pre-seeded development users with a bypass auth flow — the Welcome screen's "login" and "register" buttons simply flip a local flag, and the backend only issues sessions for known `externalId` values. There is no way for a new person to create an account, authenticate with credentials, search for other users, send a friend request, or start a real conversation from scratch. This change builds the end-to-end path that turns GKIM into a functional multi-user IM: real registration, credential-based login, friend discovery and requests, and contact-gated messaging.

## What Changes

- **User registration (backend)**: New `POST /api/auth/register` endpoint that accepts username, password, and display name, creates a `users` row with a bcrypt-hashed password, and returns a session token. **BREAKING**: The `users` table gains `username` and `password_hash` columns.
- **Credential login (backend)**: New `POST /api/auth/login` endpoint that validates username + password and returns a session token, replacing the dev-session flow for production use.
- **User search (backend)**: New `GET /api/users/search?q=<query>` endpoint that lets authenticated users search for other users by username or display name.
- **Friend request system (backend)**: New endpoints for sending (`POST /api/friends/request`), listing pending (`GET /api/friends/requests`), and accepting/rejecting (`POST /api/friends/requests/:id/accept`, `POST /api/friends/requests/:id/reject`) friend requests. A new `friend_requests` table tracks pending/accepted/rejected state. Contacts are created only when a friend request is accepted — messaging requires mutual contact status.
- **Contact-gated messaging (backend)**: `persist_direct_message` enforces that sender and recipient are mutual contacts before allowing message send.
- **Registration screen (Android)**: New screen with username, password, display-name inputs that calls the register endpoint and navigates to the authenticated shell on success.
- **Login screen (Android)**: New screen with username + password inputs that calls the login endpoint, stores the session token, and navigates to the authenticated shell.
- **User search screen (Android)**: New screen accessible from Contacts that lets users search and send friend requests.
- **Friend request UI (Android)**: Pending request list in Contacts with accept/reject actions; badge for pending requests.
- **Session persistence (Android)**: Token stored in encrypted SharedPreferences; auto-login on app restart if token is valid.

## Capabilities

### New Capabilities
- `user-registration`: Backend registration endpoint, password hashing, username uniqueness, Android registration screen
- `credential-login`: Backend login endpoint, Android login screen, session token persistence, auto-login
- `user-search`: Backend user search endpoint, Android search UI for discovering other users
- `friend-requests`: Backend friend request lifecycle (send/list/accept/reject), Android friend request UI, contact creation on acceptance

### Modified Capabilities
- `im-backend`: Contact creation gated by friend acceptance instead of auto-created on first message; `users` table schema change for username/password_hash
- `core/im-app`: Welcome screen wires to real login/register screens instead of directly flipping auth state; Contacts tab gains search and friend-request entry points

## Impact

- **Backend database**: Schema migration adding `username`, `password_hash` to `users`; new `friend_requests` table. Existing dev-seeded users need username backfill.
- **Backend dependencies**: New crate for bcrypt password hashing (e.g., `bcrypt` or `argon2`).
- **Android networking**: New Retrofit endpoints for auth, search, and friend-request APIs.
- **Android local storage**: Encrypted SharedPreferences for session token persistence.
- **Android navigation**: New composable routes for register, login, and user-search screens.
- **Breaking change**: Existing pre-seeded users without usernames cannot log in until backfilled. Dev-session endpoint remains available for testing but is not the production path.
