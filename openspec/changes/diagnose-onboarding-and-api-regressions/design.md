## Context

This change crosses multiple Android surfaces and one backend service-boundary concern. The current regressions are not isolated visual nits: the missing Space-page settings pill removes the only intended app-level settings entry, the welcome video path has become harder to trust after recent cover-scaling and overlay changes, the login/register back affordances are rendered but not wired, and the auth flow still scatters `http://127.0.0.1:18080/` fallbacks across screens even though emulator validation is supposed to use operator-managed endpoint inputs.

The current implementation already shows the main fault lines:
- `SpaceRoute` renders `PageHeader(title = ...)` without the settings action callback even though `settings` still exists as a secondary route.
- `WelcomeRoute` still binds `R.raw.welcome_intro_1`, but the playback contract is implicit inside a `VideoView` wrapper and can regress silently when overlay or lifecycle behavior changes.
- `LoginRoute` and `RegisterRoute` accept `onBack`, but their visible back labels are plain `Text` nodes without click handling.
- Auth, contacts, user search, and startup bootstrap each fall back directly to `127.0.0.1:18080`, while IM Validation settings persist a potentially different HTTP base URL in `PreferencesStore`.

## Goals / Non-Goals

**Goals:**
- Restore the Space header so settings navigation is visibly present again from the only intended shell entry point.
- Re-establish a testable welcome-video playback contract that keeps the approved asset, looping, mute, and cover behavior while making playback regressions easier to diagnose.
- Make login/register back affordances genuinely navigable and consistent with the route callbacks already provided by the root auth graph.
- Replace scattered localhost fallbacks with a single emulator-aware auth/backend endpoint resolution path shared by login, registration, bootstrap, contacts, and user search.
- Add diagnostics and verification steps that can distinguish “server unreachable from emulator” from “wrong URL selected in client state.”

**Non-Goals:**
- Redesigning the visual language of the welcome screen, settings surface, or auth forms beyond the minimal interaction fixes needed for these regressions.
- Replacing the approved welcome runtime asset with a different video source.
- Redefining backend auth semantics, database schema, or friend-request business rules beyond what is required to validate emulator-facing endpoint reachability.
- Introducing production cloud discovery, service registry logic, or automatic environment switching beyond the existing operator-managed IM Validation inputs.

## Decisions

### 1. Restore settings navigation by reusing the existing Space header contract
The Space tab should regain its settings button by wiring `PageHeader` action props directly inside `SpaceRoute` and navigating to the already-registered `settings` route. This keeps the existing information architecture intact instead of adding a second settings entry somewhere else.

Why this decision:
- The spec already makes Space the sole app-level settings entry point.
- The route already exists, so the regression is a missing header action, not a missing feature.
- Restoring the header action is lower-risk than introducing new buttons deeper in the feed or bottom navigation.

Alternatives considered:
- Add settings access back to Messages or the bottom nav: rejected because it reopens the shell IA decisions already captured in the main spec.
- Hide settings until a later auth slice: rejected because the user currently needs the IM Validation surface to troubleshoot connectivity.

### 2. Treat welcome-video playback as an explicit runtime contract, not just a resource binding
The welcome screen should keep the current `welcome_intro_1` raw asset and cover-scaling container, but the implementation work should explicitly verify three things together: the asset is still the bound source, playback starts/continues after view updates, and the overlay/cover treatment still leaves visible motion for the user. Verification should combine targeted playback tests with a focused manual/emulator evidence step instead of assuming resource presence alone proves visible playback.

Why this decision:
- The recent regression did not come from a missing resource; it came from the user no longer trusting what they saw on screen.
- The `VideoView` path can still work for this slice if we strengthen the surrounding assertions instead of immediately swapping media stacks again.
- This keeps the change scoped to regression recovery rather than a full media-player rewrite.

Alternatives considered:
- Replace `VideoView` with a new player dependency immediately: rejected because the current issue is first a trust/verification gap, not yet proof that the playback primitive must change.
- Only loosen the overlay and stop there: rejected because that fixes one symptom but does not make future playback regressions diagnosable.

### 3. Make route callbacks the single source of truth for auth back navigation
Both auth screens should convert their rendered back labels into actual clickable controls that invoke the existing `onBack` callback. Tests should assert that tapping back returns to the welcome surface instead of leaving inert copy on screen.

