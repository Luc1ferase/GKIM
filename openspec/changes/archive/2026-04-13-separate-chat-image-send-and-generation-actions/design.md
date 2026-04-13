## Context

The Android chat screen currently mixes three different image intents inside one secondary `+` menu: picking media for image-to-image, picking media as a normal chat attachment, and reviewing a generated image after success. In practice this creates two product problems. First, `Pick image` is ambiguous because users cannot tell whether the photo will be sent to the conversation or staged as AIGC input. Second, the generation result card ends at preview, so even a successful image has no clear next step for saving or sharing. The current implementation also keeps only one `selectedMedia` draft, which makes image-to-image state and chat-attachment state easy to conflate.

This change touches chat UI state, message attachment behavior, AIGC task affordances, and provider capability gating, so it benefits from an explicit design before implementation.

## Goals / Non-Goals

**Goals:**
- Split chat image attachment flow from image-to-image source selection so each action has a single purpose.
- Make image-to-image availability truthful by requiring an explicit source image and only exposing the action when the active provider can actually run it.
- Add clear post-generation actions for successful images, starting with saving locally and sending the generated result into the active conversation.
- Keep the changes inside the existing Android chat/AIGC architecture instead of inventing a second image workflow surface.

**Non-Goals:**
- Do not redesign the broader chat layout or bottom-sheet/navigation architecture beyond what is needed to separate image intents.
- Do not add cloud gallery sync, multi-image batch management, or cross-conversation forwarding in this change.
- Do not broaden provider support beyond the current preset/custom provider matrix.

## Decisions

### 1. Split composer image actions by intent instead of sharing one picker state

The `+` menu will expose separate affordances for:
- sending an image message to chat
- choosing an image-to-image source
- choosing a video-to-video source where supported

The chat route/state layer will track chat-attachment draft media separately from generation-source draft media. This avoids the current bug where one picked image can silently stand in for two different jobs.

Why this approach:
- It matches the user mental model: "send this photo" is different from "use this photo as generation input."
- It keeps ambiguity out of the picker flow instead of trying to infer user intent after selection.
- It minimizes repository churn because the separation can happen at the chat feature state boundary.

Alternatives considered:
- Keep one picker and ask the user after image selection what they meant. Rejected because it still causes accidental wrong-mode picks and adds a late correction step.
- Auto-route based on the last tapped AIGC mode. Rejected because it is too implicit and makes the UI harder to reason about during demos.

### 2. Gate image-to-image on explicit source readiness, not just provider capability metadata

The visible secondary menu should still reflect provider capabilities, but image-to-image also needs local input readiness. The design will therefore distinguish:
- provider supports image-to-image
- user has selected a generation source image

The UI should show the image-to-image action as unavailable or route users first to "choose generation source image" until a source is present. This keeps failures from surfacing only after a network request begins.

Why this approach:
- The current capability-only gating is necessary but insufficient.
- It makes the unsupported/unready state truthful before execution.
- It provides a clearer answer to the current "图生图似乎不可用" complaint.

Alternatives considered:
- Let the user tap image-to-image and fail inline if no source image exists. Rejected because it feels broken rather than guided.

### 3. Treat generated images as actionable result objects

Successful image generations should expose follow-up actions directly on the result card. The initial actions will be:
- save to local device storage
- send to the current conversation as an outgoing image attachment

The send action should reuse the existing conversation message/attachment model instead of inventing a parallel "share generated result" channel. Saving locally should use the Android storage path appropriate for exported media rather than requiring the user to screenshot the result.

Why this approach:
- It turns the generation result from a dead-end preview into a usable artifact.
- It builds on existing message attachment rendering instead of creating another result surface.
- It matches the user's stated next-step expectations without expanding scope into full asset management.

Alternatives considered:
- Only add a copy-link action. Rejected because it does not satisfy the save/send requirement.
- Auto-send successful generations to chat. Rejected because generation and message sending are distinct user intents.

## Risks / Trade-offs

- [More composer state can increase UI complexity] → Mitigation: keep the split draft state local to chat feature models with explicit names for chat attachment vs generation source.
- [Saving remote image URLs locally may fail on transient network/download issues] → Mitigation: surface explicit save success/failure feedback and keep the original result card intact.
- [Sending generated images back into chat could duplicate visual content with the existing result card] → Mitigation: treat the send action as an explicit second step and keep the timeline/result-card distinction clear.
- [Provider-specific image-to-image limitations may still differ] → Mitigation: drive action availability from both provider metadata and per-flow prerequisites, with focused regression tests around ready/unready states.

## Migration Plan

1. Split chat composer media state into normal chat attachments versus generation source media.
2. Update the secondary menu labels/actions so users choose the correct intent before opening the picker.
3. Tighten image-to-image availability and validation around explicit generation source readiness.
4. Add generation-result follow-up actions and wire save/send behavior into existing chat attachment/message models.
5. Refresh unit/instrumentation coverage and document verification evidence before task acceptance.

Rollback strategy:
- Revert to the current shared picker and passive generation card behavior.
- No persistent schema or backend migration is required; the change is confined to Android client behavior.

## Open Questions

- Should the generated-image "send" action insert the remote provider URL directly into the chat attachment model, or should the app download the file first and send a local cached copy?
- Should the chat composer keep a visible chip for the currently selected generation source image so users can clear or replace it before running image-to-image?
