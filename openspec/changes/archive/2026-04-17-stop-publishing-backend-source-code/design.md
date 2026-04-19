## Context

The current repository publishes the Rust backend source tree and backend deployment assets alongside the Android client and OpenSpec artifacts. Existing backend specs also assume that the repository itself provides the scripts and file layout needed for Ubuntu deployment. The user now wants the backend implementation removed from the public Git remote while keeping it available locally for continued deployment and maintenance.

This is a security and distribution-boundary change, not a backend runtime redesign. The implementation needs to stop future publication of backend source at the public repository tip without losing the current backend code during the transition. It also needs to avoid leaving the public repo in a broken state after the tracked backend files disappear.

## Goals / Non-Goals

**Goals:**
- Preserve the current backend source in a maintainer-controlled local/private location before removing tracked backend files from the public repository.
- Remove backend implementation and backend-only operational assets from the public repository tip.
- Replace public assumptions about checked-in backend code with sanitized guidance that points to local/private backend materials.
- Keep the published Android/client-facing repository internally consistent after backend removal.

**Non-Goals:**
- Rewrite existing Git history, tags, or GitHub archives that may already contain backend source.
- Redesign the backend runtime, APIs, or the current Ubuntu deployment target.
- Make the public repository sufficient for backend development without the private/local backend handoff.

## Decisions

### 1. Treat the public repository tip as the publication boundary

The implementation will remove tracked backend implementation files from the public branch tip instead of relying on policy alone.

Why:
- This stops future publication through normal branch and tag pushes.
- It gives reviewers a concrete tracked diff that proves backend materials are no longer part of the public tree.

Alternatives considered:
- Keep the files tracked and rely on maintainers not to push them: rejected because one accidental push would republish the backend.
- Make the whole repository private: rejected because the user asked to stop publishing the backend, not to hide the Android/client repository entirely.

### 2. Preserve backend source in a local-only or otherwise private handoff location before tracked removal

Before removing tracked backend files, the backend tree will be copied or moved into a maintainer-controlled location that is ignored by Git, such as a repo-local private directory or a separate private checkout.

Why:
- The user explicitly wants to keep the backend locally.
- It creates a rollback path if the public cleanup removes files that are still operationally needed.

Alternatives considered:
- `git update-index --skip-worktree` or similar local flags: rejected because they are fragile and do not create a durable private handoff path.
- Immediate migration to a private submodule or a new private repository as part of the same change: rejected because it adds infrastructure complexity beyond the minimum needed to stop publication now.

### 3. Replace public backend references with sanitized guidance instead of leaving broken paths

Any tracked public documentation, scripts, or workflows that currently assume the presence of `backend/` will be updated so the public repository remains internally coherent after backend removal.

Why:
- Fresh public clones should not contain dead instructions that depend on files intentionally removed from version control.
- Public documentation still needs to explain the boundary without exposing the private backend implementation.

Alternatives considered:
- Leave existing references in place and rely on maintainers to know the repo changed: rejected because it would make the public repo misleading and harder to maintain.

### 4. Keep the backend deployment contract, but scope it to private/local materials

The `im-backend` deployment requirement will continue to require a repeatable server rollout and verification path, but it will no longer require the published remote Git repository itself to carry backend source files.

Why:
- The deployed service still exists and still needs operator verification.
- The operational contract should survive even after the public repo stops carrying the backend implementation.

Alternatives considered:
- Remove the backend deployment requirement entirely: rejected because the backend service is still a live product dependency.

## Risks / Trade-offs

- Existing remote history may still expose backend source. -> Mitigation: treat history rewrite and remote cleanup as a separate follow-up if stricter secrecy is required.
- A local-only backend copy can be lost if the transition is done carelessly. -> Mitigation: verify the private/local copy before deleting tracked files and keep a rollback path.
- Public docs or scripts may still reference removed backend paths. -> Mitigation: inventory references before deletion and run focused public-repo verification after cleanup.

## Migration Plan

1. Inventory tracked backend files and the public docs/workflows that depend on them.
2. Copy or move the backend tree into a maintainer-controlled local/private location that Git will not publish.
3. Update ignore rules and public guidance for the new boundary.
4. Remove the tracked backend files from the public repository tip.
5. Verify that the public repo no longer tracks backend source while the private/local copy remains available for backend work.

Rollback:
- Restore the tracked backend tree from the preserved local/private copy if the cleanup is aborted before the public push.
- If a public push breaks required docs or workflows, reintroduce the necessary sanitized tracked assets without republishing backend source.

## Open Questions

- Does the user also want to purge existing remote history, release assets, or cached archives that may already contain backend source?
- Should the long-term private backend home be a Git-ignored directory inside this workspace or a separate private repository?
