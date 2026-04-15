## MODIFIED Requirements

### Requirement: Backend supports remote deployment and debugging on the target Ubuntu host
The system SHALL include a repeatable remote deployment/debug workflow for the Ubuntu host at `124.222.15.128`, and it MUST let each accepted implementation slice be started, inspected, and smoke-tested on that server through SSH-accessible operational commands without committing the SSH password. The deployed service MUST expose published HTTP and WebSocket endpoints that are suitable for Android end-to-end validation, and remote acceptance MUST include enough server-side checks to distinguish service-health problems from Android client configuration problems.

#### Scenario: Accepted backend slice is smoke-tested on the server
- **WHEN** a backend implementation slice is accepted for delivery
- **THEN** the repository provides the scripts, service shape, or documented commands needed to deploy that slice to `124.222.15.128`, inspect logs or service status, confirm the backend health endpoint, and run a remote smoke test before the next slice begins

#### Scenario: Deployed backend endpoints are suitable for Android validation
- **WHEN** operators publish the backend HTTP and WebSocket endpoints for Android validation against the Ubuntu host
- **THEN** those endpoints are reachable for remote auth/bootstrap and realtime traffic without relying on local Docker port publishing, adb reverse, or SSH-tunnel-only assumptions
