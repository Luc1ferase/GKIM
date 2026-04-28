## Context

The current Android app already supports backend credential login, registration, user search, friend requests, bootstrap loading, and realtime IM transport. However, the shipped runtime is still composed like a validation build: `LiveMessagingRepository` bootstraps through `issueDevSession`, `AppPreferencesStore` defaults a development external ID, and `AppContainer` still wires seed conversations, seed contacts, and placeholder `example.com` endpoints into the app container. This makes the published APK feel like a mock or test build even when the release build bundles the deployed server origin.

## Goals / Non-Goals

**Goals:**
- Make the shipped Android app authenticate and hydrate IM state from the deployed backend using the real stored session by default.
- Ensure release builds can derive HTTP and WebSocket IM endpoints from one bundled backend origin without manual server-address entry.
- Remove release-path dependence on seed contacts, seed conversations, and implicit development-session bootstrap for core auth/contacts/messages behavior.
- Preserve a workable development-validation path for debug/test builds where it is still useful.

**Non-Goals:**
- Rebuild the Space feed or AIGC provider stack around new backend APIs.
- Remove every local seed model from test or preview-only containers.
- Introduce a broader backend contract change beyond what the Android client already consumes for auth, contacts, bootstrap, history, and realtime messaging.

## Decisions

1. **Make IM bootstrap session-driven in the authenticated app shell.**
   - The messaging layer will use the encrypted `SessionStore` token/base URL when present and only rely on development-session bootstrap in explicitly supported debug validation cases.
   - This avoids shipping a release app that silently impersonates a default development user such as `nox-dev`.

2. **Drive contact state from live backend bootstrap data instead of seed repositories.**
   - The app already receives contacts and conversation bootstrap data from the IM backend. The release path should reuse that source of truth rather than maintaining a separate seed-only contacts repository for authenticated users.
   - This keeps the Contacts and Messages tabs aligned around the same backend identity/session state.

3. **Keep single-origin endpoint derivation as the public configuration contract.**
   - Release builds will ship with the deployed IM backend origin bundled through build config, and runtime code will derive WebSocket endpoints from that origin.
   - Debug-only overrides remain acceptable, but the release app must not depend on manual HTTP/WebSocket entry before login.

4. **Limit the scope to core IM release readiness.**
   - Feed, Space, and AIGC can remain separate follow-on work unless they actively interfere with auth or IM. This keeps the fix focused on delivering a usable server-connected APK quickly and safely.

## Risks / Trade-offs

- **[Risk] Debug validation flows may regress when moving bootstrap off `issueDevSession` by default.** → Mitigation: keep explicit debug/test-only development-session support and cover it with targeted unit/instrumentation tests.
- **[Risk] Contacts and conversations can drift if they keep separate live sources.** → Mitigation: derive release contact state from the same backend bootstrap/session lifecycle that powers live messaging.
- **[Risk] Existing local data or preferences may contain legacy IM endpoint keys.** → Mitigation: preserve existing backend-origin migration logic and continue normalizing legacy endpoint preferences into the single-origin format.
- **[Risk] GitHub tag builds may still publish the wrong behavior if the fix is only local.** → Mitigation: verify the relevant runtime wiring is committed, pushed, and then exercise the remote tag workflow against the updated commit.
