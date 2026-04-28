# Tasks — companion-skin-gacha

Each task is a single DELIVERY_WORKFLOW unit (verify → review ≥ 95 → commit + push → tick). Numbered by sub-slice (`R1.x` etc.).

## R1. Storage foundation — R2 + CDN + ImageLoader + URL helper

- [ ] R1.1 Provision the `gkim-assets` Cloudflare R2 bucket and bind the custom domain `cdn.lastxuans.sbs`. Set the bucket-level default response headers (`Cache-Control: public, max-age=31536000, immutable`, `Content-Type: image/webp`). Mint an R2 API token scoped to write on this bucket only and store it in the deploy operator's secret store under `R2_GKIM_ASSETS_TOKEN`. Verification: `curl -sI https://cdn.lastxuans.sbs/health.txt` returns 404 (bucket reachable, key not present); after uploading a 1-byte test object, `curl -sI` returns 200 with the immutable cache header set.

- [ ] R1.2 Add `tools/skins/upload.ps1` — a PowerShell driver that takes `-StagingDir <path>` and `-CharacterId / -SkinId / -Version` parameters, validates that the four expected variant files exist with correct pixel sizes (`thumb` 96², `avatar` 256², `portrait` 512×768, `banner` 1080×2400) and webp encoding, and uploads via the AWS CLI's S3-compat endpoint to `s3://gkim-assets/character-skins/{characterId}/{skinId}/v{n}/`. Verification: `pwsh tools/skins/upload.ps1 -StagingDir tools/skins/fixtures/sample/ -CharacterId test -SkinId fixture -Version 1 -DryRun` exits 0 and prints the four resolved CDN URLs without making network calls.

- [ ] R1.3 Upload the eight existing preset characters' default art as `{id}/default/v1/{thumb,avatar,portrait,banner}.webp` using R1.2's script. Verification: a smoke script `pwsh tools/skins/verify_default_uploads.ps1` issues `HEAD` requests against the 32 expected URLs (8 chars × 4 variants) and asserts each returns HTTP 200 with `Content-Type: image/webp` and the immutable cache header.

- [x] R1.4 Add `core/assets/SkinAssetUrls.kt` exposing `skinAssetUrl(characterId: String, skinId: String, version: Int, variant: SkinVariant): String` as a pure function plus a `SkinVariant` enum (`THUMB / AVATAR / PORTRAIT / BANNER`). Add `core/assets/AppImageLoader.kt` exposing a singleton `ImageLoader` with memory cache `MemoryCache.Builder(ctx).maxSizePercent(0.20)` and disk cache `DiskCache.Builder().directory(ctx.cacheDir.resolve("skins")).maxSizeBytes(256L * 1024 * 1024)`. Wire `LocalImageLoader` to provide the singleton at the app root. Verification: `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.core.assets.SkinAssetUrlsTest --tests com.gkim.im.android.core.assets.AppImageLoaderTest` covers URL formation against the locked contract and asserts the loader's configured cache sizes.

- [ ] R1.5 Wire the tavern card avatar slot in `feature/tavern/TavernRoute.kt` to load the default skin's `THUMB` variant via Coil with `AvatarFallbackSilhouette` as the failure fallback. The `CompanionCharacterCard` already carries the character id; for this slice we hardcode `skinId = "default"`, `version = 1` until R2 lands the real flow. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.TavernCardAvatarLoaderTest` asserts the URL passed to the loader follows the contract and that on `LoadState.Error` the fallback silhouette renders.

## R2. Data model + active-skin display

- [ ] R2.1 Add `backend/migrations/0XXX_skin_gacha_schema.sql` creating `character_skins`, `skin_traits`, `user_skins`, `user_active_skin`, `user_pity_counter`, `user_story_shards`. Seed `character_skins` with the eight default rows from R1.3 (one per preset, `is_default = true`, `art_version = 1`, no traits). For every existing `user_id` in the user table, insert `user_active_skin` rows pointing each owned character to its default skin. Verification: dry-run `sqlx migrate run` against a throwaway PG instance succeeds, and `psql -c "SELECT character_id, skin_id, is_default FROM character_skins ORDER BY character_id"` lists the eight expected rows.

