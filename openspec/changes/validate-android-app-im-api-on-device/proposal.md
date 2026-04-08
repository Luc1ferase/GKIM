## Why

The Rust IM backend now exposes working health, session, bootstrap, history, and WebSocket flows, but the Android app still points at placeholder endpoints and in-memory messaging state. We need a focused change now to prove the API suite in real app usage on a physical device, so integration gaps show up in the product surface instead of only in backend-only smoke scripts.

## What Changes

- Replace the Android IM placeholder wiring with backend-driven HTTP and WebSocket integrations suitable for physical-device debugging against the deployed IM service.
- Add a device-safe runtime configuration path for backend base URLs, WebSocket endpoints, and development user selection without embedding database or server secrets in the APK.
- Add app-facing validation flows and evidence capture for session issuance, bootstrap, history loading, message send, realtime receive, delivery/read updates, reconnect behavior, and failure diagnostics on a real device.
- Add repeatable operator guidance for running the Android app against the deployed backend and recording acceptance evidence under the repository delivery workflow.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: refine the Android service-boundary and messaging requirements so the app can exercise the live IM backend over configurable HTTP and WebSocket endpoints on a physical device instead of relying on placeholder URLs and in-memory-only chat behavior.

## Impact

- Affected code: Android networking, repository, and chat-state layers under `android/app/src/main/java/com/gkim/im/android/data`, plus any settings/debug surfaces needed to select or store backend endpoints and dev identities.
- Affected specs: `openspec/specs/core/im-app/spec.md` via a delta that adds real-backend device validation expectations for IM flows.
- Affected systems: the deployed Rust backend on `124.222.15.128`, Android physical-device connectivity, and the verification workflow captured in `docs/DELIVERY_WORKFLOW.md`.
- Affected dependencies: Retrofit/OkHttp/WebSocket client wiring, Android preference storage for backend profile inputs, and any adb/logcat-based real-device validation steps used to capture acceptance evidence.
