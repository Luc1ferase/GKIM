package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.MessageStatus
import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionTurnRepositoryTest {
    private val conversationId = "conversation-1"

    private fun userMessage(id: String, parent: String? = null): ChatMessage = ChatMessage(
        id = id,
        direction = MessageDirection.Outgoing,
        kind = MessageKind.Text,
        body = "Hello",
        createdAt = "2026-04-21T08:00:00Z",
        parentMessageId = parent,
    )

    @Test
    fun `submit drives thinking to streaming to completed`() {
        val repo = DefaultCompanionTurnRepository()
        repo.recordUserTurn(userMessage("user-1"), conversationId)

        repo.handleTurnStarted(
            ImGatewayEvent.CompanionTurnStarted(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                variantGroupId = "vg-1",
                variantIndex = 0,
                parentMessageId = "user-1",
            )
        )

        val afterStart = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals(2, afterStart.size)
        assertEquals(MessageStatus.Thinking, afterStart.last().status)

        repo.handleTurnDelta(
            ImGatewayEvent.CompanionTurnDelta(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                deltaSeq = 0,
                textDelta = "Hello ",
            )
        )
        repo.handleTurnDelta(
            ImGatewayEvent.CompanionTurnDelta(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                deltaSeq = 1,
                textDelta = "there",
            )
        )

        val afterStream = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals(MessageStatus.Streaming, afterStream.last().status)
        assertEquals("Hello there", afterStream.last().body)

        repo.handleTurnCompleted(
            ImGatewayEvent.CompanionTurnCompleted(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                finalBody = "Hello there, friend.",
                completedAt = "2026-04-21T08:00:05Z",
            )
        )

        val afterComplete = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals(MessageStatus.Completed, afterComplete.last().status)
        assertEquals("Hello there, friend.", afterComplete.last().body)
        assertTrue(afterComplete.last().companionTurnMeta?.canRegenerate == true)
    }

    @Test
    fun `regenerate appends sibling and becomes active`() {
        val repo = DefaultCompanionTurnRepository()
        repo.recordUserTurn(userMessage("user-1"), conversationId)

        repo.handleTurnStarted(
            ImGatewayEvent.CompanionTurnStarted(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                variantGroupId = "vg-1",
                variantIndex = 0,
            )
        )
        repo.handleTurnCompleted(
            ImGatewayEvent.CompanionTurnCompleted(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                finalBody = "first reply",
                completedAt = "2026-04-21T08:00:05Z",
            )
        )
        repo.handleTurnStarted(
            ImGatewayEvent.CompanionTurnStarted(
                turnId = "turn-2",
                conversationId = conversationId,
                messageId = "companion-2",
                variantGroupId = "vg-1",
                variantIndex = 1,
            )
        )

        val tree = repo.treeByConversation.value[conversationId]!!
        val group = tree.variantGroups["vg-1"]!!
        assertEquals(listOf("companion-1", "companion-2"), group.siblingMessageIds)
        assertEquals(1, group.activeIndex)

        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals("companion-2", path.last().id)
        assertEquals(MessageStatus.Thinking, path.last().status)
    }

    @Test
    fun `selectVariant switches active path between siblings`() {
        val repo = DefaultCompanionTurnRepository()
        repo.recordUserTurn(userMessage("user-1"), conversationId)
        repo.handleTurnStarted(
            ImGatewayEvent.CompanionTurnStarted(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                variantGroupId = "vg-1",
                variantIndex = 0,
            )
        )
        repo.handleTurnCompleted(
            ImGatewayEvent.CompanionTurnCompleted(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                finalBody = "first",
                completedAt = "2026-04-21T08:00:05Z",
            )
        )
        repo.handleTurnStarted(
            ImGatewayEvent.CompanionTurnStarted(
                turnId = "turn-2",
                conversationId = conversationId,
                messageId = "companion-2",
                variantGroupId = "vg-1",
                variantIndex = 1,
            )
        )
        repo.handleTurnCompleted(
            ImGatewayEvent.CompanionTurnCompleted(
                turnId = "turn-2",
                conversationId = conversationId,
                messageId = "companion-2",
                finalBody = "second",
                completedAt = "2026-04-21T08:00:10Z",
            )
        )

        assertEquals(
            "second",
            repo.activePathByConversation.value[conversationId]?.last()?.body,
        )

        repo.selectVariant(turnId = "turn-1", variantIndex = 0)

        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals("first", path.last().body)
        assertEquals("companion-1", path.last().id)
    }

    @Test
    fun `failed event promotes timeout subtype to MessageStatus Timeout`() {
        val repo = DefaultCompanionTurnRepository()
        repo.handleTurnStarted(
            ImGatewayEvent.CompanionTurnStarted(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                variantGroupId = "vg-1",
                variantIndex = 0,
            )
        )
        repo.handleTurnFailed(
            ImGatewayEvent.CompanionTurnFailed(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                subtype = "timeout",
                errorMessage = "idle for 15s",
            )
        )

        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals(MessageStatus.Timeout, path.last().status)
        assertEquals("idle for 15s", path.last().body)
        assertTrue(path.last().companionTurnMeta?.canRegenerate == true)
    }

    @Test
    fun `failed event with non-timeout subtype produces Failed status`() {
        val repo = DefaultCompanionTurnRepository()
        repo.handleTurnStarted(
            ImGatewayEvent.CompanionTurnStarted(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                variantGroupId = "vg-1",
                variantIndex = 0,
            )
        )
        repo.handleTurnFailed(
            ImGatewayEvent.CompanionTurnFailed(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                subtype = "provider_error",
                errorMessage = "provider 500",
            )
        )

        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals(MessageStatus.Failed, path.last().status)
    }

    @Test
    fun `blocked event carries reason into body and disables regenerate`() {
        val repo = DefaultCompanionTurnRepository()
        repo.handleTurnStarted(
            ImGatewayEvent.CompanionTurnStarted(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                variantGroupId = "vg-1",
                variantIndex = 0,
            )
        )
        repo.handleTurnBlocked(
            ImGatewayEvent.CompanionTurnBlocked(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                reason = "nsfw_denied",
            )
        )

        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals(MessageStatus.Blocked, path.last().status)
        assertEquals("nsfw_denied", path.last().body)
        assertTrue(path.last().companionTurnMeta?.canRegenerate == false)
    }

    @Test
    fun `reducer ignores duplicate deltas with non-monotonic deltaSeq`() {
        val repo = DefaultCompanionTurnRepository()
        repo.handleTurnStarted(
            ImGatewayEvent.CompanionTurnStarted(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                variantGroupId = "vg-1",
                variantIndex = 0,
            )
        )
        repo.handleTurnDelta(
            ImGatewayEvent.CompanionTurnDelta(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                deltaSeq = 0,
                textDelta = "hello",
            )
        )
        repo.handleTurnDelta(
            ImGatewayEvent.CompanionTurnDelta(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                deltaSeq = 0,
                textDelta = "hello",
            )
        )
        repo.handleTurnDelta(
            ImGatewayEvent.CompanionTurnDelta(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                deltaSeq = 1,
                textDelta = " world",
            )
        )
        repo.handleTurnDelta(
            ImGatewayEvent.CompanionTurnDelta(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                deltaSeq = 1,
                textDelta = " world",
            )
        )

        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals("hello world", path.last().body)
    }

    @Test
    fun `applyRecord rehydrates mid-streaming turn from snapshot`() {
        val repo = DefaultCompanionTurnRepository()
        repo.applyRecord(
            CompanionTurnRecordDto(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                variantGroupId = "vg-1",
                variantIndex = 0,
                parentMessageId = "user-1",
                status = "streaming",
                accumulatedBody = "partial",
                lastDeltaSeq = 4,
                startedAt = "2026-04-21T08:00:00Z",
            )
        )
        val after = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals(1, after.size)
        assertEquals(MessageStatus.Streaming, after.last().status)
        assertEquals("partial", after.last().body)

        repo.handleTurnDelta(
            ImGatewayEvent.CompanionTurnDelta(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                deltaSeq = 4,
                textDelta = " - dropped duplicate",
            )
        )
        assertEquals(
            "partial",
            repo.activePathByConversation.value[conversationId]?.last()?.body,
        )

        repo.handleTurnDelta(
            ImGatewayEvent.CompanionTurnDelta(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                deltaSeq = 5,
                textDelta = " more",
            )
        )
        assertEquals(
            "partial more",
            repo.activePathByConversation.value[conversationId]?.last()?.body,
        )
    }

    @Test
    fun `applyRecord is idempotent across repeated snapshots`() {
        val repo = DefaultCompanionTurnRepository()
        val record = CompanionTurnRecordDto(
            turnId = "turn-1",
            conversationId = conversationId,
            messageId = "companion-1",
            variantGroupId = "vg-1",
            variantIndex = 0,
            status = "completed",
            accumulatedBody = "final body",
            lastDeltaSeq = 7,
            startedAt = "2026-04-21T08:00:00Z",
            completedAt = "2026-04-21T08:00:05Z",
        )
        repo.applyRecord(record)
        repo.applyRecord(record)
        repo.applyRecord(record)

        val tree = repo.treeByConversation.value[conversationId]!!
        val group = tree.variantGroups["vg-1"]!!
        assertEquals(listOf("companion-1"), group.siblingMessageIds)
        assertEquals(1, tree.orderedMessageIds.size)
    }

    @Test
    fun `selectVariant with out-of-range index is a no-op`() {
        val repo = DefaultCompanionTurnRepository()
        repo.handleTurnStarted(
            ImGatewayEvent.CompanionTurnStarted(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                variantGroupId = "vg-1",
                variantIndex = 0,
            )
        )
        repo.selectVariant("turn-1", variantIndex = 5)
        val group = repo.treeByConversation.value[conversationId]!!.variantGroups["vg-1"]!!
        assertEquals(0, group.activeIndex)
    }

    @Test
    fun `selectVariant with unknown turnId is a no-op`() {
        val repo = DefaultCompanionTurnRepository()
        repo.selectVariant("unknown-turn", variantIndex = 0)
        assertTrue(repo.treeByConversation.value.isEmpty())
    }

    @Test
    fun `recordUserTurn is idempotent on same user message id`() {
        val repo = DefaultCompanionTurnRepository()
        repo.recordUserTurn(userMessage("user-1"), conversationId)
        repo.recordUserTurn(userMessage("user-1"), conversationId)
        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals(1, path.size)
    }

    @Test
    fun `user messages are not reframed as variants and stay on the active path`() {
        val repo = DefaultCompanionTurnRepository()
        repo.recordUserTurn(userMessage("user-1"), conversationId)
        repo.handleTurnStarted(
            ImGatewayEvent.CompanionTurnStarted(
                turnId = "turn-1",
                conversationId = conversationId,
                messageId = "companion-1",
                variantGroupId = "vg-1",
                variantIndex = 0,
            )
        )
        val path = repo.activePathByConversation.value[conversationId].orEmpty()
        assertEquals("user-1", path.first().id)
        assertNull(path.first().companionTurnMeta)
        assertNotNull(path.last().companionTurnMeta)
    }
}
