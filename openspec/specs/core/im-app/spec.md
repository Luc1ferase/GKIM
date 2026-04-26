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
The system SHALL render the Android unauthenticated welcome/onboarding surface as native runtime UI aligned to the approved design direction, and it MUST NOT display the provided static welcome/register mockup image as a background, overlay, or other runtime composition layer behind the interactive auth controls. The welcome screen video backdrop MUST use the currently approved packaged runtime motion asset derived from `docs/stitch-design/welcome_screen/1.mp4` while preserving the existing looping muted playback behavior. The runtime backdrop MUST preserve the source video aspect ratio and scale to cover the available welcome-screen viewport across supported Android phone screen resolutions, allowing centered crop rather than stretching, pillarboxing, or letterboxing when ratios differ. The lower auth CTA area MUST NOT render a separate translucent decorative block immediately above the `注册` / `登录` action row. The welcome-screen motion MUST remain visibly active to the user after runtime cover-scaling and overlay treatment are applied, instead of degrading into a seemingly static backdrop while the onboarding UI remains on screen. The `登录` and `注册` actions on that welcome surface MUST route to real auth screens rather than directly bypassing authentication into the shell.

#### Scenario: Unauthenticated startup shows a native-composed welcome surface
- **WHEN** the Android app launches without an authenticated session
- **THEN** the welcome screen renders the onboarding title and `注册` / `登录` actions from native runtime UI layers instead of from a packaged screenshot composition

#### Scenario: Welcome actions route into real auth screens
- **WHEN** the user taps `登录` or `注册` on the unauthenticated welcome surface
- **THEN** the app opens the corresponding auth route instead of directly marking the user authenticated

#### Scenario: Auth controls are not layered on top of a static mockup capture
- **WHEN** the unauthenticated welcome screen is displayed
- **THEN** the login/register controls remain readable without relying on a shipped static mockup image behind them

#### Scenario: Welcome CTA row avoids standalone translucent chrome above the actions
- **WHEN** the unauthenticated welcome screen renders the lower auth action area
- **THEN** the `注册` / `登录` buttons appear without a separate translucent decorative block immediately above them

#### Scenario: Welcome screen uses the approved runtime video asset
- **WHEN** the Android app packages and renders the welcome-screen video backdrop
- **THEN** the runtime welcome video resource corresponds to the approved motion source derived from `docs/stitch-design/welcome_screen/1.mp4` rather than the superseded packaged backdrop asset

#### Scenario: Welcome video covers supported phone viewports without distortion
- **WHEN** the Android app renders the welcome-screen video backdrop on supported phone viewports whose aspect ratio differs from the source video
- **THEN** the runtime backdrop fills the available viewport edge to edge, preserves the source video aspect ratio, and crops outer edges if needed instead of stretching the image or showing blank bands

#### Scenario: Welcome video still reads as visibly playing after cover and overlay adjustments
- **WHEN** the Android app renders the welcome-screen video backdrop with the current runtime scaling and scrim treatment
- **THEN** the user can still perceive active motion from the packaged welcome video instead of seeing a backdrop that appears frozen or fully obscured

### Requirement: Android app provides credential login and registration entry points
The system SHALL expose real `登录 / 注册` routes from the unauthenticated welcome flow and account surfaces, and it MUST submit those forms to backend auth endpoints instead of treating auth actions as local preview toggles. Successful register/login actions MUST persist the returned session and enter the authenticated shell. Backend validation or authentication failures MUST be shown to the user inline. The login and registration surfaces MUST also provide a working in-UI back affordance that returns the user to the welcome route.

#### Scenario: User logs in from the welcome-driven auth flow
- **WHEN** an unauthenticated user opens the login route from the welcome surface and submits valid credentials
- **THEN** the app calls the backend login endpoint, stores the returned session token, and enters the authenticated shell

#### Scenario: User registers from the welcome-driven auth flow
- **WHEN** an unauthenticated user opens the register route from the welcome surface and submits valid account details
- **THEN** the app calls the backend register endpoint, stores the returned session token, and enters the authenticated shell

#### Scenario: Auth form shows backend validation feedback
- **WHEN** the backend returns invalid-credential, duplicate-username, or invalid-input feedback during login or registration
- **THEN** the app shows the error inline on the corresponding auth route instead of silently advancing into the shell

