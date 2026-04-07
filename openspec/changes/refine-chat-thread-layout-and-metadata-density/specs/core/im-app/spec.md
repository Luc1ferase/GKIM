## MODIFIED Requirements

### Requirement: Chat detail presents compact identity chrome and attributed message rows
The system SHALL present chat detail with a compact top identity row that places the `<` back affordance immediately before the contact nickname, and it MUST render incoming and system message rows with denser metadata that keeps the avatar in the leading column while aligning the incoming timestamp to the right side of the message header rhythm. Outgoing self-authored messages MUST remain dense self-bubbles, and timestamps for both directions MUST consume less dedicated space than the current loose layout.

#### Scenario: User sees compact chat identity row
- **WHEN** the user opens a conversation
- **THEN** the chat screen shows a compact top row with the `<` back affordance beside the contact nickname instead of a large "Back" pill and "Active Room" header block

#### Scenario: Incoming metadata stays compact with right-aligned timestamp
- **WHEN** the chat timeline renders an incoming message
- **THEN** the avatar remains in the leading column, the timestamp is aligned on the right side of the incoming metadata line, and the metadata consumes less vertical space than a stacked loose layout

#### Scenario: Outgoing self-authored messages stay visually dense
- **WHEN** the chat timeline renders a message sent by the current user
- **THEN** the outgoing message remains a compact self-bubble without redundant self-avatar chrome while keeping its timestamp secondary to message content

#### Scenario: Timestamps no longer dominate message spacing
- **WHEN** the chat timeline renders either incoming or outgoing message metadata
- **THEN** timestamp placement and styling stay readable while taking materially less dedicated space around the bubble than the previous layout

### Requirement: Chat detail exposes AIGC generation entry points
The system SHALL provide a standard chat composer with a text input field and send action as the primary control path, and it MUST keep that composer visually anchored to the bottom of the chat screen while exposing AIGC actions plus local photo and video pickers from a secondary `+` action menu.

#### Scenario: Composer stays anchored while thread content grows
- **WHEN** the user scrolls or the conversation accumulates more messages and auxiliary chat content
- **THEN** the composer remains visually anchored to the bottom of the chat screen instead of drifting downward with the thread layout

#### Scenario: User sends plain text from the primary composer
- **WHEN** the user is in chat detail and enters text without opening secondary actions
- **THEN** the screen provides a text input field with a send control on the right side as the primary messaging action

#### Scenario: User opens the secondary action menu
- **WHEN** the user taps the `+` affordance in the chat composer
- **THEN** the app reveals a secondary action menu that contains AIGC generation entries and local media pickers instead of showing those controls inline by default

#### Scenario: Secondary tools do not displace the anchored composer
- **WHEN** the user opens or closes the secondary action menu
- **THEN** the chat composer remains the persistent bottom input region and the menu appears without redefining the composer's anchored position
