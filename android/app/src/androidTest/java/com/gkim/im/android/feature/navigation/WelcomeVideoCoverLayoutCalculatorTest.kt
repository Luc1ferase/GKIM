package com.gkim.im.android.feature.navigation

import android.util.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeVideoCoverLayoutCalculatorTest {
    @Test
    fun coverLayoutExpandsPortraitAssetToFillTallerPhoneViewport() {
        val scaledSize = calculateCoverSize(
            videoWidthPx = 1080,
            videoHeightPx = 1920,
            viewportWidthPx = 1080,
            viewportHeightPx = 2400,
        )

        assertEquals(Size(1350, 2400), scaledSize)
        assertTrue(scaledSize.width > 1080)
        assertEquals(2400, scaledSize.height)
    }

    @Test
    fun coverLayoutExpandsPortraitAssetToFillWiderPhoneViewport() {
        val scaledSize = calculateCoverSize(
            videoWidthPx = 1080,
            videoHeightPx = 1920,
            viewportWidthPx = 1440,
            viewportHeightPx = 1920,
        )

        assertEquals(Size(1440, 2560), scaledSize)
        assertEquals(1440, scaledSize.width)
        assertTrue(scaledSize.height > 1920)
    }

    private fun calculateCoverSize(
        videoWidthPx: Int,
        videoHeightPx: Int,
        viewportWidthPx: Int,
        viewportHeightPx: Int,
    ): Size {
        val calculatorClass = Class.forName("com.gkim.im.android.feature.navigation.WelcomeVideoCoverLayoutCalculator")
        val instance = calculatorClass.getField("INSTANCE").get(null)
        val method = calculatorClass.getDeclaredMethod(
            "calculateCoverSize",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        )

        return method.invoke(
            instance,
            videoWidthPx,
            videoHeightPx,
            viewportWidthPx,
            viewportHeightPx,
        ) as Size
    }
}
