## Why

The `llm-text-companion-chat` slice archived on 2026-04-23 declared task 3.4 ("Wire submit path: when the user taps Send in a companion conversation, call `CompanionTurnRepository.submitUserTurn`") and task 5.2 (instrumentation coverage on `codex_api34`) as complete. Live testing on `emulator-5554` (codex_api34) on 2026-04-24 shows the runtime does not satisfy either task:

1. `feature/tavern/CharacterDetailRoute.kt:85-89` `onActivate` calls `container.messagingRepository.ensureConversation(...)` and navigates to `chat/{conversationId}` — the legacy peer-IM route — without signaling that the target is a companion conversation.
2. `feature/chat/ChatRoute.kt:103` `ChatViewModel` constructor does not inject `CompanionTurnRepository`. The `sendMessage(body)` function at line 144 calls `messagingRepository.sendMessage(...)` directly — it never invokes `submitUserTurn`, regardless of conversation type.
3. Cross-repo `grep` for `companionTurnRepository` shows matches only in `AppContainer.kt` (registration), `CompanionTurnRepository.kt`, and `LiveCompanionTurnRepository.kt`. There are zero feature-layer consumers.
4. The `ChatMessageRow` lifecycle renderer for `Thinking` / `Streaming` / `Completed` / `Failed` / `Blocked` / `Timeout` exists in `feature/chat/ChatRoute.kt` around lines 641–1150 and is covered by `ChatPresentationTest`, but receives no live data because the repository is never called.
5. `LlmCompanionChatTest.kt` on `codex_api34` drives only the leaf `CompanionGreetingPicker` composable plus three pure-function assertions. It does not launch `ChatRoute`, `CharacterDetailRoute`, `GkimRootApp`, or the real `AppContainer`. The archived `llm-text-companion-chat/tasks.md` §5.2 explicitly records *"Full-route scenarios ... ride the unit suites from task 5.1"* — conceding that end-to-end was deferred to component-level tests.
6. Observed behavior on 2026-04-24 (screenshots `tmp-comp-sent.png`, `tmp-comp-t2s.png`, `tmp-comp-t12s.png`, `tmp-comp-enter.png`): activate → chat screen renders with the companion title, but sending a message produces zero optimistic user bubble, zero Thinking bubble, and zero terminal state; pressing Back once pops the companion chrome and reveals a generic `Chat · AIGC-enabled conversation surface` screen underneath; re-entering shows an empty thread, indicating no local persistence either. `logcat` confirms network traffic does fire (`TrafficStats tagSocket` against the `com.gkim.im.android` pid), so the backend is being reached through the legacy path, but nothing the backend returns is rendered.

The `ai-companion-experience` spec Requirement *"AI companion dialogue exposes explicit safety and recovery boundaries"* states the system *"MUST avoid pretending that a blocked, failed, or unsafe companion turn is a healthy conversational response."* The current runtime violates this Requirement by rendering nothing at all on Send.

The `tavern-experience-polish` umbrella is 0/23 and its §3 (branch-tree navigation, Edit, Regenerate-from-here) is predicated on working companion bubbles. Until this runtime wiring is restored, tavern-experience-polish §3 cannot be meaningfully implemented or verified.

## What Changes

- **The activate handler** in `feature/tavern/CharacterDetailRoute.kt` produces a conversation that carries a companion-conversation marker (e.g., `Conversation.companionCardId: String?` populated, or a `ConversationKind.Companion` enum variant), so `ChatViewModel` can dispatch on it without having to reach back through the repository layer for identification.
- **`ChatViewModel`** is extended to take `companionTurnRepository: CompanionTurnRepository` from `AppContainer`. Its `sendMessage(body)` function branches: if the loaded `conversation` is a companion conversation, call `companionTurnRepository.submitUserTurn(conversationId, body, activeLanguage, parentMessageId)`; otherwise fall back to `messagingRepository.sendMessage(...)` (the legacy peer-IM path is unchanged so that peer chat continues to work).
- **`ChatViewModel.uiState`** is extended to expose a unified `messages: List<ChatMessage>` whose lifecycle statuses are sourced from the companion turn state flow when the conversation is a companion conversation. `ChatMessageRow` is unchanged — it already renders all six states from `ChatMessage.status`.
- **A new end-to-end instrumentation test** on `codex_api34` launches the real `GkimRootApp`, navigates tavern → character detail → activate → chat, sends a message against a scripted fake `CompanionTurnRepository`, and asserts the optimistic user bubble renders plus Thinking and Completed (or Failed) bubbles appear within bounded windows. This closes the false-archival gap that let the integration regression ship.
- **The `core/im-app` capability** gains a new Requirement *"Companion chat Send path invokes CompanionTurnRepository end-to-end"* whose scenarios explicitly call out submit-path dispatch, optimistic user bubble, state-flow rendering, and end-to-end instrumentation on `codex_api34`. Making the runtime wiring itself a spec surface prevents future slices from archiving companion-chat UI work without end-to-end evidence.

## Capabilities

### Modified Capabilities
- `llm-text-companion-chat`: The companion-chat capability gains an explicit runtime-wiring Requirement so the Send dispatch, optimistic user bubble, state-flow rendering, and end-to-end instrumentation on `codex_api34` are spec-gated. The Requirement is attached to this capability (rather than `core/im-app`) because it restores behavior the `llm-text-companion-chat` slice already claimed and because OpenSpec 1.3.0 parses flat-capability delta paths, not the nested `core/im-app` path.

## Impact

- Affected Android code:
  - `android/app/src/main/java/com/gkim/im/android/feature/tavern/CharacterDetailRoute.kt` (`onActivate` handler)
  - `android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt` (`ChatViewModel` constructor, `sendMessage` branching, `uiState` wiring)
  - `android/app/src/main/java/com/gkim/im/android/core/model/Conversation.kt` (add the companion marker field, if not already present) and/or `MessagingRepository.ensureConversation(...)` return shape
  - New `android/app/src/androidTest/java/com/gkim/im/android/feature/chat/CompanionChatEndToEndInstrumentationTest.kt`
- Affected specs:
  - `openspec/changes/wire-companion-turn-runtime/specs/llm-text-companion-chat/spec.md` (ADDED Requirement)
- Affected UX: activate → Send → optimistic user bubble, Thinking bubble, and a real terminal state all render; Back from a companion chat no longer leaks a generic fallback screen because the underlying conversation now carries its companion identity.
- Non-goals (scoped out of this slice):
  - The 8 `tavern-experience-polish` items (portrait large-view, alt-greeting refinement, branch navigation, per-character override, JSONL export, relationship reset, gacha surfacing, creator attribution) remain out — this slice restores only the baseline companion reply lifecycle they depend on.
  - A full navigation-stack audit is out. The Back behavior inside the companion chat is addressed only to the extent that the chat route's back pop lands coherently; other navigation surfaces are out.
  - Adding new `BlockReason` / `FailedSubtype` values — the six closed-set keys from `companion-settings-and-safety-reframe` are fixed.
  - Backend contract changes — this slice is pure Android integration. The submit / regenerate / snapshot / pending endpoints already exist in `im-backend/spec.md`.
  - Updating `LlmCompanionChatTest.kt` beyond what the new end-to-end test needs. The existing four leaf-level tests stay as they are; retroactive conversion to end-to-end is out.
  - Auditing every other false-archival across the pivot umbrella. This slice addresses the companion-chat-runtime gap specifically; other gaps (if any) are separate slices.
