## Why

The `tavern-visual-direction-redesign` slice landed the chrome (palette, typography, ambient layer, pill discipline). The companion roster behind it is still flat: every character has at most one piece of art, the gacha pool is character-level granular, and "drawing again" past the first pull only produces duplicate-as-bonus events. The product has nothing to *evolve* — once you've drawn the eight presets you've seen the whole gallery. There's no surface for long-term users to chase, no in-character reward for repeat conversation depth, and no operational lever for the design team to ship a new visual without a client release.

The plan is to introduce **skins** as the gacha unit. Each character has 1..N skins; each skin carries its own portrait set + optional persona-modifier traits; the gacha pool draws `(character, skin)` tuples; duplicate skins convert to a soft currency the user can later spend on a known target. Image assets live in **Cloudflare R2 behind a CDN domain** (`cdn.lastxuans.sbs`) — egress is free, immutable versioned keys make CDN cache invalidation a non-issue, and the design team can push a new skin without re-shipping the client.

This proposal is a six-slice delivery (R1 → R6) where each slice produces a shippable behavior delta on its own and the next builds on it. R1 establishes the storage substrate, R2 wires the skin data model end-to-end (chat header + tavern card start reading from CDN), R3 adds the in-app skin gallery, R4 migrates the gacha pool to skin granularity, R5 polishes the reveal animations, and R6 is decorative (splash carousel + trait progress).

## What Changes

### R1 — Storage foundation (R2 + CDN + ImageLoader + URL helper)

- Provision a public Cloudflare R2 bucket `gkim-assets` with custom domain `cdn.lastxuans.sbs`. Set the bucket's default cache headers (`public, max-age=31536000, immutable`) and lock writes behind an R2 API token held by the deploy operator.
- Lock the asset key contract: `character-skins/{characterId}/{skinId}/v{n}/{variant}.webp` where `variant ∈ {thumb, avatar, portrait, banner}` at fixed pixel sizes (96², 256², 512×768, 1080×2400). Versioned keys are never mutated; updates ship as `v{n+1}`.
- Add `tools/skins/upload.ps1` — a PowerShell driver that takes a local `{characterId}/{skinId}/v{n}/` directory, validates filenames and pixel dimensions, and uploads to R2 via the AWS CLI's S3-compat endpoint.
- Upload the eight existing preset characters' default art as `{id}/default/v1/{thumb,avatar,portrait,banner}.webp`. The "default" skin is the migration anchor for everything that exists today; it carries no traits.
- Add `core/assets/SkinAssetUrls.kt` exposing `skinAssetUrl(characterId, skinId, version, variant): String` as a pure function.
- Configure a singleton `ImageLoader` in `core/assets/AppImageLoader.kt` (Coil 2.7.0 is already on the classpath — this slice does not add a dependency, only the loader configuration). Memory cache 20 % of available, disk cache 256 MB at `cacheDir/skins/`.
- Wire the tavern card avatar to load `thumb` from CDN through the loader, with `AvatarFallbackSilhouette` as the failure fallback. Visible delta: presets show real art instead of silhouettes.

### R2 — Skin data model + active-skin display

- PG migration adds four tables: `character_skins`, `skin_traits`, `user_skins`, `user_active_skin`. Seed `character_skins` with the eight default rows from R1 (`is_default = true`, `art_version = 1`).
- Backend exposes `GET /api/v1/skins/catalog` returning `{skinId, characterId, name, rarity, artVersion, isDefault, traits[]}` for every skin currently in the catalog. 24-hour CDN cache.
- Backend exposes `GET /api/v1/users/me/skins` (owned skins + draw counts) and `POST /api/v1/users/me/skins/active` (set active skin per character; row-level upsert into `user_active_skin`).
- Kotlin model: `CharacterSkin` and `SkinTrait` data classes; `CompanionCharacterCard` gains `activeSkinId: String` defaulting to `"default"`. Repository combines catalog + owned + active into a single flow.
- Tavern card and chat header read `activeSkinId` from the card and construct asset URLs via `skinAssetUrl(...)`. No UI surface for switching yet — the backend's default-active-skin row is "default" for every user, so the visible delta is identical to R1; what shifts is the data path going through `activeSkinId` rather than a hardcoded "default".

