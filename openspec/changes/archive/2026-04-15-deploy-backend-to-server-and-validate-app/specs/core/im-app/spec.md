## MODIFIED Requirements

### Requirement: Android app exercises live IM backend flows during emulator validation
The system SHALL let the Android app authenticate through the backend development session flow, hydrate conversations and message history from the live IM backend, and reconcile authenticated WebSocket events into visible chat state during Android validation runs against an operator-selected backend service endpoint. That validation path MUST support a deployed remote backend in addition to a locally containerized backend, so operators can point the app at the published Ubuntu-hosted service and verify the live user flow without rebuilding the APK.

#### Scenario: Validation bootstraps live conversation data from the selected backend endpoint
- **WHEN** a tester starts an IM validation session on the Android app with a configured development user and a selected backend HTTP/WebSocket endpoint
- **THEN** the app issues the backend session/bootstrap requests to that selected service endpoint and renders conversation state from live backend data instead of seed-only in-memory messaging rows

#### Scenario: Validation drives realtime send and receipt updates against the deployed backend
- **WHEN** the tester sends a message from the Android app while the deployed backend and counterpart session are online
- **THEN** the app updates the visible conversation from the live backend send, receive, delivered, and read flows instead of relying on local-only message append behavior

#### Scenario: Validation recovers after reconnect or relaunch against the selected backend endpoint
- **WHEN** the Android app loses its realtime connection or is relaunched during a validation session against the selected backend service
- **THEN** it can recover current conversation state from that backend’s bootstrap/history APIs and resume WebSocket synchronization without requiring a rebuild or seed reset

#### Scenario: Validation surfaces remote backend failures explicitly
- **WHEN** session issuance, bootstrap loading, history retrieval, or realtime connection setup fails against the selected backend endpoint
- **THEN** the app shows an explicit validation/debug failure state instead of silently falling back to placeholder success behavior
