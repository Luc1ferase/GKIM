package com.gkim.im.android.feature.tavern

import androidx.compose.ui.geometry.Offset
import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.LocalizedText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §1.1 verification — the portrait large-view has three rendering modes (Missing / Fallback /
 * Avatar) driven by pure derivation from [CompanionCharacterCard] + [AppLanguage], and its
 * zoom state is a pure data class whose transforms (pinch, pan, double-tap toggle) are
 * testable without standing up a Compose tree.
 */
class PortraitLargeViewPresentationTest {

    private fun cardWith(
        id: String = "test-card",
        displayName: LocalizedText = LocalizedText("Daylight Listener", "晴光抚慰者"),
        avatarUri: String? = null,
    ): CompanionCharacterCard = CompanionCharacterCard(
        id = id,
        displayName = displayName,
        roleLabel = LocalizedText("Tester", "测试"),
        summary = LocalizedText.Empty,
        firstMes = LocalizedText.Empty,
        avatarText = "DL",
        avatarUri = avatarUri,
        accent = AccentTone.Primary,
        source = CompanionCharacterSource.Preset,
    )

    // -------------------------------------------------------------------------
    // portraitPresentation — derivation branches
    // -------------------------------------------------------------------------

    @Test
    fun `missing card yields Missing presentation regardless of language`() {
        assertEquals(
            PortraitPresentation.Missing,
            portraitPresentation(card = null, language = AppLanguage.English),
        )
        assertEquals(
            PortraitPresentation.Missing,
            portraitPresentation(card = null, language = AppLanguage.Chinese),
        )
    }

    @Test
    fun `card with avatarUri yields Avatar with resolved displayName`() {
        val card = cardWith(avatarUri = "content://com.example/avatar.png")

        val english = portraitPresentation(card, AppLanguage.English)
        assertTrue("expected Avatar for English", english is PortraitPresentation.Avatar)
        english as PortraitPresentation.Avatar
        assertEquals("content://com.example/avatar.png", english.avatarUri)
        assertEquals("Daylight Listener", english.displayName)
        assertEquals("test-card", english.cardId)

        val chinese = portraitPresentation(card, AppLanguage.Chinese)
        assertTrue(chinese is PortraitPresentation.Avatar)
        chinese as PortraitPresentation.Avatar
        assertEquals("晴光抚慰者", chinese.displayName)
    }

    @Test
    fun `card without avatarUri yields Fallback with resolved displayName`() {
        val card = cardWith(avatarUri = null)

        val fallback = portraitPresentation(card, AppLanguage.English)
        assertTrue(fallback is PortraitPresentation.Fallback)
        fallback as PortraitPresentation.Fallback
        assertEquals("Daylight Listener", fallback.displayName)
        assertEquals("test-card", fallback.cardId)
    }

    @Test
    fun `card with blank avatarUri also yields Fallback (defensive)`() {
        val card = cardWith(avatarUri = "   ")

        val fallback = portraitPresentation(card, AppLanguage.English)
        assertTrue(
            "blank avatarUri string must collapse to Fallback, not Avatar",
            fallback is PortraitPresentation.Fallback,
        )
    }

    // -------------------------------------------------------------------------
    // ZoomState — pure transform contract
    // -------------------------------------------------------------------------

    @Test
    fun `default ZoomState is at MIN_SCALE and centered`() {
        assertEquals(ZoomState.MIN_SCALE, ZoomState.Default.scale, 0.001f)
        assertEquals(0f, ZoomState.Default.offsetX, 0.001f)
        assertEquals(0f, ZoomState.Default.offsetY, 0.001f)
        assertFalse("Default state must not be classified as zoomed", ZoomState.Default.isZoomed)
    }

    @Test
    fun `double-tap from default goes to ZOOMED_SCALE centered`() {
        val zoomed = ZoomState.Default.toggleFromDoubleTap()
        assertEquals(ZoomState.ZOOMED_SCALE, zoomed.scale, 0.001f)
        assertEquals(0f, zoomed.offsetX, 0.001f)
        assertEquals(0f, zoomed.offsetY, 0.001f)
        assertTrue(zoomed.isZoomed)
    }

    @Test
    fun `double-tap from any zoomed state returns to Default`() {
        val zoomed = ZoomState(scale = 3.2f, offsetX = 120f, offsetY = 80f)
        assertEquals(ZoomState.Default, zoomed.toggleFromDoubleTap())
    }

    @Test
    fun `pinch gesture multiplies scale and clamps to MAX`() {
        val afterFirstPinch = ZoomState.Default.applyTransformGesture(Offset.Zero, zoomDelta = 3f)
        assertEquals(3f, afterFirstPinch.scale, 0.001f)

        val afterHugePinch = afterFirstPinch.applyTransformGesture(Offset.Zero, zoomDelta = 10f)
        assertEquals(ZoomState.MAX_SCALE, afterHugePinch.scale, 0.001f)
    }

    @Test
    fun `pinch gesture clamps scale down to MIN`() {
        val afterShrink = ZoomState(scale = 1.2f).applyTransformGesture(Offset.Zero, zoomDelta = 0.1f)
        assertEquals(ZoomState.MIN_SCALE, afterShrink.scale, 0.001f)
    }

    @Test
    fun `pan is ignored when image is at MIN_SCALE`() {
        val afterPan = ZoomState.Default.applyTransformGesture(Offset(50f, 40f), zoomDelta = 1f)
        assertEquals(0f, afterPan.offsetX, 0.001f)
        assertEquals(0f, afterPan.offsetY, 0.001f)
    }

    @Test
    fun `pan accumulates when image is zoomed`() {
        val zoomed = ZoomState(scale = 2f)
        val afterPan = zoomed.applyTransformGesture(Offset(30f, 15f), zoomDelta = 1f)
        assertEquals(30f, afterPan.offsetX, 0.001f)
        assertEquals(15f, afterPan.offsetY, 0.001f)

        val afterMore = afterPan.applyTransformGesture(Offset(-10f, 5f), zoomDelta = 1f)
        assertEquals(20f, afterMore.offsetX, 0.001f)
        assertEquals(20f, afterMore.offsetY, 0.001f)
    }

    @Test
    fun `combined pinch+pan gesture applies both in one step`() {
        val zoomed = ZoomState(scale = 1.5f, offsetX = 10f, offsetY = 5f)
        val combined = zoomed.applyTransformGesture(Offset(20f, 10f), zoomDelta = 2f)
        // scale: 1.5 * 2 = 3.0
        assertEquals(3f, combined.scale, 0.001f)
        assertEquals(30f, combined.offsetX, 0.001f)
        assertEquals(15f, combined.offsetY, 0.001f)
    }
}
