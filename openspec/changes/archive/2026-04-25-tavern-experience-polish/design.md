## Goal

Bundle eight independent polish concerns into one coherent slice. Each concern lands its minimum-viable contract without depending on the others, so that the slice is implementable in any order and degrades cleanly if one sub-feature ships late.

## Constraints

- Additive to every prior slice: this proposal rewrites no requirement from `llm-text-companion-chat`, `companion-memory-and-preset`, `user-persona`, `sillytavern-card-interop`, `companion-settings-and-safety-reframe`, or `world-info-binding`. It extends existing contracts with new endpoints and new card fields.
- Bilingual where copy is user-visible, through the `LocalizedText` contract.
- Service-boundary preserved: the JSONL export is a backend-emitted payload, not a client-side serialization. Edit-user-turn and arbitrary-layer regenerate create new messages through the existing companion-turn endpoint shape.

## Key design decisions

### 1. Portrait large-view

- Tapping a companion's avatar anywhere (tavern card, chat header, chat bubble avatar) opens `PortraitLargeViewRoute`.
- The route renders the card's highest-resolution avatar source (the re-encoded PNG from `sillytavern-card-interop` import, or the user-captured image) full-screen over a blur-layer backdrop.
- Pinch-to-zoom + single-finger pan; double-tap toggles 1×/2× zoom. No editing or sharing from this route.
- If the card has no avatar (rare edge case), the route falls back to a placeholder with the card's display name and an "Edit card" shortcut.

Alternatives considered:
- **Inline expand-on-tap within the avatar's own layout frame**: rejected — the inline frame cannot give the portrait the full-screen context it needs on small devices.
- **Multiple portrait slots per card (expression system)**: out of scope; SillyTavern-style expression packs are a future slice.

### 2. Alt-greetings picker refinement

The opener picker already exists (from `llm-text-companion-chat`). This slice refines its presentation:

- Each option (the `firstMes` plus every `alternateGreetings` entry) renders as a card with a preview of the greeting's first ~120 characters, localized to the user's active `AppLanguage`.
- The user can tap to preview the full greeting before committing.
- Selecting an option commits it as the conversation's `variantIndex=0` opener (unchanged from prior slice); the picker remembers the user's last selection for this companion so re-opening with an empty history (after relationship reset) defaults to the most-recently-picked option.

Rationale:
- A visible preview reduces accidental picks on long greeting lists.
- Remembering the last selection across reset makes relationship-reset feel ergonomic for iterative users.

### 3. Chat branch tree navigation: edit-any-bubble, arbitrary-layer regenerate, sibling swipe at any depth

The variant tree model from `llm-text-companion-chat` (every `ChatMessage` carries `parentMessageId`, `variantGroupId`, `variantIndex`; a variant group is the sibling set under one parent) is already designed to support these — this slice ships the remaining UI + endpoint work.

#### 3a. Sibling swipe at any depth

