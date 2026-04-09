## Why

The current shell surfaces have drifted apart in layout rhythm: `Messages`, `Contacts`, and `Space` no longer present their primary headings at a consistent visual level, the Contacts sort control sits on its own row and pushes the list down too far, and the live IM status panel occupies space on the Messages screen even though it is really a validation/debug concern. We need a focused UI cleanup so the top-level tabs feel intentional and the IM validation state lives with the settings flow that already owns backend configuration.

## What Changes

- Move the Contacts sort dropdown into the same top band as the `Contacts / 联系人` heading so the first visible contact row starts higher and the page wastes less vertical space.
- Normalize the visual position and typography scale of the large top-level headings used by `Recent conversations / 最近对话`, `Contacts / 联系人`, and `Space / 空间`.
- Remove the live IM status card from the Messages screen.
- Surface the same IM connection/validation status inside `Settings > IM Validation` so backend readiness and troubleshooting copy live beside the IM endpoint inputs.
- Add or update focused Android UI coverage for the adjusted heading alignment, Contacts top-band layout, and IM status relocation.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `core/im-app`: refine top-level shell header layout, compact Contacts sort placement, and move visible live IM validation status from Messages into Settings.

## Impact

- Affected code: Android shell presentation under `android/app/src/main/java/com/gkim/im/android/feature/messages`, `android/app/src/main/java/com/gkim/im/android/feature/contacts`, `android/app/src/main/java/com/gkim/im/android/feature/space`, and `android/app/src/main/java/com/gkim/im/android/feature/settings`.
- Affected tests: Android UI coverage under `android/app/src/androidTest/java/com/gkim/im/android/feature/navigation`.
- Affected specs: `openspec/specs/core/im-app/spec.md`.
- Affected systems: Android app information architecture and validation chrome only; backend contracts are unchanged.
