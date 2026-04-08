## Why

The app currently has only client-side placeholders and mock repositories, but no real backend service to provide IM-grade messaging, persistence, or realtime delivery. We need a first Rust backend milestone now so the product can move from local demo state to a debuggable server environment on the Ubuntu host at `124.222.15.128` while keeping PostgreSQL access on the backend side and giving IM traffic a low-latency path.

## What Changes

- Introduce a Rust-based backend service that exposes HTTP and WebSocket interfaces for core IM workflows instead of relying on in-memory client-only data.
- Add backend capabilities for conversation bootstrap, message persistence, realtime message delivery, unread state, and basic presence/session handling suitable for an IM MVP.
- Define a remote-debuggable deployment path for the Ubuntu server at `124.222.15.128:22`, including secret-managed SSH and PostgreSQL configuration instead of checked-in credentials.
- Add backend-side observability, health checking, and verification hooks so each implementation slice can be deployed to the server and smoke-tested immediately.
- Keep the first backend milestone focused on IM fundamentals; feed rendering, workshop content, and full production-grade multi-node scaling remain later changes.

## Capabilities

### New Capabilities
- `im-backend`: Rust backend service for authenticated IM sessions, PostgreSQL-backed conversation/message state, realtime WebSocket delivery, and Ubuntu-host deployment/debug workflows.

### Modified Capabilities
- None.

## Impact

- Affected code: new backend Rust workspace or module tree, backend configuration and deployment scripts, PostgreSQL migrations, and backend verification tooling.
- Affected systems: Ubuntu server `124.222.15.128`, backend-only PostgreSQL connectivity, client-facing HTTP/WebSocket service contracts, and remote debugging workflows over SSH.
- Affected dependencies: Rust async web/runtime stack, PostgreSQL driver and migration tooling, structured logging/metrics, and secret-managed server environment files instead of checked-in passwords.
