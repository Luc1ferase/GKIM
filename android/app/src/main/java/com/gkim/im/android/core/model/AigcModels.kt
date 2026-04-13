package com.gkim.im.android.core.model

import android.net.Uri

enum class AigcMode {
    TextToImage,
    ImageToImage,
    VideoToVideo,
}

enum class AccentTone {
    Primary,
    Secondary,
    Tertiary,
}

data class MediaInput(
    val type: AttachmentType,
    val uri: Uri,
)

data class CustomProviderConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
)

data class PresetProviderConfig(
    val model: String,
    val apiKey: String,
)

data class AigcProvider(
    val id: String,
    val label: String,
    val vendor: String,
    val description: String,
    val model: String,
    val accent: AccentTone,
    val preset: Boolean,
    val capabilities: Set<AigcMode>,
)

data class DraftAigcRequest(
    val mode: AigcMode = AigcMode.TextToImage,
    val prompt: String = "",
    val mediaInput: MediaInput? = null,
)

enum class TaskStatus {
    Queued,
    Succeeded,
    Failed,
}

data class AigcTask(
    val id: String,
    val remoteId: String? = null,
    val providerId: String,
    val model: String,
    val mode: AigcMode,
    val prompt: String,
    val createdAt: String,
    val status: TaskStatus,
    val input: MediaInput? = null,
    val outputPreview: String? = null,
    val errorMessage: String? = null,
)
