package com.gkim.im.android.data.remote.im

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.core.model.Conversation
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.CompanionDrawResult
import com.gkim.im.android.core.model.CompanionMemory
import com.gkim.im.android.core.model.CompanionMemoryPin
import com.gkim.im.android.core.model.CompanionMemoryResetScope
import com.gkim.im.android.core.model.AttachmentType
import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.BlockReason
import com.gkim.im.android.core.model.FailedSubtype
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Lorebook
import com.gkim.im.android.core.model.LorebookBinding
import com.gkim.im.android.core.model.LorebookEntry
import com.gkim.im.android.core.model.MessageAttachment
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.Preset
import com.gkim.im.android.core.model.PresetParams
import com.gkim.im.android.core.model.PresetTemplate
import com.gkim.im.android.core.model.SecondaryGate
import com.gkim.im.android.core.model.UserPersona
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class BackendUserDto(
    val id: String,
    val externalId: String,
    val displayName: String,
    val title: String,
    val avatarText: String,
)

@Serializable
data class ContactProfileDto(
    val userId: String,
    val externalId: String,
    val displayName: String,
    val title: String,
    val avatarText: String,
    val addedAt: String,
)

@Serializable
data class MessageAttachmentDto(
    val type: String,
    val contentType: String,
    val fetchPath: String,
    val sizeBytes: Long,
)

@Serializable
data class MessageRecordDto(
    val id: String,
    val conversationId: String,
    val senderUserId: String,
    val senderExternalId: String,
    val kind: String,
    val body: String,
    val createdAt: String,
    val deliveredAt: String? = null,
    val readAt: String? = null,
    val attachment: MessageAttachmentDto? = null,
) {
    fun toChatMessage(
        activeUserExternalId: String,
        backendBaseUrl: String? = null,
        authToken: String? = null,
    ): ChatMessage = ChatMessage(
        id = id,
        direction = if (senderExternalId == activeUserExternalId) {
            MessageDirection.Outgoing
        } else {
            MessageDirection.Incoming
        },
        kind = when (kind.lowercase()) {
            "aigc" -> MessageKind.Aigc
            else -> MessageKind.Text
        },
        body = body,
        createdAt = createdAt,
        deliveredAt = deliveredAt,
        readAt = readAt,
        attachment = attachment?.toMessageAttachment(backendBaseUrl = backendBaseUrl, authToken = authToken),
    )
}

@Serializable
data class SendDirectImageMessageRequestDto(
    val recipientExternalId: String,
    val clientMessageId: String? = null,
    val body: String,
    val contentType: String,
    val imageBase64: String,
)

@Serializable
data class SendDirectMessageResultDto(
    val conversationId: String,
    val recipientExternalId: String,
    val recipientUnreadCount: Int,
    val message: MessageRecordDto,
)

@Serializable
data class ConversationSummaryDto(
    val conversationId: String,
    val contact: ContactProfileDto,
    val unreadCount: Int,
    val lastMessage: MessageRecordDto? = null,
)

@Serializable
data class DevSessionResponseDto(
    val token: String,
    val expiresAt: String,
    val user: BackendUserDto,
)

@Serializable
data class AuthResponseDto(
    val token: String,
    val expiresAt: String,
    val user: BackendUserDto,
)

@Serializable
data class RegisterRequestDto(
    val username: String,
    val password: String,
    val displayName: String,
)

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String,
)

@Serializable
data class UserSearchResultDto(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarText: String,
    val contactStatus: String,
)

@Serializable
data class FriendRequestBodyDto(
    val toUserId: String,
)

@Serializable
data class FriendRequestViewDto(
    val id: String,
    val fromUser: BackendUserDto,
    val toUserId: String,
    val toUserExternalId: String,
    val status: String,
    val createdAt: String,
)

@Serializable
data class BootstrapBundleDto(
    val user: BackendUserDto,
    val contacts: List<ContactProfileDto>,
    val conversations: List<ConversationSummaryDto>,
    val lorebookSummaries: List<LorebookSummaryDto> = emptyList(),
) {
    fun toBootstrapState(
        activeUserExternalId: String,
        backendBaseUrl: String? = null,
        authToken: String? = null,
    ): ImBootstrapState = ImBootstrapState(
        user = user,
        contacts = contacts.map { contact ->
            Contact(
                id = contact.externalId,
                nickname = contact.displayName,
                title = contact.title,
                avatarText = contact.avatarText,
                addedAt = contact.addedAt,
                isOnline = false,
            )
        },
        conversations = conversations.map { conversation ->
            Conversation(
                id = conversation.conversationId,
                contactId = conversation.contact.externalId,
                contactName = conversation.contact.displayName,
                contactTitle = conversation.contact.title,
                avatarText = conversation.contact.avatarText,
                lastMessage = conversation.lastMessage?.summaryText().orEmpty(),
                lastTimestamp = conversation.lastMessage?.createdAt ?: conversation.contact.addedAt,
                unreadCount = conversation.unreadCount,
                isOnline = false,
                messages = conversation.lastMessage
                    ?.let {
                        listOf(
                            it.toChatMessage(
                                activeUserExternalId = activeUserExternalId,
                                backendBaseUrl = backendBaseUrl,
                                authToken = authToken,
                            )
                        )
                    }
                    ?: emptyList(),
            )
        },
    )
}

