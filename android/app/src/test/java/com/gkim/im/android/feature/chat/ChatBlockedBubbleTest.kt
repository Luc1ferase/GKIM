package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.designsystem.BlockReasonCopy
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.BlockReason
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

class ChatBlockedBubbleTest {
    @Test
    fun `blocked bubble decodes every BlockReason wire key`() {
        BlockReason.entries.forEach { reason ->
            val presentation = blockedPresentation(reason.wireKey)
            assertEquals(reason, presentation.blockReason)
        }
    }

    @Test
    fun `blocked bubble falls back to Other on unknown wire key`() {
        val presentation = blockedPresentation("some_future_reason_the_backend_invented")
        assertEquals(BlockReason.Other, presentation.blockReason)
    }

    @Test
    fun `blocked bubble falls back to Other when wire key is null`() {
        val presentation = blockedPresentation(null)
        assertEquals(BlockReason.Other, presentation.blockReason)
    }

    @Test
    fun `blocked bubble never shows retry or regenerate affordances`() {
        BlockReason.entries.forEach { reason ->
            val presentation = blockedPresentation(reason.wireKey)
            assertFalse("retry must never render on blocked for $reason", presentation.showRetry)
            assertFalse("regenerate must never render on blocked for $reason", presentation.showRegenerate)
        }
    }

    @Test
    fun `blocked bubble wires compose-new and learn-more actions for every reason`() {
        BlockReason.entries.forEach { reason ->
            val presentation = blockedPresentation(reason.wireKey)
            assertTrue("compose-new must be wired for $reason", presentation.showComposeNew)
            assertTrue("learn-more must be wired for $reason", presentation.showLearnMorePolicy)
        }
    }

    @Test
    fun `blocked bubble tone is Blocked for every reason`() {
        BlockReason.entries.forEach { reason ->
            val presentation = blockedPresentation(reason.wireKey)
            assertEquals(CompanionLifecycleTone.Blocked, presentation.tone)
        }
    }

    @Test
    fun `blocked bubble hides body so block copy is the only prose shown`() {
        BlockReason.entries.forEach { reason ->
            val presentation = blockedPresentation(reason.wireKey)
            assertFalse("showBody must be false on blocked for $reason", presentation.showBody)
        }
    }

    @Test
    fun `blocked bubble English copy matches BlockReasonCopy table for every reason`() {
        BlockReason.entries.forEach { reason ->
            val presentation = blockedPresentation(reason.wireKey)
            val expected = BlockReasonCopy.localizedCopy(reason, AppLanguage.English)
            val resolved = presentation.blockReason?.let { BlockReasonCopy.localizedCopy(it, AppLanguage.English) }
            assertEquals("English copy must round-trip through presentation for $reason", expected, resolved)
        }
    }

    @Test
    fun `blocked bubble Chinese copy matches BlockReasonCopy table for every reason`() {
        BlockReason.entries.forEach { reason ->
            val presentation = blockedPresentation(reason.wireKey)
            val expected = BlockReasonCopy.localizedCopy(reason, AppLanguage.Chinese)
            val resolved = presentation.blockReason?.let { BlockReasonCopy.localizedCopy(it, AppLanguage.Chinese) }
            assertEquals("Chinese copy must round-trip through presentation for $reason", expected, resolved)
        }
    }

    @Test
    fun `non-blocked lifecycle never carries a block reason`() {
        val completed = companionLifecyclePresentation(
            message = companionMessage(MessageStatus.Completed, "hi", blockReasonKey = "self_harm"),
            isMostRecentCompanionVariant = true,
        )
        assertNotNull(completed)
        assertNull("Completed must not surface a block reason", completed!!.blockReason)
        assertFalse(completed.showComposeNew)
        assertFalse(completed.showLearnMorePolicy)
    }

    private fun blockedPresentation(blockReasonKey: String?): CompanionLifecyclePresentation {
        val presentation = companionLifecyclePresentation(
            message = companionMessage(MessageStatus.Blocked, body = "", blockReasonKey = blockReasonKey),
            isMostRecentCompanionVariant = true,
        )
        assertNotNull("Blocked message must resolve a lifecycle presentation", presentation)
        return presentation!!
    }

    private fun companionMessage(
        status: MessageStatus,
        body: String,
        blockReasonKey: String? = null,
    ): ChatMessage = ChatMessage(
        id = "companion-block-1",
        direction = MessageDirection.Incoming,
        kind = MessageKind.Text,
        body = body,
        createdAt = "2026-04-23T10:00:00Z",
        status = status,
        companionTurnMeta = CompanionTurnMeta(
            turnId = "turn-block-1",
            variantGroupId = "vg-block-1",
            variantIndex = 0,
            providerId = "openai",
            model = "gpt-4o-mini",
            blockReasonKey = blockReasonKey,
        ),
    )
}
