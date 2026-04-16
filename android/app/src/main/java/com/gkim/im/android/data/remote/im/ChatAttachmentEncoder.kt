package com.gkim.im.android.data.remote.im

import android.net.Uri
import com.gkim.im.android.core.model.AttachmentType
import com.gkim.im.android.core.model.MediaInput
import com.gkim.im.android.core.model.MessageAttachment
import com.gkim.im.android.data.remote.aigc.EncodedMediaPayload
import com.gkim.im.android.data.remote.aigc.MediaInputEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

interface ChatAttachmentEncoder {
    suspend fun encode(attachment: MessageAttachment): EncodedMediaPayload
}

class AndroidChatAttachmentEncoder(
    private val httpClient: OkHttpClient,
    private val mediaInputEncoder: MediaInputEncoder,
) : ChatAttachmentEncoder {
    override suspend fun encode(attachment: MessageAttachment): EncodedMediaPayload {
        val preview = attachment.preview
        return if (preview.startsWith("http://") || preview.startsWith("https://")) {
            encodeRemoteAttachment(preview, attachment.type)
        } else {
            mediaInputEncoder.encode(
                MediaInput(
                    type = attachment.type,
                    uri = Uri.parse(preview),
                )
            )
        }
    }

    private suspend fun encodeRemoteAttachment(
        url: String,
        type: AttachmentType,
    ): EncodedMediaPayload = withContext(Dispatchers.IO) {
        val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
        response.use { handled ->
            check(handled.isSuccessful) { "Unable to download remote attachment for message send." }
            val bytes = handled.body?.bytes()
                ?: error("Remote attachment response did not include a body.")
            val mimeType = handled.header("Content-Type")
                ?: when (type) {
                    AttachmentType.Image -> "image/png"
                    AttachmentType.Video -> "video/mp4"
                }
            EncodedMediaPayload(
                base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP),
                mimeType = mimeType,
            )
        }
    }
}

class UnsupportedChatAttachmentEncoder : ChatAttachmentEncoder {
    override suspend fun encode(attachment: MessageAttachment): EncodedMediaPayload {
        throw IllegalStateException("Chat attachment encoding is not configured for this IM environment.")
    }
}
