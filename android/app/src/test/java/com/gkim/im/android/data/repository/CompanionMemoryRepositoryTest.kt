package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.CompanionMemory
import com.gkim.im.android.core.model.CompanionMemoryPin
import com.gkim.im.android.core.model.CompanionMemoryResetScope
import com.gkim.im.android.core.model.LocalizedText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionMemoryRepositoryTest {

    private val cardId = "card-aria"

    private fun pin(
        id: String,
        english: String,
        chinese: String,
        sourceMessageId: String? = null,
        createdAt: Long = 1_000L,
    ): CompanionMemoryPin = CompanionMemoryPin(
        id = id,
        sourceMessageId = sourceMessageId,
        text = LocalizedText(english, chinese),
        createdAt = createdAt,
        pinnedByUser = true,
    )

    private fun memorySnapshot(
        summary: LocalizedText = LocalizedText("base", "基础"),
        summaryUpdatedAt: Long = 500L,
        summaryTurnCursor: Int = 7,
        pins: List<CompanionMemoryPin> = emptyList(),
    ): CompanionMemorySnapshot = CompanionMemorySnapshot(
        memory = CompanionMemory(
            userId = "user-nox",
            companionCardId = cardId,
            summary = summary,
            summaryUpdatedAt = summaryUpdatedAt,
            summaryTurnCursor = summaryTurnCursor,
        ),
        pins = pins,
    )

    private fun repositoryWithSnapshot(
        snapshot: CompanionMemorySnapshot,
        idStart: Int = 1,
        clockTicks: LongArray = longArrayOf(2_000L),
    ): DefaultCompanionMemoryRepository {
        var counter = idStart
        var tick = 0
        return DefaultCompanionMemoryRepository(
            initial = mapOf(cardId to snapshot),
            idGenerator = { "pin-generated-${counter++}" },
            clock = {
                val next = clockTicks[tick.coerceAtMost(clockTicks.lastIndex)]
                tick++
                next
            },
        )
    }

    @Test
    fun `createPin assigns id and pinnedByUser true and preserves prior pins`() = runBlocking {
        val existing = pin(id = "pin-seed", english = "first", chinese = "第一", createdAt = 1_000L)
        val repo = repositoryWithSnapshot(
            memorySnapshot(pins = listOf(existing)),
            clockTicks = longArrayOf(2_000L),
        )

        val created = repo.createPin(
            cardId = cardId,
            sourceMessageId = "message-9",
            text = LocalizedText("second", "第二"),
        )

        assertEquals("pin-generated-1", created.id)
        assertEquals("message-9", created.sourceMessageId)
        assertEquals(2_000L, created.createdAt)
        assertTrue(created.pinnedByUser)

        val pins = repo.observePins(cardId).first()
        assertEquals(listOf("pin-seed", "pin-generated-1"), pins.map { it.id })
    }

    @Test
    fun `updatePin swaps text but preserves createdAt order stable`() = runBlocking {
        val a = pin(id = "pin-a", english = "A", chinese = "甲", createdAt = 1_000L)
        val b = pin(id = "pin-b", english = "B", chinese = "乙", createdAt = 2_000L)
        val c = pin(id = "pin-c", english = "C", chinese = "丙", createdAt = 3_000L)
        val repo = repositoryWithSnapshot(memorySnapshot(pins = listOf(a, b, c)))

        val updated = repo.updatePin(
            pinId = "pin-b",
            text = LocalizedText("B'", "乙撇"),
        )

        assertNotNull(updated)
        assertEquals(LocalizedText("B'", "乙撇"), updated!!.text)
        assertEquals("createdAt on updated pin is preserved", 2_000L, updated.createdAt)

        val pins = repo.observePins(cardId).first()
        assertEquals(listOf("pin-a", "pin-b", "pin-c"), pins.map { it.id })
        assertEquals(listOf(1_000L, 2_000L, 3_000L), pins.map { it.createdAt })
    }

    @Test
    fun `updatePin on unknown pin returns null and leaves state unchanged`() = runBlocking {
        val a = pin(id = "pin-a", english = "A", chinese = "甲", createdAt = 1_000L)
        val repo = repositoryWithSnapshot(memorySnapshot(pins = listOf(a)))

        val updated = repo.updatePin(pinId = "pin-missing", text = LocalizedText("X", "X"))

        assertNull(updated)
        assertEquals(listOf(a), repo.observePins(cardId).first())
    }

    @Test
    fun `deletePin removes the target and leaves siblings intact`() = runBlocking {
        val a = pin(id = "pin-a", english = "A", chinese = "甲", createdAt = 1_000L)
        val b = pin(id = "pin-b", english = "B", chinese = "乙", createdAt = 2_000L)
        val repo = repositoryWithSnapshot(memorySnapshot(pins = listOf(a, b)))

        val removed = repo.deletePin("pin-a")

        assertTrue(removed)
        val pins = repo.observePins(cardId).first()
        assertEquals(listOf("pin-b"), pins.map { it.id })
    }

    @Test
    fun `deletePin on unknown pin returns false`() = runBlocking {
        val repo = repositoryWithSnapshot(memorySnapshot())
        assertFalse(repo.deletePin("pin-nothing"))
    }

    @Test
    fun `reset pins scope removes pins but preserves summary`() = runBlocking {
        val a = pin(id = "pin-a", english = "A", chinese = "甲", createdAt = 1_000L)
        val repo = repositoryWithSnapshot(
            memorySnapshot(
                summary = LocalizedText("base", "基础"),
                summaryUpdatedAt = 500L,
                summaryTurnCursor = 7,
                pins = listOf(a),
            ),
        )

        repo.reset(cardId, CompanionMemoryResetScope.Pins)

        assertTrue(repo.observePins(cardId).first().isEmpty())
        val memory = repo.observeMemory(cardId).first()
        assertNotNull(memory)
        assertEquals(LocalizedText("base", "基础"), memory!!.summary)
        assertEquals(500L, memory.summaryUpdatedAt)
        assertEquals(7, memory.summaryTurnCursor)
    }

    @Test
    fun `reset summary scope wipes summary + cursor but preserves pins`() = runBlocking {
        val a = pin(id = "pin-a", english = "A", chinese = "甲", createdAt = 1_000L)
        val repo = repositoryWithSnapshot(
            memorySnapshot(
                summary = LocalizedText("base", "基础"),
                summaryUpdatedAt = 500L,
                summaryTurnCursor = 7,
                pins = listOf(a),
            ),
        )

        repo.reset(cardId, CompanionMemoryResetScope.Summary)

        val memory = repo.observeMemory(cardId).first()
        assertNotNull(memory)
        assertEquals(LocalizedText.Empty, memory!!.summary)
        assertEquals(0L, memory.summaryUpdatedAt)
        assertEquals(0, memory.summaryTurnCursor)
        assertEquals(listOf(a), repo.observePins(cardId).first())
    }

    @Test
    fun `reset all scope wipes summary + pins while keeping the memory row addressable`() = runBlocking {
        val a = pin(id = "pin-a", english = "A", chinese = "甲", createdAt = 1_000L)
        val repo = repositoryWithSnapshot(
            memorySnapshot(
                summary = LocalizedText("base", "基础"),
                summaryUpdatedAt = 500L,
                summaryTurnCursor = 7,
                pins = listOf(a),
            ),
        )

        repo.reset(cardId, CompanionMemoryResetScope.All)

        val memory = repo.observeMemory(cardId).first()
        assertNotNull(memory)
        assertEquals(LocalizedText.Empty, memory!!.summary)
        assertEquals(0L, memory.summaryUpdatedAt)
        assertEquals(0, memory.summaryTurnCursor)
        assertTrue(repo.observePins(cardId).first().isEmpty())
    }

    @Test
    fun `observer continuity across refresh — refresh does not drop subscribers or state`() = runBlocking {
        val a = pin(id = "pin-a", english = "A", chinese = "甲", createdAt = 1_000L)
        val repo = repositoryWithSnapshot(memorySnapshot(pins = listOf(a)))

        val memoryBefore = repo.observeMemory(cardId).first()
        val pinsBefore = repo.observePins(cardId).first()

        repo.refresh(cardId)

        val memoryAfter = repo.observeMemory(cardId).first()
        val pinsAfter = repo.observePins(cardId).first()

        assertEquals(memoryBefore, memoryAfter)
        assertEquals(pinsBefore, pinsAfter)
    }

    @Test
    fun `refresh is idempotent — calling twice does not mutate state`() = runBlocking {
        val a = pin(id = "pin-a", english = "A", chinese = "甲", createdAt = 1_000L)
        val repo = repositoryWithSnapshot(memorySnapshot(pins = listOf(a)))

        repo.refresh(cardId)
        val afterFirst = repo.observePins(cardId).first()
        repo.refresh(cardId)
        val afterSecond = repo.observePins(cardId).first()

        assertEquals(afterFirst, afterSecond)
    }

    @Test
    fun `observeMemory returns null before any snapshot is set`() = runBlocking {
        val repo = DefaultCompanionMemoryRepository()
        assertNull(repo.observeMemory("card-empty").first())
        assertTrue(repo.observePins("card-empty").first().isEmpty())
    }

    @Test
    fun `createPin on a fresh card initializes pin list without a memory row`() = runBlocking {
        val repo = DefaultCompanionMemoryRepository(
            idGenerator = { "pin-fresh-1" },
            clock = { 500L },
        )

        val created = repo.createPin(
            cardId = "card-fresh",
            sourceMessageId = null,
            text = LocalizedText("Hi", "嗨"),
        )

        assertEquals("pin-fresh-1", created.id)
        assertNull(
            "memory row remains null until refresh populates it",
            repo.observeMemory("card-fresh").first(),
        )
        assertEquals(listOf(created), repo.observePins("card-fresh").first())
    }
}
