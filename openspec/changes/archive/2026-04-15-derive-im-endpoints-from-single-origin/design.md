## Context

The current Android IM wiring keeps three separate runtime inputs in preferences and in `Settings > IM Validation`: an HTTP base URL, a WebSocket URL, and a development user identifier. That is useful for emulator and deployment diagnostics, but it creates the wrong product contract for normal users because the app effectively asks them to understand backend topology and protocol mapping. It also leaves too much room for mismatched origins, insecure plaintext entry, and accidental drift between HTTP and realtime targets.

This change needs to preserve the existing validation and operator workflow without keeping those raw endpoint fields in the normal user experience. The safe direction is to center the app on one trusted backend origin, derive the matching realtime endpoint automatically, and keep any manual endpoint override behind a guarded developer or validation-only path.

## Goals / Non-Goals

**Goals:**
- Remove the need for ordinary users to enter separate server and WebSocket addresses.
- Resolve IM HTTP and WebSocket traffic from one backend-origin source of truth.
- Keep a developer or validation override path for emulator and deployment testing without making it the default product flow.
- Tighten release-safety rules so production users cannot silently connect to loopback, plaintext, or mismatched origins.

**Non-Goals:**
- Do not redesign backend auth, IM message semantics, or session persistence.
- Do not introduce a new service-discovery platform, config backend, or dynamic feature-flag system in this change.
- Do not remove the repository’s ability to validate against local Docker, emulator host bridge, or deployed Ubuntu endpoints during development.
- Do not require shipping PostgreSQL or infrastructure secrets in the Android client.

## Decisions

### 1. Use one canonical IM backend origin as the runtime source of truth

The app will resolve IM connectivity from one backend-origin value instead of storing separate user-managed HTTP and WebSocket addresses. HTTP API calls will use the normalized origin directly, and realtime will derive from the same authority by converting `https -> wss`, `http -> ws`, and appending the known realtime path.

Why:
- It removes the most common source of misconfiguration: HTTP and WebSocket fields pointing at different hosts or schemes.
- It gives the product a clearer contract: users trust one backend origin, not two transport-specific addresses.
- The existing backend already uses a stable `/ws` entry path, so the websocket side can be derived instead of typed manually.

Alternatives considered:
- Keep both fields and add better validation: rejected because users would still be responsible for infrastructure details.
- Introduce a remote discovery document first: rejected because it adds a second configuration hop before the simpler single-origin cleanup has been delivered.

### 2. Keep manual endpoint override only in a guarded developer-validation path

The regular Settings experience should stop exposing raw IM endpoint entry. If manual override remains, it should move behind a guarded developer or validation-only path and accept one backend origin override rather than separate HTTP and WebSocket fields.

Why:
- Normal users should not be asked to know or edit transport endpoints.
- Developers still need a way to validate against local Docker, emulator bridge, or a temporary remote backend during debugging.
- A single override origin keeps the validation story intact while reducing override complexity.

Alternatives considered:
- Remove override support entirely: rejected because it would break current emulator and remote deployment validation workflows.
- Keep the current IM Validation screen visible to everyone: rejected because it preserves the unsafe product contract we are trying to remove.

### 3. Apply stricter safety rules in release builds than in debug validation flows

Release runtime should only use a shipped trusted backend origin and should reject arbitrary loopback or plaintext overrides in the normal user path. Debug or validation workflows may continue allowing `http`/`ws` origins for local and lab testing, but those allowances must not leak into the production-facing route.

Why:
- The current deployed validation workflow still depends on non-production-friendly origins such as raw IPs and cleartext during development.
- Production safety requires a stricter boundary than validation convenience.
- Separating release and debug policies lets the team preserve current testing while moving the user-facing contract in a safer direction.

Alternatives considered:
- Enforce HTTPS/WSS everywhere immediately: rejected because it would break the existing validation flow before the infrastructure story is upgraded.
- Allow arbitrary schemes in all builds: rejected because it makes the release path too easy to misconfigure.

### 4. Centralize origin derivation in one resolver shared by auth, bootstrap, contacts, and realtime

The app should keep one resolver responsible for selecting the effective backend origin, normalizing it, deriving the websocket endpoint, and reporting the source used. Auth, bootstrap, contacts, user search, and realtime connection setup should all depend on that shared resolver instead of mixing persisted fields or local defaults independently.

Why:
- The repository already has endpoint-resolution logic; extending it is lower risk than scattering new rules across routes.
- A shared resolver ensures login/register/bootstrap/realtime stay aligned even after session restore or developer override.
- It creates one place to enforce scheme, host, and path safety rules.

Alternatives considered:
- Resolve origin separately inside each feature: rejected because it would recreate drift between auth and realtime paths.

## Risks / Trade-offs

- [Debug users may lose easy access to the current raw IM Validation fields] → Mitigation: preserve a guarded validation-only route with a single override-origin field and the existing dev-user selector.
- [Release safety rules may temporarily diverge from the current plain-HTTP deployment workflow] → Mitigation: keep stricter rules scoped to production-facing flows while retaining debug-only validation allowances until infrastructure is upgraded.
- [Auto-derived websocket paths could fail if backend routing changes later] → Mitigation: centralize the derivation contract and keep the websocket path as one well-defined constant rather than repeating string assembly across features.
- [Existing tests may assume separate HTTP and WebSocket settings] → Mitigation: update focused resolver, settings, auth, and live-validation coverage around the single-origin contract.

## Migration Plan

1. Introduce a single IM backend-origin preference and resolver path that can derive HTTP and websocket endpoints consistently.
2. Update auth, bootstrap, contacts, search, and realtime wiring to consume the derived endpoints from the shared resolver.
3. Replace the normal user-facing endpoint-entry UI with a product-facing status or environment summary, and move any manual override controls into a guarded validation path.
4. Refresh tests and validation coverage so release flows use the trusted origin contract while debug flows still cover override-based validation.
5. Update Android operator guidance to distinguish the normal user path from the guarded developer-validation workflow.

Rollback strategy:
- Restore the current separate IM HTTP and WebSocket preference fields and user-facing validation inputs if the single-origin contract proves incompatible with existing backend routing or validation needs.

## Open Questions

- What production backend origin should be treated as the shipped trusted release target once the infrastructure is ready for end-user traffic?
- Should the guarded developer-validation path be debug-build-only, hidden behind a gesture, or exposed behind an explicit developer-mode toggle?
