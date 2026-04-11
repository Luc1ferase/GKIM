package com.gkim.im.android.data.remote.im

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImHttpEndpointResolverTest {
    @Test
    fun `resolver prefers authenticated session base url`() {
        val resolved = ImHttpEndpointResolver.resolve(
            sessionBaseUrl = "https://session.example.com/api",
            validationBaseUrl = "https://validation.example.com/im/",
        )

        assertEquals("https://session.example.com/api/", resolved.baseUrl)
        assertEquals(ImHttpEndpointSource.Session, resolved.source)
        assertFalse(resolved.isLoopbackHost)
    }

    @Test
    fun `resolver falls back to persisted validation url before default loopback`() {
        val resolved = ImHttpEndpointResolver.resolve(
            sessionBaseUrl = null,
            validationBaseUrl = "https://forward.example.com",
        )

        assertEquals("https://forward.example.com/", resolved.baseUrl)
        assertEquals(ImHttpEndpointSource.ImValidation, resolved.source)
        assertFalse(resolved.isLoopbackHost)
    }

    @Test
    fun `resolver uses published emulator target only when neither session nor validation urls exist`() {
        val resolved = ImHttpEndpointResolver.resolve(
            sessionBaseUrl = "   ",
            validationBaseUrl = "",
        )

        assertEquals(DEFAULT_IM_HTTP_BASE_URL, resolved.baseUrl)
        assertEquals(ImHttpEndpointSource.DefaultPublishedTarget, resolved.source)
        assertFalse(resolved.isLoopbackHost)
    }

    @Test
    fun `resolver flags explicit localhost validation urls as emulator risks`() {
        val resolved = ImHttpEndpointResolver.resolve(
            sessionBaseUrl = null,
            validationBaseUrl = "http://127.0.0.1:18080",
        )

        assertEquals("http://127.0.0.1:18080/", resolved.baseUrl)
        assertEquals(ImHttpEndpointSource.ImValidation, resolved.source)
        assertTrue(resolved.isLoopbackHost)
    }
}
