package com.gkim.im.android.core.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant

sealed interface GeneratedImageSaveResult {
    data class Success(val savedUri: Uri) : GeneratedImageSaveResult
    data class Failure(val message: String) : GeneratedImageSaveResult
}

interface GeneratedImageSaver {
    suspend fun saveImage(imageUrl: String, prompt: String): GeneratedImageSaveResult
}

class AndroidGeneratedImageSaver(
    private val context: Context,
    private val httpClient: OkHttpClient,
) : GeneratedImageSaver {
    override suspend fun saveImage(imageUrl: String, prompt: String): GeneratedImageSaveResult = withContext(Dispatchers.IO) {
        runCatching {
            val payload = readPayload(imageUrl)
            val mimeType = payload.mimeType ?: DEFAULT_MIME_TYPE
            val extension = extensionFor(mimeType)
            val savedUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, buildFileName(prompt, extension))
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GKIM")
                },
            ) ?: error("Failed to create a local image entry.")

            context.contentResolver.openOutputStream(savedUri)?.use { output ->
                output.write(payload.bytes)
                output.flush()
            } ?: error("Failed to open the local image destination.")

            GeneratedImageSaveResult.Success(savedUri)
        }.getOrElse { error ->
            GeneratedImageSaveResult.Failure(error.message ?: "Failed to save the generated image locally.")
        }
    }

    private fun readPayload(imageUrl: String): SourcePayload {
        val parsedUri = Uri.parse(imageUrl)
        return when (parsedUri.scheme?.lowercase()) {
            "content" -> {
                val bytes = context.contentResolver.openInputStream(parsedUri)?.use { it.readBytes() }
                    ?: error("Failed to read the generated image source.")
                SourcePayload(
                    bytes = bytes,
                    mimeType = context.contentResolver.getType(parsedUri),
                )
            }

            "http", "https" -> {
                val request = Request.Builder().url(imageUrl).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("Failed to download the generated image (${response.code}).")
                    }
                    SourcePayload(
                        bytes = response.body?.bytes() ?: error("Generated image download returned no data."),
                        mimeType = response.body?.contentType()?.toString(),
                    )
                }
            }

            else -> error("Unsupported generated image source: $imageUrl")
        }
    }

    private fun extensionFor(mimeType: String): String = when {
        mimeType.endsWith("jpeg") || mimeType.endsWith("jpg") -> "jpg"
        mimeType.endsWith("webp") -> "webp"
        else -> "png"
    }

    private fun buildFileName(prompt: String, extension: String): String {
        val slug = prompt
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(32)
            .ifBlank { "generated-image" }
        return "$slug-${Instant.now().epochSecond}.$extension"
    }
}

private data class SourcePayload(
    val bytes: ByteArray,
    val mimeType: String?,
)

private const val DEFAULT_MIME_TYPE = "image/png"
