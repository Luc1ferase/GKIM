package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.data.repository.CardImportPreview
import com.gkim.im.android.data.repository.CardInteropRepository
import com.gkim.im.android.data.repository.ExportedCardFormat
import com.gkim.im.android.data.repository.ExportedCardPayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardExportInvocationTest {
    @Test
    fun `invokeCardExport dispatches PNG share payload on success`() = runBlocking {
        val payload = ExportedCardPayload(
            format = ExportedCardFormat.Png,
            filename = "aria.png",
            contentType = "image/png",
            bytes = byteArrayOf(1, 2, 3, 4),
        )
        val repository = FakeExportRepository(payload)
        val dispatched = mutableListOf<Pair<ExportedCardPayload, CardExportTarget>>()
        val dispatcher = CardExportDispatcher { p, t ->
            dispatched += p to t
            Result.success(Unit)
        }
        val state = initialExportDialogState(ExportedCardFormat.Png, AppLanguage.English)

        val outcome = invokeCardExport("card-1", state, repository, dispatcher)

        assertTrue(outcome is CardExportInvocationOutcome.Success)
        assertEquals(CardExportTarget.Share, (outcome as CardExportInvocationOutcome.Success).target)
        assertTrue(outcome.payload.bytes.contentEquals(payload.bytes))
        assertEquals(1, dispatched.size)
        assertEquals("en", repository.lastLanguage)
        assertEquals(false, repository.lastIncludeTranslationAlt)
    }

    @Test
    fun `invokeCardExport dispatches JSON downloads payload on success`() = runBlocking {
        val payload = ExportedCardPayload(
            format = ExportedCardFormat.Json,
            filename = "aria.json",
            contentType = "application/json",
            bytes = "{\"name\":\"Aria\"}".toByteArray(Charsets.UTF_8),
        )
        val repository = FakeExportRepository(payload)
        val dispatcher = CardExportDispatcher { _, _ -> Result.success(Unit) }
        val state = initialExportDialogState(ExportedCardFormat.Json, AppLanguage.Chinese)
            .withTarget(CardExportTarget.Downloads)
            .withIncludeTranslationAlt(true)

        val outcome = invokeCardExport("card-1", state, repository, dispatcher)

        assertTrue(outcome is CardExportInvocationOutcome.Success)
        assertEquals(CardExportTarget.Downloads, (outcome as CardExportInvocationOutcome.Success).target)
        assertEquals("zh", repository.lastLanguage)
        assertEquals(true, repository.lastIncludeTranslationAlt)
    }

    @Test
    fun `invokeCardExport routes repository failure as Failed outcome with code`() = runBlocking {
        val repository = FakeExportRepository(failure = IllegalStateException("404_unknown_card"))
        val dispatcher = CardExportDispatcher { _, _ -> Result.success(Unit) }
        val state = initialExportDialogState(ExportedCardFormat.Png, AppLanguage.English)

        val outcome = invokeCardExport("missing", state, repository, dispatcher)
        assertEquals(CardExportInvocationOutcome.Failed("404_unknown_card"), outcome)
    }

    @Test
    fun `invokeCardExport routes dispatcher failure as Failed outcome`() = runBlocking {
        val payload = ExportedCardPayload(
            format = ExportedCardFormat.Png,
            filename = "aria.png",
            contentType = "image/png",
            bytes = byteArrayOf(1, 2),
        )
        val repository = FakeExportRepository(payload)
        val dispatcher = CardExportDispatcher { _, _ -> Result.failure(IllegalStateException("share_cancelled")) }
        val state = initialExportDialogState(ExportedCardFormat.Png, AppLanguage.English)

        val outcome = invokeCardExport("card-1", state, repository, dispatcher)
        assertEquals(CardExportInvocationOutcome.Failed("share_cancelled"), outcome)
    }

    private class FakeExportRepository(
        private val payload: ExportedCardPayload? = null,
        private val failure: Throwable? = null,
    ) : CardInteropRepository {
        var lastLanguage: String? = null
        var lastIncludeTranslationAlt: Boolean? = null

        override suspend fun previewImport(bytes: ByteArray, filename: String): Result<CardImportPreview> =
            Result.failure(IllegalStateException("not used"))

        override suspend fun commitImport(
            preview: CardImportPreview,
            overrides: CompanionCharacterCard?,
            languageOverride: String?,
        ): Result<CompanionCharacterCard> = Result.failure(IllegalStateException("not used"))

        override suspend fun exportCard(
            cardId: String,
            format: ExportedCardFormat,
            language: String,
            includeTranslationAlt: Boolean,
        ): Result<ExportedCardPayload> {
            lastLanguage = language
            lastIncludeTranslationAlt = includeTranslationAlt
            failure?.let { return Result.failure(it) }
            return payload?.let { Result.success(it) } ?: Result.failure(IllegalStateException("no payload"))
        }
    }
}
