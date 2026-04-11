package com.gkim.im.android.feature.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeVideoOverlayStyleTest {
    @Test
    fun overlayScrimLeavesMoreVideoVisibleForDemoPlayback() {
        val styleClass = Class.forName("com.gkim.im.android.feature.navigation.WelcomeVideoOverlayStyle")
        val instance = styleClass.getField("INSTANCE").get(null)
        val method = styleClass.getDeclaredMethod("scrimAlphas")
        @Suppress("UNCHECKED_CAST")
        val alphas = method.invoke(instance) as List<Float>

        assertEquals(listOf(0.03f, 0.09f, 0.36f), alphas)
        assertTrue(alphas.max() <= 0.36f)
    }
}
