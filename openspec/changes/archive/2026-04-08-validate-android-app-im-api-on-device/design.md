## Context

The repository now has two pieces that have not yet met in a real product loop: a Rust IM backend with verified session/bootstrap/history/WebSocket behavior, and an Android client whose IM surface still runs on `InMemoryMessagingRepository`, `https://api.example.com/`, and `wss://example.com/realtime`. The user clarified that first-pass validation should happen through the existing Android emulator, and that the backend can run locally in Docker as long as the same image can later be pulled and deployed on the server.

That changes the shortest safe validation path: instead of SSH tunneling into the remote Ubuntu host for the first acceptance loop, we can package the backend into a local Docker image, publish the container port on the developer workstation, and let the Android emulator hit the host through the standard emulator network bridge. The design should still keep database and server secrets out of the APK, preserve a clean path to future server image deployment, and uphold the repository rule that each implementation slice must be verified, reviewed, scored, evidenced, and pushed independently.

## Goals / Non-Goals

**Goals:**
- Let the Android app authenticate against the existing IM backend and render live conversation/bootstrap/history data instead of seed-only messaging state.
- Let the Android emulator exercise the backend WebSocket message flow, including send, receive, delivered, read, reconnect, and visible failure states.
- Add a repeatable runtime configuration and validation workflow so operators can point the app at a host-published or deployed IM backend without rebuilding the APK or embedding secrets.
- Add Docker packaging for the backend so the same image used in local validation can be pulled and deployed on the server later.
- Keep the work reviewable in small accepted slices with explicit device-validation evidence.

**Non-Goals:**
- Rebuilding the feed or AIGC stacks to use live backend APIs in this same change.
- Production auth, permanent public ingress hardening, or App Store release preparation.
- iOS, UniAPP, or cross-platform client validation in this change.
- Full local persistence/offline-first messaging redesign beyond what is needed to validate reconnect and recovery against backend bootstrap/history APIs.

## Decisions

### 1. Scope this change to the Android IM validation path, not the full app stack
The implementation will focus on the Android messaging surface, its settings/debug inputs, and the Rust IM backend contract. Feed and AIGC flows will remain on their current placeholder or local paths unless they are required incidentally for screen stability.

Why this decision:
- The request is about validating "this API suite" in the app, and the only concrete live API suite in the repository today is the IM backend.
- Trying to turn every app subsystem live in one change would make the change too large for the repository's per-task acceptance workflow.
- The IM path already has backend contract evidence, which makes it the highest-value place to close the product loop.

Alternatives considered:
- Testing only backend endpoints outside the app again: rejected because it would not prove real product usage on device.
- Expanding the change to feed and AIGC endpoints too: rejected because those backend contracts are not equally mature and would blur the goal.

### 2. Use a locally published Docker backend plus emulator host bridging for first-pass validation
The primary validation path will run the Rust IM backend in a local Docker container on the developer workstation, publish the service port to the host, and point the Android emulator at the host bridge endpoint (`10.0.2.2` by default). `adb reverse` can remain a fallback tool, but it is no longer the primary route for this change.

Why this decision:
- It removes the immediate dependency on the remote Ubuntu host for the core app/API validation loop.
- It aligns with the user's stated deployment strategy: build an image locally now, pull the same image on the server later.
- The Android emulator already has a standard path to host-published ports, which makes the workflow simpler and more reproducible than USB-only forwarding.

Alternatives considered:
- Keeping the original SSH tunnel to the deployed host as the primary path: rejected after the user clarified that local Docker is acceptable and preferred for iteration.
- Standing up a public reverse proxy/TLS gateway first: viable later, but rejected for the initial validation loop because it adds infrastructure scope before the app contract is proven.

### 3. Add dedicated IM backend validation settings instead of reusing AIGC endpoint fields
The app will gain IM-specific runtime settings for HTTP base URL, WebSocket URL or derivation rule, and a development user identifier/profile. These values will live in Android preference storage and will be surfaced in a device-debuggable settings flow that is separate from the existing AIGC provider configuration.

Why this decision:
- The AIGC custom endpoint fields already mean something else; overloading them would create confusing operator behavior and accidental misconfiguration.
- IM validation needs both HTTP and WebSocket inputs plus a selected user identity, which is a different shape from the AIGC provider model.
- Separate settings make it easier to keep the live backend test path explicit in screenshots, docs, and task evidence.

Alternatives considered:
- Hardcoding the deployed backend endpoint in `AppContainer`: rejected because it would require rebuilds and would be unsafe for future environments.
- Reusing the existing custom provider base URL: rejected because it conflates unrelated backend surfaces and cannot carry the needed IM identity settings cleanly.

