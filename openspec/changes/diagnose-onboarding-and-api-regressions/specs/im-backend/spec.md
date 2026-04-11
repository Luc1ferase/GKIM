## MODIFIED Requirements

### Requirement: Rust IM backend boots with secret-managed server configuration
The system SHALL provide a Rust backend service that runs as an HTTP and WebSocket server on the target Ubuntu host, and it MUST load PostgreSQL connection details, bind address, and other operational secrets from backend-only environment inputs instead of checked-in credentials. The published HTTP base URL used for Android emulator validation MUST resolve to a host-reachable service endpoint for auth and bootstrap traffic, and validation guidance MUST NOT assume that `127.0.0.1` inside the emulator/device runtime is automatically the backend host.

#### Scenario: Backend service boots on the Ubuntu host
- **WHEN** the backend process starts on the Ubuntu server at `124.222.15.128`
- **THEN** it reads its runtime configuration from secret-managed environment values, establishes PostgreSQL connectivity, and exposes a health-checkable service endpoint without requiring database or SSH passwords inside the repository

#### Scenario: Emulator-facing auth API target is host-reachable
- **WHEN** operators select the HTTP base URL used by the Android emulator for registration, login, and bootstrap validation
- **THEN** that URL points to a backend service endpoint that is reachable from the emulator/runtime boundary instead of relying on device-local `127.0.0.1` by assumption
