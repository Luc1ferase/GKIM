package com.gkim.im.android.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.gkim.im.android.core.assets.SkinVariant
import com.gkim.im.android.core.assets.skinAssetUrl

// R1.5 — companion-skin avatar surface.
//
// Loads the named skin variant from the CDN through the singleton Coil
// ImageLoader (registered by GkimApplication). Loading and error slots
// both fall back to AvatarFallbackSilhouette, which gives every avatar
// surface a stable visual identity regardless of network state.
//
// While R2 is still in flight, call sites pass `skinId = "default"` and
// `version = 1`. R2.5 will plumb the real `activeSkinId` through.

@Composable
fun SkinAvatar(
    characterId: String,
    skinId: String,
    version: Int,
    variant: SkinVariant,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = ChatAvatarShape,
    contentDescription: String? = null,
) {
    val url = remember(characterId, skinId, version, variant) {
        skinAssetUrl(
            characterId = characterId,
            skinId = skinId,
            version = version,
            variant = variant,
        )
    }
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .clip(shape),
        contentScale = ContentScale.Crop,
        loading = {
            AvatarFallbackSilhouette(
                size = size,
                shape = shape,
                contentDescription = contentDescription,
            )
        },
        error = {
            AvatarFallbackSilhouette(
                size = size,
                shape = shape,
                contentDescription = contentDescription,
            )
        },
    )
}

// R2.5 — URL-construction policies isolated as pure functions so contract
// tests can pin the activeSkinId path without spinning up Compose.

internal fun tavernCardAvatarUrl(characterId: String, activeSkinId: String): String =
    skinAssetUrl(
        characterId = characterId,
        skinId = activeSkinId,
        version = 1,
        variant = SkinVariant.Thumb,
    )

internal fun chatHeaderAvatarUrl(characterId: String, activeSkinId: String): String =
    skinAssetUrl(
        characterId = characterId,
        skinId = activeSkinId,
        version = 1,
        variant = SkinVariant.Avatar,
    )
