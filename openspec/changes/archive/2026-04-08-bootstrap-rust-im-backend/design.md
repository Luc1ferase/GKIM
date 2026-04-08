## Context

The repository currently contains Android and legacy UniAPP client scaffolds, but the data layer is still driven by local seed data, placeholder Retrofit base URLs, and a stub WebSocket client. The product specs already require backend service boundaries, and the infrastructure notes now point PostgreSQL usage to the backend side at `124.222.15.128:5432`, but there is still no real backend service to power IM workflows.

The user wants backend development to start now, using Rust, with each accepted slice debugged directly on the Ubuntu server at `124.222.15.128:22`. At the same time, this request is too broad to treat as "the whole platform" in one change: full auth, group chat, media pipelines, AIGC orchestration, multi-node fanout, and production SRE hardening would create an oversized first milestone. This design therefore defines a first backend slice focused on a single-node IM MVP that can support direct messaging, conversation sync, unread state, and low-latency realtime delivery.

## Goals / Non-Goals

**Goals:**
- Create a Rust backend foundation that clients can reach through HTTP and WebSocket instead of local mock repositories.
- Persist IM state in PostgreSQL on the backend side, including users, conversations, messages, unread metadata, and basic session state.
- Deliver direct messages with low observable latency on a single-node deployment by using persistent WebSocket connections and in-process fanout rather than polling.
- Make each implementation slice deployable and debuggable on the Ubuntu host at `124.222.15.128` through SSH-driven workflows, without committing SSH or PostgreSQL passwords into the repository.
- Keep the first backend milestone small enough to implement and verify incrementally under the repository's per-task quality gate workflow.

**Non-Goals:**
- Full production auth, account recovery, admin tooling, or multi-tenant identity management.
- Group chat, media upload/storage, push notifications, end-to-end encryption, or federation.
- Feed, workshop, or AIGC backend orchestration in this same change.
- Multi-node horizontal scaling, Redis/NATS fanout, or cross-region latency optimization in the first milestone.

## Decisions

### 1. Scope the first backend milestone to single-node direct messaging on Rust + PostgreSQL
The first implementation slice will support 1:1 conversations only, plus the minimum state required for IM: dev session bootstrap, contacts/conversation bootstrap, message history pagination, unread counters, presence heartbeat, and realtime direct-message delivery.

Why this decision:
- The request is broad, but the repository needs an implementable first backend milestone rather than a speculative platform rewrite.
- 1:1 messaging exercises the core persistence and fanout paths that later features will reuse.
- A smaller scope is easier to deploy, benchmark, and debug repeatedly on the target Ubuntu server.

Alternatives considered:
- Designing the full backend platform, including group chat and media, in one change: rejected because it would be too large for a single spec/apply cycle.
- Deferring persistence and starting with a WebSocket-only in-memory server: rejected because it would not validate the PostgreSQL-backed contract the clients ultimately need.

### 2. Use a Rust async service built on Axum, Tokio, SQLx, and WebSocket upgrades
The backend will use an async Rust stack: Axum for HTTP and WebSocket entry points, Tokio for runtime/concurrency, SQLx for PostgreSQL access and migrations, Serde for payloads, and tracing-based structured logs for diagnostics.

Why this decision:
- Rust gives predictable performance and a good fit for long-lived connection workloads.
- Axum keeps HTTP and WebSocket handling in one codebase with strong typing and middleware support.
- SQLx allows compile-checked queries and built-in migration tooling without introducing an ORM abstraction that slows early iteration.

Alternatives considered:
- Actix Web: viable, but Axum fits the repository's preference for straightforward layered service composition.
- Diesel: rejected for the first slice because SQLx plus explicit SQL migrations is easier to evolve while the data model is still settling.

### 3. Start with development-safe session bootstrap instead of full account auth
The first milestone will authenticate against seeded or bootstrap-managed users and issue signed development session tokens for HTTP and WebSocket access. This keeps database credentials on the backend while unblocking end-to-end IM flows before a richer auth product exists.

Why this decision:
- The current clients do not yet expose a complete login/account flow.
- A lightweight session layer is enough to validate identity-bound conversations, unread state, and presence.
- This approach avoids coupling the first backend milestone to a large authentication project.

Alternatives considered:
- Anonymous client identities: rejected because unread state, conversation membership, and receipts need stable user identities.
- Full production auth in the same change: rejected as too much scope for the first backend slice.

