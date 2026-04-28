package com.gkim.im.android.core.model

import kotlinx.serialization.json.JsonObject

enum class SecondaryGate {
    None,
    And,
    Or,
}

data class LorebookEntry(
    val id: String,
    val lorebookId: String,
    val name: LocalizedText,
    val keysByLang: Map<AppLanguage, List<String>> = emptyMap(),
    val secondaryKeysByLang: Map<AppLanguage, List<String>> = emptyMap(),
    val secondaryGate: SecondaryGate = SecondaryGate.None,
    val content: LocalizedText = LocalizedText.Empty,
    val enabled: Boolean = true,
    val constant: Boolean = false,
    val caseSensitive: Boolean = false,
    val scanDepth: Int = DefaultScanDepth,
    val insertionOrder: Int = 0,
    val comment: String = "",
    val extensions: JsonObject = JsonObject(emptyMap()),
) {
    companion object {
        const val DefaultScanDepth: Int = 3
        const val MaxServerScanDepth: Int = 20
    }
}

fun LorebookEntry.primaryKeysFor(language: AppLanguage): List<String> =
    keysByLang[language].orEmpty()

fun LorebookEntry.secondaryKeysFor(language: AppLanguage): List<String> =
    secondaryKeysByLang[language].orEmpty()

fun LorebookEntry.canMatchInLanguage(language: AppLanguage): Boolean =
    constant || primaryKeysFor(language).any { it.isNotBlank() }
