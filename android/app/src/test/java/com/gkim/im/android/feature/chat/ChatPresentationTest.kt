package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.AigcProvider
import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.AigcTask
import com.gkim.im.android.core.model.AttachmentType
import com.gkim.im.android.core.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
