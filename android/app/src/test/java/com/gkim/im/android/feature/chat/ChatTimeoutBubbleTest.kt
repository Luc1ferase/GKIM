package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.designsystem.SafetyCopy
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.CompanionTurnMeta
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTimeoutBubbleTest {

    @Test
    fun `timeout bubble tone is Timeout`() {
        val p = timeoutPresentation(activePresetMaxReplyTokens = null)
        assertEquals(CompanionLifecycleTone.Timeout, p.tone)
    }

    @Test
    fun `timeout bubble offers Retry as primary affordance`() {
        val p = timeoutPresentation(activePresetMaxReplyTokens = null)
        assertTrue("Timeout must offer a Retry affordance", p.showRetry)
    }

    @Test
    fun `timeout bubble never shows regenerate or compose-new or learn-more`() {
        val p = timeoutPresentation(activePresetMaxReplyTokens = null)
        assertFalse("Timeout must not render Regenerate", p.showRegenerate)
        assertFalse("Timeout must not render Compose-new", p.showComposeNew)
        assertFalse("Timeout must not render Learn-more", p.showLearnMorePolicy)
    }

    @Test
    fun `timeout bubble never carries a block reason or failed subtype`() {
        val p = timeoutPresentation(activePresetMaxReplyTokens = null)
        assertNull("Timeout must not carry a block reason", p.blockReason)
        assertNull("Timeout must not carry a failed subtype", p.failedSubtype)
    }

    @Test
    fun `timeout bubble never renders edit-user-turn or check-connection hint`() {
        val p = timeoutPresentation(activePresetMaxReplyTokens = null)
        assertFalse("Timeout must not render Edit-user-turn", p.showEditUserTurn)
        assertFalse("Timeout must not render Check-connection hint", p.showCheckConnectionHint)
    }

    @Test
    fun `timeout bubble hides preset hint when no active preset is known`() {
        val p = timeoutPresentation(activePresetMaxReplyTokens = null)
        assertFalse(
            "A null maxReplyTokens means we cannot evaluate the heuristic — hint stays hidden",
            p.showSwitchPresetHint,
        )
    }

    @Test
    fun `timeout bubble hides preset hint when maxReplyTokens is below the cap`() {
        val p = timeoutPresentation(activePresetMaxReplyTokens = 512)
        assertFalse(p.showSwitchPresetHint)
    }

    @Test
    fun `timeout bubble hides preset hint when maxReplyTokens equals the cap`() {
        val p = timeoutPresentation(
            activePresetMaxReplyTokens = TIMEOUT_PRESET_HINT_MAX_REPLY_TOKENS_CAP,
        )
        assertFalse(
            "Strict greater-than boundary — equal value must not render the hint",
            p.showSwitchPresetHint,
        )
    }

    @Test
    fun `timeout bubble shows preset hint when maxReplyTokens exceeds the cap`() {
        val p = timeoutPresentation(
            activePresetMaxReplyTokens = TIMEOUT_PRESET_HINT_MAX_REPLY_TOKENS_CAP + 1,
        )
        assertTrue(p.showSwitchPresetHint)
    }

    @Test
    fun `timeout bubble shows preset hint for large maxReplyTokens values`() {
        val p = timeoutPresentation(activePresetMaxReplyTokens = 4096)
        assertTrue(p.showSwitchPresetHint)
    }

    @Test
    fun `timeout English copy matches SafetyCopy timeout table`() {
        val expected = SafetyCopy.localizedTimeoutCopy(AppLanguage.English)
        assertEquals("The AI took too long to respond. Please try again.", expected)
    }

    @Test
    fun `timeout Chinese copy matches SafetyCopy timeout table`() {
        val expected = SafetyCopy.localizedTimeoutCopy(AppLanguage.Chinese)
        assertEquals("AI 响应超时,请重试。", expected)
    }

    @Test
    fun `timeout preset-hint English copy matches SafetyCopy table`() {
        val expected = SafetyCopy.localizedTimeoutPresetHint(AppLanguage.English)
        assertEquals("Switching to a shorter preset may help.", expected)
    }

    @Test
    fun `timeout preset-hint Chinese copy matches SafetyCopy table`() {
        val expected = SafetyCopy.localizedTimeoutPresetHint(AppLanguage.Chinese)
        assertEquals("切换到更简短的预设可能有帮助。", expected)
    }

    @Test
    fun `timeout bubble status line begins with Timed out`() {
        val p = timeoutPresentation(activePresetMaxReplyTokens = null)
        assertTrue(
            "Timeout status line keeps the 'Timed out' prefix for glanceability",
            p.statusLine?.startsWith("Timed out") == true,
        )
    }

    @Test
    fun `timeout bubble preserves legacy body-free rendering`() {
        val p = timeoutPresentation(activePresetMaxReplyTokens = null)
        assertFalse("Timeout must not render the model body", p.showBody)
    }

    @Test
    fun `non-timeout lifecycle never carries showSwitchPresetHint`() {
        val completed = companionLifecyclePresentation(
            message = companionMessage(MessageStatus.Completed, "hi"),
            isMostRecentCompanionVariant = true,
            activePresetMaxReplyTokens = 4096,
        )
        assertNotNull(completed)
        assertFalse(
            "Completed must never inherit the timeout preset-hint flag",
            completed!!.showSwitchPresetHint,
        )

        val failed = companionLifecyclePresentation(
            message = companionMessage(MessageStatus.Failed, "rate limited"),
            isMostRecentCompanionVariant = true,
            activePresetMaxReplyTokens = 4096,
        )
        assertNotNull(failed)
        assertFalse(
            "Failed must never inherit the timeout preset-hint flag",
            failed!!.showSwitchPresetHint,
        )

        val blocked = companionLifecyclePresentation(
            message = companionMessage(MessageStatus.Blocked, "safety policy"),
            isMostRecentCompanionVariant = true,
            activePresetMaxReplyTokens = 4096,
        )
        assertNotNull(blocked)
        assertFalse(
            "Blocked must never inherit the timeout preset-hint flag",
            blocked!!.showSwitchPresetHint,
        )
    }

    private fun timeoutPresentation(
        activePresetMaxReplyTokens: Int?,
    ): CompanionLifecyclePresentation {
        val presentation = companionLifecyclePresentation(
            message = companionMessage(MessageStatus.Timeout, body = "upstream slow"),
            isMostRecentCompanionVariant = true,
            activePresetMaxReplyTokens = activePresetMaxReplyTokens,
        )
        assertNotNull("Timeout message must resolve a lifecycle presentation", presentation)
        return presentation!!
    }

    private fun companionMessage(
        status: MessageStatus,
        body: String,
    ): ChatMessage = ChatMessage(
        id = "companion-timeout-1",
        direction = MessageDirection.Incoming,
        kind = MessageKind.Text,
        body = body,
        createdAt = "2026-04-23T11:00:00Z",
        status = status,
        companionTurnMeta = CompanionTurnMeta(
            turnId = "turn-timeout-1",
            variantGroupId = "vg-timeout-1",
            variantIndex = 0,
            providerId = "openai",
            model = "gpt-4o-mini",
        ),
    )
}
