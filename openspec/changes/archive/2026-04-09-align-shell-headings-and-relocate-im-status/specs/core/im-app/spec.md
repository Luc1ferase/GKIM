## ADDED Requirements

### Requirement: Primary shell tabs use a consistent heading rhythm
The system SHALL present `Recent conversations / 最近对话`, `Contacts / 联系人`, and `Space / 空间` using the same primary heading scale and top-band alignment so the three top-level tabs read as one coordinated shell.

#### Scenario: User switches between the three primary tabs
- **WHEN** the user opens Messages, Contacts, and Space from the bottom navigation
- **THEN** each page shows its main heading at a consistent visual level with the same large heading treatment instead of noticeably different title sizing or top offsets

### Requirement: IM validation status is surfaced from Settings
The system SHALL present live IM connection and validation status inside `Settings > IM Validation`, and it MUST place that status alongside the IM endpoint inputs used for emulator validation and troubleshooting.

#### Scenario: User opens the IM Validation settings surface
- **WHEN** the user navigates to `Settings > IM Validation`
- **THEN** the screen shows the current live IM validation or connection status near the HTTP base URL, WebSocket URL, and development-user inputs

## MODIFIED Requirements

### Requirement: Messages tab summarizes conversations in a single-row list
The system SHALL present conversations as one row per contact showing nickname, latest message preview, message time, and unread badge count when unread messages exist, and it MUST keep the conversation list as the primary focal area without rendering a separate unread summary panel above the list or a standalone live IM status card ahead of the list. The first visible section heading on the non-empty Messages screen MUST start at `Recent conversations`. The Messages screen MUST NOT include a settings action button; settings access is provided exclusively from the Space page.

#### Scenario: Conversation row includes unread metadata
- **WHEN** a conversation has unread messages
- **THEN** its row displays the contact nickname, latest message snippet, latest timestamp, and a numeric unread badge

#### Scenario: Messages screen starts at the conversation heading
- **WHEN** the user opens the Messages tab and conversations exist
- **THEN** the first visible heading above the list is `Recent conversations` and no extra introductory copy appears before it

#### Scenario: Empty conversation state is shown
- **WHEN** the user has no conversations in local state
- **THEN** the Messages page displays an empty-state panel instead of a blank list

#### Scenario: Messages screen does not expose a settings action
- **WHEN** the Messages screen renders its header
- **THEN** the header row shows the conversation count but does not include a settings pill or button

### Requirement: Contacts tab supports deterministic sorting controls
The system SHALL provide a compact inline dropdown pill on the Contacts page that allows sorting by nickname initial, added time ascending, and added time descending. The sort control MUST render as a single pill-shaped element displaying the active sort label with a dropdown indicator, and it MUST NOT occupy a full-width card or include explanatory text. The Contacts page header MUST display only the page title without an eyebrow label, description paragraph, or settings action, and the sort control MUST share that top band instead of rendering on a detached row above the list.

#### Scenario: User sorts contacts alphabetically
- **WHEN** the user selects the nickname-initial sorting option
- **THEN** contacts are grouped or ordered by nickname initial in ascending order

#### Scenario: User sorts contacts by oldest added time
- **WHEN** the user selects the earliest-added sorting option
- **THEN** contacts are ordered from the earliest added record to the most recent

#### Scenario: User sorts contacts by newest added time
- **WHEN** the user selects the latest-added sorting option
- **THEN** contacts are ordered from the most recent added record to the earliest

#### Scenario: Sort control renders as a compact inline pill
- **WHEN** the Contacts page renders its sort controls
- **THEN** the screen shows a single pill-shaped dropdown displaying the current sort label and a dropdown indicator, without a surrounding card, heading label, or explanatory text

#### Scenario: Contacts header shows title-only top band with inline sorting
- **WHEN** the Contacts page renders its top-level header area
- **THEN** the header displays only the `Contacts / 联系人` title plus the compact sort control in the same row, and the first contact row starts immediately below that shared top band
