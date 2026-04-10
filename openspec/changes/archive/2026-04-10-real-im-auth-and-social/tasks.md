## 1. Database schema migration

- [x] 1.1 Add `username` (unique, case-insensitive) and `password_hash` columns to `users` table; backfill existing dev users with `username = external_id` and a known dev-password hash
- [x] 1.2 Create `friend_requests` table with `id`, `from_user_id`, `to_user_id`, `status` (pending/accepted/rejected), `created_at`, `responded_at`
- [x] 1.3 Add `argon2` crate dependency to `Cargo.toml`

## 2. Backend registration and login

- [x] 2.1 Implement `POST /api/auth/register` endpoint: validate input (username 3–20 alphanum/underscore, password >= 8 chars, displayName 1–30 chars), hash password with Argon2id, insert user, issue session token, return token + profile
- [x] 2.2 Implement `POST /api/auth/login` endpoint: look up user by username, verify password against stored hash, issue session token, return token + profile; return 401 for invalid credentials without user enumeration

## 3. Backend user search

- [x] 3.1 Implement `GET /api/users/search?q=<query>` endpoint: search users by username/display_name substring (case-insensitive), exclude requesting user, limit 20, include `contactStatus` field (contact/pending_sent/pending_received/none)

## 4. Backend friend request lifecycle

- [x] 4.1 Implement `POST /api/friends/request` endpoint: create pending friend request, reject if duplicate or already contacts
- [x] 4.2 Implement `GET /api/friends/requests` endpoint: list pending incoming friend requests with sender profile
- [x] 4.3 Implement `POST /api/friends/requests/:id/accept` endpoint: update status, create reciprocal contacts
- [x] 4.4 Implement `POST /api/friends/requests/:id/reject` endpoint: update status to rejected

## 5. Backend contact-gated messaging

- [x] 5.1 Add mutual-contact check to `persist_direct_message`; remove `ensure_contact_pair_tx` auto-creation from the messaging path; return error if users are not mutual contacts

## 6. Backend WebSocket friend-request events

- [x] 6.1 Push `friend_request_received`, `friend_request_accepted`, and `friend_request_rejected` events to online users via existing ConnectionHub

## 7. Android auth networking

- [x] 7.1 Add Retrofit endpoints for `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/users/search`, and friend-request CRUD endpoints
- [x] 7.2 Implement `SessionStore` using EncryptedSharedPreferences to persist/clear session token and username

## 8. Android registration screen

- [x] 8.1 Create `RegisterRoute` composable with username, password, display-name inputs and register button; call register endpoint; show inline validation errors; navigate to authenticated shell on success

## 9. Android login screen

- [x] 9.1 Create `LoginRoute` composable with username and password inputs; call login endpoint; show error on 401; persist token and navigate to authenticated shell on success

## 10. Android auto-login on app launch

- [x] 10.1 Update `GkimRootApp` to check `SessionStore` on launch: if token exists, attempt bootstrap; on success navigate to authenticated shell, on failure clear token and show Welcome screen
- [x] 10.2 Wire Welcome screen buttons to navigate to `login` and `register` routes instead of directly flipping auth state

## 11. Android user search screen

- [x] 11.1 Create `UserSearchRoute` composable with search input, result list showing display name/username/status, and "Add friend" action button; wire navigation from Contacts tab

## 12. Android friend request UI

- [x] 12.1 Add pending friend request section to Contacts screen: show incoming requests with accept/reject actions above the contact list
- [x] 12.2 Add pending-request count badge to the Contacts tab in bottom navigation
- [x] 12.3 Handle WebSocket friend-request events to update friend request and contact lists in real-time

## 13. Deploy and validate

- [x] 13.1 Run schema migration on production PostgreSQL at `124.222.15.128`
- [x] 13.2 Smoke-test full flow: register two users, search, send friend request, accept, exchange messages
