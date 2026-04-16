## Why

Users can exchange text messages today, but image sends never leave the Android client because attachments are only staged locally and the backend/WebSocket contracts still accept text-only payloads. Realtime message delivery is also fragile because the Android client does not keep the socket healthy or recover automatically after disconnects, so users often need to leave and re-enter the app before missed messages appear.

## What Changes

- Add a real direct-message attachment path so outgoing image messages are uploaded or otherwise resolved into a durable backend payload instead of stopping at local-only UI state.
- Extend IM backend message contracts, persistence, bootstrap/history payloads, and WebSocket events so direct messages can carry attachment metadata alongside text.
- Teach the Android messaging repository and realtime client to send backend-backed image messages, reconcile attachment-bearing events, and stop treating image sends as local-only placeholders.
- Add realtime connection health and recovery behavior on Android, including heartbeat/reconnect handling and post-reconnect conversation/history refresh for missed events.
- Add focused backend, Android unit, and Android live-validation coverage for image-message round trips and reconnect recovery after realtime interruption.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: change the Android IM app requirements so normal image messages must use backend-backed attachment delivery and realtime sync must recover automatically after connection loss.
- `im-backend`: change the backend IM requirements so durable direct messages and live WebSocket delivery include attachment metadata and reconnect-safe message recovery.

## Impact

- Affected Android code: `android/app/src/main/java/com/gkim/im/android/data/repository`, `android/app/src/main/java/com/gkim/im/android/data/remote/im`, `android/app/src/main/java/com/gkim/im/android/data/remote/realtime`, `android/app/src/main/java/com/gkim/im/android/feature/chat`, and media helper code used to turn local images into sendable payloads.
- Affected backend code: `backend/src/im`, `backend/src/ws.rs`, HTTP/bootstrap/history serializers, database message persistence, and any schema/migration needed for attachment fields.
- Affected contracts: authenticated WebSocket `message.send` payloads, message history/bootstrap responses, and direct-message record shapes shared between backend and Android.
- Affected tests: Android repository/realtime unit tests, Android emulator live IM validation, backend IM/WebSocket tests, and any migration or payload parsing coverage tied to message attachments and reconnect recovery.
