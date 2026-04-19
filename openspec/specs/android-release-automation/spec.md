# android-release-automation Specification

## Purpose
TBD - created by archiving change build-apk-on-github-tag. Update Purpose after archive.
## Requirements
### Requirement: Version tags trigger Android release APK builds on GitHub
The system SHALL run a GitHub Actions workflow when a supported Android release tag is pushed, and it MUST build the Android client release APK from the repository state referenced by that tag.

#### Scenario: Supported version tag starts the release workflow
- **WHEN** a maintainer pushes a supported release tag such as `v0.2.0`
- **THEN** GitHub Actions starts the Android release workflow and invokes the repository's Android release APK build path against that tagged commit

#### Scenario: Non-release refs do not publish APK releases
- **WHEN** commits are pushed without a supported release tag
- **THEN** the repository does not create or publish a tag-bound APK release artifact automatically

### Requirement: Published APK metadata aligns with the pushed tag
The system SHALL derive release-facing version metadata from the pushed tag, and it MUST make the resulting APK artifact and release record clearly traceable back to that tag.

#### Scenario: Tag version appears in the generated release asset
- **WHEN** the workflow builds an APK from tag `v1.4.3`
- **THEN** the produced APK filename or attached release asset name clearly includes `v1.4.3` so operators can identify the exact shipped version

#### Scenario: Invalid tag format fails before publication
- **WHEN** a maintainer pushes a tag that does not match the supported Android release version format
- **THEN** the workflow stops before publishing the APK and reports that the tag format is invalid for Android release automation

### Requirement: Release signing inputs remain secret-managed in CI
The system SHALL load Android release signing material from GitHub-managed secrets at workflow runtime, and it MUST NOT require a committed keystore, committed passwords, or checked-in signing credentials to produce a release build.

#### Scenario: CI reconstructs signing material from secrets
- **WHEN** the tag workflow starts with the required signing secrets configured
- **THEN** the workflow reconstructs the release signing inputs at runtime and uses them to build a signed release APK without committing those inputs into the repository

#### Scenario: Missing signing secrets block release publication
- **WHEN** the required Android signing secrets are absent or incomplete
- **THEN** the workflow fails with a clear configuration error and does not publish an unsigned asset as though it were a release-ready APK

### Requirement: Successful tag builds publish downloadable APK artifacts
The system SHALL make a successful tag build downloadable from GitHub, and it MUST publish the resulting APK as both a workflow artifact and a tag-bound GitHub Release asset while confirming the final published asset name for operators.

#### Scenario: Successful build uploads workflow artifact and release asset
- **WHEN** the Android release workflow completes successfully for a supported tag
- **THEN** the built APK is uploaded to the workflow run, attached to the GitHub Release associated with that tag, and surfaced to operators under the expected release asset name

#### Scenario: Publication verification fails after build
- **WHEN** the workflow builds an APK but cannot confirm that the expected GitHub Release asset is present under the supported tag
- **THEN** the workflow fails the release publication outcome instead of leaving operators to infer that the APK was published successfully

### Requirement: Release workflow exposes operator-facing outcome context
The system SHALL expose operator-facing release context for each Android tag run, and it MUST summarize the publication outcome clearly enough for maintainers to capture evidence without relying on raw log hunting.

#### Scenario: Release run starts for a supported tag
- **WHEN** GitHub Actions starts the Android tag release workflow for tag `v0.9.0`
- **THEN** the workflow summary or equivalent operator-facing output identifies the release tag and the expected APK asset name for that run

#### Scenario: Release run finishes with a publication blocker
- **WHEN** the Android tag release workflow cannot publish the release-ready APK because signing or release publication prerequisites fail
- **THEN** the workflow summary or equivalent operator-facing output explains that the APK is not yet available as a finished GitHub Release asset

