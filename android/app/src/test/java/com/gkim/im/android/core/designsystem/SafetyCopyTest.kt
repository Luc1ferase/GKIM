package com.gkim.im.android.core.designsystem

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.FailedSubtype
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyCopyTest {

    @Test
    fun `every failed subtype returns non-blank bilingual copy`() {
        FailedSubtype.entries.forEach { subtype ->
            val copy = SafetyCopy.localizedFailedCopy(subtype)
            assertTrue("english copy blank for $subtype", copy.english.isNotBlank())
            assertTrue("chinese copy blank for $subtype", copy.chinese.isNotBlank())
        }
    }

    @Test
    fun `english and chinese failed copy are distinct per subtype`() {
        FailedSubtype.entries.forEach { subtype ->
            val copy = SafetyCopy.localizedFailedCopy(subtype)
            assertTrue(
                "english must differ from chinese for $subtype",
                copy.english != copy.chinese,
            )
        }
    }

    @Test
    fun `failed english resolve returns english per subtype`() {
        FailedSubtype.entries.forEach { subtype ->
            val expected = SafetyCopy.localizedFailedCopy(subtype).english
            val actual = SafetyCopy.localizedFailedCopy(subtype, AppLanguage.English)
            assertEquals("English resolve for $subtype", expected, actual)
        }
    }

    @Test
    fun `failed chinese resolve returns chinese per subtype`() {
        FailedSubtype.entries.forEach { subtype ->
            val expected = SafetyCopy.localizedFailedCopy(subtype).chinese
            val actual = SafetyCopy.localizedFailedCopy(subtype, AppLanguage.Chinese)
            assertEquals("Chinese resolve for $subtype", expected, actual)
        }
    }

    @Test
    fun `transient copy matches design doc wording`() {
        val copy = SafetyCopy.localizedFailedCopy(FailedSubtype.Transient)
        assertEquals("Something went wrong. Please try again.", copy.english)
    }

    @Test
    fun `prompt budget exceeded copy mentions shortening the message`() {
        val copy = SafetyCopy.localizedFailedCopy(FailedSubtype.PromptBudgetExceeded)
        assertTrue(copy.english.contains("shorten", ignoreCase = true))
        assertTrue(copy.chinese.contains("缩短"))
    }

    @Test
    fun `authentication failed copy suggests sign in again`() {
        val copy = SafetyCopy.localizedFailedCopy(FailedSubtype.AuthenticationFailed)
        assertTrue(copy.english.contains("sign in", ignoreCase = true))
        assertTrue(copy.chinese.contains("登录"))
    }

    @Test
    fun `provider unavailable copy hints at retry later`() {
        val copy = SafetyCopy.localizedFailedCopy(FailedSubtype.ProviderUnavailable)
        assertTrue(copy.english.contains("unavailable", ignoreCase = true))
        assertTrue(copy.chinese.contains("服务"))
    }

    @Test
    fun `network error copy mentions connection check`() {
        val copy = SafetyCopy.localizedFailedCopy(FailedSubtype.NetworkError)
        assertTrue(copy.english.contains("connection", ignoreCase = true))
        assertTrue(copy.chinese.contains("网络"))
    }

    @Test
    fun `failed copies are unique across every subtype in english`() {
        val englishCopies = FailedSubtype.entries.map { SafetyCopy.localizedFailedCopy(it).english }
        assertEquals(
            "english copy must be unique per subtype",
            englishCopies.size,
            englishCopies.toSet().size,
        )
    }

    @Test
    fun `failed copies are unique across every subtype in chinese`() {
        val chineseCopies = FailedSubtype.entries.map { SafetyCopy.localizedFailedCopy(it).chinese }
        assertEquals(
            "chinese copy must be unique per subtype",
            chineseCopies.size,
            chineseCopies.toSet().size,
        )
    }

    @Test
    fun `timeout copy is non-blank and bilingual`() {
        val copy = SafetyCopy.timeoutCopy
        assertTrue(copy.english.isNotBlank())
        assertTrue(copy.chinese.isNotBlank())
        assertTrue(copy.english != copy.chinese)
    }

    @Test
    fun `timeout english resolve matches struct`() {
        assertEquals(SafetyCopy.timeoutCopy.english, SafetyCopy.localizedTimeoutCopy(AppLanguage.English))
    }

    @Test
    fun `timeout chinese resolve matches struct`() {
        assertEquals(SafetyCopy.timeoutCopy.chinese, SafetyCopy.localizedTimeoutCopy(AppLanguage.Chinese))
    }

    @Test
    fun `timeout copy mentions took too long in english`() {
        assertTrue(SafetyCopy.timeoutCopy.english.contains("too long", ignoreCase = true))
    }

    @Test
    fun `timeout copy mentions 超时 in chinese`() {
        assertTrue(SafetyCopy.timeoutCopy.chinese.contains("超时"))
    }

    @Test
    fun `timeout preset hint is non-blank and bilingual`() {
        val copy = SafetyCopy.timeoutPresetHint
        assertTrue(copy.english.isNotBlank())
        assertTrue(copy.chinese.isNotBlank())
        assertTrue(copy.english != copy.chinese)
    }

    @Test
    fun `timeout preset hint resolves via language enum`() {
        assertEquals(SafetyCopy.timeoutPresetHint.english, SafetyCopy.localizedTimeoutPresetHint(AppLanguage.English))
        assertEquals(SafetyCopy.timeoutPresetHint.chinese, SafetyCopy.localizedTimeoutPresetHint(AppLanguage.Chinese))
    }

    @Test
    fun `failed subtype enum exposes correct wire keys`() {
        assertEquals("transient", FailedSubtype.Transient.wireKey)
        assertEquals("prompt_budget_exceeded", FailedSubtype.PromptBudgetExceeded.wireKey)
        assertEquals("authentication_failed", FailedSubtype.AuthenticationFailed.wireKey)
        assertEquals("provider_unavailable", FailedSubtype.ProviderUnavailable.wireKey)
        assertEquals("network_error", FailedSubtype.NetworkError.wireKey)
        assertEquals("unknown", FailedSubtype.Unknown.wireKey)
    }

    @Test
    fun `failed subtype fromWireKey round trips every value`() {
        FailedSubtype.entries.forEach { subtype ->
            assertEquals(subtype, FailedSubtype.fromWireKey(subtype.wireKey))
        }
    }

    @Test
    fun `failed subtype fromWireKey falls back to Unknown on unrecognized key`() {
        assertEquals(FailedSubtype.Unknown, FailedSubtype.fromWireKey("brand_new_server_reason"))
        assertEquals(FailedSubtype.Unknown, FailedSubtype.fromWireKey(null))
        assertEquals(FailedSubtype.Unknown, FailedSubtype.fromWireKey(""))
    }
}