data class ImBootstrapState(
    val user: BackendUserDto,
    val contacts: List<Contact>,
    val conversations: List<Conversation>,
)

@Serializable
data class MessageHistoryPageDto(
    val conversationId: String,
    val messages: List<MessageRecordDto>,
    val hasMore: Boolean,
) {
    fun toChatMessages(
        activeUserExternalId: String,
        backendBaseUrl: String? = null,
        authToken: String? = null,
    ): List<ChatMessage> = messages.map {
        it.toChatMessage(
            activeUserExternalId = activeUserExternalId,
            backendBaseUrl = backendBaseUrl,
            authToken = authToken,
        )
    }
}

@Serializable
data class LocalizedTextDto(
    val english: String,
    val chinese: String,
) {
    fun toLocalizedText(): LocalizedText = LocalizedText(
        english = english,
        chinese = chinese,
    )
}

@Serializable
data class CharacterBookEntryDto(
    val keys: List<String> = emptyList(),
    val content: String = "",
    val enabled: Boolean = true,
    val insertionOrder: Int = 0,
    val caseSensitive: Boolean = false,
    val constant: Boolean = false,
    val name: String = "",
    val comment: String = "",
    val selective: Boolean = false,
    val secondaryKeys: List<String> = emptyList(),
    val extensions: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class CharacterBookDto(
    val name: String = "",
    val description: String = "",
    val scanDepth: Int? = null,
    val tokenBudget: Int? = null,
    val recursiveScanning: Boolean = false,
    val entries: List<CharacterBookEntryDto> = emptyList(),
    val extensions: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class CompanionCharacterCardDto(
    val id: String,
    val displayName: LocalizedTextDto,
    val roleLabel: LocalizedTextDto,
    val summary: LocalizedTextDto,
    val firstMes: LocalizedTextDto? = null,
    val openingLine: LocalizedTextDto? = null,
    val alternateGreetings: List<LocalizedTextDto> = emptyList(),
    val systemPrompt: LocalizedTextDto? = null,
    val personality: LocalizedTextDto? = null,
    val scenario: LocalizedTextDto? = null,
    val exampleDialogue: LocalizedTextDto? = null,
    val tags: List<String> = emptyList(),
    val creator: String = "",
    val creatorNotes: String = "",
    val characterVersion: String = "",
    val avatarText: String,
    val avatarUri: String? = null,
    val accent: String,
    val source: String,
    val extensions: JsonObject = JsonObject(emptyMap()),
    val characterBook: CharacterBookDto? = null,
) {
    fun toCompanionCharacterCard(): CompanionCharacterCard {
        val resolvedFirstMes = firstMes?.toLocalizedText()
            ?: openingLine?.toLocalizedText()
            ?: LocalizedText("", "")
        return CompanionCharacterCard(
            id = id,
            displayName = displayName.toLocalizedText(),
            roleLabel = roleLabel.toLocalizedText(),
            summary = summary.toLocalizedText(),
            firstMes = resolvedFirstMes,
            alternateGreetings = alternateGreetings.map { it.toLocalizedText() },
            systemPrompt = systemPrompt?.toLocalizedText() ?: LocalizedText("", ""),
            personality = personality?.toLocalizedText() ?: LocalizedText("", ""),
            scenario = scenario?.toLocalizedText() ?: LocalizedText("", ""),
            exampleDialogue = exampleDialogue?.toLocalizedText() ?: LocalizedText("", ""),
            tags = tags,
            creator = creator,
            creatorNotes = creatorNotes,
            characterVersion = characterVersion,
            avatarText = avatarText,
            avatarUri = avatarUri,
            accent = when (accent.lowercase()) {
                "secondary" -> AccentTone.Secondary
                "tertiary" -> AccentTone.Tertiary
                else -> AccentTone.Primary
            },
            source = when (source.lowercase()) {
                "drawn" -> CompanionCharacterSource.Drawn
                "userauthored", "user_authored", "user-authored" -> CompanionCharacterSource.UserAuthored
                else -> CompanionCharacterSource.Preset
            },
            extensions = extensions,
            characterPresetId = readCharPresetIdFromStExtensions(extensions),
        )
    }

    companion object {
        fun fromCompanionCharacterCard(card: CompanionCharacterCard): CompanionCharacterCardDto =
            CompanionCharacterCardDto(
                id = card.id,
                displayName = LocalizedTextDto(card.displayName.english, card.displayName.chinese),
                roleLabel = LocalizedTextDto(card.roleLabel.english, card.roleLabel.chinese),
                summary = LocalizedTextDto(card.summary.english, card.summary.chinese),
                firstMes = LocalizedTextDto(card.firstMes.english, card.firstMes.chinese),
                openingLine = LocalizedTextDto(card.firstMes.english, card.firstMes.chinese),
                alternateGreetings = card.alternateGreetings.map {
                    LocalizedTextDto(it.english, it.chinese)
                },
                systemPrompt = LocalizedTextDto(card.systemPrompt.english, card.systemPrompt.chinese),
                personality = LocalizedTextDto(card.personality.english, card.personality.chinese),
                scenario = LocalizedTextDto(card.scenario.english, card.scenario.chinese),
                exampleDialogue = LocalizedTextDto(
                    card.exampleDialogue.english,
                    card.exampleDialogue.chinese,
                ),
                tags = card.tags,
                creator = card.creator,
                creatorNotes = card.creatorNotes,
                characterVersion = card.characterVersion,
                avatarText = card.avatarText,
                avatarUri = card.avatarUri,
                accent = when (card.accent) {
                    AccentTone.Secondary -> "secondary"
                    AccentTone.Tertiary -> "tertiary"
                    AccentTone.Primary -> "primary"
                },
                source = when (card.source) {
                    CompanionCharacterSource.Drawn -> "drawn"
                    CompanionCharacterSource.UserAuthored -> "user_authored"
                    CompanionCharacterSource.Preset -> "preset"
                },
                extensions = mergeCharPresetIdIntoStExtensions(
                    extensions = card.extensions,
                    charPresetId = card.characterPresetId,
                ),
            )
    }
}

private fun readCharPresetIdFromStExtensions(extensions: JsonObject): String? {
    val st = extensions["st"] as? JsonObject ?: return null
    return (st["charPresetId"] as? JsonPrimitive)?.contentOrNull?.takeUnless { it.isBlank() }
}

private fun mergeCharPresetIdIntoStExtensions(
    extensions: JsonObject,
    charPresetId: String?,
): JsonObject {
    val existingSt = extensions["st"] as? JsonObject
    if (charPresetId == null && existingSt?.containsKey("charPresetId") != true) {
        return extensions
    }
    val newSt: JsonObject? = when {
        charPresetId != null -> {
            val baseStMap: Map<String, kotlinx.serialization.json.JsonElement> = existingSt ?: emptyMap()
            JsonObject(baseStMap + ("charPresetId" to JsonPrimitive(charPresetId)))
        }
        else -> {
            val pruned = existingSt!! - "charPresetId"
            if (pruned.isEmpty()) null else JsonObject(pruned)
        }
    }
    return when (newSt) {
        null -> JsonObject(extensions - "st")
        else -> JsonObject(extensions + ("st" to newSt))
    }
}

@Serializable
data class CompanionRosterDto(
    val presetCharacters: List<CompanionCharacterCardDto>,
    val ownedCharacters: List<CompanionCharacterCardDto>,
    val activeCharacterId: String? = null,
)

@Serializable
data class CompanionDrawResultDto(
    val card: CompanionCharacterCardDto,
    val wasNew: Boolean,
) {
    fun toCompanionDrawResult(): CompanionDrawResult = CompanionDrawResult(
        card = card.toCompanionCharacterCard(),
        wasNew = wasNew,
    )
}

@Serializable
data class SelectCompanionCharacterRequestDto(
    val characterId: String,
)

@Serializable
data class ActiveCompanionSelectionDto(
    val characterId: String,
)

@Serializable
data class CompanionTurnSubmitRequestDto(
    val conversationId: String,
    val activeCompanionId: String,
    val userTurnBody: String,
    val activeLanguage: String,
    val clientTurnId: String,
    val parentMessageId: String? = null,
)

@Serializable
data class CompanionTurnRecordDto(
    val turnId: String,
    val conversationId: String,
    val messageId: String,
    val variantGroupId: String,
    val variantIndex: Int,
    val parentMessageId: String? = null,
    val status: String,
    val accumulatedBody: String,
    val lastDeltaSeq: Int,
    val providerId: String? = null,
    val model: String? = null,
    val startedAt: String,
    val completedAt: String? = null,
    val failureSubtype: String? = null,
    val errorMessage: String? = null,
    val blockReason: String? = null,
)

@Serializable
data class CompanionTurnPendingListDto(
    val turns: List<CompanionTurnRecordDto>,
)

@Serializable
data class CompanionTurnRegenerateRequestDto(
    val clientTurnId: String,
)

@Serializable
data class EditUserTurnRequestDto(
    val parentMessageId: String,
    val newUserText: String,
    val clientTurnId: String,
    val activeCompanionId: String,
    val activeLanguage: String,
)

@Serializable
data class NewUserMessageRecordDto(
    val messageId: String,
    val variantGroupId: String,
    val variantIndex: Int,
    val parentMessageId: String? = null,
    val role: String,
)

@Serializable
data class EditUserTurnResponseDto(
    val userMessage: NewUserMessageRecordDto,
    val companionTurn: CompanionTurnRecordDto,
)

@Serializable
data class RegenerateAtRequestDto(
    val clientTurnId: String,
    val targetMessageId: String? = null,
)

@Serializable
data class ConversationExportLineDto(
    val messageId: String,
    val parentMessageId: String? = null,
    val variantGroupId: String,
    val variantIndex: Int,
    val role: String,
    val timestamp: String,
    val content: String,
    val extensions: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
data class RelationshipResetResponseDto(
    val ok: Boolean,
)

@Serializable
data class CardImportUploadRequestDto(
    val filename: String,
    val contentBase64: String,
    val claimedFormat: String,
)

@Serializable
data class CardImportWarningDto(
    val code: String,
    val field: String? = null,
    val detail: String? = null,
)

@Serializable
data class LorebookImportSummaryDto(
    val entryCount: Int,
    val totalTokenEstimate: Int = 0,
    val hasConstantEntries: Boolean = false,
)

@Serializable
data class CardImportPreviewDto(
    val previewToken: String,
    val card: CompanionCharacterCardDto,
    val detectedLanguage: String,
    val warnings: List<CardImportWarningDto> = emptyList(),
    val stExtensionKeys: List<String> = emptyList(),
    val lorebookSummary: LorebookImportSummaryDto? = null,
)

@Serializable
data class CardImportCommitRequestDto(
    val previewToken: String,
    val card: CompanionCharacterCardDto,
    val languageOverride: String? = null,
)

@Serializable
data class CardExportRequestDto(
    val format: String,
    val language: String,
    val includeTranslationAlt: Boolean = false,
)

@Serializable
data class CardExportWarningDto(
    val code: String,
    val field: String? = null,
    val detail: String? = null,
)

@Serializable
data class CardExportResponseDto(
    val format: String,
    val filename: String,
    val contentType: String,
    val encoding: String,
    val payload: String,
    val warnings: List<CardExportWarningDto> = emptyList(),
)

@Serializable
data class UserPersonaDto(
    val id: String,
    val displayName: LocalizedTextDto,
    val description: LocalizedTextDto,
    val isBuiltIn: Boolean = false,
    val isActive: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val extensions: JsonObject = JsonObject(emptyMap()),
) {
    fun toUserPersona(): UserPersona = UserPersona(
        id = id,
        displayName = displayName.toLocalizedText(),
        description = description.toLocalizedText(),
        isBuiltIn = isBuiltIn,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        extensions = extensions,
    )

    companion object {
        fun fromUserPersona(persona: UserPersona): UserPersonaDto = UserPersonaDto(
            id = persona.id,
            displayName = LocalizedTextDto(
                persona.displayName.english,
                persona.displayName.chinese,
            ),
            description = LocalizedTextDto(
                persona.description.english,
                persona.description.chinese,
            ),
            isBuiltIn = persona.isBuiltIn,
            isActive = persona.isActive,
            createdAt = persona.createdAt,
            updatedAt = persona.updatedAt,
            extensions = persona.extensions,
        )
    }
}

@Serializable
data class UserPersonaListDto(
    val personas: List<UserPersonaDto>,
    val activePersonaId: String? = null,
)

@Serializable
data class UserPersonaActivateRequestDto(
    val personaId: String,
)

@Serializable
data class PerLanguageStringListDto(
    val english: List<String> = emptyList(),
    val chinese: List<String> = emptyList(),
) {
    fun toLanguageMap(): Map<AppLanguage, List<String>> {
        val map = mutableMapOf<AppLanguage, List<String>>()
        if (english.isNotEmpty()) map[AppLanguage.English] = english
        if (chinese.isNotEmpty()) map[AppLanguage.Chinese] = chinese
        return map
    }

    companion object {
        fun fromLanguageMap(map: Map<AppLanguage, List<String>>): PerLanguageStringListDto =
            PerLanguageStringListDto(
                english = map[AppLanguage.English].orEmpty(),
                chinese = map[AppLanguage.Chinese].orEmpty(),
            )
    }
}

@Serializable
data class LorebookDto(
    val id: String,
    val ownerId: String,
    val displayName: LocalizedTextDto,
    val description: LocalizedTextDto = LocalizedTextDto("", ""),
    val isGlobal: Boolean = false,
    val isBuiltIn: Boolean = false,
    val tokenBudget: Int = Lorebook.DefaultTokenBudget,
    val extensions: JsonObject = JsonObject(emptyMap()),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    fun toLorebook(): Lorebook = Lorebook(
        id = id,
        ownerId = ownerId,
        displayName = displayName.toLocalizedText(),
        description = description.toLocalizedText(),
        isGlobal = isGlobal,
        isBuiltIn = isBuiltIn,
        tokenBudget = tokenBudget,
        extensions = extensions,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromLorebook(lorebook: Lorebook): LorebookDto = LorebookDto(
            id = lorebook.id,
            ownerId = lorebook.ownerId,
            displayName = LocalizedTextDto(
                lorebook.displayName.english,
                lorebook.displayName.chinese,
            ),
            description = LocalizedTextDto(
                lorebook.description.english,
                lorebook.description.chinese,
            ),
            isGlobal = lorebook.isGlobal,
            isBuiltIn = lorebook.isBuiltIn,
            tokenBudget = lorebook.tokenBudget,
            extensions = lorebook.extensions,
            createdAt = lorebook.createdAt,
            updatedAt = lorebook.updatedAt,
        )
    }
}

@Serializable
data class LorebookListDto(
    val lorebooks: List<LorebookDto>,
)

@Serializable
data class LorebookSummaryDto(
    val id: String,
    val displayName: LocalizedTextDto,
    val entryCount: Int,
    val isGlobal: Boolean = false,
    val isBuiltIn: Boolean = false,
)

@Serializable
data class CreateLorebookRequestDto(
    val displayName: LocalizedTextDto,
    val description: LocalizedTextDto = LocalizedTextDto("", ""),
    val isGlobal: Boolean = false,
    val tokenBudget: Int = Lorebook.DefaultTokenBudget,
    val extensions: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class UpdateLorebookRequestDto(
    val displayName: LocalizedTextDto? = null,
    val description: LocalizedTextDto? = null,
    val isGlobal: Boolean? = null,
    val tokenBudget: Int? = null,
    val extensions: JsonObject? = null,
)

@Serializable
data class LorebookEntryDto(
    val id: String,
    val lorebookId: String,
    val name: LocalizedTextDto,
    val keysByLang: PerLanguageStringListDto = PerLanguageStringListDto(),
    val secondaryKeysByLang: PerLanguageStringListDto = PerLanguageStringListDto(),
    val secondaryGate: String = "NONE",
    val content: LocalizedTextDto = LocalizedTextDto("", ""),
    val enabled: Boolean = true,
    val constant: Boolean = false,
    val caseSensitive: Boolean = false,
    val scanDepth: Int = LorebookEntry.DefaultScanDepth,
    val insertionOrder: Int = 0,
    val comment: String = "",
    val extensions: JsonObject = JsonObject(emptyMap()),
) {
    fun toLorebookEntry(): LorebookEntry = LorebookEntry(
        id = id,
        lorebookId = lorebookId,
        name = name.toLocalizedText(),
        keysByLang = keysByLang.toLanguageMap(),
        secondaryKeysByLang = secondaryKeysByLang.toLanguageMap(),
        secondaryGate = secondaryGate.toSecondaryGate(),
        content = content.toLocalizedText(),
        enabled = enabled,
        constant = constant,
        caseSensitive = caseSensitive,
        scanDepth = scanDepth,
        insertionOrder = insertionOrder,
        comment = comment,
        extensions = extensions,
    )

    companion object {
        fun fromLorebookEntry(entry: LorebookEntry): LorebookEntryDto = LorebookEntryDto(
            id = entry.id,
            lorebookId = entry.lorebookId,
            name = LocalizedTextDto(entry.name.english, entry.name.chinese),
            keysByLang = PerLanguageStringListDto.fromLanguageMap(entry.keysByLang),
            secondaryKeysByLang = PerLanguageStringListDto.fromLanguageMap(
                entry.secondaryKeysByLang,
            ),
            secondaryGate = entry.secondaryGate.toWireName(),
            content = LocalizedTextDto(entry.content.english, entry.content.chinese),
            enabled = entry.enabled,
            constant = entry.constant,
            caseSensitive = entry.caseSensitive,
            scanDepth = entry.scanDepth,
            insertionOrder = entry.insertionOrder,
            comment = entry.comment,
            extensions = entry.extensions,
        )
    }
}

@Serializable
data class LorebookEntryListDto(
    val entries: List<LorebookEntryDto>,
)

@Serializable
data class CreateLorebookEntryRequestDto(
    val name: LocalizedTextDto,
    val keysByLang: PerLanguageStringListDto = PerLanguageStringListDto(),
    val secondaryKeysByLang: PerLanguageStringListDto = PerLanguageStringListDto(),
    val secondaryGate: String = "NONE",
    val content: LocalizedTextDto = LocalizedTextDto("", ""),
    val enabled: Boolean = true,
    val constant: Boolean = false,
    val caseSensitive: Boolean = false,
    val scanDepth: Int = LorebookEntry.DefaultScanDepth,
    val insertionOrder: Int = 0,
    val comment: String = "",
    val extensions: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class UpdateLorebookEntryRequestDto(
    val name: LocalizedTextDto? = null,
    val keysByLang: PerLanguageStringListDto? = null,
    val secondaryKeysByLang: PerLanguageStringListDto? = null,
    val secondaryGate: String? = null,
    val content: LocalizedTextDto? = null,
    val enabled: Boolean? = null,
    val constant: Boolean? = null,
    val caseSensitive: Boolean? = null,
    val scanDepth: Int? = null,
    val insertionOrder: Int? = null,
    val comment: String? = null,
    val extensions: JsonObject? = null,
)

@Serializable
data class LorebookBindingDto(
    val lorebookId: String,
    val characterId: String,
    val isPrimary: Boolean = false,
) {
    fun toLorebookBinding(): LorebookBinding = LorebookBinding(
        lorebookId = lorebookId,
        characterId = characterId,
        isPrimary = isPrimary,
    )

    companion object {
        fun fromLorebookBinding(binding: LorebookBinding): LorebookBindingDto = LorebookBindingDto(
            lorebookId = binding.lorebookId,
            characterId = binding.characterId,
            isPrimary = binding.isPrimary,
        )
    }
}

@Serializable
data class LorebookBindingListDto(
    val bindings: List<LorebookBindingDto>,
)

@Serializable
data class CreateLorebookBindingRequestDto(
    val characterId: String,
    val isPrimary: Boolean = false,
)

@Serializable
data class UpdateLorebookBindingRequestDto(
    val isPrimary: Boolean,
)

@Serializable
data class WorldInfoDebugScanRequestDto(
    val characterId: String,
    val scanText: String,
)

@Serializable
data class WorldInfoDebugMatchDto(
    val entryId: String,
    val lorebookId: String,
    val insertionOrder: Int,
    val constant: Boolean = false,
    val matchedKey: String? = null,
    val language: String? = null,
)

@Serializable
data class WorldInfoDebugScanResponseDto(
    val matches: List<WorldInfoDebugMatchDto> = emptyList(),
)

@Serializable
data class CompanionMemoryDto(
    val userId: String,
    val companionCardId: String,
    val summary: LocalizedTextDto = LocalizedTextDto("", ""),
    val summaryUpdatedAt: Long = 0L,
    val summaryTurnCursor: Int = 0,
    val tokenBudgetHint: Int? = null,
) {
    fun toCompanionMemory(): CompanionMemory = CompanionMemory(
        userId = userId,
        companionCardId = companionCardId,
        summary = summary.toLocalizedText(),
        summaryUpdatedAt = summaryUpdatedAt,
        summaryTurnCursor = summaryTurnCursor,
        tokenBudgetHint = tokenBudgetHint,
    )

    companion object {
        fun fromCompanionMemory(memory: CompanionMemory): CompanionMemoryDto = CompanionMemoryDto(
            userId = memory.userId,
            companionCardId = memory.companionCardId,
            summary = LocalizedTextDto(memory.summary.english, memory.summary.chinese),
            summaryUpdatedAt = memory.summaryUpdatedAt,
            summaryTurnCursor = memory.summaryTurnCursor,
            tokenBudgetHint = memory.tokenBudgetHint,
        )
    }
}

@Serializable
data class CompanionMemoryPinDto(
    val id: String,
    val sourceMessageId: String? = null,
    val text: LocalizedTextDto = LocalizedTextDto("", ""),
    val createdAt: Long = 0L,
    val pinnedByUser: Boolean = true,
) {
    fun toCompanionMemoryPin(): CompanionMemoryPin = CompanionMemoryPin(
        id = id,
        sourceMessageId = sourceMessageId,
        text = text.toLocalizedText(),
        createdAt = createdAt,
        pinnedByUser = pinnedByUser,
    )

    companion object {
        fun fromCompanionMemoryPin(pin: CompanionMemoryPin): CompanionMemoryPinDto = CompanionMemoryPinDto(
            id = pin.id,
            sourceMessageId = pin.sourceMessageId,
            text = LocalizedTextDto(pin.text.english, pin.text.chinese),
            createdAt = pin.createdAt,
            pinnedByUser = pin.pinnedByUser,
        )
    }
}

@Serializable
data class CompanionMemoryPinListDto(
    val pins: List<CompanionMemoryPinDto> = emptyList(),
)

@Serializable
data class CompanionMemoryResetRequestDto(
    val scope: String,
) {
    fun toCompanionMemoryResetScope(): CompanionMemoryResetScope = when (scope.lowercase()) {
        "summary" -> CompanionMemoryResetScope.Summary
        "all" -> CompanionMemoryResetScope.All
        else -> CompanionMemoryResetScope.Pins
    }

    companion object {
        fun fromCompanionMemoryResetScope(
            scope: CompanionMemoryResetScope,
        ): CompanionMemoryResetRequestDto = CompanionMemoryResetRequestDto(
            scope = scope.toWireKey(),
        )
    }
}

@Serializable
data class PresetParamsDto(
    val temperature: Double? = null,
    val topP: Double? = null,
    val maxReplyTokens: Int? = null,
) {
    fun toPresetParams(): PresetParams = PresetParams(
        temperature = temperature,
        topP = topP,
        maxReplyTokens = maxReplyTokens,
    )

    companion object {
        fun fromPresetParams(params: PresetParams): PresetParamsDto = PresetParamsDto(
            temperature = params.temperature,
            topP = params.topP,
            maxReplyTokens = params.maxReplyTokens,
        )
    }
}

@Serializable
data class PresetTemplateDto(
    val systemPrefix: LocalizedTextDto = LocalizedTextDto("", ""),
    val systemSuffix: LocalizedTextDto = LocalizedTextDto("", ""),
    val formatInstructions: LocalizedTextDto = LocalizedTextDto("", ""),
    val postHistoryInstructions: LocalizedTextDto = LocalizedTextDto("", ""),
) {
    fun toPresetTemplate(): PresetTemplate = PresetTemplate(
        systemPrefix = systemPrefix.toLocalizedText(),
        systemSuffix = systemSuffix.toLocalizedText(),
        formatInstructions = formatInstructions.toLocalizedText(),
        postHistoryInstructions = postHistoryInstructions.toLocalizedText(),
    )

    companion object {
        fun fromPresetTemplate(template: PresetTemplate): PresetTemplateDto = PresetTemplateDto(
            systemPrefix = LocalizedTextDto(
                template.systemPrefix.english,
                template.systemPrefix.chinese,
            ),
            systemSuffix = LocalizedTextDto(
                template.systemSuffix.english,
                template.systemSuffix.chinese,
            ),
            formatInstructions = LocalizedTextDto(
                template.formatInstructions.english,
                template.formatInstructions.chinese,
            ),
            postHistoryInstructions = LocalizedTextDto(
                template.postHistoryInstructions.english,
                template.postHistoryInstructions.chinese,
            ),
        )
    }
}

@Serializable
data class PresetDto(
    val id: String,
    val displayName: LocalizedTextDto,
    val description: LocalizedTextDto = LocalizedTextDto("", ""),
    val template: PresetTemplateDto = PresetTemplateDto(),
    val params: PresetParamsDto = PresetParamsDto(),
    val isBuiltIn: Boolean = false,
    val isActive: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val extensions: JsonObject = JsonObject(emptyMap()),
) {
    fun toPreset(): Preset = Preset(
        id = id,
        displayName = displayName.toLocalizedText(),
        description = description.toLocalizedText(),
        template = template.toPresetTemplate(),
        params = params.toPresetParams(),
        isBuiltIn = isBuiltIn,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        extensions = extensions,
    )

    companion object {
        fun fromPreset(preset: Preset): PresetDto = PresetDto(
            id = preset.id,
            displayName = LocalizedTextDto(
                preset.displayName.english,
                preset.displayName.chinese,
            ),
            description = LocalizedTextDto(
                preset.description.english,
                preset.description.chinese,
            ),
            template = PresetTemplateDto.fromPresetTemplate(preset.template),
            params = PresetParamsDto.fromPresetParams(preset.params),
            isBuiltIn = preset.isBuiltIn,
            isActive = preset.isActive,
            createdAt = preset.createdAt,
            updatedAt = preset.updatedAt,
            extensions = preset.extensions,
        )
    }
}

@Serializable
data class PresetListDto(
    val presets: List<PresetDto> = emptyList(),
    val activePresetId: String? = null,
)

@Serializable
data class PresetActivateRequestDto(
    val presetId: String,
)

private fun CompanionMemoryResetScope.toWireKey(): String = when (this) {
    CompanionMemoryResetScope.Pins -> "pins"
    CompanionMemoryResetScope.Summary -> "summary"
    CompanionMemoryResetScope.All -> "all"
}

private fun String.toSecondaryGate(): SecondaryGate = when (uppercase()) {
    "AND" -> SecondaryGate.And
    "OR" -> SecondaryGate.Or
    else -> SecondaryGate.None
}

private fun SecondaryGate.toWireName(): String = when (this) {
    SecondaryGate.None -> "NONE"
    SecondaryGate.And -> "AND"
    SecondaryGate.Or -> "OR"
}

private fun MessageAttachmentDto.toMessageAttachment(
    backendBaseUrl: String?,
    authToken: String?,
): MessageAttachment {
    val resolvedPreview = if (backendBaseUrl.isNullOrBlank() || fetchPath.startsWith("http")) {
        fetchPath
    } else {
        val normalizedBase = if (backendBaseUrl.endsWith("/")) backendBaseUrl.dropLast(1) else backendBaseUrl
        "$normalizedBase$fetchPath"
    }
    return MessageAttachment(
        type = when (type.lowercase()) {
            "video" -> AttachmentType.Video
            else -> AttachmentType.Image
        },
        preview = resolvedPreview,
        authToken = authToken,
    )
}

private fun MessageRecordDto.summaryText(): String = when {
    body.isNotBlank() -> body
    attachment?.type?.lowercase() == "video" -> "Sent a video"
    attachment != null -> "Sent an image"
    else -> ""
}

sealed interface ImGatewayEvent {
    @Serializable
    data class SessionRegistered(
        val connectionId: String,
        val activeConnections: Int,
        val user: BackendUserDto,
    ) : ImGatewayEvent

    @Serializable
    data class Pong(val at: String) : ImGatewayEvent

    @Serializable
    data class MessageSent(
        val conversationId: String,
        val message: MessageRecordDto,
    ) : ImGatewayEvent

    @Serializable
    data class MessageReceived(
        val conversationId: String,
        val unreadCount: Int,
        val message: MessageRecordDto,
    ) : ImGatewayEvent

    @Serializable
    data class MessageDelivered(
        val conversationId: String,
        val messageId: String,
        val recipientExternalId: String,
        val deliveredAt: String,
    ) : ImGatewayEvent

    @Serializable
    data class MessageRead(
        val conversationId: String,
        val messageId: String,
        val readerExternalId: String,
        val unreadCount: Int,
        val readAt: String,
    ) : ImGatewayEvent

    @Serializable
    data class Error(
        val code: String,
        val message: String,
    ) : ImGatewayEvent

    @Serializable
    data class FriendRequestReceived(
        val requestId: String,
        val fromUser: BackendUserDto,
    ) : ImGatewayEvent

    @Serializable
    data class FriendRequestAccepted(
        val requestId: String,
        val byUser: BackendUserDto,
    ) : ImGatewayEvent

    @Serializable
    data class FriendRequestRejected(
        val requestId: String,
        val byUserId: String,
    ) : ImGatewayEvent

    @Serializable
    data class CompanionTurnStarted(
        val turnId: String,
        val conversationId: String,
        val messageId: String,
        val variantGroupId: String,
        val variantIndex: Int,
        val parentMessageId: String? = null,
        val providerId: String? = null,
        val model: String? = null,
    ) : ImGatewayEvent

    @Serializable
    data class CompanionTurnDelta(
        val turnId: String,
        val conversationId: String,
        val messageId: String,
        val deltaSeq: Int,
        val textDelta: String,
    ) : ImGatewayEvent

    @Serializable
    data class CompanionTurnCompleted(
        val turnId: String,
        val conversationId: String,
        val messageId: String,
        val finalBody: String,
        val completedAt: String,
    ) : ImGatewayEvent

    @Serializable
    data class CompanionTurnFailed(
        val turnId: String,
        val conversationId: String,
        val messageId: String,
        val subtype: String,
        val errorMessage: String? = null,
        val completedAt: String? = null,
        /**
         * Optional retry hint in milliseconds — populated server-side from
         * the upstream `Retry-After` header on HTTP 429 responses (per the
         * paired backend slice `companion-turn-backend-recovery-followups`
         * §1). Absent for non-rate-limit failures or when the header is
         * missing / unparseable; older payloads that omit the field still
         * decode (kotlinx-serialization treats absent + default-valued as
         * the same case). The repository converts this relative ms into an
         * absolute `retryAfterEpochMs` on `CompanionTurnMeta` so the bubble
         * countdown survives reconnects without drift.
         */
        val retryAfterMs: Long? = null,
    ) : ImGatewayEvent {
        val subtypeAsFailedSubtype: FailedSubtype
            get() = FailedSubtype.fromWireKey(subtype)
    }

    @Serializable
    data class CompanionTurnBlocked(
        val turnId: String,
        val conversationId: String,
        val messageId: String,
        val reason: String,
        val completedAt: String? = null,
    ) : ImGatewayEvent {
        val reasonAsBlockReason: BlockReason
            get() = BlockReason.fromWireKey(reason)
    }

    @Serializable
    data class CompanionTurnTimeout(
        val turnId: String,
        val conversationId: String,
        val messageId: String,
        val elapsedMs: Long,
        val completedAt: String? = null,
    ) : ImGatewayEvent
}

object ImGatewayEventParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(payload: String): ImGatewayEvent {
        val element = json.parseToJsonElement(payload) as JsonObject
        return when (element["type"]?.jsonPrimitive?.content) {
            "session.registered" -> json.decodeFromJsonElement<ImGatewayEvent.SessionRegistered>(element)
            "pong" -> json.decodeFromJsonElement<ImGatewayEvent.Pong>(element)
            "message.sent" -> json.decodeFromJsonElement<ImGatewayEvent.MessageSent>(element)
            "message.received" -> json.decodeFromJsonElement<ImGatewayEvent.MessageReceived>(element)
            "message.delivered" -> json.decodeFromJsonElement<ImGatewayEvent.MessageDelivered>(element)
            "message.read" -> json.decodeFromJsonElement<ImGatewayEvent.MessageRead>(element)
            "error" -> json.decodeFromJsonElement<ImGatewayEvent.Error>(element)
            "friend_request.received" -> json.decodeFromJsonElement<ImGatewayEvent.FriendRequestReceived>(element)
            "friend_request.accepted" -> json.decodeFromJsonElement<ImGatewayEvent.FriendRequestAccepted>(element)
            "friend_request.rejected" -> json.decodeFromJsonElement<ImGatewayEvent.FriendRequestRejected>(element)
            "companion_turn.started" -> json.decodeFromJsonElement<ImGatewayEvent.CompanionTurnStarted>(element)
            "companion_turn.delta" -> json.decodeFromJsonElement<ImGatewayEvent.CompanionTurnDelta>(element)
            "companion_turn.completed" -> json.decodeFromJsonElement<ImGatewayEvent.CompanionTurnCompleted>(element)
            "companion_turn.failed" -> json.decodeFromJsonElement<ImGatewayEvent.CompanionTurnFailed>(element)
            "companion_turn.blocked" -> json.decodeFromJsonElement<ImGatewayEvent.CompanionTurnBlocked>(element)
            "companion_turn.timeout" -> json.decodeFromJsonElement<ImGatewayEvent.CompanionTurnTimeout>(element)
            else -> throw IllegalArgumentException("Unsupported gateway event type")
        }
    }
}

@Serializable
data class ContentPolicyAcknowledgmentDto(
    val accepted: Boolean,
    val version: String,
    val acceptedAtMillis: Long?,
)

@Serializable
data class ContentPolicyAcknowledgmentRequestDto(
    val version: String,
)
