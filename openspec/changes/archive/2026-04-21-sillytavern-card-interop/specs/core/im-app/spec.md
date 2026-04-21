## ADDED Requirements

### Requirement: Android tavern exposes an "Import character card" entry point

The system SHALL expose an "Import character card" entry point from the Android tavern surface both through the tavern overflow menu and as a primary CTA on the empty-state of the roster. Tapping the entry point MUST open the system file picker filtered to `.png` and `.json`. Client-side size guards MUST surface an inline error for oversized or unsupported files before the upload begins.

#### Scenario: Overflow menu entry is always reachable

- **WHEN** a user opens the tavern surface
- **THEN** the tavern overflow menu contains an "Import character card" action regardless of roster emptiness

#### Scenario: Empty-state CTA promotes import when roster is sparse

- **WHEN** the user's owned roster has zero or one card
- **THEN** the empty-state or near-empty-state UI surfaces "Import character card" as a prominent CTA alongside the existing "Create new card" CTA

#### Scenario: Client-side size guard rejects oversize files pre-upload

- **WHEN** the user picks a `.png` larger than 8 MiB or a `.json` larger than 1 MiB
- **THEN** the app surfaces an inline error referencing the applicable limit and does not upload the file

### Requirement: Android post-import preview lets users adjust language mapping and edit fields before committing

The system SHALL render an import preview surface populated from the backend's preview response. The surface MUST show the mapped deep persona record with every bilingual field on both language sides, the detected source language with a language picker override, the warnings list, and a summary of the `st.*` extensions preserved. The user MUST be able to edit any field inline before committing. Committing MUST call the backend commit endpoint and, on success, return to the tavern surface with the new card visible in the owned roster.

#### Scenario: Preview shows bilingual fields with language override

- **WHEN** the backend returns a preview for a monolingual ST source
- **THEN** the preview surface displays the imported text on the detected primary side and the mirrored placeholder on the other side, with a language picker that lets the user swap the primary selection

#### Scenario: Preview surfaces warnings without blocking the commit

- **WHEN** the preview carries warnings (truncation, trim, avatar resize, unknown-field parking)
- **THEN** the surface renders each warning in a visible list and still allows the user to commit; warnings do not block the commit action

#### Scenario: Commit routes back to the tavern with the new card visible

- **WHEN** the user taps Commit and the backend persists the card successfully
- **THEN** the app navigates back to the tavern surface, refreshes the owned roster, and the newly imported card is visible and activatable

### Requirement: Android card detail exposes "Export as PNG" and "Export as JSON" actions

The system SHALL expose Export-as-PNG and Export-as-JSON actions from the tavern card-detail surface. Tapping either action MUST open an export dialog that confirms the format, lets the user select the target language (defaulted to the active `AppLanguage`), offers a toggle for including other-language text as `extensions.stTranslationAlt.*` (default off), and lets the user target either the Android share sheet or `DownloadManager`. Invocation MUST call the backend export endpoint and forward the returned payload to the chosen target.

#### Scenario: Export dialog defaults to the active AppLanguage

- **WHEN** the user opens the export dialog for a card
- **THEN** the target-language selector is pre-selected to the user's current active `AppLanguage`, and the other-language-inclusion toggle is off by default

#### Scenario: Share sheet receives the exported payload

- **WHEN** the user selects "Share sheet" as the target and confirms the export
- **THEN** the system share sheet is opened with the exported payload (PNG bytes for PNG, JSON text for JSON) and a reasonable default filename derived from the card's display name

#### Scenario: Downloads target saves the file

- **WHEN** the user selects "Save to Downloads" as the target and confirms the export
- **THEN** the exported payload is written through `DownloadManager` to the user's Downloads directory with a reasonable default filename
