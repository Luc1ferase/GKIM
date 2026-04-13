package com.gkim.im.android.data.remote.aigc

import android.content.Context
import android.util.Base64
import com.gkim.im.android.core.model.MediaInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class EncodedMediaPayload(
    val base64Data: String,
    val mimeType: String,
)

interface MediaInputEncoder {
    suspend fun encode(mediaInput: MediaInput): EncodedMediaPayload
}

class AndroidMediaInputEncoder(
    private val context: Context,
) : MediaInputEncoder {
    override suspend fun encode(mediaInput: MediaInput): EncodedMediaPayload = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(mediaInput.uri)?.use { stream ->
            stream.readBytes()
        } ?: throw IllegalStateException("Unable to read local media input for AIGC generation.")
        val mimeType = context.contentResolver.getType(mediaInput.uri) ?: DEFAULT_MEDIA_MIME_TYPE
        EncodedMediaPayload(
            base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP),
            mimeType = mimeType,
        )
    }
}

class UnsupportedMediaInputEncoder : MediaInputEncoder {
    override suspend fun encode(mediaInput: MediaInput): EncodedMediaPayload {
        throw IllegalStateException("Local media encoding is not configured for this AIGC environment.")
    }
}

private const val DEFAULT_MEDIA_MIME_TYPE = "application/octet-stream"
