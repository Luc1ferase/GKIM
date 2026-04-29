package com.gkim.im.android.feature.splash

import com.gkim.im.android.core.model.CharacterSkin
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.SkinRarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashCarouselTimingTest {

    @Test
    fun `per-card cycle equals fade-in plus hold`() {
        // R6.1 contract: Crossfade overlap means the fade-out of card N
        // happens DURING the fade-in of card N+1, so the per-card advance
        // interval is fadeIn + hold, NOT fadeIn + hold + fadeOut.
        assertEquals(
            SplashCarouselTimings.FadeInMs + SplashCarouselTimings.HoldMs,
            SplashCarouselTimings.CycleMs,
        )
    }

    @Test
    fun `total runtime ceiling matches design contract`() {
        // design.md / proposal.md: splash runs only during the first
        // 5 000 ms of cold start before yielding to the tavern home.
        assertEquals(5_000L, SplashCarouselTimings.TotalRuntimeMs)
    }

    @Test
    fun `timing values match design table`() {
        assertEquals(600L, SplashCarouselTimings.FadeInMs)
        assertEquals(3_000L, SplashCarouselTimings.HoldMs)
        assertEquals(600L, SplashCarouselTimings.FadeOutMs)
    }

    @Test
    fun `total runtime fits at least one cycle but not all eight`() {
        // Sanity: 5 000 ms / 3 600 ms ≈ 1.4 cards. Splash is a brief brand
        // moment, not a full 8-card sweep (which would take 28.8 s).
        val cyclesFitting = SplashCarouselTimings.TotalRuntimeMs / SplashCarouselTimings.CycleMs
        assertTrue(
            "Total runtime should fit at least 1 cycle but not 8",
            cyclesFitting in 1..7,
        )
    }

    @Test
    fun `playlist always has exactly 8 slots`() {
        // Whether owned ≥ 8 or < 8, the carousel always returns 8 entries.
        val emptyOwned = buildSplashPlaylist(catalog = emptyList(), ownedSkinIds = emptySet())
        assertEquals(SplashPlaylistSize, emptyOwned.size)

        val tenOwned = (1..10).map { skin("c$it", "c$it-default", isDefault = true) }
            .let { catalog ->
                buildSplashPlaylist(catalog, catalog.map { it.skinId }.toSet())
            }
        assertEquals(SplashPlaylistSize, tenOwned.size)
    }

    @Test
    fun `playlist falls back to seeded eight when fewer than eight owned`() {
        // User has only one owned skin → fall back to seeded 8-character
        // default playlist (tavern-keeper, architect-oracle, sunlit-almoner, ...)
        val catalog = listOf(skin("a", "a-default", isDefault = true))
        val owned = setOf("a-default")
        val playlist = buildSplashPlaylist(catalog, owned)
        assertEquals(SplashPlaylistSize, playlist.size)
        assertTrue(
            "Fallback playlist must include tavern-keeper banner",
            playlist.any { it.contains("/tavern-keeper/") && it.endsWith("/banner.png") },
        )
        assertTrue(
            "Fallback playlist must include retired-veteran banner",
            playlist.any { it.contains("/retired-veteran/") && it.endsWith("/banner.png") },
        )
    }

    @Test
    fun `playlist uses owned banners when at least eight owned`() {
        val catalog = (1..10).map {
            skin("char-$it", "char-$it-default", isDefault = true)
        }
        val owned = catalog.map { it.skinId }.toSet()
        val playlist = buildSplashPlaylist(catalog, owned)
        assertEquals(SplashPlaylistSize, playlist.size)
        // Owned-mode preserves catalog order for the first 8 entries.
        for (i in 0 until SplashPlaylistSize) {
            val expected = "https://cdn.lastxuans.sbs/character-skins/char-${i + 1}/char-${i + 1}-default/v1/banner.png"
            assertEquals("slot $i", expected, playlist[i])
        }
    }

    @Test
    fun `seeded fallback covers all eight archetype characters`() {
        assertEquals(SplashPlaylistSize, SeededDefaultPlaylistCharacterIds.size)
        val expected = setOf(
            "tavern-keeper",
            "architect-oracle",
            "sunlit-almoner",
            "midnight-sutler",
            "opal-lantern",
            "glass-mariner",
            "wandering-bard",
            "retired-veteran",
        )
        assertEquals(expected, SeededDefaultPlaylistCharacterIds.toSet())
    }

    @Test
    fun `every fallback url targets the BANNER variant on the CDN`() {
        val playlist = buildSplashPlaylist(catalog = emptyList(), ownedSkinIds = emptySet())
        for (url in playlist) {
            assertTrue("must use banner.png variant: $url", url.endsWith("/banner.png"))
            assertTrue("must hit CDN host: $url", url.startsWith("https://cdn.lastxuans.sbs/"))
            assertTrue("must use v1 path: $url", url.contains("/v1/"))
        }
    }

    private fun skin(characterId: String, skinId: String, isDefault: Boolean): CharacterSkin =
        CharacterSkin(
            skinId = skinId,
            characterId = characterId,
            displayName = LocalizedText("name", "名"),
            rarity = SkinRarity.Common,
            artVersion = 1,
            isDefault = isDefault,
            traits = emptyList(),
        )
}
