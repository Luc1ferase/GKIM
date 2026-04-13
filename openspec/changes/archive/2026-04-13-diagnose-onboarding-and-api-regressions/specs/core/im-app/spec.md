## MODIFIED Requirements

### Requirement: Welcome onboarding uses native runtime composition instead of reference mockup overlays
The system SHALL render the Android unauthenticated welcome/onboarding surface as native runtime UI aligned to the approved design direction, and it MUST NOT display the provided static welcome/register mockup image as a background, overlay, or other runtime composition layer behind the interactive auth controls. The welcome screen video backdrop MUST use the currently approved packaged runtime motion asset derived from `docs/stitch-design/welcome_screen/1.mp4` while preserving the existing looping muted playback behavior. The welcome-screen motion MUST remain visibly active to the user after runtime cover-scaling and overlay treatment are applied, instead of degrading into a seemingly static backdrop while the onboarding UI remains on screen. The `登录` and `注册` actions on that welcome surface MUST route to real auth screens rather than directly bypassing authentication into the shell.

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
