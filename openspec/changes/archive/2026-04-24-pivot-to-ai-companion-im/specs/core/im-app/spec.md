## ADDED Requirements

### Requirement: Android app presents a companion-first inbox and conversation flow
The system SHALL treat AI companions as first-class conversations in the Android app, and it MUST make companion dialogue the primary emphasis of the inbox and chat-detail experience on this branch. Companion threads MUST be visibly distinguishable from any retained human or system threads through clear identity, state, and UI framing.

#### Scenario: User opens the primary inbox
- **WHEN** the user enters the main conversation surface of the AI companion branch
- **THEN** the app presents AI companion conversations as the dominant entry points instead of framing the screen as a human-contact-first inbox

#### Scenario: User opens a companion conversation
- **WHEN** the user enters a chat thread backed by an AI companion
- **THEN** the app shows companion-specific identity and conversation context cues so the thread reads as an ongoing companion relationship rather than a generic IM contact row

### Requirement: Android companion chat surfaces reply lifecycle and recovery state
The system SHALL expose the lifecycle of AI companion turns in the Android chat UI, including thinking, streaming or progressive response, completion, and failure or recovery states. The UI MUST let users distinguish a healthy in-progress companion reply from a stalled or failed turn.

#### Scenario: Companion begins responding after a user turn
- **WHEN** the user sends a message to an AI companion
- **THEN** the chat UI surfaces the companion’s in-progress response state before the final reply is complete

#### Scenario: Companion reply must recover after reconnect or relaunch
- **WHEN** the app reconnects or relaunches while a companion turn was previously pending
- **THEN** the chat UI restores the correct pending, resumed, completed, or failed state instead of losing that turn’s status silently

### Requirement: Android settings and AI controls align with companion behavior
The system SHALL expose user-facing controls that tune companion-related behavior, and it MUST frame retained AI/provider settings around companion experience needs such as persona behavior, model choice, memory behavior, or safety posture rather than a disconnected creator-only tool workflow.

#### Scenario: User opens AI-related settings on the companion branch
- **WHEN** the user enters settings or other AI control surfaces
- **THEN** the app presents those controls as companion-oriented configuration rather than as a separate generic generation lab
