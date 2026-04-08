## 1. Backend scaffold and remote-debug foundation

- [x] 1.1 Create a Rust backend workspace or service directory with Axum/Tokio entrypoints, structured config loading, tracing, and a health endpoint that can boot without checked-in secrets.
- [x] 1.2 Add backend environment templates plus Ubuntu deployment/debug assets for `124.222.15.128`, including SSH-safe scripts or service-unit scaffolding that keep SSH and PostgreSQL passwords outside the repository.

## 2. PostgreSQL-backed IM domain and bootstrap APIs

- [x] 2.1 Add SQLx migrations and schema for development users, contacts, direct conversations, messages, unread/delivery state, and session tokens required by the first IM milestone.
- [x] 2.2 Implement repository and service layers that persist direct-message state in PostgreSQL and can rebuild conversation bootstrap, unread counts, and paginated message history for reconnecting users.
- [x] 2.3 Implement the development-safe session bootstrap flow plus authenticated HTTP endpoints for health, session issuance, contacts/conversation bootstrap, and message history retrieval.

## 3. Low-latency realtime messaging path

- [x] 3.1 Implement the authenticated WebSocket gateway with connection tracking, heartbeat or presence updates, and user-scoped session registration on the single-node deployment.
- [x] 3.2 Implement persist-then-fanout direct-message send flow, delivery/read event emission, and offline unread recovery so online users receive pushed updates without polling.

## 4. Verification and server-side delivery evidence

- [x] 4.1 Add automated verification for config loading, SQLx-backed bootstrap/history behavior, session auth, and WebSocket messaging semantics.
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

### Task 1.2: Add backend environment templates plus Ubuntu deployment/debug assets for `124.222.15.128`, including SSH-safe scripts or service-unit scaffolding that keep SSH and PostgreSQL passwords outside the repository.

- Verification:
  - `cd backend && cargo test` - pass (`3` unit tests)
  - `cd backend && cargo check` - pass
  - `git -c safe.directory=X:/Repos/GKIM check-ignore -v backend/.env.local backend/.env.server.local` - pass (`.env.local` and `.env.server.local` are ignored)
  - `rg -n "PGHOST=124\\.222\\.15\\.128|PGPASSWORD=<set in untracked \\.env\\.local or deployment secret>|PGSSLROOTCERT=" backend/.env.example` - pass
  - `rg -n "EnvironmentFile=/etc/gkim-im-backend/gkim-im-backend.env|ExecStart=/opt/gkim-im/backend/target/release/gkim-im-backend|User=ubuntu" backend/systemd/gkim-im-backend.service` - pass
  - `rg --quiet "mb8TggtXiA9B6mWV|Fezr8tg-_qfvqGHy!P38" backend` - pass (no live secrets found)
  - `C:\\Program Files\\Git\\bin\\bash.exe -n backend/scripts/bootstrap-ubuntu.sh backend/scripts/debug-service.sh backend/scripts/smoke-health.sh` - pass
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `3d660e8`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.1: Add SQLx migrations and schema for development users, contacts, direct conversations, messages, unread/delivery state, and session tokens required by the first IM milestone.

- Verification:
  - `cd backend && cargo fmt --check` - pass
  - `cd backend && cargo test` - pass (`5` tests)
  - `cd backend && cargo check` - pass
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `ed7e79b`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.2: Implement repository and service layers that persist direct-message state in PostgreSQL and can rebuild conversation bootstrap, unread counts, and paginated message history for reconnecting users.

- Verification:
  - `cd backend && cargo fmt --check` - pass
  - `cd backend && cargo test` - pass (`7` tests)
  - `cd backend && cargo check` - pass
  - `cd backend && GKIM_TEST_DATABASE_URL=<redacted> cargo test --test im_service_pg` - pass (`2` PostgreSQL integration tests against the target host)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `bf0b43e`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.3: Implement the development-safe session bootstrap flow plus authenticated HTTP endpoints for health, session issuance, contacts/conversation bootstrap, and message history retrieval.

- Verification:
  - `cd backend && cargo fmt --check` - pass
  - `cd backend && cargo test` - pass (`9` tests)
  - `cd backend && cargo check` - pass
  - `cd backend && GKIM_TEST_DATABASE_URL=<redacted> cargo test --test http_im_api --test im_service_pg` - pass (`4` PostgreSQL integration tests across HTTP and service layers)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `1e8c264`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 3.1: Implement the authenticated WebSocket gateway with connection tracking, heartbeat or presence updates, and user-scoped session registration on the single-node deployment.

- Verification:
  - `cd backend && cargo fmt --check` - pass
  - `cd backend && cargo test` - pass (`11` tests)
  - `cd backend && cargo check` - pass
  - `cd backend && GKIM_TEST_DATABASE_URL=<redacted> cargo test --test http_im_api --test im_service_pg --test ws_gateway` - pass (`5` PostgreSQL integration tests across HTTP, service, and WebSocket layers)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `fd6323b`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 3.2: Implement persist-then-fanout direct-message send flow, delivery/read event emission, and offline unread recovery so online users receive pushed updates without polling.

- Verification:
  - `cd backend && cargo fmt --check` - pass
  - `cd backend && cargo test` - pass (`13` tests)
  - `cd backend && cargo check` - pass
  - `cd backend && GKIM_TEST_DATABASE_URL=<redacted> cargo test --test http_im_api --test im_service_pg --test ws_gateway` - pass (`7` PostgreSQL integration tests across HTTP, service, and WebSocket layers)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `faa573e`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 4.1: Add automated verification for config loading, SQLx-backed bootstrap/history behavior, session auth, and WebSocket messaging semantics.

- Verification:
  - `cd backend && cargo fmt --check` - pass
  - `cd backend && cargo test` - pass (`13` tests)
  - `cd backend && cargo check` - pass
  - `cd backend && GKIM_TEST_DATABASE_URL=<redacted> cargo test --test http_im_api --test im_service_pg --test ws_gateway` - pass (`7` PostgreSQL integration tests across HTTP, service, and WebSocket layers)
- Review:
  - Score: `98/100`
  - Findings: `No findings`
- Upload:
  - Commit: `5761224`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`
