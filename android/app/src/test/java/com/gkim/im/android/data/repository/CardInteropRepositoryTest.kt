package com.gkim.im.android.data.repository

import com.gkim.im.android.core.interop.SillyTavernCardCodec
import com.gkim.im.android.data.remote.im.ActiveCompanionSelectionDto
import com.gkim.im.android.data.remote.im.AuthResponseDto
import com.gkim.im.android.data.remote.im.BootstrapBundleDto
import com.gkim.im.android.data.remote.im.CardExportResponseDto
import com.gkim.im.android.data.remote.im.CardImportPreviewDto
import com.gkim.im.android.data.remote.im.CardImportWarningDto
import com.gkim.im.android.data.remote.im.CompanionCharacterCardDto
import com.gkim.im.android.data.remote.im.CompanionDrawResultDto
import com.gkim.im.android.data.remote.im.CompanionRosterDto
import com.gkim.im.android.data.remote.im.DevSessionResponseDto
import com.gkim.im.android.data.remote.im.FriendRequestViewDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.LocalizedTextDto
import com.gkim.im.android.data.remote.im.MessageHistoryPageDto
import com.gkim.im.android.data.remote.im.SendDirectImageMessageRequestDto
import com.gkim.im.android.data.remote.im.SendDirectMessageResultDto
import com.gkim.im.android.data.remote.im.UserSearchResultDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardInteropRepositoryTest {
    private val pngSignature = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )

    @Test
    fun `previewImport rejects unknown format before contacting backend`() = runBlocking {
        val backend = FakeCardBackendClient()
        val repo = DefaultCardInteropRepository(
            delegate = LiveCardInteropRepository(backend, { "http://example/" }, { "token" }),
        )
        val result = repo.previewImport("not a card".toByteArray(), "mystery.bin")
        assertTrue(result.isFailure)
        assertEquals("unsupported_format", result.exceptionOrNull()?.message)
        assertEquals(0, backend.importPreviewCalls)
    }

    @Test
    fun `previewImport rejects empty payload before contacting backend`() = runBlocking {
        val backend = FakeCardBackendClient()
        val repo = DefaultCardInteropRepository(
            delegate = LiveCardInteropRepository(backend, { "http://example/" }, { "token" }),
        )
        val result = repo.previewImport(ByteArray(0), "empty.png")
        assertTrue(result.isFailure)
        assertEquals("empty_payload", result.exceptionOrNull()?.message)
        assertEquals(0, backend.importPreviewCalls)
    }

    @Test
    fun `previewImport rejects oversized PNG before contacting backend`() = runBlocking {
        val backend = FakeCardBackendClient()
        val repo = DefaultCardInteropRepository(
            delegate = LiveCardInteropRepository(backend, { "http://example/" }, { "token" }),
        )
        val huge = pngSignature + ByteArray(SillyTavernCardCodec.MaxPngBytes)
        val result = repo.previewImport(huge, "huge.png")
        assertTrue(result.isFailure)
        assertEquals("payload_too_large", result.exceptionOrNull()?.message)
        assertEquals(0, backend.importPreviewCalls)
    }

    @Test
    fun `previewImport rejects oversized JSON before contacting backend`() = runBlocking {
        val backend = FakeCardBackendClient()
        val repo = DefaultCardInteropRepository(
            delegate = LiveCardInteropRepository(backend, { "http://example/" }, { "token" }),
        )
        val big = ByteArray(SillyTavernCardCodec.MaxJsonBytes + 1) { 0x20 }
        big[0] = '{'.code.toByte()
        val result = repo.previewImport(big, "fat.json")
        assertTrue(result.isFailure)
        assertEquals("payload_too_large", result.exceptionOrNull()?.message)
        assertEquals(0, backend.importPreviewCalls)
    }

    @Test
    fun `previewImport maps backend preview to domain with warnings`() = runBlocking {
        val backend = FakeCardBackendClient(
            previewResponse = CardImportPreviewDto(
                previewToken = "preview-x",
                card = sampleCardDto("card-1"),
                detectedLanguage = "en",
                warnings = listOf(
                    CardImportWarningDto("post_history_instruction_parked", "stPostHistoryInstructions"),
                    CardImportWarningDto("field_truncated", "personality", "> 32 KiB"),
                ),
                stExtensionKeys = listOf("stPostHistoryInstructions"),
            ),
        )
        val repo = DefaultCardInteropRepository(
            delegate = LiveCardInteropRepository(backend, { "http://example/" }, { "token" }),
        )
        val result = repo.previewImport(pngSignature + ByteArray(16), "aria.png")
        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        assertEquals("preview-x", preview.previewToken)
        assertEquals("en", preview.detectedLanguage)
        assertEquals("card-1", preview.card.id)
        assertEquals(2, preview.warnings.size)
        assertEquals("post_history_instruction_parked", preview.warnings.first().code)
        assertEquals(listOf("stPostHistoryInstructions"), preview.stExtensionKeys)
        assertEquals(1, backend.importPreviewCalls)
    }

    @Test
    fun `previewImport surfaces missing base url as failure`() = runBlocking {
        val backend = FakeCardBackendClient()
        val repo = DefaultCardInteropRepository(
            delegate = LiveCardInteropRepository(backend, { null }, { "token" }),
        )
        val result = repo.previewImport(pngSignature + ByteArray(8), "aria.png")
        assertTrue(result.isFailure)
        assertEquals("missing_base_url", result.exceptionOrNull()?.message)
    }

    @Test
    fun `previewImport surfaces missing token as failure`() = runBlocking {
        val backend = FakeCardBackendClient()
        val repo = DefaultCardInteropRepository(
            delegate = LiveCardInteropRepository(backend, { "http://example/" }, { null }),
        )
        val result = repo.previewImport(pngSignature + ByteArray(8), "aria.png")
        assertTrue(result.isFailure)
        assertEquals("missing_token", result.exceptionOrNull()?.message)
    }

    @Test
    fun `commitImport surfaces the persisted card`() = runBlocking {
        val backend = FakeCardBackendClient(
            commitResponse = sampleCardDto("card-persisted-1"),
        )
        val repo = DefaultCardInteropRepository(
            delegate = LiveCardInteropRepository(backend, { "http://example/" }, { "token" }),
        )
        val preview = CardImportPreview(
            previewToken = "preview-x",
            card = sampleCardDto("card-import-1").toCompanionCharacterCard(),
            rawCardDto = sampleCardDto("card-import-1"),
            detectedLanguage = "en",
            warnings = emptyList(),
            stExtensionKeys = emptyList(),
        )
        val result = repo.commitImport(preview, overrides = null, languageOverride = "zh")
        assertTrue(result.isSuccess)
        assertEquals("card-persisted-1", result.getOrThrow().id)
        assertEquals(1, backend.importCommitCalls)
        assertEquals("zh", backend.lastLanguageOverride)
    }

    @Test
    fun `exportCard returns binary payload for PNG`() = runBlocking {
        val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        val encoded = java.util.Base64.getEncoder().encodeToString(pngBytes)
        val backend = FakeCardBackendClient(
            exportResponse = CardExportResponseDto(
                format = "png",
                filename = "aria.png",
                contentType = "image/png",
                encoding = "base64",
                payload = encoded,
            ),
        )
        val repo = DefaultCardInteropRepository(
            delegate = LiveCardInteropRepository(backend, { "http://example/" }, { "token" }),
        )
        val result = repo.exportCard("card-1", ExportedCardFormat.Png, language = "zh", includeTranslationAlt = true)
        assertTrue(result.isSuccess)
        val payload = result.getOrThrow()
        assertEquals(ExportedCardFormat.Png, payload.format)
        assertEquals("aria.png", payload.filename)
        assertEquals("image/png", payload.contentType)
        assertTrue(payload.bytes.contentEquals(pngBytes))
        assertEquals("zh", backend.lastExportLanguage)
        assertEquals(true, backend.lastExportIncludeTranslationAlt)
    }

    @Test
    fun `exportCard returns text payload for JSON`() = runBlocking {
        val jsonText = "{\"name\":\"Aria\"}"
        val backend = FakeCardBackendClient(
            exportResponse = CardExportResponseDto(
                format = "json",
                filename = "aria.json",
                contentType = "application/json",
                encoding = "utf8",
                payload = jsonText,
            ),
        )
        val repo = DefaultCardInteropRepository(
            delegate = LiveCardInteropRepository(backend, { "http://example/" }, { "token" }),
        )
        val result = repo.exportCard("card-1", ExportedCardFormat.Json, language = "en")
        assertTrue(result.isSuccess)
        val payload = result.getOrThrow()
        assertEquals(ExportedCardFormat.Json, payload.format)
        assertEquals(jsonText, payload.bytes.toString(Charsets.UTF_8))
        assertEquals(false, backend.lastExportIncludeTranslationAlt)
    }

    @Test
    fun `exportCard propagates backend failure`() = runBlocking {
        val backend = FakeCardBackendClient(exportFailure = IllegalStateException("404"))
        val repo = DefaultCardInteropRepository(
            delegate = LiveCardInteropRepository(backend, { "http://example/" }, { "token" }),
        )
        val result = repo.exportCard("missing", ExportedCardFormat.Png, language = "en")
        assertTrue(result.isFailure)
        assertEquals("404", result.exceptionOrNull()?.message)
    }

    private fun sampleCardDto(id: String): CompanionCharacterCardDto = CompanionCharacterCardDto(
        id = id,
        displayName = LocalizedTextDto("Aria", "Aria"),
        roleLabel = LocalizedTextDto("Guide", "向导"),
        summary = LocalizedTextDto("Calm guide", "Calm guide"),
        firstMes = LocalizedTextDto("Hello traveller.", "你好，旅人。"),
        avatarText = "AR",
        accent = "primary",
        source = "userauthored",
    )

    private class FakeCardBackendClient(
        var previewResponse: CardImportPreviewDto? = null,
        var commitResponse: CompanionCharacterCardDto? = null,
        var exportResponse: CardExportResponseDto? = null,
        var exportFailure: Throwable? = null,
    ) : ImBackendClient {
        var importPreviewCalls: Int = 0
        var importCommitCalls: Int = 0
        var lastLanguageOverride: String? = null
        var lastExportLanguage: String? = null
        var lastExportIncludeTranslationAlt: Boolean? = null

        override suspend fun issueDevSession(baseUrl: String, externalId: String): DevSessionResponseDto = error("n/a")
        override suspend fun register(baseUrl: String, username: String, password: String, displayName: String): AuthResponseDto = error("n/a")
        override suspend fun login(baseUrl: String, username: String, password: String): AuthResponseDto = error("n/a")
        override suspend fun searchUsers(baseUrl: String, token: String, query: String): List<UserSearchResultDto> = error("n/a")
        override suspend fun sendFriendRequest(baseUrl: String, token: String, toUserId: String): FriendRequestViewDto = error("n/a")
        override suspend fun listFriendRequests(baseUrl: String, token: String): List<FriendRequestViewDto> = error("n/a")
        override suspend fun acceptFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto = error("n/a")
        override suspend fun rejectFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto = error("n/a")
        override suspend fun loadBootstrap(baseUrl: String, token: String): BootstrapBundleDto = error("n/a")
        override suspend fun sendDirectImageMessage(
            baseUrl: String,
            token: String,
            request: SendDirectImageMessageRequestDto,
        ): SendDirectMessageResultDto = error("n/a")

        override suspend fun loadHistory(
            baseUrl: String,
            token: String,
            conversationId: String,
            limit: Int,
            before: String?,
        ): MessageHistoryPageDto = error("n/a")

        override suspend fun loadCompanionRoster(baseUrl: String, token: String): CompanionRosterDto = error("n/a")
        override suspend fun drawCompanionCharacter(baseUrl: String, token: String): CompanionDrawResultDto = error("n/a")
        override suspend fun selectCompanionCharacter(
            baseUrl: String,
            token: String,
            characterId: String,
        ): ActiveCompanionSelectionDto = error("n/a")

        override suspend fun importCardPreview(
            baseUrl: String,
            token: String,
            bytes: ByteArray,
            filename: String,
        ): CardImportPreviewDto {
            importPreviewCalls += 1
            return previewResponse ?: error("no preview response configured")
        }

        override suspend fun importCardCommit(
            baseUrl: String,
            token: String,
            preview: CardImportPreviewDto,
            overrides: CompanionCharacterCardDto?,
            languageOverride: String?,
        ): CompanionCharacterCardDto {
            importCommitCalls += 1
            lastLanguageOverride = languageOverride
            return commitResponse ?: error("no commit response configured")
        }

        override suspend fun exportCard(
            baseUrl: String,
            token: String,
            cardId: String,
            format: String,
            language: String,
            includeTranslationAlt: Boolean,
        ): CardExportResponseDto {
            lastExportLanguage = language
            lastExportIncludeTranslationAlt = includeTranslationAlt
            exportFailure?.let { throw it }
            return exportResponse ?: error("no export response configured")
        }
    }
}
