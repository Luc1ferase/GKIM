package com.gkim.im.android.data.remote.im

import com.gkim.im.android.core.model.AttachmentType
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import kotlinx.serialization.json.Json
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
}
