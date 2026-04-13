## 1. Regression guards

- [x] 1.1 Add or refresh Android instrumentation coverage that reproduces the missing Space settings entry and the inert login/register back affordances before the UI fixes land.
- [x] 1.2 Add focused welcome-video and endpoint-resolution diagnostics or tests so playback regressions and auth-target selection regressions can be detected without relying on manual guesswork alone.

## 2. Shell and onboarding recovery

- [x] 2.1 Restore the Space header settings pill so the shell once again exposes a visible, tappable route into `settings` from the Space page.
- [x] 2.2 Wire the login and registration back affordances to the existing root-auth callbacks and verify that both flows return to the welcome surface.
- [x] 2.3 Recover the welcome-screen video playback presentation so the approved `welcome_intro_1` asset remains visibly active after the current cover-scaling and scrim treatment.

## 3. Auth endpoint unification

- [x] 3.1 Introduce a shared HTTP base URL resolver that prefers authenticated session state but otherwise falls back to the persisted IM Validation HTTP endpoint, then update login/register/bootstrap/contacts/search callers to use it instead of hardcoded `127.0.0.1:18080` defaults.
- [x] 3.2 Improve auth/connectivity diagnostics so failed login or registration attempts can distinguish “wrong client URL selected” from “backend unreachable at the selected endpoint.”

## 4. Emulator-to-backend validation

- [x] 4.1 Validate that the published auth API target chosen for emulator testing is actually host-reachable, and update backend-side bind/guidance or validation notes if the current target still relies on device-local localhost assumptions.
- [x] 4.2 Re-run focused Android and backend verification for the restored settings path, welcome playback, auth back navigation, and configured API connectivity, then capture verification/review/score/evidence updates in `docs/DELIVERY_WORKFLOW.md`.

## Task Evidence

- Record verification, review, scoring, upload, and acceptance evidence for each completed task using the `docs/DELIVERY_WORKFLOW.md` format before checking the next task off.