#### Scenario: User returns from login or registration to welcome
- **WHEN** the user taps the visible back affordance on the login or registration screen
- **THEN** the app navigates back to the unauthenticated welcome surface instead of leaving an inert back label on screen

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
The system SHALL access IM, feed, and AIGC backend capabilities through HTTPS and WebSocket service endpoints, and it MUST NOT embed direct PostgreSQL credentials or database trust material in the Android client runtime. Backend-side PostgreSQL connectivity MUST be configurable through deployment-managed secrets for the current provider, so switching away from Aiven to the operations-provided PostgreSQL instance does not require shipping raw database inputs inside the repository or the mobile app. For IM emulator validation, the Android app MUST also support operator-managed HTTP and WebSocket endpoint inputs plus a development user selection so the Android emulator can target a host-published or deployed backend service without rebuilding the APK. Credential login, registration, startup bootstrap, contacts, and user-search flows MUST resolve their HTTP API target from the persisted operator-managed IM Validation configuration or an already-authenticated session URL, rather than silently falling back to an opaque emulator-local `127.0.0.1` default when no session has been established yet.

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

#### Scenario: Auth routes honor the persisted emulator-facing HTTP endpoint before session bootstrap
- **WHEN** an unauthenticated user opens login or registration before any authenticated session URL has been stored
- **THEN** the app sends auth requests to the persisted IM Validation HTTP base URL instead of silently assuming `http://127.0.0.1:18080/`

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
The system SHALL provide a `Space` feed optimized for developer posts and prompt-discovery content, and it MUST render mixed discovery content through the shared content-rendering pipeline while exposing `为你推荐`, `提示工程`, `AI 工具`, and `动态` as the visible discovery filter row without showing a separate unread-summary card above the feed. The Space page header MUST include a visible and tappable settings action pill as the sole app-level settings entry point across the three primary tabs.

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

#### Scenario: Space header settings action remains tappable after shell refactors
- **WHEN** the user taps the visible settings pill from the Space page header
- **THEN** the app navigates into the settings route instead of rendering a missing or inert settings affordance

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
The system SHALL provide a standard chat composer with a text input field and send action as the primary control path, and it MUST expose distinct secondary actions for only the AIGC generation modes supported by the currently active provider, normal chat media attachments, and generation-source media selection from the `+` menu inside the chat experience. Image-to-image and video-to-video actions MUST only appear or enable as ready-to-run actions when both the active provider supports the mode and the required local source media for that mode has been selected explicitly for generation. Unsupported AIGC modes MUST NOT appear as ready-to-run actions for the active provider.

#### Scenario: User sends plain text from the primary composer
- **WHEN** the user is in chat detail and enters text without opening secondary actions
- **THEN** the screen provides a text input field with a send control on the right side as the primary messaging action

#### Scenario: User opens the secondary action menu
- **WHEN** the user taps the `+` affordance in the chat composer
- **THEN** the app reveals a secondary action menu that separates normal chat media attachment actions from the AIGC generation actions supported by the active provider instead of relying on one ambiguous `Pick image` path

#### Scenario: User picks an image to send as a normal chat message
- **WHEN** the user chooses the chat image attachment action from the secondary menu and selects a local photo
- **THEN** the app stages that photo as a normal outgoing chat attachment rather than treating it as image-to-image input

#### Scenario: User picks an image as image-to-image source media
- **WHEN** the user chooses the generation-source image action from the secondary menu and selects a local photo
- **THEN** the app stages that photo as image-to-image input and keeps it separate from normal outgoing chat attachments

#### Scenario: User starts text-to-image generation in chat
- **WHEN** the user selects the text-to-image AIGC action from the secondary composer menu
- **THEN** the app opens the generation flow with a prompt input experience tied to the shared AIGC composable

#### Scenario: Image-to-image only becomes runnable after source image selection
- **WHEN** the active provider supports image-to-image but the user has not selected a generation-source image yet
- **THEN** the chat `+` menu does not present image-to-image as a ready-to-run action and instead guides the user to choose generation source media first

#### Scenario: User starts image-to-image generation with explicit source media
- **WHEN** the active provider supports image-to-image, the user has selected a generation-source photo explicitly for AIGC, and the user chooses image-to-image
- **THEN** the app passes that staged generation-source media and prompt data through the shared AIGC generation flow

#### Scenario: User starts video-to-video generation with local media
- **WHEN** the user selects video-to-video from the secondary composer menu and chooses a local video as generation source media
- **THEN** the app passes the chosen generation-source media and prompt data through the shared AIGC generation flow

#### Scenario: Unsupported AIGC modes are not presented as ready actions
- **WHEN** the active provider does not support one or more AIGC modes
- **THEN** the chat `+` menu hides or otherwise clearly blocks those unsupported generation actions before any network request starts

