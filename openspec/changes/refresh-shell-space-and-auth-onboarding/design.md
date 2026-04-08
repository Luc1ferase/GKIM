## Context

The Android app currently assumes a prototype shell: first run drops directly into the authenticated surfaces, the default locale/theme remain English and dark, Settings is one long vertically stacked page, Space and Workshop are separated into different navigation destinations, and IM identity still depends on development-only bootstrap users. The requested change turns that prototype shell into a production-style entry experience while also introducing a first real account system and a content/navigation cleanup.

Two constraints matter immediately. First, the request spans several subsystems that can easily become a tangled "big bang" rewrite if we do not phase it intentionally. Second, the repository now enforces per-task verification, scoring, evidence capture, commit, and push gates, so the implementation plan must break this work into slices that can land independently without leaving the app half-usable.

The provided onboarding materials under `docs/stitch-design/welcome_screen` establish the intended visual direction: a high-key, architectural light-mode welcome surface with explicit `注册` / `登录` actions and a video-led first impression. The backend already provides Rust/PostgreSQL IM infrastructure, so the safest way to introduce accounts is to layer a simple authenticated account/session model into that backend instead of bolting on a separate identity service.

## Goals / Non-Goals

**Goals:**
- Gate first use of the app behind a real welcome, registration, and login flow.
- Introduce persistent account identity and account-ID-based contact adding.
- Convert the current Settings experience into a menu-style structure and change first-run defaults to Chinese plus light theme.
- Merge Workshop discovery into the Space surface and render mixed Space/prompt content using one waterfall-style browsing pattern.
- Remove unread-count bubble badges from message-shell surfaces after incoming messages arrive.
- Keep the implementation phased so shell refresh work can land before the full backend-backed account migration is finished.

**Non-Goals:**
- Full social graph features such as contact requests, blocking, profile editing, or account recovery flows beyond the minimum needed to register and log in.
- Third-party identity providers, phone-number OTP, or email verification in this milestone.
- Replacing the existing IM message timeline layout wholesale; this change only removes message-shell bubble badges, not chat message bubbles.
- A full redesign of every non-requested tab or feed card beyond what is necessary to support the new information architecture.

## Decisions

### 1. Implement this change in three vertical phases instead of one cut-over
Phase 1 will refresh the shell surfaces that do not require backend auth completion yet: default locale/theme, settings menu structure, and Space/Workshop information architecture. Phase 2 will add the welcome/auth gate and Android account-session plumbing. Phase 3 will replace the current development-only backend identity path with real account-backed registration, login, session, and add-by-ID flows.

Why this decision:
- It keeps reviewable slices small enough for the repository's acceptance workflow.
- The shell refresh can proceed while backend account APIs are still being introduced.
- It lowers rollback risk because the riskiest change, auth migration, lands after the app shell structure is already stabilized.

Alternatives considered:
- One big bang across app and backend: rejected because it would be hard to verify and recover from partial failures.
- Separate unrelated changes for each UI tweak: rejected because the welcome/auth gate and account identity affect the shell contract and should be designed together.

### 2. Use a simple account ID + password model for the first account milestone
The first account system will use a unique account ID plus password as the registration/login credential pair. Display name remains editable product identity, but account ID is the canonical add-by-ID and login identifier.

Why this decision:
- It is the smallest self-contained credential model that satisfies registration, login, and add-by-ID.
- It avoids external infrastructure such as SMS, email, or OAuth while still replacing the development-session shortcut.
- It keeps the backend ownership local to the existing Rust/PostgreSQL service.

Alternatives considered:
- Phone/email verification first: rejected because it adds external dependencies and onboarding complexity before the core account model exists.
- Continue using development external IDs as permanent accounts: rejected because those IDs are seeded backend shortcuts, not a user-facing account system.

### 3. Interpret "message bubble" removal as unread badge bubble removal on shell surfaces
For this change, "收到消息之后不再显示气泡" is interpreted as removing unread-count bubble badges from the Messages shell and related app chrome, not as removing the core message-bubble layout inside chat detail.

Why this decision:
- The current chat timeline is still message-bubble based and remains the clearest reading model for IM.
- The request sits alongside shell/navigation cleanup items, which makes unread badge bubble noise the more likely target.
- This interpretation yields a contained UI change that does not conflict with the existing chat-reading model.

Alternatives considered:
- Removing chat message bubbles entirely: rejected because it would require a separate message-timeline redesign and would materially change chat readability.

