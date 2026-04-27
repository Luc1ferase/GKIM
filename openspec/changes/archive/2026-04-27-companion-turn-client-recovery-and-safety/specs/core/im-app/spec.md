## MODIFIED Requirements

### Requirement: Chat detail presents compact identity chrome and attributed message rows
The system SHALL present chat detail with a compact top identity row that places the `<` back affordance immediately before the contact nickname, and it MUST render incoming and system message rows with an avatar before the message bubble plus a small sender label above the bubble while rendering outgoing self-authored messages as compact self-bubbles without redundant self identity chrome. Short outgoing text-only bubbles MUST adapt their width more closely to message content, while longer outgoing text and attachment-bearing outgoing rows MUST continue to preserve readable mobile wrapping and stable footer/timestamp placement. Incoming message timestamps MUST render inside the lower-right area of the incoming bubble so time remains secondary to message content for both directions. The chat detail MUST expose a single bilingual Regenerate affordance per companion bubble (no duplicate English-only legacy entry), MUST provide reachable Settings navigation from the chat top-bar overflow when the active conversation is a companion conversation, and MUST hide the Completed-bubble status-line row when no provider/model badge is available so the row never renders an untranslated `"Ready"` literal.

#### Scenario: User sees compact chat identity row
- **WHEN** the user opens a conversation
- **THEN** the chat screen shows a compact top row with the `<` back affordance beside the contact nickname instead of a large "Back" pill and "Active Room" header block

#### Scenario: Timeline shows identity-led rows for incoming and system messages
- **WHEN** the chat timeline renders incoming or system messages
- **THEN** each visible message row includes an avatar before the bubble and a small sender label above the bubble so author identity is readable before the message body

#### Scenario: Outgoing self-authored messages omit redundant self identity chrome
- **WHEN** the chat timeline renders a message sent by the current user
- **THEN** the outgoing message is shown as a compact self-bubble without a self-avatar and without a `You` sender label

#### Scenario: Incoming timestamp stays inside the bubble footer
- **WHEN** the chat timeline renders an incoming message timestamp
- **THEN** the timestamp is positioned near the lower-right edge inside the incoming message bubble instead of on a separate header line

#### Scenario: Outgoing timestamp stays inside the bubble footer
- **WHEN** the chat timeline renders a self-authored outgoing message timestamp
- **THEN** the timestamp keeps the existing display format and is positioned near the lower-right edge inside the message bubble to reduce unused vertical space

#### Scenario: Short outgoing text bubble hugs content width
- **WHEN** the user sends a short plain-text outgoing message
- **THEN** the outgoing bubble width stays materially closer to the message content than to the full available row width

#### Scenario: Longer outgoing content still wraps cleanly
- **WHEN** the chat timeline renders a longer outgoing text message or an outgoing message with an attachment
- **THEN** the bubble preserves readable wrapping and stable footer placement instead of collapsing into an overly narrow layout

#### Scenario: Companion bubbles render exactly one bilingual Regenerate affordance
- **WHEN** the chat timeline renders the most-recent completed companion bubble
- **THEN** the bubble MUST expose exactly one Regenerate affordance, rendered as the bilingual text `"Regenerate from here"` (English) / `"从这里重新生成"` (Chinese) per the active app language; the legacy English-only `"Regenerate"` Text MUST NOT be present
- **AND** any non-most-recent companion bubble that supports mid-conversation regeneration MUST also render the same bilingual affordance and no other Regenerate entry

#### Scenario: Settings is reachable from the chat top-bar overflow
- **WHEN** the user is on the chat detail surface for a companion conversation and opens the top-bar overflow `⋮` dropdown
- **THEN** the dropdown MUST contain a `"Settings"` (English) / `"设置"` (Chinese) item alongside the existing `"Export chat"` / `"导出对话"` item; tapping the Settings item MUST navigate to the `"settings"` route without losing the chat detail's place in the back stack

#### Scenario: Completed companion bubble hides the status line when no model badge is available
- **WHEN** a companion turn lifecycle reaches `MessageStatus.Completed` and the response carries no `modelBadge`
- **THEN** the bubble's status-line row MUST be hidden entirely; no fallback string (in particular, no English `"Ready"` literal) MUST render under the bubble
- **AND** when a `modelBadge` IS available, the status line MUST render as `"Model · <badge>"` unchanged

