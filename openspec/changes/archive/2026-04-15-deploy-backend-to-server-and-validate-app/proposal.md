## Why

The repository already documents local backend startup and emulator-to-host validation, but the user now needs the product to run end-to-end against the real server deployment instead of stopping at local Docker or host-bridge assumptions. We need a focused change that turns the existing deployment notes and app endpoint controls into a repeatable remote deployment plus app-validation workflow so the backend can be published to the Ubuntu host, the Android app can target that deployed service, and functional checks can prove the live path works.

## What Changes

- Deploy the Rust IM backend to the target Ubuntu server using the existing secret-managed runtime model, and tighten the repository workflow so the deployed service can be health-checked and debugged remotely.
- Refine the Android IM validation path so the app can be pointed at the deployed server backend, not only a locally published host endpoint.
- Add or refresh verification coverage and operator guidance for the remote validation chain: server health, auth/bootstrap reachability, and app-side live IM checks against the deployed backend.
- Keep backend secrets, SSH credentials, and database credentials outside version control while making the remote deployment steps repeatable.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `im-backend`: the remote deployment requirement needs to define a stricter deployed-service acceptance path, including server-side health/debug checks that are suitable for Android end-to-end validation against the Ubuntu host.
- `core/im-app`: the live IM validation contract needs to cover operator-driven app validation against a deployed remote backend endpoint, not only a locally containerized or host-bridged backend.

## Impact

- Affected code: backend deployment scripts/config guidance under `backend/`, Android IM validation and endpoint-handling surfaces under `android/app/src/main/java/com/gkim/im/android/**`, and any targeted app/backend validation tests.
- Affected specs: `openspec/specs/im-backend/spec.md` and `openspec/specs/core/im-app/spec.md`.
- Affected systems: Ubuntu-hosted Rust backend service, Android client runtime endpoint configuration, and the remote validation workflow captured in repository docs/evidence.
- Affected dependencies: SSH-accessible Ubuntu operations flow, backend runtime env management, and Android verification paths that exercise the deployed service boundary.
