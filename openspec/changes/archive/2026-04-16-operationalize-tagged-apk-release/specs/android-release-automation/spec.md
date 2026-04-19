## ADDED Requirements

### Requirement: Release workflow exposes operator-facing outcome context
The system SHALL expose operator-facing release context for each Android tag run, and it MUST summarize the publication outcome clearly enough for maintainers to capture evidence without relying on raw log hunting.

#### Scenario: Release run starts for a supported tag
- **WHEN** GitHub Actions starts the Android tag release workflow for tag `v0.9.0`
- **THEN** the workflow summary or equivalent operator-facing output identifies the release tag and the expected APK asset name for that run

#### Scenario: Release run finishes with a publication blocker
- **WHEN** the Android tag release workflow cannot publish the release-ready APK because signing or release publication prerequisites fail
- **THEN** the workflow summary or equivalent operator-facing output explains that the APK is not yet available as a finished GitHub Release asset

## MODIFIED Requirements

### Requirement: Successful tag builds publish downloadable APK artifacts
The system SHALL make a successful tag build downloadable from GitHub, and it MUST publish the resulting APK as both a workflow artifact and a tag-bound GitHub Release asset while confirming the final published asset name for operators.

#### Scenario: Successful build uploads workflow artifact and release asset
- **WHEN** the Android release workflow completes successfully for a supported tag
- **THEN** the built APK is uploaded to the workflow run, attached to the GitHub Release associated with that tag, and surfaced to operators under the expected release asset name

#### Scenario: Publication verification fails after build
- **WHEN** the workflow builds an APK but cannot confirm that the expected GitHub Release asset is present under the supported tag
- **THEN** the workflow fails the release publication outcome instead of leaving operators to infer that the APK was published successfully
