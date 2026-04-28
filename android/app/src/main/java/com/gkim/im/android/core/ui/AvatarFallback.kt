package com.gkim.im.android.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gkim.im.android.core.designsystem.AetherColors

/**
 * R3.2 — silhouette avatar fallback.
 *
 * Renders a generic upper-body silhouette (`Icons.Outlined.Person`) on a
 * `surfaceContainerHighest` field, framed by a 1 dp `primary`-tinted
 * stroke, in the supplied [shape]. Preserves the surrounding shape per
 * call site:
 *  - tavern cards   -> [TavernCardAvatarShape] (rounded-square)
 *  - chat avatars   -> [ChatAvatarShape]       (circle)
 *
 * MUST NOT render a letter / initial monogram.
 */
@Composable
fun AvatarFallbackSilhouette(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = ChatAvatarShape,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(AetherColors.SurfaceContainerHighest, shape)
            .border(BorderStroke(1.dp, AetherColors.Primary), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = contentDescription,
            tint = AetherColors.Primary,
        )
    }
}

/** Rounded-square shape for tavern card portrait fallbacks. */
val TavernCardAvatarShape: Shape = RoundedCornerShape(12.dp)

/** Circle shape for chat header / chat bubble avatar fallbacks. */
val ChatAvatarShape: Shape = CircleShape
