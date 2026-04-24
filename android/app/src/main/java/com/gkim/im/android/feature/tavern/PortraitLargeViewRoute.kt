package com.gkim.im.android.feature.tavern

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.PageHeader
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.Conversation
import com.gkim.im.android.core.model.resolve
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.feature.shared.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Route identity + builder for the portrait large-view surface.
 *
 * The pattern mirrors the existing nav graph — a static prefix plus a single `{characterId}`
 * argument — so three tap surfaces (tavern card rows, chat header, chat bubbles) can share
 * the same route builder and a single test covers the routing contract.
 */
internal object PortraitTapRouter {
    const val ROUTE_PATTERN: String = "tavern/portrait/{characterId}"
    fun route(cardId: String): String = "tavern/portrait/$cardId"
}

/**
 * Per-surface tap-route resolvers for the three avatar tap sources (§1.2).
 *
 * Each resolver takes only what that surface has locally in scope (a card id, a conversation)
 * and returns either a concrete portrait route or null when navigation should be suppressed.
 * PortraitTapRoutingTest exercises each resolver directly so the three surfaces share a single
 * contract without any composable being stood up.
 */
internal fun portraitTapRouteForTavernCard(cardId: String): String = PortraitTapRouter.route(cardId)

internal fun portraitTapRouteForChatHeader(conversation: Conversation?): String? =
    conversation?.companionCardId?.let { PortraitTapRouter.route(it) }

internal fun portraitTapRouteForChatBubble(conversation: Conversation?): String? =
    conversation?.companionCardId?.let { PortraitTapRouter.route(it) }

internal data class PortraitLargeViewUiState(
    val card: CompanionCharacterCard? = null,
)

/**
 * Presentation derived from [PortraitLargeViewUiState] plus the active [AppLanguage]. Split
 * into a sealed interface so tests can drive each branch without standing up Compose and
 * without depending on the render itself.
 */
internal sealed interface PortraitPresentation {
    data object Missing : PortraitPresentation
    data class Fallback(val displayName: String, val cardId: String) : PortraitPresentation
    data class Avatar(
        val avatarUri: String,
        val displayName: String,
        val cardId: String,
    ) : PortraitPresentation
}

internal fun portraitPresentation(
    card: CompanionCharacterCard?,
    language: AppLanguage,
): PortraitPresentation {
    if (card == null) return PortraitPresentation.Missing
    val resolved = card.resolve(language)
    val uri = card.avatarUri
    return if (uri.isNullOrBlank()) {
        PortraitPresentation.Fallback(displayName = resolved.displayName, cardId = card.id)
    } else {
        PortraitPresentation.Avatar(
            avatarUri = uri,
            displayName = resolved.displayName,
            cardId = card.id,
        )
    }
}

/**
 * Pinch-zoom + pan state for the portrait image. Pure data + pure transforms so the gesture
 * math is exercised by a unit test without relying on Compose's gesture machinery.
 */
internal data class ZoomState(
    val scale: Float = MIN_SCALE,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    val isZoomed: Boolean get() = scale > MIN_SCALE + ZOOM_EPSILON

    companion object {
        const val MIN_SCALE: Float = 1f
        const val MAX_SCALE: Float = 4f
        const val ZOOMED_SCALE: Float = 2.5f
        private const val ZOOM_EPSILON: Float = 0.01f
        val Default: ZoomState = ZoomState()
    }
}

internal fun ZoomState.applyTransformGesture(pan: Offset, zoomDelta: Float): ZoomState {
    val newScale = (scale * zoomDelta).coerceIn(ZoomState.MIN_SCALE, ZoomState.MAX_SCALE)
    // Pan is ignored when the image is at 1x so an accidental pan doesn't drift the centered image.
    val effectivePan = if (newScale > ZoomState.MIN_SCALE) pan else Offset.Zero
    return copy(
        scale = newScale,
        offsetX = offsetX + effectivePan.x,
        offsetY = offsetY + effectivePan.y,
    )
}

internal fun ZoomState.toggleFromDoubleTap(): ZoomState =
    if (isZoomed) {
        ZoomState.Default
    } else {
        copy(scale = ZoomState.ZOOMED_SCALE, offsetX = 0f, offsetY = 0f)
    }

