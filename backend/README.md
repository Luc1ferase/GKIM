# Backend Source Boundary

The Rust IM backend source is no longer published in this repository tip.

This `backend/` directory now exists only as a public placeholder so historical links to
`backend/README.md` still resolve. Backend implementation files, migrations, Docker assets,
systemd units, tests, and deployment scripts are intentionally excluded from tracked files.

## Maintainer handoff

- Keep the backend in a maintainer-controlled private checkout.
- In this workspace, the recommended local-only path is `.private/backend/`.
- `.private/` is ignored by Git and is not meant to be pushed to the public remote.
- Server rollout, smoke tests, and backend development should run from that private/local backend
  checkout, not from the public repository tip.

## Public repository expectations

- The public repo keeps the Android client, OpenSpec artifacts, release automation, and sanitized
  architecture/operations notes.
- Public clones are not sufficient for backend development or backend container builds by
  themselves.
- If backend implementation access is required, recover it from the maintainer's private handoff
  location rather than restoring backend source to the public branch.

## Historical note

Older delivery records may reference tracked `backend/` files from before this boundary change.
Treat those references as historical evidence tied to a maintainer-private backend checkout.