### Requirement: Successful generated images expose follow-up actions
The system SHALL expose actionable follow-up controls on successful generated-image results, and it MUST let the user either save the generated image locally or send it into the current conversation without auto-sending the result by default.

#### Scenario: User saves a generated image locally
- **WHEN** a generated image result succeeds and the user selects the save action from the result surface
- **THEN** the app exports the generated image to local device storage and reports whether the save completed successfully

#### Scenario: User sends a generated image into the active conversation
- **WHEN** a generated image result succeeds and the user selects the send action from the result surface
- **THEN** the app inserts the generated image into the current conversation as a normal outgoing image message

#### Scenario: Successful generation does not auto-send without user confirmation
- **WHEN** an image generation succeeds
- **THEN** the result remains a preview/action surface until the user explicitly chooses a follow-up action such as save or send

### Requirement: Creative workshop supports prompt discovery and contribution
The system SHALL provide a creative workshop that helps users discover, reuse, and contribute prompts, and it MUST expose creator attribution data so future KOL/community features can build on the same content model.

#### Scenario: User applies a workshop template to generation
- **WHEN** the user selects a prompt template from the creative workshop
- **THEN** the template content is inserted into the downstream AIGC generation flow for editing or immediate use

#### Scenario: User contributes a new prompt template
- **WHEN** the user submits a prompt template to the creative workshop
- **THEN** the app stores the contribution in the workshop content model with author attribution metadata

### Requirement: AI settings support preset and custom infrastructure providers
The system SHALL provide a Settings page that includes preset providers for Tencent Hunyuan and Alibaba Tongyi, and it MUST allow users to configure preset-provider API keys locally through secure device storage while also supporting a custom OpenAI-compatible endpoint, API key, and model identifier plus app-level language and appearance preferences. The Tencent Hunyuan preset MUST default to model `hy-image-v3.0`, and the Alibaba Tongyi preset MUST default to model `wan2.7-image`. The page MUST support Chinese and English selection plus explicit light and dark theme modes, and those preferences MUST persist across app restarts. Preset and custom provider secrets MUST NOT be sourced from tracked repository defaults.

#### Scenario: User activates Tencent Hunyuan with the requested preset model
- **WHEN** the user selects Tencent Hunyuan in Settings
- **THEN** the provider configuration store marks `hunyuan` as active for subsequent AIGC requests and uses `hy-image-v3.0` as the preset model unless the user explicitly overrides it locally

#### Scenario: User activates Alibaba Tongyi with the requested preset model
- **WHEN** the user selects Alibaba Tongyi in Settings
- **THEN** the provider configuration store marks `tongyi` as active for subsequent AIGC requests and uses `wan2.7-image` as the preset model unless the user explicitly overrides it locally

#### Scenario: User stores preset-provider API keys locally
- **WHEN** the user enters a Tencent Hunyuan or Alibaba Tongyi API key in Settings
- **THEN** the app persists that key through secure local storage and restores it for later local generation sessions without reading it from tracked source defaults

#### Scenario: User configures a custom provider
- **WHEN** the user enters a custom API base URL, API key, and model in Settings
- **THEN** the provider configuration store persists the custom configuration for the shared AIGC adapter layer

#### Scenario: User switches app language
- **WHEN** the user selects Chinese or English in Settings
- **THEN** the app persists that language preference and updates affected UI copy to the selected language on the supported surfaces

#### Scenario: User switches app theme
- **WHEN** the user selects light mode or dark mode in Settings
- **THEN** the app persists that theme preference and re-renders the supported surfaces using the selected appearance mode

### Requirement: Preset image providers execute real generation requests with truthful task state
The system SHALL execute image-generation requests against the active preset provider’s real HTTP API when the user has configured a local API key, and it MUST render truthful task status, errors, and returned image output instead of synthesizing stock preview success. Successful generation MUST record the selected provider and resolved model in the resulting task metadata.

#### Scenario: Tencent Hunyuan generation returns a real image result
- **WHEN** the active provider is Tencent Hunyuan, a local API key is configured, and the user runs a supported image-generation request
- **THEN** the app sends a real provider request using model `hy-image-v3.0` (or the locally overridden preset model), receives the provider response, and renders the returned image output in the existing AIGC result surfaces

#### Scenario: Alibaba Tongyi generation returns a real image result
- **WHEN** the active provider is Alibaba Tongyi, a local API key is configured, and the user runs a supported image-generation request
- **THEN** the app sends a real provider request using model `wan2.7-image` (or the locally overridden preset model), receives the provider response, and renders the returned image output in the existing AIGC result surfaces