### R3 — Skin gallery on character detail

- Seed one or two additional skins per preset character. Upload via R1's script, register rows in `character_skins` + `skin_traits` (where applicable). At least one preset gains an EPIC skin with one `PERSONA_MOD` trait so the gallery has something locked + something with traits to show.
- Add `feature/tavern/SkinGallery.kt` — a horizontal `LazyRow` of skin cells. Cell states: `OWNED_INACTIVE`, `OWNED_ACTIVE`, `LOCKED`. Active cell carries a brass border + 1 dp ember-red focus ring; locked cell renders a silhouette at 8 % opacity with a small lock icon.
- Wire the gallery to the character detail screen between the description block and the relationship-reset trigger. Tap an owned cell → confirm + call `POST /users/me/skins/active`. Tap a locked cell → bottom sheet showing the skin's portrait, rarity, and the traits it would unlock — fully transparent so the user knows what they're chasing before they spend pulls.
- Trait list rendering: each `SkinTrait` shows a one-line in-character description ("Greets you with a fortune-teller's opener", "Adds a faint candle-glow tint to messages") rather than a numeric stat. Persona-modifier traits become real on chat-thread enter only after activation.

### R4 — Skin-granular gacha pool

- Migrate the gacha pool config from character-level weights to `(characterId, skinId)` tuples. Each tuple has a weight + rarity. The migration preserves the marginal probability of drawing a given character (sum of its skin weights = its prior weight) so the existing balance is held constant on day one.
- Backend exposes `POST /api/v1/skins/draw` returning `{results: [{skinId, state, awardedTraits, currencyDelta}], pityCounter}` where `state ∈ {NEW_CHARACTER, NEW_SKIN, DUPLICATE_SKIN}`. State derivation:
  - `NEW_CHARACTER` if the user has no rows in `user_skins` for this `characterId`
  - `NEW_SKIN` if the user has rows for the character but not for this `skinId`
  - `DUPLICATE_SKIN` otherwise; in that case the response includes a positive `currencyDelta` (story shards) instead of the skin
- Pity counters: per-user counter that resets on `EPIC+`; force `EPIC` at draw 50, `LEGENDARY` at draw 200. Stored in `user_pity_counter` (single row per user).
- Tavern home draw entry-point swaps to `/skins/draw`. Three branch handling exists, but reveal animations stay on the existing R4.3 surfaces — animations get their proper differentiation in R5.

### R5 — Reveal animations + probability tree

- `NEW_CHARACTER` reveal: full-screen banner.webp slide-in from below + brass halo radial pulse. Reuses the existing R4.3 gacha-result surface as the substrate; only the animation track is new.
- `NEW_SKIN` reveal: mid-card portrait slide-up + ember-red 1 dp pulse twice + a small "新装束" / "New attire" caption. Distinct from `NEW_CHARACTER` by being mid-card not full-screen, signalling "evolution of someone you know" instead of "someone new walked in".
- `DUPLICATE_SKIN` reveal: keep the R4.3 ember-red bonus surface, replace "Keep as bonus" with currency-delta text ("+12 故事碎片 / story shards") and a one-line caption naming the skin that contributed.
- Probability detail UI on the draw screen: by default shows the rarity-aggregated breakdown the existing `computeProbabilityBreakdown` produces. An expand toggle reveals a two-level tree (`character → skin`) with per-row percentages, sorted by rarity descending. Lets long-term users target their pity push.

### R6 — Decorative slice (splash carousel + trait progress)

- Replace the static authenticated-startup landing with `feature/splash/SplashCarousel.kt` — a `Crossfade` cycling the `banner` variant of every skin the user owns, falling back to a fixed eight-slot default playlist for users with `< 8` owned skins. Each card holds 3 600 ms (600 ms fade-in / 3 000 ms hold / 600 ms fade-out cross). The carousel runs only during the first 5 seconds of cold start before the tavern home takes over; after that it does not re-fire.
- Character detail screen gains a thin trait progress strip: "3 / 7 skins · 4 / 12 traits unlocked" — a horizontal segmented bar, brass for unlocked, `surfaceContainerHigh` for locked, with a one-line tooltip on tap.

