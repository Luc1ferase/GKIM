package com.gkim.im.android.data.remote.im

import com.gkim.im.android.BuildConfig
import com.gkim.im.android.data.remote.api.ServiceFactory
import okhttp3.OkHttpClient
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

const val DEBUG_ACCESS_HEADER = "X-GKIM-Debug-Access"

interface ImWorldInfoClient {
    suspend fun list(baseUrl: String, token: String): LorebookListDto
    suspend fun get(baseUrl: String, token: String, lorebookId: String): LorebookDto
    suspend fun create(
        baseUrl: String,
        token: String,
        request: CreateLorebookRequestDto,
    ): LorebookDto
    suspend fun update(
        baseUrl: String,
        token: String,
        lorebookId: String,
        request: UpdateLorebookRequestDto,
    ): LorebookDto
    suspend fun delete(baseUrl: String, token: String, lorebookId: String)
    suspend fun duplicate(baseUrl: String, token: String, lorebookId: String): LorebookDto

    suspend fun listEntries(
        baseUrl: String,
        token: String,
        lorebookId: String,
    ): LorebookEntryListDto
    suspend fun createEntry(
        baseUrl: String,
        token: String,
        lorebookId: String,
        request: CreateLorebookEntryRequestDto,
    ): LorebookEntryDto
    suspend fun updateEntry(
        baseUrl: String,
        token: String,
        lorebookId: String,
        entryId: String,
        request: UpdateLorebookEntryRequestDto,
    ): LorebookEntryDto
    suspend fun deleteEntry(
        baseUrl: String,
        token: String,
        lorebookId: String,
        entryId: String,
    )

    suspend fun listBindings(
        baseUrl: String,
        token: String,
        lorebookId: String,
    ): LorebookBindingListDto
    suspend fun bind(
        baseUrl: String,
        token: String,
        lorebookId: String,
        characterId: String,
        isPrimary: Boolean,
    ): LorebookBindingDto
    suspend fun updateBinding(
        baseUrl: String,
        token: String,
        lorebookId: String,
        characterId: String,
        isPrimary: Boolean,
    ): LorebookBindingDto
    suspend fun unbind(
        baseUrl: String,
        token: String,
        lorebookId: String,
        characterId: String,
    )

    suspend fun debugScan(
        baseUrl: String,
        token: String,
        characterId: String,
        scanText: String,
        devAccessHeader: String,
        allowDebug: Boolean = BuildConfig.DEBUG,
    ): WorldInfoDebugScanResponseDto
}

private interface ImWorldInfoService {
    @GET("api/lorebooks")
    suspend fun list(@Header("Authorization") authorization: String): LorebookListDto

    @POST("api/lorebooks")
    suspend fun create(
        @Header("Authorization") authorization: String,
        @Body request: CreateLorebookRequestDto,
    ): LorebookDto

    @GET("api/lorebooks/{id}")
    suspend fun get(
        @Header("Authorization") authorization: String,
        @Path("id") lorebookId: String,
    ): LorebookDto

    @HTTP(method = "PATCH", path = "api/lorebooks/{id}", hasBody = true)
    suspend fun update(
        @Header("Authorization") authorization: String,
        @Path("id") lorebookId: String,
        @Body request: UpdateLorebookRequestDto,
    ): LorebookDto

    @DELETE("api/lorebooks/{id}")
    suspend fun delete(
        @Header("Authorization") authorization: String,
        @Path("id") lorebookId: String,
    )

    @POST("api/lorebooks/{id}/duplicate")
    suspend fun duplicate(
        @Header("Authorization") authorization: String,
        @Path("id") lorebookId: String,
    ): LorebookDto

    @GET("api/lorebooks/{id}/entries")
    suspend fun listEntries(
        @Header("Authorization") authorization: String,
        @Path("id") lorebookId: String,
    ): LorebookEntryListDto

    @POST("api/lorebooks/{id}/entries")
    suspend fun createEntry(
        @Header("Authorization") authorization: String,
        @Path("id") lorebookId: String,
        @Body request: CreateLorebookEntryRequestDto,
    ): LorebookEntryDto

    @HTTP(method = "PATCH", path = "api/lorebooks/{id}/entries/{entryId}", hasBody = true)
    suspend fun updateEntry(
        @Header("Authorization") authorization: String,
        @Path("id") lorebookId: String,
        @Path("entryId") entryId: String,
        @Body request: UpdateLorebookEntryRequestDto,
    ): LorebookEntryDto

    @DELETE("api/lorebooks/{id}/entries/{entryId}")
    suspend fun deleteEntry(
        @Header("Authorization") authorization: String,
        @Path("id") lorebookId: String,
        @Path("entryId") entryId: String,
    )

    @GET("api/lorebooks/{id}/bindings")
    suspend fun listBindings(
        @Header("Authorization") authorization: String,
        @Path("id") lorebookId: String,
    ): LorebookBindingListDto

    @POST("api/lorebooks/{id}/bindings")
    suspend fun bind(
        @Header("Authorization") authorization: String,
        @Path("id") lorebookId: String,
        @Body request: CreateLorebookBindingRequestDto,
    ): LorebookBindingDto

    @HTTP(
        method = "PATCH",
        path = "api/lorebooks/{id}/bindings/{characterId}",
        hasBody = true,
    )
    suspend fun updateBinding(
        @Header("Authorization") authorization: String,
        @Path("id") lorebookId: String,
        @Path("characterId") characterId: String,
        @Body request: UpdateLorebookBindingRequestDto,
    ): LorebookBindingDto

