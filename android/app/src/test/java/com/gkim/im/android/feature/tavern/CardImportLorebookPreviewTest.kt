package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.data.remote.im.CompanionCharacterCardDto
import com.gkim.im.android.data.remote.im.LocalizedTextDto
import com.gkim.im.android.data.remote.im.LorebookImportSummaryDto
import com.gkim.im.android.data.remote.im.CardImportPreviewDto
import com.gkim.im.android.data.repository.CardImportPreview
import com.gkim.im.android.data.repository.CardInteropRepository
import com.gkim.im.android.data.repository.ExportedCardFormat
import com.gkim.im.android.data.repository.ExportedCardPayload
import com.gkim.im.android.data.repository.LorebookImportSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CardImportLorebookPreviewTest {

    @Test
    fun `lorebookSummary is null by default so non-lorebook cards skip the summary block`() {
        val preview = samplePreview(lorebookSummary = null)
        assertNull(preview.lorebookSummary)
    }

    @Test
    fun `lorebookSummary carries entry count token estimate and constant flag`() {
        val summary = LorebookImportSummary(
            entryCount = 5,
            totalTokenEstimate = 1400,
            hasConstantEntries = true,
        )
        val preview = samplePreview(lorebookSummary = summary)
        assertEquals(5, preview.lorebookSummary?.entryCount)
        assertEquals(1400, preview.lorebookSummary?.totalTokenEstimate)
        assertEquals(true, preview.lorebookSummary?.hasConstantEntries)
    }

    @Test
    fun `loaded state exposes lorebookSummary through the ViewModel`() = runTest(UnconfinedTestDispatcher()) {
        val preview = samplePreview(
            lorebookSummary = LorebookImportSummary(
                entryCount = 3,
                totalTokenEstimate = 820,
                hasConstantEntries = false,
            ),
        )
        val repo = FakeInteropRepository(previewResult = Result.success(preview))
        val vm = ImportCardPreviewViewModel(repo, scopeOverride = backgroundScope)
        vm.submit("{}".toByteArray(), "x.json")
        advanceUntilIdle()

        val loaded = vm.state.value as ImportCardPreviewUiState.Loaded
        val summary = loaded.preview.lorebookSummary
        assertNotNull(summary)
        assertEquals(3, summary!!.entryCount)
        assertEquals(820, summary.totalTokenEstimate)
        assertEquals(false, summary.hasConstantEntries)
    }

    @Test
    fun `entryCountCopy uses singular entry in English when count is 1`() {
        val summary = LorebookImportSummary(entryCount = 1, totalTokenEstimate = 50, hasConstantEntries = false)
        assertEquals("1 entry", lorebookSummaryEntryCountCopy(summary, english = true))
    }

    @Test
    fun `entryCountCopy pluralises entries in English when count is greater than 1`() {
        val summary = LorebookImportSummary(entryCount = 4, totalTokenEstimate = 100, hasConstantEntries = false)
        assertEquals("4 entries", lorebookSummaryEntryCountCopy(summary, english = true))
    }

    @Test
    fun `entryCountCopy renders bilingual zh string`() {
        val summary = LorebookImportSummary(entryCount = 7, totalTokenEstimate = 0, hasConstantEntries = false)
        assertEquals("7 条条目", lorebookSummaryEntryCountCopy(summary, english = false))
    }

    @Test
    fun `entryCountCopy handles zero entries`() {
        val summary = LorebookImportSummary(entryCount = 0, totalTokenEstimate = 0, hasConstantEntries = false)
        assertEquals("0 entries", lorebookSummaryEntryCountCopy(summary, english = true))
        assertEquals("0 条条目", lorebookSummaryEntryCountCopy(summary, english = false))
    }

    @Test
    fun `LorebookImportSummaryDto round-trips over json with defaults for optional fields`() {
        val json = Json {
            ignoreUnknownKeys = true
        }
        val minimal = json.decodeFromString(LorebookImportSummaryDto.serializer(), "{\"entryCount\":2}")
        assertEquals(2, minimal.entryCount)
        assertEquals(0, minimal.totalTokenEstimate)
        assertEquals(false, minimal.hasConstantEntries)

        val full = LorebookImportSummaryDto(entryCount = 9, totalTokenEstimate = 2_500, hasConstantEntries = true)
        val serialized = json.encodeToString(LorebookImportSummaryDto.serializer(), full)
        val back = json.decodeFromString(LorebookImportSummaryDto.serializer(), serialized)
        assertEquals(full, back)
    }

    @Test
    fun `CardImportPreviewDto decodes a payload that omits lorebookSummary as null`() {
        val json = Json { ignoreUnknownKeys = true }
        val payload = """
            {
              "previewToken": "tok-1",
              "card": {
                "id": "card-xyz",
                "displayName": {"english":"Aria","chinese":"Aria"},
                "roleLabel": {"english":"Guide","chinese":"向导"},
                "summary": {"english":"","chinese":""},
                "avatarText": "AR",
                "accent": "primary",
                "source": "userauthored"
              },
              "detectedLanguage": "en"
            }
        """.trimIndent()
        val dto = json.decodeFromString(CardImportPreviewDto.serializer(), payload)
        assertNull(dto.lorebookSummary)
    }

    @Test
    fun `CardImportPreviewDto decodes lorebookSummary when present`() {
        val json = Json { ignoreUnknownKeys = true }
        val payload = """
            {
              "previewToken": "tok-2",
              "card": {
                "id": "card-xyz",
                "displayName": {"english":"Aria","chinese":"Aria"},
                "roleLabel": {"english":"Guide","chinese":"向导"},
                "summary": {"english":"","chinese":""},
                "avatarText": "AR",
                "accent": "primary",
                "source": "userauthored"
              },
              "detectedLanguage": "zh",
              "lorebookSummary": {
                "entryCount": 6,
                "totalTokenEstimate": 2100,
                "hasConstantEntries": true
              }
            }
        """.trimIndent()
        val dto = json.decodeFromString(CardImportPreviewDto.serializer(), payload)
        assertEquals(6, dto.lorebookSummary?.entryCount)
        assertEquals(2100, dto.lorebookSummary?.totalTokenEstimate)
        assertEquals(true, dto.lorebookSummary?.hasConstantEntries)
    }

    @Test
    fun `non-lorebook preview omits summary at the ViewModel layer`() = runTest(UnconfinedTestDispatcher()) {
        val preview = samplePreview(lorebookSummary = null)
        val repo = FakeInteropRepository(previewResult = Result.success(preview))
        val vm = ImportCardPreviewViewModel(repo, scopeOverride = backgroundScope)
        vm.submit("{}".toByteArray(), "x.json")
        advanceUntilIdle()

        val loaded = vm.state.value as ImportCardPreviewUiState.Loaded
        assertNull(loaded.preview.lorebookSummary)
        assertTrue(loaded.preview.warnings.isEmpty())
    }

    private fun samplePreview(
        lorebookSummary: LorebookImportSummary?,
    ): CardImportPreview {
        val dto = sampleCardDto("card-import-1")
        return CardImportPreview(
            previewToken = "preview-x",
            card = dto.toCompanionCharacterCard(),
            rawCardDto = dto,
            detectedLanguage = "en",
            warnings = emptyList(),
            stExtensionKeys = emptyList(),
            lorebookSummary = lorebookSummary,
        )
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

    private class FakeInteropRepository(
        private val previewResult: Result<CardImportPreview>,
    ) : CardInteropRepository {
        override suspend fun previewImport(bytes: ByteArray, filename: String): Result<CardImportPreview> =
            previewResult

        override suspend fun commitImport(
            preview: CardImportPreview,
            overrides: CompanionCharacterCard?,
            languageOverride: String?,
        ): Result<CompanionCharacterCard> = Result.failure(IllegalStateException("not configured"))

        override suspend fun exportCard(
            cardId: String,
            format: ExportedCardFormat,
            language: String,
            includeTranslationAlt: Boolean,
        ): Result<ExportedCardPayload> = Result.failure(IllegalStateException("not configured"))
    }
}