## Capabilities

### New Capability

- `companion-skin-gacha` — owns the asset CDN routing contract (R2 key naming + variant set + cache headers), the skin / trait / active-skin data model, the gacha pool's skin-tuple shape, the three-state draw response contract, the pity-counter rule, and the skin-gallery UX guardrails (state set, trait transparency, currency-from-duplicate rule).

### Modified Capabilities

- `companion-character-roster` — gacha pool moves from character-level granularity to `(character, skin)` tuples; the existing draw-result contract gains a `state` discriminator; "owned roster" semantics refine to "owned skins" since a single character can now be owned at multiple skin levels.
- `core/im-app` — `CompanionCharacterCard` gains an `activeSkinId` field; tavern card and chat header avatar URLs are constructed via the `companion-skin-gacha` capability's URL helper rather than from a packaged drawable; the `ImageLoader` singleton becomes a piece of the scaffold contract.

## Impact

- **Affected Android code (R1)**: new `core/assets/SkinAssetUrls.kt`, new `core/assets/AppImageLoader.kt`, `feature/tavern/TavernRoute.kt` (avatar slot wiring on preset cards). New `tools/skins/upload.ps1` operator script.
- **Affected Android code (R2)**: new `core/model/CharacterSkin.kt` + `core/model/SkinTrait.kt`, `core/model/CompanionCharacterCard.kt` adds `activeSkinId`, `data/repository/CompanionRosterRepository.kt` combines a catalog flow + owned-skins flow + active-skin flow, `feature/chat/ChatRoute.kt` header avatar reads through active skin.
- **Affected Android code (R3)**: new `feature/tavern/SkinGallery.kt`, `feature/tavern/CharacterDetailRoute.kt` mounts the gallery + activate-confirm flow + locked-preview sheet.
- **Affected Android code (R4)**: `feature/tavern/TavernRoute.kt` draw entry-point routes to the new endpoint, gacha-result reducer handles three states. Existing `GachaResultAccents` manifest extends to cover `NEW_SKIN`.
- **Affected Android code (R5)**: new `feature/tavern/animation/SkinRevealAnimations.kt` (three reveal tracks), `feature/tavern/ProbabilityBreakdownSheet.kt` adds the two-level expand state.
- **Affected Android code (R6)**: new `feature/splash/SplashCarousel.kt`, `feature/tavern/CharacterDetailRoute.kt` adds the trait-progress strip.
- **Affected backend code**: four new sqlx migrations under `backend/migrations/` (skins schema, then seed, then pity counter, then pool migration), four new endpoints (`/skins/catalog`, `/skins/draw`, `/users/me/skins`, `/users/me/skins/active`), one config-loader change to read pool entries as `(character, skin)` tuples.
- **Affected ops**: a new R2 bucket + CDN domain on Cloudflare, R2 API token added to the deploy secret store, `tools/skins/upload.ps1` documented in `docs/skins/operating.md`.
- **Affected specs**: new `companion-skin-gacha` capability; deltas to `companion-character-roster` and `core/im-app`.
- **Tests**: each task names a Kotlin presentation/contract test, an instrumentation test, or a backend integration test that locks the new contract. Migrations dry-run against a throwaway PG instance before tarballing for prod (per the existing CRLF migration gotcha — checksums are CRLF-sensitive on this deployment).
- **Cost**: ≈ $0 / month at expected scale. R2 storage at 50 char × 3 skin × 4 variant × 200 KB ≈ 120 MB → $0.002/mo storage; egress is free; reads at 1 M ops/mo ≈ $0.36.
- **Non-goals (scoped out)**:
  - Animated portraits or sprite sequences — banners are still single PNGs.
  - Per-skin chat-bubble re-skinning beyond a single tint trait. The chat layout itself does not branch on skin.
  - Voice / TTS variants per skin.
  - Trading skins between users.
  - Skin preview before draw (probability detail tree is the transparency concession; we do not let users "try on" an unowned skin).
  - Localization of skin names beyond bilingual EN / ZH (already on the `BilingualCopy` infra).
  - Tablet / foldable layout density rework on the gallery.
  - In-app purchase. Story-shard currency is earnable from duplicates only; no fiat hook in this slice.
