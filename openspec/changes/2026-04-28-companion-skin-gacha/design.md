# Companion Skin Gacha — Reference

This document is the **design reference** for the slice. Anything contradicting this file in implementation is a finding.

## North-star vibe

A character you've drawn is a person you've met. A *skin* is a different evening with that person — they walked in tonight wearing the lantern-keeper's coat instead of the standard apron, and that changes the cadence of conversation. The product loop is: draw → get to know → keep drawing → see them in new lights → some of those lights unlock a different way of talking.

**NOT** a cosmetic-only loot system: every non-default skin SHOULD ship at least one persona-modifier trait so the user feels the change in conversation, not just in the avatar.

## Asset contract

### R2 bucket

- **Name**: `gkim-assets`
- **Custom domain**: `cdn.lastxuans.sbs` (Cloudflare DNS → R2)
- **Visibility**: public read; write via R2 API token held by deploy operator only
- **Default response headers** (set on bucket policy):
  - `Cache-Control: public, max-age=31536000, immutable`
  - `Content-Type: image/png`

### Key contract

```
character-skins/{characterId}/{skinId}/v{n}/{variant}.png
```

| Variant | Pixel size | Used by |
|---|---|---|
| `thumb`    | 96 × 96    | tavern card grid, conversation-row avatar, gallery cell, probability-tree row |
| `avatar`   | 256 × 256  | chat header, chat bubble incoming-side avatar |
| `portrait` | 512 × 768  | character detail hero, gallery preview sheet, skin reveal mid-card |
| `banner`   | 941 × 1672 | gacha `NEW_CHARACTER` reveal, splash carousel slide |

### Versioning rule

- Versioned keys are **immutable**. To update a skin's art, upload as `v{n+1}` and bump `art_version` in `character_skins`.
- Old versions stay reachable for clients still on stale catalogs; CDN never needs invalidation.
- The `v` prefix is intentional — keeps the path readable as `…/v1/portrait.png` rather than the bare number that risks collision with `1` as a filename.

### Naming rules for `characterId` and `skinId`

- Lowercase ASCII letters, digits, hyphens. No underscores, no dots, no Unicode.
- `characterId` matches the existing `CompanionCharacterCard.id` (already conforms).
- `skinId` is descriptive: `default`, `lantern-keeper`, `winter-cloak`, `star-chart`. Never numeric-only.
- The pair `(characterId, skinId)` is globally unique across the catalog.

## Data model (PG)

```sql
CREATE TABLE character_skins (
  skin_id         TEXT PRIMARY KEY,
  character_id    TEXT NOT NULL,
  display_name_en TEXT NOT NULL,
  display_name_zh TEXT NOT NULL,
  rarity          SMALLINT NOT NULL CHECK (rarity BETWEEN 1 AND 4),  -- 1 common, 2 rare, 3 epic, 4 legendary
  art_version     SMALLINT NOT NULL DEFAULT 1,
  released_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  is_default      BOOLEAN NOT NULL DEFAULT false,
  UNIQUE (character_id, skin_id)
);

CREATE TABLE skin_traits (
  trait_id     TEXT PRIMARY KEY,
  skin_id      TEXT NOT NULL REFERENCES character_skins(skin_id) ON DELETE CASCADE,
  kind         TEXT NOT NULL CHECK (kind IN ('PERSONA_MOD','GREETING','VOICE_TONE','RELATIONSHIP_BOOST')),
  payload_json JSONB NOT NULL
);

CREATE TABLE user_skins (
  user_id     UUID NOT NULL,
  skin_id     TEXT NOT NULL REFERENCES character_skins(skin_id),
  acquired_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  draw_count  INT NOT NULL DEFAULT 1,
  PRIMARY KEY (user_id, skin_id)
);

CREATE TABLE user_active_skin (
  user_id      UUID NOT NULL,
  character_id TEXT NOT NULL,
  skin_id      TEXT NOT NULL REFERENCES character_skins(skin_id),
  PRIMARY KEY (user_id, character_id)
);

CREATE TABLE user_pity_counter (
  user_id        UUID PRIMARY KEY,
  draws_since_epic       INT NOT NULL DEFAULT 0,
  draws_since_legendary  INT NOT NULL DEFAULT 0
);

CREATE TABLE user_story_shards (
  user_id  UUID PRIMARY KEY,
  balance  INT NOT NULL DEFAULT 0
);
```

### Migration rule (existing presets)

- For every existing `CompanionCharacterCard` in the seed roster, insert one `character_skins` row with `skin_id = '{characterId}-default'` (qualified to keep PK unique), `is_default = true`, `art_version = 1`, no `skin_traits`.
- For every user in `user_skins` legacy table (if any), promote ownership to the default skin.
- For every user, insert `user_active_skin` rows pointing each owned character to its default skin.