- Every companion bubble and every user bubble exposes left/right chevrons when `siblingCount > 1` on its variant group.
- Tapping a chevron switches the active path to the adjacent sibling. The switch is persistent (stored as the conversation's `activePath` — a map of `variantGroupId → variantIndex`).
- Active-path resolution is: for each variant group in the conversation, pick the sibling at the group's active index; render the path from root to most-recent-message along that selection.

#### 3b. Edit-any-bubble

- Every **user** bubble exposes an "Edit" overflow action.
- Editing a user bubble creates a new `variantGroupId` under the same `parentMessageId` as a sibling (same semantics as the existing swipe model, but for user turns). The backend re-runs companion-turn generation for the edited sibling. The new user + companion branch becomes the active path; the original branch remains intact as a non-active sibling.
- Endpoint: `POST /api/companion-turns/{conversationId}/edit` with `{ parentMessageId, newUserText }`. Returns the new user-message sibling plus kicks off the companion turn stream.

#### 3c. Arbitrary-layer regenerate

- Every **companion** bubble (not just the most recent) exposes a "Regenerate from here" overflow action.
- Regenerate appends a new sibling under the same `variantGroupId` as the selected bubble. The active path switches to the new sibling; downstream conversation that existed under the previously-active sibling is preserved as a non-active branch.
- Endpoint: the existing regenerate endpoint from `llm-text-companion-chat` is extended to accept an `at-message` parameter — `POST /api/companion-turns/{conversationId}/regenerate` with `{ targetMessageId }` — creating a new sibling under the same variant group.

Rationale:
- Edit and regen both append siblings under the existing `variantGroupId` / `parentMessageId` model; no new data model needed.
- Preserving non-active branches means users can always swipe back to recover prior content.
- The two endpoints are symmetric; edit creates a user-side sibling, regenerate creates a companion-side sibling.

Alternatives considered:
- **In-place edit that overwrites the bubble**: rejected — loses prior content; tavern products expect reversible history.
- **Separate tree-view surface**: out of scope; branches are navigable inline via swipe.

### 4. Per-character provider / preset override

- A companion card gains an optional `characterPresetId: String?` field pointing at a preset in the user's library.
- When a card with a non-null `characterPresetId` is active, the allocator uses that preset instead of the user's globally active preset for that conversation.
- The UI surfaces the override: the chat chrome's preset pill shows the override label with a visually distinct "(card override)" suffix; tapping it routes to the card's detail surface where the override can be removed.
- Removing the override (setting `characterPresetId = null`) reverts the conversation to the globally active preset.

Rationale:
- Per-character preset is the single most requested override in ST forums — users want "RP-grim" tone for dark cards and "cheerful-concise" for slice-of-life cards without manually swapping presets.
- The field lives on the card (persistent through import/export where the value is preserved under `extensions.st.charPresetId` for ST interop — but since ST's format doesn't have a native equivalent, the field won't round-trip across ST exports; it's preserved on our own persisted record).

Out-of-scope:
- Per-conversation override (would require adding the field to conversations) — explicitly deferred.

### 5. JSONL chat export

- From the chat overflow menu, a user can tap "Export as JSONL".
- A dialog (same shape as the ST card-export dialog from `sillytavern-card-interop`) offers: active-path-only vs. full tree, target language selector (default = active `AppLanguage`), share sheet vs. Downloads target.
- Endpoint: `GET /api/conversations/{conversationId}/export?format=jsonl&pathOnly=true|false` returning one JSON object per line:
  - `{ messageId, parentMessageId, variantGroupId, variantIndex, role, timestamp, content, extensions }`
- Active-path-only export emits only the path's messages. Full-tree export emits every message in the conversation regardless of active path.

Rationale:
- JSONL is the de facto format for tavern chat dumps; users can re-ingest into ST, Lobe-chat, or dedicated tools.
- Active-path-only is the common case; full-tree preserves branches for power users.

### 6. Full relationship reset

- From the character-detail surface, a "Reset relationship" affordance (behind a two-step confirmation dialog) clears:
  - Every `Conversation` between the user and this companion.
  - The `MemoryRecord` (rolling summary + pinned facts) for the user-companion pair.
  - The last-selected alt-greeting for this companion.
- The companion card itself, its bindings (world-info lorebooks), and its user-library data (presets, personas) are NOT deleted.
- Endpoint: `POST /api/relationships/{characterId}/reset` — idempotent.

Rationale:
- Memory-reset from `companion-memory-and-preset` clears memory only; users asking "start over with this character" also want conversation history cleared. Separate affordance with an unambiguous confirmation prevents accidental data loss.
- Distinct from deleting the card (which would wipe everything including the card record and lorebook bindings).

### 7. Gacha probability + duplicate-handling animation

- The existing gacha roster flow (from `replace-space-with-character-roster-and-gacha`) already backs draws with probability data from the backend. This slice adds:
  - **Probability surfacing**: before a draw, the UI shows the rarity breakdown (e.g., "Rare 5%, Epic 1%, Common 94%") drawn from the backend catalog response.
  - **Duplicate-handling animation**: when the drawn card is already owned, the result animation branches into a dedicated "Already owned" variant that renders a consolation affordance — for this slice, a placeholder "Keep as bonus" CTA that accepts the duplicate and records a `bonusAwarded` event against the user's account. Actual shard/currency conversion is deferred; the hook is in place.
- Duplicate detection: a drawn card id that already appears in the user's owned roster triggers the duplicate variant.

Rationale:
- Users expect both: probability disclosure (regulatory trend + ST/card-game UX convention) and duplicate-handling (gacha games universally degrade duplicates into something).
- Placeholder consolation semantics (`bonusAwarded`) let us ship the UX now without committing to a currency system; the backend can accrue these events for a future reward system.

### 8. Creator attribution display

- The character-detail surface gains an "About this card" sub-section rendering:
  - `creator` (string)
  - `creatorNotes` (string, scrollable if long)
  - `characterVersion` (string)
  - `stSource` from `extensions.st.stSource` when present (URL linkified, opens in the system browser)
  - `stCreationDate` / `stModificationDate` from `extensions.st.*` when present (formatted per locale)
- Missing fields are hidden (not shown as empty rows).
- The sub-section is always below the persona-prose fields in the scroll; not collapsible in this slice.

Rationale:
- Credit is morally-required for imported ST cards; the fields already exist on the record and just need rendering.
- Linkifying `stSource` closes the loop to the card's origin page (Chub, Hybrid, etc.).

## Risks

- **Branch-tree UI makes the chat screen feel cluttered**: Mitigation — affordances (Edit / Regenerate from here / swipe chevrons) live in an overflow menu, not inline; only the chevrons appear on the bubble when `siblingCount > 1`. Default rendering stays clean.
- **JSONL export leaks provider artifacts** (token counts, raw provider response, etc.): Mitigation — the export only emits the cleaned `content` + structural fields; provider-specific debug data lives in a separate server log, not the export.
- **Per-character override conflicts with in-flight preset activation**: Mitigation — the override is resolved at assembly time, so changing the override mid-stream does not affect the in-flight turn. Clear UI signal (the pill label) tells users what's active.
- **Relationship reset accidentally destroys valuable history**: Mitigation — two-step confirmation (tap once to arm, tap again to confirm) with a localized destructive-action warning.
- **Gacha probability disclosure can vary by locale (regulatory)**: Mitigation — the backend returns catalog probabilities; the UI renders what the backend sends. If a locale requires special treatment, the backend controls visibility.
- **Duplicate-handling consolation is a placeholder**: Mitigation — document clearly that the `bonusAwarded` event is a hook without a settled reward; future slice can wire actual shard conversion.
- **Arbitrary-layer regen forks can proliferate**: Mitigation — non-active branches are preserved but never rendered unless the user navigates to them via sibling swipe. Storage is cheap; the cost is negligible.
- **Alt-greeting picker default-to-last-selected can surprise users on unrelated reset**: Mitigation — explicit "Remembered from last time" hint below the last-selected card in the picker.

## Rollout

1. Backend: new endpoints (edit-user-turn, regenerate-with-target, relationship-reset, JSONL export), new `characterPresetId` field on the character record, probability field in gacha catalog response (likely already present; verify), duplicate-detection flag in draw response.
2. Android: extend `ChatMessageRow` with the new affordances (overflow actions + sibling chevrons) and the active-path resolution logic; add `PortraitLargeViewRoute`; extend character detail with the creator sub-section, override UI, and reset affordance; extend gacha result animation with the probability + dedup branches; add the JSONL export dialog.
3. Verification: unit (active-path resolution, sibling-index mutation, dedupe detection), integration (edit-user-turn + regen-with-target create correct sibling structures; JSONL export round-trips), instrumentation (portrait view pinch-to-zoom, gacha dedup animation playback).

## Trade-offs

- One slice vs. two: chose one because all eight concerns are polish and each is small on its own; keeping them together reduces archival overhead and gives the user a clear "tavern polish shipped" milestone.
- Tree view vs. inline navigation: inline is simpler; a tree-graph visualization is polish for a later slice if demanded.
- Per-character vs. per-conversation override: per-character is simpler and ships the 90% use case; per-conversation is deferred.

## Edge cases

- Character with no `characterPresetId` behaves exactly as today; override UI is absent.
- Conversation with zero companion bubbles (opener only) — sibling swipe and regen-at-message only appear on companion bubbles, so the opener's sibling swipe is limited to the opener's variant group (typically the multiple alt-greetings chosen historically or regenerated via existing flow).
- JSONL export on an empty conversation returns an empty response body (no rows).
- Relationship reset on a companion with zero conversation history is a no-op but still clears any lingering memory record.
- Gacha dedup on first-ever draw for a specific card = no duplicate variant (card is new to the user).
- Portrait large-view on a card whose avatar is a small user-captured thumbnail: route still opens at full-screen but the upscaled image may look pixelated; a hint below the image explains the original resolution.
- Creator fields for a non-imported card (user-authored) are often empty; hidden entirely when empty rather than showing as "Unknown creator".

## Open questions

- Should the JSONL export filename include a hash of the conversation id for disambiguation? Default: yes, suffix `_<first 8 chars of conversationId>`.
- Should sibling chevrons show `n / total` (e.g. `2 / 5`) beside them? Default: yes, as a small caption; helps users know how deep a branch is.
- Should relationship reset preserve the card's `dialogueFavorited` / pinned conversation indicators? Default: pinned-conversation state is cleared (since conversations are deleted); favorite/relationship-level indicators on the card stay.
- Should the per-character override be exported via `sillytavern-card-interop`? Default: preserve the field under `extensions.st.charPresetId` for lossless round-trip within our own import/export, but ST's format has no direct slot, so cross-product round-trip is lossy (documented).
