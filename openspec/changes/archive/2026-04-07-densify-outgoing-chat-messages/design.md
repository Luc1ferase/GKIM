## Context

The Android-native chat surface already uses a compact top bar, a messaging-first composer, and attributed message rows that differentiate incoming, outgoing, and system content. After the previous chat refinement, all message directions share the same avatar-leading row structure, which improved attribution for incoming and system content but now leaves outgoing self-authored messages visually over-specified: the user's own rows still show a self-avatar and a `You` label even though ownership is already obvious from alignment and bubble styling.

This change must preserve the existing Aether visual language, message timestamp format, attachment rendering, and current test strategy in `GkimRootAppTest`. It should stay local to the chat timeline composable and related UI coverage so it can be implemented, reviewed, scored, and pushed in small apply-session slices.

## Goals / Non-Goals

**Goals:**
- Remove redundant self-identity chrome from outgoing messages by hiding the self-avatar and `You` sender label.
- Reposition the outgoing timestamp to the lower-right corner inside the message bubble so the row uses less vertical space.
- Preserve avatar-led attribution, sender labels, attachment rendering, and readable timestamps for incoming and system messages.
- Keep the change scoped to Android Compose chat UI plus targeted automated coverage.

**Non-Goals:**
- No changes to message repository models, timestamps, or server payloads.
- No redesign of incoming or system message semantics beyond preserving their current attributed row structure.
- No changes to message composer, workshop entry points, or AIGC menu behavior.
- No changes to timestamp formatting rules or localization behavior.

## Decisions

### 1. Treat outgoing messages as compact self-bubbles instead of attributed rows

Outgoing messages will stop rendering the self-avatar and `You` sender label, while incoming and system messages will keep the current avatar-leading structure. The row container can still use the same shared message renderer, but it should branch the top-level layout so self-authored messages take a denser bubble-only form.

Why this decision:
- It directly satisfies the requested behavior and removes redundant chrome from the most frequent user-authored state.
- It preserves the existing message color system, so the self-bubble still reads as owned by the user without needing extra labels.
- It avoids introducing a separate message model or changing repository contracts.

Alternatives considered:
- Keep the current row structure and only hide the `You` label: rejected because the self-avatar would still waste horizontal space.
- Hide the self-avatar and keep the label: rejected because the label alone still adds avoidable vertical height.

### 2. Anchor the outgoing timestamp inside the bubble footer

The outgoing timestamp will move into a footer-aligned position inside the bubble, hugging the lower-right edge with tighter spacing than the current standalone text row. Incoming and system messages can keep their existing timestamp treatment unless the implementation naturally shares a denser footer container without reducing readability.

Why this decision:
- It preserves the current timestamp format while reducing the empty space below outgoing messages.
- It matches common mobile chat density patterns where self-authored timestamps are secondary and tucked into the bubble edge.
- It can be expressed through Compose alignment and padding changes without altering data flow.

Alternatives considered:
- Remove outgoing timestamps entirely: rejected because the user asked to keep the existing timestamp form.
- Move all timestamps outside the bubble but closer: rejected because it does not recover as much vertical space.

### 3. Lock the behavior with direction-specific UI assertions

Compose UI coverage will verify that outgoing messages do not expose self-avatar or `You` nodes, while incoming and system messages still expose their identity affordances. Tests will also assert that the outgoing timestamp stays inside the bubble footprint so future refactors do not reintroduce extra row height.

Why this decision:
- The requested change is highly visual and easy to regress accidentally during timeline refactors.
- Existing `GkimRootAppTest` infrastructure already covers chat layout and provides the right place for this behavioral contract.
- Direction-specific assertions make the acceptance criteria explicit for apply sessions.

Alternatives considered:
- Rely on manual emulator review only: rejected because the repository requires automated verification evidence.
- Add snapshot-only coverage: rejected because geometry and node-presence assertions are more precise for this layout change.

## Risks / Trade-offs

- [Outgoing bubble-only layout could drift too far from current shared row structure] -> Keep a single message-row composable with small direction branches instead of splitting into unrelated renderers.
- [Timestamp moved inside the bubble could overlap long messages or attachments] -> Use footer alignment and spacing rules that keep timestamp content on its own trailing line when needed.
- [Node removals can break existing UI tests that assumed all directions shared the same structure] -> Update tests alongside the layout change and assert the new direction-specific behavior explicitly.

## Migration Plan

1. Update the `core/im-app` delta spec so outgoing self-message density rules are explicit before implementation starts.
2. Refactor the chat timeline renderer to branch outgoing messages into a compact self-bubble layout while preserving incoming and system attribution.
3. Add or update Compose UI tests for hidden self-avatar/self-label nodes and in-bubble outgoing timestamp placement.
4. Verify each task slice with instrumentation and unit checks, record delivery evidence, and only then mark tasks complete.

## Open Questions

- None. The requested layout change is narrow and can be implemented without additional product clarification.
