package com.gkim.im.android.data.remote.im

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
    fun `submitCompanionTurn posts request body and bearer token and decodes record`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "turnId": "turn-1",
                  "conversationId": "conversation-1",
                  "messageId": "message-1",
                  "variantGroupId": "vg-1",
                  "variantIndex": 0,
                  "parentMessageId": "message-0",
                  "status": "thinking",
                  "accumulatedBody": "",
                  "lastDeltaSeq": 0,
                  "providerId": "openai",
                  "model": "gpt-4o-mini",
                  "startedAt": "2026-04-21T08:00:00Z"
                }
                """.trimIndent()
            ).addHeader("Content-Type", "application/json"),
        )

        val record = client.submitCompanionTurn(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            request = CompanionTurnSubmitRequestDto(
                conversationId = "conversation-1",
                activeCompanionId = "architect-oracle",
                userTurnBody = "Hello",
                activeLanguage = "en",
                clientTurnId = "client-turn-1",
                parentMessageId = "message-0",
            ),
        )
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/companion-turns", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"conversationId\":\"conversation-1\""))
        assertTrue(body.contains("\"activeCompanionId\":\"architect-oracle\""))
        assertTrue(body.contains("\"clientTurnId\":\"client-turn-1\""))
        assertEquals("turn-1", record.turnId)
        assertEquals("thinking", record.status)
    }

    @Test
    fun `submitCompanionTurn raises on non-success response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("server broke"))

        var thrown: Throwable? = null
        try {
            client.submitCompanionTurn(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                request = CompanionTurnSubmitRequestDto(
                    conversationId = "conversation-1",
                    activeCompanionId = "architect-oracle",
                    userTurnBody = "Hello",
                    activeLanguage = "en",
                    clientTurnId = "client-turn-err",
                ),
            )
            fail("expected exception on 500 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue("expected retrofit error", thrown != null)
    }

    @Test
    fun `regenerateCompanionTurn posts clientTurnId in body with turnId path`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "turnId": "turn-2",
                  "conversationId": "conversation-1",
                  "messageId": "message-2",
                  "variantGroupId": "vg-1",
                  "variantIndex": 1,
                  "status": "thinking",
                  "accumulatedBody": "",
                  "lastDeltaSeq": 0,
                  "startedAt": "2026-04-21T08:05:00Z"
                }
                """.trimIndent()
            ).addHeader("Content-Type", "application/json"),
        )

        val record = client.regenerateCompanionTurn(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            turnId = "turn-1",
            clientTurnId = "client-turn-regen",
        )
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/companion-turns/turn-1/regenerate", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertTrue(request.body.readUtf8().contains("\"clientTurnId\":\"client-turn-regen\""))
        assertEquals("turn-2", record.turnId)
        assertEquals(1, record.variantIndex)
    }

    @Test
    fun `regenerateCompanionTurn raises on non-success response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(409).setBody("already active"))

        var thrown: Throwable? = null
        try {
            client.regenerateCompanionTurn(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                turnId = "turn-1",
                clientTurnId = "client-turn-dup",
            )
            fail("expected exception on 409 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `listPendingCompanionTurns returns decoded pending turns with bearer token`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "turns": [
                    {
                      "turnId": "turn-7",
                      "conversationId": "conversation-1",
                      "messageId": "message-7",
                      "variantGroupId": "vg-2",
                      "variantIndex": 0,
                      "status": "streaming",
                      "accumulatedBody": "partial body",
                      "lastDeltaSeq": 4,
                      "startedAt": "2026-04-21T07:59:00Z"
                    }
                  ]
                }
                """.trimIndent()
            ).addHeader("Content-Type", "application/json"),
        )

        val pending = client.listPendingCompanionTurns(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
        )
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/api/companion-turns/pending", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertEquals(1, pending.turns.size)
        assertEquals("streaming", pending.turns.single().status)
        assertEquals(4, pending.turns.single().lastDeltaSeq)
    }

    @Test
    fun `listPendingCompanionTurns raises on non-success response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("unauthorized"))

        var thrown: Throwable? = null
        try {
            client.listPendingCompanionTurns(
                baseUrl = server.url("").toString(),
                token = "bad-token",
            )
            fail("expected exception on 401 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `snapshotCompanionTurn fetches per-turn snapshot with turnId in path`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "turnId": "turn-7",
                  "conversationId": "conversation-1",
                  "messageId": "message-7",
                  "variantGroupId": "vg-2",
                  "variantIndex": 0,
                  "status": "completed",
                  "accumulatedBody": "The tavern fire settles.",
                  "lastDeltaSeq": 6,
                  "startedAt": "2026-04-21T07:59:00Z",
                  "completedAt": "2026-04-21T08:00:30Z"
                }
                """.trimIndent()
            ).addHeader("Content-Type", "application/json"),
        )

        val record = client.snapshotCompanionTurn(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            turnId = "turn-7",
        )
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/api/companion-turns/turn-7", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertEquals("completed", record.status)
        assertEquals("The tavern fire settles.", record.accumulatedBody)
        assertEquals("2026-04-21T08:00:30Z", record.completedAt)
    }

    @Test
    fun `snapshotCompanionTurn raises on non-success response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        var thrown: Throwable? = null
        try {
            client.snapshotCompanionTurn(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                turnId = "turn-missing",
            )
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
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
