## Purpose
Define the product, interaction, and service-boundary requirements for the GKIM Android IM application surface.

## Requirements

### Requirement: Android scaffold follows the repository harness
The system SHALL initialize an Android application scaffold implemented with Kotlin and Jetpack Compose, and it MUST organize client responsibilities into Android-native UI, state, repository, remote, local, rendering, and model layers that preserve the repository harness intent.

#### Scenario: Scaffolded Android project structure is created
- **WHEN** the change is applied to initialize the project
- **THEN** the repository contains the mandated Android application configuration, package structure, and shared design system foundation for the IM app

#### Scenario: Screen logic is delegated to state and data layers
- **WHEN** a screen needs chat, contact, feed, workshop, or AIGC behavior
- **THEN** the implementation uses ViewModels, repositories, and shared services instead of direct API calls or complex business logic in Compose screen functions

### Requirement: Android app exercises live IM backend flows during emulator validation
The system SHALL let the Android app authenticate through the backend development session flow, hydrate conversations and message history from the live IM backend, and reconcile authenticated WebSocket events into visible chat state during Android emulator validation runs against a locally containerized backend.

#### Scenario: Emulator validation bootstraps live conversation data
- **WHEN** a tester starts an IM validation session on the Android app in the emulator with a configured development user
- **THEN** the app issues the backend session/bootstrap requests and renders conversation state from live backend data instead of seed-only in-memory messaging rows

#### Scenario: Emulator validation drives realtime send and receipt updates
- **WHEN** the tester sends a message from the Android app in the emulator while the backend and counterpart session are online
- **THEN** the app updates the visible conversation from the live backend send, receive, delivered, and read flows instead of relying on local-only message append behavior

#### Scenario: Emulator validation recovers after reconnect or relaunch
- **WHEN** the Android app loses its realtime connection or is relaunched during an emulator validation session
- **THEN** it can recover current conversation state from backend bootstrap/history APIs and resume WebSocket synchronization without requiring a rebuild or seed reset

#### Scenario: Emulator validation surfaces backend failures explicitly
- **WHEN** session issuance, bootstrap loading, history retrieval, or realtime connection setup fails in the Android emulator
- **THEN** the app shows an explicit validation/debug failure state instead of silently falling back to placeholder success behavior

### Requirement: Android client accesses protected infrastructure through service boundaries
The system SHALL access IM, feed, and AIGC backend capabilities through HTTPS and WebSocket service endpoints, and it MUST NOT embed direct PostgreSQL credentials or database trust material in the Android client runtime. Backend-side PostgreSQL connectivity MUST be configurable through deployment-managed secrets for the current provider, so switching away from Aiven to the operations-provided PostgreSQL instance does not require shipping raw database inputs inside the repository or the mobile app. For IM emulator validation, the Android app MUST also support operator-managed HTTP and WebSocket endpoint inputs plus a development user selection so the Android emulator can target a host-published or deployed backend service without rebuilding the APK.

#### Scenario: Android app initializes remote connectivity
- **WHEN** the app configures its connected services
- **THEN** it uses API base URLs and WebSocket endpoints instead of a direct PostgreSQL DSN

#### Scenario: Protected infrastructure material stays outside the APK
- **WHEN** database credentials or database CA trust material are required
- **THEN** they are held by backend infrastructure and are not packaged into the Android app

#### Scenario: Backend Postgres provider changes without mobile database coupling
- **WHEN** operations switches backend infrastructure from the previous Aiven database to a replacement PostgreSQL host such as `124.222.15.128:5432`
- **THEN** the checked-in app contract remains centered on backend service endpoints and treats the database target as backend runtime configuration only

#### Scenario: Raw database secrets are not committed as product configuration
- **WHEN** backend deployment needs the PostgreSQL role, password, optional database name, or optional TLS inputs for the replacement database
- **THEN** those values are supplied through untracked environment or secret-management inputs rather than committed to OpenSpec artifacts, Android assets, or versioned default configuration files

#### Scenario: Old Aiven certificate assumptions do not remain the default trust story
- **WHEN** active repository guidance documents backend PostgreSQL trust inputs after the provider switch
- **THEN** it no longer treats the previous Aiven `ca.pem` flow as the standing default and only documents replacement trust material if the current backend actually requires it

#### Scenario: Operator updates IM validation endpoints in the emulator
- **WHEN** a tester enters or selects IM backend endpoint values and a development user identity in the Android emulator
- **THEN** the app persists those service-boundary inputs locally for subsequent validation sessions without bundling backend credentials or database secrets into the APK

### Requirement: Application shell provides three primary mobile tabs
The system SHALL provide a fixed bottom navigation bar with Messages, Contacts, and Space as the three primary destinations, and it MUST visually indicate the active tab using the design-system token set.

#### Scenario: User switches between primary tabs
- **WHEN** the user taps Messages, Contacts, or Space in the bottom navigation
- **THEN** the application displays the corresponding top-level page and highlights the selected tab state

#### Scenario: Secondary pages do not replace the primary tab model
- **WHEN** the user opens chat detail, creative workshop, or settings
- **THEN** the application routes to a secondary page flow without redefining the primary tab set

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

### Requirement: Creative workshop supports prompt discovery and contribution
The system SHALL provide a creative workshop that helps users discover, reuse, and contribute prompts, and it MUST expose creator attribution data so future KOL/community features can build on the same content model.

#### Scenario: User applies a workshop template to generation
- **WHEN** the user selects a prompt template from the creative workshop
- **THEN** the template content is inserted into the downstream AIGC generation flow for editing or immediate use

#### Scenario: User contributes a new prompt template
- **WHEN** the user submits a prompt template to the creative workshop
- **THEN** the app stores the contribution in the workshop content model with author attribution metadata

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
