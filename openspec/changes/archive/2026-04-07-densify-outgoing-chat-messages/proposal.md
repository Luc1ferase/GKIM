## Why

The current chat timeline still treats outgoing messages like fully attributed conversation rows, which adds unnecessary avatar and sender chrome to the user's own messages and wastes vertical space. Tightening the outgoing bubble layout now will make the IM surface feel denser and more messenger-native without changing the existing Aether interaction model.

## What Changes

- Remove the avatar and `You` sender label from outgoing chat messages so the user's own messages render as compact self-authored bubbles.
- Move the message timestamp into the lower-right corner of the message bubble and tighten its spacing so outgoing messages consume less vertical space while keeping timestamp readability.
- Preserve the existing attribution pattern for incoming and system messages, including avatar-leading layout, sender labels, attachments, and current timestamp formatting.
- Add or update automated coverage so outgoing message rows verify the absence of self-avatar/self-label chrome and the new timestamp placement.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: refine chat message presentation requirements so self-authored messages omit redundant self-identity chrome and use denser in-bubble timestamp placement.

## Impact

- Affected code: Android chat timeline composables and related Compose UI tests under `android/app/src/main/java/com/gkim/im/android/feature/chat` and `android/app/src/androidTest/java/com/gkim/im/android/feature/navigation`.
- Affected specs: `openspec/specs/core/im-app/spec.md`.
- Affected tests: chat timeline attribution and timestamp placement coverage in `GkimRootAppTest`.
- No backend, repository, or provider API changes are required.
