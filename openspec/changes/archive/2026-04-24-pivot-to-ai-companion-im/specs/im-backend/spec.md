## ADDED Requirements

### Requirement: Backend orchestrates durable AI companion conversations
The system SHALL orchestrate AI companion dialogue on the backend, and it MUST persist companion identity, per-user conversation state, bounded memory summaries, and pending turn lifecycle in durable storage. The backend MUST keep companion conversation state isolated per user so one user’s relationship context or memory does not leak into another user’s thread.

#### Scenario: User sends a turn to an AI companion
- **WHEN** an authenticated user submits a message to an AI companion conversation
- **THEN** the backend records the user turn, resolves the companion identity and current memory context, and starts the companion reply lifecycle as durable server-side work

#### Scenario: User resumes a companion thread after interruption
- **WHEN** the user reconnects while a companion turn or memory update was previously in progress
- **THEN** the backend can restore the pending or completed turn lifecycle and current memory summary from durable state

#### Scenario: Companion memory remains isolated per user
- **WHEN** multiple users converse with the same companion persona
- **THEN** the backend keeps memory and relationship state scoped to each user-companion pair instead of sharing private conversation context across accounts

### Requirement: Backend emits realtime companion response lifecycle events
The system SHALL deliver AI companion reply lifecycle over the existing realtime IM boundary, and it MUST emit enough event states for the Android client to represent thinking, partial or progressive reply state, final response completion, and explicit failure or blocked outcomes.

#### Scenario: Online client receives in-progress and final companion reply states
- **WHEN** a companion response is generated for an online user
- **THEN** the backend emits the reply lifecycle events needed for the client to render both in-progress and completed companion response states

#### Scenario: Companion generation fails or is blocked
- **WHEN** provider execution, safety policy, or orchestration logic prevents a companion reply from completing
- **THEN** the backend emits an explicit failure or blocked outcome instead of leaving the client waiting on an apparently healthy reply that will never arrive
