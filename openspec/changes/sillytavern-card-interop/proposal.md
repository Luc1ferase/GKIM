## Why

The tavern pivot positions this app alongside the SillyTavern / character.ai ecosystem. Users in that ecosystem curate collections of character cards as either PNG images with tEXt-chunk metadata or standalone JSON files conforming to the ST character-card V2 / V3 schemas. Today the `companion-character-card-depth` capability models the deep persona record but offers no import or export path — new users must recreate every card by hand, and cards authored in our app cannot be shared outside it. Without interop, the branch fails the tavern promise at the library level: users cannot bring their existing companions in, and they cannot leave with what they authored.

This slice adds bi-directional interop with the SillyTavern character card V2 / V3 formats (both PNG and JSON), maps ST fields onto our existing deep persona record, and parks unknown / not-yet-modeled fields in the card's `extensions` bag so later slices (world info, persona, presets) can populate them without reimporting.

## What Changes

- **BREAKING** Character card records gain a validated PNG/JSON import path and a PNG/JSON export path; the `extensions` bag on `companion-character-card-depth` is formalized as the single landing zone for ST fields we do not yet model.
- Introduce a dedicated capability covering the ST V2 / V3 card schemas, the mapping between ST fields and our deep persona record, bilingual handling on monolingual ST sources, validation rules, and bounded safety limits (image size, field length, malformed-chunk rejection).
- Modify the Android app contract so the tavern surface exposes an "Import character card" entry point (file picker for PNG + JSON) and a card-detail surface exposes "Export as PNG" and "Export as JSON" actions, including a language picker for export.
- Modify the backend contract so card import and export round-trip through server-side endpoints that validate, parse, assemble, and persist the resulting deep persona record; the backend stays the source of truth for decoded content (no client-side mutation bypasses validation).
- Modify the `companion-character-card-depth` capability with explicit requirements for the `extensions` bag shape (known ST key list reserved, forward-compatible keys allowed) and avatar-source field semantics that account for imported avatar images.

## Capabilities

### New Capabilities
- `sillytavern-card-interop`: Defines the ST V2 / V3 card schema mapping, PNG tEXt chunk + JSON parsing, bilingual promotion rules for monolingual sources, export formatting, and bounded safety limits.

### Modified Capabilities
- `core/im-app`: The Android app requirements change so the tavern surface exposes import + export entry points, and a post-import preview surface lets the user confirm language mapping before persisting.
- `im-backend`: The backend requirements change so it accepts uploads, validates + decodes ST payloads, returns structured import previews, and emits signed PNG / JSON payloads on export.
- `companion-character-card-depth`: The existing extensions bag gets a documented reserved-key list for ST fields we do not yet model (e.g., `post_history_instructions`, `depth_prompt`, `group_only_greetings`), and the avatar-source field formalizes how imported avatars are stored.

## Impact

- Affected Android code: new `data/interop/SillyTavernCardCodec.kt` (or equivalent under `core/interop` package), new `data/remote/im/ImBackendClient.kt` methods for `importCard` / `exportCard`, new `feature/tavern/ImportCardRoute.kt` + `CardExportDialog`, minor extension to `CompanionCharacterCard` mapping helpers so the `extensions` bag preserves ST fields round-trip through the editor.
- Affected backend contract: new `POST /api/cards/import` + `GET /api/cards/{cardId}/export?format=png|json` endpoints, new parser for PNG tEXt chunks that surfaces `chara` (base64-encoded JSON) + optional `ccv3` keys, new field-mapper that produces our deep persona record, new exporter that emits V2 (baseline) or V3 (extended) payloads with language selection.
- Affected specs: new `sillytavern-card-interop`, plus deltas for `core/im-app`, `im-backend`, and `companion-character-card-depth`.
- Affected UX: users can import a character card from local storage (PNG or JSON), preview the decoded fields in a bilingual editor-like surface with a language picker, and save into their roster. Users can export cards they own from the card detail surface, choosing PNG or JSON and the target language. Round-trip preserves unknown fields via `extensions`.
- Non-goals (scoped out of this slice):
  - Lorebook / World Info import or export (→ `world-info-binding`)
  - SillyTavern preset JSON import or export (→ reserved; `companion-memory-and-preset` extensions bag parks the shape, but interop ships in a later slice)
  - ST chat log import or export
  - Character card sharing via short URL, QR, or in-app marketplace
  - Automatic NSFW content classification on imported cards (`companion-settings-and-safety-reframe` owns safety reframe)
  - Automatic persona extraction from imported cards into a `UserPersona` (`user-persona` extensions bag + an optional follow-up owns this)
  - Bulk import of a folder of cards in a single operation (single-file import in this slice)
  - Image-format conversion beyond PNG (WebP, GIF avatars not supported on import; unsupported formats are rejected with an explicit error)
  - Legacy V1 ST card format — this slice supports V2 and V3 only
