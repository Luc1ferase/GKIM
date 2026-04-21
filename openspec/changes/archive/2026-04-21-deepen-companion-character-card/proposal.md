## Why

The AI companion branch has already replaced the `Space` tab with a tavern-style roster, but the underlying `CompanionCharacterCard` only carries four display-oriented fields (`displayName`, `roleLabel`, `summary`, `openingLine`). That shape is enough to render a card list but not enough to drive tavern-style conversation: there is no persistent system prompt, no personality/scenario framing, no example dialogue to anchor style, no alternate greetings, no tags for search/filtering, no creator attribution, no version, and no authored avatar image. Without those fields the app cannot produce a persona-coherent companion reply, cannot accept SillyTavern-ecosystem cards (future change 2), and cannot let users author their own companions.

## What Changes

- Expand the companion card domain model on Android so each card carries a full tavern-grade persona record: system prompt, personality, scenario, example dialogue, first-message greeting, alternate greetings, tags, creator attribution, character version, optional local avatar URI, and a forward-compatible `extensions` bag.
- Replace the current static seed-only `CompanionRosterRepository` contract with one that supports create / update / delete while keeping preset entries read-only.
- Rename `feature/space` to `feature/tavern` and add a character detail route plus a character editor route so users can inspect and author companion cards. Preserve the existing tab location and current tavern shell chrome.
- Update the backend companion-roster contract so the deeper persona record is durable server-side, including per-field bilingual variants and forward-compatible extension data. The public repo ships the contract only; the private backend implements it.
- Keep the existing bilingual contract: every new human-authored text field (system prompt, personality, scenario, example dialogue, first-message, alternate greetings) MUST use `LocalizedText`. `tags`, `creator`, `characterVersion` stay single-string since they are not user-facing prose.

## Capabilities

### New Capabilities

- `companion-character-card-depth`: Defines the full tavern-grade persona record that a companion character card must expose, covering persona instructions, scenario framing, example dialogue, greetings, authoring metadata, and forward-compatible extensions.

### Modified Capabilities

- `core/im-app`: Android requirements change so the tavern surface can present character detail and editing flows backed by the deeper persona record, and so companion-chat entry carries the full persona context rather than only a display label.
- `im-backend`: Backend requirements change so companion persistence, roster APIs, and active-selection responses expose the deeper persona record with bilingual text and extension passthrough.

## Impact

- Affected Android code: `core/model/CompanionModels.kt`, `data/repository/CompanionRosterRepository.kt`, `data/repository/BackendAwareCompanionRosterRepository.kt`, `data/repository/SeedData.kt`, `feature/space/SpaceRoute.kt` (moved to `feature/tavern/TavernRoute.kt`), plus two new routes under `feature/tavern/`.
- Affected backend: `companion_characters` schema plus roster/draw/active-selection serialization. Only the contract is updated in this public repo.
- Affected specs: new `companion-character-card-depth`, plus deltas for `core/im-app` and `im-backend`.
- Non-goals for this slice:
  - No LLM text generation or streaming lifecycle (that is change 3 `llm-text-companion-chat`).
  - No SillyTavern PNG / JSON import/export (change 2).
  - No user personas, world info, preset, or memory (changes 4-6).
  - No rewrite of gacha / draw behavior; draw keeps working on the expanded card shape.