Why this decision:
- The root auth graph already passes a valid back action.
- The current regression is caused by dropped interaction wiring, not by missing navigation structure.
- Reusing the callback preserves the current navigation stack and keeps the screens simple.

Alternatives considered:
- Let system back be the only way out: rejected because the UI visibly promises an in-surface back control.
- Navigate directly from each auth screen to a hardcoded route string: rejected because the callback already encapsulates the correct back-stack behavior.

### 4. Unify auth/backend endpoint resolution around persisted IM Validation configuration
The Android client should stop sprinkling `sessionStore.baseUrl ?: "http://127.0.0.1:18080/"` across screens. Instead, auth and other backend-bound flows should resolve their HTTP endpoint from one shared source that prefers an authenticated session URL when present but otherwise falls back to the persisted IM Validation HTTP base URL from `PreferencesStore`. If that resolved URL is still a loopback address, the app should surface it explicitly as an emulator-risk condition during verification and in error messaging where appropriate.

Why this decision:
- The current bug is a configuration split-brain: IM Validation stores one URL, while auth uses another fallback path.
- A shared resolver keeps login, register, bootstrap, contacts, and search aligned instead of fixing only one screen.
- This also makes emulator diagnostics more honest by exposing the actual chosen base URL.

Alternatives considered:
- Keep using `SessionStore.baseUrl` only: rejected because first-run auth happens before a session exists.
- Read `PreferencesStore` ad hoc in each screen: rejected because it duplicates the same bug-prone fallback logic across multiple features.
- Continue defaulting to `127.0.0.1` silently: rejected because emulator-local localhost is often the wrong target and obscures real connectivity failures.

### 5. Validate backend reachability at the service boundary, not through assumptions about localhost
The backend-facing spec delta should make emulator validation explicit: the Android app must target a host-reachable auth API URL, and acceptance needs a concrete smoke check that proves the selected URL is reachable from the emulator/runtime being used. This keeps the change honest even if the backend code itself does not need functional changes.

Why this decision:
- The user’s report is fundamentally about a broken contract between emulator and backend, not just UI copy.
- The main failure mode is “wrong endpoint assumption,” which is easiest to catch with boundary-level smoke verification.
- This lets the change recover confidence without inventing new backend product behavior.

Alternatives considered:
- Ignore backend spec changes and treat this as client-only: rejected because the acceptance question is whether the emulator is reaching a real backend service.
- Hardcode a specific LAN/IP target in the app: rejected because the project already intends operator-managed endpoint inputs.

## Risks / Trade-offs

- [Playback still appears static on some emulator GPUs] → Mitigation: keep the resource/callback contract under test and require one fresh emulator validation step that confirms visible motion after install/launch.
- [Shared endpoint resolution changes multiple flows at once] → Mitigation: cover login, registration, startup bootstrap, and at least one post-auth API surface with focused verification so the resolver does not fix one flow while breaking another.
- [Old saved session/base URL combinations may conflict with new resolver precedence] → Mitigation: document precedence clearly and, if needed, seed session URL from persisted IM Validation config only when the session does not yet carry a base URL.
- [Backend may already be correct while emulator targeting is wrong] → Mitigation: record the selected client URL and a separate reachability/smoke result so diagnosis distinguishes client selection bugs from server availability issues.

## Migration Plan

1. Restore the Space-page settings action and auth back-click wiring with targeted UI regression coverage.
2. Centralize backend HTTP endpoint resolution, update auth/social/startup callers to use it, and add diagnostics that expose the chosen URL during failure analysis.
3. Re-verify welcome playback behavior on the emulator after the latest overlay/cover changes, keeping the approved packaged asset intact.
4. Run emulator/backend smoke validation using the resolved URL, then capture the required verification, review, scoring, and evidence updates before any follow-on task proceeds.

## Open Questions

- Should the shared endpoint resolver also normalize Android-emulator host aliases such as `10.0.2.2`, or is this change limited to honoring whatever operator-managed URL is already stored?
- Do we want the auth screens to surface the resolved base URL directly in failure copy for debugging, or keep that detail only in test/debug evidence to avoid noisy product UI?
