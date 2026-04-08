## 1. Backend scaffold and remote-debug foundation

- [x] 1.1 Create a Rust backend workspace or service directory with Axum/Tokio entrypoints, structured config loading, tracing, and a health endpoint that can boot without checked-in secrets.
- [ ] 1.2 Add backend environment templates plus Ubuntu deployment/debug assets for `124.222.15.128`, including SSH-safe scripts or service-unit scaffolding that keep SSH and PostgreSQL passwords outside the repository.

## 2. PostgreSQL-backed IM domain and bootstrap APIs

- [ ] 2.1 Add SQLx migrations and schema for development users, contacts, direct conversations, messages, unread/delivery state, and session tokens required by the first IM milestone.
- [ ] 2.2 Implement repository and service layers that persist direct-message state in PostgreSQL and can rebuild conversation bootstrap, unread counts, and paginated message history for reconnecting users.
- [ ] 2.3 Implement the development-safe session bootstrap flow plus authenticated HTTP endpoints for health, session issuance, contacts/conversation bootstrap, and message history retrieval.

## 3. Low-latency realtime messaging path

- [ ] 3.1 Implement the authenticated WebSocket gateway with connection tracking, heartbeat or presence updates, and user-scoped session registration on the single-node deployment.
- [ ] 3.2 Implement persist-then-fanout direct-message send flow, delivery/read event emission, and offline unread recovery so online users receive pushed updates without polling.

## 4. Verification and server-side delivery evidence

- [ ] 4.1 Add automated verification for config loading, SQLx-backed bootstrap/history behavior, session auth, and WebSocket messaging semantics.
- [ ] 4.2 Deploy accepted backend slices to `124.222.15.128`, run remote smoke tests and latency-focused message-flow checks there, and capture the required evidence in `docs/DELIVERY_WORKFLOW.md`.

## Task Evidence

### Task 1.1: Create a Rust backend workspace or service directory with Axum/Tokio entrypoints, structured config loading, tracing, and a health endpoint that can boot without checked-in secrets.

- Verification:
  - `cd backend && cargo fmt --check` - pass
  - `cd backend && cargo test` - pass (`3` unit tests)
  - `cd backend && cargo check` - pass
  - `cd backend && APP_SERVICE_NAME=gkim-im-backend APP_BIND_ADDR=127.0.0.1:18080 DATABASE_URL=postgres://example APP_LOG_FILTER=info target/debug/gkim-im-backend && GET /health` - pass (`200 OK`, body `{"service":"gkim-im-backend","status":"ok"}`)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `30d8bdc`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`