## Trait contract

```kotlin
sealed interface SkinTraitPayload {
    data class PersonaMod(val systemPromptAppendix: String) : SkinTraitPayload
    data class Greeting(val openerEn: String, val openerZh: String) : SkinTraitPayload
    data class VoiceTone(val toneTag: String) : SkinTraitPayload    // e.g. "warm", "wry", "ceremonial"
    data class RelationshipBoost(val multiplier: Float) : SkinTraitPayload  // 1.0..2.0 cap
}
```

Trait kinds activate at different points:

| Kind | Activates when |
|---|---|
| `PERSONA_MOD` | Chat thread opens with that skin active; appendix is appended to the system prompt for the duration of the active session |
| `GREETING` | First message in a thread when that skin is active and the thread has no prior messages this session |
| `VOICE_TONE` | Sent to the LLM as a tone hint; rendering is owned by the prompt-assembly layer |
| `RELATIONSHIP_BOOST` | Multiplier applied to relationship-progress accrual events while the skin is active |

Multiple traits on a single skin compose; conflicts (two `GREETING` on the same skin) are a config error caught at catalog-load time.

### Trait-text transparency

Locked-skin previews MUST show the trait's `description` field in plain language ("Greets you with a fortune-teller's opener"). They MUST NOT show the underlying `payload_json` (especially `PersonaMod.systemPromptAppendix`) — that's prompt-engineering surface, not user-facing.

## Gacha contract

### Pool config (server-side YAML, hot-reloadable)

```yaml
draw_pool:
  - { skin: founder-architect-default,        weight: 100, rarity: 1 }
  - { skin: founder-architect-lantern-keeper, weight:   5, rarity: 3 }
  - { skin: founder-architect-winter-cloak,   weight:   1, rarity: 4 }
  - { skin: scholar-default,                  weight: 100, rarity: 1 }
  - { skin: scholar-star-chart,               weight:   5, rarity: 3 }
  # ...
```

**Marginal-probability invariant**: the sum of `weight` over `(character, skin)` for a single character on day-one MUST equal the prior character-level weight. Day-one draw rates are unchanged; only the *fanout* into skins is new. Document the migration math inline in the migration commit message so future tuning preserves it.

### Draw response

```jsonc
{
  "results": [
    {
      "skinId": "founder-architect-lantern-keeper",
      "characterId": "founder-architect",
      "state": "NEW_SKIN",                  // NEW_CHARACTER | NEW_SKIN | DUPLICATE_SKIN
      "awardedTraits": [{
        "traitId": "lantern-greeting",
        "kind": "GREETING",
        "description": "Greets you with a lantern-keeper's nighttime opener"
      }],
      "currencyDelta": 0                    // > 0 only when state = DUPLICATE_SKIN
    }
  ],
  "pityCounter": { "drawsSinceEpic": 23, "drawsSinceLegendary": 87 }
}
```

State derivation runs server-side only; client trusts the response. No client-side roll: prevents tampering and keeps the pity counter authoritative.

### Pity contract

- Counter `drawsSinceEpic` increments on every non-EPIC+ draw, resets to 0 on any EPIC or LEGENDARY hit.
- Counter `drawsSinceLegendary` increments on every non-LEGENDARY draw, resets only on LEGENDARY hit.
- At `drawsSinceEpic == 49` (the 50th draw), the next draw is forced to an EPIC+ skin. At `drawsSinceLegendary == 199`, forced LEGENDARY.
- Forcing happens by re-rolling within the EPIC+ subset of the pool. The user does not see the re-roll; from the response shape, a pity hit is indistinguishable from a lucky natural hit.

### Currency contract

- Soft-currency name: **故事碎片** / **story shards**. No fiat purchase, no transfer.
- `DUPLICATE_SKIN` awards `currencyDelta = rarity_value(skinRarity)` shards (1 / 5 / 25 / 125 for common / rare / epic / legendary).
- Future slice (out of scope): a redemption surface that converts shards → a known target skin. This proposal does not include the redemption UI; only the accrual.

## UI contract

### Tavern card avatar

- Reads `activeSkinId` from `CompanionCharacterCard`, constructs `skinAssetUrl(characterId, activeSkinId, artVersion, THUMB)`, loads via `ImageLoader`.
- On 404 / network failure: render `AvatarFallbackSilhouette` (existing component from R3.2 of the visual-direction redesign). Never blank.
- The thumb is `96 × 96`; tavern card slot is up to `64 × 64` — Coil downsamples on device, not on origin.

### Skin gallery cell

