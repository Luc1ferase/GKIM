## MODIFIED Requirements

### Requirement: Messages tab summarizes conversations in a single-row list
The system SHALL present conversations as one row per contact showing nickname, latest message preview, message time, and unread badge count when unread messages exist, and it MUST keep the conversation list as the primary focal area without rendering a separate unread summary panel above the list or a standalone live IM status card ahead of the list. The first visible section heading on the non-empty Messages screen MUST start at `Recent conversations`. The Messages screen MUST NOT include a settings action button, and it MUST present a compact `+` quick-action trigger in the header instead of passive active-conversation count copy. That quick-action trigger MUST expose `Add friend / 加好友` and `Scan QR code / 扫描二维码` as menu options.

#### Scenario: Conversation row includes unread metadata
- **WHEN** a conversation has unread messages
- **THEN** its row displays the contact nickname, latest message snippet, latest timestamp, and a numeric unread badge

#### Scenario: Messages screen starts at the conversation heading
- **WHEN** the user opens the Messages tab and conversations exist
- **THEN** the first visible heading above the list is `Recent conversations` and no extra introductory copy appears before it

#### Scenario: Empty conversation state is shown
- **WHEN** the user has no conversations in local state
- **THEN** the Messages page displays an empty-state panel instead of a blank list

#### Scenario: Messages header exposes quick actions
- **WHEN** the user taps the `+` trigger in the Messages header
- **THEN** the app opens a menu that includes `Add friend / 加好友` and `Scan QR code / 扫描二维码`

#### Scenario: Messages screen omits settings and passive conversation-count copy
- **WHEN** the Messages screen renders its header
- **THEN** the header does not show a settings pill and does not show `${count} active` or `${count} 个活跃会话` copy

## ADDED Requirements

### Requirement: Messages quick add-friend entry uses the real social workflow
The system SHALL route the Messages quick `Add friend / 加好友` action into an authenticated user-discovery flow that is backed by the live social APIs, and it MUST reflect real relationship state instead of a front-end-only demo result.

#### Scenario: User opens add-friend from Messages
- **WHEN** an authenticated user selects `Add friend / 加好友` from the Messages quick-action menu
- **THEN** the app opens the authenticated user-search/request flow instead of showing a local-only placeholder

#### Scenario: User sends a real friend request from the Messages entry path
- **WHEN** the user searches for another account from the Messages-launched add-friend flow and taps `Add / 添加`
- **THEN** the app calls the live friend-request path, updates the visible relationship state to a pending state, and does not report success unless the backend request succeeds

### Requirement: QR scanning displays decoded content before any action
The system SHALL allow authenticated users to scan a QR code from the Messages quick-action menu, and it MUST display the decoded payload content in a dedicated result surface before any redirect, add-friend attempt, or other side effect occurs.

#### Scenario: Successful scan shows QR payload content
- **WHEN** the user scans a readable QR code from the Messages quick-action flow
- **THEN** the app shows the decoded content in a result surface and does not automatically navigate, open a link, or mutate account state

#### Scenario: Scan flow exits without side effects
- **WHEN** the user backs out of the scan flow or scanning does not yield a readable payload
- **THEN** the app returns to the prior shell flow without creating friendships, opening external content, or changing conversation state
