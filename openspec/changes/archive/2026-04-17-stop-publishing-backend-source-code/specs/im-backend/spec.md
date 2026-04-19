## MODIFIED Requirements

### Requirement: Backend supports remote deployment and debugging on the target Ubuntu host
The system SHALL include a repeatable remote deployment/debug workflow for the current Ubuntu host behind `chat.lastxuans.sbs`, and it MUST let each accepted implementation slice be started, inspected, and smoke-tested on that server through SSH-accessible operational commands without committing the SSH password. That workflow MUST be satisfiable with a maintainer-held local-only or otherwise private backend checkout, and it MUST NOT require the published remote Git repository to carry backend source files. The deployed service MUST expose published HTTP and WebSocket endpoints that are suitable for Android end-to-end validation, and remote acceptance MUST include enough server-side checks to distinguish service-health problems, outdated backend binaries, and published-endpoint drift from Android client configuration problems.

#### Scenario: Accepted backend slice is smoke-tested on the server
- **WHEN** a backend implementation slice is accepted for delivery
- **THEN** the maintainers' local/private backend materials provide the scripts, service shape, or documented commands needed to deploy that slice to the Ubuntu host, inspect logs or service status, confirm the backend health endpoint, and run a remote smoke test before the next slice begins

#### Scenario: Published backend is verified to run the image-message-capable version
- **WHEN** a backend slice that includes direct image-message behavior is deployed to the Ubuntu host
- **THEN** deployment acceptance proves host-local and published support for `POST /api/direct-messages/image` plus attachment fetch, instead of stopping at generic health or bootstrap checks

#### Scenario: Deployed backend endpoints are suitable for Android validation
- **WHEN** operators publish the backend HTTP and WebSocket endpoints for Android validation against the Ubuntu host
- **THEN** those endpoints are reachable for remote auth/bootstrap, realtime traffic, and the current accepted image-message API contract without relying on local Docker port publishing, adb reverse, or SSH-tunnel-only assumptions
