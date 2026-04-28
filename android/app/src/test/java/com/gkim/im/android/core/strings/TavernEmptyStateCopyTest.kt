package com.gkim.im.android.core.strings

import com.gkim.im.android.core.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernEmptyStateCopyTest {

    private val surfaces = listOf(
        CompanionStrings.MessagesListEmpty,
        CompanionStrings.ContactSearchNoResults,
        CompanionStrings.PostRelationshipReset,
    )

    @Test
    fun `every copy is bilingual and non-blank`() {
        surfaces.forEach { copy ->
            assertTrue("english blank: ${copy.english}", copy.english.isNotBlank())
            assertTrue("chinese blank: ${copy.chinese}", copy.chinese.isNotBlank())
            assertNotEquals(
                "english must differ from chinese for $copy",
                copy.english,
                copy.chinese,
            )
        }
    }

    @Test
    fun `messages list copy is in-character and drops the IM-residue phrasing`() {
        val copy = CompanionStrings.MessagesListEmpty
        // Must not regress to the pre-redesign phrasing.
        assertTrue("regressed to '还没有活跃会话'", "还没有活跃会话" !in copy.chinese)
        assertTrue("regressed to '登录后开始'", "登录后" !in copy.chinese)
        assertTrue("regressed to 'sign in'", !copy.english.contains("sign in", ignoreCase = true))
        assertTrue("regressed to 'active conversations'", !copy.english.contains("active conversations", ignoreCase = true))
        // Must use the cocktail-bar metaphor.
        assertTrue("expected '酒馆' in chinese", "酒馆" in copy.chinese)
        assertTrue("expected 'bar' in english", copy.english.contains("bar", ignoreCase = true))
    }

    @Test
    fun `contact search no-results uses tavern-guest metaphor`() {
        val copy = CompanionStrings.ContactSearchNoResults
        assertTrue("expected '今晚' or '客人' in chinese", "今晚" in copy.chinese || "客人" in copy.chinese)
        assertTrue(
            "expected 'tonight' or 'walked in' in english",
            copy.english.contains("tonight", ignoreCase = true) ||
                copy.english.contains("walked in", ignoreCase = true),
        )
    }

    @Test
    fun `post-relationship-reset copy is in-character`() {
        val copy = CompanionStrings.PostRelationshipReset
        assertTrue("expected '座位' or '擦' in chinese", "座位" in copy.chinese || "擦" in copy.chinese)
        assertTrue(
            "expected 'seat' or 'wipe' in english",
            copy.english.contains("seat", ignoreCase = true) ||
                copy.english.contains("wipe", ignoreCase = true),
        )
    }

    @Test
    fun `pick resolves english accessor`() {
        surfaces.forEach { copy ->
            assertEquals(copy.english, copy.pick(AppLanguage.English))
        }
    }

    @Test
    fun `pick resolves chinese accessor`() {
        surfaces.forEach { copy ->
            assertEquals(copy.chinese, copy.pick(AppLanguage.Chinese))
        }
    }
}
