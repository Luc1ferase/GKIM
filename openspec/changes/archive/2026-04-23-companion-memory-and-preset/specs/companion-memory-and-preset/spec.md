## ADDED Requirements

### Requirement: Companion memory is bounded per user-companion pair and composed of a rolling summary plus pinned facts

The system SHALL persist memory state per `(userId, companionCardId)` pair and MUST represent that state as a rolling natural-language summary plus an ordered list of user-curated pinned facts. Memory state MUST persist across reconnect, relaunch, and logout, and MUST NOT be tied to the lifetime of any single conversation. Deleting or resetting memory MUST NOT modify the conversation transcript.

#### Scenario: Memory persists per user-companion pair

- **WHEN** a user interacts with the same companion across multiple sessions
- **THEN** the system retains the same memory record, identified by the `(userId, companionCardId)` pair, and the record survives relaunch and logout

#### Scenario: Memory separates summary prose from pinned facts

- **WHEN** a user retrieves a companion's memory state
- **THEN** the response contains both a rolling summary field (bilingual natural-language prose) and a list of pinned facts (short bilingual snippets), and the two fields are independently addressable

#### Scenario: Resetting memory preserves the transcript

- **WHEN** a user resets any memory scope (pinned, summary, or all)
- **THEN** the underlying conversation history is unchanged and no companion or user message is deleted by the reset

### Requirement: Memory pins are user-curated, bilingual, and addressable

The system SHALL let a user pin any past message from the companion conversation — including variants that are not on the active path — as a long-lived fact, and it MUST let the user create, edit, and delete pins from a memory-management surface independent of the chat timeline. Pins MUST carry a bilingual text body, a creation timestamp, and optionally the `sourceMessageId` of the message they were pinned from. Pins MUST NOT be automatically created by the system in this capability; pin creation is a user action.

#### Scenario: User pins a message from the chat timeline

- **WHEN** a user invokes the pin action on any past message bubble within a companion conversation
- **THEN** the system creates a pin record referencing the bubble's `messageId`, captures its text into the pin's bilingual body, and surfaces the pin in the companion's memory state on the next read

#### Scenario: User creates a pin manually

- **WHEN** a user creates a pin from the memory-management surface without selecting a source message
- **THEN** the system creates a pin with `sourceMessageId` null, the user-entered bilingual text, and a fresh creation timestamp, and surfaces it in the companion's memory state

#### Scenario: User edits or deletes a pin

- **WHEN** a user edits a pin's text or deletes a pin
- **THEN** the memory state reflects the edit or removal immediately on the next read, and prior turns' prompt assembly is not retroactively rewritten

### Requirement: Memory reset has three explicit granularities

The system SHALL expose three distinct memory-reset operations: clear pinned facts only, clear the rolling summary only, and clear everything. Each operation MUST affect only the stated scope and MUST leave all other memory fields and the transcript untouched.

#### Scenario: Clear pinned facts preserves the summary

- **WHEN** a user invokes the "clear pinned facts" reset on a companion
- **THEN** every pinned fact for that companion is removed and the rolling summary, `summaryTurnCursor`, and transcript are unchanged

#### Scenario: Clear summary preserves the pinned facts

- **WHEN** a user invokes the "clear summary" reset on a companion
- **THEN** the rolling summary is wiped and `summaryTurnCursor` is reset to zero, while every pinned fact and the transcript are unchanged

#### Scenario: Clear all memory preserves the transcript

- **WHEN** a user invokes the "clear all memory" reset on a companion
- **THEN** every pinned fact is removed, the rolling summary is wiped, `summaryTurnCursor` is reset to zero, and the transcript is unchanged

### Requirement: Presets bundle prompt-template sections and provider parameters into a named, reusable unit

The system SHALL represent a Preset as a named bundle containing four prompt-template sections (system prefix, system suffix, format instructions, post-history instructions — each bilingual) and three provider parameters (temperature, top-p, max reply tokens — each nullable). The system MUST support user-owned presets that are freely editable and built-in presets that are immutable and undeletable. The system MUST ship at least three built-in presets on first boot, seeded idempotently.

#### Scenario: Preset exposes the four template sections and three parameters

- **WHEN** a client retrieves a preset
- **THEN** the response contains bilingual `systemPrefix`, `systemSuffix`, `formatInstructions`, and `postHistoryInstructions`, plus nullable `temperature`, `topP`, and `maxReplyTokens`, plus a forward-compatible `extensions` bag

