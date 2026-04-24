## ADDED Requirements

### Requirement: AI companions maintain durable persona and relationship continuity
The system SHALL define AI companions as durable conversation partners with stable persona identity, visible relationship context, and bounded memory continuity across sessions. Each companion MUST expose enough structured state for the app and backend to keep the dialogue feeling like an ongoing relationship rather than a stateless one-shot generation prompt.

#### Scenario: User opens the companion roster
- **WHEN** a user enters the branch’s primary AI companion conversation experience
- **THEN** the system presents one or more companion identities with stable persona metadata and relationship context instead of treating every conversation as an anonymous prompt box

#### Scenario: User returns to an existing companion thread
- **WHEN** the user reopens a prior AI companion conversation
- **THEN** the system restores the bounded memory summary and current relationship context needed to continue the dialogue coherently

### Requirement: AI companion dialogue exposes explicit safety and recovery boundaries
The system SHALL represent companion reply generation as a bounded lifecycle with deliberate failure and safety states, and it MUST avoid pretending that a blocked, failed, or unsafe companion turn is a healthy conversational response.

#### Scenario: Companion reply is blocked or fails
- **WHEN** a companion turn cannot be completed because of safety policy, provider failure, or orchestration error
- **THEN** the system surfaces an explicit non-success state for that turn instead of silently dropping the response or presenting stale placeholder text as a real reply

#### Scenario: Pending companion turn is resumed after interruption
- **WHEN** the user disconnects or leaves the app while a companion response is still pending
- **THEN** the system can recover the turn lifecycle and continue or resolve it explicitly when the user returns
