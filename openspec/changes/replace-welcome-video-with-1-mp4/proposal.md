## Why

The Android welcome/auth entry flow is still playing the previously packaged `welcome_atrium.mp4`, but the latest approved motion reference for the opening registration surface is `docs/stitch-design/welcome_screen/1.mp4`. We need a focused asset swap so the runtime welcome animation matches the newest design direction without reopening the rest of the onboarding layout.

## What Changes

- Replace the current Android welcome-screen runtime video source with the approved `1.mp4` asset from `docs/stitch-design/welcome_screen`.
- Update the shipped raw video resource and the welcome video binding so the opening auth/register surface plays the new animation in the existing looping, muted backdrop slot.
- Add or update focused Android coverage to confirm the welcome surface still renders the native video-backed onboarding composition and continues exposing the login/register entry actions after the asset swap.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `core/im-app`: the unauthenticated welcome/onboarding surface requirements need to reflect the updated runtime animation asset used by the shipped welcome video backdrop.

## Impact

- Affected code: Android welcome/onboarding runtime composition in `android/app/src/main/java/com/gkim/im/android/feature/navigation/WelcomeRoute.kt`.
- Affected assets: Android packaged raw video resource under `android/app/src/main/res/raw` plus the source reference asset under `docs/stitch-design/welcome_screen/1.mp4`.
- Affected tests: welcome/onboarding Android UI coverage in `android/app/src/androidTest/java/com/gkim/im/android/feature/navigation`.
- Affected systems: Android unauthenticated welcome/auth shell only; authentication flow behavior and backend systems remain unchanged.
