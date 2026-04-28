package com.gkim.im.android.feature.tavern

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gkim.im.android.core.assets.SkinVariant
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CharacterSkin
import com.gkim.im.android.core.model.SkinRarity
import com.gkim.im.android.core.ui.SkinAvatar

// R3.2 — companion-skin-gacha skin gallery.
//
// Three explicit states (sealed via enum) so contract tests can assert
// "every cell renders exactly one of these three". Locked cells MUST
// NOT render the skin's actual thumb art — only a generic silhouette
// at 40 % opacity per design.md. The brass-vs-outline border + ember
// inner ring is what differentiates active from inactive.

enum class GalleryCellState {
    OwnedActive,
    OwnedInactive,
    Locked,
}

// Render contract per state. The composable reads the spec via the
// `galleryCellSpec(state)` lookup so the unit test can pin every
// border / opacity / icon decision without spinning up Compose.
data class GalleryCellRenderSpec(
    val borderDp: Int,
    val borderColorToken: String,             // "primary" | "outlineVariant" | "rarity"
    val showInnerEmberRing: Boolean,
    val opacity: Float,                       // 1.0f for owned, 0.4f for locked
    val showLockIcon: Boolean,
    val showsActualThumb: Boolean,            // false ⇒ silhouette only
    val captionColorToken: String,            // "primary" | "onSurface" | "onSurfaceVariant"
)

internal val GalleryCellSpecs: Map<GalleryCellState, GalleryCellRenderSpec> = mapOf(
    GalleryCellState.OwnedActive to GalleryCellRenderSpec(
        borderDp = 2,
        borderColorToken = "primary",
        showInnerEmberRing = true,
        opacity = 1.0f,
        showLockIcon = false,
        showsActualThumb = true,
        captionColorToken = "primary",
    ),
    GalleryCellState.OwnedInactive to GalleryCellRenderSpec(
        borderDp = 1,
        borderColorToken = "outlineVariant",
        showInnerEmberRing = false,
        opacity = 1.0f,
        showLockIcon = false,
        showsActualThumb = true,
        captionColorToken = "onSurface",
    ),
    GalleryCellState.Locked to GalleryCellRenderSpec(
        borderDp = 1,
        borderColorToken = "rarity",
        showInnerEmberRing = false,
        opacity = 0.4f,
        showLockIcon = true,
        showsActualThumb = false,
        captionColorToken = "onSurfaceVariant",
    ),
)

internal fun galleryCellSpec(state: GalleryCellState): GalleryCellRenderSpec =
    GalleryCellSpecs.getValue(state)

// Mapping rarity → palette token name when the cell is in LOCKED
// state. The composable resolves the token at render time; tests pin
// the token-name mapping (uniqueness assertion in SkinGalleryStateTest).
internal fun rarityBorderToken(rarity: SkinRarity): String = when (rarity) {
    SkinRarity.Common    -> "outlineVariant"
    SkinRarity.Rare      -> "secondary"
    SkinRarity.Epic      -> "primary"
    SkinRarity.Legendary -> "tertiary"
}

@Composable
internal fun rarityBorderColor(rarity: SkinRarity): Color = when (rarityBorderToken(rarity)) {
    "outlineVariant" -> AetherColors.OutlineVariant
    "secondary"      -> AetherColors.Secondary
    "primary"        -> AetherColors.Primary
    "tertiary"       -> AetherColors.Tertiary
    else             -> AetherColors.OutlineVariant
}

@Composable
fun SkinGallery(
    characterId: String,
    skins: List<CharacterSkin>,
    activeSkinId: String,
    ownedSkinIds: Set<String>,
    appLanguage: AppLanguage,
    onActivate: (skinId: String) -> Unit,
    onPreviewLocked: (skinId: String) -> Unit,
    modifier: Modifier = Modifier,
    cellSize: Dp = 96.dp,
) {
    LazyRow(
        modifier = modifier.testTag("skin-gallery"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(skins, key = { it.skinId }) { skin ->
            val state = when {
                skin.skinId == activeSkinId -> GalleryCellState.OwnedActive
                ownedSkinIds.contains(skin.skinId) -> GalleryCellState.OwnedInactive
                else -> GalleryCellState.Locked
            }
            SkinGalleryCell(
                skin = skin,
                state = state,
                appLanguage = appLanguage,
                cellSize = cellSize,
                onClick = {
                    if (state == GalleryCellState.OwnedInactive) onActivate(skin.skinId)
                    if (state == GalleryCellState.Locked) onPreviewLocked(skin.skinId)
                },
            )
        }
    }
}

@Composable
private fun SkinGalleryCell(
    skin: CharacterSkin,
    state: GalleryCellState,
    appLanguage: AppLanguage,
    cellSize: Dp,
    onClick: () -> Unit,
) {
    val spec = galleryCellSpec(state)
    val borderColor = when (spec.borderColorToken) {
        "primary"        -> AetherColors.Primary
        "outlineVariant" -> AetherColors.OutlineVariant
        "rarity"         -> rarityBorderColor(skin.rarity)
        else             -> AetherColors.OutlineVariant
    }
    val captionColor = when (spec.captionColorToken) {
        "primary"          -> AetherColors.Primary
        "onSurface"        -> AetherColors.OnSurface
        "onSurfaceVariant" -> AetherColors.OnSurfaceVariant
        else               -> AetherColors.OnSurface
    }
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .width(cellSize)
            .clickable(onClick = onClick)
            .testTag("skin-gallery-cell-${skin.skinId}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(cellSize)
                .clip(shape)
                .border(BorderStroke(spec.borderDp.dp, borderColor), shape)
                .alpha(spec.opacity)
                .background(AetherColors.SurfaceContainerHigh, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (spec.showsActualThumb) {
                SkinAvatar(
                    characterId = skin.characterId,
                    skinId = skin.skinId,
                    version = skin.artVersion,
                    variant = SkinVariant.Thumb,
                    modifier = Modifier,
                    size = cellSize - 8.dp,
                    shape = shape,
                    contentDescription = skin.displayName.resolve(appLanguage),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = skin.displayName.resolve(appLanguage),
                    tint = AetherColors.OnSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
            if (spec.showInnerEmberRing) {
                Box(
                    modifier = Modifier
                        .size(cellSize - 4.dp)
                        .border(BorderStroke(1.dp, AetherColors.Tertiary), shape),
                )
            }
        }
        Text(
            text = skin.displayName.resolve(appLanguage),
            style = MaterialTheme.typography.labelLarge,
            color = captionColor,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
