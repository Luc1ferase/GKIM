package com.gkim.im.android.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompanionMemoryModelsTest {

    private val memory = CompanionMemory(
        userId = "user-alpha",
        companionCardId = "architect-oracle",
        summary = LocalizedText(
            english = "The user introduced themselves as a curious traveller.",
            chinese = "用户自述为一名好奇的旅人。",
        ),
        summaryUpdatedAt = 1_700_000_000L,
        summaryTurnCursor = 8,
        tokenBudgetHint = 6_000,
    )

    private val pin = CompanionMemoryPin(
        id = "pin-1",
        sourceMessageId = "msg-42",
        text = LocalizedText(english = "Allergic to peanuts.", chinese = "对花生过敏。"),
        createdAt = 1_700_000_100L,
        pinnedByUser = true,
    )

    @Test
    fun companionMemoryDefaultsMatchSpec() {
        val bare = CompanionMemory(userId = "u", companionCardId = "c")
        assertEquals(LocalizedText.Empty, bare.summary)
        assertEquals(0L, bare.summaryUpdatedAt)
        assertEquals(0, bare.summaryTurnCursor)
        assertNull(bare.tokenBudgetHint)
    }

    @Test
    fun companionMemoryEqualityConsidersEveryField() {
        val twin = memory.copy()
        assertEquals(memory, twin)

        assertNotEquals(memory, memory.copy(userId = "user-beta"))
        assertNotEquals(memory, memory.copy(companionCardId = "other"))
        assertNotEquals(memory, memory.copy(summary = LocalizedText.of("new")))
        assertNotEquals(memory, memory.copy(summaryUpdatedAt = 2L))
        assertNotEquals(memory, memory.copy(summaryTurnCursor = 9))
        assertNotEquals(memory, memory.copy(tokenBudgetHint = 8_000))
        assertNotEquals(memory, memory.copy(tokenBudgetHint = null))
    }

    @Test
    fun companionMemoryTokenBudgetHintAcceptsNull() {
        val unhinted = memory.copy(tokenBudgetHint = null)
        assertNull(unhinted.tokenBudgetHint)
    }

    @Test
    fun companionMemoryPinDefaultsMatchSpec() {
        val bare = CompanionMemoryPin(
            id = "pin-minimal",
            text = LocalizedText.of("fact"),
        )
        assertNull(bare.sourceMessageId)
        assertEquals(0L, bare.createdAt)
        assertEquals(true, bare.pinnedByUser)
    }

    @Test
    fun companionMemoryPinEqualityConsidersEveryField() {
        val twin = pin.copy()
        assertEquals(pin, twin)

        assertNotEquals(pin, pin.copy(id = "pin-2"))
        assertNotEquals(pin, pin.copy(sourceMessageId = null))
        assertNotEquals(pin, pin.copy(sourceMessageId = "msg-99"))
        assertNotEquals(pin, pin.copy(text = LocalizedText.of("different")))
        assertNotEquals(pin, pin.copy(createdAt = 1L))
        assertNotEquals(pin, pin.copy(pinnedByUser = false))
    }

    @Test
    fun companionMemoryPinSourceMessageIdIsOptional() {
        val manual = pin.copy(sourceMessageId = null)
        assertNull(manual.sourceMessageId)

        val fromBubble = pin.copy(sourceMessageId = "msg-7")
        assertEquals("msg-7", fromBubble.sourceMessageId)
    }

    @Test
    fun companionMemoryResetScopeEnumeratesExactlyThreeValues() {
        val values = CompanionMemoryResetScope.entries
        assertEquals(3, values.size)
        assertEquals(
            listOf(
                CompanionMemoryResetScope.Pins,
                CompanionMemoryResetScope.Summary,
                CompanionMemoryResetScope.All,
            ),
            values,
        )
    }

    @Test
    fun companionMemoryResetScopeValueOfRoundTripsEveryVariant() {
        for (scope in CompanionMemoryResetScope.entries) {
            val parsed = CompanionMemoryResetScope.valueOf(scope.name)
            assertEquals(scope, parsed)
        }
    }
}
