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

    @Test
    fun `importCardPreview uploads base64 payload and returns decoded preview`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "previewToken": "preview-xyz-1",
                  "card": {
                    "id": "card-import-1",
                    "displayName": {"english":"Aria","chinese":"Aria"},
                    "roleLabel": {"english":"Guide","chinese":"向导"},
                    "summary": {"english":"Calm guide","chinese":"Calm guide"},
                    "firstMes": {"english":"Hello traveller.","chinese":"Hello traveller."},
                    "alternateGreetings": [],
                    "avatarText": "AR",
                    "accent": "primary",
                    "source": "userauthored",
                    "extensions": {"st":{"stPostHistoryInstructions":"stay in character"}}
                  },
                  "detectedLanguage": "en",
                  "warnings": [{"code":"post_history_instruction_parked","field":"stPostHistoryInstructions"}],
                  "stExtensionKeys": ["stPostHistoryInstructions"]
                }
                """.trimIndent()
            ).addHeader("Content-Type", "application/json"),
        )

        val pngSignature = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x01, 0x02,
        )

        val preview = client.importCardPreview(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            bytes = pngSignature,
            filename = "aria.png",
        )
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/cards/import", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"filename\":\"aria.png\""))
        assertTrue(body.contains("\"claimedFormat\":\"png\""))
        assertTrue(body.contains("\"contentBase64\""))
        assertEquals("preview-xyz-1", preview.previewToken)
        assertEquals("en", preview.detectedLanguage)
        assertEquals(listOf("stPostHistoryInstructions"), preview.stExtensionKeys)
        assertEquals("post_history_instruction_parked", preview.warnings.single().code)
    }

    @Test
    fun `importCardPreview raises on 413 payload_too_large response`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(413).setBody(
                """{"error":"payload_too_large","detail":"png > 8 MiB"}""",
            ).addHeader("Content-Type", "application/json"),
        )

        var thrown: Throwable? = null
        try {
            client.importCardPreview(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
                filename = "huge.png",
            )
            fail("expected exception on 413 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `importCardPreview raises on 422 malformed payload response`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(422).setBody(
                """{"error":"malformed_png","detail":"tEXt chunk not found"}""",
            ).addHeader("Content-Type", "application/json"),
        )

        var thrown: Throwable? = null
        try {
            client.importCardPreview(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
                filename = "broken.png",
            )
            fail("expected exception on 422 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `importCardCommit persists the preview card and returns the committed record`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "id": "card-persisted-1",
                  "displayName": {"english":"Aria","chinese":"Aria"},
                  "roleLabel": {"english":"Guide","chinese":"向导"},
                  "summary": {"english":"Calm guide","chinese":"Calm guide"},
                  "firstMes": {"english":"Hello traveller.","chinese":"Hello traveller."},
                  "alternateGreetings": [],
                  "avatarText": "AR",
                  "accent": "primary",
                  "source": "userauthored",
                  "extensions": {"st":{"stPostHistoryInstructions":"stay in character"}}
                }
                """.trimIndent()
            ).addHeader("Content-Type", "application/json"),
        )

        val preview = CardImportPreviewDto(
            previewToken = "preview-xyz-1",
            card = CompanionCharacterCardDto(
                id = "card-import-1",
                displayName = LocalizedTextDto("Aria", "Aria"),
                roleLabel = LocalizedTextDto("Guide", "向导"),
                summary = LocalizedTextDto("Calm guide", "Calm guide"),
                firstMes = LocalizedTextDto("Hello traveller.", "Hello traveller."),
                avatarText = "AR",
                accent = "primary",
                source = "userauthored",
            ),
            detectedLanguage = "en",
        )

        val committed = client.importCardCommit(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            preview = preview,
            overrides = null,
            languageOverride = "zh",
        )
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/cards/import/commit", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"previewToken\":\"preview-xyz-1\""))
        assertTrue(body.contains("\"languageOverride\":\"zh\""))
        assertEquals("card-persisted-1", committed.id)
    }

    @Test
    fun `exportCard sends PNG request with language query and returns base64 payload`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "format":"png",
                  "filename":"aria.png",
                  "contentType":"image/png",
                  "encoding":"base64",
                  "payload":"iVBORw0KGgoAAA=="
                }
                """.trimIndent()
            ).addHeader("Content-Type", "application/json"),
        )

        val response = client.exportCard(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            cardId = "card-import-1",
            format = "png",
            language = "zh",
            includeTranslationAlt = true,
        )
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertTrue(request.path?.startsWith("/api/cards/card-import-1/export?") == true)
        assertTrue(request.path?.contains("format=png") == true)
        assertTrue(request.path?.contains("language=zh") == true)
        assertTrue(request.path?.contains("includeTranslationAlt=true") == true)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertEquals("png", response.format)
        assertEquals("base64", response.encoding)
        assertEquals("iVBORw0KGgoAAA==", response.payload)
    }

    @Test
    fun `exportCard sends JSON request and returns utf8 payload`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "format":"json",
                  "filename":"aria.json",
                  "contentType":"application/json",
                  "encoding":"utf8",
                  "payload":"{\"name\":\"Aria\"}"
                }
                """.trimIndent()
            ).addHeader("Content-Type", "application/json"),
        )

        val response = client.exportCard(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            cardId = "card-import-1",
            format = "json",
            language = "en",
        )
        val request = server.takeRequest()

        assertTrue(request.path?.contains("format=json") == true)
        assertTrue(request.path?.contains("language=en") == true)
        assertEquals("json", response.format)
        assertEquals("utf8", response.encoding)
    }

    @Test
    fun `exportCard raises on unknown card 404`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        var thrown: Throwable? = null
        try {
            client.exportCard(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                cardId = "missing-card",
                format = "png",
                language = "en",
            )
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `listPersonas attaches bearer token and decodes persona library`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "personas": [
                    {
                      "id": "persona-default",
                      "displayName": {"english":"You","chinese":"你"},
                      "description": {"english":"Default persona","chinese":"默认人格"},
                      "isBuiltIn": true,
                      "isActive": true,
                      "createdAt": 0,
                      "updatedAt": 0
                    }
                  ],
                  "activePersonaId": "persona-default"
                }
                """.trimIndent()
            ).addHeader("Content-Type", "application/json"),
        )

        val list = client.listPersonas(server.url("").toString(), "session-token-2")
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/api/personas", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertEquals("persona-default", list.activePersonaId)
        assertEquals(1, list.personas.size)
        assertEquals(true, list.personas.single().isBuiltIn)
    }

    @Test
    fun `listPersonas raises on 404 response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        var thrown: Throwable? = null
        try {
            client.listPersonas(server.url("").toString(), "session-token-2")
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `createPersona posts persona body and returns stored record`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "id": "persona-new-1",
                  "displayName": {"english":"Aria","chinese":"Aria"},
                  "description": {"english":"Adventurer","chinese":"冒险者"},
                  "isBuiltIn": false,
                  "isActive": false,
                  "createdAt": 1,
                  "updatedAt": 1
                }
                """.trimIndent()
            ).addHeader("Content-Type", "application/json"),
        )

        val persona = UserPersonaDto(
            id = "persona-new-1",
            displayName = LocalizedTextDto("Aria", "Aria"),
            description = LocalizedTextDto("Adventurer", "冒险者"),
        )
        val created = client.createPersona(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            persona = persona,
        )
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/personas", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"id\":\"persona-new-1\""))
        assertTrue(body.contains("\"english\":\"Aria\""))
        assertEquals("persona-new-1", created.id)
    }

    @Test
    fun `createPersona raises on 409 conflict response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(409).setBody("conflict"))

        var thrown: Throwable? = null
        try {
            client.createPersona(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                persona = UserPersonaDto(
                    id = "persona-new-1",
                    displayName = LocalizedTextDto("Aria", "Aria"),
                    description = LocalizedTextDto("Adventurer", "冒险者"),
                ),
            )
            fail("expected exception on 409 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `updatePersona posts to persona id path with body`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "id": "persona-1",
                  "displayName": {"english":"Aria","chinese":"Aria"},
                  "description": {"english":"Updated","chinese":"更新"},
                  "isBuiltIn": false,
                  "isActive": false,
                  "createdAt": 1,
                  "updatedAt": 5
                }
                """.trimIndent()
            ).addHeader("Content-Type", "application/json"),
        )

        val updated = client.updatePersona(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            persona = UserPersonaDto(
                id = "persona-1",
                displayName = LocalizedTextDto("Aria", "Aria"),
                description = LocalizedTextDto("Updated", "更新"),
                updatedAt = 5,
            ),
        )
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/personas/persona-1", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"english\":\"Updated\""))
        assertEquals("persona-1", updated.id)
        assertEquals(5, updated.updatedAt)
    }

    @Test
    fun `updatePersona raises on 404 missing persona`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        var thrown: Throwable? = null
        try {
            client.updatePersona(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                persona = UserPersonaDto(
                    id = "missing-persona",
                    displayName = LocalizedTextDto("X", "X"),
                    description = LocalizedTextDto("X", "X"),
                ),
            )
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `deletePersona posts to delete sub-route with bearer token`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))

        client.deletePersona(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            personaId = "persona-1",
        )
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/personas/persona-1/delete", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
    }

    @Test
    fun `deletePersona raises on 409 when active persona deletion blocked`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(409).setBody(
                """{"error":"persona_active","detail":"cannot delete active persona"}""",
            ).addHeader("Content-Type", "application/json"),
        )

        var thrown: Throwable? = null
        try {
            client.deletePersona(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                personaId = "persona-active",
            )
            fail("expected exception on 409 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `activatePersona posts to activate sub-route and returns new active`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "id": "persona-2",
                  "displayName": {"english":"Aria","chinese":"Aria"},
                  "description": {"english":"Now active","chinese":"现已激活"},
                  "isBuiltIn": false,
                  "isActive": true,
                  "createdAt": 1,
                  "updatedAt": 9
                }
                """.trimIndent()
            ).addHeader("Content-Type", "application/json"),
        )

        val active = client.activatePersona(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            personaId = "persona-2",
        )
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/personas/persona-2/activate", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertEquals("persona-2", active.id)
        assertEquals(true, active.isActive)
    }

    @Test
    fun `activatePersona raises on 404 unknown persona`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        var thrown: Throwable? = null
        try {
            client.activatePersona(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                personaId = "persona-missing",
            )
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `getActivePersona returns current active persona`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "id": "persona-default",
                  "displayName": {"english":"You","chinese":"你"},
                  "description": {"english":"Default","chinese":"默认"},
                  "isBuiltIn": true,
                  "isActive": true,
                  "createdAt": 0,
                  "updatedAt": 0
                }
                """.trimIndent()
            ).addHeader("Content-Type", "application/json"),
        )

        val active = client.getActivePersona(server.url("").toString(), "session-token-2")
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/api/personas/active", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertEquals("persona-default", active.id)
        assertEquals(true, active.isActive)
        assertEquals(true, active.isBuiltIn)
    }

    @Test
    fun `getActivePersona raises on 404 when no active persona`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("no active"))

        var thrown: Throwable? = null
        try {
            client.getActivePersona(server.url("").toString(), "session-token-2")
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `getCompanionMemory fetches per-card memory with bearer token`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "userId": "user-nox",
                  "companionCardId": "card-aria",
                  "summary": { "english": "She remembers.", "chinese": "她记得。" },
                  "summaryUpdatedAt": 1710000000000,
                  "summaryTurnCursor": 42,
                  "tokenBudgetHint": 1024
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val memory = client.getCompanionMemory(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            cardId = "card-aria",
        )
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/api/companions/card-aria/memory", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertEquals("user-nox", memory.userId)
        assertEquals("card-aria", memory.companionCardId)
        assertEquals(42, memory.summaryTurnCursor)
        assertEquals(1024, memory.tokenBudgetHint)
    }

    @Test
    fun `getCompanionMemory raises on 404 when card has no memory row yet`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("no memory"))

        var thrown: Throwable? = null
        try {
            client.getCompanionMemory(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                cardId = "card-missing",
            )
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `resetCompanionMemory posts lowercase scope wire key`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))

        client.resetCompanionMemory(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            cardId = "card-aria",
            scope = com.gkim.im.android.core.model.CompanionMemoryResetScope.Summary,
        )
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/companions/card-aria/memory/reset", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        val body = request.body.readUtf8()
        assertTrue("scope should serialize as lowercase \"summary\"", body.contains("\"scope\":\"summary\""))
    }

    @Test
    fun `resetCompanionMemory round trips all three scopes to wire key`() = runBlocking {
        listOf(
            com.gkim.im.android.core.model.CompanionMemoryResetScope.Pins to "pins",
            com.gkim.im.android.core.model.CompanionMemoryResetScope.Summary to "summary",
            com.gkim.im.android.core.model.CompanionMemoryResetScope.All to "all",
        ).forEach { (scope, expected) ->
            server.enqueue(MockResponse().setResponseCode(204))
            client.resetCompanionMemory(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                cardId = "card-aria",
                scope = scope,
            )
            val body = server.takeRequest().body.readUtf8()
            assertTrue(
                "scope $scope should serialize as \"$expected\"; body was $body",
                body.contains("\"scope\":\"$expected\""),
            )
        }
    }

    @Test
    fun `resetCompanionMemory raises on 404 when card is missing`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        var thrown: Throwable? = null
        try {
            client.resetCompanionMemory(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                cardId = "card-missing",
                scope = com.gkim.im.android.core.model.CompanionMemoryResetScope.All,
            )
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `listCompanionMemoryPins fetches pins with bearer token`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "pins": [
                    {
                      "id": "pin-1",
                      "sourceMessageId": "message-9",
                      "text": { "english": "hi", "chinese": "你好" },
                      "createdAt": 1712000000000,
                      "pinnedByUser": true
                    }
                  ]
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val pins = client.listCompanionMemoryPins(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            cardId = "card-aria",
        )
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/api/companions/card-aria/memory/pins", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertEquals(1, pins.pins.size)
        assertEquals("pin-1", pins.pins.single().id)
    }

    @Test
    fun `listCompanionMemoryPins raises on 404 when card missing`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        var thrown: Throwable? = null
        try {
            client.listCompanionMemoryPins(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                cardId = "card-missing",
            )
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `createCompanionMemoryPin posts pin body and decodes response`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {
                  "id": "pin-new-1",
                  "sourceMessageId": "message-9",
                  "text": { "english": "hi", "chinese": "你好" },
                  "createdAt": 1712000000000,
                  "pinnedByUser": true
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val created = client.createCompanionMemoryPin(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            cardId = "card-aria",
            pin = CompanionMemoryPinDto(
                id = "",
                sourceMessageId = "message-9",
                text = LocalizedTextDto("hi", "你好"),
            ),
        )
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/companions/card-aria/memory/pins", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"sourceMessageId\":\"message-9\""))
        assertTrue(body.contains("\"english\":\"hi\""))
        assertEquals("pin-new-1", created.id)
    }

    @Test
    fun `createCompanionMemoryPin raises on 404 when card missing`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        var thrown: Throwable? = null
        try {
            client.createCompanionMemoryPin(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                cardId = "card-missing",
                pin = CompanionMemoryPinDto(
                    id = "",
                    text = LocalizedTextDto("hi", "你好"),
                ),
            )
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `createCompanionMemoryPin raises on 409 when pin cap reached`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(409).setBody(
                """{"error":"pin_cap_reached"}""",
            ).addHeader("Content-Type", "application/json"),
        )

        var thrown: Throwable? = null
        try {
            client.createCompanionMemoryPin(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                cardId = "card-aria",
                pin = CompanionMemoryPinDto(
                    id = "",
                    text = LocalizedTextDto("overflow", "溢出"),
                ),
            )
            fail("expected exception on 409 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `updateCompanionMemoryPin posts to pin path with body`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "id": "pin-1",
                  "sourceMessageId": null,
                  "text": { "english": "updated", "chinese": "已更新" },
                  "createdAt": 1712000000000,
                  "pinnedByUser": true
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val updated = client.updateCompanionMemoryPin(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            cardId = "card-aria",
            pin = CompanionMemoryPinDto(
                id = "pin-1",
                text = LocalizedTextDto("updated", "已更新"),
                createdAt = 1712000000000L,
                pinnedByUser = true,
            ),
        )
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/companions/card-aria/memory/pins/pin-1", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertTrue(request.body.readUtf8().contains("\"english\":\"updated\""))
        assertEquals("updated", updated.text.english)
    }

    @Test
    fun `updateCompanionMemoryPin raises on 404 when pin missing`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        var thrown: Throwable? = null
        try {
            client.updateCompanionMemoryPin(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                cardId = "card-aria",
                pin = CompanionMemoryPinDto(
                    id = "pin-missing",
                    text = LocalizedTextDto("x", "x"),
                ),
            )
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `deleteCompanionMemoryPin posts to delete sub-route`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))

        client.deleteCompanionMemoryPin(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            cardId = "card-aria",
            pinId = "pin-1",
        )
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/companions/card-aria/memory/pins/pin-1/delete", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
    }

    @Test
    fun `deleteCompanionMemoryPin raises on 404 when pin missing`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        var thrown: Throwable? = null
        try {
            client.deleteCompanionMemoryPin(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                cardId = "card-aria",
                pinId = "pin-missing",
            )
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `listPresets returns library and optional active id`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "presets": [
                    {
                      "id": "preset-default",
                      "displayName": { "english": "Default", "chinese": "默认" },
                      "isBuiltIn": true
                    },
                    {
                      "id": "preset-roleplay",
                      "displayName": { "english": "Roleplay", "chinese": "角色扮演" },
                      "isBuiltIn": true,
                      "isActive": true
                    }
                  ],
                  "activePresetId": "preset-roleplay"
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val list = client.listPresets(server.url("").toString(), "session-token-2")
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/api/presets", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertEquals(2, list.presets.size)
        assertEquals("preset-roleplay", list.activePresetId)
    }

    @Test
    fun `createPreset posts preset body and returns new id`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {
                  "id": "preset-new-1",
                  "displayName": { "english": "Tuned", "chinese": "调优" },
                  "description": { "english": "", "chinese": "" },
                  "isBuiltIn": false,
                  "isActive": false,
                  "createdAt": 1700000000,
                  "updatedAt": 1700000000
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val created = client.createPreset(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            preset = PresetDto(
                id = "",
                displayName = LocalizedTextDto("Tuned", "调优"),
            ),
        )
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/presets", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertTrue(request.body.readUtf8().contains("\"english\":\"Tuned\""))
        assertEquals("preset-new-1", created.id)
    }

    @Test
    fun `createPreset raises on 409 when displayName collides`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(409).setBody(
                """{"error":"preset_name_taken"}""",
            ).addHeader("Content-Type", "application/json"),
        )

        var thrown: Throwable? = null
        try {
            client.createPreset(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                preset = PresetDto(
                    id = "",
                    displayName = LocalizedTextDto("Default", "默认"),
                ),
            )
            fail("expected exception on 409 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `updatePreset posts to preset id path with body`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "id": "preset-user-1",
                  "displayName": { "english": "Renamed", "chinese": "重命名" },
                  "description": { "english": "", "chinese": "" },
                  "isBuiltIn": false,
                  "isActive": false,
                  "createdAt": 1,
                  "updatedAt": 9
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val updated = client.updatePreset(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            preset = PresetDto(
                id = "preset-user-1",
                displayName = LocalizedTextDto("Renamed", "重命名"),
                updatedAt = 9,
            ),
        )
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/presets/preset-user-1", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertTrue(request.body.readUtf8().contains("\"english\":\"Renamed\""))
        assertEquals("preset-user-1", updated.id)
        assertEquals(9, updated.updatedAt)
    }

    @Test
    fun `updatePreset raises on 404 when preset missing`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        var thrown: Throwable? = null
        try {
            client.updatePreset(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                preset = PresetDto(
                    id = "preset-missing",
                    displayName = LocalizedTextDto("x", "x"),
                ),
            )
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `deletePreset posts to delete sub-route with bearer token`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))

        client.deletePreset(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            presetId = "preset-user-1",
        )
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/presets/preset-user-1/delete", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
    }

    @Test
    fun `deletePreset raises on 409 when preset is active`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(409).setBody(
                """{"error":"preset_active","detail":"cannot delete active preset"}""",
            ).addHeader("Content-Type", "application/json"),
        )

        var thrown: Throwable? = null
        try {
            client.deletePreset(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                presetId = "preset-active",
            )
            fail("expected exception on 409 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `activatePreset posts to activate sub-route and returns new active`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "id": "preset-roleplay",
                  "displayName": { "english": "Roleplay", "chinese": "角色扮演" },
                  "description": { "english": "", "chinese": "" },
                  "isBuiltIn": true,
                  "isActive": true,
                  "createdAt": 0,
                  "updatedAt": 10
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val active = client.activatePreset(
            baseUrl = server.url("").toString(),
            token = "session-token-2",
            presetId = "preset-roleplay",
        )
        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/api/presets/preset-roleplay/activate", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertEquals("preset-roleplay", active.id)
        assertTrue(active.isActive)
    }

    @Test
    fun `activatePreset raises on 404 when preset missing`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        var thrown: Throwable? = null
        try {
            client.activatePreset(
                baseUrl = server.url("").toString(),
                token = "session-token-2",
                presetId = "preset-missing",
            )
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }

    @Test
    fun `getActivePreset returns current active preset`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "id": "preset-default",
                  "displayName": { "english": "Default", "chinese": "默认" },
                  "description": { "english": "", "chinese": "" },
                  "isBuiltIn": true,
                  "isActive": true,
                  "createdAt": 0,
                  "updatedAt": 0
                }
                """.trimIndent(),
            ).addHeader("Content-Type", "application/json"),
        )

        val active = client.getActivePreset(server.url("").toString(), "session-token-2")
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/api/presets/active", request.path)
        assertEquals("Bearer session-token-2", request.getHeader("Authorization"))
        assertEquals("preset-default", active.id)
        assertTrue(active.isActive)
        assertTrue(active.isBuiltIn)
    }

    @Test
    fun `getActivePreset raises on 404 when no active preset`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("no active"))

        var thrown: Throwable? = null
        try {
            client.getActivePreset(server.url("").toString(), "session-token-2")
            fail("expected exception on 404 response")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown != null)
    }
}
