package com.gkim.im.android.data.repository

import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * §2.2 verification — `CompanionTurnRepository.selectVariantByGroup(conversationId,
 * variantGroupId, newIndex)` mutates the matching variantGroup's `activeIndex` directly,
 * idempotently, and clamped to `[0, siblingMessageIds.size - 1]`. The §3.1 chevron callback
 * `onSelectVariantAt(variantGroupId, newIndex)` calls this method without a turnId hop.
 */
class CompanionTurnRepositorySelectVariantByGroupTest {

    private fun seededRepo(): DefaultCompanionTurnRepository {
        val repo = DefaultCompanionTurnRepository()
        // Three siblings under vg-1 in conv-1.
        repo.applyRecord(record(turnId = "t-a", variantGroupId = "vg-1", variantIndex = 0, body = "a"))
        repo.applyRecord(record(turnId = "t-b", variantGroupId = "vg-1", variantIndex = 1, body = "b"))
        repo.applyRecord(record(turnId = "t-c", variantGroupId = "vg-1", variantIndex = 2, body = "c"))
        return repo
    }

    private fun record(
        turnId: String,
        messageId: String = turnId,
        variantGroupId: String,
        variantIndex: Int,
        body: String = "",
    ): CompanionTurnRecordDto = CompanionTurnRecordDto(
        turnId = turnId,
        conversationId = "conv-1",
        messageId = messageId,
        variantGroupId = variantGroupId,
        variantIndex = variantIndex,
        status = "completed",
        accumulatedBody = body,
        lastDeltaSeq = 0,
        startedAt = "2026-04-26T12:00:00Z",
        completedAt = "2026-04-26T12:00:01Z",
    )

    private fun activePathBody(repo: DefaultCompanionTurnRepository): String =
        repo.activePathByConversation.value["conv-1"]!!.first().body

    private fun activeIndex(repo: DefaultCompanionTurnRepository): Int =
        repo.treeByConversation.value["conv-1"]!!.variantGroups["vg-1"]!!.activeIndex

    @Test
    fun `selectVariantByGroup flips the activeIndex to the requested value`() {
        val repo = seededRepo()
        // Initial state — most recently appended sibling is active (index 2).
        assertEquals(2, activeIndex(repo))
        assertEquals("c", activePathBody(repo))

        // Roll back to the original sibling.
        repo.selectVariantByGroup(conversationId = "conv-1", variantGroupId = "vg-1", newIndex = 0)
        assertEquals(0, activeIndex(repo))
        assertEquals("a", activePathBody(repo))
    }

    @Test
    fun `selectVariantByGroup is idempotent — calling with the current index does not mutate`() {
        val repo = seededRepo()
        val priorTreeRef = repo.treeByConversation.value["conv-1"]
        // Current activeIndex is 2 (the latest sibling).
        repo.selectVariantByGroup(conversationId = "conv-1", variantGroupId = "vg-1", newIndex = 2)
        // The mutateTree no-op short-circuit (`if (updated === existing) return`) means the
        // StateFlow reference stays identical when no change happens.
        val newTreeRef = repo.treeByConversation.value["conv-1"]
        assertEquals(priorTreeRef, newTreeRef)
        assertEquals(2, activeIndex(repo))
    }

    @Test
    fun `selectVariantByGroup clamps newIndex above the upper bound`() {
        val repo = seededRepo()
        repo.selectVariantByGroup(conversationId = "conv-1", variantGroupId = "vg-1", newIndex = 99)
        // 3 siblings → max index is 2; the clamp pins the activeIndex there.
        assertEquals(2, activeIndex(repo))
        assertEquals("c", activePathBody(repo))
    }

    @Test
    fun `selectVariantByGroup clamps newIndex below the lower bound`() {
        val repo = seededRepo()
        // First roll back to index 0 so the clamp test is observable (the rollback also
        // triggers a state change, distinguishing from the idempotent no-op above).
        repo.selectVariantByGroup(conversationId = "conv-1", variantGroupId = "vg-1", newIndex = 0)
        assertEquals(0, activeIndex(repo))
        // Now request a negative index — the clamp pins it to 0 (no change, idempotent).
        repo.selectVariantByGroup(conversationId = "conv-1", variantGroupId = "vg-1", newIndex = -7)
        assertEquals(0, activeIndex(repo))
    }

    @Test
    fun `selectVariantByGroup is a no-op for an unknown variantGroupId`() {
        val repo = seededRepo()
        val priorActive = activeIndex(repo)
        repo.selectVariantByGroup(conversationId = "conv-1", variantGroupId = "vg-unknown", newIndex = 1)
        assertEquals(priorActive, activeIndex(repo))
    }

    @Test
    fun `selectVariantByGroup is a no-op for an unknown conversationId`() {
        val repo = seededRepo()
        val priorActive = activeIndex(repo)
        repo.selectVariantByGroup(conversationId = "conv-unknown", variantGroupId = "vg-1", newIndex = 1)
        assertEquals(priorActive, activeIndex(repo))
    }

    @Test
    fun `mutation re-projects siblingActiveIndex via the sibling projection`() {
        val repo = seededRepo()
        // Roll back to sibling 1.
        repo.selectVariantByGroup(conversationId = "conv-1", variantGroupId = "vg-1", newIndex = 1)
        val activeMessage = repo.activePathByConversation.value["conv-1"]!!.first()
        val meta = activeMessage.companionTurnMeta!!
        // §2.1 projection now reflects the new active index in companionTurnMeta.
        assertEquals(3, meta.siblingCount)
        assertEquals(1, meta.siblingActiveIndex)
        assertEquals("b", activeMessage.body)
    }
}
