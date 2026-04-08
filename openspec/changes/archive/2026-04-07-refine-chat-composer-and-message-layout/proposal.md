## Why

The current Android chat and messages surfaces satisfy the initial scaffold requirements, but they still place too much visual weight on the unread summary card and treat AIGC controls as a primary composer instead of a secondary action set. This change aligns the app with the approved IM interaction model so the core chat experience feels closer to familiar mobile messengers while preserving the existing Aether visual language.

## What Changes

- Rebalance the Messages screen so the conversation list remains the primary focus and the unread summary becomes supporting information instead of the dominant card.
- Refine the chat header so the back affordance uses the compact `<` glyph and sits inline with the contact identity rather than using a separate "Back" pill and "Active Room" eyebrow block.
- Replace the chat-level AIGC action panel with a standard text composer that keeps the send action beside the input field and moves AIGC actions plus media pickers into a secondary `+` menu.
- Update chat message presentation so avatars appear before message bubbles and sender labels sit above the bubbles for clearer conversational hierarchy.
- Preserve the existing workshop and AIGC capabilities while relocating their entry points into the secondary composer menu.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: refine the Messages and Chat requirements so the mobile IM shell uses a secondary-action composer pattern, lighter unread summary placement, compact back affordance, and avatar-plus-name message hierarchy.

## Impact

- Affected code: Android Compose screens and shared UI components under `android/app/src/main/java/com/gkim/im/android/feature/messages`, `android/app/src/main/java/com/gkim/im/android/feature/chat`, and `android/app/src/main/java/com/gkim/im/android/core/designsystem`.
- Affected specs: `openspec/specs/core/im-app/spec.md`.
- Affected tests: Compose UI coverage for the Messages screen, chat composer, and chat message layout.
- No backend or provider API changes are required; this change is limited to client-side structure, interaction, and acceptance criteria.