### 4. Replace the seed-only messaging repository with a backend-backed state holder
`MessagingRepository` will stop being a purely local seed generator for the IM path. Instead, it will become a state holder that issues a development session, hydrates conversations from backend bootstrap/history, maps backend DTOs into UI models, and updates visible state from authenticated WebSocket events. The repository may still keep in-memory observable state as the UI source of truth for this change, but that state will be sourced from backend APIs instead of invented locally.

Why this decision:
- The app cannot prove backend API behavior if it keeps synthesizing message state locally.
- An in-memory state holder backed by real APIs is a smaller first step than introducing a full Room synchronization engine and still satisfies the device-validation goal.
- The existing ViewModel/UI flow already expects repository-driven state, so this preserves current architecture boundaries.

Alternatives considered:
- Building a full Room-backed sync engine immediately: rejected because it adds scope beyond what is needed to validate the live contract on device.
- Injecting backend calls directly into Compose screens: rejected because it violates the repository and state-layer boundaries documented in the repo.

### 5. Upgrade the realtime client from a connection flag to an authenticated event adapter
`RealtimeChatClient` will be expanded to attach the backend bearer token, parse gateway event payloads, publish connection/error state, and expose send/read commands aligned to the backend event contract. The repository will own reconciliation so the UI sees a stable model rather than raw socket frames.

Why this decision:
- The current realtime client only reports open/closed state and cannot validate the backend message semantics.
- The Rust backend already has a concrete event schema, so a typed adapter reduces ambiguity and makes verification easier.
- Keeping event reconciliation out of the UI preserves testability and lets unit/instrumentation coverage focus on state outcomes.

Alternatives considered:
- Treating WebSocket payloads as opaque strings in the UI: rejected because it would make state recovery and testing brittle.
- Polling HTTP history for updates instead of consuming gateway events: rejected because it would not validate the low-latency IM path the backend was built for.

### 6. Make failure visibility and evidence capture part of the change, not afterthoughts
The app will surface connection/bootstrap/session failures in a visible debug-friendly manner, and the repository docs/tasks will define an evidence workflow that combines emulator steps, adb/logcat capture, backend container checks, and server-deployability notes. Task slices will end with proof that the emulator can run the targeted API subset against the local Docker backend image.

Why this decision:
- Device validation work often fails in transport/configuration layers first; silent fallback to seeds would hide the problem.
- The repository already enforces evidence-based acceptance through `docs/DELIVERY_WORKFLOW.md`.
- Explicit evidence requirements keep the change aligned with the user's request for real usage, not nominal code wiring.

Alternatives considered:
- Leaving diagnostics to ad hoc manual notes: rejected because it would not satisfy the repository's accepted-task workflow.
- Hiding failures behind placeholder fallback state: rejected because it would make the validation untrustworthy.

## Risks / Trade-offs

- [Local Docker validation can drift from later server deployment] → Keep the backend image shape and runtime env contract explicit so the same image can later be pulled onto the server.
- [The backend dev-session flow is not production auth] → Keep the change explicitly scoped to development-safe physical-device validation using seeded identities.
- [In-memory repository state still loses local cache on full process death] → Require bootstrap/history recovery on relaunch so backend durability, not local seeds, restores the app state.
- [Live validation may mutate shared backend data and blur test evidence] → Reserve a small set of known dev users and document cleanup/reset expectations in the validation workflow.
- [Existing Android UI tests are mostly fake-backed] → Add repository/client-level tests plus targeted instrumentation coverage so device-specific work does not rely only on manual testing.

## Migration Plan

1. Add IM backend profile storage plus settings/debug UI for device validation inputs.
2. Introduce backend DTOs/services for session/bootstrap/history and a richer authenticated WebSocket adapter.
3. Rework `MessagingRepository` and `AppContainer` so the Android IM surface can hydrate and update from the live backend.
4. Add backend Docker packaging plus automated verification around DTO mapping, repository reconciliation, and emulator-debug UI/state behavior.
5. Add the Docker/emulator validation guide, run the emulator acceptance flow, and record evidence in the delivery workflow.

Rollback strategy:
- Preserve a clean seam where the IM repository can temporarily fall back to a local implementation if backend validation blocks development.
- Keep device-validation settings additive so removing or disabling them does not force unrelated UI refactors.

## Open Questions

- For the later server pull/deploy step, do we want a single-image deployment contract only, or also a checked-in Compose example?
- Should future validation keep the emulator-first path as the default inner loop, with remote server validation moved to a later deployment-focused change?
