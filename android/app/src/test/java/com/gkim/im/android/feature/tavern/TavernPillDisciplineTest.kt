package com.gkim.im.android.feature.tavern

import org.junit.Assert.assertEquals
import org.junit.Test

class TavernPillDisciplineTest {

    @Test
    fun `tavern home declares exactly one pill`() {
        val pills = TavernHeaderActions.filter { it.kind == HeaderActionKind.Pill }
        assertEquals("Tavern home must have exactly one pill", 1, pills.size)
    }

    @Test
    fun `the sole pill is the draw companion-card action`() {
        val pill = TavernHeaderActions.single { it.kind == HeaderActionKind.Pill }
        assertEquals("tavern-draw-trigger", pill.testTag)
    }

    @Test
    fun `create is rendered as a rectangular container`() {
        val create = TavernHeaderActions.single { it.testTag == "tavern-create-trigger" }
        assertEquals(HeaderActionKind.Rectangle, create.kind)
    }

    @Test
    fun `import-card is demoted to a rectangular container`() {
        val importCard = TavernHeaderActions.single { it.testTag == "tavern-import-entry" }
        assertEquals(HeaderActionKind.Rectangle, importCard.kind)
    }

    @Test
    fun `header action manifest covers the three-action row trio`() {
        val tags = TavernHeaderActions.map { it.testTag }.toSet()
        assertEquals(
            setOf("tavern-create-trigger", "tavern-draw-trigger", "tavern-import-entry"),
            tags,
        )
    }
}
