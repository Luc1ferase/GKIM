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

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): AuthResponseDto

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): AuthResponseDto

    @GET("api/users/search")
    suspend fun searchUsers(@Header("Authorization") authorization: String, @Query("q") query: String): List<UserSearchResultDto>

    @POST("api/friends/request")
    suspend fun sendFriendRequest(@Header("Authorization") authorization: String, @Body request: FriendRequestBodyDto): FriendRequestViewDto

    @GET("api/friends/requests")
    suspend fun listFriendRequests(@Header("Authorization") authorization: String): List<FriendRequestViewDto>

    @POST("api/friends/requests/{requestId}/accept")
    suspend fun acceptFriendRequest(@Header("Authorization") authorization: String, @Path("requestId") requestId: String): FriendRequestViewDto

    @POST("api/friends/requests/{requestId}/reject")
    suspend fun rejectFriendRequest(@Header("Authorization") authorization: String, @Path("requestId") requestId: String): FriendRequestViewDto

    @GET("api/bootstrap")
    suspend fun loadBootstrap(@Header("Authorization") authorization: String): BootstrapBundleDto

    @POST("api/direct-messages/image")
    suspend fun sendDirectImageMessage(
        @Header("Authorization") authorization: String,
        @Body request: SendDirectImageMessageRequestDto,
    ): SendDirectMessageResultDto

    @GET("api/conversations/{conversationId}/messages")
    suspend fun loadHistory(
        @Header("Authorization") authorization: String,
        @Path("conversationId") conversationId: String,
        @Query("limit") limit: Int? = null,
        @Query("before") before: String? = null,
    ): MessageHistoryPageDto

    @GET("api/companions")
    suspend fun loadCompanionRoster(@Header("Authorization") authorization: String): CompanionRosterDto

    @POST("api/companions/draw")
    suspend fun drawCompanionCharacter(@Header("Authorization") authorization: String): CompanionDrawResultDto

    @POST("api/companions/select")
    suspend fun selectCompanionCharacter(
        @Header("Authorization") authorization: String,
        @Body request: SelectCompanionCharacterRequestDto,
    ): ActiveCompanionSelectionDto

    @POST("api/companions")
    suspend fun upsertCompanionCharacter(
        @Header("Authorization") authorization: String,
        @Body request: CompanionCharacterCardDto,
    ): CompanionCharacterCardDto

    @POST("api/companions/{characterId}/delete")
    suspend fun deleteCompanionCharacter(
        @Header("Authorization") authorization: String,
        @Path("characterId") characterId: String,
    )

    @POST("api/companion-turns")
    suspend fun submitCompanionTurn(
        @Header("Authorization") authorization: String,
        @Body request: CompanionTurnSubmitRequestDto,
    ): CompanionTurnRecordDto

    @POST("api/companion-turns/{turnId}/regenerate")
    suspend fun regenerateCompanionTurn(
        @Header("Authorization") authorization: String,
        @Path("turnId") turnId: String,
        @Body request: CompanionTurnRegenerateRequestDto,
    ): CompanionTurnRecordDto

    @GET("api/companion-turns/pending")
    suspend fun listPendingCompanionTurns(
        @Header("Authorization") authorization: String,
    ): CompanionTurnPendingListDto

    @GET("api/companion-turns/{turnId}")
    suspend fun snapshotCompanionTurn(
        @Header("Authorization") authorization: String,
        @Path("turnId") turnId: String,
    ): CompanionTurnRecordDto
}

