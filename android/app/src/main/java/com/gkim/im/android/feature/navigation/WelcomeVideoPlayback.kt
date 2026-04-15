package com.gkim.im.android.feature.navigation

import android.content.Context
import android.net.Uri
import android.util.Size
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.gkim.im.android.R
import com.gkim.im.android.core.designsystem.AetherColors
import kotlin.math.roundToInt

internal data class WelcomeVideoPlaybackState(
    val isSurfaceAttached: Boolean = false,
    val isLifecycleResumed: Boolean = false,
    val isPlayerReady: Boolean = false,
    val isActivelyPlaying: Boolean = false,
    val showFallback: Boolean = false,
) {
    val shouldPlay: Boolean
        get() = isSurfaceAttached && isLifecycleResumed && isPlayerReady && !showFallback

    val statusTag: String
        get() = when {
            showFallback -> "welcome-video-state-fallback"
            isActivelyPlaying -> "welcome-video-state-playing"
            else -> "welcome-video-state-loading"
        }
}

internal sealed interface WelcomeVideoPlaybackEvent {
    data object SurfaceAttached : WelcomeVideoPlaybackEvent
    data object SurfaceDetached : WelcomeVideoPlaybackEvent
    data object LifecycleResumed : WelcomeVideoPlaybackEvent
    data object LifecyclePaused : WelcomeVideoPlaybackEvent
    data object PlayerReady : WelcomeVideoPlaybackEvent
    data object PlaybackStarted : WelcomeVideoPlaybackEvent
    data object PlaybackStopped : WelcomeVideoPlaybackEvent
    data object PlayerError : WelcomeVideoPlaybackEvent
    data object RetryRequested : WelcomeVideoPlaybackEvent
}

internal object WelcomeVideoPlaybackStateMachine {
    fun reduce(
        current: WelcomeVideoPlaybackState,
        event: WelcomeVideoPlaybackEvent,
    ): WelcomeVideoPlaybackState = when (event) {
        WelcomeVideoPlaybackEvent.SurfaceAttached -> current.copy(isSurfaceAttached = true)
        WelcomeVideoPlaybackEvent.SurfaceDetached -> current.copy(
            isSurfaceAttached = false,
            isActivelyPlaying = false,
        )
        WelcomeVideoPlaybackEvent.LifecycleResumed -> current.copy(isLifecycleResumed = true)
        WelcomeVideoPlaybackEvent.LifecyclePaused -> current.copy(
            isLifecycleResumed = false,
            isActivelyPlaying = false,
        )
        WelcomeVideoPlaybackEvent.PlayerReady -> current.copy(isPlayerReady = true)
        WelcomeVideoPlaybackEvent.PlaybackStarted -> current.copy(
            isActivelyPlaying = true,
            showFallback = false,
        )
        WelcomeVideoPlaybackEvent.PlaybackStopped -> current.copy(isActivelyPlaying = false)
        WelcomeVideoPlaybackEvent.PlayerError -> current.copy(
            isPlayerReady = false,
            isActivelyPlaying = false,
            showFallback = true,
        )
        WelcomeVideoPlaybackEvent.RetryRequested -> current.copy(
            isPlayerReady = false,
            isActivelyPlaying = false,
            showFallback = false,
        )
    }
}

@Composable
internal fun WelcomeVideoBackdrop() {
    val context = LocalContext.current
    val videoUri = remember(context) {
        Uri.parse("android.resource://${context.packageName}/${R.raw.welcome_intro_1}")
    }
    var playbackState by remember { mutableStateOf(WelcomeVideoPlaybackState()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("welcome-video"),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { androidContext ->
                WelcomeVideoPlaybackView(androidContext).apply {
                    onStateChanged = { state -> playbackState = state }
                    bindToWelcomeVideo(videoUri)
                }
            },
            update = { playbackView ->
                playbackView.onStateChanged = { state -> playbackState = state }
                playbackView.bindToWelcomeVideo(videoUri)
            },
        )

        if (playbackState.showFallback) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AetherColors.Surface.copy(alpha = 0.24f))
                    .testTag("welcome-video-fallback-overlay"),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(playbackState.statusTag),
        )
    }
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

