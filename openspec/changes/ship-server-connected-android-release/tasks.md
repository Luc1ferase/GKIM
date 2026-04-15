## 1. Release IM Wiring

- [x] 1.1 Replace shipped IM bootstrap defaults so authenticated runtime uses the stored backend session instead of defaulting to a development-session user.
- [x] 1.2 Wire bundled IM backend origin and runtime endpoint resolution so the release app derives its HTTP/WebSocket targets from one server origin without placeholder endpoints.

## 2. Live Contacts And Conversations

- [x] 2.1 Replace authenticated seed-first Contacts/Messages wiring with backend-backed contact and conversation state sourced from the real IM session lifecycle.
- [x] 2.2 Remove or isolate development-only IM validation defaults from release-facing settings and startup behavior without breaking debug/test validation flows.

## 3. Verification And Release Handoff

- [x] 3.1 Add or update targeted tests covering session-driven IM bootstrap and release endpoint resolution, then run the relevant Android verification commands.
- [x] 3.2 Create a clean commit, push the branch, trigger the remote APK build path, and monitor the result until the release compile outcome is known.
