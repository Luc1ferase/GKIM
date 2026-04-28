package com.gkim.im.android.core.model

import kotlinx.serialization.json.JsonObject

data class UserPersona(
    val id: String,
    val displayName: LocalizedText,
    val description: LocalizedText,
    val isBuiltIn: Boolean = false,
    val isActive: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val extensions: JsonObject = JsonObject(emptyMap()),
)

data class ResolvedUserPersona(
    val id: String,
    val displayName: String,
    val description: String,
    val isBuiltIn: Boolean,
    val isActive: Boolean,
)

fun UserPersona.resolve(language: AppLanguage): ResolvedUserPersona =
    ResolvedUserPersona(
        id = id,
        displayName = displayName.resolve(language),
        description = description.resolve(language),
        isBuiltIn = isBuiltIn,
        isActive = isActive,
    )

val UserPersona.isDeletable: Boolean
    get() = !isBuiltIn && !isActive

enum class UserPersonaValidationError {
    DisplayNameEnglishBlank,
    DisplayNameChineseBlank,
    DescriptionEnglishBlank,
    DescriptionChineseBlank,
}

sealed class UserPersonaValidationResult {
    object Valid : UserPersonaValidationResult()
    data class Invalid(val errors: List<UserPersonaValidationError>) : UserPersonaValidationResult()
}

object UserPersonaValidation {
    fun validate(persona: UserPersona): UserPersonaValidationResult {
        val errors = mutableListOf<UserPersonaValidationError>()
        if (persona.displayName.english.isBlank()) errors += UserPersonaValidationError.DisplayNameEnglishBlank
        if (persona.displayName.chinese.isBlank()) errors += UserPersonaValidationError.DisplayNameChineseBlank
        if (persona.description.english.isBlank()) errors += UserPersonaValidationError.DescriptionEnglishBlank
        if (persona.description.chinese.isBlank()) errors += UserPersonaValidationError.DescriptionChineseBlank
        return if (errors.isEmpty()) UserPersonaValidationResult.Valid
        else UserPersonaValidationResult.Invalid(errors)
    }
}
