package com.gkim.im.android.core.model

import kotlinx.serialization.json.JsonObject

data class Lorebook(
    val id: String,
    val ownerId: String,
    val displayName: LocalizedText,
    val description: LocalizedText = LocalizedText.Empty,
    val isGlobal: Boolean = false,
    val isBuiltIn: Boolean = false,
    val tokenBudget: Int = DefaultTokenBudget,
    val extensions: JsonObject = JsonObject(emptyMap()),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    companion object {
        const val DefaultTokenBudget: Int = 1024
    }
}

data class ResolvedLorebook(
    val id: String,
    val displayName: String,
    val description: String,
    val isGlobal: Boolean,
    val isBuiltIn: Boolean,
    val tokenBudget: Int,
)

fun Lorebook.resolve(language: AppLanguage): ResolvedLorebook = ResolvedLorebook(
    id = id,
    displayName = displayName.resolve(language),
    description = description.resolve(language),
    isGlobal = isGlobal,
    isBuiltIn = isBuiltIn,
    tokenBudget = tokenBudget,
)

val Lorebook.isDeletable: Boolean
    get() = !isBuiltIn
