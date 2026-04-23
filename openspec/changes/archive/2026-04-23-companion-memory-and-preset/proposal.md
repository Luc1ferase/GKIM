## Why

The `llm-text-companion-chat` slice lands lifecycle, variant tree, and persona prompt assembly, but it explicitly scopes out bounded memory beyond a recent-N-turns window and any notion of a reusable prompt "preset". The `ai-companion-experience` capability and the `pivot-to-ai-companion-im` parent change both require bounded memory summaries and a durable server-side conversation context, and the tavern ecosystem expects swappable prompt presets (SillyTavern/Lobe-chat convention). Without this slice, every companion conversation drifts into incoherence after ~10 turns, users cannot pin facts the companion should remember, and there is no way to try different system prompt styles without rewriting every card.

## What Changes

- **BREAKING** Companion conversations gain a durable memory layer (rolling summary + pinned facts) scoped per user-companion pair, and every companion turn prompt is built through a deterministic token budget that includes that memory layer.
- Introduce a dedicated capability covering memory persistence, memory management (view, pin, unpin, reset), the Preset concept (named bundles of prompt-template and provider-parameter fields), preset library CRUD, and the deterministic budget strategy.
- Modify the Android app contract so Settings and companion chat surfaces expose memory review + reset controls, preset library management, and active-preset selection, and so the user can pin a past message as a long-lived fact.
- Modify the backend contract so memory summaries, pinned facts, and the preset library persist server-side, participate in companion prompt assembly alongside the deep persona record introduced by `deepen-companion-character-card`, and follow a deterministic token budget and summarization schedule.
- Reuse the `llm-text-companion-chat` submit contract and prompt-assembly entry point; this slice adds inputs to that assembly without redefining the turn lifecycle.

## Capabilities

### New Capabilities
- `companion-memory-and-preset`: Defines per-companion bounded memory (rolling summary + pinned facts), the Preset bundle shape, preset library semantics, deterministic token budgeting for prompt assembly, and the controls a user can invoke to manage both.

### Modified Capabilities
- `core/im-app`: The Android app requirements change so Settings exposes preset library management + active-preset selection, and so companion chat exposes per-companion memory review, pin/unpin, and reset controls.
- `im-backend`: The backend requirements change so memory state and preset state persist server-side, participate in prompt assembly, and are managed through dedicated HTTP endpoints.

## Impact

- Affected Android code: `core/model/CompanionModels.kt` + new memory/preset model types, `data/remote/im/ImBackendClient.kt` + DTOs for memory and preset endpoints, `data/repository/*` (new `CompanionMemoryRepository` + `CompanionPresetRepository`), `feature/settings/*` (preset library UI), `feature/chat/*` (memory panel entry, pin/unpin on message row), plus minor extensions to the companion conversation chrome.
- Affected backend contract: new `companion_memories` + `companion_memory_pins` + `companion_presets` tables or equivalent, memory management endpoints, preset management endpoints, summarization scheduler, deterministic token-budget assembler that composes preset + persona + memory + recent-turns.
- Affected specs: new `companion-memory-and-preset`, plus deltas for `core/im-app` and `im-backend`.
- Affected UX: users can see a per-companion memory summary, pin any message as a long-lived fact, reset memory with three granularities (pinned only / summaries only / all), create / edit / activate presets, and observe the active preset in the companion chat chrome.
- Non-goals (scoped out of this slice):
  - Tiered (short/mid/long-term) memory; episodic memory with embedding retrieval (future follow-up slices; the single-summary + pinned-facts model here is forward-compatible)
  - Per-character preset override (→ `tavern-experience-polish`)
  - World Info / lorebook injection (→ `world-info-binding`)
  - User persona self-description beyond `{{user}}` display-name substitution (→ `user-persona`)
  - Automatic extraction of "entities" or "relationships" from conversation (future)
  - UI for viewing provider-level token accounting or cost
  - Cross-companion memory sharing or public preset marketplace
