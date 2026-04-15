## Why

The Android app currently expects IM HTTP and WebSocket endpoints to be entered manually in Settings, which is useful for emulator validation but not appropriate for normal users. We need a safer runtime model where the app uses one trusted backend origin by default, derives the matching realtime endpoint automatically, and keeps manual endpoint overrides out of the regular user path.

## What Changes

- Replace the regular user-facing IM endpoint entry flow with an app-managed single backend origin for IM service access.
- Derive the WebSocket endpoint from the same backend origin instead of requiring a second user-managed URL.
- Restrict manual IM endpoint overrides to a guarded developer or validation path rather than the normal Settings experience.
- Tighten endpoint safety rules so release users cannot silently drift onto arbitrary loopback, plaintext, or mismatched origins without explicit developer intent.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: the Android service-boundary requirement will change from user-managed HTTP and WebSocket endpoint entry toward a single trusted backend origin with derived realtime connectivity and restricted override behavior.

## Impact

- Affected code: Android preference storage, Settings IM validation surfaces, endpoint resolution utilities, auth/bootstrap/realtime client wiring, and related Android tests.
- Affected specs: `openspec/specs/core/im-app/spec.md`.
- Affected systems: Android runtime endpoint selection, IM authentication/bootstrap/realtime flows, and developer validation workflow.
- Affected dependencies: any shipped backend-origin configuration, debug-only override path, and operator guidance for safe production versus validation connectivity.