### 4. Convert Settings into a menu entry surface with dedicated detail routes
Instead of one scroll-heavy settings page, the app will expose a settings menu landing page with grouped entries such as Appearance/Language, AI Provider, IM Validation/Debug, and Account. Selecting an entry opens a focused detail page or subsection.

Why this decision:
- It matches the requested "menu form" more closely than accordions inside the same page.
- It scales better once account settings and auth session actions are introduced.
- It reduces the cognitive load of the current stacked control wall.

Alternatives considered:
- Keep one page and restyle it to look like menu cards: rejected because it preserves the same interaction density.
- Move settings into a bottom-tab destination: rejected because settings is still secondary to the main content surfaces.

### 5. Merge Workshop into Space through one normalized waterfall feed model
Space will become the single discovery destination. The existing top row will absorb Workshop-related discovery under `为你推荐` and `提示工程`, and both post content and prompt/workshop cards will be rendered through a shared waterfall-style browsing pattern with filter-driven content composition.

Why this decision:
- It fulfills the request to merge Workshop into Space without leaving a duplicate secondary route.
- It gives prompt discovery the same browsing cadence as the rest of Space instead of forcing a context switch.
- It keeps future feed/prompt blending extensible by normalizing them into one card feed model.

Alternatives considered:
- Keep Workshop as a dedicated page and only deep-link from Space: rejected because it does not actually merge the experience.
- Convert all Space content to prompt-only cards: rejected because it would throw away the existing editorial/developer feed character.

### 6. Use a root auth state machine to choose Welcome vs Main Shell at app startup
The app root will resolve startup into one of three states: loading, unauthenticated welcome, or authenticated main shell. The welcome experience will use the provided design direction and bundled video asset while the authenticated shell preserves the existing three-tab structure.

Why this decision:
- It gives the cleanest separation between pre-auth and post-auth navigation.
- It avoids leaking authenticated surfaces before the user signs in.
- It allows returning authenticated users to skip the welcome entry automatically.

Alternatives considered:
- Show welcome as a modal overlay above the main shell: rejected because the main shell should not exist for unauthenticated users.
- Put login/register inside Settings: rejected because it does not satisfy first-run gating.

### 7. Keep a temporary development bypass seam during the migration
The design will preserve a debug/development seam long enough to let Android and backend slices land independently, but the intended production contract is account-backed auth only.

Why this decision:
- It reduces coupling between app shell work and backend account rollout.
- It preserves local/emulator progress if backend auth is mid-flight.
- It creates a safer rollback path for early implementation slices.

Alternatives considered:
- Remove all development identity paths immediately: rejected because it increases delivery risk while the auth migration is still incomplete.

## Risks / Trade-offs

- [The request mixes multiple product areas] → Phase the work and keep each task aligned to one vertical slice with explicit verification.
- [Account auth could stall shell delivery] → Land shell/menu/feed changes before enforcing the final backend auth cut-over.
- [The "bubble" interpretation may differ from product intent] → Encode the unread-badge interpretation clearly in the specs and revisit only if human review says the target was chat bubbles instead.
- [Welcome video assets may be heavy for app startup] → Prefer bundled/compressed local playback with a static-image fallback strategy in implementation.
- [Merging Workshop into Space can blur content identity] → Keep explicit filter labels and distinct card metadata even when the feed layout is shared.
- [A menu-based settings restructure increases navigation depth] → Group entries by user intent and keep the number of top-level menu destinations small.

## Migration Plan

1. Introduce the shell defaults and menu/feed information architecture changes without requiring the final backend auth migration yet.
2. Add the Android auth state machine, welcome route, and local account session persistence shape.
3. Add backend account tables, registration/login/session endpoints, and add-by-ID contact behavior.
4. Switch IM bootstrap/session flows from development-only identities to account-backed auth tokens.
5. Remove or hide the temporary development bypass from normal startup once account-backed auth is verified.

Rollback strategy:
- Keep the existing development-session IM path available behind a debug-only seam while the account-backed flow stabilizes.
- Land the welcome/auth gate only after the shell still works with a restored fallback path if backend auth fails during rollout.

## Open Questions

- Should add-by-ID create an immediate contact on success, or should it create a pending request that the other user must accept?
- What is the minimum registration payload beyond account ID + password: display name only, or also avatar seed/profile metadata?
- Should the welcome video be bundled directly into the Android app package, or should the implementation use a still-image fallback on lower-end devices by default?
