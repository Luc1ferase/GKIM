# Tasks — chat-export-runtime-wireup

## §1 — OpenSpec scaffold

- [x] §1.1 — proposal.md / tasks.md / specs deltas / .openspec.yaml committed (`784d74a`); slice opened on a fresh worktree off `feature/ai-companion-im` (HEAD `204febf`).

## §2 — Repository runtime + invocation orchestrator

- [x] §2.1 — `CompanionTurnRepository.exportConversation(conversationId, format, pathOnly): Result<ExportedChatPayload>` interface method with default-throw, plus `LiveCompanionTurnRepository.exportConversation` impl that calls `ImBackendClient.exportConversation` and wraps the JSONL string + filename + content-type into `ExportedChatPayload`. `DefaultCompanionTurnRepository` keeps the default-throw (offline / non-live impls won't expose export). (`4ad8f4e`)
- [x] §2.2 — `ChatExportInvocationOutcome` sealed interface + `invokeChatExport(conversationId, state, repository, dispatcher)` pure orchestrator + `ChatExportDispatcher` fun-interface in `feature/chat/ChatExportInvocation.kt`. (`ExportedChatPayload` lives next to the repo in `data/repository/CompanionTurnRepository.kt` per §2.1.) (`240591c`)

## §3 — UI + dispatcher

- [x] §3.1 — `ChatExportDialogUi.kt` Compose composable rendering the §5.1 state-machine controls (path-only / language / target pills) with in-flight gating + error-code copy + auto-dismiss on completed. Includes localized error messages for the 7 codes (`share_cancelled` / `no_share_target` / `downloads_unavailable` / `write_failed` / `404_unknown_conversation` / `unsupported_format` / `network_failure`). (`c047aff`)
- [x] §3.2 — `rememberChatExportDispatcher()` Compose helper that builds `Intent(ACTION_SEND)` via dedicated `com.gkim.im.android.chatexport.fileprovider` (new authority + `chat_export_paths.xml` registered in `AndroidManifest.xml`) and `MediaStore.Downloads` (Android Q+) / `getExternalFilesDir(DIRECTORY_DOWNLOADS)` (pre-Q) under the `GKIMChats/` subdir. (`c047aff`)
- [x] §3.3 — `ChatTopBar` overflow trigger ("Export chat" entry in DropdownMenu) visible on companion conversations only (gate: `conversation.companionCardId != null` evaluated in `ChatRoute`; passes `null` callback for non-companion conversations so the entire overflow icon is hidden). `ChatRoute` hosts `ChatExportDialog` when `showExportDialog == true`, wires `container.companionTurnRepository` + `rememberChatExportDispatcher()`. (`c047aff`)

## §4 — Tests

- [x] §4.1 — `LiveCompanionTurnRepositoryExportConversationTest` — exercises `exportConversation` with a fake `ImBackendClient`: success returns wrapped payload, HTTP 404 maps to `404_unknown_conversation`, HTTP 400 (unsupported format) maps to `unsupported_format`, `pathOnly=true` and `pathOnly=false` both forwarded correctly to the wire query parameter. 8 tests, all green. (`4ad8f4e`)
- [x] §4.2 — `ChatExportInvocationTest` — exercises `invokeChatExport` with fake repo + fake dispatcher: success returns `Success`, repo failure short-circuits with `Failed(code)`, dispatcher failure returns `Failed(code)`, pathOnly + target choices flow correctly through the orchestrator. 6 tests, all green. (`240591c`)

## §5 — Instrumentation

- [x] §5.1 — `ChatExportDialogInstrumentationTest` — composes `ChatExportDialog` with a fake repository + fake dispatcher; 4 tests: dialog renders all control slots; submit with defaults dispatches Share + auto-dismisses; toggle to full-tree + Downloads + ZH flows to repo + dispatcher; repository failure renders inline error + keeps dialog open. 4/4 green on `codex_api34` (`b1545f2`).

## §6 — Verification + archive

- [x] §6.1 — Verification roll-up: §1-§5 evidence rows captured in `docs/DELIVERY_WORKFLOW.md` (chat-export-runtime-wireup section). All unit tests green (8 + 6 = 14 + 14 ChatTreeAffordanceUiGatesTest unaffected); 4/4 instrumentation green on codex_api34. Backend endpoint live at `chat.lastxuans.sbs` (verified via `curl https://chat.lastxuans.sbs/api/conversations/foo/export?format=jsonl` → HTTP 401 = endpoint reachable). (`793936d`)
- [x] §6.2 — Archive — move `openspec/changes/chat-export-runtime-wireup` → `openspec/changes/archive/2026-04-26-chat-export-runtime-wireup/`, apply spec deltas to `openspec/specs/core/im-app` + `openspec/specs/llm-text-companion-chat`. (`793936d`)
