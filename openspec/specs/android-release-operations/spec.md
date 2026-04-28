# android-release-operations Specification

## Purpose
TBD - created by archiving change operationalize-tagged-apk-release. Update Purpose after archive.
## Requirements
### Requirement: Release preflight blocks unsafe tag operations
The system SHALL require a release preflight before pushing an Android release tag, and it MUST block the tagged release flow when the repository state is not safe to release.

#### Scenario: Worktree is not clean
- **WHEN** a maintainer starts the repo-owned Android release flow with uncommitted or untracked changes
- **THEN** the flow stops before any release tag is created or pushed and reports that release tagging must start from a committed worktree

#### Scenario: Branch head is not fully pushed
- **WHEN** the local release branch is ahead of its configured remote or the intended release commit is not yet available on GitHub
- **THEN** the flow stops before pushing the release tag and tells the maintainer to push or reconcile the branch first

#### Scenario: Requested release tag already exists
- **WHEN** the maintainer requests a version tag that already exists locally or on the remote
- **THEN** the flow blocks the release attempt and reports the conflicting tag instead of silently retagging a release

### Requirement: Operators can monitor the tagged release run from the release flow
The system SHALL surface the GitHub Actions run associated with a pushed Android release tag, and it MUST let the maintainer distinguish running, failed, and successful states without manually searching the Actions UI.

#### Scenario: Tag push starts a release run
- **WHEN** the release flow successfully pushes a supported Android release tag
- **THEN** it returns enough run context for the maintainer to open or watch the matching GitHub Actions run directly

#### Scenario: Release run fails
- **WHEN** the GitHub Actions run for the pushed release tag finishes in a failed state
- **THEN** the release flow reports that failure as a blocker and surfaces the workflow context needed to inspect the failing run

### Requirement: Release completion requires final APK asset verification
The system SHALL treat an Android release as complete only after the expected APK asset is confirmed on the tag-bound GitHub Release, and it MUST identify missing or mismatched assets as release failures.

#### Scenario: Expected APK asset is present
- **WHEN** the release workflow succeeds for tag `v1.2.3`
- **THEN** the release flow verifies that the GitHub Release for `v1.2.3` includes the downloadable asset `gkim-android-v1.2.3.apk` before reporting success

#### Scenario: Release asset is missing after workflow completion
- **WHEN** the workflow run completes but the expected GitHub Release APK asset cannot be confirmed
- **THEN** the release flow reports that the release is incomplete and does not claim that a usable APK is available