#### Scenario: Missing preset-provider key blocks fake success
- **WHEN** the user attempts generation from a preset provider without configuring its required local API key
- **THEN** the app reports a configuration error and does not create a fake succeeded task with a stock preview URL

#### Scenario: Provider failure remains visible and truthful
- **WHEN** the active provider returns an HTTP, auth, quota, or payload-validation failure during generation
- **THEN** the app surfaces a failed generation state or error message that reflects the provider failure and does not insert a placeholder success image

#### Scenario: Task metadata reflects the provider actually used
- **WHEN** a generation request succeeds
- **THEN** the resulting AIGC task records the active provider identity and resolved model so later UI and validation evidence can identify which provider produced the image

### Requirement: IM validation status is surfaced from Settings
The system SHALL present live IM connection and validation status inside `Settings > IM Validation`, and it MUST place that status alongside the IM endpoint inputs used for emulator validation and troubleshooting.

#### Scenario: User opens the IM Validation settings surface
- **WHEN** the user navigates to `Settings > IM Validation`
- **THEN** the screen shows the current live IM validation or connection status near the HTTP base URL, WebSocket URL, and development-user inputs

### Requirement: Tavern surface exposes character detail and editor flows

The system SHALL let authenticated users open a character detail surface for any roster card and open a character editor surface for user-authored or draw-acquired cards. The detail surface MUST show the resolved persona authoring record and an activation action. The editor MUST support creating new cards and updating editable cards with bilingual prose input and optional avatar selection.

#### Scenario: User navigates from tavern to character detail

- **WHEN** the user taps a companion card on the tavern surface
- **THEN** the app opens a character detail route that renders the card's persona authoring record in the active language and shows an activation action

#### Scenario: User enters the editor to author a new card

- **WHEN** the user invokes the create-card entry point from the tavern surface
- **THEN** the app opens a character editor route that accepts bilingual entries for system prompt, personality, scenario, example dialogue, first-message greeting, alternate greetings, tags, creator metadata, and avatar image selection

#### Scenario: User saves or cancels the editor

- **WHEN** the user confirms Save or Cancel from the editor route
- **THEN** the app either persists the new or updated card and returns to the prior surface, or discards edits and returns without modifying roster state

### Requirement: Companion chat entry uses the full persona authoring record

The system SHALL propagate the full persona authoring record (system prompt, personality, scenario, example dialogue, greetings) along with the selected card's bilingual display metadata when the user activates a companion and routes into chat. Companion chat entry MUST NOT rely on only the card's display summary to identify persona context.

#### Scenario: User activates a card and enters chat

- **WHEN** the user activates a card from the tavern detail surface and opens the companion conversation
- **THEN** the conversation entry path carries the full persona authoring record associated with the active card so downstream chat flows can use persona instructions rather than only the summary string
### Requirement: Android tavern + chat avatar taps route to a portrait large-view surface

The system SHALL wire every avatar tap on the tavern roster, chat header, and chat bubble avatar to navigate to a portrait large-view surface scoped to the tapped card. The surface MUST support pinch-to-zoom, single-finger pan, and double-tap to toggle between 1× and 2× zoom, with a graceful placeholder for cards without an avatar.

#### Scenario: Tavern roster avatar tap routes to large-view

- **WHEN** a user taps a card's avatar on the tavern roster
- **THEN** the app navigates to the portrait large-view surface for that card

#### Scenario: Chat header avatar tap routes to large-view

- **WHEN** a user taps the chat header's avatar for the active companion
- **THEN** the app navigates to the same portrait large-view surface as from the tavern roster

#### Scenario: Chat bubble avatar tap routes to large-view

- **WHEN** a user taps a companion bubble's inline avatar inside the chat timeline
- **THEN** the app navigates to the same portrait large-view surface scoped to that bubble's companion card

#### Scenario: Avatar-less card renders a placeholder large-view

- **WHEN** a user taps the avatar of a card that has no high-resolution portrait
- **THEN** the large-view surface renders a placeholder carrying the card's display name and an "Edit card" shortcut, instead of an empty portrait area

### Requirement: Android chat opener picker renders bilingual previews with remembered-default behavior

The system SHALL render the opener picker (introduced by `llm-text-companion-chat`) with ~120-character bilingual previews per option, tap-to-preview modals for the full greeting text, and a remembered-default highlight on the previously-selected greeting per companion. The remembered default MUST carry a localized "Remembered from last time" caption.

#### Scenario: Picker renders localized previews

- **WHEN** a user opens the opener picker for a card with three alt-greetings
- **THEN** each option card shows a ~120-character preview localized to the user's active `AppLanguage`

