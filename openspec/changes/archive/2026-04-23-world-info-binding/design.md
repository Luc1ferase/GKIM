## Goal

Ship the minimum viable lorebook / world info runtime: a persisted model, keyword-triggered scan, character binding + global pool, deterministic injection into the existing token-budget allocator, and round-trip with SillyTavern's `character_book` shape. Keep it keyword-exact, deterministic, and observable from debug logs.

## Constraints

- **Additive to `companion-memory-and-preset`**: the allocator already defines a priority order and drop order. This slice inserts one new section — `worldInfoEntries` — with a documented priority + drop position. No existing section semantics change.
- **Additive to `sillytavern-card-interop`**: imported ST cards with a `character_book` field auto-populate a freshly-created lorebook on first commit. Subsequent export calls read back from the authoritative lorebook model and emit it into `character_book` (plus preserve everything under `extensions.st.*` for lossless fields not modeled here).
- **Service-boundary rule**: the scan algorithm is server-owned. The Android client never receives raw entry bodies outside of the editor surface; during chat turn assembly the server runs the scan, applies the budget, and injects the matched entries into the prompt before calling the provider.
- **Determinism over flexibility**: the scan is exact keyword matching (with optional case sensitivity per entry), a fixed scan window (per-entry `scanDepth`, default 3), and a fixed priority ordering by `insertionOrder`. No vector similarity, no probability gates in this slice.
- **Bilingual through `LocalizedText`**: entry `content` and entry `name` are bilingual; entry `keys` are per-language string lists so Chinese users can trigger on Chinese keywords and English users on English keywords, without one language polluting the other's scan.

## Key design decisions

### 1. The `Lorebook` and `LorebookEntry` shape

```
Lorebook {
  id: stable string
  ownerId: string — authenticated user
  displayName: LocalizedText
  description: LocalizedText
  isGlobal: bool — when true, participates in every companion turn for this user regardless of character bindings
  isBuiltIn: bool — seeded lorebooks cannot be deleted (may be absent; built-in seeding is future polish)
  tokenBudget: int — per-lorebook soft cap on total injected tokens for this lorebook per turn (default 1024)
  extensions: JsonObject — opaque bag, preserves `st.*` fields from import
  createdAt / updatedAt: ISO timestamps
}

LorebookEntry {
  id: stable string
  lorebookId: string
  name: LocalizedText — short human label, shown in editor
  keysByLang: { en: string[], zh: string[] } — per-language trigger keywords
  secondaryKeysByLang: { en: string[], zh: string[] } — optional AND-gated secondary keys
  secondaryGate: "AND" | "OR" | "NONE" — how secondary keys combine with primary; "NONE" means secondary keys are ignored
  content: LocalizedText — the injected prose body
  enabled: bool
  constant: bool — always inject regardless of keyword match
  caseSensitive: bool — case-sensitive keyword match
  scanDepth: int — how many of the most recent turns to scan (default 3, server-capped at 20)
  insertionOrder: int — sort order within the bundle, lower = injected first (higher priority)
  comment: string — author's note, never injected
  extensions: JsonObject — opaque bag for ST fields not modeled (probability, group, position, etc.)
}
```

Rationale:
- `keysByLang` lets us match against the active language's turn text naturally. An entry can ship keys for only one language; the other side's key list is empty and never triggers.
- `constant` entries are ST's "Blue Lights" escape hatch — they cover cases where a user wants a fact always available (e.g., "the world has two moons").
- `insertionOrder` is ST's convention (lower = higher priority) so importing an ST lorebook is a direct copy of the field.
- `tokenBudget` on the lorebook (not the entry) caps the total injected tokens per lorebook per turn so a single verbose lorebook can't swamp the prompt.
- `extensions` at both the lorebook and entry level carries un-modeled ST fields so round-trip is lossless — consistent with the `extensions` bag treatment on character cards.

Alternatives considered:
- **Single cross-language key list**: rejected — Chinese and English keyword detectors should be orthogonal; mixing them causes false triggers (e.g., a word that is English in one language but an unrelated fragment in the other).
- **Single global token budget across all lorebooks**: rejected — the allocator's overall budget already caps that globally; per-lorebook budget gives the author intent-level control.
- **Probability gates in this slice**: rejected as scope creep; the field is preserved under `extensions.st.probability` but not acted on.

### 2. Binding semantics

A lorebook can participate in a companion turn via three disjoint paths:
1. **Character-bound**: `LorebookBinding(lorebookId, characterId)`. Joining the active character pulls its bound lorebooks into the candidate set.
2. **User-global**: `Lorebook.isGlobal = true` on a user-owned lorebook. Joining every turn for that user, regardless of active character.
3. **Embedded-imported**: on ST card import with a `character_book`, the backend auto-creates a new lorebook (owned by the importing user) pre-bound to the newly-created character, seeding its entries from the `character_book.entries`.

