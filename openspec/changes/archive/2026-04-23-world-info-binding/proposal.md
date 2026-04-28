## Why

The tavern product inherits the SillyTavern convention that a companion's world — its setting, lore, and environmental facts — is encoded as a **lorebook** (also called "world info"): a collection of keyword-triggered prose entries that inject when the conversation touches their keys. Without this, the companion either forgets setting-specific facts after the persona + scenario are exhausted, or the user has to stuff everything into the persona card's prose fields and burn token budget on always-on context.

`companion-character-card-depth` and `sillytavern-card-interop` both surface `extensions.st.characterBook` fields on deep persona records and ST imports, but nothing in the runtime actually reads them. `companion-memory-and-preset` finalized the deterministic token-budget allocator with a documented priority order, leaving a clean slot for lorebook injection to land.

This slice ships the minimum viable lorebook runtime: a persisted **Lorebook** model with **Entry** records carrying bilingual content, a keyword-based scan over the last N turns (with always-on "constant" entries as an escape hatch), character-scoped binding plus a user-owned global pool, and a new section in the token-budget allocator so matched entries are injected deterministically under the existing budget. Imported ST `character_book` payloads auto-populate the bound lorebook on first import; on export, the bound lorebook round-trips into `character_book` losslessly.

Per-user vector search, probability-gated entries, nested groups, shared-catalog marketplaces, and cross-user sharing are deferred to later slices. This one stays keyword-exact and deterministic.

## What Changes

- **BREAKING** A new `Lorebook` and `LorebookEntry` model is introduced on both the backend and the Android client. Lorebooks are owned by the authenticated user; each lorebook is bindable to zero or more characters, and a new global-pool flag lets the user's enabled global lorebooks participate in every companion turn regardless of the active character's bindings.
- Every companion turn's prompt assembly runs a **deterministic keyword scan** over the current user turn plus the last `scanDepth` turns (default 3, per-entry overridable). Matched triggered entries plus all always-on (`constant=true`) entries from the active character's bound lorebooks + the user's enabled global lorebooks collapse into a single `worldInfoEntries` bundle, sorted by `insertionOrder` ascending (lower = higher priority), capped by a per-lorebook token budget and the existing allocator's overall budget.
- The deterministic allocator from `companion-memory-and-preset` gains one new section, `worldInfoEntries`, inserted between `pinnedFacts`/`userPersonaDescription` (above) and `rollingSummary` (below) in priority, and between `rollingSummary` and non-critical preset sections in drop order. When over budget, the allocator drops the lowest-priority entries within the bundle (highest `insertionOrder`) before dropping the entire section.
- The Android Settings screen (from `companion-settings-and-safety-reframe`) gains a **World Info** entry under the `Companion` section: a list of the user's lorebooks with create/edit/delete/duplicate CRUD, a binding manager per character card, and per-entry inline edit with bilingual content through the `LocalizedText` contract.
- The character card-detail surface exposes a **Lorebook** tab showing the character's bound lorebook(s) read-only for quick inspection with a link into the editor. Embedded `extensions.st.characterBook` from imported ST cards auto-populates a freshly-created lorebook bound to the imported character on first commit; subsequent character-card exports (via `sillytavern-card-interop`) round-trip the bound lorebook into the `character_book` slot losslessly.

## Capabilities

### New Capabilities
- `world-info-binding`: Defines the lorebook + entry model, the keyword scan algorithm, the binding semantics (character-scoped + global pool), the deterministic injection contract into the allocator, and the import/export round-trip with ST `character_book`.

### Modified Capabilities
- `core/im-app`: Android gains a lorebook library surface in Settings, a character-card lorebook tab, and lorebook-scoped editing affordances.
- `im-backend`: Backend gains lorebook CRUD endpoints, binding CRUD, the server-side scan algorithm, allocator integration (placing `worldInfoEntries` between `userPersonaDescription` and `rollingSummary` in the existing deterministic allocator from `companion-memory-and-preset`), and import/export integration. Import path auto-materializes lorebooks from ST `character_book`; export path round-trips them. The allocator priority and the import/export contract are specified in this slice's `im-backend` delta rather than as separate deltas against the in-flight `companion-memory-and-preset` and `companion-character-card-depth` slices, both of which are already-committed-but-not-yet-archived; when those slices archive, the `im-backend` delta's allocator clause composes cleanly with the allocator requirement added there.

## Impact

- Affected Android code: new `feature/settings/worldinfo/WorldInfoLibraryRoute.kt` + `WorldInfoEditorRoute.kt`, new `core/model/Lorebook.kt` + `LorebookEntry.kt`, new `data/remote/im/ImWorldInfoClient.kt` with the CRUD + scan preview, and an additional tab on the existing character card-detail surface to preview the bound lorebook. The `Companion` section in Settings gains a **World Info** entry alongside the existing persona/preset/memory entries.
- Affected backend contract: new `POST/GET/PATCH/DELETE /api/lorebooks/*` endpoints, a new character-binding sub-resource, allocator integration for lorebook scan + injection, import-time auto-materialization, and export-time round-trip. The companion turn runtime consumes the scan output in the assembly step.
- Affected specs: new `world-info-binding`, plus deltas for `core/im-app`, `im-backend`, `companion-memory-and-preset`, and `companion-character-card-depth`.
- Affected UX: Settings gains a lorebook library; card detail gains a lorebook tab; chat gains no new surfaces (injection is deterministic and invisible), but a debug-build allocator log can show which entries matched and fired on a turn.
- Non-goals (scoped out of this slice):
  - Vector / embedding similarity matching — keyword exact-match only
  - Probability-gated entries (ST's `probability` field) — read and preserved in `extensions.st.*` but not acted on in the scan
  - Nested groups / group-only entries — groups are preserved as opaque field in `extensions.st.*` but do not gate the scan
  - Cross-user sharing / marketplace / public catalog
  - Re-running the scan mid-stream when later user turns change the keyword set (the scan runs once per assembly)
  - Per-entry scan position overrides (entry-at-depth, before-AN, after-AN) — a single fixed allocator slot only
  - Recursive lorebook scans (entries triggering other entries) — single-pass only
  - Lorebook versioning / revision history
