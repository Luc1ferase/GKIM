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

### Requirement: Welcome onboarding uses native runtime composition instead of reference mockup overlays
The system SHALL render the Android unauthenticated welcome/onboarding surface as native runtime UI aligned to the approved design direction, and it MUST NOT display the provided static welcome/register mockup image as a background, overlay, or other runtime composition layer behind the interactive auth controls. The welcome screen video backdrop MUST use the currently approved packaged runtime motion asset derived from `docs/stitch-design/welcome_screen/1.mp4` while preserving the existing looping muted playback behavior. The `登录` and `注册` actions on that welcome surface MUST route to real auth screens rather than directly bypassing authentication into the shell.

#### Scenario: Unauthenticated startup shows a native-composed welcome surface
- **WHEN** the Android app launches without an authenticated session
- **THEN** the welcome screen renders the onboarding title and `注册` / `登录` actions from native runtime UI layers instead of from a packaged screenshot composition

#### Scenario: Welcome actions route into real auth screens
- **WHEN** the user taps `登录` or `注册` on the unauthenticated welcome surface
- **THEN** the app opens the corresponding auth route instead of directly marking the user authenticated

#### Scenario: Auth controls are not layered on top of a static mockup capture
- **WHEN** the unauthenticated welcome screen is displayed
- **THEN** the login/register controls remain readable without relying on a shipped static mockup image behind them

#### Scenario: Welcome screen uses the approved runtime video asset
- **WHEN** the Android app packages and renders the welcome-screen video backdrop
- **THEN** the runtime welcome video resource corresponds to the approved motion source derived from `docs/stitch-design/welcome_screen/1.mp4` rather than the superseded packaged backdrop asset

### Requirement: Android app provides credential login and registration entry points
The system SHALL expose real `登录 / 注册` routes from the unauthenticated welcome flow and account surfaces, and it MUST submit those forms to backend auth endpoints instead of treating auth actions as local preview toggles. Successful register/login actions MUST persist the returned session and enter the authenticated shell. Backend validation or authentication failures MUST be shown to the user inline.

#### Scenario: User logs in from the welcome-driven auth flow
- **WHEN** an unauthenticated user opens the login route from the welcome surface and submits valid credentials
- **THEN** the app calls the backend login endpoint, stores the returned session token, and enters the authenticated shell

#### Scenario: User registers from the welcome-driven auth flow
- **WHEN** an unauthenticated user opens the register route from the welcome surface and submits valid account details
- **THEN** the app calls the backend register endpoint, stores the returned session token, and enters the authenticated shell

#### Scenario: Auth form shows backend validation feedback
- **WHEN** the backend returns invalid-credential, duplicate-username, or invalid-input feedback during login or registration
- **THEN** the app shows the error inline on the corresponding auth route instead of silently advancing into the shell

### Requirement: Android app persists authenticated sessions securely
The system SHALL persist the authenticated account session locally using encrypted device storage, and it MUST restore the authenticated shell only when the stored session remains valid against backend bootstrap or equivalent authenticated startup checks.

#### Scenario: Stored session restores the authenticated shell
- **WHEN** the app launches and a valid stored session token exists
- **THEN** the app restores the authenticated shell instead of forcing the user back through welcome/login on every launch

#### Scenario: Invalid stored session falls back to welcome
- **WHEN** the app launches with a missing, expired, or invalid stored session token
- **THEN** the app clears the stale session state and returns to the unauthenticated welcome/auth flow

### Requirement: Contacts surfaces expose user discovery and pending friend actions
The system SHALL let authenticated users discover other users and respond to incoming friend requests from Android UI surfaces. The contacts experience MUST expose a search-driven add-friend path plus pending incoming request actions needed to unlock mutual-contact messaging.

#### Scenario: User discovers another account from Contacts
- **WHEN** an authenticated user opens the contacts discovery flow and searches for another account
- **THEN** the app shows matching users with their relationship state and allows sending a friend request where appropriate

#### Scenario: User accepts or rejects an incoming friend request
- **WHEN** an authenticated user views pending incoming friend requests in the Contacts experience
- **THEN** the app exposes accept and reject actions that update the visible request/contact state

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

### Requirement: Primary shell tabs use a consistent heading rhythm
The system SHALL present `Recent conversations / 最近对话`, `Contacts / 联系人`, and `Space / 空间` using the same primary heading scale and top-band alignment so the three top-level tabs read as one coordinated shell.

#### Scenario: User switches between the three primary tabs
- **WHEN** the user opens Messages, Contacts, and Space from the bottom navigation
- **THEN** each page shows its main heading at a consistent visual level with the same large heading treatment instead of noticeably different title sizing or top offsets

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

#### Scenario: Contacts header shows title only
- **WHEN** the Contacts page renders its page header
- **THEN** the header displays only the "Contacts / 联系人" title without an eyebrow, description, or settings action

#### Scenario: Contacts header shows title-only top band with inline sorting
- **WHEN** the Contacts page renders its top-level header area
- **THEN** the header displays only the `Contacts / 联系人` title plus the compact sort control in the same row, and the first contact row starts immediately below that shared top band

### Requirement: Space feed renders developer-oriented rich posts
The system SHALL provide a `Space` feed optimized for developer posts and prompt-discovery content, and it MUST render mixed discovery content through the shared content-rendering pipeline while exposing `为你推荐`, `提示工程`, `AI 工具`, and `动态` as the visible discovery filter row without showing a separate unread-summary card above the feed. The Space page header MUST include a settings action pill as the sole app-level settings entry point across the three primary tabs.

#### Scenario: Space tab focuses on discovery content without unread summary chrome
- **WHEN** the user opens the Space tab
- **THEN** the page does not display a `未读信号` summary card or aggregate unread count panel above the feed

#### Scenario: Space filter row restores four visible discovery entries
- **WHEN** the user views the `Space` discovery rail
- **THEN** the page shows `为你推荐`, `提示工程`, `AI 工具`, and `动态` in the filter row

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

### Requirement: Production-facing app surfaces avoid development-stage commentary
The system SHALL present shipped Android UI copy as product-facing language, and it MUST NOT render development-stage notes, prototype annotations, or internal explanatory commentary directly inside production-facing app surfaces.

#### Scenario: Space header avoids development commentary
- **WHEN** the user opens the `Space` tab
- **THEN** the visible page chrome does not show internal-facing helper copy such as `创作者动态` or long development-oriented explanatory sentences above the feed

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

### Requirement: IM validation status is surfaced from Settings
The system SHALL present live IM connection and validation status inside `Settings > IM Validation`, and it MUST place that status alongside the IM endpoint inputs used for emulator validation and troubleshooting.

#### Scenario: User opens the IM Validation settings surface
- **WHEN** the user navigates to `Settings > IM Validation`
- **THEN** the screen shows the current live IM validation or connection status near the HTTP base URL, WebSocket URL, and development-user inputs