- Cell: `120 × 120` rounded square, 8 dp inner padding, hand-painted style consistent with `tavernCardAvatarShape`.
- Three states (locked into a sealed enum so contract tests can enforce):

| State | Visual |
|---|---|
| `OWNED_INACTIVE` | thumb + 1 dp `outlineVariant` border + name caption underneath |
| `OWNED_ACTIVE`   | thumb + 2 dp brass `primary` border + 1 dp ember-red `tertiary` inner focus ring + name + "active" / "已应用" caption |
| `LOCKED`         | silhouette (not the skin's actual thumb — never leak art before draw) at 40 % opacity + small lock icon overlay + rarity-color-coded border + name shown but in `onSurfaceVariant` |

- Tap on `OWNED_INACTIVE` → confirm dialog → call `POST /users/me/skins/active`. Tap on `LOCKED` → bottom sheet with rarity, trait descriptions (transparent), and a "Try drawing" CTA that scrolls to the draw entry. Tap on `OWNED_ACTIVE` → no-op (or toast "Already active").

### Reveal-animation contract per state

| State | Surface scale | Motion | Accent |
|---|---|---|---|
| `NEW_CHARACTER` | full-screen | banner.png slide-in from below over 480 ms, hold 2 200 ms, dismiss on tap | brass `primary` halo radial pulse, 2 cycles |
| `NEW_SKIN`      | mid-card 320 × 480 | portrait.png slide-up from bottom 50 % to centre over 360 ms, hold 1 800 ms | ember `tertiary` 1 dp pulse, 2 cycles + "新装束" / "New attire" caption |
| `DUPLICATE_SKIN` | inline within draw result strip | crossfade thumb in 240 ms | re-uses existing R4.3 ember surface; replaces "Keep as bonus" CTA with currency delta caption |

### Probability detail tree

- Default view (rarity-aggregated) is unchanged from current `computeProbabilityBreakdown`.
- Expand toggle ("查看每张牌" / "Per-skin breakdown") opens a `LazyColumn` of two-level rows: a character header (name + sum-of-skins probability) followed by indented skin rows (name + thumb + own-skin probability). Sort: rarity descending, then alphabetical.
- Performance: tree is precomputed once when the breakdown sheet opens; no per-frame re-sort.

## Caching strategy on Android

- Singleton `ImageLoader` lives in `core/assets/AppImageLoader.kt`, exposed through `LocalImageLoader` Compose `CompositionLocal` (already idiomatic for Coil).
- Memory cache: `MemoryCache.Builder(context).maxSizePercent(0.20)` (≈ 100 MB on a 4 GB device).
- Disk cache: `DiskCache.Builder().directory(cacheDir.resolve("skins")).maxSizeBytes(256L * 1024 * 1024)`.
- HTTP layer: shared OkHttp client with `Cache-Control` honoured via `Cache(disk).directory + maxSize`. Coil already integrates with OkHttp; no extra plumbing.
- Pre-warm: when the user enters the tavern, enqueue `thumb` requests for every owned skin's active variant. When the user enters character detail, enqueue `portrait` for every owned skin of that character.
- Eviction: LRU on memory; LRU on disk. Versioned keys mean we never need explicit invalidation.

## Operational flow (designer → live)

```
ops/skins-staging/{characterId}/{skinId}/v{n}/   ← designer drops 4 png files here
  ├── thumb.png     96  ×  96
  ├── avatar.png   256  × 256
  ├── portrait.png 512  × 768
  └── banner.png   941  ×1672
              ↓
   tools/skins/upload.ps1  → validates + uploads to R2 → reports CDN URLs
              ↓
   PR: character_skins INSERT, skin_traits INSERT (if any), draw_pool YAML edit
              ↓
   sqlx migrate run on staging → smoke test → tarball with CRLF endings → deploy to prod
              ↓
   Backend hot-reloads pool YAML; client refreshes catalog at next 24 h boundary OR on user-pull
```

## Out of scope for this slice

- Animated skin portraits / Live2D / sprite sheets. Banners are static png in this slice.
- Per-skin chat-bubble re-skinning beyond a single `VOICE_TONE` tone tag. Chat layout does not branch on skin.
- Skin trading or gifting between users.
- Skin preview before ownership ("try on"). The probability tree is the transparency concession; we do not render unowned art at full size.
- Story-shard redemption surface. Currency accrues in this slice; spending it is a future slice.
- In-app purchase / fiat hook of any kind.
- Tablet / foldable layout density rework on the gallery.
- Localization beyond bilingual EN / ZH on `display_name_*` and trait `description`.
- Audio cues on reveal.
- Per-companion-card thematic re-skinning of the tavern home itself (the home stays one palette).
