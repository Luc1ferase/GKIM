## ADDED Requirements

### Requirement: Android app gates first use behind an authenticated welcome flow
The system SHALL present a pre-authenticated welcome experience before the main shell, and it MUST require the user to register or log in from that entry surface before using the app for the first time.

#### Scenario: Unauthenticated startup shows the stitched welcome experience
- **WHEN** the Android app launches without a valid authenticated session
- **THEN** it renders the welcome entry flow using the provided welcome-screen design direction and offers `注册` and `登录` as the primary actions

#### Scenario: Authenticated startup opens the main shell
- **WHEN** the Android app launches with a valid authenticated session
- **THEN** it bypasses the welcome entry flow and opens the main authenticated shell directly

## MODIFIED Requirements

### Requirement: Application shell provides three primary mobile tabs
The system SHALL provide a fixed bottom navigation bar with Messages, Contacts, and Space as the three primary destinations after authentication, and it MUST visually indicate the active tab using the design-system token set.

#### Scenario: Authenticated user switches between primary tabs
- **WHEN** the authenticated user taps Messages, Contacts, or Space in the bottom navigation
- **THEN** the application displays the corresponding top-level page and highlights the selected tab state

#### Scenario: Secondary pages do not replace the primary tab model
- **WHEN** the user opens chat detail or settings from the authenticated shell
- **THEN** the application routes to a secondary page flow without redefining the primary tab set

### Requirement: Messages tab summarizes conversations in a single-row list
The system SHALL present conversations as one row per contact showing nickname, latest message preview, and message time, and it MUST keep the conversation list as the primary focal area without rendering unread-count bubble badges when new messages arrive. The first visible section heading on the non-empty Messages screen MUST start at `Recent conversations`.

#### Scenario: Conversation row shows latest message without unread bubble badge
- **WHEN** a conversation receives new messages
- **THEN** its row displays the contact nickname, latest message snippet, and latest timestamp without adding a numeric unread bubble badge

#### Scenario: Messages screen starts at the conversation heading
- **WHEN** the user opens the Messages tab and conversations exist
- **THEN** the first visible heading above the list is `Recent conversations` and no extra introductory copy appears before it

#### Scenario: Empty conversation state is shown
- **WHEN** the user has no conversations in local state
- **THEN** the Messages page displays an empty-state panel instead of a blank list

### Requirement: Space feed renders developer-oriented rich posts
The system SHALL provide a Space feed optimized for developer posts and prompt-discovery content, and it MUST render mixed Space/editorial posts plus merged workshop/prompt cards through one shared waterfall-style browsing pattern while also surfacing the aggregate unread message count as supporting context near the top of the page.

#### Scenario: Space tab shows unread summary as supporting context
- **WHEN** the user opens the Space tab
- **THEN** the page displays a compact unread summary panel near the top that reports the aggregate unread conversation count without displacing the feed as the page's primary content

#### Scenario: Workshop content is merged into the Space filter row
- **WHEN** the user browses the Space discovery row
- **THEN** workshop discovery appears inside the same `为你推荐` / `提示工程` filter area instead of as a separate `工作台` destination

#### Scenario: Mixed discovery content shares one waterfall feed
- **WHEN** the user switches between Space discovery filters
- **THEN** posts and prompt/workshop cards render in the same waterfall-style feed treatment rather than in separate list paradigms

#### Scenario: Markdown developer post is rendered in the feed
- **WHEN** a Space post contains Markdown headings, paragraphs, lists, or code blocks
- **THEN** the feed renders the content with the shared developer-post renderer and design-system styles

#### Scenario: Styled post content uses scoped presentation rules
- **WHEN** a post includes supported CSS presentation metadata or style blocks
- **THEN** the renderer applies the supported styling without breaking feed layout or app theme tokens

#### Scenario: MDX-compatible post document enters the renderer
- **WHEN** a post is authored in the MDX-compatible content format defined by the app
- **THEN** the renderer resolves the document through the shared parsing abstraction instead of bypassing the content pipeline

### Requirement: Creative workshop supports prompt discovery and contribution
The system SHALL provide prompt discovery and contribution capabilities through the merged Space discovery experience, and it MUST expose creator attribution data so future KOL/community features can build on the same content model.

#### Scenario: User applies a workshop template to generation
- **WHEN** the user selects a prompt template from the merged Space discovery experience
- **THEN** the template content is inserted into the downstream AIGC generation flow for editing or immediate use

#### Scenario: User contributes a new prompt template
- **WHEN** the user submits a prompt template to the workshop contribution flow
- **THEN** the app stores the contribution in the workshop content model with author attribution metadata

### Requirement: AI settings support preset and custom infrastructure providers
The system SHALL provide a menu-style Settings experience that includes preset providers for Tencent Hunyuan and Alibaba Tongyi, and it MUST allow users to configure a custom OpenAI-compatible endpoint, API key, and model identifier while also exposing app-level language and appearance preferences. The page MUST default first-run users to Chinese and light theme, and those preferences MUST persist across app restarts.

#### Scenario: Settings opens as a menu experience
- **WHEN** the user enters Settings
- **THEN** the app presents grouped menu entries for focused settings categories instead of one long scroll-heavy control page

#### Scenario: User activates a preset provider
- **WHEN** the user selects Tencent Hunyuan or Alibaba Tongyi in Settings
- **THEN** the provider configuration store marks the preset as active for subsequent AIGC requests

#### Scenario: User configures a custom provider
- **WHEN** the user enters a custom API base URL, API key, and model in Settings
- **THEN** the provider configuration store persists the custom configuration for the shared AIGC adapter layer

#### Scenario: First-run defaults use Chinese and light theme
- **WHEN** a new user opens the app for the first time
- **THEN** the app initializes the shell with Chinese language and light theme before any manual preference changes

#### Scenario: User switches app language
- **WHEN** the user selects Chinese or English in Settings
- **THEN** the app persists that language preference and updates affected UI copy to the selected language on the supported surfaces

#### Scenario: User switches app theme
- **WHEN** the user selects light mode or dark mode in Settings
- **THEN** the app persists that theme preference and re-renders the supported surfaces using the selected appearance mode
