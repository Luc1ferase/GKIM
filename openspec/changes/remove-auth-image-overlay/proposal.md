## Why

The current Android welcome/auth entry screen is directly rendering the provided static registration mockup image behind the live Compose controls. That turns a reference-only design artifact into runtime UI, causing the login/register layout to visually overlap with imported artwork instead of standing on its own.

## What Changes

- Rework the Android welcome/auth entry surface so it is composed entirely from native runtime UI layers rather than a packaged screenshot/mockup image.
- Preserve the intended architectural-light visual direction from `docs/stitch-design/welcome_screen`, but treat the supplied static image as implementation reference only.
- Remove the runtime dependency on the static welcome/register mockup asset from the shipped Android auth entry surface.
- Add focused UI coverage that proves unauthenticated startup still shows the welcome surface and that the login/register controls render cleanly without overlapping a reference image.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `core/im-app`: the authenticated welcome/onboarding surface needs an explicit requirement that reference mockup images are not rendered as runtime UI layers behind auth controls.

## Impact

- Affected code: Android welcome/auth navigation and composables under `android/app/src/main/java/com/gkim/im/android/feature/navigation`.
- Affected tests: welcome/auth UI coverage in `android/app/src/androidTest/java/com/gkim/im/android/feature/navigation`.
- Affected assets: the current packaged static mockup image usage in the Android app, plus any related onboarding resource references.
- Affected systems: Android first-run unauthenticated shell only; backend auth/session behavior is unchanged by this correction.
