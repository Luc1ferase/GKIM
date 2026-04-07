## Context

The Android chat timeline already renders outgoing messages as compact self-bubbles without avatar chrome, but the current implementation still lets short outgoing text rows expand wider than their content warrants. The likely cause is that the outgoing bubble footer uses a full-width container so the timestamp alignment logic can sit on the trailing edge, which unintentionally stretches otherwise short bubbles.

This change is intentionally narrow: it should refine outgoing text-bubble sizing without redesigning the chat surface, changing timestamp format, or destabilizing long-message and attachment layouts that already pass instrumentation coverage in `GkimRootAppTest`.

## Goals / Non-Goals

**Goals:**
- Let short outgoing text-only bubbles shrink closer to content width.
- Preserve readable wrapping for longer outgoing text on mobile screens.
- Keep incoming and system rows visually unchanged.
- Keep outgoing timestamp/footer alignment intact while removing the width inflation side effect.
- Add targeted UI coverage for short and long outgoing bubble geometry.

**Non-Goals:**
- No changes to message data models, timestamps, or send flow behavior.
- No redesign of incoming/system bubble width rules.
- No change to attachment preview sizing beyond keeping current layout stable.
- No global typography, color, or spacing refresh for the chat screen.

## Decisions

### 1. Split outgoing bubble width behavior by content shape

Outgoing text-only bubbles will use content-aware width constraints, while outgoing rows containing attachments will continue to use the broader, media-friendly layout. This keeps short messages visually compact without risking clipped or awkward media composition.

Why this decision:
- It solves the user-visible defect at the narrowest point of control.
- It avoids penalizing attachment rows, which benefit from a wider canvas.
- It maps cleanly onto the existing message-row branching by direction and content.

Alternatives considered:
- Apply one global max-width rule to every outgoing bubble: rejected because it does not guarantee short-message compaction and could still leave footer-driven stretching in place.
- Shrink all outgoing bubbles, including attachment rows: rejected because media previews need a stable larger presentation area.

### 2. Decouple timestamp alignment from bubble expansion

The outgoing timestamp/footer should align to the trailing edge without forcing the bubble to occupy the maximum available width. The implementation should prefer trailing alignment primitives that do not introduce unconditional `fillMaxWidth` behavior for short content, or should scope width-filling behavior to a bounded inner footer after the bubble width has already been resolved.

Why this decision:
- The current defect is a layout coupling problem, not a missing alignment rule.
- Fixing the footer contract is more durable than tuning magic width values around it.
- It preserves the existing timestamp visual language while restoring proper bubble sizing.

Alternatives considered:
- Remove the footer container and place the timestamp inline with message text: rejected because it would hurt readability and regress the current lower-right footer pattern.
- Use hardcoded width buckets for short messages: rejected because it would be brittle across devices, fonts, and localization.

### 3. Lock behavior with geometry-based Compose assertions

The updated tests should distinguish between short outgoing text bubbles and longer outgoing text bubbles. Short messages should verify that the bubble width is materially tighter than the available row width, while long messages should still prove that wrapping and footer placement remain readable.

Why this decision:
- The regression is mostly geometric, so node-presence checks alone are not enough.
- Existing instrumentation already uses bounds-based assertions and is the right place to extend this contract.
- Geometry thresholds can stay implementation-tolerant while still catching obvious width regressions.

Alternatives considered:
- Manual emulator inspection only: rejected because the repository requires per-task automated verification evidence.
- Snapshot testing only: rejected because semantic geometry assertions are more stable for Compose layout behavior.

## Risks / Trade-offs

- [Short-bubble thresholds become too strict across densities or font metrics] -> Use relative or bounded geometry assertions instead of pixel-perfect expectations.
- [Footer refactor accidentally breaks timestamp placement] -> Keep dedicated outgoing footer assertions and rerun the full chat instrumentation suite.
- [Width branching adds complexity to `ChatMessageRow`] -> Limit branching to outgoing text-only vs outgoing attachment-bearing cases and avoid unrelated refactors.

## Migration Plan

1. Update the `core/im-app` delta spec to state that short outgoing text bubbles must adapt to content width while preserving readable long-message behavior.
2. Add failing Compose UI coverage for short outgoing bubble width and for long-text/footer stability.
3. Refine the outgoing bubble width and footer layout implementation in the chat timeline composable.
4. Run targeted and full chat verification, then record review/upload evidence in the change tasks.

## Open Questions

- None. The scope is narrow enough to proceed with reasonable width heuristics and instrumentation-backed verification.
