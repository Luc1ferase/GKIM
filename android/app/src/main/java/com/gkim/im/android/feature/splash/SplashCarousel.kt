package com.gkim.im.android.feature.splash

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import com.gkim.im.android.core.assets.SkinVariant
import com.gkim.im.android.core.assets.skinAssetUrl
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.model.CharacterSkin
import kotlinx.coroutines.delay

// R6.1 — companion-skin-gacha splash carousel.
//
// A Crossfade-based banner cycler that runs during the first 5 000 ms
// of cold start before yielding to the tavern home. Per design.md:
//   - per-card cycle: 600 ms fade-in + 3 000 ms hold = 3 600 ms advance
//     interval (the 600 ms fade-out overlaps with the next card's
//     fade-in via Crossfade — they're the same animation track)
//   - total runtime ceiling: 5 000 ms
//
// Playlist (always 8 slots):
//   - User owns ≥ 8 skins → first 8 owned banners
//   - User owns < 8 skins → fall back to the seeded 8-character default
//
// Timing constants are exposed as plain Long so the contract test can
// pin them without spinning up Compose.

object SplashCarouselTimings {
    const val FadeInMs: Long = 600
    const val HoldMs: Long = 3_000
    const val FadeOutMs: Long = 600

    /** Per-card advance interval. Fade-out overlaps the next card's
     *  fade-in via Crossfade, so it is intentionally NOT added here. */
    const val CycleMs: Long = FadeInMs + HoldMs

    /** Total runtime ceiling — splash yields to tavern home after this. */
    const val TotalRuntimeMs: Long = 5_000
}

const val SplashPlaylistSize: Int = 8

/** Seeded fallback playlist when the user owns fewer than 8 skins. */
internal val SeededDefaultPlaylistCharacterIds: List<String> = listOf(
    "tavern-keeper",
    "architect-oracle",
    "sunlit-almoner",
    "midnight-sutler",
    "opal-lantern",
    "glass-mariner",
    "wandering-bard",
    "retired-veteran",
)

/**
 * Builds the 8-slot banner playlist for the splash. Always returns
 * exactly [SplashPlaylistSize] URLs.
 */
internal fun buildSplashPlaylist(
    catalog: List<CharacterSkin>,
    ownedSkinIds: Set<String>,
): List<String> {
    val ownedSkins = catalog.filter { ownedSkinIds.contains(it.skinId) }
    if (ownedSkins.size >= SplashPlaylistSize) {
        return ownedSkins.take(SplashPlaylistSize).map { skin ->
            skinAssetUrl(skin.characterId, skin.skinId, skin.artVersion, SkinVariant.Banner)
        }
    }
    return SeededDefaultPlaylistCharacterIds.map { cid ->
        skinAssetUrl(
            characterId = cid,
            skinId = "$cid-default",
            version = 1,
            variant = SkinVariant.Banner,
        )
    }
}

@Composable
fun SplashCarousel(
    playlist: List<String>,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val safePlaylist = remember(playlist) {
        if (playlist.isEmpty()) listOf("") else playlist
    }

    LaunchedEffect(Unit) {
        val deadline = System.currentTimeMillis() + SplashCarouselTimings.TotalRuntimeMs
        while (System.currentTimeMillis() < deadline) {
            delay(SplashCarouselTimings.CycleMs)
            if (System.currentTimeMillis() >= deadline) break
            currentIndex = (currentIndex + 1) % safePlaylist.size
        }
        onComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AetherColors.Surface)
            .testTag("splash-carousel"),
    ) {
        Crossfade(
            targetState = currentIndex,
            animationSpec = tween(durationMillis = SplashCarouselTimings.FadeInMs.toInt()),
            label = "splash-carousel-crossfade",
        ) { idx ->
            AsyncImage(
                model = safePlaylist[idx],
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
