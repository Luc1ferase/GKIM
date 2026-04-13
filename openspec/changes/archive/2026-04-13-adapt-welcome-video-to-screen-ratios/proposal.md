## Why

The Android welcome/onboarding surface already uses the approved bundled video asset, but the current runtime playback path does not define how that motion layer should behave across different phone aspect ratios. At the same time, the decorative translucent block currently sitting above the `注册` / `登录` actions makes the lower CTA area feel busier than intended. We need a focused cleanup so the opening experience stays visually stable across screens and the auth entry area reads more cleanly.

## What Changes

- Update the welcome/onboarding requirement so the runtime video backdrop must preserve its source aspect ratio while covering the full welcome viewport on supported Android phone screen sizes.
- Allow the welcome backdrop to use centered zoom-and-crop behavior instead of stretching, pillarboxing, or letterboxing when the screen ratio differs from the source video.
- Remove the standalone translucent decorative block currently rendered above the `注册` / `登录` action row.
- Refine the Android welcome playback implementation so the runtime backdrop follows that cover policy while preserving the current approved `welcome_intro_1` asset, muted looping playback, and native login/register controls.
- Add focused verification that the welcome screen still renders correctly and that the responsive video-cover policy remains in place for representative portrait viewport ratios.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `core/im-app`: the welcome/onboarding requirement needs to define responsive fullscreen video behavior across different Android screen resolutions and aspect ratios.

## Impact

- Affected code: Android welcome/onboarding composition in `android/app/src/main/java/com/gkim/im/android/feature/navigation/WelcomeRoute.kt` plus any new playback helper or media wrapper introduced for cover scaling.
- Affected dependencies: Android media playback stack may need a dependency or wrapper that supports explicit fullscreen cover/zoom behavior.
- Affected tests: welcome/onboarding Android coverage in `android/app/src/androidTest/java/com/gkim/im/android/feature/navigation` and any supporting unit tests for playback-layout policy.
- Affected systems: Android unauthenticated welcome shell only; auth flow behavior, copy, and backend systems remain unchanged.
