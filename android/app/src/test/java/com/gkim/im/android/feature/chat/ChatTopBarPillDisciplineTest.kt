package com.gkim.im.android.feature.chat

import com.gkim.im.android.feature.tavern.HeaderActionKind
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatTopBarPillDisciplineTest {

    @Test
    fun `chat top-bar declares exactly one pill`() {
        val pills = ChatTopBarHeaderActions.filter { it.kind == HeaderActionKind.Pill }
        assertEquals("Chat top-bar must have exactly one pill", 1, pills.size)
    }

    @Test
    fun `the sole pill is the persona selector`() {
        val pill = ChatTopBarHeaderActions.single { it.kind == HeaderActionKind.Pill }
        assertEquals("chat-persona-pill", pill.testTag)
    }

    @Test
    fun `overflow trigger is rectangular`() {
        val overflow = ChatTopBarHeaderActions.single { it.testTag == "chat-top-overflow-trigger" }
        assertEquals(HeaderActionKind.Rectangle, overflow.kind)
    }

    @Test
    fun `header manifest does not include the dropdown items`() {
        // 导出对话 and 设置 dropdown items are plain text-with-icon rows; they
        // are intentionally omitted from the pill-discipline manifest because
        // they have no container shape to discipline.
        val tags = ChatTopBarHeaderActions.map { it.testTag }.toSet()
        assert("chat-top-overflow-export" !in tags)
        assert("chat-top-overflow-settings" !in tags)
    }
}
