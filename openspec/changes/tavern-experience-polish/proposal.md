## Why

After the core tavern functionality (LLM chat, memory + preset, user persona, ST card interop, Settings reframe, world info) is in place, the product still carries rough edges that users from SillyTavern, Lobe-chat, or similar tavern-shaped products will notice. Specifically, `llm-text-companion-chat` explicitly deferred **edit-any-bubble** and **arbitrary-layer regenerate** to a follow-up, and the tavern surface still only renders small avatars despite shipping deep persona cards with high-quality portrait art. A handful of other polish concerns have accumulated: no alt-greeting picker on chat open, no creator attribution surfacing (despite the card carrying it), no chat history export, no per-character provider override, no full-relationship reset, and the gacha roster flow still lacks visible probability and duplicate-handling animation.

This slice bundles eight polish concerns into a single coherent proposal. None of them are structural on their own; together they close the gap between "functionally complete" and "tavern-class polish".

## What Changes

- **Portrait large-view**: tapping a companion's avatar on the tavern or chat surface opens a full-screen portrait view with pinch-to-zoom, falling back gracefully when the card has no high-resolution avatar.
- **Alt-greeting picker on chat open**: when a user opens a companion conversation with an empty history, the existing first-message flow is extended with a picker listing the card's `firstMes` plus every `alternateGreetings` entry, localized, so the user can choose which greeting seeds the conversation. (The picker already exists in `llm-text-companion-chat` for the opener; this slice refines its presentation, adds a visible preview of each option, and ensures the selected greeting becomes the `variantIndex=0` opener.)
- **Chat branch tree navigation** (edit-any-bubble, arbitrary-layer regenerate, sibling swipe at any depth): every companion message exposes swipe-left / swipe-right siblings regardless of depth; every user message exposes an "Edit" affordance that creates a new sibling variant group under the same `parentMessageId`; every companion message exposes a "Regenerate from here" action that creates a new sibling variant group under the same parent. The active path through the branch tree is explicit and user-navigable.
- **Per-character provider / preset override**: a companion card can optionally declare `characterPresetId`, which shadows the user's globally active preset while the card is active. The active preset UI surfaces the override clearly when present; unsetting the override reverts to the globally active preset.
- **JSONL chat export**: from the conversation overflow menu, a user can export the active path of the current conversation as JSONL (one message per line, with full variant metadata), usable for offline review or re-import into other tavern products. Share sheet + Downloads target, same affordance shape as the ST card-export dialog.
- **Full relationship reset**: a "Reset relationship" affordance from the character-detail screen, behind a confirmation dialog, clears every conversation with this companion plus the memory record (rolling summary + pinned facts) for the user-companion pair. The companion card itself is not deleted. This is distinct from the memory-only reset from `companion-memory-and-preset`.
- **Gacha probability + duplicate-handling animation**: the gacha roster flow (from `replace-space-with-character-roster-and-gacha`) surfaces the rarity / drop probability of each result, and when a draw produces a card the user already owns, the animation branches to a dedicated "Already owned" variant with a consolation affordance (convert to shards, or equivalent placeholder semantics that we can extend later).
- **Creator attribution display**: card-detail renders `creator`, `creatorNotes`, and `characterVersion` in a dedicated "About this card" sub-surface so imported cards credit their authors visibly.

## Capabilities

### New Capabilities
- `tavern-experience-polish`: Defines the portrait-view contract, the alt-greetings-picker refinement, the chat branch-tree navigation contract (edit-user, arbitrary-layer regenerate, sibling-swipe at any depth), the per-character preset override, the JSONL export shape, the full-relationship-reset contract, the gacha probability + duplicate surfacing, and the creator-attribution sub-surface.

### Modified Capabilities
- `core/im-app`: Android tavern + chat + gacha surfaces gain the polish affordances enumerated above, plus a new character-detail sub-surface for creator attribution.
- `im-backend`: Backend contract for the branch-tree navigation (edit-any-bubble endpoint, arbitrary-layer regenerate endpoint), JSONL export endpoint, relationship-reset endpoint, and the per-character preset override field on character records. The gacha probability surfacing is purely a client-side render of existing backend data; no backend change for that item.

## Impact

- Affected Android code: `feature/chat/ChatRoute.kt` (bubble affordance extensions for edit-any-bubble + arbitrary-layer regenerate + branch navigation), `feature/tavern/CharacterDetailRoute.kt` (portrait view, creator sub-surface, relationship-reset affordance, per-character preset override UI), the gacha roster flow (probability + dedup animation), new `feature/chat/ChatExportDialog.kt`, new `feature/tavern/PortraitLargeViewRoute.kt`. Settings gains no new top-level entries for this slice.
- Affected backend contract: new endpoints for edit-user-turn, arbitrary-layer regenerate (the existing regenerate endpoint extends to accept a `fromMessageId` target), relationship reset, and JSONL export. The `characterPresetId` field is added to the character record.
- Affected specs: new `tavern-experience-polish`, plus deltas for `core/im-app` and `im-backend`.
- Affected UX: tavern feels closer to a SillyTavern / Lobe-chat experience; chat supports deep editing + branching; users can archive conversations.
- Non-goals (scoped out of this slice):
  - Conversation import from JSONL (only export)
  - Group chats (multi-companion conversations)
  - Custom gacha rarity rules / weighted drops beyond the existing backend contract
  - Animated portraits / sprites / expression-by-emotion
  - Multi-page character-detail layout (one-screen with scrolling is fine)
  - Voice / TTS for the read-out of messages
  - Per-conversation preset override (only per-character in this slice)
  - Chat branch tree visualization (a dedicated tree-graph surface) — branches are navigable inline via sibling swipe; a tree-view is polish for a later slice
