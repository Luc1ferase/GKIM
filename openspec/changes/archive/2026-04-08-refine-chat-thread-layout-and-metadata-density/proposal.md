## Why

The current chat detail screen still has several layout behaviors that feel unlike a mature mobile messenger: the composer does not stay visually pinned to the bottom, incoming metadata alignment wastes space, and timestamps consume too much room relative to message content. Tightening these patterns now will improve chat readability and make the thread feel closer to the density and composure users expect from apps like Telegram.

## What Changes

- Keep the chat composer visually anchored to the bottom of the screen instead of allowing the thread layout to make it appear to drift downward as messages accumulate.
- Align incoming avatars and timestamps on the same horizontal line and move incoming timestamps to the right side so metadata behaves more consistently across both message directions.
- Reduce timestamp visual weight and spacing so metadata does not consume disproportionate room inside or around message bubbles.
- Refine chat bubble spacing and metadata placement using Telegram's default-theme chat layout as a reference for density and rhythm, without cloning Telegram verbatim.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: refine chat detail requirements for bottom-anchored composer behavior, denser incoming metadata alignment, right-aligned incoming timestamps, and more compact timestamp presentation.

## Impact

- Affected code: Android chat screen layout, message-row metadata layout, composer anchoring behavior, and chat UI instrumentation tests.
- Affected specs: `openspec/specs/core/im-app/spec.md` via a delta spec for chat detail behavior.
- Affected verification: Compose UI geometry and layout assertions for composer position, timestamp alignment, and message metadata density.
