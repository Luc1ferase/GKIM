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

data class ResolvedPreset(
    val id: String,
    val displayName: String,
    val description: String,
    val isBuiltIn: Boolean,
    val isActive: Boolean,
)

fun Preset.resolve(language: AppLanguage): ResolvedPreset =
    ResolvedPreset(
        id = id,
        displayName = displayName.resolve(language),
        description = description.resolve(language),
        isBuiltIn = isBuiltIn,
        isActive = isActive,
    )

enum class PresetValidationError {
    DisplayNameEnglishBlank,
    DisplayNameChineseBlank,
    TemperatureOutOfRange,
    TopPOutOfRange,
    MaxReplyTokensOutOfRange,
}

sealed class PresetValidationResult {
    object Valid : PresetValidationResult()
    data class Invalid(val errors: List<PresetValidationError>) : PresetValidationResult()
}

object PresetValidation {
    fun validate(preset: Preset): PresetValidationResult {
        val errors = mutableListOf<PresetValidationError>()
        if (preset.displayName.english.isBlank()) errors += PresetValidationError.DisplayNameEnglishBlank
        if (preset.displayName.chinese.isBlank()) errors += PresetValidationError.DisplayNameChineseBlank
        preset.params.temperature?.let {
            if (it < 0.0 || it > 2.0) errors += PresetValidationError.TemperatureOutOfRange
        }
        preset.params.topP?.let {
            if (it < 0.0 || it > 1.0) errors += PresetValidationError.TopPOutOfRange
        }
        preset.params.maxReplyTokens?.let {
            if (it < 1 || it > 32_768) errors += PresetValidationError.MaxReplyTokensOutOfRange
        }
        return if (errors.isEmpty()) PresetValidationResult.Valid
        else PresetValidationResult.Invalid(errors)
    }
}
