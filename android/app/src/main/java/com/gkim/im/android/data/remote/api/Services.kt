package com.gkim.im.android.data.remote.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

@Serializable
data class AigcGenerateRequestDto(
    val mode: String,
    val prompt: String,
    val mediaUri: String? = null,
    val mediaType: String? = null,
)

@Serializable
data class AigcGenerateResponseDto(
    val id: String,
    val outputPreview: String,
    val status: String,
)

@Serializable
data class FeedPostDto(
    val id: String,
    val title: String,
    val summary: String,
    val body: String,
)

interface AigcService {
    @POST("aigc/generate")
    suspend fun generate(@Body request: AigcGenerateRequestDto): AigcGenerateResponseDto
}

interface FeedService {
    @GET("feed")
    suspend fun fetchFeed(): List<FeedPostDto>
}

object ServiceFactory {
    private val json = Json { ignoreUnknownKeys = true }

    fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
