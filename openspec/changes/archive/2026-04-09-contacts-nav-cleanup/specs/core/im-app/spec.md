## MODIFIED Requirements

### Requirement: Messages tab summarizes conversations in a single-row list
The system SHALL present conversations as one row per contact showing nickname, latest message preview, message time, and unread badge count when unread messages exist, and it MUST keep the conversation list as the primary focal area without rendering a separate unread summary panel above the list. The first visible section heading on the non-empty Messages screen MUST start at `Recent conversations`. The Messages screen MUST NOT include a settings action button; settings access is provided exclusively from the Space page.

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
The system SHALL provide a compact inline dropdown pill on the Contacts page that allows sorting by nickname initial, added time ascending, and added time descending. The sort control MUST render as a single pill-shaped element displaying the active sort label with a dropdown indicator, and it MUST NOT occupy a full-width card or include explanatory text. The Contacts page header MUST display only the page title without an eyebrow label, description paragraph, or settings action.

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

#### Scenario: Contacts header shows title only
- **WHEN** the Contacts page renders its page header
- **THEN** the header displays only the "Contacts / 联系人" title without an eyebrow, description, or settings action

### Requirement: Space feed renders developer-oriented rich posts
The system SHALL provide a Space feed optimized for developer posts, and it MUST render Markdown content, CSS-authored presentation blocks, and MDX-compatible post documents through a shared content-rendering pipeline while also surfacing the aggregate unread message count as supporting context near the top of the page. The Space page header MUST include a settings action pill as the sole app-level settings entry point across the three primary tabs.

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

#### Scenario: Space page provides the settings entry point
- **WHEN** the Space page renders its page header
- **THEN** the header includes a "Settings / 设置" pill action that navigates to the settings page
