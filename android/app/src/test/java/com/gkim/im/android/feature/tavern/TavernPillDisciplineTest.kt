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
    fun `settings is demoted to a rectangular container`() {
        val settings = TavernHeaderActions.single { it.testTag == "tavern-settings-trigger" }
        assertEquals(HeaderActionKind.Rectangle, settings.kind)
    }

    @Test
    fun `import-card is demoted to a rectangular container`() {
        val importCard = TavernHeaderActions.single { it.testTag == "tavern-import-entry" }
        assertEquals(HeaderActionKind.Rectangle, importCard.kind)
    }

    @Test
    fun `header action manifest covers the three R1 trio`() {
        val tags = TavernHeaderActions.map { it.testTag }.toSet()
        assertEquals(
            setOf("tavern-settings-trigger", "tavern-draw-trigger", "tavern-import-entry"),
            tags,
        )
    }
}
