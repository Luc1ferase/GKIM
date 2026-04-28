## 1. Release operator flow

- [x] 1.1 Add a repo-owned Android release entrypoint that blocks release tagging when the worktree is dirty, the target branch head is not fully pushed, or the requested version tag already exists.
- [x] 1.2 Extend the release entrypoint so it pushes the validated release tag and returns the GitHub Actions run context needed to watch the tagged release from the terminal or browser.

## 2. Workflow outcome visibility

- [x] 2.1 Update the Android tag-release workflow to emit operator-facing release summary details for the pushed tag, expected APK asset name, and publication state.
- [x] 2.2 Add final release-asset verification to the Android tag-release workflow so a run only reports success when the expected GitHub Release APK is actually present.

## 3. Documentation and evidence

- [x] 3.1 Update the release/operator docs to describe the supported commit/push/tag/watch flow, GitHub CLI fallback expectations, and how to confirm the final GitHub Release APK asset.
- [x] 3.2 Rehearse the flow with a safe release validation path, then record the verification, review, score, and GitHub upload evidence in `docs/DELIVERY_WORKFLOW.md`.