#### Scenario: Remembered default is highlighted

- **WHEN** a user had previously selected greeting B, then returns to the picker for the same companion after a relationship reset
- **THEN** greeting B is default-highlighted and carries the "Remembered from last time" caption

#### Scenario: Tap-to-preview opens the full greeting modal

- **WHEN** a user taps an option's preview chevron on the picker
- **THEN** a modal renders the full greeting text in the active `AppLanguage` without committing the selection; dismissing the modal returns to the picker with no selection change

### Requirement: Android chat bubble rows support sibling-swipe, edit-user, and arbitrary-layer regenerate

The system SHALL extend `ChatMessageRow` so every bubble with `siblingCount > 1` renders left/right chevrons plus an `n / total` caption; every **user** bubble exposes an "Edit" overflow action; and every **companion** bubble exposes a "Regenerate from here" overflow action. All three affordances MUST operate on the variant-tree model from `llm-text-companion-chat` by creating new siblings under the appropriate `parentMessageId` or `variantGroupId`, never by overwriting existing messages.

#### Scenario: Sibling chevrons appear only on multi-sibling groups

- **WHEN** a bubble's variant group has a single sibling
- **THEN** no chevrons or caption render for that bubble

#### Scenario: Edit creates a user-turn sibling

- **WHEN** a user taps Edit on a user bubble, modifies the text, and submits
- **THEN** the client calls the edit endpoint with the new text and the bubble's `parentMessageId`; the returned new user sibling becomes the active path alongside its generated companion turn

#### Scenario: Regenerate from here creates a companion-turn sibling at that layer

- **WHEN** a user taps "Regenerate from here" on a mid-conversation companion bubble
- **THEN** the client calls the regenerate endpoint with that bubble's `messageId` as the target; the returned new sibling becomes the active path

#### Scenario: Sibling caption renders n / total at any depth

- **WHEN** a multi-sibling variant group is displayed mid-conversation (not only the most-recent layer)
- **THEN** the caption renders `n / total` reflecting the active sibling's 1-based index plus the group's sibling count, identical in shape to the most-recent-layer rendering

### Requirement: Android chat chrome surfaces the per-character preset override

The system SHALL render the chat chrome's preset pill with the overridden preset's display name and a localized "(card override)" suffix when the active character carries `characterPresetId` non-null. Tapping the pill MUST route to the character-detail surface where the override can be cleared.

#### Scenario: Overridden pill renders the override label

- **WHEN** the active character has `characterPresetId = "preset-X"` and the user's globally-active preset is `preset-Y`
- **THEN** the pill renders `preset-X`'s display name with the "(card override)" suffix

#### Scenario: Tapping the pill routes to the character detail

- **WHEN** a user taps the overridden preset pill
- **THEN** the app routes to the active character's detail surface with the "Override preset" row focused for clearing

#### Scenario: Clearing the override reverts the pill to the global preset label

- **WHEN** a user clears the "Override preset" row on the character-detail surface
- **THEN** the chat chrome's preset pill re-renders with the user's globally-active preset's display name, no "(card override)" suffix, and the pill's tap target reverts to the global-preset settings entry

### Requirement: Android chat exposes a JSONL export dialog

The system SHALL add a "Export as JSONL" action to the chat overflow menu that opens an export dialog offering: active-path-only vs. full-tree toggle, a target-language selector defaulted to the user's active `AppLanguage`, and a share-sheet vs. Downloads destination. Invoking the dialog MUST call the backend export endpoint and route the returned payload to the chosen destination; the default filename MUST include the `_<first8OfConversationId>` suffix.

#### Scenario: Dialog defaults to active path + share sheet + active language

- **WHEN** a user opens the export dialog for the first time
- **THEN** active-path-only is pre-selected, the target is the share sheet, and the language defaults to the user's active `AppLanguage`

#### Scenario: Downloads destination writes the file

- **WHEN** a user selects the Downloads destination and confirms the export
- **THEN** the returned JSONL payload is written through `DownloadManager` with the conversation-hash-suffixed filename

#### Scenario: Full-tree toggle requests every message regardless of active path

- **WHEN** a user flips the toggle from active-path-only to full-tree and confirms the export
- **THEN** the client calls the export endpoint with `pathOnly=false`; the resulting JSONL carries one line per message in the conversation, including non-active siblings

### Requirement: Android character detail exposes the full-relationship-reset affordance

The system SHALL render a "Reset relationship" affordance on the character-detail surface behind a two-step confirmation dialog. The affordance MUST call the relationship-reset endpoint on confirm, show an inline error with a retry on failure, and update the in-memory caches (conversations list, memory record) to reflect the reset state on success.

