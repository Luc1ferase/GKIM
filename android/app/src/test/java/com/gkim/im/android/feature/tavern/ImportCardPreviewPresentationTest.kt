package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.data.remote.im.CompanionCharacterCardDto
import com.gkim.im.android.data.remote.im.LocalizedTextDto
import com.gkim.im.android.data.repository.CardImportPreview
import com.gkim.im.android.data.repository.CardImportWarning
import com.gkim.im.android.data.repository.CardInteropRepository
import com.gkim.im.android.data.repository.ExportedCardFormat
import com.gkim.im.android.data.repository.ExportedCardPayload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImportCardPreviewPresentationTest {
    @Test
    fun `submit transitions to Loading then Loaded on preview success`() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeInteropRepository(previewResult = Result.success(samplePreview()))
        val vm = ImportCardPreviewViewModel(repo, scopeOverride = backgroundScope)
        vm.submit("{\"name\":\"Aria\"}".toByteArray(), "aria.json")
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue("expected Loaded but was $state", state is ImportCardPreviewUiState.Loaded)
        val loaded = state as ImportCardPreviewUiState.Loaded
        assertEquals("en", loaded.selectedLanguage)
        assertEquals("card-import-1", loaded.preview.card.id)
        assertEquals(2, loaded.preview.warnings.size)
        assertEquals(1, repo.previewCalls)
    }

    @Test
    fun `submit transitions to Failed when backend returns failure`() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeInteropRepository(previewResult = Result.failure(IllegalStateException("payload_too_large")))
        val vm = ImportCardPreviewViewModel(repo, scopeOverride = backgroundScope)
        vm.submit("{}".toByteArray(), "x.json")
        advanceUntilIdle()
        assertEquals(
            ImportCardPreviewUiState.Failed("payload_too_large"),
            vm.state.value,
        )
    }

    @Test
    fun `selectLanguage overrides the detected language on loaded preview`() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeInteropRepository(previewResult = Result.success(samplePreview()))
        val vm = ImportCardPreviewViewModel(repo, scopeOverride = backgroundScope)
        vm.submit("{}".toByteArray(), "x.json")
        advanceUntilIdle()
        vm.selectLanguage("zh")
        val loaded = vm.state.value as ImportCardPreviewUiState.Loaded
        assertEquals("zh", loaded.selectedLanguage)
    }

    @Test
    fun `selectLanguage is ignored outside of Loaded state`() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeInteropRepository()
        val vm = ImportCardPreviewViewModel(repo, scopeOverride = backgroundScope)
        vm.selectLanguage("zh")
        assertEquals(ImportCardPreviewUiState.Idle, vm.state.value)
    }

    @Test
    fun `commit forwards selected language override and lands on Committed with persisted card`() = runTest(UnconfinedTestDispatcher()) {
        val persisted = sampleCard("card-persisted-1")
        val repo = FakeInteropRepository(
            previewResult = Result.success(samplePreview()),
            commitResult = Result.success(persisted),
        )
        val vm = ImportCardPreviewViewModel(repo, scopeOverride = backgroundScope)
        vm.submit("{}".toByteArray(), "x.json")
        advanceUntilIdle()
        vm.selectLanguage("zh")
        vm.commit()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("expected Committed but was $state", state is ImportCardPreviewUiState.Committed)
        assertEquals(persisted, (state as ImportCardPreviewUiState.Committed).card)
        assertEquals("zh", repo.lastLanguageOverride)
        assertEquals(1, repo.commitCalls)
    }

    @Test
    fun `commit surfaces backend failure and keeps preview reachable via reset`() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeInteropRepository(
            previewResult = Result.success(samplePreview()),
            commitResult = Result.failure(IllegalStateException("server_busy")),
        )
        val vm = ImportCardPreviewViewModel(repo, scopeOverride = backgroundScope)
        vm.submit("{}".toByteArray(), "x.json")
        advanceUntilIdle()
        vm.commit()
        advanceUntilIdle()
        assertEquals(ImportCardPreviewUiState.Failed("server_busy"), vm.state.value)

        vm.reset()
        assertEquals(ImportCardPreviewUiState.Idle, vm.state.value)
    }

    @Test
    fun `commit does nothing when already committing`() = runTest(UnconfinedTestDispatcher()) {
        val persisted = sampleCard("card-persisted-1")
        val repo = FakeInteropRepository(
            previewResult = Result.success(samplePreview()),
            commitResult = Result.success(persisted),
        )
        val vm = ImportCardPreviewViewModel(repo, scopeOverride = backgroundScope)
        vm.submit("{}".toByteArray(), "x.json")
        advanceUntilIdle()

        // Force loaded state into committing=true without advancing to completion
        repo.pauseCommit = true
        vm.commit()
        vm.commit() // second call should be ignored
        repo.releaseCommit()
        advanceUntilIdle()

        assertEquals(1, repo.commitCalls)
    }

    private fun samplePreview(): CardImportPreview {
        val dto = sampleCardDto("card-import-1")
        return CardImportPreview(
            previewToken = "preview-x",
            card = dto.toCompanionCharacterCard(),
            rawCardDto = dto,
            detectedLanguage = "en",
            warnings = listOf(
                CardImportWarning("post_history_instruction_parked", "stPostHistoryInstructions"),
                CardImportWarning("field_truncated", "personality", "> 32 KiB"),
            ),
            stExtensionKeys = listOf("stPostHistoryInstructions"),
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

    private fun sampleCard(id: String): CompanionCharacterCard = CompanionCharacterCard(
        id = id,
        displayName = LocalizedText("Aria", "Aria"),
        roleLabel = LocalizedText("Guide", "向导"),
        summary = LocalizedText("Calm guide", "Calm guide"),
        firstMes = LocalizedText("Hello traveller.", "你好，旅人。"),
        alternateGreetings = emptyList(),
        systemPrompt = LocalizedText("", ""),
        personality = LocalizedText("", ""),
        scenario = LocalizedText("", ""),
        exampleDialogue = LocalizedText("", ""),
        tags = emptyList(),
        creator = "",
        creatorNotes = "",
        characterVersion = "",
        avatarText = "AR",
        avatarUri = null,
        accent = AccentTone.Primary,
        source = CompanionCharacterSource.UserAuthored,
        extensions = JsonObject(emptyMap()),
    )

    private class FakeInteropRepository(
        private val previewResult: Result<CardImportPreview> = Result.success(
            CardImportPreview(
                previewToken = "",
                card = CompanionCharacterCard(
                    id = "",
                    displayName = LocalizedText("", ""),
                    roleLabel = LocalizedText("", ""),
                    summary = LocalizedText("", ""),
                    firstMes = LocalizedText("", ""),
                    avatarText = "",
                    accent = AccentTone.Primary,
                    source = CompanionCharacterSource.UserAuthored,
                ),
                rawCardDto = CompanionCharacterCardDto(
                    id = "",
                    displayName = LocalizedTextDto("", ""),
                    roleLabel = LocalizedTextDto("", ""),
                    summary = LocalizedTextDto("", ""),
                    avatarText = "",
                    accent = "primary",
                    source = "userauthored",
                ),
                detectedLanguage = "en",
                warnings = emptyList(),
                stExtensionKeys = emptyList(),
            ),
        ),
        private val commitResult: Result<CompanionCharacterCard> = Result.failure(
            IllegalStateException("not configured"),
        ),
    ) : CardInteropRepository {
        var previewCalls: Int = 0
        var commitCalls: Int = 0
        var lastLanguageOverride: String? = null
        var pauseCommit: Boolean = false
        private var deferredCommit: (() -> Unit)? = null

        override suspend fun previewImport(bytes: ByteArray, filename: String): Result<CardImportPreview> {
            previewCalls += 1
            return previewResult
        }

        override suspend fun commitImport(
            preview: CardImportPreview,
            overrides: CompanionCharacterCard?,
            languageOverride: String?,
        ): Result<CompanionCharacterCard> {
            commitCalls += 1
            lastLanguageOverride = languageOverride
            if (pauseCommit) {
                kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                    deferredCommit = { continuation.resume(Unit) { } }
                }
            }
            return commitResult
        }

        fun releaseCommit() {
            pauseCommit = false
            deferredCommit?.invoke()
            deferredCommit = null
        }

        override suspend fun exportCard(
            cardId: String,
            format: ExportedCardFormat,
            language: String,
            includeTranslationAlt: Boolean,
        ): Result<ExportedCardPayload> = Result.failure(IllegalStateException("not configured"))
    }
}
