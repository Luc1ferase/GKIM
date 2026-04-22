package com.gkim.im.android.data.remote.im

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ImWorldInfoClientTest {
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
    fun `list fetches all user lorebooks with bearer token`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "lorebooks": [
                    {
                      "id": "lb-atlas",
                      "ownerId": "user-nox",
                      "displayName": { "english": "Atlas", "chinese": "图鉴" },
                      "description": { "english": "Notes.", "chinese": "笔记。" },
                      "isGlobal": false,
                      "isBuiltIn": false,
                      "tokenBudget": 1024,
                      "extensions": {},
                      "createdAt": 1700000000,
                      "updatedAt": 1700000000
                    }
                  ]
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val result = client.list(baseUrl(), "session-token-1")
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/api/lorebooks", request.path)
        assertEquals("Bearer session-token-1", request.getHeader("Authorization"))
        assertEquals(1, result.lorebooks.size)
        assertEquals("lb-atlas", result.lorebooks.single().id)
    }

    @Test
    fun `list raises on non-success response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("unauthorized"))

        var thrown: Throwable? = null
        try {
            client.list(baseUrl(), "bad-token")
            fail("expected exception on 401 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertNotNull(thrown)
    }

    @Test
    fun `get fetches a single lorebook by id`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "id": "lb-atlas",
                  "ownerId": "user-nox",
                  "displayName": { "english": "Atlas", "chinese": "图鉴" }
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val lorebook = client.get(baseUrl(), "tok", "lb-atlas")
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/api/lorebooks/lb-atlas", request.path)
        assertEquals("lb-atlas", lorebook.id)
    }

    @Test
    fun `create posts displayName and returns persisted lorebook`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {
                  "id": "lb-new",
                  "ownerId": "user-nox",
                  "displayName": { "english": "New", "chinese": "新" }
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val created = client.create(
            baseUrl = baseUrl(),
            token = "tok",
            request = CreateLorebookRequestDto(
                displayName = LocalizedTextDto("New", "新"),
                description = LocalizedTextDto("Desc", "描述"),
                isGlobal = true,
                tokenBudget = 2048,
            ),
        )
        val request = server.takeRequest()
        val body = request.body.readUtf8()

        assertEquals("POST", request.method)
        assertEquals("/api/lorebooks", request.path)
        assertTrue(body.contains("\"english\":\"New\""))
        assertTrue(body.contains("\"tokenBudget\":2048"))
        assertTrue(body.contains("\"isGlobal\":true"))
        assertEquals("lb-new", created.id)
    }

    @Test
    fun `create raises on backend rejection`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(400).setBody("bad request"))

        var thrown: Throwable? = null
        try {
            client.create(
                baseUrl = baseUrl(),
                token = "tok",
                request = CreateLorebookRequestDto(
                    displayName = LocalizedTextDto("", ""),
                ),
            )
            fail("expected exception on 400 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertNotNull(thrown)
    }

    @Test
    fun `update sends PATCH with partial fields`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "id": "lb-atlas",
                  "ownerId": "user-nox",
                  "displayName": { "english": "Atlas", "chinese": "图鉴" },
                  "tokenBudget": 4096
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val updated = client.update(
            baseUrl = baseUrl(),
            token = "tok",
            lorebookId = "lb-atlas",
            request = UpdateLorebookRequestDto(tokenBudget = 4096),
        )
        val request = server.takeRequest()
        val body = request.body.readUtf8()

        assertEquals("PATCH", request.method)
        assertEquals("/api/lorebooks/lb-atlas", request.path)
        assertTrue(body.contains("\"tokenBudget\":4096"))
        assertEquals(4096, updated.tokenBudget)
    }

    @Test
    fun `delete sends DELETE with bearer token`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))

        client.delete(baseUrl(), "tok", "lb-atlas")
        val request = server.takeRequest()

        assertEquals("DELETE", request.method)
        assertEquals("/api/lorebooks/lb-atlas", request.path)
        assertEquals("Bearer tok", request.getHeader("Authorization"))
    }

    @Test
    fun `delete raises on 409 when bindings exist`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(409).setBody(
                """{"errorCode":"lorebook_has_bindings"}""",
            ),
        )

        var thrown: Throwable? = null
        try {
            client.delete(baseUrl(), "tok", "lb-atlas")
            fail("expected exception on 409 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertNotNull(thrown)
    }

    @Test
    fun `duplicate posts to duplicate endpoint and returns new record`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {
                  "id": "lb-atlas-copy",
                  "ownerId": "user-nox",
                  "displayName": { "english": "Atlas copy", "chinese": "图鉴副本" }
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val copy = client.duplicate(baseUrl(), "tok", "lb-atlas")
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/lorebooks/lb-atlas/duplicate", request.path)
        assertEquals("lb-atlas-copy", copy.id)
    }

    @Test
    fun `listEntries returns entry list under lorebook path`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "entries": [
                    {
                      "id": "e1",
                      "lorebookId": "lb-atlas",
                      "name": { "english": "Moon", "chinese": "月" },
                      "keysByLang": { "english": ["moon"], "chinese": ["月亮"] },
                      "secondaryGate": "NONE"
                    }
                  ]
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val result = client.listEntries(baseUrl(), "tok", "lb-atlas")
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/api/lorebooks/lb-atlas/entries", request.path)
        assertEquals(1, result.entries.size)
        assertEquals(listOf("moon"), result.entries.single().keysByLang.english)
    }

    @Test
    fun `createEntry posts name and per-language keys`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {
                  "id": "e-new",
                  "lorebookId": "lb-atlas",
                  "name": { "english": "New", "chinese": "新" }
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val created = client.createEntry(
            baseUrl = baseUrl(),
            token = "tok",
            lorebookId = "lb-atlas",
            request = CreateLorebookEntryRequestDto(
                name = LocalizedTextDto("New", "新"),
                keysByLang = PerLanguageStringListDto(english = listOf("x")),
                secondaryGate = "OR",
                scanDepth = 2,
            ),
        )
        val request = server.takeRequest()
        val body = request.body.readUtf8()

        assertEquals("POST", request.method)
        assertEquals("/api/lorebooks/lb-atlas/entries", request.path)
        assertTrue(body.contains("\"secondaryGate\":\"OR\""))
        assertTrue(body.contains("\"scanDepth\":2"))
        assertEquals("e-new", created.id)
    }

    @Test
    fun `updateEntry sends PATCH with entry id in path`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "id": "e1",
                  "lorebookId": "lb-atlas",
                  "name": { "english": "Moon", "chinese": "月" },
                  "insertionOrder": 5
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val updated = client.updateEntry(
            baseUrl = baseUrl(),
            token = "tok",
            lorebookId = "lb-atlas",
            entryId = "e1",
            request = UpdateLorebookEntryRequestDto(insertionOrder = 5, enabled = false),
        )
        val request = server.takeRequest()
        val body = request.body.readUtf8()

        assertEquals("PATCH", request.method)
        assertEquals("/api/lorebooks/lb-atlas/entries/e1", request.path)
        assertTrue(body.contains("\"insertionOrder\":5"))
        assertTrue(body.contains("\"enabled\":false"))
        assertEquals(5, updated.insertionOrder)
    }

    @Test
    fun `deleteEntry sends DELETE with entry id`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))

        client.deleteEntry(baseUrl(), "tok", "lb-atlas", "e1")
        val request = server.takeRequest()

        assertEquals("DELETE", request.method)
        assertEquals("/api/lorebooks/lb-atlas/entries/e1", request.path)
    }

    @Test
    fun `listBindings returns bindings under lorebook path`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "bindings": [
                    { "lorebookId": "lb-atlas", "characterId": "card-aria", "isPrimary": true },
                    { "lorebookId": "lb-atlas", "characterId": "card-nova", "isPrimary": false }
                  ]
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val result = client.listBindings(baseUrl(), "tok", "lb-atlas")
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/api/lorebooks/lb-atlas/bindings", request.path)
        assertEquals(2, result.bindings.size)
        assertTrue(result.bindings.first().isPrimary)
    }

    @Test
    fun `bind posts characterId and isPrimary to bindings endpoint`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                { "lorebookId": "lb-atlas", "characterId": "card-aria", "isPrimary": true }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val binding = client.bind(
            baseUrl = baseUrl(),
            token = "tok",
            lorebookId = "lb-atlas",
            characterId = "card-aria",
            isPrimary = true,
        )
        val request = server.takeRequest()
        val body = request.body.readUtf8()

        assertEquals("POST", request.method)
        assertEquals("/api/lorebooks/lb-atlas/bindings", request.path)
        assertTrue(body.contains("\"characterId\":\"card-aria\""))
        assertTrue(body.contains("\"isPrimary\":true"))
        assertTrue(binding.isPrimary)
    }

    @Test
    fun `bind raises on 409 when binding already exists`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(409).setBody("""{"errorCode":"binding_exists"}"""),
        )

        var thrown: Throwable? = null
        try {
            client.bind(
                baseUrl = baseUrl(),
                token = "tok",
                lorebookId = "lb-atlas",
                characterId = "card-aria",
                isPrimary = false,
            )
            fail("expected exception on 409 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertNotNull(thrown)
    }

    @Test
    fun `updateBinding patches primary flag for character`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                { "lorebookId": "lb-atlas", "characterId": "card-aria", "isPrimary": false }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val binding = client.updateBinding(
            baseUrl = baseUrl(),
            token = "tok",
            lorebookId = "lb-atlas",
            characterId = "card-aria",
            isPrimary = false,
        )
        val request = server.takeRequest()
        val body = request.body.readUtf8()

        assertEquals("PATCH", request.method)
        assertEquals("/api/lorebooks/lb-atlas/bindings/card-aria", request.path)
        assertTrue(body.contains("\"isPrimary\":false"))
        assertEquals(false, binding.isPrimary)
    }

    @Test
    fun `unbind sends DELETE with characterId in path`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))

        client.unbind(baseUrl(), "tok", "lb-atlas", "card-aria")
        val request = server.takeRequest()

        assertEquals("DELETE", request.method)
        assertEquals("/api/lorebooks/lb-atlas/bindings/card-aria", request.path)
    }

    @Test
    fun `unbind raises on 404 when binding does not exist`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        var thrown: Throwable? = null
        try {
            client.unbind(baseUrl(), "tok", "lb-atlas", "card-unknown")
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertNotNull(thrown)
    }
}
