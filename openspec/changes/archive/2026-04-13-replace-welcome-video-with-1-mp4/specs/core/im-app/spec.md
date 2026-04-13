## MODIFIED Requirements

### Requirement: Welcome onboarding uses native runtime composition instead of reference mockup overlays
The system SHALL render the Android unauthenticated welcome/onboarding surface as native runtime UI aligned to the approved design direction, and it MUST NOT display the provided static welcome/register mockup image as a background, overlay, or other runtime composition layer behind the interactive auth controls. The welcome screen video backdrop MUST use the currently approved packaged runtime motion asset derived from `docs/stitch-design/welcome_screen/1.mp4` while preserving the existing looping muted playback behavior.

#### Scenario: Unauthenticated startup shows a native-composed welcome surface
- **WHEN** the Android app launches without an authenticated session
- **THEN** the welcome screen renders the onboarding title and `注册` / `登录` actions from native runtime UI layers instead of from a packaged screenshot composition

#### Scenario: Auth controls are not layered on top of a static mockup capture
- **WHEN** the unauthenticated welcome screen is displayed
- **THEN** the login/register controls remain readable without relying on a shipped static mockup image behind them

#### Scenario: Welcome screen uses the approved runtime video asset
- **WHEN** the Android app packages and renders the welcome-screen video backdrop
- **THEN** the runtime welcome video resource corresponds to the approved motion source derived from `docs/stitch-design/welcome_screen/1.mp4` rather than the superseded packaged backdrop asset
