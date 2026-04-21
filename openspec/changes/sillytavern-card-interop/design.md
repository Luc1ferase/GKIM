## Context

SillyTavern and derivative tools (Risu, Agnaistic, CharacterTavern, JanitorAI exports) have converged on two portable card formats:

- **PNG with tEXt chunk**: a regular PNG whose metadata `tEXt` chunk named `chara` contains a base64-encoded JSON object matching the Tavern Card V2 or V3 spec. Optionally a `ccv3` key carries the V3 document alongside V2 for dual-version readers.
- **Plain JSON file**: the same V2 / V3 document as a standalone file. Usually `.json`; sometimes embedded inside archives.

The ST V2 field set and the expanded V3 field set map to our `companion-character-card-depth` record with a few gaps:

| ST V2 / V3 field | Our record (`CompanionCharacterCard`) |
|---|---|
| `name` | `displayName` (or existing `name` field, whichever is canonical) |
| `description` | `personality` (concatenated with our `personality` if both exist) |
| `personality` | `personality` |
| `scenario` | `scenario` |
| `first_mes` | `firstMes` |
| `alternate_greetings` | `alternateGreetings` |
| `mes_example` | `exampleDialogue` |
| `system_prompt` | `systemPrompt` |
| `post_history_instructions` | `extensions.stPostHistoryInstructions` (we surface in UI but do not merge into preset; user can paste into the active preset's `postHistoryInstructions` manually) |
| `creator` | `creator` |
| `creator_notes` | `creatorNotes` |
| `character_version` | `characterVersion` |
| `tags` | `tags` |
| `avatar` (base64 PNG) | persisted as a local file; card's `avatarUri` points to it |
| V3 `group_only_greetings` | `extensions.stGroupOnlyGreetings` |
| V3 `depth_prompt` | `extensions.stDepthPrompt` |
| V3 `nickname` | `extensions.stNickname` |
| V3 `source` | `extensions.stSource` |
| V3 `creation_date`, `modification_date` | `extensions.stCreationDate`, `extensions.stModificationDate` |
| V3 `extensions` (arbitrary) | merged into our `extensions` under `st.<key>` |
| V3 `assets` | `extensions.stAssets` (not decoded; reserved for future interop) |

ST sources are almost universally monolingual (English in the vast majority; occasional Japanese, Chinese, or Russian). Our record is bilingual via `LocalizedText`. The import flow needs a way to decide which side of `LocalizedText` the imported text lands on.

Constraints:
- Public repo boundary: backend source stays private; this slice defines the contract + validation rules.
- Service boundary (from `core/im-app`): the card decoder runs server-side. Clients upload raw PNG/JSON; the backend validates, decodes, and returns a structured preview. Export similarly runs server-side.
- Bilingual contract (from `localize-companion-tavern-copy`): imported prose must land as `LocalizedText`. Exported prose must pick one side (user chooses).
- Forward-compatible extensions bag (from `companion-character-card-depth`): unknown fields flow into `extensions` under a `st.*` namespace.
- Additive only: this slice does not change existing deep persona fields; mapping is additive.

## Goals / Non-Goals

**Goals:**
- Import a local PNG (with tEXt `chara` chunk) or a local JSON file into a deep persona record, bilingual-ized via a user-selected source language.
- Export a deep persona record as a PNG (embedding the active card's avatar if present) or as a standalone JSON, with the user selecting which language side to emit as the ST monolingual text.
- Preserve every unknown or not-yet-modeled ST field in the `extensions` bag under the `st.*` namespace so round-trip import/export does not lose data.
- Surface validation errors with enough specificity that the user can fix the source (e.g., "V3 schema expected but the payload is V2", "PNG tEXt chunk not found").
- Enforce bounded safety limits (max image size, max field length, max alt-greetings count) and reject malformed payloads with a typed error.
- Offer a post-import preview surface that lets the user correct the language mapping and edit fields before persisting.

**Non-Goals:**
- Lorebook / World Info import or export (→ `world-info-binding`).
- ST preset JSON import or export (reserved; `sillytavern-card-interop` does not cover presets in this slice).
- ST chat log import or export.
- Cross-user card sharing (short URLs, QR codes, in-app marketplace).
- Automatic NSFW classification (→ `companion-settings-and-safety-reframe`).
- Automatic persona extraction into a `UserPersona` from imported cards.
- Bulk folder import.
- Legacy V1 Tavern Card format.
- WebP / GIF avatar formats (PNG only for import + export avatar payload in this slice).

## Decisions

### 1. Server-owned decoder and encoder

The client uploads raw PNG bytes or raw JSON text to `POST /api/cards/import`. The backend:
1. Validates size (max 8 MiB for PNG, 1 MiB for JSON).
2. Parses the PNG tEXt chunks (or the JSON object) and locates the `chara` / `ccv3` payloads.
3. Validates the decoded JSON against the V2 or V3 schema.
4. Extracts and stores the avatar PNG bytes (if present) as a server-side asset, returning an `avatarUri` for the card.
5. Maps ST fields onto our deep persona record shape.
6. Returns the structured preview (deep persona record + any validation warnings + the language-mapping choice form data) without yet persisting.

The client then displays the preview; on user confirmation, `POST /api/cards/import/commit` persists the record as a user-authored card.

Export is similarly server-owned: `GET /api/cards/{cardId}/export?format=png|json&language=en|zh` returns the encoded payload.

Rationale:
- Server-side decoding prevents a malicious PNG from exploiting client-side image parsers.
- Validation in one place; the Android codec helper does format-sniffing only (PNG signature, JSON object detection) and surfaces file sizes.
- Export produces a single canonical payload regardless of client version.

Alternatives considered:
- **Client-side decoder**: rejected — shipping a PNG tEXt parser + JSON schema validator on the client duplicates logic and widens the attack surface.
- **Peer-to-peer share via URL**: rejected — sharing is a separate concern with moderation / abuse-reporting requirements.

### 2. Two-step import: preview, then commit

Every import is a two-step flow: upload → preview → user confirms → commit. The preview returns the decoded record including:
- The mapped deep persona record in our shape (bilingual slots with the imported text on the chosen side and the other side mirroring as a placeholder).
- The detected source language (heuristic; user-overridable).
- A list of warnings (fields that exceeded length, fields that were truncated, unknown ST fields parked in `extensions`).
- A list of `st.*` extension keys with their raw values so advanced users can verify nothing was silently dropped.

Rationale:
- A one-step "import and persist" flow surprises users when ST cards have typos or NSFW content they didn't realize; two-step lets them preview.
- Warnings surface both gentle issues (field truncated) and policy issues (oversized avatar discarded) without blocking the import.

### 3. Language mapping on monolingual sources

When a monolingual ST card is imported:
- Detect primary language via a simple heuristic (character-range analysis on `first_mes` + `description`).
- Default the detected language as the "primary" side of `LocalizedText` (e.g., English text on the `en` side).
- Mirror the same text onto the other `LocalizedText` side as a placeholder; mark these placeholders with an `extensions.stTranslationPending: <field>` flag.
- User can override the detected language in the preview before committing.

Rationale:
- The bilingual contract does not accept empty `LocalizedText` sides anywhere; mirroring prevents validation failures while flagging the other side for later human translation.
- Heuristic detection is best-effort; user override is the escape hatch.

Alternatives considered:
- **Refuse to import until both sides are filled**: rejected — friction, and most users will translate later if at all.
- **Auto-translate via the provider**: rejected — provider translation is expensive, not always consented, and not reliable enough for persona prose.

### 4. Avatar handling

On import:
- If the PNG tEXt chunk carries a `chara` payload, the PNG itself IS the card avatar. Strip the tEXt chunks, re-encode as a clean PNG, and store as an asset on the backend; return `avatarUri`.
- If the JSON embeds a `chara` base64 field with a PNG, decode and store the same way.
- If there is no avatar (V3 sometimes excludes one), leave `avatarUri` null.
- Reject avatars that decode to images larger than 4096×4096 or files larger than 8 MiB.

On export:
- PNG format: fetch the card's `avatarUri` from backend storage (or synthesize a placeholder avatar with the card's initials), embed the deep persona record as base64 JSON in a `chara` tEXt chunk, and return.
- JSON format: emit the deep persona record; do not embed the avatar image (users who want images should pick PNG).

Rationale:
- Re-encoding the PNG on import prevents tEXt chunks from carrying hidden payloads through our storage.
- JSON exports stay small; PNG exports carry the image for visual recognition.

### 5. V2 vs V3 handling

Both versions are accepted on import. On export:
- JSON format emits V3 by default (superset of V2); users with V2-only downstream tools can toggle the format to `v2_json` explicitly via the export dialog.
- PNG format embeds both `chara` (V2-compatible subset) and `ccv3` (full V3) tEXt chunks, so the output is readable by both generations of tools.

Rationale:
- V3 is strictly richer; emitting it by default keeps our data fidelity.
- Dual-chunk PNG export is the ST community's convention for cross-compatibility.

### 6. Reserved `st.*` extensions namespace

All imported ST fields that do not map onto our known deep persona fields land under `extensions.st.*`:
- `extensions.stPostHistoryInstructions`
- `extensions.stGroupOnlyGreetings`
- `extensions.stDepthPrompt`
- `extensions.stNickname`
- `extensions.stSource`
- `extensions.stCreationDate`
- `extensions.stModificationDate`
- `extensions.stAssets`
- `extensions.st.<otherKey>` for anything else under the ST V3 `extensions` sub-object

Rationale:
- One namespace, one rule: anything from ST lives under `st.*`. Makes it easy to strip when exporting to non-ST formats, and easy to preserve on re-export.
- Future slices (world info, presets) can migrate their bits out of `extensions.st.*` when they model the fields natively; lossless round-trip is preserved in the meantime.

### 7. Bounded safety limits

The backend rejects imports that exceed the following limits with typed errors:
- File size: PNG > 8 MiB, JSON > 1 MiB → `payload_too_large`.
- Image dimensions: avatar > 4096×4096 → `avatar_too_large`.
- Any prose field > 32 KiB → truncated with a warning; a hard fail triggers only if total serialized payload > 2 MiB after field truncation.
- Alt-greetings: > 64 entries → trim to the first 64 with a warning.
- Tags: > 256 entries → trim with a warning.
- Unknown `extensions.st.*` values that exceed 64 KiB each → dropped with a warning; the rest is preserved.

Rationale:
- Matches the permissive-by-default ST community style (warnings over rejections) while protecting storage and render performance.
- Every limit has a typed error or warning code so the preview can surface it to the user.

### 8. Preset-field handling deferred

ST V2's `post_history_instructions` is a per-card field that nominally ends up before the user turn in prompt assembly. Our preset system (`companion-memory-and-preset`) has a global `postHistoryInstructions` section. Per-card override of that slot is owned by `tavern-experience-polish`; this slice parks the imported value in `extensions.stPostHistoryInstructions` and surfaces a warning in the preview: "This card carries a post-history instruction. Paste it into your active preset, or wait for per-card preset support in a later update."

Rationale:
- Keeps this slice focused on round-trip fidelity.
- Clear user-facing copy surfaces the limitation without silently dropping data.

### 9. Export language selection

When exporting, the user picks one `LocalizedText` side (`en` or `zh`). The exported payload emits the chosen side's text as the ST monolingual value. The other side is preserved in `extensions.stTranslationAlt.<field>` if the user opts in ("include the other language as an extension"). Default: don't include the other side in exports — keeps the output clean for ST tools that don't understand our extensions.

Rationale:
- ST readers expect monolingual text; forcing them to parse our bilingual shape breaks downstream compatibility.
- Opt-in other-language preservation lets users who round-trip the card back into our app recover both sides.

## Risks / Trade-offs

- **Malicious PNG payloads** → Mitigation: re-encode the image on import (strip unknown chunks); validate tEXt chunk length; reject compressed chunks; enforce max dimensions + file size.
- **Prompt-injection via imported text** → Mitigation: imported prose is sanitized only by length limits, not by content. Users inherit the prompt-behavior of the cards they import (they chose to). The preview surface shows the full prose so the user can read before committing.
- **Bilingual-contract placeholder drift** → Mitigation: `extensions.stTranslationPending` flag lets future UI surface "translate this field"; no auto-translation happens silently.
- **Heuristic language detection mistakes** → Mitigation: user override in the preview; default choice is visible and editable.
- **V2 export losing V3 fields** → Mitigation: emit V3 by default; V2-only mode is explicit. Preview shows which fields won't survive a V2 export.
- **Avatar storage growth** → Mitigation: per-account storage cap (enforced by an existing roster quota, not defined in this slice); oversized avatars are rejected at import.
- **ST field semantics drift** → Mitigation: the mapping table in this design doc is the canonical reference; changes require a design-doc update and a spec delta.
- **JSON parsing of untrusted payloads** → Mitigation: strict schema validation with typed errors; unknown top-level keys flow into `extensions.st.*`; known-but-wrong-typed values are rejected with a specific error.

## Migration Plan

1. Add server-side PNG tEXt parser + V2/V3 JSON schema validator. Reserve the `st.*` extensions namespace documentation in `companion-character-card-depth`.
2. Add `POST /api/cards/import` (two-step: preview + commit), `GET /api/cards/{cardId}/export?format=png|json&language=en|zh`.
3. Add DTOs + client-side format-sniffing helper `SillyTavernCardCodec`:
   - Sniff PNG vs JSON from file magic bytes.
   - Enforce client-side size check before upload.
4. Android UI:
   - Tavern surface overflow: "Import character card" entry point → file picker → upload → preview route.
   - `ImportCardPreviewRoute`: bilingual editor pre-populated from server preview + language picker + commit action. Saving issues the commit request.
   - Card detail surface: "Export as PNG" and "Export as JSON" actions with format + language picker.
5. Update `extensions` conventions in `companion-character-card-depth` delta spec: reserved key list under `st.*`, round-trip guarantees.
6. Verification:
   - Unit: PNG signature detection, JSON sniffing, size guards, helper edge cases.
   - Integration: round-trip a curated fixture set (V2 PNG, V3 PNG, V2 JSON, V3 JSON) through import → commit → export → re-import and confirm field fidelity.
   - Instrumentation: import preview render, language picker, commit + roster refresh, export dialog.
7. Record delivery evidence in `docs/DELIVERY_WORKFLOW.md`.

Rollback strategy:
- If the backend decoder is flawed post-launch, the import endpoint can return a typed `decoder_unavailable` error without affecting existing roster data.
- The Android entry points can be gated behind a feature flag in the repo.
- The `extensions.st.*` namespace is reserved-only in the card record; removing the interop endpoints does not require a data migration.

## Deferred scope (captured now, implemented later)

- **ST preset JSON interop**: maps ST preset shape onto our `Preset.extensions` bag. Owned by a dedicated preset-interop slice (or folded into `companion-memory-and-preset` follow-up).
- **Lorebook / World Info interop**: owned by `world-info-binding`.
- **ST chat log import/export**: move a conversation transcript across apps. Potential future slice.
- **Card sharing via short URL or QR**: requires share-token backend + moderation. Future.
- **Bulk folder import**: UI for selecting multiple files and running each through preview. Polish.
- **NSFW classifier on imported cards**: owned by `companion-settings-and-safety-reframe`; this slice ships the plumbing, that slice adds policy.
- **Auto persona extraction from cards**: future; extends `user-persona` with an optional "create persona from imported card" action.
- **WebP / GIF avatar support**: future, if adoption requires.

## Open Questions

- Should the tavern "Import card" entry point live in the roster's overflow menu or on the empty-state surface? Default: both. The overflow is always reachable; the empty state surfaces it as a primary CTA when the roster is sparse.
- Should export include the embedded AIGC-generated background / voice assets? Default: no in this slice — the ST `assets` field is preserved under `extensions.stAssets` but not decoded.
- Should imported cards be automatically marked as `user-authored` (editable) or as `imported` (a new source category)? Default: `user-authored` — keeps the editor path unchanged. A future slice can add an `imported` source if provenance matters.
- On export, should the default language be the user's active `AppLanguage` or the card's primary source language? Default: active `AppLanguage`; user can override in the dialog.
- Should re-importing a card that has the same `id` overwrite or create a duplicate? Default: always create a duplicate on import (new id assigned); merging is out of scope here.
- Should the preview surface show a diff of what will change if the user committed? Default: no — the preview IS the state that will be saved. Pre-import diffing is cognitively heavy for a single-direction flow.
