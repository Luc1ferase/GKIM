package com.gkim.im.android.feature.navigation

import android.content.Context
import android.net.Uri
import android.util.Size
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gkim.im.android.R
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.pick
import kotlin.math.roundToInt

@Composable
internal fun WelcomeRoute(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherColors.Surface)
            .testTag("welcome-screen"),
    ) {
        WelcomeVideoBackdrop()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("welcome-native-atmosphere")
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AetherColors.Surface.copy(alpha = 0.08f),
                            AetherColors.Primary.copy(alpha = 0.16f),
                            AetherColors.OnSurface.copy(alpha = 0.68f),
                        ),
                    ),
                ),
        ) {
            WelcomeAtmosphereAccentCatalog.visibleAccentSlots().forEach { accentSlot ->
                when (accentSlot) {
                    WelcomeAtmosphereAccentSlot.TopStartOrb -> AtmosphereAccent(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 20.dp, top = 88.dp),
                        backgroundColor = AetherColors.Surface.copy(alpha = 0.12f),
                        horizontalPadding = 26.dp,
                        verticalPadding = 26.dp,
                    )
                    WelcomeAtmosphereAccentSlot.CenterEndOrb -> AtmosphereAccent(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 28.dp),
                        backgroundColor = AetherColors.PrimaryContainer.copy(alpha = 0.18f),
                        horizontalPadding = 56.dp,
                        verticalPadding = 56.dp,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.testTag("welcome-hero-content"),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = appLanguage.pick("Next-Gen IM Protocol", "新一代 IM 协议"),
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.Surface.copy(alpha = 0.86f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(AetherColors.Surface.copy(alpha = 0.14f))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
                Text(
                    text = "GKIM",
                    style = MaterialTheme.typography.displayLarge,
                    color = AetherColors.Surface,
                )
                Text(
                    text = appLanguage.pick("Welcome to GKIM", "欢迎来到 GKIM"),
                    style = MaterialTheme.typography.headlineMedium,
                    color = AetherColors.Surface.copy(alpha = 0.92f),
                )
                Text(
                    text = appLanguage.pick(
                        "An architectural-first social shell for realtime messaging, prompting, and engineering culture.",
                        "一个以建筑感为先的社交壳层，把实时消息、提示工程与工程文化合成在同一个入口。",
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AetherColors.Surface.copy(alpha = 0.76f),
                )
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth(0.2f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    AetherColors.Surface.copy(alpha = 0f),
                                    AetherColors.Primary.copy(alpha = 0.85f),
                                    AetherColors.Surface.copy(alpha = 0f),
                                ),
                            ),
                        ),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = appLanguage.pick(
                        "Preview seam: login/register currently unlock the shell while the full auth forms land in the next slice.",
                        "开发预览说明：当前“登录 / 注册”会先接入应用壳层，下一任务再补齐真实认证表单。",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.Surface.copy(alpha = 0.7f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    WelcomeGhostButton(
                        label = "注册",
                        testTag = "welcome-register-button",
                        modifier = Modifier.weight(1f),
                        onClick = onRegister,
                    )
                    WelcomePrimaryButton(
                        label = "登录",
                        testTag = "welcome-login-button",
                        modifier = Modifier.weight(1f),
                        onClick = onLogin,
                    )
                }
                Text(
                    text = appLanguage.pick("Encrypted connection · V 2.0.4", "加密连接 · V 2.0.4"),
                    style = MaterialTheme.typography.labelMedium,
                    color = AetherColors.Surface.copy(alpha = 0.54f),
                )
            }
        }
    }
}

@Composable
private fun AtmosphereAccent(
    modifier: Modifier,
    backgroundColor: Color,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    verticalPadding: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    )
}

internal enum class WelcomeAtmosphereAccentSlot {
    TopStartOrb,
    CenterEndOrb,
}

internal object WelcomeAtmosphereAccentCatalog {
    fun visibleAccentSlots(): List<WelcomeAtmosphereAccentSlot> = listOf(
        WelcomeAtmosphereAccentSlot.TopStartOrb,
        WelcomeAtmosphereAccentSlot.CenterEndOrb,
    )
}

@Composable
private fun WelcomeVideoBackdrop() {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .testTag("welcome-video"),
        factory = {
            WelcomeVideoCoverContainer(it).apply {
                bindToWelcomeVideo(
                    Uri.parse("android.resource://${context.packageName}/${R.raw.welcome_intro_1}")
                )
            }
        },
        update = { videoContainer ->
            videoContainer.ensurePlaying()
        },
    )
}

internal object WelcomeVideoCoverLayoutCalculator {
    fun calculateCoverSize(
        videoWidthPx: Int,
        videoHeightPx: Int,
        viewportWidthPx: Int,
        viewportHeightPx: Int,
    ): Size {
        if (videoWidthPx <= 0 || videoHeightPx <= 0 || viewportWidthPx <= 0 || viewportHeightPx <= 0) {
            return Size(viewportWidthPx.coerceAtLeast(0), viewportHeightPx.coerceAtLeast(0))
        }

        val widthScale = viewportWidthPx.toFloat() / videoWidthPx.toFloat()
        val heightScale = viewportHeightPx.toFloat() / videoHeightPx.toFloat()
        val coverScale = maxOf(widthScale, heightScale)

        return Size(
            (videoWidthPx * coverScale).roundToInt(),
            (videoHeightPx * coverScale).roundToInt(),
        )
    }
}

private class WelcomeVideoCoverContainer(
    context: Context,
) : FrameLayout(context) {
    private val videoView = VideoView(context)
    private var videoWidthPx: Int = 0
    private var videoHeightPx: Int = 0

    init {
        clipChildren = true
        clipToPadding = true
        addView(
            videoView,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER,
            ),
        )
    }

    fun bindToWelcomeVideo(videoUri: Uri) {
        videoView.setVideoURI(videoUri)
        videoView.setOnPreparedListener { mediaPlayer ->
            videoWidthPx = mediaPlayer.videoWidth
            videoHeightPx = mediaPlayer.videoHeight
            mediaPlayer.isLooping = true
            mediaPlayer.setVolume(0f, 0f)
            applyCoverLayout()
            videoView.start()
        }
    }

    fun ensurePlaying() {
        applyCoverLayout()
        if (!videoView.isPlaying) {
            videoView.start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyCoverLayout()
    }

    private fun applyCoverLayout() {
        val scaledSize = WelcomeVideoCoverLayoutCalculator.calculateCoverSize(
            videoWidthPx = videoWidthPx,
            videoHeightPx = videoHeightPx,
            viewportWidthPx = width,
            viewportHeightPx = height,
        )
        val layoutParams = videoView.layoutParams as LayoutParams
        if (layoutParams.width != scaledSize.width || layoutParams.height != scaledSize.height) {
            layoutParams.width = scaledSize.width
            layoutParams.height = scaledSize.height
            layoutParams.gravity = Gravity.CENTER
            videoView.layoutParams = layoutParams
        }
    }
}

@Composable
private fun WelcomeGhostButton(
    label: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = AetherColors.Surface,
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(999.dp))
            .background(AetherColors.Surface.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    )
}

@Composable
private fun WelcomePrimaryButton(
    label: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = AetherColors.Surface,
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(999.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(AetherColors.Primary, AetherColors.PrimaryContainer),
                ),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    )
}
