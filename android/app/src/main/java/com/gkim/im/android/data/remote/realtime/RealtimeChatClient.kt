package com.gkim.im.android.data.remote.realtime

import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.remote.im.ImGatewayEventParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

@Serializable
private data class SendMessageCommand(
    val type: String,
    val recipientExternalId: String,
    val clientMessageId: String? = null,
    val body: String,
)

@Serializable
private data class MarkReadCommand(
    val type: String,
    val conversationId: String,
    val messageId: String,
)

class RealtimeChatClient(
    private val okHttpClient: OkHttpClient,
    private val endpoint: String,
) : RealtimeGateway {
    private val json = Json { encodeDefaults = false }
    private val _isConnected = MutableStateFlow(false)
    private val _lastFailure = MutableStateFlow<String?>(null)
    private val _events = MutableSharedFlow<ImGatewayEvent>(extraBufferCapacity = 32)

    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    override val lastFailure: StateFlow<String?> = _lastFailure.asStateFlow()
    override val events: SharedFlow<ImGatewayEvent> = _events.asSharedFlow()

    private var socket: WebSocket? = null

    override fun connect(token: String?, endpointOverride: String?) {
        if (socket != null) return
        val requestBuilder = Request.Builder().url(endpointOverride ?: endpoint)
        token?.let { requestBuilder.header("Authorization", "Bearer $it") }
        socket = okHttpClient.newWebSocket(
            requestBuilder.build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    _isConnected.value = true
                    _lastFailure.value = null
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    runCatching { ImGatewayEventParser.parse(text) }
                        .onSuccess { _events.tryEmit(it) }
                        .onFailure { _lastFailure.value = it.message ?: "Failed to parse realtime payload" }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _isConnected.value = false
                    socket = null
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    _isConnected.value = false
                    _lastFailure.value = t.message ?: "WebSocket connection failed"
                    socket = null
                }
            },
        )
    }

    override fun send(payload: String): Boolean {
        val sent = socket?.send(payload) ?: false
        if (!sent) {
            _isConnected.value = false
            _lastFailure.value = "WebSocket send failed"
        }
        return sent
    }

    override fun sendMessage(
        recipientExternalId: String,
        clientMessageId: String?,
        body: String,
    ): Boolean = send(
        json.encodeToString(
            SendMessageCommand(
                type = "message.send",
                recipientExternalId = recipientExternalId,
                clientMessageId = clientMessageId,
                body = body,
            )
        )
    )

    override fun markRead(
        conversationId: String,
        messageId: String,
    ): Boolean = send(
        json.encodeToString(
            MarkReadCommand(
                type = "message.read",
                conversationId = conversationId,
                messageId = messageId,
            )
        )
    )

    override fun disconnect() {
        socket?.close(1000, "client_shutdown")
        socket = null
        _isConnected.value = false
    }
}
