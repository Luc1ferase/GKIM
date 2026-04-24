## Why

The current GKIM repository already has a real Android IM client, realtime backend, and configurable AI tooling, but the product contract is still split between social IM and creator-oriented AIGC surfaces. We need a dedicated branch-level product pivot now so this codebase can become an AI companion conversation IM app instead of continuing as a general-purpose human-first IM shell.

## What Changes

- **BREAKING** Reposition the branch from a human-first social IM product to an AI companion-first conversation product.
- Introduce a dedicated AI companion capability covering persona identity, relationship continuity, bounded memory, response lifecycle, and safety boundaries.
- Modify the Android app contract so the primary conversation experience, inbox emphasis, and user controls prioritize AI companion dialogue rather than human-only contact workflows.
- Modify the backend contract so AI companion turns, memory summaries, and streamed reply lifecycle are orchestrated server-side and recovered through the existing durable/realtime IM transport.
- Reuse the current Android-native, realtime IM, and release-automation foundation instead of starting a greenfield app on a new repository.

## Capabilities

### New Capabilities
- `ai-companion-experience`: Defines AI companion personas, relationship continuity, bounded memory, safety, and conversation expectations for the branch’s new product direction.

### Modified Capabilities
- `core/im-app`: The Android app requirements change so AI companion conversations become a first-class product surface across the inbox, chat detail, and settings flows.
- `im-backend`: The backend requirements change so companion conversation state, memory, and reply lifecycle are durably orchestrated and delivered over the existing IM infrastructure.

## Impact

- Affected code: Android navigation, messages/chat/settings surfaces, repositories, model/state layers, and any retained AIGC/provider controls that become companion-oriented.
- Affected backend: conversation models, persistence, realtime event types, orchestration services, and provider integration for server-side companion turns.
- Affected specs: new `ai-companion-experience`, plus deltas for `core/im-app` and `im-backend`.
- Affected product direction: this branch becomes the foundation for an AI companion IM app while preserving the existing repo’s Android and realtime delivery base.
