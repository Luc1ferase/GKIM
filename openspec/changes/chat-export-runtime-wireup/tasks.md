# Tasks — chat-export-runtime-wireup

## §1 — OpenSpec scaffold

- [x] §1.1 — proposal.md / tasks.md / specs deltas / .openspec.yaml committed (`784d74a`); slice opened on a fresh worktree off `feature/ai-companion-im` (HEAD `204febf`).

## §2 — Repository runtime + invocation orchestrator

- [x] §2.1 — `CompanionTurnRepository.exportConversation(conversationId, format, pathOnly): Result<ExportedChatPayload>` interface method with default-throw, plus `LiveCompanionTurnRepository.exportConversation` impl that calls `ImBackendClient.exportConversation` and wraps the JSONL string + filename + content-type into `ExportedChatPayload`. `DefaultCompanionTurnRepository` keeps the default-throw (offline / non-live impls won't expose export). (`<TBD>`)
- [x] §2.2 — `ChatExportInvocationOutcome` sealed interface + `invokeChatExport(conversationId, state, repository, dispatcher)` pure orchestrator + `ChatExportDispatcher` fun-interface in `feature/chat/ChatExportInvocation.kt`. (`ExportedChatPayload` lives next to the repo in `data/repository/CompanionTurnRepository.kt` per §2.1.) (`<TBD>`)

## §3 — UI + dispatcher

- [ ] §3.1 — `ChatExportDialogUi.kt` Compose composable that renders the §5.1 state-machine controls (path-only toggle, language pills, share/downloads target pills) with in-flight gating, error-code copy, and auto-dismiss on completed.
- [ ] §3.2 — `rememberChatExportDispatcher()` Compose helper that builds Android `Intent(ACTION_SEND)` (share-sheet) and `MediaStore.Downloads` (>= Q) / `getExternalFilesDir(DIRECTORY_DOWNLOADS)` (< Q) paths. Maps platform failures → error codes (`no_share_target` / `share_cancelled` / `downloads_unavailable` / `write_failed`) identical to `CardExportDialogUi`.
- [ ] §3.3 — `ChatTopBar` overflow trigger ("Export" entry) visible on companion conversations only (gate: `conversation.companionCardId != null`). Tap opens the dialog with the active conversation id.

## §4 — Tests

- [x] §4.1 — `LiveCompanionTurnRepositoryExportConversationTest` — exercises `exportConversation` with a fake `ImBackendClient`: success returns wrapped payload, HTTP 404 maps to `404_unknown_conversation`, HTTP 400 (unsupported format) maps to `unsupported_format`, `pathOnly=true` and `pathOnly=false` both forwarded correctly to the wire query parameter. 8 tests, all green. (`<TBD>`)
- [x] §4.2 — `ChatExportInvocationTest` — exercises `invokeChatExport` with fake repo + fake dispatcher: success returns `Success`, repo failure short-circuits with `Failed(code)`, dispatcher failure returns `Failed(code)`, pathOnly + target choices flow correctly through the orchestrator. 6 tests, all green. (`<TBD>`)

## §5 — Instrumentation

- [ ] §5.1 — `ChatExportDialogInstrumentationTest` — composes `ChatExportDialog` with a fake repository + fake dispatcher; tap path-only off, language ZH, target Downloads, then submit; assert the dispatcher saw `(payload.filename = chat-export-full-tree_<8>.jsonl, target = Downloads)` and the dialog auto-dismissed.

## §6 — Verification + archive

- [ ] §6.1 — Verification roll-up: `cargo`-equivalent for Android (`./gradlew testDebug`) green; instrumentation test green on `codex_api34`; openspec validate strict passes; manual smoke against `chat.lastxuans.sbs` (open dialog → submit → confirm Share intent fires + Downloads file written) — record evidence + scores in `docs/DELIVERY_WORKFLOW.md`.
- [ ] §6.2 — Archive — move `openspec/changes/chat-export-runtime-wireup` → `openspec/changes/archive/2026-04-26-chat-export-runtime-wireup/`, apply spec deltas to `openspec/specs/core/im-app` + `openspec/specs/llm-text-companion-chat`, run `openspec archive` (or manual deltas-merge if openspec CLI nested-capability bug recurs), commit + push.
