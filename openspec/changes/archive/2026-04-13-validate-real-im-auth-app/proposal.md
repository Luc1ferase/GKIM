## Why

GKIM now has a substantial uncommitted backend + Android auth/social slice that appears to turn the app into a genuinely usable IM product with registration, credential login, persisted sessions, user discovery, and friend-request-gated messaging. Before that work can be treated as accepted product behavior, we need to read the actual implementation, run representative verification, and reconcile the main OpenSpec contract with what the code truly does today.

## What Changes

- Audit the current auth/social implementation across Rust backend and Android client instead of relying only on the older proposal text from `real-im-auth-and-social`.
- Run representative backend and Android verification commands for the newly added register/login/social flow and record concrete pass/fail evidence.
- Sync the repository's main requirements so `im-backend` and `core/im-app` describe the auth-enabled IM behavior that is now present in code, not just the earlier dev-session-only flow.
- Identify acceptance blockers revealed by testing, especially stale test doubles, missing automated coverage for real login/register flows, and any backend realtime regressions uncovered by representative integration tests.
- Turn the verification findings into implementation-ready tasks so the remaining work can be finished deliberately instead of being hand-waved as “already done.”

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `im-backend`: Main backend requirements need to reflect production registration/login endpoints, user search, friend requests, and contact-gated direct messaging that now exist in the backend code and validation scope.
- `core/im-app`: Main Android requirements need to reflect welcome-driven login/register navigation, encrypted session persistence, and account-entry surfaces that now exist in the app code and must be validated against the real backend-auth product story.

## Impact

- Affected backend code: `backend/src/app.rs`, `backend/src/auth.rs`, `backend/src/social.rs`, `backend/src/im/repository.rs`, and `backend/migrations/202604100001_auth_and_friend_requests.sql`.
- Affected Android code: `android/app/src/main/java/com/gkim/im/android/feature/auth/`, `android/app/src/main/java/com/gkim/im/android/data/local/SessionStore.kt`, `android/app/src/main/java/com/gkim/im/android/data/remote/im/`, `android/app/src/main/java/com/gkim/im/android/feature/navigation/GkimRootApp.kt`, and related Contacts/social surfaces.
- Affected verification assets: backend integration tests in `backend/tests/` plus Android unit/instrumentation tests under `android/app/src/test/` and `android/app/src/androidTest/`.
- Affected workflow outcome: this change determines whether the auth-enabled IM slice is truly acceptance-ready, or whether it still has verification gaps and regressions that must be fixed before archive/sync.
