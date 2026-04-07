# Infrastructure Notes

## Backend-only database access

The PostgreSQL DSN and CA certificate are infrastructure inputs for a future backend service. They are not consumed by the Android client and must never be packaged into the APK.

## Canonical paths

- Database URL environment variable: `DATABASE_URL`
- PostgreSQL CA certificate: `infra/certs/postgres-ca.pem`
- Suggested server SSL env var: `PGSSLROOTCERT=infra/certs/postgres-ca.pem`

## Android boundary

The Android app only talks to HTTPS and WebSocket endpoints exposed by backend services. Direct PostgreSQL access from the mobile runtime is explicitly out of scope for this scaffold.
