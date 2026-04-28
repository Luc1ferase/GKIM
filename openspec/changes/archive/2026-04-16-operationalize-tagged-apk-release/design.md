## Context

The repository already ships a tag-driven Android release workflow in [`.github/workflows/android-tag-release.yml`](x:/Repos/GKIM/.github/workflows/android-tag-release.yml), and Android release expectations are partially documented in [`android/README.md`](x:/Repos/GKIM/android/README.md). What is still missing is the operator layer around that automation: maintainers do not have one repo-owned release path that verifies the branch is ready, pushes the required refs, monitors the workflow run, and confirms that the GitHub Release ended with the expected APK asset.

This change is cross-cutting because it touches release automation, operator documentation, and the acceptance evidence path in [`docs/DELIVERY_WORKFLOW.md`](x:/Repos/GKIM/docs/DELIVERY_WORKFLOW.md). It also has an operational safety angle: the repo must not encourage unsafe shortcuts such as tagging from a dirty worktree, inferring success from partial workflow logs, or treating an unsigned rehearsal as a finished release.

## Goals / Non-Goals

**Goals:**
- Provide one repeatable maintainer workflow for going from an accepted repo state to a pushed Android release tag.
- Make the GitHub Actions run observable enough that an operator can quickly tell whether the release is still running, blocked, or finished.
- Guarantee that a successful release is validated against the expected GitHub Release APK asset name instead of relying on manual guesswork.
- Keep signing material secret-managed and avoid any design that requires committed credentials or local keystores.

**Non-Goals:**
- Replacing the existing tag-driven Android build/sign workflow with a new release system.
- Auto-generating commit messages or silently committing work on behalf of the maintainer.
- Introducing Play Store deployment, AAB publishing, or multi-platform mobile release orchestration.
- Relaxing the existing delivery gate that requires verification, review, scoring, evidence, commit, and push before task completion.

## Decisions

### Decision: Add a repo-owned release operator entrypoint instead of relying on doc-only steps

The implementation should add a repo-local release helper for maintainers, paired with concise operator docs. The helper should validate release preconditions, create or validate the requested tag, push the branch/tag, and surface the GitHub run information needed for follow-up monitoring.

Why:
- The existing workflow is already automated in GitHub; the missing failure mode is operator inconsistency.
- A repo-owned entrypoint can codify the correct order of operations and reduce "did I already push/tag/watch the right thing?" ambiguity.

Alternatives considered:
- Docs only: simpler, but keeps the highest-risk steps manual and easy to forget.
- Fully automatic commit+push+tag from any dirty worktree: faster, but too risky and in tension with the repo's existing review-and-upload gate.

### Decision: Treat GitHub Actions as the source of truth for build/publication, but add explicit outcome reporting

The existing workflow should remain the system of record for building and publishing the APK. This change should add or tighten operator-facing outputs such as job summaries, run URLs/IDs, and a final post-publication verification step that checks the expected release asset exists under the expected name.

Why:
- The current workflow already owns version derivation, signing, build, and release upload.
- Operators need clearer success/failure signals than raw step logs, especially when a run completes but the release asset is missing or misnamed.

Alternatives considered:
- Move publication verification entirely into local scripts: would duplicate release-state logic and make local credentials a hidden requirement.
- Rely on the existing artifact/release upload steps without verification: leaves the exact failure the user is trying to eliminate.

### Decision: Use GitHub CLI-compatible monitoring, with docs as the fallback

The operator flow should prefer GitHub CLI-based monitoring (`gh run view/watch`, `gh release view`) when available, because it gives a stable machine-facing interface for watching a run and checking release assets. The docs must still describe a browser fallback so the release process remains usable without new mandatory local dependencies.

Why:
- `gh` matches the repository's GitHub-centered workflow and keeps monitoring close to the terminal-driven maintainer flow.
- A fallback path avoids blocking operators who do not have GitHub CLI configured yet.

Alternatives considered:
- Raw GitHub REST calls: more brittle and harder to explain.
- Browser-only instructions: workable, but much weaker for repeatability and evidence capture.

### Decision: Model unsigned builds as rehearsals, not successful releases

The operator workflow and release verification logic should keep the current distinction between a compilation rehearsal and a ship-ready signed release. If signing secrets are missing or incomplete, the release must remain blocked and the monitoring flow must surface that as a publication failure, not a success.

Why:
- The repository already differentiates between "APK compiled" and "APK published as release-ready asset."
- Blurring that line would make the final release outcome less trustworthy.

Alternatives considered:
- Treat unsigned APK artifacts as equivalent to a release: faster feedback, but directly conflicts with the user's need for a usable downloadable APK in Releases.

## Risks / Trade-offs

- [GitHub CLI is not installed or authenticated locally] -> Keep browser-based monitoring instructions as a supported fallback and avoid making `gh` the only viable path.
- [Tagging the wrong commit or a stale remote branch] -> Make preflight checks block tagging when the worktree is dirty or the branch is not fully pushed.
- [GitHub Release asset publication is eventually consistent] -> Add a bounded verification/retry window before declaring the release asset missing.
- [Automation becomes Windows-only] -> Keep the release contract independent of shell choice; if a PowerShell helper is added first, document the underlying git/gh commands clearly enough to port later.

## Migration Plan

1. Add the operator-facing release contract in specs, then implement the helper/docs/workflow summary changes behind the existing tag-based release path.
2. Rehearse the flow with a disposable version tag that exercises branch push, tag push, run monitoring, and final release-asset verification.
3. Record the verification and upload evidence in [`docs/DELIVERY_WORKFLOW.md`](x:/Repos/GKIM/docs/DELIVERY_WORKFLOW.md).
4. Roll back by removing the helper/docs/workflow summary additions if they prove unreliable; the underlying archived tag-release workflow remains the fallback baseline.

## Open Questions

- Should the first implementation ship a PowerShell-focused helper only, or should it include a minimal cross-platform shell equivalent from day one?
- Should release monitoring stop at surfacing the run URL and asset checks, or should it also emit a machine-readable evidence file that can be attached to delivery records?
