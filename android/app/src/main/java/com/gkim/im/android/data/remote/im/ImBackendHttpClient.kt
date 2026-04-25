package com.gkim.im.android.data.remote.im

import com.gkim.im.android.core.interop.SillyTavernCardCodec
import com.gkim.im.android.core.interop.SillyTavernCardFormat
import com.gkim.im.android.core.model.CompanionMemoryResetScope
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

    @POST("api/companion-turns/{conversationId}/edit")
    suspend fun editUserTurn(
        @Header("Authorization") authorization: String,
        @Path("conversationId") conversationId: String,
        @Body request: EditUserTurnRequestDto,
    ): EditUserTurnResponseDto

    @POST("api/companion-turns/{conversationId}/regenerate-at")
    suspend fun regenerateCompanionTurnAtTarget(
        @Header("Authorization") authorization: String,
        @Path("conversationId") conversationId: String,
        @Body request: RegenerateAtRequestDto,
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

    @POST("api/cards/import")
    suspend fun importCardPreview(
        @Header("Authorization") authorization: String,
        @Body request: CardImportUploadRequestDto,
    ): CardImportPreviewDto

    @POST("api/cards/import/commit")
    suspend fun importCardCommit(
        @Header("Authorization") authorization: String,
        @Body request: CardImportCommitRequestDto,
    ): CompanionCharacterCardDto

    @GET("api/cards/{cardId}/export")
    suspend fun exportCard(
        @Header("Authorization") authorization: String,
        @Path("cardId") cardId: String,
        @Query("format") format: String,
        @Query("language") language: String,
        @Query("includeTranslationAlt") includeTranslationAlt: Boolean? = null,
    ): CardExportResponseDto

    @GET("api/personas")
    suspend fun listPersonas(@Header("Authorization") authorization: String): UserPersonaListDto

    @POST("api/personas")
    suspend fun createPersona(
        @Header("Authorization") authorization: String,
        @Body persona: UserPersonaDto,
    ): UserPersonaDto

    @POST("api/personas/{personaId}")
    suspend fun updatePersona(
        @Header("Authorization") authorization: String,
        @Path("personaId") personaId: String,
        @Body persona: UserPersonaDto,
    ): UserPersonaDto

    @POST("api/personas/{personaId}/delete")
    suspend fun deletePersona(
        @Header("Authorization") authorization: String,
        @Path("personaId") personaId: String,
    )

    @POST("api/personas/{personaId}/activate")
    suspend fun activatePersona(
        @Header("Authorization") authorization: String,
        @Path("personaId") personaId: String,
    ): UserPersonaDto

    @GET("api/personas/active")
    suspend fun getActivePersona(@Header("Authorization") authorization: String): UserPersonaDto

    @GET("api/companions/{cardId}/memory")
    suspend fun getCompanionMemory(
        @Header("Authorization") authorization: String,
        @Path("cardId") cardId: String,
    ): CompanionMemoryDto

    @POST("api/companions/{cardId}/memory/reset")
    suspend fun resetCompanionMemory(
        @Header("Authorization") authorization: String,
        @Path("cardId") cardId: String,
        @Body request: CompanionMemoryResetRequestDto,
    )

    @GET("api/companions/{cardId}/memory/pins")
    suspend fun listCompanionMemoryPins(
        @Header("Authorization") authorization: String,
        @Path("cardId") cardId: String,
    ): CompanionMemoryPinListDto

    @POST("api/companions/{cardId}/memory/pins")
    suspend fun createCompanionMemoryPin(
        @Header("Authorization") authorization: String,
        @Path("cardId") cardId: String,
        @Body request: CompanionMemoryPinDto,
    ): CompanionMemoryPinDto

    @POST("api/companions/{cardId}/memory/pins/{pinId}")
    suspend fun updateCompanionMemoryPin(
        @Header("Authorization") authorization: String,
        @Path("cardId") cardId: String,
        @Path("pinId") pinId: String,
        @Body request: CompanionMemoryPinDto,
    ): CompanionMemoryPinDto

    @POST("api/companions/{cardId}/memory/pins/{pinId}/delete")
    suspend fun deleteCompanionMemoryPin(
        @Header("Authorization") authorization: String,
        @Path("cardId") cardId: String,
        @Path("pinId") pinId: String,
    )

    @GET("api/presets")
    suspend fun listPresets(@Header("Authorization") authorization: String): PresetListDto

    @POST("api/presets")
    suspend fun createPreset(
        @Header("Authorization") authorization: String,
        @Body preset: PresetDto,
    ): PresetDto

    @POST("api/presets/{presetId}")
    suspend fun updatePreset(
        @Header("Authorization") authorization: String,
        @Path("presetId") presetId: String,
        @Body preset: PresetDto,
    ): PresetDto

    @POST("api/presets/{presetId}/delete")
    suspend fun deletePreset(
        @Header("Authorization") authorization: String,
        @Path("presetId") presetId: String,
    )

    @POST("api/presets/{presetId}/activate")
    suspend fun activatePreset(
        @Header("Authorization") authorization: String,
        @Path("presetId") presetId: String,
    ): PresetDto

    @GET("api/presets/active")
    suspend fun getActivePreset(@Header("Authorization") authorization: String): PresetDto

    @GET("api/account/content-policy-acknowledgment")
    suspend fun getContentPolicyAcknowledgment(
        @Header("Authorization") authorization: String,
    ): ContentPolicyAcknowledgmentDto

    @POST("api/account/content-policy-acknowledgment")
    suspend fun postContentPolicyAcknowledgment(
        @Header("Authorization") authorization: String,
        @Body request: ContentPolicyAcknowledgmentRequestDto,
    ): ContentPolicyAcknowledgmentDto
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

    override suspend fun editUserTurn(
        baseUrl: String,
        token: String,
        conversationId: String,
        request: EditUserTurnRequestDto,
    ): EditUserTurnResponseDto = serviceFor(baseUrl).editUserTurn(
        authorization = bearerToken(token),
        conversationId = conversationId,
        request = request,
    )

    override suspend fun regenerateCompanionTurnAtTarget(
        baseUrl: String,
        token: String,
        conversationId: String,
        request: RegenerateAtRequestDto,
    ): CompanionTurnRecordDto = serviceFor(baseUrl).regenerateCompanionTurnAtTarget(
        authorization = bearerToken(token),
        conversationId = conversationId,
        request = request,
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

    override suspend fun importCardPreview(
        baseUrl: String,
        token: String,
        bytes: ByteArray,
        filename: String,
    ): CardImportPreviewDto {
        val claimedFormat = when (SillyTavernCardCodec.detectFormat(bytes)) {
            SillyTavernCardFormat.Png -> "png"
            SillyTavernCardFormat.Json -> "json"
            SillyTavernCardFormat.Unknown -> "unknown"
        }
        return serviceFor(baseUrl).importCardPreview(
            authorization = bearerToken(token),
            request = CardImportUploadRequestDto(
                filename = filename,
                contentBase64 = java.util.Base64.getEncoder().encodeToString(bytes),
                claimedFormat = claimedFormat,
            ),
        )
    }

    override suspend fun importCardCommit(
        baseUrl: String,
        token: String,
        preview: CardImportPreviewDto,
        overrides: CompanionCharacterCardDto?,
        languageOverride: String?,
    ): CompanionCharacterCardDto = serviceFor(baseUrl).importCardCommit(
        authorization = bearerToken(token),
        request = CardImportCommitRequestDto(
            previewToken = preview.previewToken,
            card = overrides ?: preview.card,
            languageOverride = languageOverride,
        ),
    )

    override suspend fun exportCard(
        baseUrl: String,
        token: String,
        cardId: String,
        format: String,
        language: String,
        includeTranslationAlt: Boolean,
    ): CardExportResponseDto = serviceFor(baseUrl).exportCard(
        authorization = bearerToken(token),
        cardId = cardId,
        format = format,
        language = language,
        includeTranslationAlt = includeTranslationAlt,
    )

    override suspend fun listPersonas(baseUrl: String, token: String): UserPersonaListDto =
        serviceFor(baseUrl).listPersonas(bearerToken(token))

    override suspend fun createPersona(
        baseUrl: String,
        token: String,
        persona: UserPersonaDto,
    ): UserPersonaDto = serviceFor(baseUrl).createPersona(
        authorization = bearerToken(token),
        persona = persona,
    )

    override suspend fun updatePersona(
        baseUrl: String,
        token: String,
        persona: UserPersonaDto,
    ): UserPersonaDto = serviceFor(baseUrl).updatePersona(
        authorization = bearerToken(token),
        personaId = persona.id,
        persona = persona,
    )

    override suspend fun deletePersona(baseUrl: String, token: String, personaId: String) {
        serviceFor(baseUrl).deletePersona(
            authorization = bearerToken(token),
            personaId = personaId,
        )
    }

    override suspend fun activatePersona(
        baseUrl: String,
        token: String,
        personaId: String,
    ): UserPersonaDto = serviceFor(baseUrl).activatePersona(
        authorization = bearerToken(token),
        personaId = personaId,
    )

    override suspend fun getActivePersona(baseUrl: String, token: String): UserPersonaDto =
        serviceFor(baseUrl).getActivePersona(bearerToken(token))

    override suspend fun getCompanionMemory(
        baseUrl: String,
        token: String,
        cardId: String,
    ): CompanionMemoryDto = serviceFor(baseUrl).getCompanionMemory(
        authorization = bearerToken(token),
        cardId = cardId,
    )

    override suspend fun resetCompanionMemory(
        baseUrl: String,
        token: String,
        cardId: String,
        scope: CompanionMemoryResetScope,
    ) {
        serviceFor(baseUrl).resetCompanionMemory(
            authorization = bearerToken(token),
            cardId = cardId,
            request = CompanionMemoryResetRequestDto.fromCompanionMemoryResetScope(scope),
        )
    }

    override suspend fun listCompanionMemoryPins(
        baseUrl: String,
        token: String,
        cardId: String,
    ): CompanionMemoryPinListDto = serviceFor(baseUrl).listCompanionMemoryPins(
        authorization = bearerToken(token),
        cardId = cardId,
    )

    override suspend fun createCompanionMemoryPin(
        baseUrl: String,
        token: String,
        cardId: String,
        pin: CompanionMemoryPinDto,
    ): CompanionMemoryPinDto = serviceFor(baseUrl).createCompanionMemoryPin(
        authorization = bearerToken(token),
        cardId = cardId,
        request = pin,
    )

    override suspend fun updateCompanionMemoryPin(
        baseUrl: String,
        token: String,
        cardId: String,
        pin: CompanionMemoryPinDto,
    ): CompanionMemoryPinDto = serviceFor(baseUrl).updateCompanionMemoryPin(
        authorization = bearerToken(token),
        cardId = cardId,
        pinId = pin.id,
        request = pin,
    )

    override suspend fun deleteCompanionMemoryPin(
        baseUrl: String,
        token: String,
        cardId: String,
        pinId: String,
    ) {
        serviceFor(baseUrl).deleteCompanionMemoryPin(
            authorization = bearerToken(token),
            cardId = cardId,
            pinId = pinId,
        )
    }

    override suspend fun listPresets(baseUrl: String, token: String): PresetListDto =
        serviceFor(baseUrl).listPresets(bearerToken(token))

    override suspend fun createPreset(
        baseUrl: String,
        token: String,
        preset: PresetDto,
    ): PresetDto = serviceFor(baseUrl).createPreset(
        authorization = bearerToken(token),
        preset = preset,
    )

    override suspend fun updatePreset(
        baseUrl: String,
        token: String,
        preset: PresetDto,
    ): PresetDto = serviceFor(baseUrl).updatePreset(
        authorization = bearerToken(token),
        presetId = preset.id,
        preset = preset,
    )

    override suspend fun deletePreset(baseUrl: String, token: String, presetId: String) {
        serviceFor(baseUrl).deletePreset(
            authorization = bearerToken(token),
            presetId = presetId,
        )
    }

    override suspend fun activatePreset(
        baseUrl: String,
        token: String,
        presetId: String,
    ): PresetDto = serviceFor(baseUrl).activatePreset(
        authorization = bearerToken(token),
        presetId = presetId,
    )

    override suspend fun getActivePreset(baseUrl: String, token: String): PresetDto =
        serviceFor(baseUrl).getActivePreset(bearerToken(token))

    override suspend fun getContentPolicyAcknowledgment(
        baseUrl: String,
        token: String,
    ): ContentPolicyAcknowledgmentDto = serviceFor(baseUrl).getContentPolicyAcknowledgment(bearerToken(token))

    override suspend fun postContentPolicyAcknowledgment(
        baseUrl: String,
        token: String,
        version: String,
    ): ContentPolicyAcknowledgmentDto = serviceFor(baseUrl).postContentPolicyAcknowledgment(
        authorization = bearerToken(token),
        request = ContentPolicyAcknowledgmentRequestDto(version = version),
    )

    private fun serviceFor(baseUrl: String): ImBackendService =
        ServiceFactory.retrofit(normalizeBaseUrl(baseUrl), okHttpClient).create(ImBackendService::class.java)

    private fun normalizeBaseUrl(baseUrl: String): String =
        if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    private fun bearerToken(token: String): String = "Bearer $token"
}
