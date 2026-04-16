package com.gkim.im.android.data.remote.realtime

import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.remote.im.ImGatewayEventParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

@Serializable
private data class PingCommand(
    val type: String,
)

class RealtimeChatClient(
    private val okHttpClient: OkHttpClient,
    private val endpoint: String,
) : RealtimeGateway {
    private val json = Json { encodeDefaults = false }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _isConnected = MutableStateFlow(false)
    private val _lastFailure = MutableStateFlow<String?>(null)
    private val _events = MutableSharedFlow<ImGatewayEvent>(extraBufferCapacity = 32)

    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    override val lastFailure: StateFlow<String?> = _lastFailure.asStateFlow()
    override val events: SharedFlow<ImGatewayEvent> = _events.asSharedFlow()

    private var socket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var desiredToken: String? = null
    private var desiredEndpoint: String = endpoint
    private var reconnectAttempts = 0
    private var manualDisconnect = false

    override fun connect(token: String?, endpointOverride: String?) {
        desiredToken = token
        desiredEndpoint = endpointOverride ?: endpoint
        manualDisconnect = false
        if (socket != null) return
        openSocket(token = token, resolvedEndpoint = desiredEndpoint)
    }

    private fun openSocket(token: String?, resolvedEndpoint: String) {
        val requestBuilder = Request.Builder().url(resolvedEndpoint)
        token?.let { requestBuilder.header("Authorization", "Bearer $it") }
        socket = okHttpClient.newWebSocket(
            requestBuilder.build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    _isConnected.value = true
                    _lastFailure.value = null
                    reconnectAttempts = 0
                    reconnectJob?.cancel()
                    reconnectJob = null
                    startHeartbeat()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    runCatching { ImGatewayEventParser.parse(text) }
                        .onSuccess { _events.tryEmit(it) }
                        .onFailure { _lastFailure.value = it.message ?: "Failed to parse realtime payload" }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                    _isConnected.value = false
                    socket = null
                    heartbeatJob?.cancel()
                    heartbeatJob = null
                    scheduleReconnectIfNeeded()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _isConnected.value = false
                    socket = null
                    heartbeatJob?.cancel()
                    heartbeatJob = null
                    scheduleReconnectIfNeeded()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    _isConnected.value = false
                    _lastFailure.value = t.message ?: "WebSocket connection failed"
                    socket = null
                    heartbeatJob?.cancel()
                    heartbeatJob = null
                    scheduleReconnectIfNeeded()
                }
            },
        )
    }

    override fun send(payload: String): Boolean {
        val sent = socket?.send(payload) ?: false
        if (!sent) {
            _isConnected.value = false
            _lastFailure.value = "WebSocket send failed"
            scheduleReconnectIfNeeded()
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
        manualDisconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        socket?.close(1000, "client_shutdown")
        socket = null
        _isConnected.value = false
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(15_000)
                if (!_isConnected.value) continue
                send(json.encodeToString(PingCommand(type = "ping")))
            }
        }
    }

    private fun scheduleReconnectIfNeeded() {
        if (manualDisconnect || desiredToken.isNullOrBlank() || reconnectJob != null) return
        reconnectJob = scope.launch {
            val delayMs = (250L * (reconnectAttempts + 1)).coerceAtMost(2_000L)
            reconnectAttempts += 1
            delay(delayMs)
            reconnectJob = null
            if (!manualDisconnect && socket == null) {
                openSocket(token = desiredToken, resolvedEndpoint = desiredEndpoint)
            }
        }
    }
}
