package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.designsystem.SafetyCopy
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.CompanionTurnMeta
import com.gkim.im.android.core.model.FailedSubtype
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatFailedBubbleTest {

    @Test
    fun `failed bubble decodes every FailedSubtype wire key`() {
        FailedSubtype.entries.forEach { subtype ->
            val presentation = failedPresentation(subtype.wireKey)
            assertEquals(subtype, presentation.failedSubtype)
        }
    }

    @Test
    fun `failed bubble falls back to Unknown on unrecognized wire key`() {
        val presentation = failedPresentation("something_the_backend_added_later")
        assertEquals(FailedSubtype.Unknown, presentation.failedSubtype)
    }

    @Test
    fun `failed bubble with null wire key leaves subtype null for legacy fallback`() {
        val presentation = failedPresentation(null)
        assertNull(
            "When no subtype is persisted the bubble must keep the legacy statusLine path",
            presentation.failedSubtype,
        )
    }

    @Test
    fun `Transient failed offers Retry and no Edit or connection hint`() {
        val p = failedPresentation(FailedSubtype.Transient.wireKey)
        assertTrue(p.showRetry)
        assertFalse(p.showEditUserTurn)
        assertFalse(p.showCheckConnectionHint)
    }

    @Test
    fun `PromptBudgetExceeded failed offers Edit user turn with no Retry`() {
        val p = failedPresentation(FailedSubtype.PromptBudgetExceeded.wireKey)
        assertFalse(p.showRetry)
        assertTrue(p.showEditUserTurn)
        assertFalse(p.showCheckConnectionHint)
    }

    @Test
    fun `AuthenticationFailed failed offers Edit user turn with no Retry`() {
        val p = failedPresentation(FailedSubtype.AuthenticationFailed.wireKey)
        assertFalse(p.showRetry)
        assertTrue(p.showEditUserTurn)
        assertFalse(p.showCheckConnectionHint)
    }

    @Test
    fun `ProviderUnavailable failed offers Retry with check-connection hint`() {
        val p = failedPresentation(FailedSubtype.ProviderUnavailable.wireKey)
        assertTrue(p.showRetry)
        assertFalse(p.showEditUserTurn)
        assertTrue(p.showCheckConnectionHint)
    }

    @Test
    fun `NetworkError failed offers Retry with check-connection hint`() {
        val p = failedPresentation(FailedSubtype.NetworkError.wireKey)
        assertTrue(p.showRetry)
        assertFalse(p.showEditUserTurn)
        assertTrue(p.showCheckConnectionHint)
    }

    @Test
    fun `Unknown failed offers generic Retry with no Edit or connection hint`() {
        val p = failedPresentation(FailedSubtype.Unknown.wireKey)
        assertTrue(p.showRetry)
        assertFalse(p.showEditUserTurn)
        assertFalse(p.showCheckConnectionHint)
    }

    @Test
    fun `failed bubble never shows regenerate or a block reason`() {
        FailedSubtype.entries.forEach { subtype ->
            val p = failedPresentation(subtype.wireKey)
            assertFalse("regenerate must never render on failed for $subtype", p.showRegenerate)
            assertNull("block reason must never leak onto failed for $subtype", p.blockReason)
            assertFalse(
                "compose-new must never render on failed for $subtype",
                p.showComposeNew,
            )
            assertFalse(
                "learn-more must never render on failed for $subtype",
                p.showLearnMorePolicy,
            )
        }
    }

    @Test
    fun `failed bubble tone is Failed for every subtype`() {
        FailedSubtype.entries.forEach { subtype ->
            val p = failedPresentation(subtype.wireKey)
            assertEquals(CompanionLifecycleTone.Failed, p.tone)
        }
    }

    @Test
    fun `failed bubble English copy matches SafetyCopy table for every subtype`() {
        FailedSubtype.entries.forEach { subtype ->
            val p = failedPresentation(subtype.wireKey)
            val expected = SafetyCopy.localizedFailedCopy(subtype, AppLanguage.English)
            val resolved = p.failedSubtype?.let { SafetyCopy.localizedFailedCopy(it, AppLanguage.English) }
            assertEquals("English copy must round-trip through presentation for $subtype", expected, resolved)
        }
    }

    @Test
    fun `failed bubble Chinese copy matches SafetyCopy table for every subtype`() {
        FailedSubtype.entries.forEach { subtype ->
            val p = failedPresentation(subtype.wireKey)
            val expected = SafetyCopy.localizedFailedCopy(subtype, AppLanguage.Chinese)
            val resolved = p.failedSubtype?.let { SafetyCopy.localizedFailedCopy(it, AppLanguage.Chinese) }
            assertEquals("Chinese copy must round-trip through presentation for $subtype", expected, resolved)
        }
    }

    @Test
    fun `non-failed lifecycle never carries a failed subtype`() {
        val completed = companionLifecyclePresentation(
            message = companionMessage(MessageStatus.Completed, "hi", failedSubtypeKey = "transient"),
            isMostRecentCompanionVariant = true,
        )
        assertNotNull(completed)
        assertNull("Completed must not surface a failed subtype", completed!!.failedSubtype)
        assertFalse(completed.showEditUserTurn)
        assertFalse(completed.showCheckConnectionHint)
    }

    private fun failedPresentation(failedSubtypeKey: String?): CompanionLifecyclePresentation {
        val presentation = companionLifecyclePresentation(
            message = companionMessage(MessageStatus.Failed, body = "", failedSubtypeKey = failedSubtypeKey),
            isMostRecentCompanionVariant = true,
        )
        assertNotNull("Failed message must resolve a lifecycle presentation", presentation)
        return presentation!!
    }

    private fun companionMessage(
        status: MessageStatus,
        body: String,
        failedSubtypeKey: String? = null,
    ): ChatMessage = ChatMessage(
        id = "companion-failed-1",
        direction = MessageDirection.Incoming,
        kind = MessageKind.Text,
        body = body,
        createdAt = "2026-04-23T11:00:00Z",
        status = status,
        companionTurnMeta = CompanionTurnMeta(
            turnId = "turn-failed-1",
            variantGroupId = "vg-failed-1",
            variantIndex = 0,
            providerId = "openai",
            model = "gpt-4o-mini",
            failedSubtypeKey = failedSubtypeKey,
        ),
    )
}
