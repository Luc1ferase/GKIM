## ADDED Requirements

### Requirement: CompanionTurnRepository exposes a JSONL conversation-export method

The `CompanionTurnRepository` SHALL expose `exportConversation(conversationId, format, pathOnly): Result<ExportedChatPayload>`. The `LiveCompanionTurnRepository` impl SHALL delegate to `ImBackendClient.exportConversation(...)` (which calls `GET /api/conversations/:conversationId/export?format=...&pathOnly=...`), wrap the response body string + the canonical `chat-export-<active-path|full-tree>_<first8OfConversationId>.jsonl` filename + the `application/x-ndjson` content-type into `ExportedChatPayload(filename, bytes, contentType)`, and surface wire failures as `Result.failure(throwable)` whose message is a stable error code so the UI can render localized copy without parsing exception types.

#### Scenario: Active-path JSONL export returns a wrapped payload

- **WHEN** `LiveCompanionTurnRepository.exportConversation(conversationId, format="jsonl", pathOnly=true)` is invoked and `ImBackendClient.exportConversation` succeeds with a non-empty JSONL string
- **THEN** the method returns `Result.success(ExportedChatPayload)` whose `filename` is `chat-export-active-path_<first8OfConversationId>.jsonl`, whose `bytes` matches the JSONL string encoded as UTF-8, and whose `contentType` is `application/x-ndjson`

#### Scenario: Full-tree JSONL export uses the full-tree filename

- **WHEN** the same method is invoked with `pathOnly=false`
- **THEN** the returned payload's filename is `chat-export-full-tree_<first8OfConversationId>.jsonl` and the wire query carries `pathOnly=false`

#### Scenario: 404 maps to `404_unknown_conversation`

- **WHEN** `ImBackendClient.exportConversation` throws an HTTP exception with status 404
- **THEN** the method returns `Result.failure` whose throwable message is `404_unknown_conversation`

#### Scenario: 400 unsupported_format maps to `unsupported_format`

- **WHEN** `ImBackendClient.exportConversation` throws an HTTP exception with status 400 carrying the backend's `unsupported_format` error body
- **THEN** the method returns `Result.failure` whose throwable message is `unsupported_format`
