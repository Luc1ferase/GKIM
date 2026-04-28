package com.gkim.im.android.data.repository

import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * §2.1 verification — `DefaultCompanionTurnRepository.resolveActivePath` projects each variant
 * group's sibling count + active index onto the rendered `ChatMessage.companionTurnMeta`'s
 * `siblingCount` + `siblingActiveIndex` fields. Without this projection the §3.1 chevron
 * rendering path never triggers in production because `companionTurnMeta` ships with the
 * data-class defaults `siblingCount = 1 / siblingActiveIndex = 0`.
 */
class CompanionTurnRepositorySiblingProjectionTest {

    private fun record(
        turnId: String,
        messageId: String = turnId,
        variantGroupId: String,
        variantIndex: Int,
        parentMessageId: String? = null,
        body: String = "",
        status: String = "completed",
    ): CompanionTurnRecordDto = CompanionTurnRecordDto(
        turnId = turnId,
        conversationId = "conv-1",
        messageId = messageId,
        variantGroupId = variantGroupId,
        variantIndex = variantIndex,
        parentMessageId = parentMessageId,
        status = status,
        accumulatedBody = body,
        lastDeltaSeq = 0,
        startedAt = "2026-04-26T12:00:00Z",
        completedAt = if (status == "completed") "2026-04-26T12:00:01Z" else null,
    )

    @Test
    fun `single-sibling group projects siblingCount = 1`() {
        val repo = DefaultCompanionTurnRepository()
        repo.applyRecord(
            record(
                turnId = "t-1",
                variantGroupId = "vg-1",
                variantIndex = 0,
                body = "first reply",
            ),
        )

        val activePath = repo.activePathByConversation.value["conv-1"].orEmpty()
        assertEquals(1, activePath.size)
        val meta = activePath[0].companionTurnMeta
        assertNotNull(meta)
        assertEquals(1, meta!!.siblingCount)
        assertEquals(0, meta.siblingActiveIndex)
    }

    @Test
    fun `two-sibling group after regenerate projects siblingCount = 2 with active index advanced`() {
        val repo = DefaultCompanionTurnRepository()
        // Seed sibling 0.
        repo.applyRecord(
            record(turnId = "t-1a", variantGroupId = "vg-1", variantIndex = 0, body = "first"),
        )
        // Append regenerated sibling 1 (same variantGroupId, next variantIndex).
        repo.applyRecord(
            record(turnId = "t-1b", variantGroupId = "vg-1", variantIndex = 1, body = "regen"),
        )

        val activePath = repo.activePathByConversation.value["conv-1"].orEmpty()
        assertEquals(1, activePath.size) // single active variant rendered (the second sibling)
        val meta = activePath[0].companionTurnMeta!!
        assertEquals(2, meta.siblingCount)
        assertEquals(1, meta.siblingActiveIndex)
        // Body of the active variant is the regenerated one.
        assertEquals("regen", activePath[0].body)
    }

    @Test
    fun `three-sibling group projects siblingCount = 3 with the latest sibling active`() {
        val repo = DefaultCompanionTurnRepository()
        repo.applyRecord(record(turnId = "t-a", variantGroupId = "vg-1", variantIndex = 0, body = "a"))
        repo.applyRecord(record(turnId = "t-b", variantGroupId = "vg-1", variantIndex = 1, body = "b"))
        repo.applyRecord(record(turnId = "t-c", variantGroupId = "vg-1", variantIndex = 2, body = "c"))

        val activePath = repo.activePathByConversation.value["conv-1"].orEmpty()
        assertEquals(1, activePath.size)
        val meta = activePath[0].companionTurnMeta!!
        assertEquals(3, meta.siblingCount)
        assertEquals(2, meta.siblingActiveIndex)
        assertEquals("c", activePath[0].body)
    }

    @Test
    fun `selectVariant rolling back to the original sibling re-projects siblingActiveIndex = 0`() {
        val repo = DefaultCompanionTurnRepository()
        repo.applyRecord(record(turnId = "t-a", variantGroupId = "vg-1", variantIndex = 0, body = "first"))
        repo.applyRecord(record(turnId = "t-b", variantGroupId = "vg-1", variantIndex = 1, body = "regen"))
        // After regenerate, active is 1 ("regen"). Roll back to the original via selectVariant.
        repo.selectVariant(turnId = "t-a", variantIndex = 0)

        val activePath = repo.activePathByConversation.value["conv-1"].orEmpty()
        assertEquals(1, activePath.size)
        val meta = activePath[0].companionTurnMeta!!
        assertEquals(2, meta.siblingCount)
        assertEquals(0, meta.siblingActiveIndex)
        assertEquals("first", activePath[0].body)
    }

    @Test
    fun `independent variantGroups in the same conversation each get their own projection`() {
        val repo = DefaultCompanionTurnRepository()
        // First variantGroup: 2 siblings.
        repo.applyRecord(record(turnId = "t-1a", variantGroupId = "vg-1", variantIndex = 0, body = "u1-r1"))
        repo.applyRecord(record(turnId = "t-1b", variantGroupId = "vg-1", variantIndex = 1, body = "u1-r2"))
        // Second variantGroup (different parent): 1 sibling.
        repo.applyRecord(
            record(
                turnId = "t-2",
                variantGroupId = "vg-2",
                variantIndex = 0,
                parentMessageId = "t-1b",
                body = "u2-r1",
            ),
        )

        val activePath = repo.activePathByConversation.value["conv-1"].orEmpty()
        assertEquals(2, activePath.size)
        // First message belongs to vg-1: siblingCount = 2, active = 1
        val firstMeta = activePath[0].companionTurnMeta!!
        assertEquals("vg-1", firstMeta.variantGroupId)
        assertEquals(2, firstMeta.siblingCount)
        assertEquals(1, firstMeta.siblingActiveIndex)
        // Second message belongs to vg-2: siblingCount = 1
        val secondMeta = activePath[1].companionTurnMeta!!
        assertEquals("vg-2", secondMeta.variantGroupId)
        assertEquals(1, secondMeta.siblingCount)
        assertEquals(0, secondMeta.siblingActiveIndex)
    }
}
