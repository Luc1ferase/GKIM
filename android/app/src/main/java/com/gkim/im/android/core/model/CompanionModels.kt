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
    // R2.4 — companion-skin-gacha. Identifies which of this character's
    // skins is currently active for this user. Defaults to "default" so
    // every existing seeded card resolves to the v1 default skin until
    // the user owns and activates an alternate. Drives the avatar / chat
    // header URL construction via skinAssetUrl(...).
    val activeSkinId: String = DEFAULT_SKIN_ID,
) {
    companion object {
        const val DEFAULT_SKIN_ID: String = "default"
    }
}

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
    // R2.4 — propagated from CompanionCharacterCard.activeSkinId so UI
    // surfaces (tavern card, chat header) can construct the correct
    // skinAssetUrl without re-resolving against the unprojected card.
    val activeSkinId: String,
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
        activeSkinId = activeSkinId,
    )

val CompanionCharacterCard.isEditable: Boolean
    get() = source != CompanionCharacterSource.Preset

val CompanionCharacterCard.isDeletable: Boolean
    get() = source == CompanionCharacterSource.UserAuthored

data class CompanionDrawResult(
    val card: CompanionCharacterCard,
    val wasNew: Boolean,
    // R4.3 — companion-skin-gacha three-state result. The skin-granular
    // /skins/draw endpoint returns one of NEW_CHARACTER / NEW_SKIN /
    // DUPLICATE_SKIN; the post-draw surface routes off this value when
    // present. Pre-existing call sites (legacy /draw, in-memory roster
    // repo) leave it null and fall back to the `wasNew` heuristic via
    // gachaResultVariant(). Stored as a closed [SkinDrawResultState]
    // enum so the routing helpers + reveal-track router share a single
    // contract.
    val drawResultState: SkinDrawResultState? = null,
    // R4.3 — currency awarded for this draw (DUPLICATE_SKIN only). Null
    // for the other two states; rendered via the duplicate caption when
    // > 0. Pinned in story-shard units (1 / 5 / 25 / 125 by rarity per
    // design.md).
    val currencyDelta: Int? = null,
)

/**
 * R4.3 — companion-skin-gacha closed-set draw response state.
 *
 * The skin-granular `/api/v1/skins/draw` endpoint returns exactly one of
 * these three states per result entry. New states require extending the
 * downstream variant + reveal-track maps in lockstep; the unit tests pin
 * "every state has a unique routing target".
 */
enum class SkinDrawResultState {
    NewCharacter,
    NewSkin,
    DuplicateSkin,
}

@Suppress("unused")
fun CompanionCharacterCard.extensionValue(key: String): JsonElement? = extensions[key]