class ImBackendHttpClient(
    private val okHttpClient: OkHttpClient,
) : ImBackendClient {
    override suspend fun issueDevSession(baseUrl: String, externalId: String): DevSessionResponseDto =
        serviceFor(baseUrl).issueDevSession(DevSessionRequestDto(externalId = externalId))

    override suspend fun register(baseUrl: String, username: String, password: String, displayName: String): AuthResponseDto =
        serviceFor(baseUrl).register(RegisterRequestDto(username = username, password = password, displayName = displayName))

    override suspend fun login(baseUrl: String, username: String, password: String): AuthResponseDto =
        serviceFor(baseUrl).login(LoginRequestDto(username = username, password = password))

    override suspend fun searchUsers(baseUrl: String, token: String, query: String): List<UserSearchResultDto> =
        serviceFor(baseUrl).searchUsers(bearerToken(token), query)

    override suspend fun sendFriendRequest(baseUrl: String, token: String, toUserId: String): FriendRequestViewDto =
        serviceFor(baseUrl).sendFriendRequest(bearerToken(token), FriendRequestBodyDto(toUserId = toUserId))

    override suspend fun listFriendRequests(baseUrl: String, token: String): List<FriendRequestViewDto> =
        serviceFor(baseUrl).listFriendRequests(bearerToken(token))

    override suspend fun acceptFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto =
        serviceFor(baseUrl).acceptFriendRequest(bearerToken(token), requestId)

    override suspend fun rejectFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto =
        serviceFor(baseUrl).rejectFriendRequest(bearerToken(token), requestId)

    override suspend fun loadBootstrap(baseUrl: String, token: String): BootstrapBundleDto =
        serviceFor(baseUrl).loadBootstrap(bearerToken(token))

    override suspend fun sendDirectImageMessage(
        baseUrl: String,
        token: String,
        request: SendDirectImageMessageRequestDto,
    ): SendDirectMessageResultDto = serviceFor(baseUrl).sendDirectImageMessage(
        authorization = bearerToken(token),
        request = request,
    )

    override suspend fun loadHistory(
        baseUrl: String,
        token: String,
        conversationId: String,
        limit: Int,
        before: String?,
    ): MessageHistoryPageDto = serviceFor(baseUrl).loadHistory(
        authorization = bearerToken(token),
        conversationId = conversationId,
        limit = limit,
        before = before,
    )

    override suspend fun loadCompanionRoster(baseUrl: String, token: String): CompanionRosterDto =
        serviceFor(baseUrl).loadCompanionRoster(bearerToken(token))

    override suspend fun drawCompanionCharacter(baseUrl: String, token: String): CompanionDrawResultDto =
        serviceFor(baseUrl).drawCompanionCharacter(bearerToken(token))

    override suspend fun selectCompanionCharacter(
        baseUrl: String,
        token: String,
        characterId: String,
    ): ActiveCompanionSelectionDto = serviceFor(baseUrl).selectCompanionCharacter(
        authorization = bearerToken(token),
        request = SelectCompanionCharacterRequestDto(characterId = characterId),
    )

    override suspend fun upsertCompanionCharacter(
        baseUrl: String,
        token: String,
        card: CompanionCharacterCardDto,
    ): CompanionCharacterCardDto = serviceFor(baseUrl).upsertCompanionCharacter(
        authorization = bearerToken(token),
        request = card,
    )

    override suspend fun deleteCompanionCharacter(
        baseUrl: String,
        token: String,
        characterId: String,
    ) {
        serviceFor(baseUrl).deleteCompanionCharacter(
            authorization = bearerToken(token),
            characterId = characterId,
        )
    }

    override suspend fun submitCompanionTurn(
        baseUrl: String,
        token: String,
        request: CompanionTurnSubmitRequestDto,
    ): CompanionTurnRecordDto = serviceFor(baseUrl).submitCompanionTurn(
        authorization = bearerToken(token),
        request = request,
    )

    override suspend fun regenerateCompanionTurn(
        baseUrl: String,
        token: String,
        turnId: String,
        clientTurnId: String,
    ): CompanionTurnRecordDto = serviceFor(baseUrl).regenerateCompanionTurn(
        authorization = bearerToken(token),
        turnId = turnId,
        request = CompanionTurnRegenerateRequestDto(clientTurnId = clientTurnId),
    )

    override suspend fun listPendingCompanionTurns(
        baseUrl: String,
        token: String,
    ): CompanionTurnPendingListDto = serviceFor(baseUrl).listPendingCompanionTurns(
        authorization = bearerToken(token),
    )

    override suspend fun snapshotCompanionTurn(
        baseUrl: String,
        token: String,
        turnId: String,
    ): CompanionTurnRecordDto = serviceFor(baseUrl).snapshotCompanionTurn(
        authorization = bearerToken(token),
        turnId = turnId,
    )

    private fun serviceFor(baseUrl: String): ImBackendService =
        ServiceFactory.retrofit(normalizeBaseUrl(baseUrl), okHttpClient).create(ImBackendService::class.java)

    private fun normalizeBaseUrl(baseUrl: String): String =
        if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    private fun bearerToken(token: String): String = "Bearer $token"
}
