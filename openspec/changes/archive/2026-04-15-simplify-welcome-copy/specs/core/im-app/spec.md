## MODIFIED Requirements

### Requirement: Welcome onboarding uses native runtime composition instead of reference mockup overlays
The system SHALL render the Android unauthenticated welcome/onboarding surface as native runtime UI aligned to the approved design direction, and it MUST NOT display the provided static welcome/register mockup image as a background, overlay, or other runtime composition layer behind the interactive auth controls. The welcome screen video backdrop MUST use the currently approved packaged runtime motion asset derived from `docs/stitch-design/welcome_screen/1.mp4` while preserving the existing looping muted playback behavior. The runtime backdrop MUST preserve the source video aspect ratio and scale to cover the available welcome-screen viewport across supported Android phone screen resolutions, allowing centered crop rather than stretching, pillarboxing, or letterboxing when ratios differ. The lower auth CTA area MUST NOT render a separate translucent decorative block immediately above the `注册` / `登录` action row. The welcome-screen motion MUST remain visibly active to the user after runtime cover-scaling and overlay treatment are applied, instead of degrading into a seemingly static backdrop while the onboarding UI remains on screen. The `登录` and `注册` actions on that welcome surface MUST route to real auth screens rather than directly bypassing authentication into the shell. The main welcome description MUST read as concise, product-facing introduction copy rather than internal engineering narration. The lower auth action area MUST NOT render a helper sentence that explains entering the live IM shell or keeping onboarding motion visible. The welcome footer treatment MAY retain a lightweight version marker, but it MUST NOT include `加密连接` or `Encrypted connection` wording.

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

#### Scenario: Welcome intro copy reads like a user-facing product introduction
- **WHEN** the unauthenticated welcome screen renders its main description under `Welcome to GKIM`
- **THEN** the screen shows concise product-facing copy instead of internal-sounding engineering or architecture narration

#### Scenario: Welcome CTA area omits technical helper narration
- **WHEN** the unauthenticated welcome screen renders the copy above the `注册` / `登录` actions
- **THEN** it does not show a sentence describing live IM shell entry or the onboarding motion layer

#### Scenario: Welcome footer avoids encrypted-connection wording
- **WHEN** the unauthenticated welcome screen renders its small footer line below the auth actions
- **THEN** the footer does not include `加密连接` or `Encrypted connection` wording
