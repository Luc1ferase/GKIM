package com.gkim.im.android.feature.tavern

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.data.repository.CardImportPreview
import com.gkim.im.android.data.repository.CardInteropRepository
import com.gkim.im.android.data.repository.ExportedCardFormat
import com.gkim.im.android.data.repository.ExportedCardPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardExportInstrumentationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cardExportDialogRendersAllSurfaceElementsForPng() {
        val repository = RecordingExportRepository(
            payload = ExportedCardPayload(
                format = ExportedCardFormat.Png,
                filename = "aria.png",
                contentType = "image/png",
                bytes = byteArrayOf(1, 2, 3),
            ),
        )
        val dispatcher = CardExportDispatcher { _, _ -> Result.success(Unit) }

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                CardExportDialog(
                    cardId = "card-1",
                    initialFormat = ExportedCardFormat.Png,
                    repository = repository,
                    dispatcher = dispatcher,
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithTag("card-export-dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("card-export-dialog-title").assertIsDisplayed()
        composeRule.onNodeWithTag("card-export-dialog-language-en").assertIsDisplayed()
        composeRule.onNodeWithTag("card-export-dialog-language-zh").assertIsDisplayed()
        composeRule.onNodeWithTag("card-export-dialog-translation-alt").assertIsDisplayed()
        composeRule.onNodeWithTag("card-export-dialog-target-share").assertIsDisplayed()
        composeRule.onNodeWithTag("card-export-dialog-target-downloads").assertIsDisplayed()
        composeRule.onNodeWithTag("card-export-dialog-cancel").assertIsDisplayed()
        composeRule.onNodeWithTag("card-export-dialog-submit").assertIsDisplayed()
    }

    @Test
    fun pngShareSubmitRoutesPayloadAndDismissesDialog() {
        val repository = RecordingExportRepository(
            payload = ExportedCardPayload(
                format = ExportedCardFormat.Png,
                filename = "aria.png",
                contentType = "image/png",
                bytes = byteArrayOf(9, 8, 7),
            ),
        )
        val dispatched = mutableListOf<Pair<ExportedCardPayload, CardExportTarget>>()
        val dispatcher = CardExportDispatcher { p, t ->
            dispatched += p to t
            Result.success(Unit)
        }
        var dismissed = false

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                CardExportDialog(
                    cardId = "card-1",
                    initialFormat = ExportedCardFormat.Png,
                    repository = repository,
                    dispatcher = dispatcher,
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithTag("card-export-dialog-submit").performClick()
        composeRule.waitUntil(5_000) { dismissed }

        assertTrue(dismissed)
        assertEquals(1, repository.invocations.size)
        val call = repository.invocations.first()
        assertEquals("card-1", call.cardId)
        assertEquals(ExportedCardFormat.Png, call.format)
        assertEquals("en", call.language)
        assertEquals(false, call.includeTranslationAlt)
        assertEquals(1, dispatched.size)
        assertEquals(CardExportTarget.Share, dispatched.first().second)
    }

    @Test
    fun jsonDownloadsPathWithTranslationAltTogglePropagatesToRepository() {
        val repository = RecordingExportRepository(
            payload = ExportedCardPayload(
                format = ExportedCardFormat.Json,
                filename = "aria.json",
                contentType = "application/json",
                bytes = "{\"name\":\"Aria\"}".toByteArray(Charsets.UTF_8),
            ),
        )
        val dispatched = mutableListOf<Pair<ExportedCardPayload, CardExportTarget>>()
        val dispatcher = CardExportDispatcher { p, t ->
            dispatched += p to t
            Result.success(Unit)
        }
        var dismissed = false

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.Chinese) {
                CardExportDialog(
                    cardId = "card-2",
                    initialFormat = ExportedCardFormat.Json,
                    repository = repository,
                    dispatcher = dispatcher,
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithTag("card-export-dialog-translation-alt").performClick()
        composeRule.onNodeWithTag("card-export-dialog-target-downloads").performClick()
        composeRule.onNodeWithTag("card-export-dialog-submit").performClick()
        composeRule.waitUntil(5_000) { dismissed }

        val call = repository.invocations.single()
        assertEquals("card-2", call.cardId)
        assertEquals(ExportedCardFormat.Json, call.format)
        assertEquals("zh", call.language)
        assertEquals(true, call.includeTranslationAlt)
        assertEquals(CardExportTarget.Downloads, dispatched.single().second)
    }

    @Test
    fun repositoryFailureRendersErrorSlotAndKeepsDialogOpen() {
        val repository = RecordingExportRepository(
            failure = IllegalStateException("404_unknown_card"),
        )
        val dispatcher = CardExportDispatcher { _, _ -> Result.success(Unit) }
        var dismissed = false

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                CardExportDialog(
                    cardId = "missing",
                    initialFormat = ExportedCardFormat.Png,
                    repository = repository,
                    dispatcher = dispatcher,
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithTag("card-export-dialog-submit").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("card-export-dialog-error")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("card-export-dialog-error").assertIsDisplayed()
        composeRule.onNodeWithTag("card-export-dialog").assertIsDisplayed()
        assertFalse(dismissed)
    }

    private class RecordingExportRepository(
        private val payload: ExportedCardPayload? = null,
        private val failure: Throwable? = null,
    ) : CardInteropRepository {
        data class Invocation(
            val cardId: String,
            val format: ExportedCardFormat,
            val language: String,
            val includeTranslationAlt: Boolean,
        )

        val invocations = mutableListOf<Invocation>()

        override suspend fun previewImport(
            bytes: ByteArray,
            filename: String,
        ): Result<CardImportPreview> = Result.failure(IllegalStateException("not used"))

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
            invocations += Invocation(cardId, format, language, includeTranslationAlt)
            failure?.let { return Result.failure(it) }
            return payload?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("no payload"))
        }
    }
}
