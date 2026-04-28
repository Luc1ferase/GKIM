package com.gkim.im.android.data.repository

import com.gkim.im.android.core.interop.SillyTavernCardCodec
import com.gkim.im.android.core.interop.SillyTavernCardFormat
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.data.remote.im.CardImportCommitRequestDto
import com.gkim.im.android.data.remote.im.CardImportPreviewDto
import com.gkim.im.android.data.remote.im.CardImportWarningDto
import com.gkim.im.android.data.remote.im.CharacterBookDto
import com.gkim.im.android.data.remote.im.CompanionCharacterCardDto
import com.gkim.im.android.data.remote.im.ImBackendClient

data class CardImportWarning(
    val code: String,
    val field: String? = null,
    val detail: String? = null,
)

data class LorebookImportSummary(
    val entryCount: Int,
    val totalTokenEstimate: Int,
    val hasConstantEntries: Boolean,
)

data class CardImportPreview(
    val previewToken: String,
    val card: CompanionCharacterCard,
    val rawCardDto: CompanionCharacterCardDto,
    val detectedLanguage: String,
    val warnings: List<CardImportWarning>,
    val stExtensionKeys: List<String>,
    val lorebookSummary: LorebookImportSummary? = null,
)

enum class ExportedCardFormat(val wireValue: String) {
    Png("png"),
    Json("json"),
    ;

    companion object {
        fun fromWire(value: String): ExportedCardFormat = when (value.lowercase()) {
            "json" -> Json
            else -> Png
        }
    }
}

data class CardExportWarning(
    val code: String,
    val field: String? = null,
    val detail: String? = null,
)

data class ExportedCardPayload(
    val format: ExportedCardFormat,
    val filename: String,
    val contentType: String,
    val bytes: ByteArray,
    val warnings: List<CardExportWarning> = emptyList(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExportedCardPayload) return false
        return format == other.format &&
            filename == other.filename &&
            contentType == other.contentType &&
            bytes.contentEquals(other.bytes) &&
            warnings == other.warnings
    }

    override fun hashCode(): Int {
        var result = format.hashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + warnings.hashCode()
        return result
    }
}

interface CardInteropRepository {
    suspend fun previewImport(bytes: ByteArray, filename: String): Result<CardImportPreview>
    suspend fun commitImport(
        preview: CardImportPreview,
        overrides: CompanionCharacterCard? = null,
        languageOverride: String? = null,
    ): Result<CompanionCharacterCard>
    suspend fun exportCard(
        cardId: String,
        format: ExportedCardFormat,
        language: String,
        includeTranslationAlt: Boolean = false,
    ): Result<ExportedCardPayload>
}

class DefaultCardInteropRepository(
    private val delegate: CardInteropRepository,
) : CardInteropRepository {
    override suspend fun previewImport(bytes: ByteArray, filename: String): Result<CardImportPreview> {
        if (bytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("empty_payload"))
        }
        val format = SillyTavernCardCodec.detectFormat(bytes)
        if (format == SillyTavernCardFormat.Unknown) {
            return Result.failure(IllegalArgumentException("unsupported_format"))
        }
        if (format == SillyTavernCardFormat.Png && !SillyTavernCardCodec.fitsPngSizeLimit(bytes)) {
            return Result.failure(IllegalArgumentException("payload_too_large"))
        }
        if (format == SillyTavernCardFormat.Json && !SillyTavernCardCodec.fitsJsonSizeLimit(bytes)) {
            return Result.failure(IllegalArgumentException("payload_too_large"))
        }
        return delegate.previewImport(bytes, filename)
    }

    override suspend fun commitImport(
        preview: CardImportPreview,
        overrides: CompanionCharacterCard?,
        languageOverride: String?,
    ): Result<CompanionCharacterCard> = delegate.commitImport(preview, overrides, languageOverride)

    override suspend fun exportCard(
        cardId: String,
        format: ExportedCardFormat,
        language: String,
        includeTranslationAlt: Boolean,
    ): Result<ExportedCardPayload> = delegate.exportCard(cardId, format, language, includeTranslationAlt)
}