- [ ] R2.2 Add backend endpoint `GET /api/v1/skins/catalog` returning `[{skinId, characterId, name: {en, zh}, rarity, artVersion, isDefault, traits: [{traitId, kind, description}]}]`. Set response cache headers `Cache-Control: public, max-age=86400`. Verification: `cargo test -p backend skin_catalog_endpoint` covers happy-path response shape + cache header.

- [ ] R2.3 Add backend endpoints `GET /api/v1/users/me/skins` (returns owned skins + draw counts) and `POST /api/v1/users/me/skins/active` (body `{characterId, skinId}`, asserts the user owns the requested skin, upserts into `user_active_skin`). Verification: `cargo test -p backend skin_active_endpoint` covers ownership-required negative case + happy-path upsert.

- [ ] R2.4 Add `core/model/CharacterSkin.kt` and `core/model/SkinTrait.kt` Kotlin data classes mirroring the catalog response. Add `activeSkinId: String` (default `"default"`) to `CompanionCharacterCard`. Extend `CompanionRosterRepository` to combine catalog + owned + active-skin flows into the existing `CompanionCharacterCard` projection. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.CompanionRosterRepositoryActiveSkinTest` covers the combined flow shape and that `activeSkinId` defaults to `"default"` when no row exists in `user_active_skin`.

- [ ] R2.5 Wire chat header avatar in `feature/chat/ChatRoute.kt` and tavern card avatar in `feature/tavern/TavernRoute.kt` to construct URLs from `card.activeSkinId` instead of the hardcoded `"default"` from R1.5. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatHeaderActiveSkinTest --tests com.gkim.im.android.feature.tavern.TavernCardActiveSkinTest` covers that the URL passed to the loader uses the card's `activeSkinId`.

## R3. Skin gallery on character detail

- [ ] R3.1 Author + upload one EPIC skin per preset character (eight skins total) plus one LEGENDARY skin for one chosen character (one skin total). Register all nine in `character_skins`; for skins with traits, populate `skin_traits` with `PERSONA_MOD` or `GREETING` payloads. Verification: `pwsh tools/skins/verify_default_uploads.ps1 -IncludeNonDefault` returns HTTP 200 for the new 36 URLs (9 skins × 4 variants), and the catalog endpoint lists 17 total skins (8 default + 9 new).

- [ ] R3.2 Add `feature/tavern/SkinGallery.kt` — a `LazyRow` of cells with sealed `GalleryCellState` enum (`OwnedActive / OwnedInactive / Locked`). Render contracts per state per `design.md`. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.SkinGalleryStateTest` covers each state's border / opacity / icon contract via a `GalleryCellRenderSpec` manifest.

- [ ] R3.3 Mount `SkinGallery` on the character detail screen between description and relationship-reset trigger. Wire tap-OwnedInactive → confirm dialog → `POST /users/me/skins/active`. Wire tap-Locked → bottom sheet (rarity + trait descriptions + "Try drawing" CTA scrolling to draw entry). Verification: `.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.tavern.SkinGalleryActivationInstrumentationTest` confirms an inactive skin can be activated end-to-end and the locked-preview sheet shows trait descriptions but does not render the locked thumb.

## R4. Skin-granular gacha pool

- [ ] R4.1 Add `backend/migrations/0XXX_skin_pool_migration.sql` migrating the gacha pool config table from character-level rows to `(character_id, skin_id)` rows. Preserve marginal probabilities (sum of skin weights per character == prior character weight); document the math in the migration commit message. Verification: `cargo test -p backend skin_pool_marginal_invariant` asserts that for every character, the post-migration sum equals the pre-migration weight to within ±1 (integer rounding).

- [ ] R4.2 Replace backend `POST /api/v1/draw` with `POST /api/v1/skins/draw` returning `{results: [{skinId, characterId, state, awardedTraits, currencyDelta}], pityCounter}`. State derivation per `design.md`. Pity counter (`drawsSinceEpic`, `drawsSinceLegendary`) increments and resets per the contract. `DUPLICATE_SKIN` awards `currencyDelta = rarity_value(rarity)` shards (1 / 5 / 25 / 125). Verification: `cargo test -p backend skin_draw_endpoint` covers the three state branches + currency accrual + pity reset on EPIC+ hit.

- [ ] R4.3 Wire the tavern home draw entry-point in `feature/tavern/TavernRoute.kt` to the new `/skins/draw` endpoint. Map the three response states onto the existing R4.3 gacha-result surface (animations stay R5). Extend the `GachaResultAccents` manifest from `tavern-visual-direction-redesign` to include a `NEW_SKIN` entry pinned to `tertiary`. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.GachaThreeStateRouterTest` covers that each backend `state` value renders the matching surface and that the accents manifest still satisfies R4.3's pill-discipline constraint.

