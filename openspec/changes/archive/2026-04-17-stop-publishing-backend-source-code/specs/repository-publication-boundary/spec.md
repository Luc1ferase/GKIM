## ADDED Requirements

### Requirement: Public repository excludes backend implementation materials
The system SHALL keep backend source code, backend deployment scripts, backend environment templates, and backend-only operational assets out of the published remote Git repository, and it MUST allow maintainers to retain those materials in local-only or otherwise private storage.

#### Scenario: Maintainer prepares a public push
- **WHEN** a maintainer reviews the tracked changes intended for the public remote repository
- **THEN** backend implementation paths and backend-only operational assets are absent from the tracked diff and are not published with that push

#### Scenario: Backend must remain available for private operations
- **WHEN** the public repository tip no longer carries backend source files
- **THEN** maintainers still have a documented local/private preservation path that keeps backend development and server deployment possible without restoring those files to the public remote

### Requirement: Public repository guidance remains coherent after backend removal
The system SHALL keep the public repository internally consistent after backend source removal, and it MUST replace public references that require checked-in backend code with sanitized guidance to private/local backend materials.

#### Scenario: Fresh public clone is inspected
- **WHEN** a maintainer or reviewer clones the published repository after backend removal
- **THEN** the tracked docs, scripts, and workflows that remain in the public tree do not assume that `backend/` source files are present

#### Scenario: Operator needs backend-related guidance
- **WHEN** an operator reads the public repository for backend-related context
- **THEN** the repository points to the private/local handoff model instead of exposing backend implementation files or private deployment assets
