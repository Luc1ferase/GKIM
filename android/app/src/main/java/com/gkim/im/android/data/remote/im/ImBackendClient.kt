package com.gkim.im.android.data.remote.im

interface ImBackendClient {
    suspend fun issueDevSession(baseUrl: String, externalId: String): DevSessionResponseDto
    suspend fun loadBootstrap(baseUrl: String, token: String): BootstrapBundleDto
    suspend fun loadHistory(
        baseUrl: String,
        token: String,
        conversationId: String,
        limit: Int = 50,
        before: String? = null,
    ): MessageHistoryPageDto
}
