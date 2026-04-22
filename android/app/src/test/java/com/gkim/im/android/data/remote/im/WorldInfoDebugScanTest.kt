package com.gkim.im.android.data.remote.im

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class WorldInfoDebugScanTest {
    private lateinit var server: MockWebServer
    private lateinit var client: ImWorldInfoHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = ImWorldInfoHttpClient(OkHttpClient())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun baseUrl(): String = server.url("").toString()

    @Test
    fun `debugScan posts request body to scan endpoint with bearer token and dev-access header`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "matches": [
                    {
                      "entryId": "e-moon",
                      "lorebookId": "lb-atlas",
                      "insertionOrder": 1,
                      "constant": false,
                      "matchedKey": "moon",
                      "language": "en"
                    }
                  ]
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val response = client.debugScan(
            baseUrl = baseUrl(),
            token = "tok-debug",
            characterId = "card-aria",
            scanText = "the moon hangs low",
            devAccessHeader = "dev-key-abc",
            allowDebug = true,
        )
        val request = server.takeRequest()
        val body = request.body.readUtf8()

        assertEquals("POST", request.method)
        assertEquals("/api/debug/worldinfo/scan", request.path)
        assertEquals("Bearer tok-debug", request.getHeader("Authorization"))
        assertEquals("dev-key-abc", request.getHeader(DEBUG_ACCESS_HEADER))
        assertTrue(body.contains("\"characterId\":\"card-aria\""))
        assertTrue(body.contains("\"scanText\":\"the moon hangs low\""))
        assertEquals(1, response.matches.size)
        val match = response.matches.single()
        assertEquals("e-moon", match.entryId)
        assertEquals("lb-atlas", match.lorebookId)
        assertEquals(1, match.insertionOrder)
        assertEquals("moon", match.matchedKey)
        assertEquals("en", match.language)
        assertEquals(false, match.constant)
    }

    @Test
    fun `debugScan constant entry decodes with matchedKey null and constant true`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "matches": [
                    {
                      "entryId": "e-always",
                      "lorebookId": "lb-atlas",
                      "insertionOrder": 0,
                      "constant": true
                    }
                  ]
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val response = client.debugScan(
            baseUrl = baseUrl(),
            token = "tok",
            characterId = "card-aria",
            scanText = "anything",
            devAccessHeader = "dev",
            allowDebug = true,
        )

        val match = response.matches.single()
        assertEquals(true, match.constant)
        assertNull(match.matchedKey)
        assertNull(match.language)
    }

    @Test
    fun `debugScan sorts matches by insertionOrder ascending regardless of server order`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "matches": [
                    { "entryId": "e-c", "lorebookId": "lb-a", "insertionOrder": 30, "constant": false },
                    { "entryId": "e-a", "lorebookId": "lb-a", "insertionOrder": 10, "constant": true },
                    { "entryId": "e-b", "lorebookId": "lb-a", "insertionOrder": 20, "constant": false }
                  ]
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val response = client.debugScan(
            baseUrl = baseUrl(),
            token = "tok",
            characterId = "card-aria",
            scanText = "text",
            devAccessHeader = "dev",
            allowDebug = true,
        )

        assertEquals(
            listOf("e-a", "e-b", "e-c"),
            response.matches.map { it.entryId },
        )
    }

    @Test
    fun `debugScan breaks insertionOrder ties by lorebookId then entryId`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "matches": [
                    { "entryId": "e-z", "lorebookId": "lb-b", "insertionOrder": 10, "constant": false },
                    { "entryId": "e-a", "lorebookId": "lb-b", "insertionOrder": 10, "constant": false },
                    { "entryId": "e-m", "lorebookId": "lb-a", "insertionOrder": 10, "constant": false }
                  ]
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val response = client.debugScan(
            baseUrl = baseUrl(),
            token = "tok",
            characterId = "card-aria",
            scanText = "text",
            devAccessHeader = "dev",
            allowDebug = true,
        )

        assertEquals(
            listOf("e-m" to "lb-a", "e-a" to "lb-b", "e-z" to "lb-b"),
            response.matches.map { it.entryId to it.lorebookId },
        )
    }

    @Test
    fun `debugScan tolerates an empty matches list`() = runBlocking {
        server.enqueue(
            MockResponse().setBody("""{"matches":[]}""").addHeader("Content-Type", "application/json"),
        )

        val response = client.debugScan(
            baseUrl = baseUrl(),
            token = "tok",
            characterId = "card-aria",
            scanText = "nothing matches",
            devAccessHeader = "dev",
            allowDebug = true,
        )

        assertTrue(response.matches.isEmpty())
    }

    @Test
    fun `debugScan decodes a missing matches field as empty for backwards compatibility`() = runBlocking {
        server.enqueue(
            MockResponse().setBody("""{}""").addHeader("Content-Type", "application/json"),
        )

        val response = client.debugScan(
            baseUrl = baseUrl(),
            token = "tok",
            characterId = "card-aria",
            scanText = "x",
            devAccessHeader = "dev",
            allowDebug = true,
        )

        assertTrue(response.matches.isEmpty())
    }

    @Test
    fun `debugScan short-circuits without a network request when allowDebug is false`() = runBlocking {
        var thrown: Throwable? = null
        try {
            client.debugScan(
                baseUrl = baseUrl(),
                token = "tok",
                characterId = "card-aria",
                scanText = "anything",
                devAccessHeader = "dev",
                allowDebug = false,
            )
            fail("expected debug_scan_disabled_in_release")
        } catch (t: IllegalStateException) {
            thrown = t
        }
        assertNotNull(thrown)
        assertEquals("debug_scan_disabled_in_release", thrown!!.message)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `debugScan rejects a blank dev-access header without a network request`() = runBlocking {
        var thrown: Throwable? = null
        try {
            client.debugScan(
                baseUrl = baseUrl(),
                token = "tok",
                characterId = "card-aria",
                scanText = "x",
                devAccessHeader = "   ",
                allowDebug = true,
            )
            fail("expected debug_access_header_missing")
        } catch (t: IllegalArgumentException) {
            thrown = t
        }
        assertNotNull(thrown)
        assertEquals("debug_access_header_missing", thrown!!.message)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `debugScan raises on 403 when backend rejects the dev-access header`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(403).setBody("""{"errorCode":"debug_forbidden"}"""),
        )

        var thrown: Throwable? = null
        try {
            client.debugScan(
                baseUrl = baseUrl(),
                token = "tok",
                characterId = "card-aria",
                scanText = "x",
                devAccessHeader = "wrong-key",
                allowDebug = true,
            )
            fail("expected exception on 403 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertNotNull(thrown)
    }

    @Test
    fun `debugScan raises on 404 when the character id is unknown`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(404).setBody("""{"errorCode":"unknown_character"}"""),
        )

        var thrown: Throwable? = null
        try {
            client.debugScan(
                baseUrl = baseUrl(),
                token = "tok",
                characterId = "card-unknown",
                scanText = "x",
                devAccessHeader = "dev",
                allowDebug = true,
            )
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertNotNull(thrown)
    }

    @Test
    fun `DEBUG_ACCESS_HEADER exposes the X-GKIM-Debug-Access constant for cross-layer reuse`() {
        assertEquals("X-GKIM-Debug-Access", DEBUG_ACCESS_HEADER)
    }
}