## R5. Reveal animations + probability tree

- [ ] R5.1 Add `feature/tavern/animation/SkinRevealAnimations.kt` with three pure animation tracks: `NewCharacterReveal` (full-screen banner slide + brass halo pulse 2 cycles, 480 ms in / 2 200 ms hold), `NewSkinReveal` (mid-card portrait slide-up + ember-red 1 dp pulse 2 cycles, 360 ms in / 1 800 ms hold), `DuplicateSkinReveal` (inline thumb crossfade 240 ms). Each track exposes `AnimationSpec`-typed values readable by tests. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.animation.SkinRevealAnimationContractTest` asserts the track durations + cycle counts match the design contract.

- [ ] R5.2 Wire the three reveal animations into the gacha-result surface so that response state `NEW_CHARACTER` plays `NewCharacterReveal`, `NEW_SKIN` plays `NewSkinReveal`, `DUPLICATE_SKIN` plays `DuplicateSkinReveal`. The "新装束" / "New attire" caption is wired only to `NEW_SKIN`; the currency-delta caption ("+N 故事碎片 / story shards") is wired only to `DUPLICATE_SKIN`. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.SkinRevealRoutingTest` covers state-to-track routing and caption visibility.

- [ ] R5.3 Add `feature/tavern/ProbabilityBreakdownSheet.kt` two-level expand state. Default view stays the rarity-aggregated breakdown from `computeProbabilityBreakdown`. The expand toggle ("查看每张牌" / "Per-skin breakdown") reveals a `LazyColumn` of character-header + indented-skin-row pairs sorted by rarity descending then alphabetical. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.ProbabilityBreakdownTreeTest` covers the rarity-sort ordering + that own-skin probabilities sum (per character) to the rarity-aggregated probability for that character.

## R6. Splash carousel + trait progress (decorative)

- [ ] R6.1 Add `feature/splash/SplashCarousel.kt` — a `Crossfade` cycling the `BANNER` variant of every skin the user owns, falling back to a fixed eight-slot default playlist when `< 8` are owned. Each card holds 3 600 ms total (600 ms fade-in / 3 000 ms hold / 600 ms fade-out). The carousel runs only during the first 5 000 ms of cold start before yielding to the tavern home. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.splash.SplashCarouselTimingTest` covers the per-card timing + total runtime ceiling.

- [ ] R6.2 Add a trait progress strip to the character detail screen: "{ownedSkins} / {totalSkins} skins · {ownedTraits} / {totalTraits} traits unlocked" rendered as a horizontal segmented bar (brass for unlocked, `surfaceContainerHigh` for locked). Tap shows a one-line tooltip naming the next-most-likely-rarity skin to chase. Verification: `.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.TraitProgressStripTest` covers the segment-count math + that tooltip targets the lowest-rarity unowned skin first.

## Acceptance gates per slice

- R1 is shippable on its own: presets render real art via CDN; nothing else changes for the user, but the substrate is in place.
- R2 builds on R1: data path goes through `activeSkinId`, but every user is on `default` so the visible UI is identical to R1. The investment is internal — chat header + tavern card now read the dynamic URL.
- R3 builds on R2: users see a gallery, can switch between owned skins, can preview locked skins. Requires R3.1's seeded non-default skins to be visible.
- R4 builds on R3 (catalog must contain non-default skins for the new states to fire): gacha pool is skin-granular, three-state response, currency accrues on duplicates.
- R5 builds on R4: animations differentiate the three states + probability tree gives long-term users a target.
- R6 builds on R2 (banner variant uploads) and is otherwise self-contained: splash carousel + trait progress strip. Skip if pressed for time without blocking R1–R5.

Per `docs/DELIVERY_WORKFLOW.md`, every checkbox is ticked **only after** verification passes, review scores ≥ 95, and the change is committed and pushed.
