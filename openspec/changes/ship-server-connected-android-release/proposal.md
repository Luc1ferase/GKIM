## Why

The tagged APK is still shipping a mixed-mode Android client: credential login points at the IM backend, but core runtime wiring still falls back to development-session bootstrap, seed repositories, and placeholder service endpoints. As a result, the build published from GitHub does not behave like a directly usable server-connected release.

## What Changes

- Bundle the deployed IM backend origin into the Android release build so the shipped app can resolve both HTTP and WebSocket endpoints from a single server origin without operator-only setup.
- Change authenticated Android IM bootstrap to prefer the stored credential session and backend bootstrap payload instead of automatically issuing a development session in the shipped app.
- Replace seed-first Contacts and Messages runtime behavior with live backend-backed contact/conversation state for authenticated users, while keeping development-only validation hooks out of the release path.
- Keep non-IM discovery and AIGC surfaces out of scope for this change unless they block the authenticated IM release flow.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: The shipped Android app must behave as a real server-connected IM client by default, using bundled backend origin resolution plus authenticated bootstrap/contact state instead of development-session and seed-first runtime wiring.

## Impact

- Android app runtime wiring in build config, preferences/session resolution, repository composition, and authenticated shell startup.
- Android IM/bootstrap/contact flows, including login, register, messages, contacts, and settings surfaces that currently expose development-only defaults.
- Android release verification and tagged release behavior because the APK produced from GitHub must now be directly usable against the deployed server.
