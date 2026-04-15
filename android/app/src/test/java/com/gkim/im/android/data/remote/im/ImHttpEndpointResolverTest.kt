package com.gkim.im.android.data.remote.im

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImHttpEndpointResolverTest {
    @Test
    fun `resolver prefers authenticated session base url and derives websocket`() {
        val resolved = ImHttpEndpointResolver.resolve(
            sessionBaseUrl = "https://session.example.com/api",
            developerOverrideOrigin = "https://validation.example.com/im/",
            shippedBackendOrigin = "https://release.example.com/",
            allowDeveloperOverrides = true,
        )

        assertEquals("https://session.example.com/api/", resolved.backendOrigin)
        assertEquals("https://session.example.com/api/", resolved.httpBaseUrl)
        assertEquals("wss://session.example.com/api/ws", resolved.webSocketUrl)
        assertEquals(ImEndpointSource.Session, resolved.source)
        assertFalse(resolved.isLoopbackHost)
    }

    @Test
    fun `resolver uses developer override when allowed`() {
        val resolved = ImHttpEndpointResolver.resolve(
            sessionBaseUrl = null,
            developerOverrideOrigin = "https://forward.example.com/im",
            shippedBackendOrigin = "https://release.example.com/",
            allowDeveloperOverrides = true,
        )

        assertEquals("https://forward.example.com/im/", resolved.backendOrigin)
        assertEquals("wss://forward.example.com/im/ws", resolved.webSocketUrl)
        assertEquals(ImEndpointSource.DeveloperOverride, resolved.source)
        assertFalse(resolved.isLoopbackHost)
    }

    @Test
    fun `resolver falls back to shipped backend origin when override is not allowed`() {
        val resolved = ImHttpEndpointResolver.resolve(
            sessionBaseUrl = null,
            developerOverrideOrigin = "http://10.0.2.2:18080/",
            shippedBackendOrigin = "https://release.example.com/",
            allowDeveloperOverrides = false,
        )

        assertEquals("https://release.example.com/", resolved.backendOrigin)
        assertEquals("wss://release.example.com/ws", resolved.webSocketUrl)
        assertEquals(ImEndpointSource.BundledDefault, resolved.source)
        assertFalse(resolved.isLoopbackHost)
    }

    @Test
    fun `resolver flags explicit localhost developer overrides as emulator risks`() {
        val resolved = ImHttpEndpointResolver.resolve(
            sessionBaseUrl = null,
            developerOverrideOrigin = "http://127.0.0.1:18080",
            shippedBackendOrigin = "https://release.example.com/",
            allowDeveloperOverrides = true,
        )

        assertEquals("http://127.0.0.1:18080/", resolved.backendOrigin)
        assertEquals("ws://127.0.0.1:18080/ws", resolved.webSocketUrl)
        assertEquals(ImEndpointSource.DeveloperOverride, resolved.source)
        assertTrue(resolved.isLoopbackHost)
    }

    @Test
    fun `stored backend origin prefers explicit value then legacy http then legacy websocket then shipped default`() {
        assertEquals(
            "https://stored.example.com/",
            resolveStoredImBackendOrigin(
                storedBackendOrigin = "https://stored.example.com",
                legacyHttpBaseUrl = "https://legacy-http.example.com",
                legacyWebSocketUrl = "wss://legacy-ws.example.com/ws",
                shippedBackendOrigin = "https://release.example.com/",
            ),
        )
        assertEquals(
            "https://legacy-http.example.com/",
            resolveStoredImBackendOrigin(
                storedBackendOrigin = "",
                legacyHttpBaseUrl = "https://legacy-http.example.com",
                legacyWebSocketUrl = "wss://legacy-ws.example.com/ws",
                shippedBackendOrigin = "https://release.example.com/",
            ),
        )
        assertEquals(
            "https://legacy-ws.example.com/",
            resolveStoredImBackendOrigin(
                storedBackendOrigin = "",
                legacyHttpBaseUrl = "",
                legacyWebSocketUrl = "wss://legacy-ws.example.com/ws",
                shippedBackendOrigin = "https://release.example.com/",
            ),
        )
        assertEquals(
            "https://release.example.com/",
            resolveStoredImBackendOrigin(
                storedBackendOrigin = "",
                legacyHttpBaseUrl = "",
                legacyWebSocketUrl = "",
                shippedBackendOrigin = "https://release.example.com/",
            ),
        )
    }
}
