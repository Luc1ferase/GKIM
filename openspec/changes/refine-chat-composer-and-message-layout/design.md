## Context

The Android-native scaffold already provides Messages, Chat, Workshop, and Settings routes with an Aether-themed Compose design system, but the current chat surface still behaves like an AIGC control panel instead of a messaging-first conversation view. The Messages screen also elevates the unread summary card above the conversation list, which weakens the core inbox scan pattern the user expects from an IM app.

This change must keep the existing Android stack, the three-tab shell, and the established Aether color and typography tokens. It must also preserve the existing AIGC capabilities and workshop navigation while moving them into an interaction model that feels closer to WeChat or QQ: text input first, secondary actions behind a `+` affordance, and lightweight conversation chrome.

Every implementation slice for this change will still be gated by `docs/DELIVERY_WORKFLOW.md` and `docs/QUALITY_SCORE.md`, so the design favors small composable updates that can be verified, reviewed, scored, and pushed independently.

## Goals / Non-Goals

**Goals:**
- Restore the conversation list as the primary focal area of the Messages tab.
- Redesign the chat composer so text entry and send are the primary action path.
- Move AIGC modes and local media pickers into a secondary `+` action menu without removing those capabilities.
- Present chat identity more naturally by using an inline `<` back affordance, contact name in the top row, avatars before bubbles, and sender labels above bubbles.
- Keep the change local to Android Compose screens, shared design-system primitives, and their corresponding tests and specs.

**Non-Goals:**
- No changes to backend APIs, provider configuration, media generation logic, or repository contracts.
- No redesign of Contacts, Space, Workshop, or Settings beyond preserving existing entry points.
- No introduction of a full attachment tray, emoji system, voice input, or new navigation destinations.
- No changes to the per-task review, scoring, and push workflow already defined in repository docs.

## Decisions

### 1. Make the Messages conversation list visually primary

The Messages screen will demote unread summary from a hero card to a compact supporting element so the list of conversations becomes the first scanning target. The summary can remain present near the top, but it should consume materially less vertical emphasis than the list and should not visually read as the page's main content block.

Why this decision:
- It matches the user's stated priority that unread signals should not dominate the screen.
- It aligns better with established mobile inbox behavior, where unread state supports the list instead of replacing it.
- It preserves unread visibility without sacrificing one-hand scanning of conversations.

Alternatives considered:
- Removing the unread summary entirely: rejected because the aggregate unread signal is still useful.
- Keeping the current card and only changing copy: rejected because the problem is hierarchy, not wording.

### 2. Replace the chat control panel with a standard composer plus secondary action menu

The bottom of the chat screen will center on a text input field and a send button, while AIGC actions, image picking, and video picking move into a `+` action menu. The menu should behave like a secondary action tray: hidden by default, explicitly invoked, and structured so users can access text-to-image, image-to-image, video-to-video, workshop entry, and media selection without competing with the core text-send flow.

Why this decision:
- It resolves the current mismatch where the chat input area behaves like an AIGC dashboard instead of an IM composer.
- It preserves all current AIGC entry points while making plain text messaging faster and more familiar.
- It lets the team evolve the secondary menu independently from the primary composer.

Alternatives considered:
- Keeping AIGC buttons inline beside the input field: rejected because it keeps the composer visually overloaded.
- Moving AIGC actions to a separate screen only: rejected because the user explicitly wants them available from chat, just not as the primary control block.

### 3. Collapse chat header chrome into a compact identity row

The chat screen will remove the large `PageHeader` treatment with the "Active Room" eyebrow and "Back" pill. Instead, the top of the screen will use a compact row containing `<`, contact nickname, and optional secondary actions like Workshop, with the contact subtitle handled as small supporting text only if needed.

Why this decision:
- It matches the user's request for a lighter header and a `<` back affordance.
- It reduces wasted vertical space and gives more room to the timeline and composer.
- It better fits the IM-first interaction pattern.

Alternatives considered:
- Reusing `PageHeader` with a different label: rejected because the component's spacing and hierarchy are inherently too large for this chat surface.
- Removing all top actions: rejected because Workshop access still needs a visible path.

### 4. Shift message bubbles to identity-led rows

Each chat entry will be rendered as a row-based unit where the avatar sits before the bubble and the sender label appears above the message bubble in smaller text. Directional styling can still differ for outgoing and incoming messages, but the layout must preserve clear author attribution at a glance.

Why this decision:
- It directly satisfies the user's requested avatar and name placement.
- It improves readability when AIGC results and system messages appear in the same timeline.
- It creates a more extensible message container for future status chips or attachments.

Alternatives considered:
- Keeping sender identity implicit through color and alignment alone: rejected because it is weaker for mixed-content chats.
- Showing the sender name inside the bubble: rejected because it competes with message content.

## Risks / Trade-offs

- [Secondary action menu adds state complexity] -> Keep the menu stateless outside the chat screen and back it with small, testable UI state rather than repository changes.
- [Compact header may diverge from shared header styles] -> Implement a dedicated chat-top-bar pattern instead of forcing `PageHeader` into a shape it does not fit.
- [Composer redesign can break existing UI tests] -> Update Compose tests alongside the UI change and add assertions for send input, secondary menu visibility, and supporting unread summary placement.
- [Avatar-led message rows can increase vertical density] -> Use small label text and tight spacing tokens so the timeline remains readable without becoming cramped.

## Migration Plan

1. Update the core spec delta so the target behavior is explicit before implementation begins.
2. Refactor the Messages screen hierarchy and tests to make the conversation list primary while retaining unread summary support.
3. Introduce a dedicated chat top bar, text composer, and secondary `+` action menu.
4. Refactor message row rendering to support avatar-leading layout, sender labels, and attachment continuity.
5. Verify each task slice with Compose or Gradle checks, record evidence per `docs/DELIVERY_WORKFLOW.md`, and only then mark tasks complete.

## Open Questions

- None at proposal time; the requested interaction model is specific enough to implement without additional product clarification.
