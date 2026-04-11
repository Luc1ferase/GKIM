## Why

The Android shell currently presents several user-visible regressions at the exact point where the app is being demoed and validated: the Space-page settings entry is missing, the welcome animation no longer reads as reliably playing, the login/register back affordances do not work, and auth requests appear to fall back to `127.0.0.1:18080` instead of a truly reachable backend endpoint. We need one focused change that diagnoses these regressions end-to-end and restores a trustworthy onboarding-to-auth flow before more acceptance or archive work continues.

## What Changes

- Restore the Space-page settings entry so the only intended settings access path in the primary shell is visibly available again.
- Diagnose and harden the welcome-screen video playback path so the approved runtime animation remains visibly playing after the recent cover-scaling and overlay adjustments.
- Fix the login and registration routes so their rendered back affordances actually navigate back to the welcome surface instead of appearing interactive while doing nothing.
- Trace Android auth endpoint selection from settings/preferences/session state through the Retrofit client and confirm whether registration/login calls are reaching the intended backend service or incorrectly defaulting to emulator-local `127.0.0.1:18080`.
- Add focused verification coverage and evidence for the recovered settings entry, welcome media playback contract, auth-route back navigation, and emulator-to-backend auth connectivity diagnostics.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: Tighten the shell/onboarding/auth requirements so Space keeps its settings entry point, welcome video playback remains visibly active, auth routes expose working back navigation, and login/register use a reachable operator-configured backend endpoint instead of an opaque localhost fallback.
- `im-backend`: Clarify the backend service-boundary requirement for emulator validation so auth endpoints are validated through a host-reachable API target rather than assuming device-local `127.0.0.1` is the correct runtime path.

## Impact

- Affected Android shell code: `android/app/src/main/java/com/gkim/im/android/feature/space/SpaceRoute.kt`, `android/app/src/main/java/com/gkim/im/android/feature/navigation/GkimRootApp.kt`, and `android/app/src/main/java/com/gkim/im/android/core/designsystem/AetherComponents.kt`.
- Affected welcome/auth code: `android/app/src/main/java/com/gkim/im/android/feature/navigation/WelcomeRoute.kt`, `android/app/src/main/java/com/gkim/im/android/feature/auth/LoginRoute.kt`, and `android/app/src/main/java/com/gkim/im/android/feature/auth/RegisterRoute.kt`.
- Affected connectivity/configuration code: `android/app/src/main/java/com/gkim/im/android/data/local/SessionStore.kt`, `android/app/src/main/java/com/gkim/im/android/data/remote/im/ImBackendHttpClient.kt`, related settings/preferences storage, and any backend bind/deployment guidance that controls the emulator-facing auth URL.
- Affected verification: Android instrumentation tests around welcome/auth/settings plus any backend or emulator smoke checks needed to prove the auth API target is real and reachable.
