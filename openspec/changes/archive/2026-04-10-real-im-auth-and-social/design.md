## Context

GKIM has a Rust/Axum backend with PostgreSQL and an Android Kotlin/Compose client. The backend currently uses a dev-session flow (`POST /api/session/dev`) that looks up pre-seeded users by `externalId` and issues a bearer token. The Android Welcome screen bypasses real auth entirely — "login"/"register" just flip a local flag. Contacts are auto-created when a message is sent. There are no credentials, no registration, no friend requests.

**Current schema tables**: `users` (id, external_id, display_name, title, avatar_text), `contacts`, `direct_conversations`, `conversation_members`, `messages`, `message_receipts`, `session_tokens`.

**Target host**: Ubuntu at `124.222.15.128` with PostgreSQL.

## Goals / Non-Goals

**Goals:**
- Real user registration with username/password credentials
- Credential-based login that returns session tokens
- Session token persistence on Android for auto-login
- User discovery via search by username/display name
- Friend request lifecycle (send → accept/reject → mutual contacts)
- Contact-gated messaging (only mutual contacts can message)
- Preserve dev-session endpoint for testing/validation

**Non-Goals:**
- OAuth/SSO/third-party login providers (future work)
- Email/phone verification (future work)
- Group chat (out of scope)
- End-to-end encryption (future work)
- Password reset/recovery flow (future work)
- Rate limiting on auth endpoints (future work, noted as risk)

## Decisions

### D1: Password hashing with argon2

**Choice**: Use the `argon2` crate (Argon2id variant) for password hashing.

**Rationale**: Argon2id is the OWASP-recommended password hashing algorithm, resistant to both side-channel and GPU attacks. The `argon2` crate is pure Rust with no C dependencies, simplifying cross-compilation.

**Alternative considered**: `bcrypt` — mature but limited to 72-byte passwords and less resistant to GPU attacks than Argon2id.

### D2: Username as the primary credential identifier

**Choice**: Add a `username` column (unique, case-insensitive via `citext` or lower-index) to the `users` table. Login uses username + password.

**Rationale**: Simplest credential scheme that doesn't require email/phone infrastructure. `external_id` continues to exist for backward compatibility with dev-session flow.

**Alternative considered**: Email-based login — requires email verification infrastructure that is out of scope for this change.

### D3: Friend request table with status enum

**Choice**: New `friend_requests` table with columns: `id`, `from_user_id`, `to_user_id`, `status` (pending/accepted/rejected), `created_at`, `responded_at`. On acceptance, a reciprocal contact pair is created. On rejection, the row stays for dedup/history.

**Rationale**: Explicit request lifecycle gives both parties control and provides audit trail. The `contacts` table continues to represent mutual confirmed relationships.

### D4: Contact-gated messaging via check in persist_direct_message

**Choice**: Add a contact-existence check at the start of `persist_direct_message`. If either user is not in the other's contacts, return an error. The existing auto-contact-creation (`ensure_contact_pair_tx`) is removed from the messaging path.

**Rationale**: Friend acceptance is now the only way contacts are created, so messaging between strangers is blocked by design.

### D5: Android session persistence with EncryptedSharedPreferences

**Choice**: Store the session token and username in `EncryptedSharedPreferences`. On app start, check for a stored token, validate it against `GET /api/bootstrap` (which already requires auth), and navigate to the authenticated shell or welcome screen accordingly.

**Rationale**: AndroidX Security library provides AES-256 encrypted storage with minimal boilerplate. No need for a separate token-refresh endpoint — the existing 7-day expiry is sufficient for the first version.

### D6: New Android screens as NavHost composable routes

**Choice**: Add `register`, `login`, and `user-search` as new composable routes in the existing `NavHost`. Welcome screen's buttons navigate to `login`/`register` routes. User search is accessible from the Contacts tab.

**Rationale**: Follows the existing navigation pattern. No architectural change needed.

## Risks / Trade-offs

- **No rate limiting on auth endpoints** → Brute-force risk on login. Mitigation: Log failed attempts; add rate limiting as a follow-up. Argon2's computational cost provides some natural protection.
- **No password reset** → Locked-out users have no recovery path. Mitigation: Admin can reset via DB; add recovery flow as follow-up.
- **Schema migration on production** → Adding columns to `users` and a new table requires coordinated deploy. Mitigation: Use `ALTER TABLE ... ADD COLUMN` with defaults; additive migration only.
- **Dev-seeded users lack username/password** → Existing test users won't have credentials. Mitigation: Migration script backfills `username = external_id` and a known dev password for existing rows.
- **Contact gate breaks existing dev flow** → Dev-session users who relied on auto-contact-creation will need to use friend requests or have pre-seeded contacts. Mitigation: Seed migration creates reciprocal contacts for existing dev users.
