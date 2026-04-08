## Context

The current Android client already has a bottom-anchored chat composer, denser outgoing bubbles, a three-tab shell, and a dedicated Settings screen for AI provider configuration. However, several presentation and preference behaviors still feel split across surfaces: unread state is called out on Messages instead of the more discovery-oriented Space tab, Contacts sorting still reads like a development control instead of a polished mobile filter, and Settings does not yet own app-level language or theme preferences. In chat detail, the most recent metadata-density change moved incoming timestamps into the header rhythm, but the new product direction intentionally supersedes that choice and wants the other person's timestamp back in the lower-right area of the incoming bubble.

This change touches multiple top-level screens plus shared preference/theme infrastructure, so the design needs to keep UI behavior consistent while preserving the current Android-native architecture, Aether visual language, and per-task delivery gates in `docs/DELIVERY_WORKFLOW.md`.

## Goals / Non-Goals

**Goals:**
- Move incoming message timestamps into the lower-right area of incoming bubbles while preserving compact message density.
- Add app-level language selection for Chinese and English and an explicit light/dark theme switch in Settings.
- Relocate the aggregate unread summary from Messages to Space so Messages can foreground the conversation list.
- Simplify the Messages screen chrome so `Recent conversations` becomes the first visible heading on the page.
- Replace the Contacts horizontal sort chip strip with a single dropdown affordance that still exposes the same deterministic sort modes.
- Keep the resulting tasks small enough to verify, review, score, and push independently.

**Non-Goals:**
- No backend, API, or database changes.
- No new tabs, navigation model changes, or feed-content redesign.
- No third language, system-follow theme mode, or full Android resource localization rewrite in this change.
- No changes to existing AI provider semantics beyond reorganizing the Settings surface.

## Decisions

### 1. Treat language and theme as shared app preferences, not screen-local state

Language and theme selection should be stored alongside the existing DataStore-backed preferences so they can be observed from the app root and from Settings without bespoke plumbing. `GkimTheme` and the app shell should read a shared preference-backed UI settings stream, while the Settings screen becomes the editing surface for those values.

Why this decision:
- Theme and language affect multiple screens and must survive process restarts.
- The project already has a `PreferencesStore` abstraction, so extending it keeps the architecture consistent.
- It avoids hiding app-wide behavior inside a single screen ViewModel.

Alternatives considered:
- Keep theme and language as `remember` state inside Settings: rejected because changes would not persist or propagate reliably.
- Implement full Android locale/resource overrides immediately: rejected because that is a larger migration than this UI-focused change needs.

### 2. Use a lightweight app-copy layer for Chinese/English instead of a full string-resource migration

The current Android UI uses hard-coded Compose strings broadly. For this change, the implementation should introduce a focused language abstraction for app-facing copy on the affected surfaces rather than attempting to migrate the whole project to Android string resources in one pass. The design should still ensure the selected language flows through Messages, Contacts, Space, Settings, and chat strings touched by this work.

Why this decision:
- It satisfies the user-visible requirement without forcing a repository-wide localization rewrite in one change.
- It keeps implementation slices reviewable and lowers migration risk.
- It can evolve into a fuller resource-based localization pass later.

Alternatives considered:
- Migrate all UI text to localized XML resources now: rejected as too broad for this scoped refinement.
- Limit language switching to Settings labels only: rejected because it would feel incomplete and misleading.

### 3. Move aggregate unread state into Space as a supporting summary panel

Unread totals should no longer occupy the top support card on Messages. Instead, Space should host a compact unread summary card near the top, while Messages should start with the conversation heading and list content. Messages rows still keep per-conversation unread badges.

Why this decision:
- It directly follows the requested information hierarchy.
- It preserves unread visibility without competing with the conversation list.
- It gives Space a stronger cross-surface status role without changing its primary feed identity.

Alternatives considered:
- Remove aggregate unread state entirely: rejected because the user asked for relocation, not deletion.
- Keep a tiny unread badge in Messages and duplicate it in Space: rejected because duplication would reintroduce visual clutter.

### 4. Use denser bubble footers for both directions, with incoming timestamps returning inside the bubble

The chat timeline should converge on a consistent bubble-footer model: outgoing timestamps stay in the lower-right footer area, and incoming timestamps should move from the header line back into the incoming bubble's lower-right region. Incoming identity remains readable through avatar plus sender label above the bubble, but time becomes secondary metadata again.

Why this decision:
- It matches the new requirement even though it intentionally reverses the latest incoming-header experiment.
- It reduces header clutter and keeps time secondary to message content.
- It creates a more symmetrical message rhythm across directions.

Alternatives considered:
- Keep incoming timestamps on the header right: rejected because it conflicts with the new requirement.
- Remove incoming sender labels once timestamps move down: rejected because the current identity treatment is still useful and already specified.

### 5. Replace the Contacts sort chip row with a single dropdown control

The Contacts screen should keep the existing sort modes, but the control should become a single dropdown-like affordance inside the sort card, aligned to the right side of the bubble. This keeps the control compact and avoids turning the top of Contacts into a row of equal-weight buttons.

Why this decision:
- It matches the requested mobile interaction model.
- It preserves deterministic sort behavior while reducing visual noise.
- It fits naturally with the existing `PreferencesStore.contactSortMode`.

Alternatives considered:
- Keep the current chip row and only restyle it: rejected because the interaction pattern would still feel horizontally noisy.
- Move sorting into a separate modal sheet: rejected as unnecessary complexity for three options.

## Risks / Trade-offs

- [Lightweight language abstraction may leave some older screens in English longer than others] -> Scope the requirement and tests to the affected user-facing surfaces in this change, and keep the abstraction extensible for future coverage.
- [Theme switching may expose weak contrast assumptions in the existing dark-biased Aether tokens] -> Introduce a parallel light palette deliberately and cover key screen smoke tests in Compose UI.
- [Moving unread summary to Space could make Messages feel visually sparse] -> Preserve the `Recent conversations` heading and conversation badges so Messages still has a clear top anchor.
- [Reversing incoming timestamp placement could conflict with the just-completed metadata-density change] -> Encode the new behavior explicitly in the delta spec and geometry tests so the override is intentional and traceable.

## Migration Plan

1. Update the `core/im-app` delta spec so chat timestamps, Settings preferences, Space unread summary, Messages chrome, and Contacts sort behavior all have explicit requirements.
2. Add failing or updated UI/repository coverage for chat timestamp placement, language/theme settings, unread-summary relocation, and dropdown sort interaction.
3. Implement shared preference plumbing for language and theme, then wire the affected screens to the new settings and layout behavior.
4. Run targeted and full regression verification, record review/upload evidence per task, and leave the change apply-ready for archive only after every task reaches the required quality gate.

## Open Questions

- None. The implementation can proceed using the current Aether design language, the existing DataStore preference layer, and the established review-gated delivery workflow.