private class WelcomeVideoPlaybackView(
    context: Context,
) : FrameLayout(context) {
    private val playerView = PlayerView(context).apply {
        useController = false
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            Gravity.CENTER,
        )
    }

    private var lifecycleOwner: LifecycleOwner? = null
    private var player: ExoPlayer? = null
    private var playbackState = WelcomeVideoPlaybackState()
    private var boundVideoUri: Uri? = null

    var onStateChanged: (WelcomeVideoPlaybackState) -> Unit = {}

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                handleEvent(WelcomeVideoPlaybackEvent.PlayerReady)
                syncPlayback()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            handleEvent(
                if (isPlaying) {
                    WelcomeVideoPlaybackEvent.PlaybackStarted
                } else {
                    WelcomeVideoPlaybackEvent.PlaybackStopped
                }
            )
        }

        override fun onPlayerError(error: PlaybackException) {
            handleEvent(WelcomeVideoPlaybackEvent.PlayerError)
        }
    }

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            handleEvent(WelcomeVideoPlaybackEvent.LifecycleResumed)
            retryIfNeeded()
            syncPlayback()
        }

        override fun onPause(owner: LifecycleOwner) {
            handleEvent(WelcomeVideoPlaybackEvent.LifecyclePaused)
            syncPlayback()
        }
    }

    init {
        clipChildren = true
        clipToPadding = true
        addView(playerView)
    }

    fun bindToWelcomeVideo(videoUri: Uri) {
        boundVideoUri = videoUri
        val mediaItem = MediaItem.fromUri(videoUri)
        val player = ensurePlayer()
        if (player.currentMediaItem?.localConfiguration?.uri != videoUri) {
            player.setMediaItem(mediaItem)
            player.prepare()
        } else if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
        syncPlayback()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handleEvent(WelcomeVideoPlaybackEvent.SurfaceAttached)
        ensureLifecycleObserver()
        syncPlayback()
        retryIfNeeded()
    }

    override fun onDetachedFromWindow() {
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = null
        handleEvent(WelcomeVideoPlaybackEvent.SurfaceDetached)
        releasePlayer()
        super.onDetachedFromWindow()
    }

    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }
        return ExoPlayer.Builder(context)
            .build()
            .also { createdPlayer ->
                createdPlayer.repeatMode = Player.REPEAT_MODE_ONE
                createdPlayer.volume = 0f
                createdPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                createdPlayer.addListener(playerListener)
                playerView.player = createdPlayer
                player = createdPlayer
            }
    }

    private fun ensureLifecycleObserver() {
        val nextOwner = findViewTreeLifecycleOwner() ?: return
        if (lifecycleOwner === nextOwner) {
            syncLifecycleState(nextOwner.lifecycle.currentState)
            return
        }

        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = nextOwner
        nextOwner.lifecycle.addObserver(lifecycleObserver)
        syncLifecycleState(nextOwner.lifecycle.currentState)
    }

    private fun syncLifecycleState(state: Lifecycle.State) {
        handleEvent(
            if (state.isAtLeast(Lifecycle.State.RESUMED)) {
                WelcomeVideoPlaybackEvent.LifecycleResumed
            } else {
                WelcomeVideoPlaybackEvent.LifecyclePaused
            }
        )
    }

    private fun retryIfNeeded() {
        val videoUri = boundVideoUri ?: return
        if (!playbackState.showFallback) {
            return
        }

        handleEvent(WelcomeVideoPlaybackEvent.RetryRequested)
        ensurePlayer().apply {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
        }
    }

    private fun syncPlayback() {
        val player = player ?: return
        if (playbackState.shouldPlay) {
            player.playWhenReady = true
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.play()
            return
        }

        player.playWhenReady = false
        player.pause()
    }

    private fun handleEvent(event: WelcomeVideoPlaybackEvent) {
        val nextState = WelcomeVideoPlaybackStateMachine.reduce(playbackState, event)
        if (nextState == playbackState) {
            return
        }
        playbackState = nextState
        onStateChanged(nextState)
    }

    private fun releasePlayer() {
        playerView.player = null
        player?.removeListener(playerListener)
        player?.release()
        player = null
    }
}
