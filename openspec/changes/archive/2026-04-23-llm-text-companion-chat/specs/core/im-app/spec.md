## ADDED Requirements

### Requirement: Android companion chat renders reply lifecycle bubbles

The system SHALL render each companion reply as a bubble whose visual state reflects the current lifecycle phase, and it MUST surface `Thinking`, `Streaming`, `Completed`, `Failed`, `Blocked`, and `Timeout` as distinct visible states. The UI MUST NOT collapse `Blocked` or `Timeout` into a generic failure visual and MUST NOT leave a companion bubble silently stuck in a non-terminal state.

#### Scenario: User sees thinking and streaming state before completion

- **WHEN** the user sends a turn to a companion and the lifecycle enters `Thinking` followed by `Streaming`
- **THEN** the chat UI renders a thinking-state bubble first, then streams visible text as deltas arrive, before presenting the final completed reply

#### Scenario: Failed, blocked, and timeout bubbles are visually distinct

- **WHEN** a companion turn terminates in `Failed`, `Blocked`, or `Timeout`
- **THEN** the chat UI renders a bubble whose copy and affordances differ between the three terminals, so the user can distinguish a generic error from a safety block or a timeout

### Requirement: Android companion chat exposes swipe navigation and regenerate

The system SHALL render swipe navigation controls plus a variant-index indicator on any companion bubble that belongs to a variant group with more than one variant, and it MUST expose a regenerate affordance on the most recent companion variant in the active path. Swipe navigation MUST change only the active-variant selection and MUST NOT delete any variant.

#### Scenario: Swipe chevrons appear on multi-variant companion bubbles

- **WHEN** the chat timeline renders a companion bubble whose `variantGroupId` contains more than one variant
- **THEN** the bubble shows chevrons and an `n/m` indicator, and tapping a chevron changes only the active variant without removing siblings

#### Scenario: Regenerate appends a new variant

- **WHEN** the user taps regenerate on the most recent companion variant in the active path
- **THEN** the app submits a regeneration request and renders a new variant bubble that transitions through the normal `Thinking → Streaming → Completed` lifecycle, while the prior variants remain reachable via swipe

### Requirement: Android companion chat provides first-message selection on empty entry

The system SHALL present a first-message / alternate-greeting selector when a user opens a companion conversation whose message history is empty, and it MUST skip the selector once any message exists. The picker MUST list the resolved `firstMes` plus every `alternateGreetings` entry in the active `AppLanguage`, and the selected greeting MUST be persisted as a companion variant before the user composes a reply.

#### Scenario: User selects an opening greeting

- **WHEN** the user enters an empty companion conversation and chooses one option from the greeting picker
- **THEN** the app records that greeting as a companion variant and closes the picker before the user can start composing

#### Scenario: Existing conversation skips picker

- **WHEN** the user reopens a companion conversation that already contains history
- **THEN** the app routes directly into the timeline without presenting a greeting picker

### Requirement: Android companion chat recovers in-progress turns after reconnect or relaunch

The system SHALL rehydrate any in-flight companion turn when the Android app cold-starts or the realtime gateway reconnects, and it MUST NOT drop a pending turn from the user-visible timeline without either completing it or surfacing a terminal state.

#### Scenario: Cold start rehydrates a pending companion turn

- **WHEN** the app cold-starts while a companion turn remains in `Thinking` or `Streaming` on the backend
- **THEN** the chat timeline shows that turn in its pre-termination lifecycle state and consumes continuing lifecycle events until termination

#### Scenario: Reconnect resumes streaming deltas

- **WHEN** the realtime gateway reconnects while a companion turn is still streaming
- **THEN** the chat timeline continues to consume lifecycle deltas from the correct monotonic sequence, falling back to a server snapshot refresh on any gap

### Requirement: Android companion chat surfaces safety and timeout outcomes explicitly

The system SHALL display the block reason on a `Blocked` companion bubble and MUST label `Timeout` bubbles differently from generic `Failed` bubbles. The UI MUST NOT fabricate companion reply content for a blocked, failed, or timed-out turn.

#### Scenario: Block reason is visible but not graphic

- **WHEN** a companion turn terminates in `Blocked` with a typed reason
- **THEN** the bubble surfaces the reason in neutral product copy and does not replay or restate graphic content

#### Scenario: Timeout guidance differs from generic failure guidance

- **WHEN** a companion turn terminates in `Timeout`
- **THEN** the bubble surfaces timeout-specific copy and offers a retry affordance whose semantics differ from a generic `Failed` retry
