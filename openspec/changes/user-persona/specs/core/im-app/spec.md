## ADDED Requirements

### Requirement: Android Settings exposes persona library management

The system SHALL expose a persona library surface in the Android app's Settings screen that lists every persona (built-in and user-owned), indicates which persona is active with a visible badge, and provides entry points for creating, editing, duplicating, activating, and deleting user-owned personas. The list MUST render each persona's display name in the active `AppLanguage`, and the delete affordance MUST be disabled for built-in personas and for the currently active persona.

#### Scenario: Persona list shows the active persona distinctly

- **WHEN** a user opens the Settings screen and navigates to the Personas section
- **THEN** the list renders every persona with its display name in the active `AppLanguage`, and the active persona carries a visible active-badge that is not present on any other persona

#### Scenario: Built-in and active personas cannot be deleted from the list

- **WHEN** a user attempts to invoke delete on a persona flagged `isBuiltIn` or on the currently active persona
- **THEN** the delete affordance is either absent or disabled, and no mutation is submitted

#### Scenario: Duplicate creates a user-owned editable persona

- **WHEN** a user invokes duplicate on any persona
- **THEN** the app creates a new user-owned persona whose display name carries a bilingual "(copy)" suffix and whose description matches the source, and the new persona is immediately editable through the persona editor

### Requirement: Android persona editor enforces bilingual non-blank display name and description

The system SHALL render a persona editor that edits bilingual `displayName` and bilingual `description`, and it MUST reject a save attempt where either field is blank on either language side. Save MUST call `UserPersonaRepository.update` (for edits) or `UserPersonaRepository.create` (for new personas). Cancel MUST discard changes and return to the persona list.

#### Scenario: Save requires all four text fields to be non-blank

- **WHEN** a user attempts to save a persona with blank display name or blank description on either language side
- **THEN** the editor surfaces a validation error for the blank field and does not submit the save

#### Scenario: Save persists the edit through the repository

- **WHEN** a user saves a valid persona edit
- **THEN** the editor calls `UserPersonaRepository.update` (existing persona) or `UserPersonaRepository.create` (new persona), returns to the list, and the list reflects the edit on its next observed emission

### Requirement: Android companion chat chrome surfaces the active persona alongside the active preset

The system SHALL display the active persona's display name in the companion chat chrome, rendered as a persona pill alongside the active-preset pill. Tapping the persona pill MUST route the user to the Personas section of Settings. The chrome MUST also display a subtle "Talking as {personaName}" footer line under the pills, localized to the active `AppLanguage`.

#### Scenario: Chat chrome shows the active persona name

- **WHEN** a user opens a companion conversation
- **THEN** the chat chrome shows a persona pill with the active persona's display name in the active `AppLanguage`, positioned alongside the active-preset pill

#### Scenario: Persona pill routes to Settings

- **WHEN** a user taps the active-persona pill in the chat chrome
- **THEN** the app navigates to the Personas section of Settings, where the active persona is visible and editable

#### Scenario: Footer line reflects the active persona

- **WHEN** a user opens a companion conversation with a given active persona
- **THEN** the chrome renders a "Talking as {personaName}" footer (localized to the active `AppLanguage`) where `{personaName}` is the active persona's display name in the active `AppLanguage`

### Requirement: Android client-side macro preview uses the active persona and active companion display names

The system SHALL use a shared `MacroSubstitution` helper to expand `{{user}}` / `{user}` / `<user>` and `{{char}}` / `{char}` / `<char>` macros in client-side previews (including the greeting picker and any other chat surface that renders raw template strings). The helper MUST resolve `{{user}}` to the active persona's display name in the active `AppLanguage` and `{{char}}` to the active companion card's display name in the active `AppLanguage`. The helper MUST NOT mutate stored message bodies; only rendered previews reflect the substitution.

#### Scenario: Greeting picker renders substituted text

- **WHEN** a user opens a companion conversation with no history and the greeting picker surfaces templates that contain `{{user}}` or `{{char}}`
- **THEN** the picker's rendered preview shows the substituted display names, while the stored greeting variant (once selected) retains the raw macro tokens

#### Scenario: Unknown macros pass through as literal text

- **WHEN** a template contains a macro-like token that is not one of the six supported forms
- **THEN** the preview renders the token unchanged, and no silent fallback substitution occurs
