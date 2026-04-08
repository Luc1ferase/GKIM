## Why

The Android client now covers the core IM flow, but several surface details still feel inconsistent with a polished messenger: incoming timestamps do not match the outgoing density model, unread state is emphasized on the wrong tab, and key settings are still missing for language and theme. Addressing these together now will make the app feel more coherent across chat, navigation, and personalization without changing backend behavior.

## What Changes

- Move incoming message timestamps into the lower-right area of the incoming bubble so both message directions use a denser, more consistent timestamp treatment.
- Extend Settings with app-level language selection for Chinese and English plus a light/dark theme switch alongside the existing provider controls.
- Move the aggregate unread summary from Messages into Space so Messages can focus on the conversation list while Space gains the supporting signal overview.
- Simplify the Messages screen header so the first visible list heading starts at `Recent conversations` without extra introductory copy above it.
- Refine Contacts sorting controls from a horizontal option strip into a single bubble-aligned dropdown affordance on the right side of the sort card.
- Preserve existing Aether styling, current provider configuration behavior, and the delivery workflow that requires per-task verification, review scoring, and upload evidence.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: refine chat timestamp placement, settings preferences, unread-summary placement, Messages header hierarchy, and Contacts sort-control presentation.

## Impact

- Affected code: Android Compose screens and state under `android/app/src/main/java/com/gkim/im/android/feature/chat`, `android/app/src/main/java/com/gkim/im/android/feature/messages`, `android/app/src/main/java/com/gkim/im/android/feature/space`, `android/app/src/main/java/com/gkim/im/android/feature/contacts`, `android/app/src/main/java/com/gkim/im/android/feature/settings`, plus shared preference/theme infrastructure under `android/app/src/main/java/com/gkim/im/android/data` and `android/app/src/main/java/com/gkim/im/android/core/designsystem`.
- Affected specs: `openspec/specs/core/im-app/spec.md` via a delta spec covering chat detail, Messages, Contacts, Space, and Settings requirements.
- Affected verification: Compose UI and repository/unit coverage for chat geometry, settings persistence, theme/language preference behavior, unread-summary relocation, and Contacts sort interactions.
