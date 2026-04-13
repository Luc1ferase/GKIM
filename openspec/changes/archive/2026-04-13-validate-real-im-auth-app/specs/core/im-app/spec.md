## ADDED Requirements

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

## MODIFIED Requirements

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
