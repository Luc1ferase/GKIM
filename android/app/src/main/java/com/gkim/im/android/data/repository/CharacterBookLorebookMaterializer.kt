package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Lorebook
import com.gkim.im.android.core.model.LorebookEntry
import com.gkim.im.android.data.remote.im.CharacterBookDto
import com.gkim.im.android.data.remote.im.CharacterBookEntryDto
import com.gkim.im.android.data.remote.im.CreateLorebookBindingRequestDto
import com.gkim.im.android.data.remote.im.CreateLorebookEntryRequestDto
import com.gkim.im.android.data.remote.im.CreateLorebookRequestDto
import com.gkim.im.android.data.remote.im.ImWorldInfoClient
import com.gkim.im.android.data.remote.im.LocalizedTextDto
import com.gkim.im.android.data.remote.im.PerLanguageStringListDto
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

data class CharacterBookMaterializationResult(
    val lorebookId: String,
    val entryIds: List<String>,
    val characterId: String,
)

class CharacterBookLorebookMaterializer(
    private val worldInfoClient: ImWorldInfoClient,
) {
    suspend fun materialize(
        baseUrl: String,
        token: String,
        characterId: String,
        characterBook: CharacterBookDto,
        characterDisplayName: LocalizedText,
        importLanguage: AppLanguage,
    ): Result<CharacterBookMaterializationResult> = runCatching {
        val lorebook = worldInfoClient.create(
            baseUrl = baseUrl,
            token = token,
            request = buildLorebookRequest(characterBook, characterDisplayName),
        )

        val entryIds = mutableListOf<String>()
        try {
            characterBook.entries.forEach { entry ->
                val created = worldInfoClient.createEntry(
                    baseUrl = baseUrl,
                    token = token,
                    lorebookId = lorebook.id,
                    request = buildEntryRequest(entry, importLanguage),
                )
                entryIds += created.id
            }

            worldInfoClient.bind(
                baseUrl = baseUrl,
                token = token,
                lorebookId = lorebook.id,
                characterId = characterId,
                isPrimary = true,
            )
        } catch (t: Throwable) {
            runCatching { worldInfoClient.delete(baseUrl, token, lorebook.id) }
            throw t
        }

        CharacterBookMaterializationResult(
            lorebookId = lorebook.id,
            entryIds = entryIds.toList(),
            characterId = characterId,
        )
    }

    companion object {
        internal fun buildLorebookRequest(
            book: CharacterBookDto,
            characterDisplayName: LocalizedText,
        ): CreateLorebookRequestDto {
            val english = book.name.ifBlank {
                val fallback = characterDisplayName.english.trim()
                if (fallback.isEmpty()) "Imported lorebook" else "$fallback lorebook"
            }
            val chinese = run {
                val fallback = characterDisplayName.chinese.trim()
                if (fallback.isEmpty()) "" else "$fallback 世界书"
            }
            return CreateLorebookRequestDto(
                displayName = LocalizedTextDto(english = english, chinese = chinese),
                description = LocalizedTextDto(english = book.description, chinese = ""),
                isGlobal = false,
                tokenBudget = book.tokenBudget ?: Lorebook.DefaultTokenBudget,
                extensions = buildLorebookExtensions(book),
            )
        }

        internal fun buildEntryRequest(
            entry: CharacterBookEntryDto,
            importLanguage: AppLanguage,
        ): CreateLorebookEntryRequestDto {
            val keysDto = perLanguageList(entry.keys, importLanguage)
            val secondaryKeysDto = perLanguageList(entry.secondaryKeys, importLanguage)
            val scanDepthCandidate = readScanDepth(entry)
            val scanDepth = scanDepthCandidate
                ?.coerceIn(0, LorebookEntry.MaxServerScanDepth)
                ?: LorebookEntry.DefaultScanDepth
            return CreateLorebookEntryRequestDto(
                name = perLanguageText(entry.name, importLanguage),
                keysByLang = keysDto,
                secondaryKeysByLang = secondaryKeysDto,
                secondaryGate = if (entry.selective) "AND" else "NONE",
                content = perLanguageText(entry.content, importLanguage),
                enabled = entry.enabled,
                constant = entry.constant,
                caseSensitive = entry.caseSensitive,
                scanDepth = scanDepth,
                insertionOrder = entry.insertionOrder,
                comment = entry.comment,
                extensions = buildEntryExtensions(entry),
            )
        }

        private fun perLanguageText(
            value: String,
            language: AppLanguage,
        ): LocalizedTextDto = when (language) {
            AppLanguage.Chinese -> LocalizedTextDto(english = "", chinese = value)
            AppLanguage.English -> LocalizedTextDto(english = value, chinese = "")
        }

        private fun perLanguageList(
            values: List<String>,
            language: AppLanguage,
        ): PerLanguageStringListDto = if (values.isEmpty()) {
            PerLanguageStringListDto()
        } else when (language) {
            AppLanguage.Chinese -> PerLanguageStringListDto(chinese = values)
            AppLanguage.English -> PerLanguageStringListDto(english = values)
        }

        private fun buildLorebookExtensions(book: CharacterBookDto): JsonObject = buildJsonObject {
            put("st", buildJsonObject {
                if (book.name.isNotEmpty()) put("name", JsonPrimitive(book.name))
                if (book.scanDepth != null) put("scan_depth", JsonPrimitive(book.scanDepth))
                put("recursive_scanning", JsonPrimitive(book.recursiveScanning))
                put("extensions", book.extensions)
            })
        }

        private fun buildEntryExtensions(entry: CharacterBookEntryDto): JsonObject = buildJsonObject {
            put("st", buildJsonObject {
                put("selective", JsonPrimitive(entry.selective))
                put("extensions", entry.extensions)
            })
        }

        private fun readScanDepth(entry: CharacterBookEntryDto): Int? {
            val raw = entry.extensions["depth"] as? JsonPrimitive ?: return null
            return raw.content.toIntOrNull()
        }
    }
}