#### Scenario: Two-step confirmation arms and commits

- **WHEN** a user taps "Reset relationship" once
- **THEN** the affordance enters the armed state with a destructive-action warning; a second tap commits the reset; a cancel affordance dismisses without change

#### Scenario: Successful reset refreshes in-memory caches

- **WHEN** the backend acknowledges the reset
- **THEN** the tavern surface shows zero conversations for this companion, the memory panel renders the empty-memory state, and no stale data lingers

#### Scenario: Failed reset surfaces an inline error with a retry affordance

- **WHEN** the backend rejects the reset request (non-2xx response or transport failure)
- **THEN** the affordance renders an inline localized error and a retry button; tapping retry re-invokes the endpoint without re-arming the two-step confirmation

### Requirement: Android gacha roster renders rarity probabilities and a duplicate-animation branch

The system SHALL extend the gacha roster flow so the pre-draw surface renders the rarity / probability breakdown from the backend catalog response. After a draw whose resulting card id appears in the user's owned roster, the result-animation MUST play the "Already owned" variant with a "Keep as bonus" CTA that records a `bonusAwarded` event.

#### Scenario: Pre-draw surface renders rarity breakdown

- **WHEN** the user opens the gacha pre-draw surface
- **THEN** a rarity breakdown derived from the backend catalog probabilities renders in the user's active `AppLanguage`

#### Scenario: Owned-card draw plays the duplicate variant

- **WHEN** a draw returns a card id that is already in the user's owned roster
- **THEN** the result animation plays the "Already owned" variant; tapping the "Keep as bonus" CTA records the bonus event

### Requirement: Android character detail renders an About-this-card sub-section for creator attribution

The system SHALL add an "About this card" sub-section to the character-detail surface rendering `creator`, `creatorNotes`, `characterVersion`, the linkified `stSource` from `extensions.st.stSource`, and formatted `stCreationDate` / `stModificationDate` values. Missing fields MUST be hidden (no empty-row placeholder). The `stSource` link MUST open in the system browser.

#### Scenario: Populated fields render labeled rows

- **WHEN** a card's `creator` and `characterVersion` are populated and `creatorNotes` is empty
- **THEN** the sub-section renders two rows (creator, characterVersion) and omits the creatorNotes row

#### Scenario: All-fields-missing card hides the sub-section entirely

- **WHEN** a card has no `creator`, `creatorNotes`, `characterVersion`, `extensions.st.stSource`, `extensions.st.stCreationDate`, nor `extensions.st.stModificationDate`
- **THEN** the "About this card" sub-section is not rendered at all (no header, no empty-row placeholders), so the card detail keeps a clean layout

#### Scenario: stSource opens the system browser

- **WHEN** a user taps a linkified `stSource` URL
- **THEN** the system browser opens the URL; the app remains in the background so returning resumes the character-detail surface

### Requirement: Android chat surface renders the production Edit-user and Regenerate-from-here affordances on every applicable bubble

The system SHALL render an Edit overflow on every user (Outgoing) bubble that carries a `parentMessageId`, opening an edit sheet prefilled with the bubble's body text. The system SHALL render a Regenerate-from-here overflow on every companion (Incoming + `companionTurnMeta`) bubble — not only the most recent — that on tap invokes the `regenerateCompanionTurnAtTarget` repository call. The §3.1 sibling chevrons MUST stay visible on every variantGroup with `siblingCount > 1` regardless of how the siblings were created (submit, regenerate, regenerate-at, or edit).

#### Scenario: User bubble with a parent renders the Edit overflow

- **WHEN** the chat timeline includes a user bubble with a non-null `parentMessageId` and the conversation has an active companion id
- **THEN** the bubble renders an Edit overflow whose tap opens a Compose `ModalBottomSheet` prefilled with the bubble's body text and a Submit button gated on the §3.2 `canSubmit` rule (non-blank + differs-from-original)

#### Scenario: Root user bubble suppresses the Edit overflow

