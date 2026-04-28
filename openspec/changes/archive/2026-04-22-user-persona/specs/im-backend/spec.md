## ADDED Requirements

### Requirement: Backend persists user persona library and exposes CRUD endpoints

The system SHALL persist a user persona library per account server-side and MUST expose authenticated HTTP endpoints to list, create, update, duplicate, activate, and delete personas. Persona records MUST carry bilingual `displayName`, bilingual `description`, `isBuiltIn`, `isActive`, creation and update timestamps, and a forward-compatible `extensions` JSON object. Persona state MUST survive realtime gateway reconnect, backend restart, and client relaunch.

#### Scenario: Backend returns the persona library on authenticated read

- **WHEN** an authenticated client requests the persona library
- **THEN** the backend returns every persona owned by the account with `id`, bilingual `displayName`, bilingual `description`, `isBuiltIn`, `isActive`, timestamps, and `extensions`

#### Scenario: Persona CRUD operations round-trip through the backend

- **WHEN** an authenticated client creates, updates, duplicates, or deletes a persona
- **THEN** the backend persists the change, returns the updated record (or a success ack on delete), and subsequent list reads reflect the change

#### Scenario: Persona state survives reconnect and restart

- **WHEN** the realtime gateway disconnects, the client relaunches, or the backend process restarts
- **THEN** a subsequent persona library read returns the same records produced before the interruption, with no persona dropped and no field reset

### Requirement: Backend enforces exactly one active persona per user and blocks deletion of built-in or active personas

The system SHALL persist exactly one active persona per user, MUST expose the active persona id on bootstrap, and MUST enforce activation exclusivity atomically. The backend MUST reject deletion of a persona flagged `isBuiltIn` with a typed error, and MUST reject deletion of the currently active persona with a typed error. Deletion of an inactive user-owned persona MUST succeed.

#### Scenario: Activation is atomic and exclusive

- **WHEN** an authenticated client activates a persona id that is not currently active
- **THEN** the backend deactivates the previously active persona, marks the newly selected persona active, and returns the updated active persona id, all as a single atomic operation

#### Scenario: Bootstrap response exposes the active persona

- **WHEN** an authenticated client reads bootstrap state
- **THEN** the backend returns the active persona id (and optionally its record inline), so the client can label the chat chrome without an additional round trip

#### Scenario: Deleting a built-in persona is rejected

- **WHEN** an authenticated client attempts to delete a persona flagged `isBuiltIn`
- **THEN** the backend rejects the request with a typed error indicating the persona is built-in, and the record remains unchanged

#### Scenario: Deleting the active persona is rejected

- **WHEN** an authenticated client attempts to delete the currently active persona
- **THEN** the backend rejects the request with a typed error indicating the persona is active, and the record remains unchanged

### Requirement: Backend seeds a built-in persona on first bootstrap

The system SHALL create a built-in persona on first bootstrap for each account, seeding bilingual `displayName` from the account's `displayName` (both language sides initially identical) and bilingual `description` from a neutral placeholder. The seeded persona MUST have `isBuiltIn=true` and `isActive=true`. Re-bootstrapping MUST NOT create duplicates.

#### Scenario: First bootstrap seeds the built-in persona

- **WHEN** an account completes its first bootstrap
- **THEN** the persona library contains exactly one persona flagged `isBuiltIn=true` and `isActive=true`, whose bilingual `displayName` matches the account's `displayName` and whose bilingual `description` contains a neutral placeholder

#### Scenario: Subsequent bootstraps do not duplicate the built-in

- **WHEN** an already-bootstrapped account reconnects or restarts
- **THEN** the persona library still contains exactly one persona flagged `isBuiltIn=true`, and no duplicate built-in personas exist

### Requirement: Backend substitutes user and char macros in every assembled prompt

The system SHALL replace every occurrence of `{{user}}`, `{user}`, and `<user>` in an assembled companion turn prompt with the active persona's `displayName` in the active `AppLanguage`, and every occurrence of `{{char}}`, `{char}`, and `<char>` with the active companion card's `displayName` in the active `AppLanguage`. Substitution MUST happen before the prompt is sent to the provider. Stored message bodies MUST NOT be rewritten by this substitution step. Unknown macro-like tokens MUST be left as literal text.

#### Scenario: Assembled prompt substitutes user and char macros in all six forms

- **WHEN** the backend assembles a companion turn prompt containing any of the six macro forms
- **THEN** each macro occurrence is replaced with the active persona's display name (for user) or the active card's display name (for char) in the active `AppLanguage`, and the provider never receives a raw macro token of these six forms

#### Scenario: Stored message bodies retain raw macros

- **WHEN** a companion turn completes and the assembled prompt body that was sent to the provider contained substituted display names
- **THEN** the stored user message body and the stored companion reply body retain the raw macro tokens (if the user had included them); the substitution produced only the ephemeral prompt sent to the provider

#### Scenario: Unknown macros pass through

- **WHEN** an assembled prompt contains a macro-like token that is not one of the six supported forms
- **THEN** the backend leaves the token unchanged when sending to the provider

### Requirement: Backend injects the active persona description into the prompt allocator with documented priority and drop position

The system SHALL include the active persona's `description` as a dedicated section (`userPersonaDescription`) in the deterministic token-budget allocator introduced by `companion-memory-and-preset`. The section's priority MUST place it above the rolling summary and below pinned facts. The drop order MUST place it between the rolling summary and the non-critical preset sections (`formatInstructions`, `systemSuffix`). The `{{user}}` substitution MUST continue to resolve correctly even when the description section has been dropped.

#### Scenario: Persona description is included when budget allows

- **WHEN** the backend assembles a companion turn prompt with the active persona having a non-blank description and the allocator is not over budget
- **THEN** the prompt contains the `userPersonaDescription` section between the pinned facts (above) and the rolling summary (below)

#### Scenario: Persona description is dropped before non-critical preset sections

- **WHEN** the allocator must drop sections to fit the provider budget
- **THEN** the allocator drops `userPersonaDescription` after the rolling summary has already been dropped and before any non-critical preset section (`formatInstructions`, `systemSuffix`) is dropped

#### Scenario: User display name substitution survives description drop

- **WHEN** the allocator has dropped `userPersonaDescription` for budget reasons
- **THEN** the `{{user}}` substitution continues to resolve to the active persona's display name in the active `AppLanguage`, and the provider still receives the substituted name in any remaining section (including the preserved user turn)
