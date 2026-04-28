package com.gkim.im.android.feature.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavTwoTabContractTest {

    @Test
    fun `bottom nav exposes exactly two tabs`() {
        assertEquals(2, RootBottomNavTabs.size)
    }

    @Test
    fun `tavern is the first tab`() {
        assertEquals("tavern", RootBottomNavTabs[0].route)
    }

    @Test
    fun `messages is the second tab`() {
        assertEquals("messages", RootBottomNavTabs[1].route)
    }

    @Test
    fun `contacts tab is no longer in primary bottom nav`() {
        // R2.2 explicit anti-regression: the prior IM-residue surface had
        // 消息 / 联系人 / 酒馆 across three tabs; the contacts tab folds into
        // Tavern as the "All companions" section. The contacts route still
        // exists in the NavHost (deep links / legacy callers) but MUST NOT
        // appear as a primary bottom-nav tab.
        val routes = RootBottomNavTabs.map { it.route }
        assertTrue("contacts must not appear in primary bottom nav: $routes", "contacts" !in routes)
    }

    @Test
    fun `every tab has a non-blank bilingual label pair`() {
        RootBottomNavTabs.forEach { tab ->
            assertTrue("english label blank for ${tab.route}", tab.englishLabel.isNotBlank())
            assertTrue("chinese label blank for ${tab.route}", tab.chineseLabel.isNotBlank())
            assertTrue(
                "english must differ from chinese for ${tab.route}",
                tab.englishLabel != tab.chineseLabel,
            )
        }
    }

    @Test
    fun `tavern tab carries Tavern English label and 酒馆 Chinese label`() {
        val tavern = RootBottomNavTabs.first { it.route == "tavern" }
        assertEquals("Tavern", tavern.englishLabel)
        assertEquals("酒馆", tavern.chineseLabel)
    }

    @Test
    fun `messages tab carries Messages English label and 消息 Chinese label`() {
        val messages = RootBottomNavTabs.first { it.route == "messages" }
        assertEquals("Messages", messages.englishLabel)
        assertEquals("消息", messages.chineseLabel)
    }
}
