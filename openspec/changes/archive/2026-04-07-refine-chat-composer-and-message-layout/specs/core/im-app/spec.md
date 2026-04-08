## ADDED Requirements

### Requirement: Chat detail presents compact identity chrome and attributed message rows
The system SHALL present chat detail with a compact top identity row that places the `<` back affordance immediately before the contact nickname, and it MUST render message rows with an avatar before the message bubble and a small sender label above the bubble.

#### Scenario: User sees compact chat identity row
- **WHEN** the user opens a conversation
- **THEN** the chat screen shows a compact top row with the `<` back affordance beside the contact nickname instead of a large "Back" pill and "Active Room" header block

#### Scenario: Timeline shows identity-led message rows
- **WHEN** the chat timeline renders incoming, outgoing, or system messages
- **THEN** each visible message row includes an avatar before the bubble and a small sender label above the bubble so author identity is readable before the message body

## MODIFIED Requirements

### Requirement: Messages tab summarizes conversations in a single-row list
The system SHALL present conversations as one row per contact showing nickname, latest message preview, message time, and unread badge count when unread messages exist, and it MUST keep the conversation list as the primary focal area while rendering any unread summary as supporting information instead of the dominant content block.

#### Scenario: Conversation row includes unread metadata
- **WHEN** a conversation has unread messages
- **THEN** its row displays the contact nickname, latest message snippet, latest timestamp, and a numeric unread badge

#### Scenario: Unread summary remains supporting context
- **WHEN** the Messages page displays aggregate unread state
- **THEN** the unread summary is rendered as supporting context that does not visually displace the conversation list as the page's primary content

#### Scenario: Empty conversation state is shown
- **WHEN** the user has no conversations in local state
- **THEN** the Messages page displays an empty-state panel instead of a blank list

### Requirement: Chat detail exposes AIGC generation entry points
The system SHALL provide a standard chat composer with a text input field and send action as the primary control path, and it MUST expose AIGC actions plus local photo and video pickers from a secondary `+` action menu inside the chat experience.

#### Scenario: User sends plain text from the primary composer
- **WHEN** the user is in chat detail and enters text without opening secondary actions
- **THEN** the screen provides a text input field with a send control on the right side as the primary messaging action

#### Scenario: User opens the secondary action menu
- **WHEN** the user taps the `+` affordance in the chat composer
- **THEN** the app reveals a secondary action menu that contains AIGC generation entries and local media pickers instead of showing those controls inline by default

#### Scenario: User starts text-to-image generation in chat
- **WHEN** the user selects the text-to-image AIGC action from the secondary composer menu
- **THEN** the app opens the generation flow with a prompt input experience tied to the shared AIGC composable

#### Scenario: User starts image-to-image generation with local media
- **WHEN** the user selects image-to-image from the secondary composer menu and chooses a local photo
- **THEN** the app passes the chosen media and prompt data through the shared AIGC generation flow

#### Scenario: User starts video-to-video generation with local media
- **WHEN** the user selects video-to-video from the secondary composer menu and chooses a local video
- **THEN** the app passes the chosen media and prompt data through the shared AIGC generation flow
