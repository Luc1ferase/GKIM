package com.gkim.im.android.data.remote.realtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class RealtimeChatClient(
    private val okHttpClient: OkHttpClient,
    private val endpoint: String,
) {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var socket: WebSocket? = null

    fun connect() {
        if (socket != null) return
        socket = okHttpClient.newWebSocket(
            Request.Builder().url(endpoint).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    _isConnected.value = true
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _isConnected.value = false
                    socket = null
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    _isConnected.value = false
                    socket = null
                }
            },
        )
    }

    fun send(payload: String) {
        socket?.send(payload)
    }

    fun disconnect() {
        socket?.close(1000, "client_shutdown")
        socket = null
        _isConnected.value = false
    }
}
