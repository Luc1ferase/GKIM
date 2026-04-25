package com.gkim.im.android.core.model

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

data class LocalizedText(
    val english: String,
    val chinese: String,
) {
    fun resolve(language: AppLanguage): String = when (language) {
        AppLanguage.English -> english
        AppLanguage.Chinese -> chinese
    }

    companion object {
        val Empty: LocalizedText = LocalizedText(english = "", chinese = "")

        fun of(value: String): LocalizedText = LocalizedText(english = value, chinese = value)
    }
}

enum class CompanionCharacterSource {
    Preset,
    Drawn,
    UserAuthored,
}

data class CompanionCharacterCard(
    val id: String,
    val displayName: LocalizedText,
    val roleLabel: LocalizedText,
    val summary: LocalizedText,
    val firstMes: LocalizedText,
    val alternateGreetings: List<LocalizedText> = emptyList(),
    val systemPrompt: LocalizedText = LocalizedText.Empty,
    val personality: LocalizedText = LocalizedText.Empty,
    val scenario: LocalizedText = LocalizedText.Empty,
    val exampleDialogue: LocalizedText = LocalizedText.Empty,
    val tags: List<String> = emptyList(),
    val creator: String = "",
    val creatorNotes: String = "",
    val characterVersion: String = "",
    val avatarText: String,
    val avatarUri: String? = null,
    val accent: AccentTone,
    val source: CompanionCharacterSource,
    val extensions: JsonObject = JsonObject(emptyMap()),
    val characterPresetId: String? = null,
)

data class ResolvedCompanionCharacterCard(
    val id: String,
    val displayName: String,
    val roleLabel: String,
    val summary: String,
    val firstMes: String,
    val alternateGreetings: List<String>,
    val systemPrompt: String,
    val personality: String,
    val scenario: String,
    val exampleDialogue: String,
    val tags: List<String>,
    val creator: String,
    val creatorNotes: String,
    val characterVersion: String,
    val avatarText: String,
    val avatarUri: String?,
    val accent: AccentTone,
    val source: CompanionCharacterSource,
    val characterPresetId: String?,
)

fun CompanionCharacterCard.resolve(language: AppLanguage): ResolvedCompanionCharacterCard =
    ResolvedCompanionCharacterCard(
        id = id,
        displayName = displayName.resolve(language),
        roleLabel = roleLabel.resolve(language),
        summary = summary.resolve(language),
        firstMes = firstMes.resolve(language),
        alternateGreetings = alternateGreetings.map { it.resolve(language) },
        systemPrompt = systemPrompt.resolve(language),
        personality = personality.resolve(language),
        scenario = scenario.resolve(language),
        exampleDialogue = exampleDialogue.resolve(language),
        tags = tags,
        creator = creator,
        creatorNotes = creatorNotes,
        characterVersion = characterVersion,
        avatarText = avatarText,
        avatarUri = avatarUri,
        accent = accent,
        source = source,
        characterPresetId = characterPresetId,
    )

val CompanionCharacterCard.isEditable: Boolean
    get() = source != CompanionCharacterSource.Preset

val CompanionCharacterCard.isDeletable: Boolean
    get() = source == CompanionCharacterSource.UserAuthored

data class CompanionDrawResult(
    val card: CompanionCharacterCard,
    val wasNew: Boolean,
)

@Suppress("unused")
fun CompanionCharacterCard.extensionValue(key: String): JsonElement? = extensions[key]
