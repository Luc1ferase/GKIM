## ADDED Requirements

### Requirement: Chat export entry-point and dispatcher in the production Android chat surface

The Android chat surface SHALL expose an "Export" entry in the chat top bar's overflow menu on companion conversations (gated on `conversation.companionCardId != null`). Tapping the entry SHALL open `ChatExportDialog` with the active `conversationId`. The dialog SHALL render the `ChatExportDialogState` controls (path-only toggle, language pills, share/downloads target pills) and project the dispatcher's outcome through the §5.1 lifecycle flags (in-flight disables controls, completed auto-dismisses, errorCode renders inline copy).

#### Scenario: Companion conversation surfaces the export entry

- **WHEN** the user opens a chat where the active conversation has a non-null `companionCardId`
- **THEN** the chat top bar's overflow menu displays an "Export" entry
- **AND** non-companion (peer / direct) conversations do not surface the entry

#### Scenario: Submit dispatches via share-sheet and auto-dismisses on success

- **WHEN** the user opens the export dialog, leaves `pathOnly=true`, leaves the language at the active app language, and selects `target=Share` then taps Export
- **THEN** the dialog enters the in-flight state, calls `repository.exportConversation(conversationId, format="jsonl", pathOnly=true)`, builds an `Intent(ACTION_SEND)` with the JSONL bytes attached via `FileProvider`, fires `Intent.createChooser(...)`, and on success marks the dialog completed which dismisses it

#### Scenario: Submit dispatches via Downloads and auto-dismisses on success

- **WHEN** the user opens the export dialog, toggles `pathOnly=false`, selects `target=Downloads`, and taps Export
- **THEN** the dialog enters the in-flight state, calls `repository.exportConversation(conversationId, format="jsonl", pathOnly=false)`, writes the JSONL bytes to `MediaStore.Downloads` (Android Q+) under the chat-exports relative subdir using the `chat-export-full-tree_<first8OfConversationId>.jsonl` filename, and on success marks the dialog completed which dismisses it

#### Scenario: Wire failure renders the inline error and re-enables controls

- **WHEN** `repository.exportConversation` returns `Result.failure(throwable)` whose message matches one of `404_unknown_conversation` / `unsupported_format` / `network_failure`
- **THEN** the dialog leaves the in-flight state, sets `errorCode = <code>`, renders the localized error copy below the controls, and re-enables the controls so the user can retry or change choices

#### Scenario: Dispatcher failure renders the inline error

- **WHEN** the wire call succeeds but the platform dispatcher fails (no share target installed, Downloads unavailable, write IO failure)
- **THEN** the dialog leaves the in-flight state, sets `errorCode` to the dispatcher's emitted code (`no_share_target` / `share_cancelled` / `downloads_unavailable` / `write_failed`), and renders the localized error copy