class LiveCardInteropRepository(
    private val backendClient: ImBackendClient,
    private val baseUrlProvider: () -> String?,
    private val tokenProvider: () -> String?,
    private val characterBookMaterializer: CharacterBookLorebookMaterializer? = null,
) : CardInteropRepository {
    override suspend fun previewImport(bytes: ByteArray, filename: String): Result<CardImportPreview> {
        val baseUrl = baseUrlProvider() ?: return Result.failure(IllegalStateException("missing_base_url"))
        val token = tokenProvider() ?: return Result.failure(IllegalStateException("missing_token"))
        return runCatching {
            val dto = backendClient.importCardPreview(baseUrl, token, bytes, filename)
            dto.toDomain()
        }
    }

    override suspend fun commitImport(
        preview: CardImportPreview,
        overrides: CompanionCharacterCard?,
        languageOverride: String?,
    ): Result<CompanionCharacterCard> {
        val baseUrl = baseUrlProvider() ?: return Result.failure(IllegalStateException("missing_base_url"))
        val token = tokenProvider() ?: return Result.failure(IllegalStateException("missing_token"))
        val overrideDto = overrides?.let { CompanionCharacterCardDto.fromCompanionCharacterCard(it) }
        val request = CardImportCommitRequestDto(
            previewToken = preview.previewToken,
            card = overrideDto ?: preview.rawCardDto,
            languageOverride = languageOverride,
        )
        return runCatching {
            val committedDto = backendClient.importCardCommit(
                baseUrl = baseUrl,
                token = token,
                preview = CardImportPreviewDto(
                    previewToken = request.previewToken,
                    card = request.card,
                    detectedLanguage = preview.detectedLanguage,
                    warnings = preview.warnings.map {
                        CardImportWarningDto(it.code, it.field, it.detail)
                    },
                    stExtensionKeys = preview.stExtensionKeys,
                    lorebookSummary = preview.lorebookSummary?.let {
                        com.gkim.im.android.data.remote.im.LorebookImportSummaryDto(
                            entryCount = it.entryCount,
                            totalTokenEstimate = it.totalTokenEstimate,
                            hasConstantEntries = it.hasConstantEntries,
                        )
                    },
                ),
                overrides = overrideDto,
                languageOverride = languageOverride,
            )
            val committed = committedDto.toCompanionCharacterCard()
            val characterBook = committedDto.characterBook
                ?: overrideDto?.characterBook
                ?: preview.rawCardDto.characterBook
            materializeCharacterBook(
                baseUrl = baseUrl,
                token = token,
                characterId = committed.id,
                characterDisplayName = committed.displayName,
                characterBook = characterBook,
                languageOverride = languageOverride,
                detectedLanguage = preview.detectedLanguage,
            )
            committed
        }
    }

    private suspend fun materializeCharacterBook(
        baseUrl: String,
        token: String,
        characterId: String,
        characterDisplayName: LocalizedText,
        characterBook: CharacterBookDto?,
        languageOverride: String?,
        detectedLanguage: String,
    ) {
        val materializer = characterBookMaterializer ?: return
        if (characterBook == null) return
        val importLanguage = resolveImportLanguage(languageOverride ?: detectedLanguage)
        materializer.materialize(
            baseUrl = baseUrl,
            token = token,
            characterId = characterId,
            characterBook = characterBook,
            characterDisplayName = characterDisplayName,
            importLanguage = importLanguage,
        ).getOrThrow()
    }

    private fun resolveImportLanguage(language: String): AppLanguage = when (language.lowercase()) {
        "zh", "zh-cn", "zh_cn", "chinese" -> AppLanguage.Chinese
        else -> AppLanguage.English
    }

    override suspend fun exportCard(
        cardId: String,
        format: ExportedCardFormat,
        language: String,
        includeTranslationAlt: Boolean,
    ): Result<ExportedCardPayload> {
        val baseUrl = baseUrlProvider() ?: return Result.failure(IllegalStateException("missing_base_url"))
        val token = tokenProvider() ?: return Result.failure(IllegalStateException("missing_token"))
        return runCatching {
            val dto = backendClient.exportCard(
                baseUrl = baseUrl,
                token = token,
                cardId = cardId,
                format = format.wireValue,
                language = language,
                includeTranslationAlt = includeTranslationAlt,
            )
            val decoded = when (dto.encoding.lowercase()) {
                "base64" -> java.util.Base64.getDecoder().decode(dto.payload)
                "utf8", "utf-8" -> dto.payload.toByteArray(Charsets.UTF_8)
                else -> dto.payload.toByteArray(Charsets.UTF_8)
            }
            ExportedCardPayload(
                format = ExportedCardFormat.fromWire(dto.format),
                filename = dto.filename,
                contentType = dto.contentType,
                bytes = decoded,
                warnings = dto.warnings.map { CardExportWarning(it.code, it.field, it.detail) },
            )
        }
    }
}

private fun CardImportPreviewDto.toDomain(): CardImportPreview = CardImportPreview(
    previewToken = previewToken,
    card = card.toCompanionCharacterCard(),
    rawCardDto = card,
    detectedLanguage = detectedLanguage,
    warnings = warnings.map { CardImportWarning(it.code, it.field, it.detail) },
    stExtensionKeys = stExtensionKeys,
    lorebookSummary = lorebookSummary?.let {
        LorebookImportSummary(
            entryCount = it.entryCount,
            totalTokenEstimate = it.totalTokenEstimate,
            hasConstantEntries = it.hasConstantEntries,
        )
    },
)
