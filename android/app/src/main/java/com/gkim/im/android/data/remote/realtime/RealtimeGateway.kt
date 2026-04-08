package com.gkim.im.android.data.remote.realtime

import com.gkim.im.android.data.remote.im.ImGatewayEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface RealtimeGateway {
    val isConnected: StateFlow<Boolean>
    val lastFailure: StateFlow<String?>
    val events: SharedFlow<ImGatewayEvent>

    fun connect(token: String? = null, endpointOverride: String? = null)
    fun send(payload: String): Boolean
    fun sendMessage(recipientExternalId: String, clientMessageId: String?, body: String): Boolean
    fun markRead(conversationId: String, messageId: String): Boolean
    fun disconnect()
}
