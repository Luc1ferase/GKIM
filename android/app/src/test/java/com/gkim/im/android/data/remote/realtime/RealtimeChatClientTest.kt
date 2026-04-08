package com.gkim.im.android.data.remote.realtime

import app.cash.turbine.test
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RealtimeChatClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: RealtimeChatClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = RealtimeChatClient(OkHttpClient.Builder().build(), server.url("/ws").toString().replace("http", "ws"))
    }

    @After
    fun tearDown() {
        client.disconnect()
        runCatching { server.shutdown() }
    }

    @Test
    fun `connect sends bearer token and emits parsed gateway events`() = runTest {
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(
                        """
                        {
                          "type": "session.registered",
                          "connectionId": "ws-200",
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
                    webSocket.send(
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
                }
            }),
        )

        client.events.test {
            client.connect(token = "session-token-9")

            val registered = awaitItem()
            assertTrue(registered is ImGatewayEvent.SessionRegistered)
            assertEquals("ws-200", (registered as ImGatewayEvent.SessionRegistered).connectionId)

            val received = awaitItem()
            assertTrue(received is ImGatewayEvent.MessageReceived)
            assertEquals("Realtime hello", (received as ImGatewayEvent.MessageReceived).message.body)

            cancelAndIgnoreRemainingEvents()
        }

        val handshake = server.takeRequest()
        assertEquals("Bearer session-token-9", handshake.getHeader("Authorization"))
        assertTrue(client.isConnected.value)
        assertEquals(null, client.lastFailure.value)

        client.disconnect()
        waitUntilDisconnected()
    }

    @Test
    fun `sendMessage and markRead emit backend command payloads`() = runTest {
        val frames = LinkedBlockingQueue<String>()
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    frames.offer(text)
                }
            }),
        )

        client.connect(token = "session-token-9")
        waitUntilConnected()

        assertTrue(client.sendMessage("leo-vance", "client-1", "Ship the orbit build"))
        assertTrue(client.markRead("conversation-1", "message-9"))

        val sendFrame = frames.poll(5, TimeUnit.SECONDS)
        val readFrame = frames.poll(5, TimeUnit.SECONDS)

        assertNotNull(sendFrame)
        assertNotNull(readFrame)
        assertTrue(sendFrame!!.contains("\"type\":\"message.send\""))
        assertTrue(sendFrame.contains("\"recipientExternalId\":\"leo-vance\""))
        assertTrue(sendFrame.contains("\"clientMessageId\":\"client-1\""))
        assertTrue(sendFrame.contains("\"body\":\"Ship the orbit build\""))
        assertTrue(readFrame!!.contains("\"type\":\"message.read\""))
        assertTrue(readFrame.contains("\"conversationId\":\"conversation-1\""))
        assertTrue(readFrame.contains("\"messageId\":\"message-9\""))
    }

    @Test
    fun `socket failures surface disconnect state and error message`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        client.connect(token = "session-token-9")
        waitUntilFailure()

        assertTrue(!client.isConnected.value)
        assertNotNull(client.lastFailure.value)
    }

    private fun waitUntilConnected() {
        repeat(50) {
            if (client.isConnected.value) return
            Thread.sleep(20)
        }
        error("websocket did not connect in time")
    }

    private fun waitUntilFailure() {
        repeat(50) {
            if (client.lastFailure.value != null) return
            Thread.sleep(20)
        }
        error("websocket did not fail in time")
    }

    private fun waitUntilDisconnected() {
        repeat(50) {
            if (!client.isConnected.value) return
            Thread.sleep(20)
        }
        error("websocket did not disconnect in time")
    }
}
