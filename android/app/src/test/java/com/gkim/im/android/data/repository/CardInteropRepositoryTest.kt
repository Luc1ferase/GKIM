package com.gkim.im.android.data.repository

import com.gkim.im.android.core.interop.SillyTavernCardCodec
import com.gkim.im.android.data.remote.im.ActiveCompanionSelectionDto
import com.gkim.im.android.data.remote.im.AuthResponseDto
import com.gkim.im.android.data.remote.im.BootstrapBundleDto
import com.gkim.im.android.data.remote.im.CardExportResponseDto
import com.gkim.im.android.data.remote.im.CardImportPreviewDto
import com.gkim.im.android.data.remote.im.CardImportWarningDto
import com.gkim.im.android.data.remote.im.CharacterBookDto
import com.gkim.im.android.data.remote.im.CharacterBookEntryDto
import com.gkim.im.android.data.remote.im.CompanionCharacterCardDto
import com.gkim.im.android.data.remote.im.CompanionDrawResultDto
import com.gkim.im.android.data.remote.im.CompanionRosterDto
import com.gkim.im.android.data.remote.im.CreateLorebookBindingRequestDto
import com.gkim.im.android.data.remote.im.CreateLorebookEntryRequestDto
import com.gkim.im.android.data.remote.im.CreateLorebookRequestDto
import com.gkim.im.android.data.remote.im.DevSessionResponseDto
import com.gkim.im.android.data.remote.im.FriendRequestViewDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.ImWorldInfoClient
import com.gkim.im.android.data.remote.im.LocalizedTextDto
import com.gkim.im.android.data.remote.im.LorebookBindingDto
import com.gkim.im.android.data.remote.im.LorebookBindingListDto
import com.gkim.im.android.data.remote.im.LorebookDto
import com.gkim.im.android.data.remote.im.LorebookEntryDto
import com.gkim.im.android.data.remote.im.LorebookEntryListDto
import com.gkim.im.android.data.remote.im.LorebookListDto
import com.gkim.im.android.data.remote.im.MessageHistoryPageDto
import com.gkim.im.android.data.remote.im.SendDirectImageMessageRequestDto
import com.gkim.im.android.data.remote.im.SendDirectMessageResultDto
import com.gkim.im.android.data.remote.im.UpdateLorebookBindingRequestDto
import com.gkim.im.android.data.remote.im.UpdateLorebookEntryRequestDto
import com.gkim.im.android.data.remote.im.UpdateLorebookRequestDto
import com.gkim.im.android.data.remote.im.UserSearchResultDto
import com.gkim.im.android.data.remote.im.WorldInfoDebugScanResponseDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun `commitImport materializes character_book into lorebook + entries + primary binding`() = runBlocking {
        val book = CharacterBookDto(
            name = "Tome",
            entries = listOf(
                CharacterBookEntryDto(keys = listOf("dragon"), content = "d"),
                CharacterBookEntryDto(keys = emptyList(), content = "c", constant = true),
            ),
        )
        val committed = sampleCardDto("card-with-book").copy(characterBook = book)
        val backend = FakeCardBackendClient(commitResponse = committed)
        val worldInfo = RecordingWorldInfoBackend()
        val repo = DefaultCardInteropRepository(
            delegate = LiveCardInteropRepository(
                backendClient = backend,
                baseUrlProvider = { "http://example/" },
                tokenProvider = { "token" },
                characterBookMaterializer = CharacterBookLorebookMaterializer(worldInfo),
            ),
        )
        val preview = CardImportPreview(
            previewToken = "preview-x",
            card = sampleCardDto("card-with-book").toCompanionCharacterCard(),
            rawCardDto = sampleCardDto("card-with-book").copy(characterBook = book),
            detectedLanguage = "en",
            warnings = emptyList(),
            stExtensionKeys = emptyList(),
        )

        val result = repo.commitImport(preview, overrides = null, languageOverride = null)

        assertTrue(result.isSuccess)
        assertEquals("card-with-book", result.getOrThrow().id)
        assertEquals("card-with-book", worldInfo.lastBindingCharacterId)
        assertEquals(worldInfo.createdLorebookId, worldInfo.lastBindingLorebookId)
        assertTrue("binding must be primary", worldInfo.recordedBindings.single().isPrimary)
        assertEquals(2, worldInfo.entryRequests.size)
    }

    @Test
    fun `commitImport skips materializer when committed card has no character_book`() = runBlocking {
        val backend = FakeCardBackendClient(commitResponse = sampleCardDto("plain"))
        val worldInfo = RecordingWorldInfoBackend()
        val repo = DefaultCardInteropRepository(
            delegate = LiveCardInteropRepository(
                backendClient = backend,
                baseUrlProvider = { "http://example/" },
                tokenProvider = { "token" },
                characterBookMaterializer = CharacterBookLorebookMaterializer(worldInfo),
            ),
        )
        val preview = CardImportPreview(
            previewToken = "preview-x",
            card = sampleCardDto("plain").toCompanionCharacterCard(),
            rawCardDto = sampleCardDto("plain"),
            detectedLanguage = "en",
            warnings = emptyList(),
            stExtensionKeys = emptyList(),
        )

        val result = repo.commitImport(preview, overrides = null, languageOverride = null)

        assertTrue(result.isSuccess)
        assertEquals(0, worldInfo.createEntryCalls)
        assertTrue(worldInfo.recordedBindings.isEmpty())
        assertNull(worldInfo.lastBindingLorebookId)
    }

    @Test
    fun `commitImport routes Chinese language override to materializer language slot`() = runBlocking {
        val book = CharacterBookDto(
            name = "书",
            entries = listOf(CharacterBookEntryDto(keys = listOf("龙"), content = "c")),
        )
        val committed = sampleCardDto("card-zh").copy(characterBook = book)
        val backend = FakeCardBackendClient(commitResponse = committed)
        val worldInfo = RecordingWorldInfoBackend()
        val repo = DefaultCardInteropRepository(
            delegate = LiveCardInteropRepository(
                backendClient = backend,
                baseUrlProvider = { "http://example/" },
                tokenProvider = { "token" },
                characterBookMaterializer = CharacterBookLorebookMaterializer(worldInfo),
            ),
        )
        val preview = CardImportPreview(
            previewToken = "preview-x",
            card = sampleCardDto("card-zh").toCompanionCharacterCard(),
            rawCardDto = sampleCardDto("card-zh").copy(characterBook = book),
            detectedLanguage = "en",
            warnings = emptyList(),
            stExtensionKeys = emptyList(),
        )

        val result = repo.commitImport(preview, overrides = null, languageOverride = "zh")

        assertTrue(result.isSuccess)
        val entryRequest = worldInfo.entryRequests.single()
        assertEquals(listOf("龙"), entryRequest.keysByLang.chinese)
        assertEquals(emptyList<String>(), entryRequest.keysByLang.english)
    }

    @Test
    fun `commitImport fails when materialization fails and rollback executes`() = runBlocking {
        val book = CharacterBookDto(
            name = "Breaks",
            entries = listOf(CharacterBookEntryDto(keys = listOf("k"), content = "c")),
        )
        val committed = sampleCardDto("card-break").copy(characterBook = book)
        val backend = FakeCardBackendClient(commitResponse = committed)
        val worldInfo = RecordingWorldInfoBackend().apply { failOnCreateEntry = true }
        val repo = DefaultCardInteropRepository(
            delegate = LiveCardInteropRepository(
                backendClient = backend,
                baseUrlProvider = { "http://example/" },
                tokenProvider = { "token" },
                characterBookMaterializer = CharacterBookLorebookMaterializer(worldInfo),
            ),
        )
        val preview = CardImportPreview(
            previewToken = "preview-x",
            card = sampleCardDto("card-break").toCompanionCharacterCard(),
            rawCardDto = sampleCardDto("card-break").copy(characterBook = book),
            detectedLanguage = "en",
            warnings = emptyList(),
            stExtensionKeys = emptyList(),
        )

        val result = repo.commitImport(preview, overrides = null, languageOverride = null)

        assertTrue("materialization failure must surface as Result.failure", result.isFailure)
        assertEquals(worldInfo.createdLorebookId, worldInfo.deletedLorebookId)
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

    private class RecordingWorldInfoBackend : ImWorldInfoClient {
        var createdLorebookId: String = "lb-created"
        var deletedLorebookId: String? = null
        val entryRequests: MutableList<CreateLorebookEntryRequestDto> = mutableListOf()
        val recordedBindings: MutableList<CreateLorebookBindingRequestDto> = mutableListOf()
        var createEntryCalls: Int = 0
        var failOnCreateEntry: Boolean = false
        var lastBindingLorebookId: String? = null
        var lastBindingCharacterId: String? = null
        private var nextEntryId: Int = 0

        override suspend fun list(baseUrl: String, token: String): LorebookListDto = LorebookListDto(lorebooks = emptyList())
        override suspend fun get(baseUrl: String, token: String, lorebookId: String): LorebookDto =
            LorebookDto(id = lorebookId, ownerId = "u", displayName = LocalizedTextDto("", ""))

        override suspend fun create(
            baseUrl: String,
            token: String,
            request: CreateLorebookRequestDto,
        ): LorebookDto = LorebookDto(
            id = createdLorebookId,
            ownerId = "u",
            displayName = request.displayName,
        )

        override suspend fun update(
            baseUrl: String,
            token: String,
            lorebookId: String,
            request: UpdateLorebookRequestDto,
        ): LorebookDto = error("not used")

        override suspend fun delete(baseUrl: String, token: String, lorebookId: String) {
            deletedLorebookId = lorebookId
        }

        override suspend fun duplicate(baseUrl: String, token: String, lorebookId: String): LorebookDto = error("not used")
        override suspend fun listEntries(baseUrl: String, token: String, lorebookId: String): LorebookEntryListDto =
            LorebookEntryListDto(emptyList())

        override suspend fun createEntry(
            baseUrl: String,
            token: String,
            lorebookId: String,
            request: CreateLorebookEntryRequestDto,
        ): LorebookEntryDto {
            createEntryCalls += 1
            if (failOnCreateEntry) error("create entry failed")
            entryRequests += request
            val id = "entry-${nextEntryId++}"
            return LorebookEntryDto(id = id, lorebookId = lorebookId, name = request.name)
        }

        override suspend fun updateEntry(
            baseUrl: String,
            token: String,
            lorebookId: String,
            entryId: String,
            request: UpdateLorebookEntryRequestDto,
        ): LorebookEntryDto = error("not used")

        override suspend fun deleteEntry(baseUrl: String, token: String, lorebookId: String, entryId: String) = Unit

        override suspend fun listBindings(baseUrl: String, token: String, lorebookId: String): LorebookBindingListDto =
            LorebookBindingListDto(emptyList())

        override suspend fun bind(
            baseUrl: String,
            token: String,
            lorebookId: String,
            characterId: String,
            isPrimary: Boolean,
        ): LorebookBindingDto {
            recordedBindings += CreateLorebookBindingRequestDto(characterId = characterId, isPrimary = isPrimary)
            lastBindingLorebookId = lorebookId
            lastBindingCharacterId = characterId
            return LorebookBindingDto(lorebookId = lorebookId, characterId = characterId, isPrimary = isPrimary)
        }

        override suspend fun updateBinding(
            baseUrl: String,
            token: String,
            lorebookId: String,
            characterId: String,
            isPrimary: Boolean,
        ): LorebookBindingDto = error("not used")

        override suspend fun unbind(baseUrl: String, token: String, lorebookId: String, characterId: String) = Unit

        override suspend fun debugScan(
            baseUrl: String,
            token: String,
            characterId: String,
            scanText: String,
            devAccessHeader: String,
            allowDebug: Boolean,
        ): WorldInfoDebugScanResponseDto = WorldInfoDebugScanResponseDto()
    }
}
