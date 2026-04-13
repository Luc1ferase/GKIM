package com.gkim.im.android.data.remote.aigc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class RemoteAigcGenerateRequest(
    val model: String,
    val prompt: String,
    val apiKey: String,
    val imageBase64: String? = null,
    val imageMimeType: String? = null,
)

data class RemoteAigcGenerateResult(
    val remoteId: String,
    val outputUrl: String,
)

interface RemoteAigcProviderClient {
    suspend fun generate(request: RemoteAigcGenerateRequest): RemoteAigcGenerateResult
}

class HunyuanImageProviderClient(
    private val baseUrl: String = DEFAULT_HUNYUAN_BASE_URL,
    private val httpClient: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : RemoteAigcProviderClient {
    override suspend fun generate(request: RemoteAigcGenerateRequest): RemoteAigcGenerateResult = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(
            HunyuanGenerateRequest.serializer(),
            HunyuanGenerateRequest(
                model = normalizeHunyuanModel(request.model),
                prompt = request.prompt,
                responseImageType = HUNYUAN_RESPONSE_IMAGE_TYPE_URL,
            ),
        )
        val responseBody = execute(
            path = HUNYUAN_GENERATE_PATH,
            apiKey = request.apiKey,
            body = payload,
        )
        val parsed = json.decodeFromString(HunyuanGenerateResponse.serializer(), responseBody)
        val outputUrl = parsed.data.firstOrNull()?.url?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Tencent Hunyuan returned success without an image URL.")
        RemoteAigcGenerateResult(
            remoteId = parsed.requestId?.takeIf { it.isNotBlank() }.orEmpty(),
            outputUrl = outputUrl,
        )
    }

    private fun execute(
        path: String,
        apiKey: String,
        body: String? = null,
    ): String {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/" + path.trimStart('/'))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", JSON_MEDIA_TYPE)
            .post((body ?: "{}").toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(extractErrorMessage(responseBody, "Tencent Hunyuan request failed with ${response.code}."))
            }
            return responseBody
        }
    }
}

class TongyiImageProviderClient(
    private val baseUrl: String = DEFAULT_TONGYI_BASE_URL,
    private val httpClient: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : RemoteAigcProviderClient {
    override suspend fun generate(request: RemoteAigcGenerateRequest): RemoteAigcGenerateResult = withContext(Dispatchers.IO) {
        val content = buildList {
            request.imageBase64?.let { base64 ->
                val mimeType = request.imageMimeType ?: DEFAULT_IMAGE_MIME_TYPE
                add(TongyiMessageContent(image = "data:$mimeType;base64,$base64"))
            }
            add(TongyiMessageContent(text = request.prompt))
        }
        val payload = json.encodeToString(
            TongyiGenerateRequest.serializer(),
            TongyiGenerateRequest(
                model = request.model,
                input = TongyiInput(
                    messages = listOf(
                        TongyiMessage(
                            role = "user",
                            content = content,
                        )
                    )
                )
            )
        )
        val requestValue = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/" + TONGYI_GENERATE_PATH)
            .header("Authorization", "Bearer ${request.apiKey}")
            .header("Content-Type", JSON_MEDIA_TYPE)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()

        httpClient.newCall(requestValue).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(extractErrorMessage(responseBody, "Alibaba Tongyi request failed with ${response.code}."))
            }
            val parsed = json.decodeFromString(TongyiGenerateResponse.serializer(), responseBody)
            val outputUrl = parsed.output?.choices
                ?.firstOrNull()
                ?.message
                ?.content
                ?.firstOrNull { !it.image.isNullOrBlank() }
                ?.image
                ?: throw IllegalStateException("Alibaba Tongyi returned success without an image URL.")
            return@withContext RemoteAigcGenerateResult(
                remoteId = parsed.requestId ?: "",
                outputUrl = outputUrl,
            )
        }
    }
}

private fun normalizeHunyuanModel(model: String): String = when (model.lowercase()) {
    "hy-image-v3.0" -> "hy-image-v3.0"
    else -> model
}

private fun extractErrorMessage(body: String, fallback: String): String {
    val messageMatch = Regex(""""message"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.getOrNull(1)
    return messageMatch ?: fallback
}

private const val JSON_MEDIA_TYPE = "application/json"
private const val DEFAULT_IMAGE_MIME_TYPE = "image/png"
private const val DEFAULT_HUNYUAN_BASE_URL = "https://tokenhub.tencentmaas.com/"
private const val DEFAULT_TONGYI_BASE_URL = "https://dashscope.aliyuncs.com/"
private const val TONGYI_GENERATE_PATH = "api/v1/services/aigc/multimodal-generation/generation"
private const val HUNYUAN_GENERATE_PATH = "v1/api/image/lite"
private const val HUNYUAN_RESPONSE_IMAGE_TYPE_URL = "url"

@Serializable
private data class HunyuanGenerateRequest(
    val model: String,
    val prompt: String,
    @SerialName("rsp_img_type")
    val responseImageType: String,
)

@Serializable
private data class HunyuanGenerateResponse(
    @SerialName("request_id")
    val requestId: String? = null,
    val data: List<HunyuanOutput> = emptyList(),
)

@Serializable
private data class HunyuanOutput(
    val url: String? = null,
)

@Serializable
private data class ProviderError(
    val message: String? = null,
)

@Serializable
private data class TongyiGenerateRequest(
    val model: String,
    val input: TongyiInput,
)

@Serializable
private data class TongyiInput(
    val messages: List<TongyiMessage>,
)

@Serializable
private data class TongyiMessage(
    val role: String,
    val content: List<TongyiMessageContent>,
)

@Serializable
private data class TongyiMessageContent(
    val image: String? = null,
    val text: String? = null,
)

@Serializable
private data class TongyiGenerateResponse(
    @SerialName("request_id")
    val requestId: String? = null,
    val output: TongyiOutput? = null,
)

@Serializable
private data class TongyiOutput(
    val choices: List<TongyiChoice> = emptyList(),
)

@Serializable
private data class TongyiChoice(
    val message: TongyiResponseMessage,
)

@Serializable
private data class TongyiResponseMessage(
    val content: List<TongyiResponseContent> = emptyList(),
)

@Serializable
private data class TongyiResponseContent(
    val image: String? = null,
)
