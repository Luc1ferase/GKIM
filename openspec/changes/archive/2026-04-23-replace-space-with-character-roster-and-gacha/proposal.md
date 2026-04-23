## Why

On the AI companion branch, the current `Space` tab is still shaped like a developer-content and prompt-discovery feed, which no longer matches the product direction. We need to turn that surface into a tavern-style role-selection and card-draw entry point so users can choose preset companions or draw character cards before entering AI companion conversations.

## What Changes

- **BREAKING** Replace the current `Space` product surface with a tavern-style character roster and draw flow.
- Introduce a dedicated companion character roster capability covering preset role cards, draw pools, owned cards, and active role selection.
- Modify the Android app contract so the third primary tab becomes a role-selection / draw surface instead of a developer-content feed.
- Modify the backend contract so preset character catalogs, draw results, owned roster state, and active companion selection are durable and can drive the conversation experience.
- Keep the scope centered on tavern-like companion selection and draw behavior; do not turn this slice into a monetized marketplace or a full user-generated card editor.

## Capabilities

### New Capabilities
- `companion-character-roster`: Defines tavern-style preset role cards, draw behavior, owned roster state, and active companion selection for the AI companion branch.

### Modified Capabilities
- `core/im-app`: The Android app requirements change so the current `Space` tab becomes a role-selection and draw surface that leads into companion conversations.
- `im-backend`: The backend requirements change so character catalogs, draw outcomes, and active companion selection are durable server-side state.

## Impact

- Affected code: Android `feature/space` surfaces, navigation/tab labels, chat-entry affordances, model/repository layers, and related tests.
- Affected backend: companion persona catalog APIs, owned-character persistence, active selection state, and conversation startup/orchestration.
- Affected specs: new `companion-character-roster`, plus deltas for `core/im-app` and `im-backend`.
- Affected UX: users will enter a tavern-like companion lobby where they can browse preset角色、抽取角色卡、管理已持有角色并选择当前陪伴对象。
