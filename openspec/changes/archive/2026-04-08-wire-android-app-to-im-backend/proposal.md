## Why

The Rust IM backend is already alive with working session, bootstrap, history, and WebSocket contracts, but the Android app still renders messaging from seed data and placeholder endpoints. We need a focused change now to wire the app onto the live IM backend so the product can move from local demo behavior to real backend-driven state before we spend time on full physical-device validation.

## What Changes

- Replace the Android app's seed-only IM wiring with backend-driven HTTP and WebSocket integration for the existing Rust IM service.
- Add a dedicated runtime configuration path for IM HTTP base URL, WebSocket URL, and development user identity so the app can switch to live backend endpoints without a rebuild.
- Introduce a live messaging repository flow that authenticates, bootstraps conversations, loads history, and reconciles realtime gateway events into the existing Messages and Chat UI models.
- Surface integration state and failures clearly enough that follow-up device-validation work can test real backend flows instead of placeholder behavior.
- Keep this change focused on app-to-backend wiring only; full adb/SSH real-device validation remains a later step.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: refine the Android messaging contract so Messages and Chat can hydrate from the live IM backend over configurable HTTP and WebSocket endpoints instead of staying on in-memory seed data.

## Impact

- Affected code: Android messaging, settings, runtime configuration, and realtime layers under `android/app/src/main/java/com/gkim/im/android/data`, plus any related ViewModels and UI state surfaces under `android/app/src/main/java/com/gkim/im/android/feature`.
- Affected specs: `openspec/specs/core/im-app/spec.md` via a delta that formalizes live IM backend hydration and realtime reconciliation behavior.
- Affected systems: the Android client, the deployed Rust IM backend, and test harnesses that need to verify backend DTO mapping, HTTP contracts, gateway-event parsing, and repository state reconciliation.
- Affected dependencies: Retrofit/OkHttp usage for IM HTTP APIs, WebSocket event parsing and auth header handling, and Android preference storage for IM runtime config.
