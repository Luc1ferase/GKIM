package com.gkim.im.android.data.remote.im

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.AttachmentType
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Lorebook
import com.gkim.im.android.core.model.LorebookBinding
import com.gkim.im.android.core.model.LorebookEntry
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.SecondaryGate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImBackendPayloadsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `dev session and bootstrap payloads map into android conversation state`() {
        val session = json.decodeFromString<DevSessionResponseDto>(
            """
            {
              "token": "session-token-1",
              "expiresAt": "2026-04-15T10:00:00Z",
              "user": {
                "id": "user-nox",
                "externalId": "nox-dev",
                "displayName": "Nox Dev",
                "title": "IM Milestone Owner",
                "avatarText": "NX"
              }
            }
            """.trimIndent(),
        )

        assertEquals("session-token-1", session.token)
        assertEquals("nox-dev", session.user.externalId)

        val bootstrap = json.decodeFromString<BootstrapBundleDto>(
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
              "conversations": [
                {
                  "conversationId": "conversation-1",
                  "contact": {
                    "userId": "user-leo",
                    "externalId": "leo-vance",
                    "displayName": "Leo Vance",
                    "title": "Realtime Systems",
                    "avatarText": "LV",
                    "addedAt": "2026-04-08T08:58:00Z"
                  },
                  "unreadCount": 2,
                  "lastMessage": {
                    "id": "message-1",
                    "conversationId": "conversation-1",
                    "senderUserId": "user-leo",
                    "senderExternalId": "leo-vance",
                    "kind": "text",
                    "body": "Hello Nox",
                    "createdAt": "2026-04-08T09:00:00Z",
                    "deliveredAt": "2026-04-08T09:00:01Z",
                    "readAt": null
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        val mapped = bootstrap.toBootstrapState(activeUserExternalId = session.user.externalId)

        assertEquals("nox-dev", mapped.user.externalId)
        assertEquals(1, mapped.contacts.size)
        assertEquals("leo-vance", mapped.contacts.first().id)
        assertFalse(mapped.contacts.first().isOnline)

        val conversation = mapped.conversations.single()
        assertEquals("conversation-1", conversation.id)
        assertEquals("leo-vance", conversation.contactId)
        assertEquals("Leo Vance", conversation.contactName)
        assertEquals("Hello Nox", conversation.lastMessage)
        assertEquals("2026-04-08T09:00:00Z", conversation.lastTimestamp)
        assertEquals(2, conversation.unreadCount)
        assertEquals(1, conversation.messages.size)
        assertEquals(MessageDirection.Incoming, conversation.messages.single().direction)
        assertEquals(MessageKind.Text, conversation.messages.single().kind)
        assertEquals("2026-04-08T09:00:01Z", conversation.messages.single().deliveredAt)
        assertNull(conversation.messages.single().readAt)
    }

    @Test
    fun `history payload maps backend receipt timestamps and sender direction`() {
        val history = json.decodeFromString<MessageHistoryPageDto>(
            """
            {
              "conversationId": "conversation-1",
              "messages": [
                {
                  "id": "message-1",
                  "conversationId": "conversation-1",
                  "senderUserId": "user-nox",
                  "senderExternalId": "nox-dev",
                  "kind": "text",
                  "body": "First outbound",
                  "createdAt": "2026-04-08T09:01:00Z",
                  "deliveredAt": "2026-04-08T09:01:01Z",
                  "readAt": "2026-04-08T09:01:03Z"
                },
                {
                  "id": "message-2",
                  "conversationId": "conversation-1",
                  "senderUserId": "user-leo",
                  "senderExternalId": "leo-vance",
                  "kind": "text",
                  "body": "Inbound reply",
                  "createdAt": "2026-04-08T09:02:00Z",
                  "deliveredAt": null,
                  "readAt": null
                }
              ],
              "hasMore": true
            }
            """.trimIndent(),
        )

        val messages = history.toChatMessages(activeUserExternalId = "nox-dev")

        assertTrue(history.hasMore)
        assertEquals(2, messages.size)
        assertEquals(MessageDirection.Outgoing, messages[0].direction)
        assertEquals("2026-04-08T09:01:01Z", messages[0].deliveredAt)
        assertEquals("2026-04-08T09:01:03Z", messages[0].readAt)
        assertEquals(MessageDirection.Incoming, messages[1].direction)
        assertEquals("Inbound reply", messages[1].body)
    }

    @Test
    fun `gateway parser emits typed session and message events`() {
        val registered = ImGatewayEventParser.parse(
            """
            {
              "type": "session.registered",
              "connectionId": "ws-11",
              "activeConnections": 1,
              "user": {
                "id": "user-nox",
                "externalId": "nox-dev",
                "displayName": "Nox Dev",
                "title": "IM Milestone Owner",
                "avatarText": "NX"
              }
            }
            """.trimIndent(),
        )
        assertTrue(registered is ImGatewayEvent.SessionRegistered)
        assertEquals("ws-11", (registered as ImGatewayEvent.SessionRegistered).connectionId)

        val received = ImGatewayEventParser.parse(
            """
            {
              "type": "message.received",
              "conversationId": "conversation-1",
              "unreadCount": 3,
              "message": {
                "id": "message-9",
                "conversationId": "conversation-1",
                "senderUserId": "user-leo",
                "senderExternalId": "leo-vance",
                "kind": "text",
                "body": "Realtime hello",
                "createdAt": "2026-04-08T09:03:00Z",
                "deliveredAt": null,
                "readAt": null
              }
            }
            """.trimIndent(),
        )
        assertTrue(received is ImGatewayEvent.MessageReceived)
        received as ImGatewayEvent.MessageReceived
        assertEquals(3, received.unreadCount)
        assertEquals("Realtime hello", received.message.body)
        assertEquals(MessageDirection.Incoming, received.message.toChatMessage(activeUserExternalId = "nox-dev").direction)

        val read = ImGatewayEventParser.parse(
            """
            {
              "type": "message.read",
              "conversationId": "conversation-1",
              "messageId": "message-9",
              "readerExternalId": "leo-vance",
              "unreadCount": 0,
              "readAt": "2026-04-08T09:03:04Z"
            }
            """.trimIndent(),
        )
        assertTrue(read is ImGatewayEvent.MessageRead)
        assertEquals("2026-04-08T09:03:04Z", (read as ImGatewayEvent.MessageRead).readAt)

        val error = ImGatewayEventParser.parse(
            """
            {
              "type": "error",
              "code": "message.send_failed",
              "message": "recipient missing"
            }
            """.trimIndent(),
        )
        assertTrue(error is ImGatewayEvent.Error)
        assertEquals("message.send_failed", (error as ImGatewayEvent.Error).code)
    }

    @Test
    fun `message payload keeps attachment descriptor when mapped to chat state`() {
        val history = json.decodeFromString<MessageHistoryPageDto>(
            """
            {
              "conversationId": "conversation-1",
              "messages": [
                {
                  "id": "message-image-1",
                  "conversationId": "conversation-1",
                  "senderUserId": "user-leo",
                  "senderExternalId": "leo-vance",
                  "kind": "text",
                  "body": "",
                  "createdAt": "2026-04-08T09:04:00Z",
                  "deliveredAt": null,
                  "readAt": null,
                  "attachment": {
                    "type": "image",
                    "contentType": "image/png",
                    "fetchPath": "/api/messages/message-image-1/attachment",
                    "sizeBytes": 11
                  }
                }
              ],
              "hasMore": false
            }
            """.trimIndent(),
        )

        val message = history.toChatMessages(activeUserExternalId = "nox-dev").single()

        assertEquals(MessageDirection.Incoming, message.direction)
        assertEquals(AttachmentType.Image, message.attachment?.type)
        assertEquals("/api/messages/message-image-1/attachment", message.attachment?.preview)
    }

    @Test
    fun `companion turn submit request round-trips through kotlinx serialization`() {
        val request = CompanionTurnSubmitRequestDto(
            conversationId = "conversation-1",
            activeCompanionId = "architect-oracle",
            userTurnBody = "Tell me a story about the tavern",
            activeLanguage = "en",
            clientTurnId = "client-turn-001",
            parentMessageId = "message-root",
        )

        val encoded = json.encodeToString(CompanionTurnSubmitRequestDto.serializer(), request)
        val decoded = json.decodeFromString<CompanionTurnSubmitRequestDto>(encoded)

        assertEquals(request, decoded)
    }

    @Test
    fun `companion turn pending list payload decodes record shape`() {
        val pending = json.decodeFromString<CompanionTurnPendingListDto>(
            """
            {
              "turns": [
                {
                  "turnId": "turn-42",
                  "conversationId": "conversation-1",
                  "messageId": "message-9",
                  "variantGroupId": "vg-3",
                  "variantIndex": 0,
                  "parentMessageId": "message-8",
                  "status": "streaming",
                  "accumulatedBody": "The tavern door creaks",
                  "lastDeltaSeq": 7,
                  "providerId": "openai",
                  "model": "gpt-4o-mini",
                  "startedAt": "2026-04-21T08:00:00Z"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(1, pending.turns.size)
        val turn = pending.turns.single()
        assertEquals("turn-42", turn.turnId)
        assertEquals("streaming", turn.status)
        assertEquals(7, turn.lastDeltaSeq)
        assertEquals("The tavern door creaks", turn.accumulatedBody)
        assertNull(turn.completedAt)
        assertNull(turn.blockReason)
    }

    @Test
    fun `gateway parser emits companion_turn started event`() {
        val started = ImGatewayEventParser.parse(
            """
            {
              "type": "companion_turn.started",
              "turnId": "turn-100",
              "conversationId": "conversation-1",
              "messageId": "message-100",
              "variantGroupId": "vg-5",
              "variantIndex": 0,
              "parentMessageId": "message-99",
              "providerId": "openai",
              "model": "gpt-4o-mini"
            }
            """.trimIndent(),
        )
        assertTrue(started is ImGatewayEvent.CompanionTurnStarted)
        started as ImGatewayEvent.CompanionTurnStarted
        assertEquals("turn-100", started.turnId)
        assertEquals("vg-5", started.variantGroupId)
        assertEquals(0, started.variantIndex)
        assertEquals("openai", started.providerId)
    }

    @Test
    fun `gateway parser emits companion_turn delta event with monotonic deltaSeq`() {
        val delta = ImGatewayEventParser.parse(
            """
            {
              "type": "companion_turn.delta",
              "turnId": "turn-100",
              "conversationId": "conversation-1",
              "messageId": "message-100",
              "deltaSeq": 3,
              "textDelta": "The tavern fire crackles"
            }
            """.trimIndent(),
        )
        assertTrue(delta is ImGatewayEvent.CompanionTurnDelta)
        delta as ImGatewayEvent.CompanionTurnDelta
        assertEquals(3, delta.deltaSeq)
        assertEquals("The tavern fire crackles", delta.textDelta)
    }

    @Test
    fun `gateway parser emits companion_turn completed event with final body`() {
        val completed = ImGatewayEventParser.parse(
            """
            {
              "type": "companion_turn.completed",
              "turnId": "turn-100",
              "conversationId": "conversation-1",
              "messageId": "message-100",
              "finalBody": "The tavern fire crackles warmly.",
              "completedAt": "2026-04-21T08:01:00Z"
            }
            """.trimIndent(),
        )
        assertTrue(completed is ImGatewayEvent.CompanionTurnCompleted)
        completed as ImGatewayEvent.CompanionTurnCompleted
        assertEquals("turn-100", completed.turnId)
        assertEquals("The tavern fire crackles warmly.", completed.finalBody)
        assertEquals("2026-04-21T08:01:00Z", completed.completedAt)
    }

    @Test
    fun `gateway parser emits companion_turn failed event with subtype`() {
        val failed = ImGatewayEventParser.parse(
            """
            {
              "type": "companion_turn.failed",
              "turnId": "turn-101",
              "conversationId": "conversation-1",
              "messageId": "message-101",
              "subtype": "timeout",
              "errorMessage": "idle for 15s"
            }
            """.trimIndent(),
        )
        assertTrue(failed is ImGatewayEvent.CompanionTurnFailed)
        failed as ImGatewayEvent.CompanionTurnFailed
        assertEquals("timeout", failed.subtype)
        assertEquals("idle for 15s", failed.errorMessage)
    }

    @Test
    fun `gateway parser emits companion_turn blocked event with typed reason`() {
        val blocked = ImGatewayEventParser.parse(
            """
            {
              "type": "companion_turn.blocked",
              "turnId": "turn-102",
              "conversationId": "conversation-1",
              "messageId": "message-102",
              "reason": "nsfw_denied"
            }
            """.trimIndent(),
        )
        assertTrue(blocked is ImGatewayEvent.CompanionTurnBlocked)
        blocked as ImGatewayEvent.CompanionTurnBlocked
        assertEquals("nsfw_denied", blocked.reason)
    }

    @Test
    fun `companion roster payload maps bilingual companion fields into android card model`() {
        val roster = json.decodeFromString<CompanionRosterDto>(
            """
            {
              "presetCharacters": [
                {
                  "id": "architect-oracle",
                  "displayName": {
                    "english": "Architect Oracle",
                    "chinese": "筑谕师"
                  },
                  "roleLabel": {
                    "english": "Calm Strategist",
                    "chinese": "冷静策士"
                  },
                  "summary": {
                    "english": "A precise companion who turns messy feelings into structured plans and gentle next steps.",
                    "chinese": "把纷乱感受整理成清晰计划，并陪你迈出下一步的精确同伴。"
                  },
                  "openingLine": {
                    "english": "I have been waiting in the tavern. Tell me what kind of night this is.",
                    "chinese": "我一直在酒馆等你。今晚是什么样的夜色，说给我听。"
                  },
                  "avatarText": "AO",
                  "accent": "primary",
                  "source": "preset"
                }
              ],
              "ownedCharacters": [],
              "activeCharacterId": "architect-oracle"
            }
            """.trimIndent(),
        )

        val card = roster.presetCharacters.single().toCompanionCharacterCard()

        assertEquals("Architect Oracle", card.displayName.english)
        assertEquals("筑谕师", card.displayName.chinese)
        assertEquals("Calm Strategist", card.roleLabel.english)
        assertEquals("冷静策士", card.roleLabel.chinese)
        assertEquals("我一直在酒馆等你。今晚是什么样的夜色，说给我听。", card.firstMes.chinese)
    }

    @Test
    fun `card import upload request round-trips through kotlinx serialization`() {
        val request = CardImportUploadRequestDto(
            filename = "aria.png",
            contentBase64 = "iVBORw0KGgoAAA==",
            claimedFormat = "png",
        )
        val encoded = json.encodeToString(CardImportUploadRequestDto.serializer(), request)
        val decoded = json.decodeFromString<CardImportUploadRequestDto>(encoded)
        assertEquals(request, decoded)
    }

    @Test
    fun `card import preview payload decodes deep record plus warnings plus st extensions passthrough`() {
        val preview = json.decodeFromString<CardImportPreviewDto>(
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
                "extensions": {
                  "st": {
                    "stPostHistoryInstructions": "Stay in character.",
                    "stDepthPrompt": "You live deep in the woods.",
                    "stTranslationPending": ["firstMes"]
                  }
                }
              },
              "detectedLanguage": "en",
              "warnings": [
                {"code":"field_truncated","field":"personality","detail":"exceeded 32 KiB"},
                {"code":"avatar_discarded","field":"avatar","detail":"> 4096x4096"},
                {"code":"alt_greetings_trimmed","detail":"kept first 64"},
                {"code":"tags_trimmed"},
                {"code":"extension_dropped","field":"st.unknown_large"},
                {"code":"st_translation_pending","field":"firstMes"},
                {"code":"post_history_instruction_parked","field":"stPostHistoryInstructions"}
              ],
              "stExtensionKeys": ["stPostHistoryInstructions","stDepthPrompt","stTranslationPending"]
            }
            """.trimIndent(),
        )

        assertEquals("preview-xyz-1", preview.previewToken)
        assertEquals("en", preview.detectedLanguage)
        assertEquals("card-import-1", preview.card.id)
        assertEquals("Hello traveller.", preview.card.firstMes?.english)
        assertEquals(7, preview.warnings.size)
        val codes = preview.warnings.map { it.code }
        assertTrue(codes.contains("field_truncated"))
        assertTrue(codes.contains("avatar_discarded"))
        assertTrue(codes.contains("alt_greetings_trimmed"))
        assertTrue(codes.contains("tags_trimmed"))
        assertTrue(codes.contains("extension_dropped"))
        assertTrue(codes.contains("st_translation_pending"))
        assertTrue(codes.contains("post_history_instruction_parked"))
        assertEquals("personality", preview.warnings.first { it.code == "field_truncated" }.field)
        assertNull(preview.warnings.first { it.code == "tags_trimmed" }.field)
        assertEquals(listOf("stPostHistoryInstructions", "stDepthPrompt", "stTranslationPending"), preview.stExtensionKeys)

        val reEncoded = json.encodeToString(CardImportPreviewDto.serializer(), preview)
        val reDecoded = json.decodeFromString<CardImportPreviewDto>(reEncoded)
        assertEquals(preview, reDecoded)
        assertTrue(reDecoded.card.extensions.containsKey("st"))
    }

    @Test
    fun `card import commit request round-trips through kotlinx serialization`() {
        val commit = CardImportCommitRequestDto(
            previewToken = "preview-xyz-1",
            card = CompanionCharacterCardDto(
                id = "card-import-1",
                displayName = LocalizedTextDto("Aria", "Aria"),
                roleLabel = LocalizedTextDto("Guide", "向导"),
                summary = LocalizedTextDto("Calm guide", "Calm guide"),
                firstMes = LocalizedTextDto("Hello traveller.", "你好，旅人。"),
                avatarText = "AR",
                accent = "primary",
                source = "userauthored",
            ),
            languageOverride = "zh",
        )
        val encoded = json.encodeToString(CardImportCommitRequestDto.serializer(), commit)
        val decoded = json.decodeFromString<CardImportCommitRequestDto>(encoded)
        assertEquals(commit, decoded)
    }

    @Test
    fun `card export request and response round-trip through kotlinx serialization`() {
        val request = CardExportRequestDto(
            format = "png",
            language = "zh",
            includeTranslationAlt = true,
        )
        val encodedRequest = json.encodeToString(CardExportRequestDto.serializer(), request)
        val decodedRequest = json.decodeFromString<CardExportRequestDto>(encodedRequest)
        assertEquals(request, decodedRequest)

        val pngResponse = CardExportResponseDto(
            format = "png",
            filename = "aria.png",
            contentType = "image/png",
            encoding = "base64",
            payload = "iVBORw0KGgoAAA==",
        )
        val decodedPng = json.decodeFromString<CardExportResponseDto>(
            json.encodeToString(CardExportResponseDto.serializer(), pngResponse),
        )
        assertEquals(pngResponse, decodedPng)

        val jsonResponse = CardExportResponseDto(
            format = "json",
            filename = "aria.json",
            contentType = "application/json",
            encoding = "utf8",
            payload = "{\"name\":\"Aria\"}",
        )
        val decodedJson = json.decodeFromString<CardExportResponseDto>(
            json.encodeToString(CardExportResponseDto.serializer(), jsonResponse),
        )
        assertEquals(jsonResponse, decodedJson)
    }

    @Test
    fun `user persona dto carries bilingual names and extensions passthrough`() {
        val raw = """
            {
              "id": "persona-1",
              "displayName": { "english": "Traveller", "chinese": "旅人" },
              "description": { "english": "A curious wanderer.", "chinese": "好奇的漫游者。" },
              "isBuiltIn": false,
              "isActive": true,
              "createdAt": 1700000000,
              "updatedAt": 1700001234,
              "extensions": { "nickname": "T" }
            }
        """.trimIndent()
        val dto = json.decodeFromString<UserPersonaDto>(raw)

        assertEquals("persona-1", dto.id)
        assertEquals("Traveller", dto.displayName.english)
        assertEquals("旅人", dto.displayName.chinese)
        assertEquals("好奇的漫游者。", dto.description.chinese)
        assertFalse(dto.isBuiltIn)
        assertTrue(dto.isActive)
        assertEquals(1_700_000_000L, dto.createdAt)
        assertEquals(1_700_001_234L, dto.updatedAt)
        assertEquals(
            "T",
            dto.extensions["nickname"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content },
        )

        val persona = dto.toUserPersona()
        assertEquals("Traveller", persona.displayName.english)
        assertEquals("旅人", persona.displayName.chinese)
        assertTrue(persona.isActive)

        val encoded = json.encodeToString(UserPersonaDto.serializer(), dto)
        val decoded = json.decodeFromString<UserPersonaDto>(encoded)
        assertEquals(dto, decoded)
    }

    @Test
    fun `user persona list dto carries active id and persona list`() {
        val dto = UserPersonaListDto(
            personas = listOf(
                UserPersonaDto(
                    id = "persona-built-in",
                    displayName = LocalizedTextDto("User", "用户"),
                    description = LocalizedTextDto(
                        "A user interacting with the companion.",
                        "与同伴互动的用户。",
                    ),
                    isBuiltIn = true,
                    isActive = true,
                ),
                UserPersonaDto(
                    id = "persona-alpha",
                    displayName = LocalizedTextDto("Aria", "艾莉亚"),
                    description = LocalizedTextDto("Calm guide.", "沉稳向导。"),
                ),
            ),
            activePersonaId = "persona-built-in",
        )
        val encoded = json.encodeToString(UserPersonaListDto.serializer(), dto)
        val decoded = json.decodeFromString<UserPersonaListDto>(encoded)
        assertEquals(dto, decoded)
        assertEquals("persona-built-in", decoded.activePersonaId)
        assertEquals(2, decoded.personas.size)
    }

    @Test
    fun `user persona activate request forwards persona id`() {
        val dto = UserPersonaActivateRequestDto(personaId = "persona-alpha")
        val encoded = json.encodeToString(UserPersonaActivateRequestDto.serializer(), dto)
        val decoded = json.decodeFromString<UserPersonaActivateRequestDto>(encoded)
        assertEquals(dto, decoded)
        assertEquals("persona-alpha", decoded.personaId)
    }

    @Test
    fun `user persona dto round trip through fromUserPersona and back preserves every field`() {
        val domain = com.gkim.im.android.core.model.UserPersona(
            id = "persona-round",
            displayName = com.gkim.im.android.core.model.LocalizedText("Pilgrim", "朝圣者"),
            description = com.gkim.im.android.core.model.LocalizedText(
                "Walks many roads.",
                "走过许多道路。",
            ),
            isBuiltIn = false,
            isActive = true,
            createdAt = 1_700_000_000L,
            updatedAt = 1_700_001_234L,
            extensions = kotlinx.serialization.json.buildJsonObject {
                put("nickname", kotlinx.serialization.json.JsonPrimitive("P"))
            },
        )
        val dto = UserPersonaDto.fromUserPersona(domain)
        val encoded = json.encodeToString(UserPersonaDto.serializer(), dto)
        val decoded = json.decodeFromString<UserPersonaDto>(encoded)
        val roundTripped = decoded.toUserPersona()
        assertEquals(domain, roundTripped)
    }

    @Test
    fun `lorebook dto carries bilingual fields and extensions passthrough`() {
        val raw = """
            {
              "id": "lorebook-atlas",
              "ownerId": "user-nox",
              "displayName": { "english": "World Atlas", "chinese": "世界图鉴" },
              "description": { "english": "Cross-character notes.", "chinese": "跨角色的笔记。" },
              "isGlobal": true,
              "isBuiltIn": false,
              "tokenBudget": 2048,
              "extensions": { "st": { "custom": "from ST" } },
              "createdAt": 1700000000,
              "updatedAt": 1700001234
            }
        """.trimIndent()
        val dto = json.decodeFromString<LorebookDto>(raw)

        assertEquals("lorebook-atlas", dto.id)
        assertEquals("user-nox", dto.ownerId)
        assertEquals("World Atlas", dto.displayName.english)
        assertEquals("世界图鉴", dto.displayName.chinese)
        assertEquals("跨角色的笔记。", dto.description.chinese)
        assertTrue(dto.isGlobal)
        assertEquals(2048, dto.tokenBudget)
        assertEquals(1_700_000_000L, dto.createdAt)
        assertTrue(dto.extensions.containsKey("st"))

        val lorebook = dto.toLorebook()
        assertEquals("lorebook-atlas", lorebook.id)
        assertEquals("World Atlas", lorebook.displayName.english)
        assertEquals(2048, lorebook.tokenBudget)

        val encoded = json.encodeToString(LorebookDto.serializer(), dto)
        val decoded = json.decodeFromString<LorebookDto>(encoded)
        assertEquals(dto, decoded)
    }

    @Test
    fun `lorebook dto defaults absent optional fields to sensible values`() {
        val raw = """
            {
              "id": "lb-minimal",
              "ownerId": "user-nox",
              "displayName": { "english": "Minimal", "chinese": "精简" }
            }
        """.trimIndent()
        val dto = json.decodeFromString<LorebookDto>(raw)

        assertEquals("", dto.description.english)
        assertFalse(dto.isGlobal)
        assertFalse(dto.isBuiltIn)
        assertEquals(Lorebook.DefaultTokenBudget, dto.tokenBudget)
        assertEquals(0L, dto.createdAt)
        assertTrue(dto.extensions.isEmpty())
    }

    @Test
    fun `lorebook dto round trip through fromLorebook and back preserves every field`() {
        val domain = Lorebook(
            id = "lorebook-atlas",
            ownerId = "user-nox",
            displayName = LocalizedText("World Atlas", "世界图鉴"),
            description = LocalizedText("Cross-character notes.", "跨角色的笔记。"),
            isGlobal = true,
            isBuiltIn = false,
            tokenBudget = 2048,
            extensions = buildJsonObject { put("st_probability", JsonPrimitive(100)) },
            createdAt = 1_700_000_000L,
            updatedAt = 1_700_001_234L,
        )
        val dto = LorebookDto.fromLorebook(domain)
        val encoded = json.encodeToString(LorebookDto.serializer(), dto)
        val decoded = json.decodeFromString<LorebookDto>(encoded)
        assertEquals(domain, decoded.toLorebook())
    }

    @Test
    fun `lorebook list dto wraps collection`() {
        val listDto = LorebookListDto(
            lorebooks = listOf(
                LorebookDto(
                    id = "lb-1",
                    ownerId = "user-nox",
                    displayName = LocalizedTextDto("One", "一"),
                ),
                LorebookDto(
                    id = "lb-2",
                    ownerId = "user-nox",
                    displayName = LocalizedTextDto("Two", "二"),
                    isGlobal = true,
                ),
            ),
        )
        val encoded = json.encodeToString(LorebookListDto.serializer(), listDto)
        val decoded = json.decodeFromString<LorebookListDto>(encoded)
        assertEquals(listDto, decoded)
        assertEquals(2, decoded.lorebooks.size)
        assertTrue(decoded.lorebooks[1].isGlobal)
    }

    @Test
    fun `lorebook summary dto feeds the settings badge`() {
        val summary = LorebookSummaryDto(
            id = "lb-1",
            displayName = LocalizedTextDto("Atlas", "图鉴"),
            entryCount = 7,
            isGlobal = true,
        )
        val encoded = json.encodeToString(LorebookSummaryDto.serializer(), summary)
        val decoded = json.decodeFromString<LorebookSummaryDto>(encoded)
        assertEquals(summary, decoded)
        assertEquals(7, decoded.entryCount)
    }

    @Test
    fun `bootstrap bundle carries lorebook summaries for the Settings badge`() {
        val bootstrap = json.decodeFromString<BootstrapBundleDto>(
            """
            {
              "user": {
                "id": "user-nox",
                "externalId": "nox-dev",
                "displayName": "Nox Dev",
                "title": "IM Milestone Owner",
                "avatarText": "NX"
              },
              "contacts": [],
              "conversations": [],
              "lorebookSummaries": [
                {
                  "id": "lb-1",
                  "displayName": { "english": "Atlas", "chinese": "图鉴" },
                  "entryCount": 3,
                  "isGlobal": false
                },
                {
                  "id": "lb-2",
                  "displayName": { "english": "Global notes", "chinese": "全局笔记" },
                  "entryCount": 12,
                  "isGlobal": true
                }
              ]
            }
            """.trimIndent(),
        )
        assertEquals(2, bootstrap.lorebookSummaries.size)
        assertEquals("lb-1", bootstrap.lorebookSummaries[0].id)
        assertEquals(3, bootstrap.lorebookSummaries[0].entryCount)
        assertTrue(bootstrap.lorebookSummaries[1].isGlobal)
    }

    @Test
    fun `bootstrap bundle defaults lorebook summaries to empty when absent`() {
        val bootstrap = json.decodeFromString<BootstrapBundleDto>(
            """
            {
              "user": {
                "id": "user-nox",
                "externalId": "nox-dev",
                "displayName": "Nox Dev",
                "title": "IM Milestone Owner",
                "avatarText": "NX"
              },
              "contacts": [],
              "conversations": []
            }
            """.trimIndent(),
        )
        assertTrue(bootstrap.lorebookSummaries.isEmpty())
    }

    @Test
    fun `create lorebook request round-trips through kotlinx serialization`() {
        val request = CreateLorebookRequestDto(
            displayName = LocalizedTextDto("New Atlas", "新图鉴"),
            description = LocalizedTextDto("Notes", "笔记"),
            isGlobal = true,
            tokenBudget = 1500,
        )
        val encoded = json.encodeToString(CreateLorebookRequestDto.serializer(), request)
        val decoded = json.decodeFromString<CreateLorebookRequestDto>(encoded)
        assertEquals(request, decoded)
    }

    @Test
    fun `update lorebook request carries only changed fields via nullable opt-in`() {
        val partial = UpdateLorebookRequestDto(tokenBudget = 4096)
        val encoded = json.encodeToString(UpdateLorebookRequestDto.serializer(), partial)
        val decoded = json.decodeFromString<UpdateLorebookRequestDto>(encoded)
        assertEquals(partial, decoded)
        assertEquals(4096, decoded.tokenBudget)
        assertNull(decoded.displayName)
        assertNull(decoded.isGlobal)
    }

    @Test
    fun `per-language string list dto converts to and from language map`() {
        val dto = PerLanguageStringListDto(
            english = listOf("moon", "moons"),
            chinese = listOf("月亮", "双月"),
        )
        val map = dto.toLanguageMap()
        assertEquals(listOf("moon", "moons"), map[AppLanguage.English])
        assertEquals(listOf("月亮", "双月"), map[AppLanguage.Chinese])

        val back = PerLanguageStringListDto.fromLanguageMap(map)
        assertEquals(dto, back)
    }

    @Test
    fun `per-language string list dto omits language key when list is empty`() {
        val englishOnly = PerLanguageStringListDto(english = listOf("moon"))
        val map = englishOnly.toLanguageMap()
        assertEquals(listOf("moon"), map[AppLanguage.English])
        assertFalse(map.containsKey(AppLanguage.Chinese))
    }

    @Test
    fun `lorebook entry dto serializes each secondary gate variant and maps to domain`() {
        SecondaryGate.values().forEach { gate ->
            val dto = LorebookEntryDto(
                id = "entry-${gate.name.lowercase()}",
                lorebookId = "lb-1",
                name = LocalizedTextDto("Entry ${gate.name}", "条目 ${gate.name}"),
                secondaryGate = when (gate) {
                    SecondaryGate.None -> "NONE"
                    SecondaryGate.And -> "AND"
                    SecondaryGate.Or -> "OR"
                },
            )
            val encoded = json.encodeToString(LorebookEntryDto.serializer(), dto)
            val decoded = json.decodeFromString<LorebookEntryDto>(encoded)
            assertEquals(dto, decoded)
            assertEquals(gate, decoded.toLorebookEntry().secondaryGate)
        }
    }

    @Test
    fun `lorebook entry dto tolerates lowercase gate strings when decoding`() {
        val dto = LorebookEntryDto(
            id = "entry-1",
            lorebookId = "lb-1",
            name = LocalizedTextDto("Entry", "条目"),
            secondaryGate = "and",
        )
        val domain = dto.toLorebookEntry()
        assertEquals(SecondaryGate.And, domain.secondaryGate)
    }

    @Test
    fun `lorebook entry dto preserves per-language key lists through json round-trip`() {
        val dto = LorebookEntryDto(
            id = "entry-moons",
            lorebookId = "lb-1",
            name = LocalizedTextDto("Two moons", "双月"),
            keysByLang = PerLanguageStringListDto(
                english = listOf("moon", "moons"),
                chinese = listOf("月亮", "双月"),
            ),
            secondaryKeysByLang = PerLanguageStringListDto(
                english = listOf("night"),
                chinese = listOf("夜晚"),
            ),
            secondaryGate = "AND",
            content = LocalizedTextDto("Two moons hang above.", "天上挂着双月。"),
            constant = true,
            scanDepth = 5,
            insertionOrder = 10,
            comment = "authoring note",
            extensions = buildJsonObject { put("st_probability", JsonPrimitive(100)) },
        )
        val encoded = json.encodeToString(LorebookEntryDto.serializer(), dto)
        val decoded = json.decodeFromString<LorebookEntryDto>(encoded)
        assertEquals(dto, decoded)

        val domain = decoded.toLorebookEntry()
        assertEquals(listOf("moon", "moons"), domain.keysByLang[AppLanguage.English])
        assertEquals(listOf("月亮", "双月"), domain.keysByLang[AppLanguage.Chinese])
        assertEquals(listOf("night"), domain.secondaryKeysByLang[AppLanguage.English])
        assertEquals(SecondaryGate.And, domain.secondaryGate)
        assertTrue(domain.constant)
        assertEquals(5, domain.scanDepth)
        assertEquals(10, domain.insertionOrder)
        assertEquals(JsonPrimitive(100), domain.extensions["st_probability"])
    }

    @Test
    fun `lorebook entry dto defaults match domain spec`() {
        val raw = """
            {
              "id": "entry-minimal",
              "lorebookId": "lb-1",
              "name": { "english": "Minimal", "chinese": "精简" }
            }
        """.trimIndent()
        val dto = json.decodeFromString<LorebookEntryDto>(raw)

        assertEquals("NONE", dto.secondaryGate)
        assertEquals("", dto.content.english)
        assertTrue(dto.enabled)
        assertFalse(dto.constant)
        assertFalse(dto.caseSensitive)
        assertEquals(LorebookEntry.DefaultScanDepth, dto.scanDepth)
        assertEquals(0, dto.insertionOrder)
        assertTrue(dto.keysByLang.english.isEmpty())
        assertTrue(dto.keysByLang.chinese.isEmpty())

        val domain = dto.toLorebookEntry()
        assertEquals(SecondaryGate.None, domain.secondaryGate)
        assertEquals(LorebookEntry.DefaultScanDepth, domain.scanDepth)
        assertTrue(domain.keysByLang.isEmpty())
    }

    @Test
    fun `lorebook entry dto round trip through fromLorebookEntry preserves every field`() {
        val domain = LorebookEntry(
            id = "entry-round",
            lorebookId = "lb-1",
            name = LocalizedText("Round trip", "往返"),
            keysByLang = mapOf(
                AppLanguage.English to listOf("alpha", "beta"),
                AppLanguage.Chinese to listOf("甲", "乙"),
            ),
            secondaryKeysByLang = mapOf(
                AppLanguage.English to listOf("night"),
            ),
            secondaryGate = SecondaryGate.Or,
            content = LocalizedText("Body", "正文"),
            enabled = false,
            constant = true,
            caseSensitive = true,
            scanDepth = 7,
            insertionOrder = 15,
            comment = "c",
            extensions = buildJsonObject { put("st", JsonPrimitive("x")) },
        )
        val dto = LorebookEntryDto.fromLorebookEntry(domain)
        val encoded = json.encodeToString(LorebookEntryDto.serializer(), dto)
        val decoded = json.decodeFromString<LorebookEntryDto>(encoded)
        assertEquals(domain, decoded.toLorebookEntry())
    }

    @Test
    fun `lorebook entry list dto wraps collection`() {
        val listDto = LorebookEntryListDto(
            entries = listOf(
                LorebookEntryDto(
                    id = "e1",
                    lorebookId = "lb-1",
                    name = LocalizedTextDto("A", "甲"),
                ),
                LorebookEntryDto(
                    id = "e2",
                    lorebookId = "lb-1",
                    name = LocalizedTextDto("B", "乙"),
                ),
            ),
        )
        val encoded = json.encodeToString(LorebookEntryListDto.serializer(), listDto)
        val decoded = json.decodeFromString<LorebookEntryListDto>(encoded)
        assertEquals(listDto, decoded)
    }

    @Test
    fun `create and update lorebook entry requests round-trip`() {
        val create = CreateLorebookEntryRequestDto(
            name = LocalizedTextDto("New", "新"),
            keysByLang = PerLanguageStringListDto(english = listOf("x")),
            secondaryGate = "OR",
            content = LocalizedTextDto("body", "正文"),
            scanDepth = 2,
        )
        assertEquals(
            create,
            json.decodeFromString<CreateLorebookEntryRequestDto>(
                json.encodeToString(CreateLorebookEntryRequestDto.serializer(), create),
            ),
        )

        val update = UpdateLorebookEntryRequestDto(insertionOrder = 5, enabled = false)
        val updEncoded = json.encodeToString(UpdateLorebookEntryRequestDto.serializer(), update)
        val updDecoded = json.decodeFromString<UpdateLorebookEntryRequestDto>(updEncoded)
        assertEquals(update, updDecoded)
        assertEquals(5, updDecoded.insertionOrder)
        assertFalse(updDecoded.enabled!!)
        assertNull(updDecoded.name)
    }

    @Test
    fun `lorebook binding dto serializes primary flag and converts to domain`() {
        val dto = LorebookBindingDto(
            lorebookId = "lb-1",
            characterId = "card-aria",
            isPrimary = true,
        )
        val encoded = json.encodeToString(LorebookBindingDto.serializer(), dto)
        val decoded = json.decodeFromString<LorebookBindingDto>(encoded)
        assertEquals(dto, decoded)

        val domain = decoded.toLorebookBinding()
        assertEquals("lb-1", domain.lorebookId)
        assertEquals("card-aria", domain.characterId)
        assertTrue(domain.isPrimary)
    }

    @Test
    fun `lorebook binding dto defaults primary flag to false when absent`() {
        val dto = json.decodeFromString<LorebookBindingDto>(
            """{ "lorebookId": "lb-1", "characterId": "card-aria" }""",
        )
        assertFalse(dto.isPrimary)
    }

    @Test
    fun `lorebook binding dto round trip through fromLorebookBinding preserves fields`() {
        val domain = LorebookBinding(
            lorebookId = "lb-1",
            characterId = "card-aria",
            isPrimary = true,
        )
        val dto = LorebookBindingDto.fromLorebookBinding(domain)
        assertEquals(domain, dto.toLorebookBinding())
    }

    @Test
    fun `lorebook binding list dto wraps collection`() {
        val listDto = LorebookBindingListDto(
            bindings = listOf(
                LorebookBindingDto("lb-1", "card-aria", isPrimary = true),
                LorebookBindingDto("lb-1", "card-nova"),
            ),
        )
        val encoded = json.encodeToString(LorebookBindingListDto.serializer(), listDto)
        val decoded = json.decodeFromString<LorebookBindingListDto>(encoded)
        assertEquals(listDto, decoded)
    }

    @Test
    fun `create and update lorebook binding requests round-trip`() {
        val create = CreateLorebookBindingRequestDto(
            characterId = "card-aria",
            isPrimary = true,
        )
        assertEquals(
            create,
            json.decodeFromString<CreateLorebookBindingRequestDto>(
                json.encodeToString(CreateLorebookBindingRequestDto.serializer(), create),
            ),
        )

        val update = UpdateLorebookBindingRequestDto(isPrimary = false)
        val encoded = json.encodeToString(UpdateLorebookBindingRequestDto.serializer(), update)
        val decoded = json.decodeFromString<UpdateLorebookBindingRequestDto>(encoded)
        assertEquals(update, decoded)
    }
}
