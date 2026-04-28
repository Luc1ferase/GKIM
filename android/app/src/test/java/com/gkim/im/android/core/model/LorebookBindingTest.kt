package com.gkim.im.android.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LorebookBindingTest {

    private val binding = LorebookBinding(
        lorebookId = "lorebook-atlas",
        characterId = "card-aria",
        isPrimary = true,
    )

    @Test
    fun dataClassEqualityConsidersEveryField() {
        val twin = binding.copy()
        assertEquals(binding, twin)

        val reassigned = binding.copy(characterId = "card-nova")
        assertNotEquals(binding, reassigned)

        val demoted = binding.copy(isPrimary = false)
        assertNotEquals(binding, demoted)
    }

    @Test
    fun defaultIsPrimaryIsFalse() {
        val secondary = LorebookBinding(lorebookId = "lb-1", characterId = "card-1")
        assertEquals(false, secondary.isPrimary)
    }

    @Test
    fun primaryForReturnsTheBindingMarkedPrimary() {
        val bindings = listOf(
            LorebookBinding("lb-a", "card-aria", isPrimary = false),
            LorebookBinding("lb-b", "card-aria", isPrimary = true),
            LorebookBinding("lb-c", "card-nova", isPrimary = true),
        )
        assertEquals("lb-b", bindings.primaryFor("card-aria")?.lorebookId)
        assertEquals("lb-c", bindings.primaryFor("card-nova")?.lorebookId)
    }

    @Test
    fun primaryForReturnsNullWhenNoPrimaryExists() {
        val bindings = listOf(
            LorebookBinding("lb-a", "card-aria", isPrimary = false),
            LorebookBinding("lb-b", "card-aria", isPrimary = false),
        )
        assertNull(bindings.primaryFor("card-aria"))
    }

    @Test
    fun primaryForReturnsNullWhenCharacterHasNoBindings() {
        val bindings = listOf(
            LorebookBinding("lb-a", "card-aria", isPrimary = true),
        )
        assertNull(bindings.primaryFor("card-mystery"))
    }

    @Test
    fun lorebookIdsBoundToCollectsEveryBindingForCharacter() {
        val bindings = listOf(
            LorebookBinding("lb-a", "card-aria", isPrimary = true),
            LorebookBinding("lb-b", "card-aria", isPrimary = false),
            LorebookBinding("lb-c", "card-nova", isPrimary = true),
        )
        val bound = bindings.lorebookIdsBoundTo("card-aria")
        assertEquals(listOf("lb-a", "lb-b"), bound)
        assertTrue(bindings.lorebookIdsBoundTo("card-mystery").isEmpty())
    }
}
