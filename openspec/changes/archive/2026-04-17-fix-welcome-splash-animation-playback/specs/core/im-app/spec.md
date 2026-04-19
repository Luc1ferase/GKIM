## ADDED Requirements

### Requirement: Welcome animation playback remains reliable through welcome-surface lifecycle changes
The system SHALL start the approved packaged welcome animation when the unauthenticated welcome route first becomes visible, and it MUST keep that animation perceptibly active across normal welcome-surface lifecycle changes such as view attachment, resume, and layout refresh instead of leaving users on a frozen first frame or stalled playback surface.

#### Scenario: Welcome animation starts when the welcome route first appears
- **WHEN** the Android app launches into the unauthenticated welcome flow
- **THEN** the packaged welcome animation begins muted looping playback on the welcome surface rather than remaining on a static initial frame

#### Scenario: Welcome animation recovers after playback surface re-entry
- **WHEN** the welcome playback surface is reattached, resumed, or refreshed while the user is still on the unauthenticated welcome route
- **THEN** the welcome animation returns to perceptible playback without requiring the user to relaunch the app or navigate away and back manually

#### Scenario: Playback failure does not masquerade as healthy motion
- **WHEN** the welcome animation cannot prepare or resume correctly at runtime
- **THEN** the welcome surface exposes a deliberate non-playing fallback state instead of silently presenting a frozen frame as though the opening animation were still working
