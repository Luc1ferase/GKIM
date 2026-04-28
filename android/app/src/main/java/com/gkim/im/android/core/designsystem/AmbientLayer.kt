package com.gkim.im.android.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.gkim.im.android.R

// R4.1 — ambient layer geometry constants. The grain overlay caps at 8%
// opacity; the candle glow caps at 5% opacity. Both are surface-level
// effects intended to read as "the surface has weight" rather than as a
// visible texture / gradient.
const val TavernGrainOpacityCeiling: Float = 0.08f
const val CandleGlowOpacityCeiling: Float = 0.05f

// R4.2 — ambient application manifest. The tavern home outer column applies
// both grain AND a TopEnd glow; the chat top-bar applies a TopStart glow
// only. No other surface in this slice opts in. The composables in
// feature/tavern/TavernRoute.kt and feature/chat/ChatRoute.kt apply these
// modifiers directly; this manifest exists so the unit-test contract can
// pin the application without spinning up Compose.
data class AmbientApplication(
    val surfaceTestTag: String,
    val grain: Boolean,
    val glowAnchor: AmbientGlowAnchor?,
)

enum class AmbientGlowAnchor { TopStart, TopEnd, TopCenter, CenterStart, CenterEnd, BottomStart, BottomCenter, BottomEnd, Center }

val TavernHomeAmbient: AmbientApplication = AmbientApplication(
    surfaceTestTag = "tavern-screen",
    grain = true,
    glowAnchor = AmbientGlowAnchor.TopEnd,
)

val ChatHeaderAmbient: AmbientApplication = AmbientApplication(
    surfaceTestTag = "chat-top-bar",
    grain = false,
    glowAnchor = AmbientGlowAnchor.TopStart,
)

val ChromeSurfacesWithAmbient: List<AmbientApplication> = listOf(TavernHomeAmbient, ChatHeaderAmbient)

/**
 * Paints the packaged `raw/tavern_grain.png` as an Overlay-blended layer
 * on top of the modified surface, capped at [TavernGrainOpacityCeiling].
 *
 * Pure draw: the underlying bitmap is loaded once via Compose's
 * `painterResource` cache and rendered through `drawWithCache`. No
 * recomposition is triggered by the layer itself.
 */
@Composable
fun Modifier.tavernGrain(): Modifier {
    val painter: Painter = painterResource(R.drawable.tavern_grain)
    return this.then(
        Modifier.drawWithCache {
            onDrawWithContent {
                drawContent()
                with(painter) {
                    draw(size = this@onDrawWithContent.size, alpha = TavernGrainOpacityCeiling)
                }
            }
        },
    )
}

/**
 * Paints a single radial highlight ("candle light") anchored at [anchor]
 * over the modified surface. The highlight uses
 * `primaryContainer.copy(alpha = CandleGlowOpacityCeiling)` at the centre
 * fading to transparent at the surface diagonal, reading as a soft warm
 * spot rather than a gradient.
 *
 * Pure draw: no state, no recomposition trigger; the brush is recomputed
 * from current size + theme color on every draw.
 */
fun Modifier.candleGlow(anchor: Alignment, primaryContainer: Color): Modifier =
    this.then(
        Modifier.drawBehind {
            val pos = anchor.align(
                size = androidx.compose.ui.unit.IntSize(0, 0),
                space = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
                layoutDirection = layoutDirection,
            )
            val center = Offset(pos.x.toFloat(), pos.y.toFloat())
            val radius = kotlin.math.sqrt(size.width * size.width + size.height * size.height)
            val brush = Brush.radialGradient(
                0f to primaryContainer.copy(alpha = CandleGlowOpacityCeiling),
                1f to Color.Transparent,
                center = center,
                radius = radius,
            )
            drawRect(brush = brush, topLeft = Offset.Zero, size = size)
        },
    )
