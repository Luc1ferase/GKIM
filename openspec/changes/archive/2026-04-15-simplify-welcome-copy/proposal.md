## Why

The Android welcome screen still carries copy that reads like internal product or engineering narration instead of a natural first-run introduction. We need a focused cleanup now so the first screen feels more human, removes redundant trust-signaling chrome, and presents GKIM in language that better matches a real user-facing product surface.

## What Changes

- Remove the lower helper sentence that currently explains entering the live IM shell and keeping onboarding motion visible.
- Remove the `加密连接` wording from the small footer line while keeping the remaining lightweight version marker treatment concise.
- Rewrite the main welcome description under `Welcome to GKIM` into more natural product-facing copy in both Chinese and English.
- Keep the existing welcome structure, buttons, approved motion backdrop, and auth routing unchanged.
- Add or refresh focused Android coverage so the cleaned-up welcome copy contract does not regress back toward technical or placeholder wording.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: the welcome/onboarding requirement needs to define a cleaner, product-facing copy contract for the unauthenticated welcome surface and remove technical helper/footer wording that reads like internal implementation commentary.

## Impact

- Affected code: Android welcome/onboarding composition in `android/app/src/main/java/com/gkim/im/android/feature/navigation/WelcomeRoute.kt`.
- Affected specs: `openspec/specs/core/im-app/spec.md` via a delta that narrows welcome-copy expectations.
- Affected tests: welcome/onboarding Android coverage under `android/app/src/androidTest/java/com/gkim/im/android/feature/navigation`.
- Affected systems: Android unauthenticated first-run experience only; auth flow, layout structure, and backend behavior remain unchanged.