- **WHEN** the chat timeline includes a user bubble with a null `parentMessageId` (the conversation's root user message)
- **THEN** the bubble does not render the Edit overflow because the §3.2 endpoint contractually rejects edits with no parent

#### Scenario: Mid-conversation companion bubble renders Regenerate-from-here

- **WHEN** the chat timeline includes a companion bubble in the middle of the conversation (not the most recent)
- **THEN** the bubble renders a Regenerate-from-here overflow whose tap invokes the repository's `regenerateCompanionTurnAtTarget` call with the bubble's `messageId` as the target

#### Scenario: Most-recent companion bubble also renders Regenerate-from-here

- **WHEN** the chat timeline's most recent message is a companion bubble
- **THEN** the same Regenerate-from-here overflow appears (the §3.3 spec wording requires it on every companion bubble, not only mid-conversation)

### Requirement: ChatViewModel maintains a per-conversation active-path map and projects it back into bubble metadata

The system SHALL maintain a `Map<conversationId, Map<variantGroupId, activeIndex>>` in `ChatViewModel` state. On every recomposition, each rendered `ChatMessage.companionTurnMeta` MUST carry the `siblingActiveIndex` corresponding to that bubble's `variantGroupId`'s entry in the map (or 0 when the group has only one sibling). The map MUST be mutable via `selectVariantAt(conversationId, variantGroupId, newIndex)`, idempotent (no-op when newIndex equals the current active), and clamped to `[0, siblingCount - 1]`.

#### Scenario: Single-sibling group projects active index 0

- **WHEN** a conversation has a `variantGroupId` with one sibling (the original submit response)
- **THEN** the rendered bubble's `companionTurnMeta.siblingActiveIndex` is 0 and no chevron renders (per the §3.1 chevron-suppression rule)

#### Scenario: Multi-sibling group projects the map's active index

- **WHEN** a conversation has a `variantGroupId` with three siblings and the map records active index 2
- **THEN** the rendered bubble's `companionTurnMeta.siblingActiveIndex` is 2, the §3.1 chevrons render with a "3/3" caption, and the prev chevron is enabled while the next chevron is disabled (terminal-disable rule)

#### Scenario: selectVariantAt mutates the map and clamps out-of-bounds inputs

- **WHEN** the user taps the next chevron on a "2/3" sibling group
- **THEN** `selectVariantAt(conversationId, variantGroupId, 2)` flips the map entry from 1 → 2 and the bubble re-renders at the new active variant

- **WHEN** `selectVariantAt(conversationId, variantGroupId, 5)` is called on a 3-sibling group
- **THEN** the value clamps to 2 (the maximum valid index) without throwing

### Requirement: ChatViewModel exposes editUserTurn and regenerateFromHere handlers with lifecycle state

The system SHALL expose two handler entry-points on `ChatViewModel`: `editUserTurn(messageId, newDraftText)` and `regenerateFromHere(messageId)`. Each handler MUST surface in-flight + failed lifecycle state on the ViewModel so the affordance UI can render an in-flight indicator and an inline error with a retry. On success, the corresponding §3.2 / §3.3 active-path effect MUST be applied so the new variant becomes the active path.

#### Scenario: editUserTurn happy path applies the active-path effect on success

- **WHEN** `editUserTurn("user-msg-7", "rewritten text")` is called and the repository call returns the new user-message + companion-turn variants
- **THEN** the lifecycle state transitions in-flight → completed and the §3.2 `editUserBubbleActivePathEffect` flips the active-path map entries for the user-message's parent variantGroup and the companion-turn's parent variantGroup to the new variants

#### Scenario: editUserTurn failure surfaces an inline error with retry

- **WHEN** the repository call rejects the request (transport failure or non-2xx response)
- **THEN** the lifecycle state transitions in-flight → failed with the failure reason; the affordance UI renders the inline error and a retry button; tapping retry re-invokes the same `editUserTurn` without re-arming any user gesture

#### Scenario: regenerateFromHere supports mid-conversation invocation

- **WHEN** `regenerateFromHere("companion-msg-3")` is called on a companion bubble that is not the most recent
- **THEN** the repository call's `targetMessageId` is "companion-msg-3" and the resulting new sibling becomes the active variant for that group, leaving every later companion turn's variant tree untouched

### Requirement: Chat export entry-point and dispatcher in the production Android chat surface

The Android chat surface SHALL expose an "Export" entry in the chat top bar's overflow menu on companion conversations (gated on `conversation.companionCardId != null`). Tapping the entry SHALL open `ChatExportDialog` with the active `conversationId`. The dialog SHALL render the `ChatExportDialogState` controls (path-only toggle, language pills, share/downloads target pills) and project the dispatcher's outcome through the §5.1 lifecycle flags (in-flight disables controls, completed auto-dismisses, errorCode renders inline copy).

#### Scenario: Companion conversation surfaces the export entry

- **WHEN** the user opens a chat where the active conversation has a non-null `companionCardId`
- **THEN** the chat top bar's overflow menu displays an "Export" entry
- **AND** non-companion (peer / direct) conversations do not surface the entry

#### Scenario: Submit dispatches via share-sheet and auto-dismisses on success

- **WHEN** the user opens the export dialog, leaves `pathOnly=true`, leaves the language at the active app language, and selects `target=Share` then taps Export
- **THEN** the dialog enters the in-flight state, calls `repository.exportConversation(conversationId, format="jsonl", pathOnly=true)`, builds an `Intent(ACTION_SEND)` with the JSONL bytes attached via `FileProvider`, fires `Intent.createChooser(...)`, and on success marks the dialog completed which dismisses it

#### Scenario: Submit dispatches via Downloads and auto-dismisses on success

- **WHEN** the user opens the export dialog, toggles `pathOnly=false`, selects `target=Downloads`, and taps Export
- **THEN** the dialog enters the in-flight state, calls `repository.exportConversation(conversationId, format="jsonl", pathOnly=false)`, writes the JSONL bytes to `MediaStore.Downloads` (Android Q+) under the chat-exports relative subdir using the `chat-export-full-tree_<first8OfConversationId>.jsonl` filename, and on success marks the dialog completed which dismisses it

#### Scenario: Wire failure renders the inline error and re-enables controls

- **WHEN** `repository.exportConversation` returns `Result.failure(throwable)` whose message matches one of `404_unknown_conversation` / `unsupported_format` / `network_failure`
- **THEN** the dialog leaves the in-flight state, sets `errorCode = <code>`, renders the localized error copy below the controls, and re-enables the controls so the user can retry or change choices

#### Scenario: Dispatcher failure renders the inline error

- **WHEN** the wire call succeeds but the platform dispatcher fails (no share target installed, Downloads unavailable, write IO failure)
- **THEN** the dialog leaves the in-flight state, sets `errorCode` to the dispatcher's emitted code (`no_share_target` / `share_cancelled` / `downloads_unavailable` / `write_failed`), and renders the localized error copy
### Requirement: Reset relationship affordance on the production character-detail surface

The Android character-detail screen SHALL render a "Reset relationship" affordance below the action row on companion-card details (gated on `card.companionCardId != null`). The affordance SHALL drive the §6.1 `RelationshipResetAffordanceState` state machine through five visible UI states (Idle / Armed / Submitting / Completed / Failed) and surface the destructive-action confirmation as an inline banner (not a modal), per the §6.1 presentation contract. On Completed, the local conversations list SHALL no longer contain entries whose `companionCardId` matches the reset character; on Failed, the inline error copy SHALL render and the user SHALL be able to retry the call without re-arming the two-step gate.

#### Scenario: Idle state shows the trigger button

- **WHEN** the user opens character detail for a card with `companionCardId != null` and the affordance is in `Idle` phase
- **THEN** the screen renders the "Reset relationship" trigger button (testTag `relationship-reset-trigger`)
- **AND** the confirmation banner is NOT rendered (testTag `relationship-reset-confirmation-banner` does not exist)

#### Scenario: Tap arms the destructive-action gate

- **WHEN** the user taps the trigger button
- **THEN** the affordance transitions to `Armed`, the confirmation banner becomes visible (testTag `relationship-reset-confirmation-banner`), and the banner offers Cancel + Confirm controls (testTags `relationship-reset-cancel` / `relationship-reset-confirm`)
- **AND** the conversations list is NOT yet mutated

#### Scenario: Cancel from Armed returns to Idle without the call

- **WHEN** the user taps Cancel from the Armed banner
- **THEN** the affordance returns to `Idle`, the banner disappears, no `MessagingRepository.resetRelationship` call is made, and the conversations list is unchanged

#### Scenario: Confirm from Armed dispatches and clears the local conversations on success

- **WHEN** the user taps Confirm from the Armed banner
- **THEN** the affordance transitions to `Submitting`, calls `MessagingRepository.resetRelationship(characterId)`, and on `Result.success` transitions to `Completed`, removing every `Conversation` whose `companionCardId == characterId` from the local cache

#### Scenario: Failed reset surfaces an inline error with a retry that does not re-arm the gate

- **WHEN** `MessagingRepository.resetRelationship(characterId)` returns `Result.failure` whose throwable message is `character_not_available` or `network_failure`
- **THEN** the affordance transitions to `Failed`, the inline error copy renders the localized message for the failure code (testTag `relationship-reset-error`), and the retry button is enabled (testTag `relationship-reset-retry`)
- **AND** tapping retry transitions the affordance directly from `Failed` to `Submitting` (NOT back through Armed); a successful retry then advances to `Completed` with the same local-cache mutation
