## Why

GKIM's Android shell still behaves like a design-heavy prototype: settings are card-stacked instead of menu-driven, the app defaults to English and dark mode, Space and Workshop are split into separate destinations, and IM still depends on development-session identity instead of real accounts. We need to turn the current prototype shell into a production-oriented first-run experience so onboarding, identity, discovery, and content browsing all work as one coherent product.

## What Changes

- Replace the current long-form settings page structure with a menu-style settings experience and change first-run defaults to Chinese plus light theme.
- Remove incoming-message count bubbles from the message shell surfaces so receiving new content no longer adds badge-style bubble noise to the app chrome.
- Merge the current Workshop entry into the Space surface by folding it into the `For You` / `Prompt Engineering` content row and rendering all resulting content with the same waterfall-style feed treatment.
- Add an authenticated welcome/onboarding entry flow based on the materials under `docs/stitch-design/welcome_screen`, including first-run login/register gating before the user can access the main app.
- Introduce a real account system with registration, login, persistent user IDs, and contact adding by ID, replacing the current development-only identity assumptions in the IM flow.
- Upgrade backend identity/session assumptions so IM conversations, contacts, and bootstrap flows are scoped to authenticated accounts instead of development-session shortcuts.

## Capabilities

### New Capabilities
- `account-system`: registration, login, persistent account identity, and user-to-user adding by account ID across app and backend surfaces.

### Modified Capabilities
- `core/im-app`: update shell navigation, default preferences, authenticated app entry, Space/Workshop information architecture, and message-shell unread/bubble behavior.
- `im-backend`: replace development-only IM identity assumptions with authenticated account-backed sessions and account-ID contact lookup/add flows.

## Impact

- Affected code: Android navigation, settings, onboarding, Space/Workshop, contacts, and messaging presentation layers under `android/app/src/main/java/com/gkim/im/android/feature` plus shared repositories/preferences under `android/app/src/main/java/com/gkim/im/android/data`.
- Affected specs: new `account-system` capability plus requirement changes to `openspec/specs/core/im-app/spec.md` and `openspec/specs/im-backend/spec.md`.
- Affected systems: Android first-run experience, local preference defaults, IM account/session handling, contact graph behavior, and Rust backend auth/contact/session APIs.
- Affected dependencies: welcome-screen media assets under `docs/stitch-design/welcome_screen`, Android preference persistence, backend persistence for accounts and contact relationships, and any client/server auth token handling required to replace the current dev-session shortcut.
