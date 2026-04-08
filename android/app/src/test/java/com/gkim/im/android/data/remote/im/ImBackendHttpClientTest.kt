package com.gkim.im.android.data.remote.im

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImBackendHttpClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: ImBackendHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = ImBackendHttpClient(OkHttpClient())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `issueDevSession posts external id to backend session endpoint`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "token": "session-token-2",
                  "expiresAt": "2026-04-15T10:00:00Z",
                  "user": {
                    "id": "user-nox",
                    "externalId": "nox-dev",
                    "displayName": "Nox Dev",
                    "title": "IM Milestone Owner",
                    "avatarText": "NX"
                  }
                }
                """.trimIndent()
            ).addHeader("Content-Type", "application/json"),
        )

        val session = client.issueDevSession(server.url("").toString(), "nox-dev")
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/session/dev", request.path)
        assertTrue(request.body.readUtf8().contains("\"externalId\":\"nox-dev\""))
        assertEquals("session-token-2", session.token)
    }

    @Test
    fun `loadBootstrap attaches bearer token and maps backend payload`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "user": {
                    "id": "user-nox",
                    "externalId": "nox-dev",
                    "displayName": "Nox Dev",
                    "title": "IM Milestone Owner",
                    "avatarText": "NX"
                  },
                  "contacts": [
                    {
                      "userId": "user-leo",
                      "externalId": "leo-vance",
                      "displayName": "Leo Vance",
                      "title": "Realtime Systems",
                      "avatarText": "LV",
                      "addedAt": "2026-04-08T08:58:00Z"
                    }
                  ],
                  "conversations": []
                }
                """.trimIndent()
            ).addHeader("Content-Type", "application/json"),
        )

        val bootstrap = client.loadBootstrap(server.url("").toString(), "session-token-2")
        val request = server.takeRequest()

        assertEquals("/api/bootstrap", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertEquals("nox-dev", bootstrap.user.externalId)
        assertEquals("leo-vance", bootstrap.contacts.single().externalId)
    }

    @Test
    fun `loadHistory forwards conversation query params and auth header`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "conversationId": "conversation-1",
                  "messages": [],
                  "hasMore": false
                }
                """.trimIndent()
            ).addHeader("Content-Type", "application/json"),
        )

        val history = client.loadHistory(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            conversationId = "conversation-1",
            limit = 20,
            before = "message-3",
        )
        val request = server.takeRequest()

        assertTrue(request.path?.startsWith("/api/conversations/conversation-1/messages?") == true)
        assertTrue(request.path?.contains("limit=20") == true)
        assertTrue(request.path?.contains("before=message-3") == true)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertEquals("conversation-1", history.conversationId)
        assertEquals(false, history.hasMore)
    }
}
