## Context

The authenticated Android shell already has a real social backbone: `UserSearchRoute` can search live users, `ImBackendClient` can send friend requests through the backend, and Contacts can accept or reject pending requests. What is missing is a high-visibility entry point in `Messages`, where the top-right header area still spends space on passive `${count} active` copy instead of actions.

The second gap is QR scanning. There is currently no QR-specific route, camera-scanning stack, or passive result presentation in the Android app. The requested behavior is intentionally limited for now: scan a QR code, decode it, show the content, and stop there. That keeps this change product-useful without prematurely locking the app into a deep-link, auto-add-friend, or navigation contract.

This change is cross-cutting across shell chrome, navigation, and social entry points, but it should avoid inventing a second friend-request workflow. The existing backend contract already covers real user search and friend-request lifecycle, so the design should reuse that path and reserve backend work for validation only if gaps appear during implementation.

## Goals / Non-Goals

**Goals:**
- Replace the Messages header count text with a compact `+` quick-action trigger.
- Expose `Add friend` and `Scan QR code` from a dropdown anchored to that trigger.
- Route `Add friend` into a real backend-backed user-search/request flow, not a mocked modal.
- Add a QR scanning route that decodes and displays the payload content without auto-navigation or side effects.
- Add validation coverage for the new menu behavior and for the real add-friend entry path.

**Non-Goals:**
- Do not redesign the broader Contacts social model or replace the existing pending-request handling.
- Do not introduce new friend-request backend endpoints unless implementation proves the current contract is insufficient.
- Do not make QR content trigger login, navigation, add-friend, or shell actions in this change.
- Do not define a permanent QR payload schema beyond “decoded content can be shown safely to the user.”

## Decisions

### 1. Reuse the existing authenticated user-search flow for `Add friend`

The Messages quick action will navigate into the existing authenticated social discovery flow rather than creating a second add-friend form or a front-end-only bottom sheet. This keeps one canonical request path, ensures the action remains backed by the current `searchUsers` and `sendFriendRequest` APIs, and avoids duplicating relationship-state logic.

Alternatives considered:
- Build a new add-friend modal inside Messages. Rejected because it would duplicate the existing search/request logic and create two competing social entry points to maintain.
- Add a purely local demo action. Rejected because the user explicitly wants cross-account friend add to remain real.

### 2. Use a header-anchored overflow-style `+` menu instead of persistent action chips

The `+` affordance belongs in the same visual slot currently occupied by the passive conversation-count copy. A compact anchored menu preserves the existing header rhythm, leaves the list as the primary content, and scales better if more quick actions are added later.

Alternatives considered:
- Floating action button. Rejected because it would add a second visual focal point and compete with conversation rows.
- Two always-visible pills (`Add friend`, `Scan QR`). Rejected because they take more horizontal room and crowd the shell heading.

### 3. Introduce a dedicated QR scan route plus a passive result surface

Scanning should happen on a dedicated secondary route so camera lifecycle, permission handling, and decode state stay isolated from `MessagesRoute`. After a successful decode, the app should present the raw scanned content in an explicit result surface with copy/read affordances, but no automatic redirect or mutation.

Alternatives considered:
- Decode and immediately route based on payload type. Rejected because the product contract is not mature enough and the user explicitly asked for display-only behavior.
- Fold scanning into a transient dialog above Messages. Rejected because camera preview and permission flows are easier to manage in a dedicated screen.

### 4. Use an embedded Android camera/barcode stack rather than an external scanner handoff

The implementation should prefer an in-app scanning stack suited to Compose navigation, such as CameraX preview with on-device barcode decoding, so the result can stay inside GKIM's navigation model and testing harness. This also keeps the UI under the app's control instead of relying on an external scanner activity.

Alternatives considered:
- Launch an external scanner intent. Rejected because it weakens UI consistency and makes result handling less deterministic.
- Defer scanning entirely and only add a placeholder page. Rejected because the user specifically wants QR content to display normally now.

## Risks / Trade-offs

- **Camera permissions and emulator variability** → Mitigation: keep scanning isolated in one route, define explicit permission-denied and scan-cancelled states, and use focused device/emulator validation instead of assuming parity.
- **Messages loses at-a-glance conversation-count copy** → Mitigation: preserve list-first layout and rely on row metadata plus unread counts, since the new requirement prioritizes actionability over passive header metrics.
- **Two social entry points (Messages and Contacts) could feel redundant** → Mitigation: reuse the same discovery/request route so the app has multiple entry points but only one actual social workflow.
- **QR payloads may contain unsafe or confusing content** → Mitigation: treat decoded data as inert text in this phase and avoid any auto-open behavior.

## Migration Plan

No backend schema migration is planned. The rollout is Android-shell-only:
1. Replace the Messages header trailing content with the new `+` menu.
2. Route `Add friend` to the existing discovery/request surface.
3. Add QR scan and QR result routes plus any required Android camera/decode dependencies.
4. Validate with focused UI coverage and a real multi-account friend-request flow against the local backend.

Rollback is straightforward: revert the Messages header/menu change and remove the QR routes/dependencies. Existing Contacts-based friend discovery remains the fallback path.

## Open Questions

- Should the first QR result surface include a copy button in addition to plain display, or is read-only display enough for the first release?
- Do we want the `Add friend` action to land on the existing `Find People / 搜索用户` page unchanged, or should that screen receive lightweight copy updates so it feels intentionally launched from Messages?
