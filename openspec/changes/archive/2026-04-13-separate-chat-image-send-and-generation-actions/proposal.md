## Why

The current chat image flow overloads one picker and one generation card for too many jobs: image-to-image is unreliable, `Pick image` can mean either "attach this to chat" or "use this as AIGC input", and generated images stop at preview without obvious next actions. We need to separate these user intents now so chat media behavior becomes trustworthy for demos and real usage instead of feeling ambiguous or half-finished.

## What Changes

- Separate the chat composer's "send a normal image message" flow from the "pick an image as image-to-image input" flow so users choose intent explicitly before selection.
- Tighten the image-to-image request path so supported providers only expose it when the required media and provider contract are actually ready.
- Add post-generation actions on successful image results so users can save to local storage or send the generated image into the current conversation.
- Update the chat UI copy, menu structure, and generation-result affordances so each image action has one clear purpose and next step.
- Add focused tests for split image-picking flows, image-to-image gating, and generated-image follow-up actions.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: refine chat media picking, image-to-image eligibility, and generated-image follow-up behavior in the Android IM app.

## Impact

- Affected code: Android chat Compose surfaces, media picker integration, AIGC repository/task handling, and message attachment flows under `android/app/src/main/java/com/gkim/im/android/feature/chat`, `android/app/src/main/java/com/gkim/im/android/data/repository`, and related media/model packages.
- Affected specs: `openspec/specs/core/im-app/spec.md` via requirement updates for chat composer actions and AIGC result handling.
- Affected tests: Android unit and instrumentation coverage for chat composer behavior, generation cards, and media-action branching.
- Affected user experience: chat users will explicitly choose whether an image is being attached as a message, used as an image-to-image source, or handled as a generated result after creation.
