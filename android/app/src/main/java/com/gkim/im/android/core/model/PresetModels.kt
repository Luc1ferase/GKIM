package com.gkim.im.android.core.model

import kotlinx.serialization.json.JsonObject

data class Preset(
    val id: String,
    val displayName: LocalizedText,
    val description: LocalizedText = LocalizedText.Empty,
    val template: PresetTemplate = PresetTemplate(),
    val params: PresetParams = PresetParams(),
    val isBuiltIn: Boolean = false,
    val isActive: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val extensions: JsonObject = JsonObject(emptyMap()),
)

data class PresetTemplate(
    val systemPrefix: LocalizedText = LocalizedText.Empty,
    val systemSuffix: LocalizedText = LocalizedText.Empty,
    val formatInstructions: LocalizedText = LocalizedText.Empty,
    val postHistoryInstructions: LocalizedText = LocalizedText.Empty,
)

data class PresetParams(
    val temperature: Double? = null,
    val topP: Double? = null,
    val maxReplyTokens: Int? = null,
)

val Preset.isDeletable: Boolean
    get() = !isBuiltIn && !isActive
