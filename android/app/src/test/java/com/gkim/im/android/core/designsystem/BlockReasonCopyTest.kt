package com.gkim.im.android.core.designsystem

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.BlockReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockReasonCopyTest {

    @Test
    fun `every enum value returns non-blank bilingual copy`() {
        BlockReason.entries.forEach { reason ->
            val copy = BlockReasonCopy.localizedCopy(reason)
            assertTrue("english copy blank for $reason", copy.english.isNotBlank())
            assertTrue("chinese copy blank for $reason", copy.chinese.isNotBlank())
        }
    }

    @Test
    fun `english and chinese are distinct strings for every reason`() {
        BlockReason.entries.forEach { reason ->
            val copy = BlockReasonCopy.localizedCopy(reason)
            assertTrue(
                "english must differ from chinese for $reason so the bilingual contract is real",
                copy.english != copy.chinese,
            )
        }
    }

    @Test
    fun `localizedCopy with English language resolves to english string`() {
        BlockReason.entries.forEach { reason ->
            val expected = BlockReasonCopy.localizedCopy(reason).english
            val actual = BlockReasonCopy.localizedCopy(reason, AppLanguage.English)
            assertEquals("English resolve for $reason", expected, actual)
        }
    }

    @Test
    fun `localizedCopy with Chinese language resolves to chinese string`() {
        BlockReason.entries.forEach { reason ->
            val expected = BlockReasonCopy.localizedCopy(reason).chinese
            val actual = BlockReasonCopy.localizedCopy(reason, AppLanguage.Chinese)
            assertEquals("Chinese resolve for $reason", expected, actual)
        }
    }

    @Test
    fun `self harm copy mentions local helpline in english`() {
        val copy = BlockReasonCopy.localizedCopy(BlockReason.SelfHarm)
        assertTrue(copy.english.contains("helpline", ignoreCase = true))
    }

    @Test
    fun `self harm copy mentions 心理援助热线 in chinese`() {
        val copy = BlockReasonCopy.localizedCopy(BlockReason.SelfHarm)
        assertTrue(copy.chinese.contains("心理援助热线"))
    }

    @Test
    fun `provider refusal copy references AI provider in english`() {
        val copy = BlockReasonCopy.localizedCopy(BlockReason.ProviderRefusal)
        assertTrue(copy.english.contains("provider", ignoreCase = true))
    }

    @Test
    fun `minor safety copy mentions minors in english and 未成年 in chinese`() {
        val copy = BlockReasonCopy.localizedCopy(BlockReason.MinorSafety)
        assertTrue(copy.english.contains("minors", ignoreCase = true))
        assertTrue(copy.chinese.contains("未成年"))
    }

    @Test
    fun `other fallback copy suggests rephrasing in english`() {
        val copy = BlockReasonCopy.localizedCopy(BlockReason.Other)
        assertTrue(copy.english.contains("rephras", ignoreCase = true))
    }

    @Test
    fun `copy values are distinct across every reason in english`() {
        val englishCopies = BlockReason.entries.map { BlockReasonCopy.localizedCopy(it).english }
        assertEquals(
            "english copy must be unique per reason",
            englishCopies.size,
            englishCopies.toSet().size,
        )
    }

    @Test
    fun `copy values are distinct across every reason in chinese`() {
        val chineseCopies = BlockReason.entries.map { BlockReasonCopy.localizedCopy(it).chinese }
        assertEquals(
            "chinese copy must be unique per reason",
            chineseCopies.size,
            chineseCopies.toSet().size,
        )
    }
}
