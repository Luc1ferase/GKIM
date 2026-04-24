# Backend Source Boundary

The Rust IM backend source lives in its own repository and is **not** tracked in
this GKIM client repository.

This `backend/` directory remains only as a public placeholder so historical
links to `backend/README.md` still resolve. Backend implementation files,
migrations, Docker assets, systemd units, tests, and deployment scripts are
intentionally excluded from the tracked files here.

## Canonical backend location

- Git remote: `https://github.com/Luc1ferase/GKIM-Backend.git`
- Recommended local checkout path: a sibling directory next to this `GKIM`
  checkout (on the maintainer's workstation, `X:\Repos\GKIM-Backend`).
- Server rollout, smoke tests, and backend development should run from that
  sibling backend checkout, not from this repository tip.

## Public repository expectations

- This repo keeps the Android client, OpenSpec artifacts, release automation,
  and sanitized architecture / operations notes.
- Cloning this repo alone is not sufficient for backend development or backend
  container builds — also clone the backend repo above.
- If backend implementation access is required, pull the backend repo rather
  than restoring backend source under this tree.

## Historical note

Older delivery records in `docs/DELIVERY_WORKFLOW.md` and earlier archive slices
may reference tracked `backend/` files, or a local-only `.private/backend/`
preservation copy used before the backend was extracted into its own public
repository. Treat those references as historical evidence tied to the earlier
layout; the canonical backend source is now the sibling backend repo at the
remote above.