private class PortraitLargeViewViewModel(
    container: AppContainer,
    characterId: String,
) : ViewModel() {
    private val state = MutableStateFlow(
        PortraitLargeViewUiState(
            card = container.companionRosterRepository.characterById(characterId)
        )
    )
    val uiState: StateFlow<PortraitLargeViewUiState> = state
}

@Composable
fun PortraitLargeViewRoute(
    navController: NavHostController,
    container: AppContainer,
    characterId: String,
) {
    val viewModel = viewModel<PortraitLargeViewViewModel>(factory = simpleViewModelFactory {
        PortraitLargeViewViewModel(container, characterId)
    })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appLanguage = LocalAppLanguage.current
    val presentation = portraitPresentation(uiState.card, appLanguage)

    PortraitLargeViewScreen(
        presentation = presentation,
        onBack = { navController.popBackStack() },
        onEditCard = { cardId ->
            navController.navigate("tavern/editor?mode=edit&id=$cardId")
        },
    )
}

@Composable
private fun PortraitLargeViewScreen(
    presentation: PortraitPresentation,
    onBack: () -> Unit,
    onEditCard: (String) -> Unit,
) {
    when (presentation) {
        is PortraitPresentation.Missing -> MissingPortrait(onBack)
        is PortraitPresentation.Fallback -> FallbackPortrait(
            displayName = presentation.displayName,
            onBack = onBack,
            onEdit = { onEditCard(presentation.cardId) },
        )
        is PortraitPresentation.Avatar -> AvatarPortrait(
            avatarUri = presentation.avatarUri,
            displayName = presentation.displayName,
            onBack = onBack,
        )
    }
}

@Composable
private fun AvatarPortrait(
    avatarUri: String,
    displayName: String,
    onBack: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    var zoom by remember { mutableStateOf(ZoomState.Default) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherColors.Surface)
            .testTag("portrait-large-view"),
    ) {
        AsyncImage(
            model = avatarUri,
            contentDescription = displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = zoom.scale,
                    scaleY = zoom.scale,
                    translationX = zoom.offsetX,
                    translationY = zoom.offsetY,
                )
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { zoom = zoom.toggleFromDoubleTap() })
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = zoom.applyTransformGesture(pan, gestureZoom)
                    }
                }
                .testTag("portrait-large-view-image"),
        )
        Text(
            text = appLanguage.pick("Back", "返回"),
            style = MaterialTheme.typography.labelLarge,
            color = AetherColors.OnSurface,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .background(AetherColors.SurfaceContainerHigh.copy(alpha = 0.85f), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("portrait-large-view-back"),
        )
    }
}

@Composable
private fun FallbackPortrait(
    displayName: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherColors.Surface)
            .padding(24.dp)
            .testTag("portrait-large-view-fallback"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader(
            title = displayName,
            description = appLanguage.pick("No portrait available", "暂无立绘"),
            leadingLabel = appLanguage.pick("Back", "返回"),
            onLeading = onBack,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AetherColors.SurfaceContainerLow, androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.displayMedium,
                    color = AetherColors.OnSurface,
                    modifier = Modifier.testTag("portrait-large-view-fallback-name"),
                )
                Text(
                    text = appLanguage.pick("Edit card to add a portrait", "编辑卡片以添加立绘"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurfaceVariant,
                )
                Text(
                    text = appLanguage.pick("Edit card", "编辑卡片"),
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.OnSurface,
                    modifier = Modifier
                        .background(AetherColors.SurfaceContainerHigh, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .testTag("portrait-large-view-edit-card"),
                )
            }
        }
    }
}

@Composable
private fun MissingPortrait(onBack: () -> Unit) {
    val appLanguage = LocalAppLanguage.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherColors.Surface)
            .padding(24.dp)
            .testTag("portrait-large-view-missing"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader(
            title = appLanguage.pick("Character missing", "角色不存在"),
            leadingLabel = appLanguage.pick("Back", "返回"),
            onLeading = onBack,
        )
    }
}
