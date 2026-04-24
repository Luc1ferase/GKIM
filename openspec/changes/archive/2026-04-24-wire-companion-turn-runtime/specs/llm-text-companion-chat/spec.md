## ADDED Requirements

### Requirement: Companion chat Send path invokes CompanionTurnRepository end-to-end

The system SHALL dispatch the chat Send affordance on a companion conversation through `CompanionTurnRepository.submitUserTurn(...)` rather than the legacy `MessagingRepository.sendMessage(...)` path. The chat surface MUST render an outgoing user bubble at `MessageStatus.Pending` optimistically on submit, then transition per the companion turn state flow through the terminal states (`Thinking`, `Streaming`, `Completed`, `Failed`, `Blocked`, `Timeout`). The integration MUST be covered by a layered test pyramid: (1) unit tests for ViewModel dispatch branching on the conversation's companion marker and for uiState assembly from the companion state flow, (2) a unit test for the activate handler populating the companion marker on conversation creation, and (3) a Compose-rendered instrumentation test on `codex_api34` that drives the production `DefaultCompanionTurnRepository` reducer transitions through `CompanionLifecycleTimelineHost` and asserts every lifecycle state rendered by `ChatMessageRow`.

#### Scenario: Send on a companion conversation dispatches to CompanionTurnRepository

- **WHEN** a user taps Send on a chat surface whose active conversation is a companion conversation
- **THEN** the `ChatViewModel` invokes `CompanionTurnRepository.submitUserTurn(...)` exactly once for that submit and does not invoke `MessagingRepository.sendMessage(...)` for the same submit

#### Scenario: Send on a peer-IM conversation still uses MessagingRepository

- **WHEN** a user taps Send on a chat surface whose active conversation is a peer-IM conversation (no companion marker)
- **THEN** the `ChatViewModel` invokes `MessagingRepository.sendMessage(...)` exactly once for that submit and does not invoke `CompanionTurnRepository.submitUserTurn(...)` for the same submit

#### Scenario: Companion user bubble renders optimistically on submit

- **WHEN** a user taps Send on a companion conversation
- **THEN** the chat surface renders an outgoing user bubble at `MessageStatus.Pending` before any backend acknowledgment has been received

#### Scenario: ChatMessageRow consumes the companion state flow

- **WHEN** the active conversation is a companion conversation and the companion turn state flow emits successive lifecycle events
- **THEN** `ChatMessageRow` renders the corresponding state (`Thinking`, `Streaming`, `Completed`, `Failed`, `Blocked`, or `Timeout`) sourced from `ChatViewModel.uiState` and driven by `CompanionTurnRepository`'s state flow for that conversation

#### Scenario: Compose-rendered instrumentation gates the lifecycle render

- **WHEN** the companion-chat instrumentation runs on `codex_api34(AVD) - 14` driving a real `DefaultCompanionTurnRepository` through `recordUserTurn` → `handleTurnStarted` → `handleTurnDelta` → `handleTurnCompleted`
- **THEN** the test collects `activePathByConversation` through `CompanionLifecycleTimelineHost` and asserts that `ChatMessageRow` renders (a) `chat-message-body-<userMessageId>` for the Pending user bubble, (b) `chat-companion-status-<companionMessageId>` with `"Thinking…"` after the started event, (c) `"Streaming…"` after the first delta, and (d) `chat-message-body-<companionMessageId>` with the final body after the completed event

#### Scenario: Failed-terminal scenario is covered in instrumentation

- **WHEN** the same instrumentation test runs its Failed scenario driving `handleTurnFailed(subtype="transient")` on the companion message and `updateUserMessageStatus(Failed)` on the user message
- **THEN** the test asserts `chat-companion-failed-copy-<companionMessageId>` is displayed (companion bubble failed copy from `SafetyCopy.localizedFailedCopy`) and `chat-user-submission-retry-<userMessageId>` is displayed (outgoing-user Retry affordance from `outgoingSubmissionFailureLine`)
