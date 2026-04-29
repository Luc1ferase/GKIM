## ADDED Requirements

### Requirement: Companion-skin assets are served from a single Cloudflare R2 bucket behind a custom CDN domain with versioned, immutable keys

The system SHALL store all companion-skin image assets in the public Cloudflare R2 bucket `gkim-assets` and serve them through the custom domain `cdn.lastxuans.sbs`. Asset keys MUST follow the contract `character-skins/{characterId}/{skinId}/v{n}/{variant}.png` where `variant` is one of `thumb` (96 × 96), `avatar` (256 × 256), `portrait` (512 × 768), or `banner` (1080 × 2400), encoded as PNG. Once written, a versioned key MUST NOT be mutated; an updated skin MUST be uploaded as `v{n+1}` and reflected by bumping `art_version` in `character_skins`. Every served object MUST carry the response headers `Cache-Control: public, max-age=31536000, immutable` and `Content-Type: image/png`.

#### Scenario: A skin asset URL resolves to the contract-shaped path

- **WHEN** the client constructs the URL for `(characterId="founder-architect", skinId="lantern-keeper", version=1, variant=PORTRAIT)`
- **THEN** the URL equals `https://cdn.lastxuans.sbs/character-skins/founder-architect/lantern-keeper/v1/portrait.png`

#### Scenario: A served skin object carries the immutable cache header

- **WHEN** any GET request is issued against a skin asset URL
- **THEN** the response includes `Cache-Control: public, max-age=31536000, immutable` and `Content-Type: image/png`

#### Scenario: Updates ship as a new version, never overwrite

- **WHEN** the operations team needs to replace the art for an existing skin
- **THEN** the new files are uploaded under `v{n+1}` and the old `v{n}` keys remain reachable; `art_version` in `character_skins` is bumped to the new number

### Requirement: Every companion character has at least one default skin and may have additional skins each carrying optional persona-modifier traits

The system SHALL model companion appearance as a `character_skins` row keyed on `(character_id, skin_id)` with rarity (`1`–`4` for common / rare / epic / legendary), an `art_version` integer, and an `is_default` flag. Each character MUST have exactly one row with `is_default = true`. A non-default skin MAY have one or more `skin_traits` rows whose `kind` belongs to the closed set `{PERSONA_MOD, GREETING, VOICE_TONE, RELATIONSHIP_BOOST}`; default skins MUST NOT carry traits.

#### Scenario: Every character has exactly one default skin

- **WHEN** the catalog is queried
- **THEN** for every distinct `character_id`, exactly one `character_skins` row has `is_default = true`

#### Scenario: Default skins do not carry traits

- **WHEN** a `character_skins` row has `is_default = true`
- **THEN** zero rows in `skin_traits` reference its `skin_id`

#### Scenario: Trait kinds are restricted to the closed set

- **WHEN** any `skin_traits` row is inserted
- **THEN** the `kind` column equals one of `PERSONA_MOD`, `GREETING`, `VOICE_TONE`, `RELATIONSHIP_BOOST`; any other value is rejected by the column check constraint

### Requirement: The gacha pool draws on `(character, skin)` tuples with a server-authoritative three-state response

The system SHALL model the gacha draw pool as a list of `(character_id, skin_id, weight, rarity)` rows. The endpoint `POST /api/v1/skins/draw` MUST return, for each result, a `state` field equal to `NEW_CHARACTER` (the user owned no skin of that character before this draw), `NEW_SKIN` (the user owned the character but not this skin), or `DUPLICATE_SKIN` (the user already owned this skin). State derivation MUST run server-side; the response body MUST be the sole authority for what the user has just drawn. When `state == DUPLICATE_SKIN`, `currencyDelta` MUST be a positive number of story shards equal to `rarity_value(rarity)` (1, 5, 25, 125 for rarity 1, 2, 3, 4 respectively); when `state ∈ {NEW_CHARACTER, NEW_SKIN}`, `currencyDelta` MUST be 0.

#### Scenario: Drawing a skin from a character the user already owns reports NEW_SKIN

- **WHEN** the user has at least one row in `user_skins` for `character_id = X` but no row for the drawn `skin_id`
- **THEN** the response result has `state = "NEW_SKIN"` and `currencyDelta = 0`

#### Scenario: Drawing a skin already owned reports DUPLICATE_SKIN with positive currency

- **WHEN** the user has a row in `user_skins` for the drawn `skin_id`
- **THEN** the response result has `state = "DUPLICATE_SKIN"` and `currencyDelta` equals `rarity_value(rarity)` for the drawn skin's rarity (1 / 5 / 25 / 125)

#### Scenario: Drawing first time on a character reports NEW_CHARACTER

- **WHEN** the user has zero rows in `user_skins` for `character_id = X`
- **THEN** the response result has `state = "NEW_CHARACTER"` and `currencyDelta = 0`

### Requirement: Pity counters force EPIC at 50 draws and LEGENDARY at 200 draws without exposing the re-roll to the client

The system SHALL maintain a `user_pity_counter` row per user with `draws_since_epic` and `draws_since_legendary` integer columns. Every non-EPIC-or-higher draw increments `draws_since_epic`; an EPIC or LEGENDARY hit resets it to 0. Every non-LEGENDARY draw increments `draws_since_legendary`; a LEGENDARY hit resets it. When `draws_since_epic` reaches 49 (the next draw is the 50th), the next draw MUST resolve to a skin of rarity ≥ 3; when `draws_since_legendary` reaches 199, the next draw MUST resolve to a skin of rarity = 4. The pity-forced re-roll MUST happen server-side and MUST NOT be visible in the client-facing response shape — a forced hit is indistinguishable from a natural hit by inspection of the response.

#### Scenario: 50th draw without an epic forces an EPIC+

