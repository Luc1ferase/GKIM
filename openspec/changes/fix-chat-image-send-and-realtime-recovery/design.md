## Context

The current Android chat stack splits text messaging and attachment handling in a way that makes user-to-user image sends impossible. `LiveMessagingRepository.sendMessage(...)` immediately returns after `upsertLocalOutgoingMessage(...)` whenever an attachment is present, so attachment-bearing messages never reach the backend or the realtime gateway. On the backend side, the only live direct-message command is WebSocket `message.send`, `ImService::send_direct_message(...)` requires a non-empty text body, and `MessageRecord` / `MessageRecordDto` do not expose attachment metadata.

Realtime delivery is also brittle after the initial session bootstraps successfully. `RealtimeChatClient` does not send heartbeats, does not reconnect automatically on `onClosed` / `onFailure`, and the repository only retries after an outbound text send fails. If the socket dies while the user is idle, inbound messages stop until a full bootstrap or app/page restart occurs. Even after reconnect, loaded conversations are treated as fresh forever because `loadedConversationIds` suppresses any subsequent history refresh.

## Goals / Non-Goals

**Goals:**
- Deliver 1:1 image messages through a real backend contract so both sender and recipient see the same durable attachment in history, bootstrap, and live events.
- Keep text-message delivery behavior intact while adding attachment descriptors to the shared backend and Android message model.
- Recover realtime sessions automatically after disconnects and resynchronize any loaded conversation state without requiring the user to exit and re-enter the app.
- Reuse the same send path for normal gallery images and generated images so the chat UI no longer has a local-only attachment branch for backend conversations.

**Non-Goals:**
- Multi-attachment messages, video message sending, or arbitrary file uploads.
- Replacing the existing text-message WebSocket send path for normal text chat.
- Introducing external object storage in this change; the first slice should remain deployable with repository-managed backend infrastructure.
- Redesigning AIGC generation itself beyond handing generated images off to the new image-message pipeline.

## Decisions

### 1. Send image messages over authenticated HTTP, not the existing text WebSocket command

Image messages will use a new authenticated backend HTTP endpoint dedicated to direct-image sending. The request will carry `recipientExternalId`, optional `clientMessageId`, optional caption/body text, and the encoded image payload or multipart upload. The backend will persist the message first, then fan out the resulting `message.sent` / `message.received` events through the existing `ConnectionHub`.

Why this approach:
- It avoids stretching the current text-only WebSocket `message.send` command into a binary upload channel.
- HTTP uploads are easier to size-limit, validate, retry, and test than large WebSocket frames.
- The sender still receives the same realtime event shape as text sends, so repository reconciliation stays consistent.

Alternatives considered:
- Inline base64 inside `message.send`: rejected because it would enlarge realtime frames, worsen failure handling, and couple large uploads to socket health.
- Object-storage-first upload tokens: deferred because the repo does not currently provision blob storage and the immediate problem is restoring correctness in the existing deployment shape.

### 2. Keep direct image messages as normal chat messages with an optional attachment descriptor

Attachment-bearing direct messages will remain part of the existing direct-message stream instead of introducing a brand-new chat mode. The backend message DTOs will gain an optional `attachment` object, while `body` continues to represent the visible caption text and can be empty when an attachment is present. The first backend implementation should persist attachment bytes in a dedicated attachment record keyed to the message and expose a stable descriptor in history/bootstrap/realtime payloads.

Why this approach:
- The Android `ChatMessage` model already treats attachments as an optional property rendered alongside `body`.
- Keeping the direct-message flow unified avoids special-case conversation ordering, unread-count, and receipt behavior for image messages.
- A dedicated attachment record is clearer than overloading `messages.metadata` with large opaque blobs and gives a cleaner future path to external storage.

Alternatives considered:
- New `message_kind = image`: rejected because the UI does not need a separate semantic channel for direct images, only an attachment descriptor.
- Store raw bytes directly in `messages.metadata`: rejected because it makes payload growth, validation, and future migration harder to manage.

