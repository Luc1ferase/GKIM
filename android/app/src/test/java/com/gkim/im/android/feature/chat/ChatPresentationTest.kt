package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.AigcProvider
import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.AigcTask
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.AttachmentType
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.CompanionTurnMeta
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.MessageStatus
import com.gkim.im.android.core.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPresentationTest {
    @Test
    fun `visible AIGC modes follow the active provider capabilities`() {
        val provider = AigcProvider(
            id = "hunyuan",
            label = "Tencent Hunyuan",
            vendor = "Tencent",
            description = "Preset provider",
            model = "hy-image-v3.0",
            accent = AccentTone.Primary,
            preset = true,
            capabilities = setOf(AigcMode.TextToImage, AigcMode.ImageToImage),
        )

        assertEquals(
            listOf(AigcMode.TextToImage, AigcMode.ImageToImage),
            visibleAigcModes(provider),
        )
    }

    @Test
    fun `generation feedback keeps queued and failed states honest`() {
        val queued = generationFeedback(
            AigcTask(
                id = "task-queued",
                providerId = "hunyuan",
                model = "hy-image-v3.0",
                mode = AigcMode.TextToImage,
                prompt = "Render a queued frame",
                createdAt = "2026-04-13T00:00:00Z",
                status = TaskStatus.Queued,
            )
        )
        val failed = generationFeedback(
            AigcTask(
                id = "task-failed",
                providerId = "hunyuan",
                model = "hy-image-v3.0",
                mode = AigcMode.TextToImage,
                prompt = "Render a failed frame",
                createdAt = "2026-04-13T00:00:00Z",
                status = TaskStatus.Failed,
                errorMessage = "API key is required",
            )
        )
        val succeeded = generationFeedback(
            AigcTask(
                id = "task-succeeded",
                remoteId = "remote-1",
                providerId = "tongyi",
                model = "wan2.7-image",
                mode = AigcMode.TextToImage,
                prompt = "Render a succeeded frame",
                createdAt = "2026-04-13T00:00:00Z",
                status = TaskStatus.Succeeded,
                outputPreview = "https://cdn.example.com/image.png",
            )
        )

        assertTrue(queued.statusLine.contains("Generating"))
        assertFalse(queued.showPreview)
        assertTrue(failed.statusLine.contains("API key is required"))
        assertFalse(failed.showPreview)
        assertTrue(succeeded.statusLine.contains("wan2.7-image"))
        assertTrue(succeeded.showPreview)
    }

    @Test
    fun `companion lifecycle thinking state shows placeholder without body`() {
        val presentation = companionLifecyclePresentation(
            message = companionMessage(status = MessageStatus.Thinking, body = ""),
            isMostRecentCompanionVariant = true,
        )
        assertNotNull(presentation)
        assertEquals(CompanionLifecycleTone.Thinking, presentation!!.tone)
        assertEquals("Thinking…", presentation.statusLine)
        assertFalse(presentation.showBody)
        assertFalse(presentation.showRegenerate)
        assertFalse(presentation.showRetry)
    }

    @Test
    fun `companion lifecycle streaming state shows growing body with status`() {
        val presentation = companionLifecyclePresentation(
            message = companionMessage(status = MessageStatus.Streaming, body = "Hello the"),
            isMostRecentCompanionVariant = true,
        )!!
        assertEquals(CompanionLifecycleTone.Streaming, presentation.tone)
        assertEquals("Streaming…", presentation.statusLine)
        assertTrue(presentation.showBody)
        assertEquals("Hello the", presentation.body)
        assertFalse(presentation.showRegenerate)
    }

    @Test
    fun `companion lifecycle completed state exposes model badge and regenerate on latest variant`() {
        val presentation = companionLifecyclePresentation(
            message = companionMessage(
                status = MessageStatus.Completed,
                body = "Hello there.",
                canRegenerate = true,
                model = "gpt-4o-mini",
            ),
            isMostRecentCompanionVariant = true,
        )!!
        assertEquals(CompanionLifecycleTone.Completed, presentation.tone)
        assertEquals("Model · gpt-4o-mini", presentation.statusLine)
        assertEquals("Hello there.", presentation.body)
        assertTrue(presentation.showBody)
        assertTrue(presentation.showRegenerate)
        assertFalse(presentation.showRetry)
    }

    @Test
    fun `companion lifecycle completed state hides status line when no model badge is available`() {
        val presentation = companionLifecyclePresentation(
            message = companionMessage(
                status = MessageStatus.Completed,
                body = "Hello there.",
                canRegenerate = true,
                model = null,
            ),
            isMostRecentCompanionVariant = true,
        )!!
        assertEquals(CompanionLifecycleTone.Completed, presentation.tone)
        assertEquals(null, presentation.statusLine)
        assertEquals("Hello there.", presentation.body)
        assertTrue(presentation.showBody)
    }

    @Test
    fun `companion lifecycle completed state hides regenerate on older variants`() {
        val presentation = companionLifecyclePresentation(
            message = companionMessage(
                status = MessageStatus.Completed,
                body = "Hello there.",
                canRegenerate = true,
                model = "gpt-4o-mini",
            ),
            isMostRecentCompanionVariant = false,
        )!!
        assertFalse(presentation.showRegenerate)
    }

    @Test
    fun `companion lifecycle failed state surfaces reason and retry`() {
        val presentation = companionLifecyclePresentation(
            message = companionMessage(status = MessageStatus.Failed, body = "rate limited"),
            isMostRecentCompanionVariant = true,
        )!!
        assertEquals(CompanionLifecycleTone.Failed, presentation.tone)
        assertEquals("Failed · rate limited", presentation.statusLine)
        assertFalse(presentation.showBody)
        assertTrue(presentation.showRetry)
        assertFalse(presentation.showRegenerate)
    }

    @Test
    fun `companion lifecycle failed state propagates retryAfterEpochMs onto presentation when meta carries it`() {
        val deadline = 1_777_377_612_000L
        val presentation = companionLifecyclePresentation(
            message = companionMessage(
                status = MessageStatus.Failed,
                body = "rate limited",
                failedSubtypeKey = "transient",
                retryAfterEpochMs = deadline,
            ),
            isMostRecentCompanionVariant = true,
        )!!
        assertEquals(deadline, presentation.retryAfterEpochMs)
        // Field presence MUST NOT toggle showRetry — the bubble still wants
        // a Retry affordance; the render-time logic decides whether it's
        // clickable or counting down.
        assertTrue(presentation.showRetry)
    }

    @Test
    fun `companion lifecycle failed state has null retryAfterEpochMs when meta does not carry it`() {
        val presentation = companionLifecyclePresentation(
            message = companionMessage(
                status = MessageStatus.Failed,
                body = "rate limited",
                failedSubtypeKey = "transient",
                retryAfterEpochMs = null,
            ),
            isMostRecentCompanionVariant = true,
        )!!
        assertEquals(null, presentation.retryAfterEpochMs)
        assertTrue(presentation.showRetry)
    }

    @Test
    fun `non-failed lifecycle states never carry retryAfterEpochMs even when meta is set`() {
        // Defensive: only the Failed branch propagates the field. Streaming /
        // Completed / Timeout / Blocked / Thinking all leave it null.
        val streaming = companionLifecyclePresentation(
            message = companionMessage(
                status = MessageStatus.Streaming,
                body = "partial",
                retryAfterEpochMs = 1_777_377_612_000L,
            ),
            isMostRecentCompanionVariant = true,
        )!!
        assertEquals(null, streaming.retryAfterEpochMs)

        val timeout = companionLifecyclePresentation(
            message = companionMessage(
                status = MessageStatus.Timeout,
                body = "elapsed",
                retryAfterEpochMs = 1_777_377_612_000L,
            ),
            isMostRecentCompanionVariant = true,
        )!!
        assertEquals(null, timeout.retryAfterEpochMs)
    }

    @Test
    fun `companion lifecycle timeout state uses distinct wording from failed`() {
        val presentation = companionLifecyclePresentation(
            message = companionMessage(status = MessageStatus.Timeout, body = "upstream slow"),
            isMostRecentCompanionVariant = true,
        )!!
        assertEquals(CompanionLifecycleTone.Timeout, presentation.tone)
        assertTrue(presentation.statusLine?.startsWith("Timed out") == true)
        assertTrue(presentation.showRetry)
    }

    @Test
    fun `companion lifecycle blocked state cites reason with neutral copy`() {
        val presentation = companionLifecyclePresentation(
            message = companionMessage(status = MessageStatus.Blocked, body = "safety policy"),
            isMostRecentCompanionVariant = true,
        )!!
        assertEquals(CompanionLifecycleTone.Blocked, presentation.tone)
        assertEquals("Blocked · safety policy", presentation.statusLine)
        assertFalse(presentation.showBody)
        assertFalse(presentation.showRetry)
        assertFalse(presentation.showRegenerate)
    }

    @Test
    fun `companion lifecycle returns null for peer messages without companion meta`() {
        val peerMessage = ChatMessage(
            id = "peer-1",
            direction = MessageDirection.Incoming,
            kind = MessageKind.Text,
            body = "hey",
            createdAt = "2026-04-21T08:00:00Z",
        )
        val presentation = companionLifecyclePresentation(
            message = peerMessage,
            isMostRecentCompanionVariant = true,
        )
        assertNull(presentation)
    }

    @Test
    fun `companion lifecycle transitions reflect sequential statuses`() {
        val thinking = companionLifecyclePresentation(
            companionMessage(status = MessageStatus.Thinking, body = ""),
            isMostRecentCompanionVariant = true,
        )!!
        val streaming = companionLifecyclePresentation(
            companionMessage(status = MessageStatus.Streaming, body = "Hel"),
            isMostRecentCompanionVariant = true,
        )!!
        val completed = companionLifecyclePresentation(
            companionMessage(
                status = MessageStatus.Completed,
                body = "Hello.",
                canRegenerate = true,
            ),
            isMostRecentCompanionVariant = true,
        )!!
        assertEquals(CompanionLifecycleTone.Thinking, thinking.tone)
        assertEquals(CompanionLifecycleTone.Streaming, streaming.tone)
        assertEquals(CompanionLifecycleTone.Completed, completed.tone)
        assertFalse(thinking.showBody)
        assertTrue(streaming.showBody)
        assertTrue(completed.showBody)
        assertTrue(completed.showRegenerate)
    }

    @Test
    fun `ready AIGC modes require matching generation source media`() {
        val provider = AigcProvider(
            id = "tongyi",
            label = "Alibaba Tongyi",
            vendor = "Alibaba",
            description = "Preset provider",
            model = "wan2.7-image",
            accent = AccentTone.Secondary,
            preset = true,
            capabilities = setOf(AigcMode.TextToImage, AigcMode.ImageToImage, AigcMode.VideoToVideo),
        )
        assertEquals(listOf(AigcMode.TextToImage), readyAigcModes(provider, null))
        assertEquals(
            listOf(AigcMode.TextToImage, AigcMode.ImageToImage),
            readyAigcModes(provider, AttachmentType.Image),
        )
        assertEquals(
            listOf(AigcMode.TextToImage, AigcMode.VideoToVideo),
            readyAigcModes(provider, AttachmentType.Video),
        )
    }

    @Test
    fun `outgoingSubmissionFailureLine returns null for incoming messages`() {
        val incoming = ChatMessage(
            id = "companion-1",
            direction = MessageDirection.Incoming,
            kind = MessageKind.Text,
            body = "Hi",
            createdAt = "2026-04-21T08:00:00Z",
            status = MessageStatus.Failed,
        )
        assertNull(outgoingSubmissionFailureLine(incoming))
    }

    @Test
    fun `outgoingSubmissionFailureLine returns null for completed outgoing messages`() {
        val completed = userMessage(status = MessageStatus.Completed)
        assertNull(outgoingSubmissionFailureLine(completed))
    }

    @Test
    fun `outgoingSubmissionFailureLine returns failed copy for failed outgoing messages`() {
        val failed = userMessage(status = MessageStatus.Failed)
        assertEquals("Failed to send", outgoingSubmissionFailureLine(failed))
    }

    @Test
    fun `outgoingSubmissionFailureLine returns timeout copy for timeout outgoing messages`() {
        val timeout = userMessage(status = MessageStatus.Timeout)
        assertEquals("Timed out — tap retry", outgoingSubmissionFailureLine(timeout))
    }

    private fun userMessage(status: MessageStatus): ChatMessage = ChatMessage(
        id = "user-1",
        direction = MessageDirection.Outgoing,
        kind = MessageKind.Text,
        body = "Hi",
        createdAt = "2026-04-21T08:00:00Z",
        status = status,
    )

    @Test
    fun `formatRetryCooldownNotice English form`() {
        assertEquals(
            "Retry available in 12s",
            formatRetryCooldownNotice(remainingSeconds = 12L, language = AppLanguage.English),
        )
    }

    @Test
    fun `formatRetryCooldownNotice Chinese form`() {
        assertEquals(
            "12 秒后才能重试",
            formatRetryCooldownNotice(remainingSeconds = 12L, language = AppLanguage.Chinese),
        )
    }

    @Test
    fun `formatRetryCooldownNotice clamps the rendered second to the helper's input regardless of size`() {
        // Defensive: the caller (ChatMessageRow's onClick lambda) clamps to >=1
        // before calling this helper. The helper itself does not re-clamp; that
        // responsibility is split. This pin guards the contract.
        assertEquals(
            "Retry available in 1s",
            formatRetryCooldownNotice(remainingSeconds = 1L, language = AppLanguage.English),
        )
        assertEquals(
            "Retry available in 600s",
            formatRetryCooldownNotice(remainingSeconds = 600L, language = AppLanguage.English),
        )
    }

    private fun companionMessage(
        status: MessageStatus,
        body: String,
        canRegenerate: Boolean = false,
        model: String? = null,
        failedSubtypeKey: String? = null,
        retryAfterEpochMs: Long? = null,
    ): ChatMessage = ChatMessage(
        id = "companion-1",
        direction = MessageDirection.Incoming,
        kind = MessageKind.Text,
        body = body,
        createdAt = "2026-04-21T08:00:00Z",
        status = status,
        companionTurnMeta = CompanionTurnMeta(
            turnId = "turn-1",
            variantGroupId = "vg-1",
            variantIndex = 0,
            providerId = "openai",
            model = model,
            canRegenerate = canRegenerate,
            failedSubtypeKey = failedSubtypeKey,
            retryAfterEpochMs = retryAfterEpochMs,
        ),
    )
}
