## MODIFIED Requirements

### Requirement: Messages tab summarizes conversations in a single-row list
The system SHALL present conversations as one row per contact showing nickname, latest message preview, message time, and unread badge count when unread messages exist, and it MUST keep the conversation list as the primary focal area without rendering a separate unread summary panel above the list. The first visible section heading on the non-empty Messages screen MUST start at `Recent conversations`.

#### Scenario: Conversation row includes unread metadata
- **WHEN** a conversation has unread messages
- **THEN** its row displays the contact nickname, latest message snippet, latest timestamp, and a numeric unread badge

#### Scenario: Messages screen starts at the conversation heading
- **WHEN** the user opens the Messages tab and conversations exist
- **THEN** the first visible heading above the list is `Recent conversations` and no extra introductory copy appears before it

#### Scenario: Empty conversation state is shown
- **WHEN** the user has no conversations in local state
- **THEN** the Messages page displays an empty-state panel instead of a blank list

### Requirement: Contacts tab supports deterministic sorting controls
The system SHALL provide a dropdown control on the Contacts page that allows sorting by nickname initial, added time ascending, and added time descending, and it MUST present that control as a single bubble-aligned dropdown affordance rather than a horizontal strip of equally weighted sort chips.

#### Scenario: User sorts contacts alphabetically
- **WHEN** the user selects the nickname-initial sorting option
- **THEN** contacts are grouped or ordered by nickname initial in ascending order

#### Scenario: User sorts contacts by oldest added time
- **WHEN** the user selects the earliest-added sorting option
- **THEN** contacts are ordered from the earliest added record to the most recent

#### Scenario: User sorts contacts by newest added time
- **WHEN** the user selects the latest-added sorting option
- **THEN** contacts are ordered from the most recent added record to the earliest

#### Scenario: Sort control stays compact inside one dropdown affordance
- **WHEN** the Contacts page renders its sort controls
- **THEN** the screen shows one dropdown-style control inside the sort card instead of multiple horizontally arranged sort chips

### Requirement: Space feed renders developer-oriented rich posts
The system SHALL provide a Space feed optimized for developer posts, and it MUST render Markdown content, CSS-authored presentation blocks, and MDX-compatible post documents through a shared content-rendering pipeline while also surfacing the aggregate unread message count as supporting context near the top of the page.

#### Scenario: Space tab shows unread summary as supporting context
- **WHEN** the user opens the Space tab
- **THEN** the page displays a compact unread summary panel near the top that reports the aggregate unread conversation count without displacing the feed as the page's primary content

#### Scenario: Markdown developer post is rendered in the feed
- **WHEN** a Space post contains Markdown headings, paragraphs, lists, or code blocks
- **THEN** the feed renders the content with the shared developer-post renderer and design-system styles

#### Scenario: Styled post content uses scoped presentation rules
- **WHEN** a post includes supported CSS presentation metadata or style blocks
- **THEN** the renderer applies the supported styling without breaking feed layout or app theme tokens

#### Scenario: MDX-compatible post document enters the renderer
- **WHEN** a post is authored in the MDX-compatible content format defined by the app
- **THEN** the renderer resolves the document through the shared parsing abstraction instead of bypassing the content pipeline

### Requirement: Chat detail presents compact identity chrome and attributed message rows
The system SHALL present chat detail with a compact top identity row that places the `<` back affordance immediately before the contact nickname, and it MUST render incoming and system message rows with an avatar before the message bubble plus a small sender label above the bubble while rendering outgoing self-authored messages as compact self-bubbles without redundant self identity chrome. Short outgoing text-only bubbles MUST adapt their width more closely to message content, while longer outgoing text and attachment-bearing outgoing rows MUST continue to preserve readable mobile wrapping and stable footer/timestamp placement. Incoming message timestamps MUST render inside the lower-right area of the incoming bubble so time remains secondary to message content for both directions.

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

### Requirement: AI settings support preset and custom infrastructure providers
The system SHALL provide a Settings page that includes preset providers for Tencent Hunyuan and Alibaba Tongyi, and it MUST allow users to configure a custom OpenAI-compatible endpoint, API key, and model identifier while also exposing app-level language and appearance preferences. The page MUST support Chinese and English selection plus explicit light and dark theme modes, and those preferences MUST persist across app restarts.

#### Scenario: User activates a preset provider
- **WHEN** the user selects Tencent Hunyuan or Alibaba Tongyi in Settings
- **THEN** the provider configuration store marks the preset as active for subsequent AIGC requests

#### Scenario: User configures a custom provider
- **WHEN** the user enters a custom API base URL, API key, and model in Settings
- **THEN** the provider configuration store persists the custom configuration for the shared AIGC adapter layer

#### Scenario: User switches app language
- **WHEN** the user selects Chinese or English in Settings
- **THEN** the app persists that language preference and updates affected UI copy to the selected language on the supported surfaces

#### Scenario: User switches app theme
- **WHEN** the user selects light mode or dark mode in Settings
- **THEN** the app persists that theme preference and re-renders the supported surfaces using the selected appearance mode