### 3. Serve stored image attachments through backend-owned descriptors that Android can render with auth

The backend will include enough attachment metadata in `MessageRecord` payloads for the Android client to render the image in history and live events. The Android app will add an auth-aware image-loading path so Compose `AsyncImage` can request backend-hosted attachments with the current session token instead of relying on local `content://` URIs or third-party URLs.

Why this approach:
- User-to-user messages must reference a backend-resolvable resource, not a sender-local URI.
- Generated-image sends and gallery-image sends can both be normalized into the same backend attachment descriptor after upload.
- Auth-aware loading keeps IM media scoped to conversation members without forcing the DTO to embed large inline data URLs.

Alternatives considered:
- Public attachment URLs: rejected because direct-message media should remain tied to authenticated conversation access.
- Passing third-party provider URLs through unchanged for generated images: rejected because those URLs can expire or be inaccessible to recipients.

### 4. Add heartbeat, automatic reconnect, and post-reconnect resync for backend conversations

`RealtimeChatClient` will gain a lightweight heartbeat / inactivity check plus automatic reconnect with bounded backoff whenever a valid session exists and the socket closes or fails. `LiveMessagingRepository` will distinguish initial bootstrap from reconnect resync: after `session.registered` on a reconnect, it will flush pending outbound text sends, refresh conversation summaries, and force-reload any conversations that were previously loaded so missed inbound messages appear without manual restart.

Why this approach:
- The current retry path only runs after an outbound send fails, which does not help idle recipients.
- A targeted resync is safer than calling the existing full `refreshBootstrap()` reconnect path, which clears runtime state and can discard in-memory context unnecessarily.
- Reusing `message.id` / `clientMessageId` reconciliation keeps reconnect recovery idempotent even when the same event arrives after a history reload.

Alternatives considered:
- Keep the current reconnect-on-send-failure behavior: rejected because it leaves inbound delivery stale for idle users.
- Full bootstrap reset on every reconnect: rejected because it is visually disruptive and can trample pending local state more than necessary.

## Risks / Trade-offs

- [Database growth from stored image bytes] -> Enforce image-only validation, size caps, and client-side compression before upload; keep attachment storage isolated so a later storage migration is straightforward.
- [Attachment delivery now spans HTTP upload plus WebSocket fan-out] -> Use stable `clientMessageId` reconciliation and sender echo events so optimistic UI state can collapse into the durable message record.
- [Reconnect resync can duplicate or reorder timeline entries] -> Continue id-based upsert semantics and use forced history reload only after reconnect is confirmed through `session.registered`.
- [Authenticated image rendering adds token plumbing to Compose] -> Centralize attachment loading through a shared OkHttp / Coil path backed by the existing session store and cover it with payload + UI tests.

## Migration Plan

1. Add backend attachment persistence, DTO fields, authenticated image-send route, and attachment fetch route behind optional attachment fields so existing text clients stay compatible.
2. Extend backend realtime broadcasting to emit attachment descriptors for image messages and preserve existing delivery/read receipts.
3. Update Android shared IM models, backend client interface, and repository logic so backend conversations use the HTTP image-send path while fallback/local-only conversations keep current in-memory behavior.
4. Add heartbeat/reconnect/resync behavior in the realtime client and repository, then expand unit and live emulator validation coverage for reconnect recovery and image round trips.
5. Roll out with backend first, then Android. Rollback can disable new image-send entry points while leaving existing text chat intact; persisted attachments remain readable for already-upgraded clients.

## Open Questions

- What maximum post-compression image size should the Android client enforce so emulator validation stays reliable without making normal camera screenshots unusably blurry?
- Is a separate thumbnail representation needed in the first slice, or is rendering the stored original through the authenticated attachment URL acceptable for the current chat bubble layout?
