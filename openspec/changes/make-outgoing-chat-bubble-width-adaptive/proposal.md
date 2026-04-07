## Why

The current outgoing chat bubble layout still leaves an unreasonable visual artifact: even very short self-authored text messages can render as overly wide bubbles. This reduces message density, wastes horizontal space, and makes the chat timeline feel less natural now that outgoing rows are otherwise compact.

## What Changes

- Make short outgoing text bubbles size themselves closer to their content width instead of stretching unnecessarily wide.
- Preserve readable width behavior for longer outgoing text so multi-line messages still wrap cleanly on mobile screens.
- Keep attachment-bearing and incoming/system message layouts stable unless width constraints must branch explicitly for outgoing text-only bubbles.
- Add or update Compose UI coverage that verifies short outgoing bubbles stay compact without regressing timestamp/footer placement.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: refine chat detail requirements so short outgoing self-authored text bubbles hug content width more closely while longer outgoing content still respects mobile readability constraints.

## Impact

- Affected code: Android chat timeline composable(s), outgoing bubble width constraints, and chat UI tests.
- Affected specs: `openspec/specs/core/im-app/spec.md` via a delta spec for chat detail behavior.
- Affected verification: targeted chat instrumentation coverage for outgoing bubble geometry and width behavior.