    @DELETE("api/lorebooks/{id}/bindings/{characterId}")
    suspend fun unbind(
        @Header("Authorization") authorization: String,
        @Path("id") lorebookId: String,
        @Path("characterId") characterId: String,
    )

    @POST("api/debug/worldinfo/scan")
    suspend fun debugScan(
        @Header("Authorization") authorization: String,
        @Header(DEBUG_ACCESS_HEADER) debugAccess: String,
        @Body request: WorldInfoDebugScanRequestDto,
    ): WorldInfoDebugScanResponseDto
}

class ImWorldInfoHttpClient(
    private val okHttpClient: OkHttpClient,
) : ImWorldInfoClient {
    override suspend fun list(baseUrl: String, token: String): LorebookListDto =
        serviceFor(baseUrl).list(bearerToken(token))

    override suspend fun get(
        baseUrl: String,
        token: String,
        lorebookId: String,
    ): LorebookDto = serviceFor(baseUrl).get(bearerToken(token), lorebookId)

    override suspend fun create(
        baseUrl: String,
        token: String,
        request: CreateLorebookRequestDto,
    ): LorebookDto = serviceFor(baseUrl).create(bearerToken(token), request)

    override suspend fun update(
        baseUrl: String,
        token: String,
        lorebookId: String,
        request: UpdateLorebookRequestDto,
    ): LorebookDto = serviceFor(baseUrl).update(bearerToken(token), lorebookId, request)

    override suspend fun delete(baseUrl: String, token: String, lorebookId: String) {
        serviceFor(baseUrl).delete(bearerToken(token), lorebookId)
    }

    override suspend fun duplicate(
        baseUrl: String,
        token: String,
        lorebookId: String,
    ): LorebookDto = serviceFor(baseUrl).duplicate(bearerToken(token), lorebookId)

    override suspend fun listEntries(
        baseUrl: String,
        token: String,
        lorebookId: String,
    ): LorebookEntryListDto = serviceFor(baseUrl).listEntries(bearerToken(token), lorebookId)

    override suspend fun createEntry(
        baseUrl: String,
        token: String,
        lorebookId: String,
        request: CreateLorebookEntryRequestDto,
    ): LorebookEntryDto = serviceFor(baseUrl).createEntry(bearerToken(token), lorebookId, request)

    override suspend fun updateEntry(
        baseUrl: String,
        token: String,
        lorebookId: String,
        entryId: String,
        request: UpdateLorebookEntryRequestDto,
    ): LorebookEntryDto = serviceFor(baseUrl).updateEntry(
        authorization = bearerToken(token),
        lorebookId = lorebookId,
        entryId = entryId,
        request = request,
    )

    override suspend fun deleteEntry(
        baseUrl: String,
        token: String,
        lorebookId: String,
        entryId: String,
    ) {
        serviceFor(baseUrl).deleteEntry(bearerToken(token), lorebookId, entryId)
    }

    override suspend fun listBindings(
        baseUrl: String,
        token: String,
        lorebookId: String,
    ): LorebookBindingListDto = serviceFor(baseUrl).listBindings(bearerToken(token), lorebookId)

    override suspend fun bind(
        baseUrl: String,
        token: String,
        lorebookId: String,
        characterId: String,
        isPrimary: Boolean,
    ): LorebookBindingDto = serviceFor(baseUrl).bind(
        authorization = bearerToken(token),
        lorebookId = lorebookId,
        request = CreateLorebookBindingRequestDto(
            characterId = characterId,
            isPrimary = isPrimary,
        ),
    )

    override suspend fun updateBinding(
        baseUrl: String,
        token: String,
        lorebookId: String,
        characterId: String,
        isPrimary: Boolean,
    ): LorebookBindingDto = serviceFor(baseUrl).updateBinding(
        authorization = bearerToken(token),
        lorebookId = lorebookId,
        characterId = characterId,
        request = UpdateLorebookBindingRequestDto(isPrimary = isPrimary),
    )

    override suspend fun unbind(
        baseUrl: String,
        token: String,
        lorebookId: String,
        characterId: String,
    ) {
        serviceFor(baseUrl).unbind(bearerToken(token), lorebookId, characterId)
    }

    override suspend fun debugScan(
        baseUrl: String,
        token: String,
        characterId: String,
        scanText: String,
        devAccessHeader: String,
        allowDebug: Boolean,
    ): WorldInfoDebugScanResponseDto {
        if (!allowDebug) {
            throw IllegalStateException("debug_scan_disabled_in_release")
        }
        if (devAccessHeader.isBlank()) {
            throw IllegalArgumentException("debug_access_header_missing")
        }
        val response = serviceFor(baseUrl).debugScan(
            authorization = bearerToken(token),
            debugAccess = devAccessHeader,
            request = WorldInfoDebugScanRequestDto(
                characterId = characterId,
                scanText = scanText,
            ),
        )
        return response.copy(
            matches = response.matches.sortedWith(
                compareBy({ it.insertionOrder }, { it.lorebookId }, { it.entryId }),
            ),
        )
    }

    private fun serviceFor(baseUrl: String): ImWorldInfoService =
        ServiceFactory.retrofit(normalizeBaseUrl(baseUrl), okHttpClient)
            .create(ImWorldInfoService::class.java)

    private fun normalizeBaseUrl(baseUrl: String): String =
        if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    private fun bearerToken(token: String): String = "Bearer $token"
}
