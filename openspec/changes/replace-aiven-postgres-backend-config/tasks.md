## 1. Active repository audit for database-provider assumptions

- [x] 1.1 Inventory active, non-archived files that still reference the previous Aiven PostgreSQL DSN or certificate story, and decide whether each reference should be updated, removed, or preserved only in historical archives.
- [ ] 1.2 Confirm the replacement-database secret-handling contract for active repo surfaces so host and boundary guidance can be documented without committing the supplied password or other live secrets.

## 2. Backend-facing configuration guidance refresh

- [ ] 2.1 Update `infra/` notes and any active configuration placeholders so they describe backend-only connectivity to the replacement PostgreSQL target at `124.222.15.128:5432` through deployment-managed secrets instead of the old Aiven guidance.
- [ ] 2.2 Remove or rewrite active references to `ca.pem` and `aiven-postgres-ca.pem` that imply the previous Aiven trust path is still canonical, while leaving room for future backend-only TLS inputs if the new server requires them.

## 3. Product-copy alignment and verification evidence

- [ ] 3.1 Refresh maintained user-facing backend-database notes, including Android and any still-supported legacy settings surfaces, so they consistently explain that PostgreSQL credentials stay on the backend and are not packaged into the client.
- [ ] 3.2 Run focused verification for the updated notes and a repo-wide search of active files to confirm the old Aiven-specific database narrative is gone where intended, then record the evidence in the delivery workflow format.
