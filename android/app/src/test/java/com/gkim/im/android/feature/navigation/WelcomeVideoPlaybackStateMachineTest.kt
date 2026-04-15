package com.gkim.im.android.feature.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeVideoPlaybackStateMachineTest {
    @Test
    fun `welcome video becomes active after attach resume and player ready`() {
        var state = WelcomeVideoPlaybackState()

        state = WelcomeVideoPlaybackStateMachine.reduce(state, WelcomeVideoPlaybackEvent.SurfaceAttached)
        state = WelcomeVideoPlaybackStateMachine.reduce(state, WelcomeVideoPlaybackEvent.LifecycleResumed)
        state = WelcomeVideoPlaybackStateMachine.reduce(state, WelcomeVideoPlaybackEvent.PlayerReady)

        assertTrue(state.shouldPlay)
        assertFalse(state.showFallback)
    }

    @Test
    fun `welcome video resumes active playback after lifecycle pause and resume`() {
        var state = WelcomeVideoPlaybackState(
            isSurfaceAttached = true,
            isLifecycleResumed = true,
            isPlayerReady = true,
        )

        state = WelcomeVideoPlaybackStateMachine.reduce(state, WelcomeVideoPlaybackEvent.LifecyclePaused)
        assertFalse(state.shouldPlay)

        state = WelcomeVideoPlaybackStateMachine.reduce(state, WelcomeVideoPlaybackEvent.LifecycleResumed)
        assertTrue(state.shouldPlay)
        assertFalse(state.showFallback)
    }

    @Test
    fun `welcome video exposes fallback after player error until retry`() {
        var state = WelcomeVideoPlaybackState(
            isSurfaceAttached = true,
            isLifecycleResumed = true,
            isPlayerReady = true,
        )

        state = WelcomeVideoPlaybackStateMachine.reduce(state, WelcomeVideoPlaybackEvent.PlayerError)

        assertFalse(state.shouldPlay)
        assertTrue(state.showFallback)

        state = WelcomeVideoPlaybackStateMachine.reduce(state, WelcomeVideoPlaybackEvent.RetryRequested)

        assertFalse(state.showFallback)
        assertFalse(state.shouldPlay)
    }
}
