## ADDED Requirements

### Requirement: Android Settings exposes preset library management

The system SHALL expose a preset library surface in the Android app's Settings screen that lists every preset (built-in and user-owned), indicates which preset is active with a visible badge, and provides entry points for creating, editing, duplicating, activating, and deleting user-owned presets. The list MUST render each preset's localized display name in the active `AppLanguage`, and the delete affordance MUST be disabled for built-in presets and for the currently active preset.

#### Scenario: Preset list shows the active preset distinctly

- **WHEN** a user opens the Settings screen and navigates to the Presets section
- **THEN** the list renders every preset with its localized display name in the active `AppLanguage`, and the active preset carries a visible active-badge that is not present on any other preset

#### Scenario: Built-in and active presets cannot be deleted from the list

- **WHEN** a user attempts to invoke delete on a preset flagged `isBuiltIn` or on the currently active preset
- **THEN** the delete affordance is either absent or disabled, and no mutation is submitted

#### Scenario: Duplicate creates a user-owned editable preset

- **WHEN** a user invokes duplicate on a built-in preset
- **THEN** the app creates a new user-owned preset whose template and params match the source, whose display name carries a bilingual "(copy)" suffix, and which is immediately editable through the preset editor

### Requirement: Android companion chat chrome surfaces the active preset and the memory entry point

The system SHALL display the currently active preset's localized display name in the companion chat chrome and MUST expose an entry point from the chat chrome to the companion's memory panel. Tapping the active-preset surface MUST route the user to Settings Presets; the memory entry point MUST open the memory panel scoped to the active companion.

#### Scenario: Chat chrome shows the active preset name

- **WHEN** a user opens a companion conversation
- **THEN** the chat chrome shows a pill or label with the currently active preset's display name in the active `AppLanguage`

#### Scenario: Active-preset pill routes to Settings

- **WHEN** a user taps the active-preset pill in the chat chrome
- **THEN** the app navigates to the Presets section of Settings, where the active preset is visible and editable (if user-owned) or duplicatable (if built-in)

#### Scenario: Memory entry point opens a panel scoped to the active companion

- **WHEN** a user taps the memory entry point in the chat chrome
- **THEN** the app opens a memory panel that shows the rolling summary, pinned-facts list, and three reset affordances scoped to the active companion

### Requirement: Android memory panel lets the user review summary, manage pins, and reset memory with three granularities

The system SHALL render a memory panel that displays the current rolling summary (read-only prose, localized to the active `AppLanguage`) with a freshness subtitle, the list of pinned facts with inline edit and delete affordances, a "New pin" entry that accepts bilingual text, and three confirmation-gated reset buttons covering the pinned-only, summary-only, and all-memory scopes. The panel MUST NOT expose any operation that modifies the conversation transcript.

#### Scenario: Memory panel shows summary with freshness subtitle

- **WHEN** a user opens the memory panel for a companion
- **THEN** the panel renders the rolling summary in the active `AppLanguage` with a subtitle indicating how many completed turns have occurred since the summary was last regenerated

#### Scenario: Pinned-facts list supports edit and delete

- **WHEN** a user invokes edit or delete on an existing pin in the panel
- **THEN** the app updates or removes the pin through `CompanionMemoryRepository`, the panel's observable list reflects the change, and the underlying conversation transcript is unchanged

#### Scenario: Reset buttons are confirmation-gated and scope-correct

- **WHEN** a user invokes one of the three reset buttons and confirms the confirmation dialog
- **THEN** the app calls `CompanionMemoryRepository.reset` with the scope matching the button (pins, summary, or all) and the panel's observable state reflects the appropriate fields being cleared

### Requirement: Android chat bubble supports "Pin as memory" on any variant

The system SHALL expose a "Pin as memory" action on the context menu of every message bubble in a companion conversation, including variant bubbles that are not on the active path. Invoking the action MUST create a pin whose `sourceMessageId` references the selected bubble and whose text captures the bubble's current body in a bilingual structure with the active `AppLanguage` as the primary side.

#### Scenario: Pin from an active-path companion bubble

- **WHEN** a user invokes "Pin as memory" on a companion bubble that is on the conversation's active path
- **THEN** the app calls `CompanionMemoryRepository.createPin` with the bubble's `messageId` as `sourceMessageId` and the bubble text as the primary-language pin body, and the new pin appears in the memory panel's observed list

#### Scenario: Pin from a non-active-path variant

- **WHEN** a user swipes to a non-active variant of a companion bubble and invokes "Pin as memory"
- **THEN** the app pins the variant's content, the pin carries the variant's `messageId` as `sourceMessageId`, and swipe navigation of the bubble is unaffected by the pin creation

#### Scenario: Pin from a user bubble

- **WHEN** a user invokes "Pin as memory" on a user-authored bubble
- **THEN** the app creates a pin whose `sourceMessageId` references that bubble and whose text captures the user's own message as the pin body
