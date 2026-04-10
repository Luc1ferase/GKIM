## Context

The repository already contains a large uncommitted auth/social implementation across the Rust backend and Android app:

- Backend routes now include `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/users/search`, and friend-request lifecycle endpoints in addition to the existing dev-session, bootstrap, history, and WebSocket flows.
- Backend data/model work now includes `backend/src/auth.rs`, `backend/src/social.rs`, and `backend/migrations/202604100001_auth_and_friend_requests.sql`, which add credential fields plus friend-request state.
- Android now contains `LoginRoute`, `RegisterRoute`, an encrypted `SessionStore`, and root auth gating in `GkimRootApp`, so the welcome flow no longer appears to be purely a local preview seam.

Representative verification on April 10, 2026 shows a mixed readiness story:

- `cargo test --test migrations_schema` passed.
- `cargo test --test http_im_api --test ws_gateway -- --nocapture` partially passed:
  - HTTP bootstrap/history integration tests passed.
  - `authenticated_websocket_registers_user_and_replies_to_ping` passed.
  - `online_send_flow_persists_message_and_emits_delivery_and_read_events` failed with a WebSocket event timeout.
  - `offline_recipient_recovers_unread_state_from_bootstrap_and_history` failed with a WebSocket event timeout.
- `./gradlew.bat :app:compileDebugKotlin` passed, which means Android production code currently compiles.
- Android automated test targets are stale:
  - `:app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendHttpClientTest` failed during test compilation because existing unit-test doubles no longer implement the expanded auth/social interfaces.
  - `:app:connectedDebugAndroidTest` failed during instrumentation test compilation because `UiTestAppContainer` no longer satisfies the `AppContainer` contract.

This change is therefore not a greenfield feature design. It is a validation-and-reconciliation pass over code that already exists but is not yet acceptance-clean.

## Goals / Non-Goals

**Goals:**
- Treat the current backend and Android implementation as the source of truth for this validation slice.
- Sync the main OpenSpec contract so it describes the auth-enabled IM behavior that is actually in the codebase.
- Separate three states clearly: implemented behavior, verified behavior, and currently blocked verification.
- Turn the failing or stale verification surfaces into explicit follow-up tasks instead of implicit debt.

**Non-Goals:**
- Designing a brand-new auth architecture or changing the user-facing auth feature set again.
- Implementing OAuth, password reset, rate limiting, or any broader account platform work.
- Archiving unrelated completed changes or cleaning unrelated worktree noise as part of this slice.
- Treating manual confidence or partial emulator runs as a substitute for repaired automated acceptance coverage.

## Decisions

### 1. Use current code plus fresh test evidence as the acceptance baseline
This change should validate what is already implemented today, not merely restate the original intent from `real-im-auth-and-social`.

Why:
- The repository now contains concrete backend routes, migrations, Android screens, and session persistence code.
- Fresh evidence already shows both successful and failing verification surfaces, which is more trustworthy than the earlier proposal alone.

Alternatives considered:
- Treat the older `real-im-auth-and-social` change as sufficient proof of delivery. Rejected because the main specs are still unsynced and the current automated evidence is mixed.

### 2. Fold auth/social behavior into the existing `im-backend` and `core/im-app` capabilities
The validation change should update the two existing main capability trees rather than introduce a second permanent standalone capability for login/registration.

Why:
- Mainline OpenSpec currently exposes `im-backend` and `core/im-app` as the durable capability anchors.
- The implemented auth/social behavior changes the meaning of both capabilities in a product-visible way.

Alternatives considered:
- Keep auth requirements only in a separate archived change. Rejected because the main specs would continue to misdescribe the shipped product behavior.

### 3. Treat broken tests and failing integration runs as first-class blockers
The validation design should explicitly preserve red signals:
- backend realtime integration timeouts
- Android unit/instrumentation test compilation drift

Why:
- A green compile of production Android code is not the same thing as verified auth acceptance.
- The failing backend realtime tests may indicate a true regression in the friend-gated messaging path, not just flaky infrastructure.

Alternatives considered:
- Mark the slice “done enough” because login/register code exists and the app compiles. Rejected because that would hide known acceptance blockers.

### 4. Prioritize verification-surface repair before new auth/social polish
The next implementation work should repair test doubles, missing auth-route coverage, and backend realtime validation before any additional auth-adjacent feature expansion.

Why:
- The current problem is confidence and acceptance, not lack of surface area.
- Adding more product behavior before the test harness catches up would compound uncertainty.

Alternatives considered:
- Keep building on top of the auth/social slice first and fix tests later. Rejected because the stale test harness is already masking whether the current implementation is stable.

## Risks / Trade-offs

- [Risk] The backend WebSocket timeout failures may represent a real product regression in contact-gated messaging, not just test flakiness.  
  Mitigation: make the failing ws-gateway cases a named blocker and require root-cause validation before acceptance.

- [Risk] Android production code compiling can create false confidence while auth-specific unit/instrumentation suites remain broken.  
  Mitigation: require repaired Android tests that exercise login/register/session persistence before calling the slice verified.

- [Risk] Syncing main specs too aggressively could bless behavior that is still under-verified.  
  Mitigation: phrase tasks so spec sync and acceptance evidence move together, and record current red signals in the change.

- [Risk] The existing complete-but-unarchived `real-im-auth-and-social` change may drift from this validation change.  
  Mitigation: use it as historical input only, and treat this change as the acceptance and reconciliation layer for what is in the working tree now.

## Migration Plan

1. Repair or replace stale backend/Android automated verification surfaces so the current auth-enabled slice can be tested without compile drift.
2. Re-run representative backend auth/social and Android auth-entry verification until the results accurately distinguish product bugs from test-harness issues.
3. Sync validated requirements into the main `im-backend` and `core/im-app` specs.
4. Once verification is green and evidence is recorded, decide whether `real-im-auth-and-social` should be archived as superseded by this acceptance slice or merged through normal OpenSpec sync/archive flow.

Rollback strategy:
- If verification disproves the current auth-enabled behavior, keep the spec sync contained to this change and do not archive/sync it until the failing flows are repaired.

## Open Questions

- Are the two failing WebSocket integration tests timing out because the new contact/friend-request gate changed the expected setup, or because realtime delivery is currently regressed?
- Should Android acceptance for this slice require true UI automation of register/login submission against a live or mocked backend, or is repository-level compile plus backend API verification sufficient for the first acceptance pass?
- Should the completed `real-im-auth-and-social` change be revived and reconciled directly, or should this validation change remain the canonical place where the auth-enabled IM slice is accepted?
