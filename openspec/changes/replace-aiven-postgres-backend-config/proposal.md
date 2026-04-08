## Why

The repository still documents the previous Aiven-flavored PostgreSQL assumptions even though operations now needs backend infrastructure to connect to a different PostgreSQL instance at `124.222.15.128:5432`. We need to update the contract so the new database target is reflected in backend-facing configuration guidance without ever embedding raw database credentials or trust material into the Android client or version-controlled product specs.

## What Changes

- Replace Aiven-specific PostgreSQL guidance with backend-only configuration guidance for the new Postgres host and port.
- Clarify that database username, password, and any future TLS trust material are deployment secrets loaded from secure environment inputs rather than written into checked-in OpenSpec artifacts, app code, or mobile assets.
- Update infrastructure notes and user-facing backend notices so they describe the new Postgres source while preserving the existing Android service-boundary rule.
- Remove prior assumptions that the old `ca.pem` and mirrored Aiven certificate path remain the canonical database inputs when switching to the new operations-provided database target.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: refine the protected-infrastructure requirement so backend-side PostgreSQL configuration is provider-agnostic, secret-managed, and no longer coupled to the previous Aiven certificate guidance while Android continues to consume only HTTPS and WebSocket services.

## Impact

- Affected code and docs: backend infrastructure notes under `infra/`, settings copy that still references the old database note, and any future environment or deployment placeholders that currently imply Aiven-specific trust material.
- Affected systems: backend deployment configuration for the new PostgreSQL host `124.222.15.128:5432`; Android client behavior and service boundaries remain unchanged.
- Affected dependencies: the previous Aiven certificate assumptions may be removed or replaced; if the new server later requires TLS trust material, that trust input must still live outside version control and outside the APK.
