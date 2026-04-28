## Why

GKIM already has tag-triggered Android release automation, but maintainers still lack one trusted path from accepted local changes to a pushed commit, pushed tag, observable CI/CD run, and a confirmed downloadable APK in GitHub Releases. That gap makes release attempts feel manual and ambiguous, especially when a run fails partway or a tag finishes without a clearly verified asset.

## What Changes

- Add an operator-facing release flow that defines the required preflight checks before a maintainer tags a release, including branch cleanliness, remote sync, and release prerequisites.
- Add a release monitoring and evidence path so operators can follow the GitHub Actions run, understand blocking failures quickly, and confirm when a tagged run has produced a usable APK.
- Tighten the Android tag-release contract so successful runs leave a clearly verifiable GitHub Release outcome instead of forcing operators to infer success from scattered workflow logs.
- Document or script the repo-side steps for commit, push, tag creation, CI/CD observation, and final APK handoff so the process can be repeated safely.

## Capabilities

### New Capabilities
- `android-release-operations`: A repeatable maintainer workflow for preparing, triggering, monitoring, and validating a tagged Android APK release from this repository.

### Modified Capabilities
- `android-release-automation`: Extend the existing tag-driven release behavior with stronger operator-facing status reporting and final asset verification expectations.

## Impact

- Affected code: `.github/workflows/**`, release helper scripts or docs under `scripts/`, `docs/`, and Android/operator guidance in `README.md` or `android/README.md`.
- Affected APIs: GitHub Actions workflow summaries, GitHub Release publication flow, and any repo-local release command surface added for operators.
- Affected dependencies: GitHub-hosted runners, repository secrets used for signing/publication, and any GitHub CLI usage adopted for release monitoring.
- Affected systems: Maintainer release workflow, Android release distribution path, and the audit trail used to confirm that a tagged release produced a usable APK.
