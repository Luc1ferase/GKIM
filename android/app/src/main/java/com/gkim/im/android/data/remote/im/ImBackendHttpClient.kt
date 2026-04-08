package com.gkim.im.android.data.remote.im

import com.gkim.im.android.data.remote.api.ServiceFactory
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class DevSessionRequestDto(
    val externalId: String,
)

private interface ImBackendService {
    @POST("api/session/dev")
    suspend fun issueDevSession(@Body request: DevSessionRequestDto): DevSessionResponseDto

    @GET("api/bootstrap")
    suspend fun loadBootstrap(@Header("Authorization") authorization: String): BootstrapBundleDto

    @GET("api/conversations/{conversationId}/messages")
    suspend fun loadHistory(
        @Header("Authorization") authorization: String,
        @Path("conversationId") conversationId: String,
        @Query("limit") limit: Int? = null,
        @Query("before") before: String? = null,
    ): MessageHistoryPageDto
}

class ImBackendHttpClient(
    private val okHttpClient: OkHttpClient,
) {
    suspend fun issueDevSession(baseUrl: String, externalId: String): DevSessionResponseDto =
        serviceFor(baseUrl).issueDevSession(DevSessionRequestDto(externalId = externalId))

    suspend fun loadBootstrap(baseUrl: String, token: String): BootstrapBundleDto =
        serviceFor(baseUrl).loadBootstrap(bearerToken(token))

    suspend fun loadHistory(
        baseUrl: String,
        token: String,
        conversationId: String,
        limit: Int = 50,
        before: String? = null,
    ): MessageHistoryPageDto = serviceFor(baseUrl).loadHistory(
        authorization = bearerToken(token),
        conversationId = conversationId,
        limit = limit,
        before = before,
    )

    private fun serviceFor(baseUrl: String): ImBackendService =
        ServiceFactory.retrofit(normalizeBaseUrl(baseUrl), okHttpClient).create(ImBackendService::class.java)

    private fun normalizeBaseUrl(baseUrl: String): String =
        if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    private fun bearerToken(token: String): String = "Bearer $token"
}