Priority for de-duplication when both a character-binding and a global pool reference the same lorebook: the lorebook appears once.

Rationale:
- Character-bound is the default (ST convention, and the natural "this character's world").
- Global pool covers cross-setting facts (e.g., a user's playstyle preferences, recurring NPCs across multiple cards) without forcing the user to bind the same lorebook to every card.
- Embedded-imported is the zero-friction path for users coming from ST — their existing cards carry lorebooks, and a single Import step wires everything up.

### 3. Scan algorithm (server-owned)

Algorithm, run exactly once per companion turn in the assembly step:

1. Collect candidate lorebooks: the active character's bound lorebooks + the user's enabled global lorebooks. De-duplicate.
2. For each candidate lorebook, iterate entries where `enabled = true`:
   - If `constant = true`, add to the matched set unconditionally.
   - Else, build the scan text: concatenate the current user turn body + the last `scanDepth` turns' bodies (bounded by the server cap of 20).
   - Match `keysByLang[activeLanguage]` against the scan text. Each key is a literal substring match, case-sensitivity per `caseSensitive`.
   - If a primary key matches, apply the secondary gate:
     - `secondaryGate = "NONE"`: matched.
     - `secondaryGate = "AND"`: matched iff all `secondaryKeysByLang[activeLanguage]` also appear in the scan text.
     - `secondaryGate = "OR"`: matched iff any `secondaryKeysByLang[activeLanguage]` appears in the scan text.
3. Sort matched entries by `(insertionOrder asc, lorebookId, entryId)` as a total order.
4. Accumulate entries into the bundle, counting tokens. When the per-lorebook budget is exceeded for a lorebook, drop that lorebook's remaining entries in reverse order of its own `insertionOrder`. When the overall allocator bundle budget is exceeded, drop entries in reverse overall order until the bundle fits.
5. The resulting bundle is a single concatenated prose block, entries joined with a configurable separator (default `\n\n`), with each entry's body emitted as-is (macro substitution from `user-persona` — `{{user}}`, `{{char}}` — still applies).

Rationale:
- Single-pass, exact-match, deterministic total order = debuggable.
- `activeLanguage` selection means a Chinese session never triggers on English keys unless the author supplied them for both languages.
- `constant` entries bypass the scan entirely; they are effectively always-on lore cheats.
- Rejecting entries at the budget boundary (not truncating their content mid-sentence) keeps each injected entry whole and readable.

Alternatives considered:
- **Tokenize turn bodies and match token-wise**: rejected — substring match is simpler and matches ST convention; token-wise matching creates surprises at word-boundary edges.
- **Recursive scan (injected entries triggering other entries)**: rejected — scope; ST calls this feature controversial and users can disable it. Single-pass is simpler and adequate.
- **Per-entry position override (before-AN, after-AN, at-depth)**: rejected — scope; the allocator's single fixed slot is MVP.

### 4. Allocator integration

The allocator priority order from `companion-memory-and-preset` (modified by `user-persona`) is:

1. Preset `systemPrefix` + persona `systemPrompt`
2. Persona `personality` + `scenario`
3. Pinned facts
4. User persona `description` (from `user-persona`)
5. Rolling summary
6. Preset `systemSuffix`
7. Recent-N turns
8. Preset `formatInstructions`
9. Persona `exampleDialogue`
10. Preset `postHistoryInstructions`
11. Current user turn

This slice inserts a new section `worldInfoEntries` between items 4 and 5:

1. Preset `systemPrefix` + persona `systemPrompt`
2. Persona `personality` + `scenario`
3. Pinned facts
4. User persona `description`
5. **World Info entries bundle** ← new
6. Rolling summary
7. Preset `systemSuffix`
8. Recent-N turns
9. Preset `formatInstructions`
10. Persona `exampleDialogue`
11. Preset `postHistoryInstructions`
12. Current user turn

Drop order (lowest priority first):
- Persona `exampleDialogue`
- Older half of recent-N turns
- Rolling summary
- **World Info entries bundle: drop lowest-priority entries (highest `insertionOrder`) until the bundle fits or is empty** ← new
- Non-critical preset sections (`formatInstructions`, then `systemSuffix`)
- Never drop: pinned facts, persona `systemPrompt`, preset `systemPrefix`, user persona `description` (displayName fallback stays even if dropped), the user turn

Rationale:
- World Info sits above rolling summary because its content is author-written and curated, while summary is auto-generated and lossy. Losing a lore entry is worse than losing one turn of auto-summary.
- World Info sits below pinned facts because pins are the user's explicit "remember X" — strongest possible signal.
- Dropping entries within the bundle (by `insertionOrder`) before dropping the whole section preserves the author-intended priority.

### 5. Import / export with `sillytavern-card-interop`

On import (preview + commit in `sillytavern-card-interop`):
- If the imported card's JSON carries a `character_book` field (V2 spec: top-level; V3 spec: `data.character_book`), the preview response includes a summary of the embedded lorebook: entry count, total token estimate, whether any entries carry `constant = true`.
- On commit, the backend creates a new `Lorebook` owned by the importing user, seeds its entries from `character_book.entries` (mapping fields 1:1 where modeled, placing un-modeled fields in `entry.extensions.st.*`), and creates a `LorebookBinding` to the newly-created character.
- Both `Lorebook.description` and `Lorebook.displayName` are bilingual-promoted from the imported monolingual text using the same strategy as card fields (primary side = detected source language, other side = mirrored placeholder that the user can edit in the lorebook editor).

On export:
- The character-card export endpoint (from `sillytavern-card-interop`) reads the character's bound lorebooks. If exactly one lorebook is bound, it is emitted into `character_book` losslessly (modeled fields → ST fields; `lorebook.extensions.st.*` → ST extension fields; entries → `character_book.entries`).
- If multiple lorebooks are bound, the exporter picks the lorebook flagged as "primary binding" (configurable in the binding-manager UI); others are emitted as a summary warning in the export response so the user can decide whether to merge before export.
- Entries carry `keysByLang[primaryLanguage]` as `keys`, with the other-language keys dropped (ST's field is monolingual). The dropped keys are preserved under the entry's `extensions.stTranslationAlt.keys`. Same for secondary keys and content.

Rationale:
- The field shape chosen here is exactly what `sillytavern-card-interop` already tracks under `extensions.st.characterBook`; this slice promotes that opaque bag into a first-class model while preserving round-trip.
- Monolingual ST export is lossy by nature; preserving the other-language payload under `extensions.stTranslationAlt.*` keeps the round-trip working on re-import.

### 6. Settings UI: World Info library

Under Settings → `Companion` (from `companion-settings-and-safety-reframe`), add a **World Info** entry sibling to Persona Library, Preset Library, and Memory Shortcut. Tapping it opens `WorldInfoLibraryRoute`, which renders:

- A list of the user's lorebooks (name + entry count + `Global` badge if `isGlobal`).
- Primary CTA: Create new lorebook.
- Overflow per row: Duplicate, Delete (blocked if bound to any character — ask user to unbind first), Toggle global.
- Row tap: open the lorebook editor.

The **lorebook editor** (`WorldInfoEditorRoute`) renders:

- Editable header: `displayName`, `description`, `tokenBudget`, `isGlobal` toggle.
- Entry list: each row shows `name`, `insertionOrder`, `enabled` toggle. Overflow: Duplicate, Delete, Move up/down (adjusts `insertionOrder`).
- Entry detail (modal or deep route): full editor with `name` + `keysByLang` (two tabs for en/zh) + `secondaryKeysByLang` + `secondaryGate` dropdown + `content` bilingual + `constant` toggle + `caseSensitive` toggle + `scanDepth` number field + `insertionOrder` number field + `comment` textarea.
- Bindings sub-surface: list of characters this lorebook is bound to, with per-character "Unbind" affordance and a "Bind to character…" picker.

The character card-detail surface (from `companion-character-card-depth`) gains a **Lorebook** tab: a read-only preview of the bound lorebooks for this character, with a "Manage in library" link into the editor. Used for quick inspection, not inline editing.

Rationale:
- One central library + read-only preview on card detail is cleaner than two-way editing surfaces. Editing always happens in the library, where bindings are explicit.
- A per-entry detail modal avoids page-level layout thrash for frequent edits; the library surface stays focused on discovery.

### 7. Debug observability

Under `BuildConfig.DEBUG`, the allocator can log which entries were considered and which fired. A developer-only Settings entry (inside `Developer & Connection` from `companion-settings-and-safety-reframe`) toggles this log visible in the chat bubble overflow → "Prompt inspector" (existing or future debug surface; scope for this slice is emitting the data, not building the inspector).

- Backend emits a per-turn `worldInfoDebug` payload (only on debug builds authenticated with the debug override) listing each candidate lorebook + entries evaluated + which matched + which were injected + which were dropped for budget.

Rationale:
- Lorebook debugging is mostly "why did this entry not fire?" or "why did this entry fire?"; exposing the scan trace is the single highest-leverage debugging tool.
- Gating behind `BuildConfig.DEBUG` keeps the release payload lean.

## Risks

- **Scan cost grows with lorebook size × candidate count**: Mitigation — per-entry `scanDepth` cap (server-enforced max 20), lorebook token budget, and single-pass. Future slices can introduce caching or indexing.
- **Keyword spam triggering too many entries**: Mitigation — `insertionOrder`-based budget drop, per-lorebook token budget, and `enabled` toggle lets authors quickly disable loud entries.
- **Bilingual key drift (entry has English keys only but Chinese session)**: Mitigation — clearly documented: if `keysByLang[activeLanguage]` is empty, the entry cannot match (non-constant). Editor surfaces an inline hint when one language's keys are empty while the other's are populated.
- **Round-trip loss with ST monolingual export**: Mitigation — preserve the other-language payload under `entry.extensions.stTranslationAlt.*`, same as the card-level language preservation in `sillytavern-card-interop`.
- **Recursive lorebook scans (an injected entry's body mentions another entry's key)**: Out of scope — scan is single-pass. Users expecting recursion should build a composite lorebook or enable `constant` on both.
- **Accidental global lorebook pollution**: Mitigation — `isGlobal` is a per-lorebook toggle with an explicit confirmation dialog; editor surfaces the flag prominently.
- **Import-time embedded lorebook collision on repeated import**: Mitigation — import always creates a new lorebook (never merges). The old one remains bound to the old character if the user re-imports; users clean up via the library surface.
- **Entry content containing `{{user}}` / `{{char}}` macros**: Mitigation — the macro substitution layer from `user-persona` runs after the allocator assembles the bundle; lorebook content participates in macro substitution exactly like persona prose.

## Rollout

1. Backend: add `Lorebook`, `LorebookEntry`, and `LorebookBinding` tables; ship CRUD endpoints behind the same auth as preset/persona endpoints; plumb the scan algorithm into the turn-assembly step; extend the import commit path and the export path with lorebook materialization and round-trip.
2. Extend the allocator's priority + drop order with the `worldInfoEntries` section.
3. Android: new `core/model/Lorebook.kt` + `LorebookEntry.kt`; new `data/remote/im/ImWorldInfoClient.kt`; new repository; new `feature/settings/worldinfo/` surfaces; new `Lorebook` tab on the character card-detail screen.
4. Wire the Settings → `Companion` → World Info entry into the existing Settings route (from `companion-settings-and-safety-reframe`).
5. Verification: unit (scan algorithm under every `secondaryGate` value, budget drops at entry granularity, bilingual key isolation), integration (import ST card with `character_book` → lorebook materializes bound → companion turn injects matched entries; export round-trips back to `character_book`), instrumentation (library CRUD on emulator; a chat turn with a crafted keyword fires the matched entry).

## Trade-offs

- Keyword-exact vs. vector similarity: exact is MVP-deterministic; vectors are future.
- Single allocator slot vs. per-entry position override: single slot is simpler and covers the common case; ST's position-override feature is a power-user concern.
- Single-pass vs. recursive scan: single-pass is safer; recursion can surprise users with runaway token consumption.

## Edge cases

- **Empty lorebook bound to a character**: the scan finds zero matches and the bundle is empty; no impact on the allocator except a no-op section.
- **Lorebook bound but disabled globally via `enabled = false` on every entry**: bundle is empty; no debug log noise beyond "all entries disabled".
- **Character has no bound lorebook and no global lorebook is enabled**: the `worldInfoEntries` section is absent from the prompt entirely (not an empty divider).
- **User-turn body contains the entry's key but the last N turns do not**: matches because the scan text includes the current user turn; this is the typical case.
- **Entry's `scanDepth = 0`**: only the current user turn is scanned. Useful for "this fact should fire when the user just said it, not because it came up 3 turns ago".
- **Entry is `constant = true` AND has keys**: `constant` wins — the entry fires regardless of keyword match. Keys are preserved for future slices where the gate semantics might be OR'd together.
- **`secondaryGate = "AND"` with empty `secondaryKeysByLang`**: the entry matches on primary alone (no secondaries to enforce). Editor surfaces a warning that `secondaryGate = "AND"` has no effect.

## Open questions

- Should a global lorebook participate across every character card the user owns regardless of card genre (SFW / NSFW / cross-setting)? Default: yes. Users who want genre-scoped global can unbind and re-bind per card for now. A future "global pool" with filter tags is polish.
- Should imported `character_book` entries inherit the importing user's active language as their bilingual primary, or the card's detected source language? Default: the card's detected source language (consistent with `sillytavern-card-interop`'s card-level detection).
- Should the `worldInfoEntries` section log which entries fired to the Android chat UI (developer-visible only) or only to the backend debug sink? Default: backend debug sink only in this slice; the chat-side inspector is polish.
