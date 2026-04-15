# Infrastructure Notes

## Backend-only database access

Backend services should connect to the replacement PostgreSQL host `124.222.15.128:5432` through secret-managed runtime configuration. The Android client does not consume database credentials or trust inputs and must never package them into the APK.

## Suggested backend env shape

- `PGHOST=124.222.15.128`
- `PGPORT=5432`
- `PGUSER=postgres`
- `PGPASSWORD=<load from untracked .env.local or deployment secret manager>`
- `PGDATABASE=<set the target database name for this environment>`
- `PGSSLMODE=<set only after operations confirms whether SSL is required>`
- `PGSSLROOTCERT=<optional backend-only CA path when the current server requires custom trust material>`
- Optional DSN for server tooling: `DATABASE_URL`

## Secret-handling rules

- Host and port may be documented in tracked files, but passwords and live DSNs must stay in untracked local env files or deployment-managed secrets.
- Backend-only PostgreSQL inputs may be used by future server processes, scripts, or relays; they are not mobile settings.
- If the replacement database later requires TLS trust material, keep that certificate path on the backend side only.

## Android boundary

The Android app only talks to HTTPS and WebSocket endpoints exposed by backend services. Direct PostgreSQL access from the mobile runtime is explicitly out of scope for this scaffold.

### Current published app origin

- `https://chat.lastxuans.sbs/`
- `wss://chat.lastxuans.sbs/ws`
