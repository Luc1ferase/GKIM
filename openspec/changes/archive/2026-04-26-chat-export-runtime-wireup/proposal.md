## Why

The tavern-experience-polish slice's §5.1 commit landed the **presentation contract** for the JSONL chat-export dialog (`ChatExportDialogState` + transition helpers in `ChatExportDialog.kt`, plus `ChatExportRequestParams` / `chatExportFilename` / `dispatchTargetFor` in `ChatExportRouting.kt`). The backend's `tavern-experience-polish-export-backend` slice then shipped `GET /api/conversations/:conversationId/export?format=jsonl&pathOnly=...` (now live on `chat.lastxuans.sbs`, deployed 2026-04-26). The Retrofit method `ImBackendClient.exportConversation(...)` and the `ImBackendHttpClient` impl are also already in place from the polish slice.

What is missing is the **production wire-up**: the Compose dialog UI that renders the §5.1 state machine, the platform dispatcher (share-sheet + DownloadManager / MediaStore Downloads), the entry-point trigger in the chat top bar overflow, and the repository method that calls `ImBackendClient.exportConversation` and projects the response into a payload the dispatcher can consume. Without this slice, the export dialog cannot be opened from the running app even though every wire-level helper is reachable.

## What Changes

- **`CompanionTurnRepository` / `LiveCompanionTurnRepository.exportConversation`**: new repository method that calls `ImBackendClient.exportConversation(...)`, wraps the JSONL string body + filename + content-type into an `ExportedChatPayload`, and surfaces wire-failure error codes (`404_unknown_conversation`, `unsupported_format`, etc.) back to the caller. Mirrors `CardInteropRepository.exportCard` shape.
- **`ChatExportInvocationOutcome` + `invokeChatExport` orchestrator**: pure function (no Android imports) that composes `repository.exportConversation` + `dispatcher.dispatch`, mirroring `invokeCardExport` so the same test pattern applies. Emits `Success(payload, target)` / `Failed(code)` for the dialog to project into the §5.1 lifecycle flags.
- **`ChatExportDialogUi.kt`**: new Compose composable that renders the §5.1 dialog with the path-only / language / target controls, lifecycle gates (in-flight disables controls; completed auto-dismisses; errorCode renders inline error copy), and a `rememberChatExportDispatcher()` that builds the Android `Intent(ACTION_SEND)` (share-sheet) and `MediaStore.Downloads` / `getExternalFilesDir(DIRECTORY_DOWNLOADS)` (Downloads) paths. Mirrors `CardExportDialog` + `CardExportDialogUi` exactly so the dispatcher's `runCatching` failure → error-code mapping (`no_share_target` / `share_cancelled` / `downloads_unavailable` / `write_failed`) is identical.
- **`ChatTopBar` overflow trigger**: add an "Export" entry to the chat top bar's overflow menu (companion conversations only — visibility gated on `conversation.companionCardId != null`). Tapping opens `ChatExportDialog` with the active conversation id. Dialog state is local Compose state (no `ChatViewModel` plumbing — mirrors `CardExportDialog`'s pattern in `tavern/CardSheetTabs.kt`).

## Capabilities

### Modified Capabilities

- `core/im-app`: Android chat surface gains the visible Export overflow trigger + the Compose dialog UI rendering the `ChatExportDialogState`. The §5.1 state-machine contract is preserved unchanged; this slice fills in the surface that mutates it and the platform delivery that consumes its outcome.
- `llm-text-companion-chat`: `CompanionTurnRepository` gains `exportConversation(conversationId, format, pathOnly): Result<ExportedChatPayload>` plus a `LiveCompanionTurnRepository` HTTP-delegating impl. Mirrors the existing `editUserTurn` / `regenerateCompanionTurnAtTarget` shape from `chat-tree-runtime-wireup` — Result-typed, error-coded, pure delegation to the already-shipped Retrofit method.

## Impact

- Affected Android code: new file `feature/chat/ChatExportDialogUi.kt` (Compose dialog + `rememberChatExportDispatcher`), new file `feature/chat/ChatExportInvocation.kt` (`invokeChatExport` orchestrator + outcome sealed interface), modified `feature/chat/ChatRoute.kt` (overflow entry in `ChatTopBar`), modified `data/repository/CompanionTurnRepository.kt` + `LiveCompanionTurnRepository.kt` (export method).
- Affected backend contract: none new — this slice consumes the already-deployed `tavern-experience-polish-export-backend` endpoint.
- Affected manifest: add `FileProvider` authority `com.gkim.im.android.chatexport.fileprovider` (or reuse the existing `com.gkim.im.android.cardexport.fileprovider` if compatible — to be decided in §3.2 implementation; the simpler path is a separate authority for clarity).
- Affected specs: delta on `core/im-app` (production runtime requirement for the export entry-point + dispatcher) + delta on `llm-text-companion-chat` (new repository method).
- Affected UX: chat top bar gains an overflow menu (3-dots) with an "Export" entry visible on companion conversations. Tapping opens the §5.1 dialog. Submit drives a JSONL download to the share-sheet or Downloads folder.
- Non-goals (scoped out of this slice):
  - Multi-conversation batch export (one conversation per dialog).
  - Server-side preset override or prompt-time format change (handled by separate backend slices).
  - Custom export formats (JSON tree-graph, CSV, etc.) — only JSONL active-path / full-tree shipping in this slice.
  - Per-conversation export history persistence (each invocation is independent).
  - User-side variant tracking on the export tree (the `pathOnly=false` server export already returns the full tree shape — client just delivers the bytes).
