# user-persona Specification

## Purpose
TBD - created by archiving change user-persona. Update Purpose after archive.
## Requirements
### Requirement: User persona is a bilingual, server-persisted identity bundle

The system SHALL persist a library of user personas per account, with each persona carrying a bilingual display name, a bilingual description, an `isBuiltIn` flag, an `isActive` flag, creation and update timestamps, and a forward-compatible `extensions` bag. Persona display name and description MUST NOT be blank on either language side. Persona state MUST persist across reconnect, relaunch, and logout.

#### Scenario: Persona state is read and written bilingually

- **WHEN** a client reads a persona record
- **THEN** the record contains bilingual `displayName` and `description` fields, and both sides contain non-blank text

#### Scenario: Persona state survives reconnect and relaunch

- **WHEN** the realtime gateway disconnects, the client relaunches, or the backend restarts
- **THEN** a subsequent persona read returns the same library state produced before the interruption, with no persona dropped and no field reset

### Requirement: Persona library exposes create, update, duplicate, activate, and delete

The system SHALL expose CRUD operations on user personas: create, update, duplicate, activate, and delete. Duplicate MUST return a new user-owned persona with a bilingual "(copy)" suffix on the display name. Delete MUST be rejected for built-in personas and for the currently active persona.

#### Scenario: Duplicate produces a user-owned copy with renamed display name

- **WHEN** a user duplicates any persona (built-in or user-owned)
- **THEN** the system creates a new user-owned persona whose description matches the source and whose display name carries a bilingual "(copy)" suffix, and the new persona is immediately editable

#### Scenario: Deleting a built-in persona is rejected

- **WHEN** a user attempts to delete a persona flagged `isBuiltIn`
- **THEN** the system rejects the request with a typed error indicating the persona is built-in, and the persona record remains unchanged

#### Scenario: Deleting the active persona is rejected

- **WHEN** a user attempts to delete the currently active persona
- **THEN** the system rejects the request with a typed error indicating the persona is active, and the persona record remains unchanged; the user must activate a different persona first

### Requirement: Exactly one persona is active per user

The system SHALL maintain exactly one active persona per user at any moment, SHALL expose the active persona id on bootstrap, and MUST enforce activation exclusivity atomically. Changing the active persona MUST affect only future companion turn assemblies and MUST NOT rewrite prior conversation history or stored message bodies.

#### Scenario: Activation is atomic and exclusive

- **WHEN** a user activates a persona id that is not currently active
- **THEN** the system deactivates the previously active persona, marks the newly selected persona active, and returns the updated active-persona id, all as a single atomic operation

#### Scenario: Bootstrap returns the active persona id

- **WHEN** an authenticated client bootstraps a session
- **THEN** the bootstrap response exposes the active persona record (or a reference that lets the client fetch it with one subsequent read), so the UI can label the chat chrome accordingly

#### Scenario: Activating a different persona does not rewrite history

- **WHEN** a user activates a different persona mid-conversation
- **THEN** prior stored message bodies in that conversation retain their original text (macros stored as raw, substituted values stored as substituted), and only subsequent turn assemblies incorporate the newly active persona's display name and description

### Requirement: Built-in persona is seeded on first bootstrap and remains editable but undeletable

The system SHALL create a built-in persona for an account on first bootstrap, seeding `displayName` from the account's `displayName` and `description` from a neutral placeholder, and flagging the persona `isBuiltIn=true` and `isActive=true`. The built-in persona MUST be editable by the user (display name and description can be rewritten), but MUST NOT be deletable. Subsequent bootstraps MUST NOT duplicate the built-in persona.

#### Scenario: Built-in persona exists on first launch

- **WHEN** a fresh account completes its first bootstrap
- **THEN** the persona library contains exactly one persona flagged `isBuiltIn=true` and `isActive=true`, whose `displayName` matches the account's `displayName` (filled on both language sides) and whose `description` contains a neutral placeholder

#### Scenario: Built-in persona is editable

- **WHEN** a user edits the built-in persona's display name or description
- **THEN** the system accepts the edit, persists the new content, and preserves the `isBuiltIn` flag

#### Scenario: Built-in persona is undeletable

- **WHEN** a user attempts to delete the built-in persona
- **THEN** the system rejects the request with a typed error indicating the persona is built-in

### Requirement: Macro substitution resolves `{{user}}` to the active persona's display name and `{{char}}` to the active companion's display name

The system SHALL substitute every occurrence of `{{user}}`, `{user}`, and `<user>` in assembled prompts and in client-side preview strings with the active persona's `displayName` in the active `AppLanguage`, and every occurrence of `{{char}}`, `{char}`, and `<char>` with the active companion card's `displayName` in the active `AppLanguage`. The substitution MUST happen before the provider call for backend assembly, and MUST happen only on the rendered copy (not on stored message bodies) for client-side previews. Unknown macro-like tokens MUST be left as literal text.

#### Scenario: Six macro forms substitute correctly

- **WHEN** a template contains `{{user}}`, `{user}`, `<user>`, `{{char}}`, `{char}`, or `<char>`
- **THEN** the system replaces each occurrence with the resolved display name (active persona for user, active card for char) in the active `AppLanguage` before sending to the provider (backend) or before rendering to the screen (client)

#### Scenario: Stored message bodies are never rewritten by substitution

- **WHEN** a stored message body containing macros is rendered in a client preview
- **THEN** the substitution produces only the rendered string; the underlying stored record retains the raw macro tokens unchanged

#### Scenario: Unknown macros pass through as literal text

- **WHEN** a template contains a macro-like token that is not one of the six known forms
- **THEN** the system leaves the token unchanged in both the assembled prompt and the rendered preview

### Requirement: Active persona's description participates in the deterministic token budget

The system SHALL include the active persona's `description` as a dedicated section (`userPersonaDescription`) in the deterministic token-budget allocator introduced by `companion-memory-and-preset`. The section's priority MUST be higher than the rolling summary and lower than pinned facts. The section's drop position MUST sit between the rolling summary and non-critical preset sections. When the section is dropped, the `{{user}}` macro substitution MUST continue to resolve to the active persona's display name.

#### Scenario: Persona description occupies its documented priority slot

- **WHEN** the backend assembles a companion turn prompt with the active persona having a non-blank description and the allocator is not over budget
- **THEN** the prompt contains the persona description in a dedicated section whose priority sits between pinned facts (above) and the rolling summary (below)

#### Scenario: Persona description drops under budget pressure without dropping name substitution

- **WHEN** the allocator must drop sections to fit the provider budget and the drop order reaches `userPersonaDescription`
- **THEN** the persona description section is removed from the assembled prompt, and the `{{user}}` macro substitution continues to resolve to the active persona's display name regardless

#### Scenario: Drop order preserves pinned facts above persona description

- **WHEN** the allocator must drop sections to fit the provider budget
- **THEN** the allocator drops `userPersonaDescription` before any pinned fact is dropped and before the persona `systemPrompt` or the preset `systemPrefix` is dropped

