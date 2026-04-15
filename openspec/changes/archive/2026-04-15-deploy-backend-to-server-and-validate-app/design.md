## Context

The repository already has most of the building blocks for this work, but they are split across separate local-first workflows. `backend/README.md` documents how to bootstrap the Rust IM backend on the Ubuntu host at `124.222.15.128`, while `android/README.md` still treats local Docker plus emulator host-bridge validation as the primary app-validation path. The specs also reflect that split: `im-backend` already expects remote deployment/debug support on the target host, but `core/im-app` still frames live IM validation mainly around a locally containerized backend.

The requested outcome is end-to-end and operational rather than purely code-local: publish the backend to the server, point the Android app at that deployed service, launch the app, and confirm the functional path actually works. That means the design needs to connect existing deployment scaffolding, app endpoint configuration, and remote validation evidence into one repeatable flow without committing secrets or relying on local-only assumptions.

## Goals / Non-Goals

**Goals:**
- Make the backend deployment path to the Ubuntu host repeatable enough that an accepted slice can be started, checked, and debugged remotely.
- Ensure the Android app can be configured to use the deployed server backend for auth/bootstrap/realtime validation without rebuilding the APK.
- Define a remote validation workflow that proves the live server path works from both the backend and Android sides.
- Keep repository guidance and OpenSpec requirements aligned with the remote deployed-service workflow.

**Non-Goals:**
- Do not redesign the backend architecture, auth model, or IM message semantics.
- Do not introduce a new infrastructure platform, orchestrator, or CI/CD deployment stack in this change.
- Do not commit SSH passwords, database credentials, or filled runtime env files into the repository.
- Do not expand this into release APK distribution, app-store packaging, or general device-farm testing.

## Decisions

### 1. Reuse the existing Ubuntu bootstrap/service shape instead of introducing a new deployment mechanism

The deployed backend should continue to use the existing `/opt/gkim-im/backend` checkout, `/etc/gkim-im-backend/gkim-im-backend.env` secret file, and `gkim-im-backend.service` systemd service shape.

Why:
- The current backend docs and `im-backend` spec already anchor on this server model.
- Reusing the existing bootstrap scripts keeps the change focused on making deployment repeatable and validated, rather than inventing a second operations path.

Alternatives considered:
- Introduce Docker Compose or a new deployment stack on the Ubuntu host: rejected because it would enlarge the change and duplicate existing systemd-oriented guidance.
- Limit the change to documentation only: rejected because the user explicitly wants the deployment and validation path, not just notes about it.

### 2. Use operator-managed app endpoint settings for the deployed backend instead of hardcoding server URLs into the app

The Android app should continue to consume HTTP and WebSocket endpoint values through the existing IM validation configuration path, with the change focused on making that path reliable for deployed-server validation.

Why:
- The current product contract already centers on service-boundary inputs instead of baked-in backend addresses.
- This allows the same build to target local host-bridge validation or the deployed server, depending on what operators enter.

Alternatives considered:
- Bake the server IP into default app configuration: rejected because it would make deployment assumptions part of product defaults and would regress the current operator-managed endpoint model.

### 3. Treat remote validation as a layered acceptance chain rather than a single manual spot-check

Acceptance should be defined in layers: server process/service health, remote HTTP/bootstrap reachability, and app-side validation against the deployed backend.

Why:
- When a remote validation attempt fails, the team needs to know whether the problem is deployment, published endpoints, or Android client configuration.
- Layered checks match the repository’s existing delivery-quality workflow and reduce guesswork during remote debugging.

Alternatives considered:
- Rely on “open the app and see if it works”: rejected because it obscures root cause and makes remote failures harder to diagnose.

### 4. Keep evidence capture in the repository workflow documents instead of creating a separate remote-ops log format

The change should record accepted validation evidence through `docs/DELIVERY_WORKFLOW.md` and update the maintained backend/Android guidance only where the remote path changes the standing operator story.

Why:
- The repository already uses `docs/DELIVERY_WORKFLOW.md` as the acceptance ledger.
- A single evidence format avoids a split between implementation proof and operations proof.

Alternatives considered:
- Create a new remote-validation report format: rejected because it would duplicate the existing evidence mechanism.

## Risks / Trade-offs

- [Remote server access or network reachability may be flaky during validation] → Mitigation: define acceptance in layers so failures can be isolated to service status, endpoint reachability, or app configuration.
- [The app may still carry local-only assumptions in some flows even if IM validation settings are updated] → Mitigation: include focused Android verification against the deployed endpoint path, not only backend-side smoke tests.
- [Publishing a remote endpoint may surface TLS, firewall, or bind-address mismatches] → Mitigation: require server health and endpoint reachability checks as part of the deployment acceptance path.
- [Operational guidance can drift away from the real accepted workflow] → Mitigation: update the maintained backend/Android docs and capture accepted evidence in the delivery log as part of the same change.

## Migration Plan

1. Review the existing backend bootstrap/service flow and tighten any missing deployment/debug commands needed for repeatable remote rollout.
2. Deploy or redeploy the backend slice to the Ubuntu host using secret-managed runtime inputs and confirm service health remotely.
3. Ensure the Android app’s IM validation path can target the deployed backend HTTP/WebSocket endpoints cleanly.
4. Run layered remote verification: backend service health/status, remote auth/bootstrap or equivalent smoke checks, then app-side validation against the deployed service.
5. Record evidence in `docs/DELIVERY_WORKFLOW.md` and update maintained operator guidance.

Rollback strategy:
- Revert to the last known-good backend deployment on the Ubuntu host and restore the previous published endpoint guidance if the new remote validation path proves unstable.

## Open Questions

- What final public HTTP and WebSocket origins should operators treat as the canonical remote validation endpoints: raw host IP/port, reverse-proxied domain, or another published gateway?
- Should the first accepted remote app validation target remain the emulator, or is a physical-device pass required before this change is considered complete?
