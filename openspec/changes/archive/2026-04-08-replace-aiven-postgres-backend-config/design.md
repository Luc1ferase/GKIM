## Context

The repository still contains backend-facing notes that were written around the previous Aiven PostgreSQL setup, including references to the old certificate flow and Aiven-specific mirrored paths. At the same time, the product spec already establishes an important constraint: the Android client may only talk to HTTPS and WebSocket services and must never carry direct PostgreSQL credentials or database trust material in the APK.

Operations now wants the backend side of the system to move to a different PostgreSQL instance at `124.222.15.128:5432`. The supplied access details are useful for deployment, but they are secrets and therefore unsuitable for checked-in OpenSpec artifacts, versioned default configs, or any mobile runtime surface. This design treats the provider switch as a backend-configuration and documentation change that preserves the existing mobile boundary.

## Goals / Non-Goals

**Goals:**
- Replace Aiven-specific repository guidance with provider-agnostic backend PostgreSQL configuration guidance for the new database target.
- Keep the Android client contract unchanged: mobile code continues to use only backend APIs and realtime service endpoints.
- Define a safe configuration pattern where host, role, password, optional database name, and any optional TLS inputs are injected from untracked deployment secrets rather than committed to repo files.
- Identify the implementation surfaces that need to stop referring to the old Aiven certificate story so future work does not drift back to unsafe assumptions.

**Non-Goals:**
- Adding direct PostgreSQL connectivity to the Android app.
- Committing the provided password or any future production secret into version control.
- Designing a full backend service or data-migration workflow beyond the configuration and repository-contract changes needed for this switch.
- Locking in a TLS mode before operations confirms whether the new PostgreSQL server requires SSL and, if so, what trust chain it uses.

## Decisions

### 1. Keep PostgreSQL access backend-only and leave the Android runtime on service endpoints
The change will preserve the existing service-boundary contract in `core/im-app`: the app continues to consume HTTPS and WebSocket capabilities only, even after the database provider changes.

Why this decision:
- The existing product spec already forbids direct PostgreSQL credentials inside the client runtime.
- The new host, role, and password are operational infrastructure inputs, not mobile product settings.
- Preserving the service boundary avoids introducing a security regression while switching providers.

Alternatives considered:
- Pointing the Android app directly at `124.222.15.128:5432`: rejected because it violates the current architecture and would expose raw database credentials to the client.

### 2. Treat the new database inputs as secret-managed deployment configuration, not checked-in DSN text
Implementation should prefer secret-managed backend runtime inputs such as host, port, role, password, optional database name, and optional SSL settings, whether they are injected as discrete environment variables or assembled into a DSN at deploy time. OpenSpec artifacts may name the target host and describe the configuration pattern, but they must not store the raw password.

Why this decision:
- The user-supplied password is sensitive and should not be copied into proposal, design, tasks, source files, or sample configs.
- Secret-managed fields are easier to rotate than a repo-baked DSN.
- This pattern works whether the eventual backend is a small relay service, a future server module, or deployment automation outside this repository.

Alternatives considered:
- Keeping a checked-in DSN template with live credentials: rejected because it leaks secrets into version control and review history.
- Leaving the repo on the old Aiven note until backend code exists: rejected because the stale guidance would keep pushing future work toward the wrong database target.

### 3. Remove Aiven-specific certificate assumptions unless the replacement server explicitly requires new trust material
The repository should stop treating `ca.pem` or the mirrored Aiven certificate path as the default database trust story for future work. If the replacement PostgreSQL server later requires SSL trust material, that new trust input should be documented as backend-only and sourced from infrastructure-owned secure storage.

Why this decision:
- The current request replaces the database provider, so the old Aiven certificate path is no longer a reliable default.
- Reusing stale trust-material guidance would create configuration drift and operational confusion.
- The correct TLS story depends on the new server's actual SSL requirements, which are not fully specified yet.

Alternatives considered:
- Keeping the old certificate references "just in case": rejected because it preserves misleading infrastructure guidance.
- Hardcoding a new trust-material path now: rejected because the new server's SSL requirements are still unknown.

### 4. Update both backend-facing notes and user-facing informational copy that still mention the old database story
Implementation should touch repository notes under `infra/` and any product copy that explains the backend-only database boundary so the same message appears consistently across maintained surfaces.

Why this decision:
- The repo currently has both infrastructure docs and settings copy that describe the old backend note differently.
- Consistent wording reduces future mistakes when developers or operators revisit the connection flow.
- This is a small but important guardrail against someone reintroducing Aiven-specific assumptions during later implementation.

Alternatives considered:
- Updating docs only: rejected because stale UI copy would continue to point at the old certificate narrative.
- Updating UI copy only: rejected because the infrastructure guidance is the canonical implementation-facing source.

## Risks / Trade-offs

- [The repository does not yet include a real backend module that consumes the new database inputs] → Scope the change around specs, docs, placeholders, and boundary text so later backend work inherits the correct contract without pretending runtime integration already exists.
- [The user provided host, role, and password but not an explicit database name or SSL mode] → Keep those fields as open deployment inputs in the design and tasks, and avoid overcommitting to a connection string shape before operations confirms the missing details.
- [Host and port are documented but the password must stay secret] → Mention the endpoint as operational context while explicitly forbidding raw secret storage in checked-in artifacts.
- [Legacy Aiven references may remain in archived changes] → Limit active-repo cleanup to non-archived implementation surfaces and allow archives to preserve historical context.

## Migration Plan

1. Update the `core/im-app` delta spec so the protected-infrastructure requirement explicitly supports provider changes through backend-only secret-managed configuration and forbids carrying forward old Aiven-specific assumptions.
2. Replace active repository guidance that still points to the old Aiven certificate or DSN story with new backend-only notes for the replacement PostgreSQL target, while keeping secrets out of tracked files.
3. Refresh maintained user-facing informational copy that explains database boundaries so it matches the new backend note and no longer references stale Aiven certificate paths.
4. Verify that active, non-archived files no longer depend on the old Aiven database narrative except where historical archives intentionally preserve past changes.

## Open Questions

- What database name should backend deployment use against `124.222.15.128:5432`?
- Does the replacement PostgreSQL server require SSL, and if so, what trust chain or client settings should backend deployment use?
- Where will the eventual backend runtime load these secrets from first: local `.env` files, deployment environment variables, or an external secret manager?
