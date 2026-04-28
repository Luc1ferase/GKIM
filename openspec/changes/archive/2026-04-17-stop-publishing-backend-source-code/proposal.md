## Why

The published Git remote currently carries the Rust backend source and backend-only deployment materials, but that implementation is no longer intended to remain public. We need a focused change that removes backend code from the public repository tip while preserving a local/private copy so backend maintenance and server deployment can continue.

## What Changes

- **BREAKING** Remove tracked backend source files, backend deployment scripts, and backend-only operational assets from the published remote repository tip.
- Add a repository publication boundary that defines which backend materials must stay out of the public Git remote and how maintainers preserve them locally or privately.
- Update backend deployment expectations so server operations can rely on local/private backend materials instead of assuming the public repository always contains `backend/`.
- Refresh public docs, workflows, and operator guidance that currently assume checked-in backend source is available in a public clone.

## Capabilities

### New Capabilities
- `repository-publication-boundary`: Defines how the public repository excludes backend implementation materials while maintainers retain a local/private backend handoff path.

### Modified Capabilities
- `im-backend`: The remote deployment/debug requirement changes from a public-repo-backed backend checkout to a local/private backend delivery workflow.

## Impact

- Affected code: the tracked `backend/` tree, `.gitignore`, and any public docs/scripts/workflows that reference checked-in backend files.
- Affected specs: new `repository-publication-boundary` and modified `im-backend`.
- Affected systems: the public Git remote, local maintainer worktrees, and the backend deployment workflow for the Ubuntu host.
- Affected dependencies: Git tracking rules, local/private backend storage, and operator documentation for backend deployment and recovery.
