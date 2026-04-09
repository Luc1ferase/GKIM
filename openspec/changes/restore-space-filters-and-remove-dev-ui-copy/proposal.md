## Why

The current `Space` tab lost parts of its discovery filter row during the Workshop merge, while also picking up developer-facing helper copy and unread-status chrome that make the screen feel like an internal prototype instead of a production surface. We need to restore the intended discovery entry points and tighten the page chrome so the app presents product UI, not development commentary.

## What Changes

- Restore `AI 工具` and `动态` as visible discovery filters in the `Space` tab alongside `为你推荐` and `提示工程`.
- Remove the `未读信号` card and the accompanying aggregate unread count copy from the `Space` screen.
- Remove the `创作者动态` eyebrow and the current developer-explanatory description copy from the `Space` page header.
- Add an explicit UI-copy rule that production-facing app surfaces must not render development-stage explanatory commentary or prototype notes inside the shipped interface.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `core/im-app`: refine the `Space` discovery header/filter behavior and codify production-facing UI-copy expectations for shipped app surfaces.

## Impact

- Affected code: Android `Space` screen presentation under `android/app/src/main/java/com/gkim/im/android/feature/space`.
- Affected tests: Android UI coverage for `Space` tab behavior under `android/app/src/androidTest/java/com/gkim/im/android/feature/navigation`.
- Affected specs: `openspec/specs/core/im-app/spec.md`.
- Affected systems: Android discovery and shell presentation only; backend behavior is unchanged.
