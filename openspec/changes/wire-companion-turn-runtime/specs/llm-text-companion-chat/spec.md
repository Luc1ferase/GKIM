## ADDED Requirements

### Requirement: Companion chat Send path invokes CompanionTurnRepository end-to-end

The system SHALL dispatch the chat Send affordance on a companion conversation through `CompanionTurnRepository.submitUserTurn(...)` rather than the legacy `MessagingRepository.sendMessage(...)` path. The chat surface MUST render an outgoing user bubble at `MessageStatus.Pending` optimistically on submit, then transition per the companion turn state flow through the terminal states (`Thinking`, `Streaming`, `Completed`, `Failed`, `Blocked`, `Timeout`). The integration MUST be covered by an end-to-end instrumentation test on `codex_api34` that launches the real `GkimRootApp`, navigates tavern → character detail → activate → chat, sends a message, and asserts the visible bubble lifecycle.

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

#### Scenario: End-to-end instrumentation gates the integration

- **WHEN** the companion-chat end-to-end instrumentation runs on `codex_api34(AVD) - 14` with a fake `CompanionTurnRepository` scripted to emit Thinking → Streaming → Completed
- **THEN** the test launches the real `GkimRootApp`, navigates tavern → character detail → activate → chat, sends a message, and observes (a) an optimistic outgoing user bubble within 1 second, (b) a Thinking bubble within 2 seconds, (c) a Streaming bubble replacing the Thinking bubble within the scripted window, and (d) a Completed bubble with the scripted body by the end of the scripted sequence

#### Scenario: Failed-terminal scenario is covered in instrumentation

- **WHEN** the same instrumentation test runs its Failed scenario with a fake `CompanionTurnRepository` scripted to emit Thinking → Failed carrying a retryable subtype
- **THEN** the test observes a Failed-labelled companion bubble with per-subtype copy and a Retry affordance surfaced on the outgoing user bubble