- **WHEN** `draws_since_epic` is 49 and the next draw is issued
- **THEN** the resulting skin has rarity ≥ 3, and `draws_since_epic` resets to 0 in the response's `pityCounter`

#### Scenario: 200th draw without a legendary forces a LEGENDARY

- **WHEN** `draws_since_legendary` is 199 and the next draw is issued
- **THEN** the resulting skin has rarity = 4, and `draws_since_legendary` resets to 0 in the response's `pityCounter`

#### Scenario: A pity-forced hit is shape-indistinguishable from a natural hit

- **WHEN** comparing the response of a pity-forced EPIC draw against the response of a naturally-rolled EPIC draw with otherwise identical state
- **THEN** the two response bodies have identical fields and field types — there is no `wasForcedByPity` flag exposed to the client

### Requirement: Skin gallery cells render exactly three explicit states and never leak art for unowned skins

The system SHALL render every cell of the skin gallery on the character detail screen in exactly one of three states: `OWNED_ACTIVE` (thumb + 2 dp brass `primary` border + 1 dp ember `tertiary` inner ring + "active" caption), `OWNED_INACTIVE` (thumb + 1 dp `outlineVariant` border + name caption), or `LOCKED` (a generic silhouette at 40 % opacity + small lock icon + rarity-color-coded border + name in `onSurfaceVariant`). The cell MUST NOT render the skin's actual `thumb` artwork while in the `LOCKED` state — locked previews show only the silhouette plus the trait `description` text, never the underlying `payload_json` and never the thumb itself.

#### Scenario: An owned, currently-active skin renders with the brass border and ember inner ring

- **WHEN** a `(characterId, skinId)` pair has a `user_skins` row for the current user and matches `user_active_skin.skin_id` for that character
- **THEN** the gallery cell renders with the 2 dp brass border, 1 dp ember inner ring, the active caption, and the skin's `thumb`

#### Scenario: A locked skin never renders its actual art

- **WHEN** a `character_skins` row exists for `skinId` but no `user_skins` row exists for the current user
- **THEN** the gallery cell renders the generic silhouette (not the skin's `thumb`), and the locked-preview bottom sheet (if opened) shows the trait `description` text but does not render `portrait` or any other variant of the actual skin art

#### Scenario: An owned but inactive skin renders with the outline border

- **WHEN** a `user_skins` row exists for the user but the `user_active_skin.skin_id` for that character is a different skin
- **THEN** the gallery cell renders with a 1 dp `outlineVariant` border and the skin's `thumb`

### Requirement: Reveal animations are routed by the draw response state and use distinct surface scales

The system SHALL play exactly one of three reveal-animation tracks per draw result, routed by the response `state` field: `NEW_CHARACTER` plays a full-screen banner slide-in (480 ms in, 2 200 ms hold) with a brass `primary` halo pulsing two cycles; `NEW_SKIN` plays a mid-card portrait slide-up (360 ms in, 1 800 ms hold) with an ember `tertiary` 1 dp border pulsing two cycles plus a "新装束" / "New attire" caption; `DUPLICATE_SKIN` plays an inline thumb crossfade (240 ms) with a "+N 故事碎片 / story shards" currency-delta caption. The `NEW_CHARACTER` track MUST occupy the full screen, the `NEW_SKIN` track MUST occupy a 320 × 480 mid-card surface, and the `DUPLICATE_SKIN` track MUST be inline within the existing draw-result strip.

#### Scenario: NEW_CHARACTER plays the full-screen banner reveal

- **WHEN** a draw result has `state = "NEW_CHARACTER"`
- **THEN** the reveal surface fills the screen, loads the `BANNER` variant, plays a 480 ms slide-in followed by a 2 200 ms hold, and pulses a brass halo two cycles

#### Scenario: NEW_SKIN plays the mid-card portrait reveal

- **WHEN** a draw result has `state = "NEW_SKIN"`
- **THEN** the reveal surface is a 320 × 480 mid-card, loads the `PORTRAIT` variant, plays a 360 ms slide-up followed by a 1 800 ms hold, pulses an ember 1 dp border two cycles, and shows the "新装束" / "New attire" caption

#### Scenario: DUPLICATE_SKIN plays inline with currency caption

- **WHEN** a draw result has `state = "DUPLICATE_SKIN"`
- **THEN** the reveal surface is the inline draw-result strip, the thumb crossfades over 240 ms, and the caption reads "+{currencyDelta} 故事碎片 / story shards" using the value from the response

### Requirement: Probability detail surface defaults to rarity-aggregated and exposes a per-skin tree on demand

The system SHALL render the gacha probability-detail surface in two view modes. The default view MUST aggregate by rarity (matching the existing `computeProbabilityBreakdown` shape). On user toggle, the surface MUST reveal a two-level tree: each character row sums its skins' probabilities, and each character row expands to indented skin rows sorted by rarity descending then alphabetical. Per-skin probabilities for a single character MUST sum to that character's rarity-aggregated probability within ±1 percentage point (integer rounding tolerance).

#### Scenario: Default view is rarity-aggregated

- **WHEN** the probability-detail sheet is opened
- **THEN** the visible content matches the rarity-aggregated breakdown produced by `computeProbabilityBreakdown` and the per-skin tree is hidden behind an expand toggle

#### Scenario: Per-skin tree is sorted rarity-descending then alphabetical

- **WHEN** the user toggles the per-skin view
- **THEN** within each character section, skin rows are ordered by rarity descending; ties are broken alphabetically by `skin_id`

#### Scenario: Per-skin probabilities sum to character probability

- **WHEN** comparing the sum of per-skin probabilities under a character row against that character's rarity-aggregated probability
- **THEN** the two values are equal within ±1 percentage point
