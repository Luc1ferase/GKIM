package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.data.repository.ExportedCardFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardDetailExportDialogTest {
    @Test
    fun `initialExportDialogState defaults language to active AppLanguage`() {
        val englishState = initialExportDialogState(ExportedCardFormat.Png, AppLanguage.English)
        assertEquals("en", englishState.language)

        val chineseState = initialExportDialogState(ExportedCardFormat.Json, AppLanguage.Chinese)
        assertEquals("zh", chineseState.language)
    }

    @Test
    fun `initialExportDialogState defaults includeTranslationAlt to false and target to Share`() {
        val state = initialExportDialogState(ExportedCardFormat.Png, AppLanguage.English)
        assertFalse(state.includeTranslationAlt)
        assertEquals(CardExportTarget.Share, state.target)
        assertFalse(state.inFlight)
        assertFalse(state.completed)
        assertNull(state.errorCode)
    }

    @Test
    fun `withLanguage overrides language but keeps other defaults`() {
        val state = initialExportDialogState(ExportedCardFormat.Png, AppLanguage.English)
            .withLanguage("zh")
        assertEquals("zh", state.language)
        assertEquals(ExportedCardFormat.Png, state.format)
        assertFalse(state.includeTranslationAlt)
    }

    @Test
    fun `withIncludeTranslationAlt toggles flag without touching other fields`() {
        val state = initialExportDialogState(ExportedCardFormat.Json, AppLanguage.Chinese)
            .withIncludeTranslationAlt(true)
        assertTrue(state.includeTranslationAlt)
        assertEquals("zh", state.language)
    }

    @Test
    fun `withTarget switches share vs downloads without other changes`() {
        val state = initialExportDialogState(ExportedCardFormat.Png, AppLanguage.English)
            .withTarget(CardExportTarget.Downloads)
        assertEquals(CardExportTarget.Downloads, state.target)
        assertFalse(state.includeTranslationAlt)
    }

    @Test
    fun `markInFlight clears prior error and completed flags`() {
        val prior = initialExportDialogState(ExportedCardFormat.Png, AppLanguage.English)
            .markFailed("server_busy")
        val running = prior.markInFlight()
        assertTrue(running.inFlight)
        assertFalse(running.completed)
        assertNull(running.errorCode)
    }

    @Test
    fun `markCompleted clears inFlight and flips completed on`() {
        val running = initialExportDialogState(ExportedCardFormat.Png, AppLanguage.English)
            .markInFlight()
        val done = running.markCompleted()
        assertFalse(done.inFlight)
        assertTrue(done.completed)
    }

    @Test
    fun `markFailed stores error code and clears inFlight and completed`() {
        val running = initialExportDialogState(ExportedCardFormat.Png, AppLanguage.English)
            .markInFlight()
        val failed = running.markFailed("server_busy")
        assertEquals("server_busy", failed.errorCode)
        assertFalse(failed.inFlight)
        assertFalse(failed.completed)
    }
}
