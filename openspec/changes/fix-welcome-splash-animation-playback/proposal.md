## Why

The Android app's launch welcome animation is no longer playing reliably, so first-time users can land on a welcome surface that looks frozen or partially initialized instead of a living onboarding experience. This needs to be fixed now because the welcome motion is already part of the product contract in `core/im-app`, and a broken first impression weakens demos, onboarding, and regression confidence.

## What Changes

- Repair the welcome/splash animation playback path so the approved bundled `welcome_intro_1` backdrop starts reliably when the unauthenticated welcome screen appears.
- Tighten the welcome video lifecycle behavior so playback can recover from view attach, layout, or resume updates instead of getting stuck on a static frame.
- Add focused automated coverage around welcome animation readiness and playback recovery so future welcome-surface refactors do not silently regress motion.
- Refresh Android-facing validation guidance so operators know how to verify that the welcome animation is actually playing after the fix lands.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: refine the welcome-onboarding motion requirement so the packaged welcome animation must start and stay perceptibly active when the welcome route is first shown and when the playback surface is reattached or refreshed.

## Impact

- Affected code: Android welcome/onboarding UI under `android/app/src/main/java/com/gkim/im/android/feature/navigation/**`, plus focused Android tests and docs.
- Affected APIs: Android media/view lifecycle interactions used by the welcome video playback container.
- Affected dependencies: Existing Android media playback stack only; no new backend or external service dependencies are expected.
- Affected systems: Android unauthenticated launch experience, onboarding regression coverage, and operator validation guidance for welcome playback.
