## 1. Split image intent flows in chat

- [x] 1.1 Refactor the Android chat composer state so normal chat image attachments and generation-source media are tracked separately instead of sharing one `selectedMedia` draft.
- [x] 1.2 Update the chat `+` menu labels and actions so users explicitly choose between sending an image message, choosing image-to-image source media, and choosing video-to-video source media before the picker opens.

## 2. Make image-to-image readiness truthful

- [x] 2.1 Tighten the chat/AIGC interaction logic so image-to-image only appears or runs when the active provider supports it and an explicit generation-source image has already been selected.
- [x] 2.2 Fix or refresh the provider request path and chat feedback needed for image-to-image so missing source media, unsupported provider paths, and real provider failures all surface truthfully.

## 3. Add generated-image follow-up actions

- [x] 3.1 Extend the successful generated-image result card with explicit follow-up actions to save the image locally or send it into the current conversation.
- [x] 3.2 Reuse the existing message attachment flow so the "send generated image" action inserts a normal outgoing image message instead of creating a second special-case share path.

## 4. Verification and acceptance

- [x] 4.1 Add or refresh Android unit/UI coverage for split image-picking intents, image-to-image gating, and generated-image follow-up actions.
- [x] 4.2 Run focused local verification for normal image send, image-to-image generation, and generated-image save/send flows, then record verification/review/score/upload evidence in `docs/DELIVERY_WORKFLOW.md`.
