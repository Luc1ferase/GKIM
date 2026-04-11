## Why

The `Recent conversations / 最近对话` header still uses a passive active-conversation count, while the real add-friend flow lives deeper in Contacts and is easy to miss during product demos. We need a more actionable top-right entry point that lets users start real social actions from Messages and adds an initial QR scanning surface that can safely display scanned content before any deep-link behavior is introduced.

## What Changes

- Replace the Messages header's active-conversation count text with a `+` quick-action trigger.
- Add a dropdown menu from that `+` trigger with `加好友 / Add friend` and `扫描二维码 / Scan QR code` actions.
- Route `Add friend` into a real, backend-backed friend discovery/request flow instead of a front-end-only mock path.
- Add a QR scanning flow that can read content and present the decoded payload to the user without auto-navigation or side effects.
- Add focused Android UI and flow validation for the new quick-action menu, the real add-friend entry path, and QR payload display behavior.

## Capabilities

### New Capabilities

_(none — this change extends the existing app shell and social flows rather than introducing a brand-new standalone capability)_

### Modified Capabilities

- `core/im-app`: Messages header behavior changes from passive conversation-count copy to an actionable quick-action menu, and the authenticated shell gains a QR scan result surface plus a Messages-entry add-friend path that must use the real social workflow.

## Impact

- Affected code: Android shell and navigation under `android/app/src/main/java/com/gkim/im/android/feature/messages`, `android/app/src/main/java/com/gkim/im/android/feature/navigation`, and the existing social flows under `android/app/src/main/java/com/gkim/im/android/feature/social` and `android/app/src/main/java/com/gkim/im/android/feature/contacts`.
- Affected tests: Android UI/instrumentation coverage for the Messages header menu, friend-add routing, and QR payload display.
- Affected systems: Android app interaction flow and validation coverage. Existing backend friend-request APIs are reused and revalidated; no new backend contract is planned in this proposal.