#### Scenario: Built-in presets are immutable and undeletable

- **WHEN** a user attempts to edit or delete a preset flagged `isBuiltIn`
- **THEN** the system rejects the mutation with an explicit error and leaves the preset unchanged; the user may duplicate the built-in to produce a user-owned editable copy

#### Scenario: Three built-in presets exist on first launch

- **WHEN** a fresh account boots the app for the first time
- **THEN** the preset library contains at least three built-in presets — a neutral default, a roleplay-immersive preset, and a concise-companion preset — and subsequent boots do not create additional duplicates

### Requirement: Exactly one preset is active per user

The system SHALL maintain exactly one active preset per user at any moment, SHALL expose the active preset id on bootstrap, and MUST reject deletion of the currently active preset. Changing the active preset MUST affect only future companion turn assemblies and MUST NOT rewrite prior conversation history.

#### Scenario: Activation is exclusive

- **WHEN** a user activates a preset while another is already active
- **THEN** the system deactivates the previously active preset, marks the newly selected preset active, and reflects the change on the next bootstrap or active-preset read

#### Scenario: Deleting the active preset is blocked

- **WHEN** a user attempts to delete the currently active preset
- **THEN** the system rejects the request with a typed error indicating the preset is active, and the preset remains intact

#### Scenario: Activating a different preset does not rewrite history

- **WHEN** a user activates a different preset mid-conversation
- **THEN** prior turns in the conversation retain their original assembled prompts and stored replies, and only subsequent turn assemblies incorporate the newly active preset

### Requirement: Companion turn prompts use a deterministic priority-ordered token budget

The system SHALL assemble each companion turn prompt through a deterministic priority-ordered token budget that composes the active preset's template sections, the persona fields from the active card, pinned facts, the rolling summary, the recent-N turns, and the user turn. The allocator MUST preserve the user turn and the pinned facts above all other sections, and it MUST drop lower-priority sections in a fixed, documented order when a provider budget would otherwise be exceeded. If the user turn alone exceeds the budget, the system MUST terminate the turn with `Failed` and reason `prompt_budget_exceeded` rather than silently truncate the user's input.

#### Scenario: Pinned facts are preserved over the rolling summary

- **WHEN** the allocator must drop content to fit the provider budget
- **THEN** it drops lower-priority sections (example dialogue first, then older recent turns, then the rolling summary) before dropping any pinned fact

#### Scenario: Current user turn is never silently truncated

- **WHEN** the current user turn cannot fit the provider budget even after every drop-order step has been applied
- **THEN** the system terminates the turn with a `Failed` terminal and a typed reason `prompt_budget_exceeded`, surfacing the reason to the client rather than truncating the user's input

#### Scenario: Drop order is deterministic and documented

- **WHEN** two different turn assemblies hit the same over-budget condition with identical inputs
- **THEN** they drop the same sections in the same order, producing the same final prompt shape

### Requirement: Rolling summary regenerates on a deterministic trigger without blocking the in-flight turn

The system SHALL regenerate the rolling summary either when the number of completed turns since the last regen reaches a configured threshold, OR when the allocator projects that the budget would otherwise be exceeded. Summary regeneration MUST run asynchronously and MUST NOT block the in-flight companion turn. If summary regeneration fails, the system MUST retain the prior summary and MUST NOT surface the failure as a user-visible error on the turn.

#### Scenario: Turn threshold triggers summary regen

- **WHEN** a companion turn completes and the number of completed turns since the last summary regen reaches the configured threshold
- **THEN** the system enqueues an asynchronous summary regeneration for that `(userId, companionCardId)` pair, the turn completes without waiting, and the next turn's assembly picks up the refreshed summary once it is available

#### Scenario: Budget pressure triggers summary regen

- **WHEN** the allocator projects that the running prompt would exceed a configured soft cap of the provider budget
- **THEN** the system enqueues an asynchronous summary regeneration and the in-flight turn proceeds using the current summary

#### Scenario: Summary regen failure does not regress memory

- **WHEN** an asynchronous summary regeneration fails for any reason
- **THEN** the prior summary remains intact, the failure is logged at warning level, and no user-visible turn error is produced for the in-flight or next turn
