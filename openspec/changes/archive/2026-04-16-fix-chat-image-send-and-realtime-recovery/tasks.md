## 1. Backend attachment message contract

- [x] 1.1 Add backend persistence, models, and authenticated API routes for direct image messages plus attachment fetch authorization.
- [x] 1.2 Extend backend bootstrap/history/WebSocket payloads so direct messages can include attachment descriptors and delivery/read handling for image messages.

## 2. Android image-message delivery

- [x] 2.1 Extend Android IM backend client models and repository APIs to upload image messages and map attachment descriptors into `ChatMessage` state.
- [x] 2.2 Update chat composer and generated-image send flows so backend conversations use the new backend image-message path instead of local-only attachment insertion, while fallback-only conversations keep current local behavior.
- [x] 2.3 Add authenticated attachment rendering support in the Android chat UI so backend-hosted direct-message images display correctly in conversation history and live events.

## 3. Realtime recovery and resync

- [x] 3.1 Add heartbeat and automatic reconnect behavior to the Android realtime client for authenticated IM sessions.
- [x] 3.2 Update `LiveMessagingRepository` to resynchronize conversation summaries and already-loaded histories after reconnect, while preserving id-based reconciliation for queued or recently delivered messages.

## 4. Verification coverage

- [x] 4.1 Expand backend and Android unit tests to cover image-message persistence, payload parsing, attachment rendering state, and reconnect-driven history recovery.
- [x] 4.2 Extend Android live IM validation coverage to exercise image-message round trips and missed-message recovery after a forced realtime interruption.
