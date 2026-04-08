## MODIFIED Requirements

### Requirement: Chat detail presents compact identity chrome and attributed message rows
The system SHALL present chat detail with a compact top identity row that places the `<` back affordance immediately before the contact nickname, and it MUST render incoming and system message rows with an avatar before the message bubble plus a small sender label above the bubble while rendering outgoing self-authored messages as compact self-bubbles without redundant self identity chrome. Short outgoing text-only bubbles MUST adapt their width more closely to message content, while longer outgoing text and attachment-bearing outgoing rows MUST continue to preserve readable mobile wrapping and stable footer/timestamp placement.

#### Scenario: User sees compact chat identity row
- **WHEN** the user opens a conversation
- **THEN** the chat screen shows a compact top row with the `<` back affordance beside the contact nickname instead of a large "Back" pill and "Active Room" header block

#### Scenario: Timeline shows identity-led rows for incoming and system messages
- **WHEN** the chat timeline renders incoming or system messages
- **THEN** each visible message row includes an avatar before the bubble and a small sender label above the bubble so author identity is readable before the message body

#### Scenario: Outgoing self-authored messages omit redundant self identity chrome
- **WHEN** the chat timeline renders a message sent by the current user
- **THEN** the outgoing message is shown as a compact self-bubble without a self-avatar and without a `You` sender label

#### Scenario: Outgoing timestamp stays inside the bubble footer
- **WHEN** the chat timeline renders a self-authored outgoing message timestamp
- **THEN** the timestamp keeps the existing display format and is positioned near the lower-right edge inside the message bubble to reduce unused vertical space

#### Scenario: Short outgoing text bubble hugs content width
- **WHEN** the user sends a short plain-text outgoing message
- **THEN** the outgoing bubble width stays materially closer to the message content than to the full available row width

#### Scenario: Longer outgoing content still wraps cleanly
- **WHEN** the chat timeline renders a longer outgoing text message or an outgoing message with an attachment
- **THEN** the bubble preserves readable wrapping and stable footer placement instead of collapsing into an overly narrow layout