### 4. Separate bootstrap/sync traffic over HTTP from low-latency message fanout over WebSocket
The backend will expose HTTP endpoints for health, session bootstrap, contacts/conversation bootstrap, and paginated message history. Realtime send, delivery, read, presence, and incoming message events will flow through a persistent WebSocket connection keyed by the authenticated user.

Why this decision:
- HTTP is a clean fit for initial state fetches and history pagination.
- WebSocket avoids request-response polling for active conversations and reduces message delivery latency for online users.
- This split maps cleanly onto the existing client-side placeholder architecture, which already expects HTTPS and WebSocket boundaries.

Alternatives considered:
- Pure REST with long polling: rejected because it works against the latency requirement.
- WebSocket-only for every workflow: rejected because bootstrap and history fetches are easier to reason about as HTTP resources.

### 5. Persist first, then fan out, while keeping an in-memory online-session hub for the single node
On send, the backend will validate membership, write the message and receipt state to PostgreSQL, update unread counters, then publish the event to any in-memory online sessions for the recipient and sender. The single-node in-memory session hub is acceptable for the first milestone because the target deployment is one Ubuntu host.

Why this decision:
- Persistence-before-fanout avoids losing accepted messages if a node crashes after emitting a transient push event.
- An in-memory connection registry is the simplest low-latency approach for a one-node deployment.
- This keeps the path to later Redis/NATS fanout open without paying that complexity now.

Alternatives considered:
- Fanout before persistence: rejected because it weakens durability semantics.
- Introducing Redis in the first slice: rejected because it increases operational scope before one-node behavior is validated.

### 6. Deploy and debug directly on the Ubuntu server with secret-managed env files and a repeatable service workflow
The implementation should include a backend directory, environment template, deployment/debug scripts, and a `systemd`-friendly service shape that can be pushed to `124.222.15.128`, started under the `ubuntu` user with `sudo` support when needed, and smoke-tested after each accepted slice. SSH host/user can be documented, but the SSH password and database password must remain outside version-controlled files.

Why this decision:
- The user explicitly wants server-side debugging after each completed slice.
- A repeatable deployment path prevents "works on my machine" drift between local and remote debugging.
- `systemd` on Ubuntu gives a stable service lifecycle and log access without overcomplicating the first milestone with container orchestration.

Alternatives considered:
- Manual ad hoc `cargo run` sessions only: rejected because they are too easy to drift and harder to verify repeatedly.
- Docker/Kubernetes first: rejected because the request prioritizes direct server debugging, not orchestration.

## Risks / Trade-offs

- [The first backend slice could still grow too large if every IM feature is treated as mandatory] → Keep the change limited to direct messaging fundamentals and defer groups/media/AIGC backend work.
- [A seeded or bootstrap auth model is not production auth] → Make that limitation explicit in the spec and tasks so later auth hardening can replace it intentionally.
- [Single-node in-memory fanout will not scale horizontally] → Treat it as an intentional first milestone design and leave the persistence model ready for a future broker-backed fanout layer.
- [Remote server debugging can leak secrets if scripts are careless] → Keep passwords out of repo files, require env-file or secret-store loading, and avoid writing live secrets into OpenSpec artifacts.
- [Low latency depends on the target server and network conditions, not just code structure] → Verify the no-polling realtime path and collect delivery timing evidence from the remote host after each accepted slice.

## Migration Plan

1. Add a new Rust backend workspace or service directory with configuration loading, tracing, health routes, and PostgreSQL connectivity/migration support.
2. Define PostgreSQL schema and repositories for users, contacts, conversations, messages, unread counters, and session tokens.
3. Implement dev-safe session bootstrap and HTTP sync endpoints for contacts, conversations, and history pagination.
4. Add the authenticated WebSocket gateway for presence, message send, delivery/read events, and online fanout.
5. Add repeatable deployment/debug scripts plus a `systemd` service shape for the Ubuntu host at `124.222.15.128`, then verify each accepted slice on the remote server.

## Open Questions

- What exact database name should the backend use on the existing PostgreSQL host?
- Should the first backend slice expose only development-seeded accounts, or is there already a preferred initial user bootstrap mechanism?
- Does the Ubuntu server already have Rust toolchain, PostgreSQL client libraries, and firewall openings for the backend HTTP/WebSocket ports, or must the change bootstrap them too?
- What latency threshold should count as acceptable for remote debug smoke tests on the first single-node deployment?
