package com.gkim.im.android.feature.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionMemory
import com.gkim.im.android.core.model.CompanionMemoryPin
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.data.repository.CompanionMemorySnapshot
import com.gkim.im.android.data.repository.DefaultCompanionMemoryRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoryAndPresetIntegrationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun memoryPanelRendersSeededSummaryAndPinsOnOpen() {
        val repo = seededRepo(
            summary = LocalizedText("Nova remembers your birthday picnic.", "Nova 记得你的生日野餐。"),
            pins = listOf(
                pin("pin-birthday", "Birthday is April 22.", "生日是 4 月 22 日。", createdAt = 100L),
                pin("pin-tea", "Favorite tea is matcha.", "最爱抹茶。", createdAt = 200L),
            ),
        )
        setPanelContent(repo, currentTurn = 7)

        composeRule.onNodeWithTag("memory-panel-root").assertIsDisplayed()
        composeRule.onNodeWithTag("memory-panel-summary-body").assertIsDisplayed()
        composeRule.onNodeWithTag("memory-panel-summary-subtitle").assertIsDisplayed()
        composeRule.onNodeWithTag("memory-panel-pin-text-pin-birthday").assertIsDisplayed()
        composeRule.onNodeWithTag("memory-panel-pin-text-pin-tea").assertIsDisplayed()
    }

    @Test
    fun newPinAppearsInPinnedFactsListAfterSave() {
        val repo = seededRepo()
        setPanelContent(repo)

        composeRule.onNodeWithTag("memory-panel-pins-empty").assertIsDisplayed()
        composeRule.onNodeWithTag("memory-panel-pins-new").performClick()
        composeRule.onNodeWithTag("memory-panel-pin-editor-create").assertIsDisplayed()
        composeRule.onNodeWithTag("memory-panel-pin-editor-en").performTextInput("Loves the beach.")
        composeRule.onNodeWithTag("memory-panel-pin-editor-zh").performTextInput("喜欢海滩。")
        composeRule.onNodeWithTag("memory-panel-pin-editor-save").performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("memory-panel-pins-empty").assertIsNotDisplayed()
    }

    @Test
    fun editPinPersistsAndUpdatesRow() {
        val repo = seededRepo(
            pins = listOf(pin("pin-a", "Old text.", "旧文字。", createdAt = 10L)),
        )
        setPanelContent(repo)

        composeRule.onNodeWithTag("memory-panel-pin-edit-pin-a").performClick()
        composeRule.onNodeWithTag("memory-panel-pin-editor-edit").assertIsDisplayed()
        composeRule.onNodeWithTag("memory-panel-pin-editor-en").performTextInput(" updated.")
        composeRule.onNodeWithTag("memory-panel-pin-editor-save").performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("memory-panel-pin-text-pin-a").assertIsDisplayed()
    }

    @Test
    fun clearPinnedResetPreservesSummaryAndEmptiesPinsList() {
        val repo = seededRepo(
            summary = LocalizedText("Keep the summary.", "保留摘要。"),
            pins = listOf(pin("pin-drop", "Drop me.", "移除。", createdAt = 10L)),
        )
        setPanelContent(repo)

        composeRule.onNodeWithTag("memory-panel-pin-text-pin-drop").assertIsDisplayed()
        composeRule.onNodeWithTag("memory-panel-summary-body").assertIsDisplayed()

        composeRule.onNodeWithTag("memory-panel-reset-pins").performScrollTo().performClick()
        composeRule.waitUntilTagDisplayed("memory-panel-reset-confirmation")
        composeRule.onNodeWithTag("memory-panel-reset-confirm-pins").performScrollTo().performClick()
        composeRule.waitUntilTagAbsent("memory-panel-pin-text-pin-drop")

        composeRule.onNodeWithTag("memory-panel-summary-body").assertIsDisplayed()
        composeRule.onNodeWithTag("memory-panel-pins-empty").assertIsDisplayed()
    }

    @Test
    fun clearSummaryResetEmptiesSummaryAndPreservesPins() {
        val repo = seededRepo(
            summary = LocalizedText("Drop the summary.", "清除摘要。"),
            pins = listOf(pin("pin-stay", "Stay pinned.", "继续保留。", createdAt = 10L)),
        )
        setPanelContent(repo)

        composeRule.onNodeWithTag("memory-panel-summary-body").assertIsDisplayed()
        composeRule.onNodeWithTag("memory-panel-pin-text-pin-stay").assertIsDisplayed()

        composeRule.onNodeWithTag("memory-panel-reset-summary").performScrollTo().performClick()
        composeRule.waitUntilTagDisplayed("memory-panel-reset-confirmation")
        composeRule.onNodeWithTag("memory-panel-reset-confirm-summary").performScrollTo().performClick()
        composeRule.waitUntilTagDisplayed("memory-panel-summary-empty")

        composeRule.onNodeWithTag("memory-panel-pin-text-pin-stay").assertIsDisplayed()
    }

    @Test
    fun clearAllResetEmptiesBothSummaryAndPins() {
        val repo = seededRepo(
            summary = LocalizedText("Drop all.", "全部清除。"),
            pins = listOf(pin("pin-x", "X.", "叉。", createdAt = 10L)),
        )
        setPanelContent(repo)

        composeRule.onNodeWithTag("memory-panel-reset-all").performScrollTo().performClick()
        composeRule.waitUntilTagDisplayed("memory-panel-reset-confirmation")
        composeRule.onNodeWithTag("memory-panel-reset-confirm-all").performScrollTo().performClick()
        composeRule.waitUntilTagDisplayed("memory-panel-summary-empty")

        composeRule.onNodeWithTag("memory-panel-pins-empty").assertIsDisplayed()
    }

    @Test
    fun cancelResetConfirmationKeepsStateIntact() {
        val repo = seededRepo(
            summary = LocalizedText("Unchanged.", "未动。"),
            pins = listOf(pin("pin-safe", "Safe.", "安全。", createdAt = 10L)),
        )
        setPanelContent(repo)

        composeRule.onNodeWithTag("memory-panel-reset-all").performScrollTo().performClick()
        composeRule.waitUntilTagDisplayed("memory-panel-reset-confirmation")
        composeRule.onNodeWithTag("memory-panel-reset-cancel-all").performScrollTo().performClick()
        composeRule.waitUntilTagAbsent("memory-panel-reset-confirmation")

        composeRule.onNodeWithTag("memory-panel-summary-body").assertIsDisplayed()
        composeRule.onNodeWithTag("memory-panel-pin-text-pin-safe").assertIsDisplayed()
    }

    @Test
    fun deletePinRemovesRowFromList() {
        val repo = seededRepo(
            pins = listOf(
                pin("pin-keep", "Keep me.", "保留。", createdAt = 10L),
                pin("pin-drop", "Drop me.", "移除。", createdAt = 20L),
            ),
        )
        setPanelContent(repo)

        composeRule.onNodeWithTag("memory-panel-pin-text-pin-drop").assertIsDisplayed()
        composeRule.onNodeWithTag("memory-panel-pin-delete-pin-drop").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("memory-panel-pin-text-pin-drop").assertIsNotDisplayed()
        composeRule.onNodeWithTag("memory-panel-pin-text-pin-keep").assertIsDisplayed()
    }

    private fun setPanelContent(
        repo: DefaultCompanionMemoryRepository,
        cardId: String = CARD_ID,
        currentTurn: Int = 0,
        language: AppLanguage = AppLanguage.English,
    ) {
        composeRule.setContent {
            TestableMemoryPanel(
                repo = repo,
                cardId = cardId,
                currentTurn = currentTurn,
                language = language,
            )
        }
    }

    @Composable
    private fun TestableMemoryPanel(
        repo: DefaultCompanionMemoryRepository,
        cardId: String,
        currentTurn: Int,
        language: AppLanguage,
    ) {
        val viewModel = remember(cardId) {
            MemoryPanelViewModel(
                repository = repo,
                cardId = cardId,
                currentTurnProvider = { currentTurn },
            )
        }
        val state by viewModel.uiState.collectAsState()
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            MemoryPanelContent(
                state = state,
                language = language,
                onBack = {},
                onNewPin = { viewModel.openPinEditorForNew() },
                onEditPin = viewModel::openPinEditorForEdit,
                onDeletePin = viewModel::deletePin,
                onSetPinEnglish = viewModel::setPinEnglish,
                onSetPinChinese = viewModel::setPinChinese,
                onSavePin = viewModel::savePinEditor,
                onCancelPin = viewModel::cancelPinEditor,
                onRequestReset = viewModel::requestReset,
                onConfirmReset = viewModel::confirmReset,
                onCancelReset = viewModel::cancelResetConfirmation,
                onClearError = viewModel::clearError,
            )
        }
    }

    private fun seededRepo(
        summary: LocalizedText = LocalizedText.Empty,
        summaryTurnCursor: Int = 0,
        pins: List<CompanionMemoryPin> = emptyList(),
    ): DefaultCompanionMemoryRepository {
        val memory = CompanionMemory(
            userId = "user-local",
            companionCardId = CARD_ID,
            summary = summary,
            summaryUpdatedAt = 0L,
            summaryTurnCursor = summaryTurnCursor,
        )
        val repo = DefaultCompanionMemoryRepository()
        repo.setSnapshot(CARD_ID, CompanionMemorySnapshot(memory = memory, pins = pins))
        return repo
    }

    private fun pin(
        id: String,
        english: String,
        chinese: String,
        sourceMessageId: String? = null,
        createdAt: Long,
    ): CompanionMemoryPin = CompanionMemoryPin(
        id = id,
        sourceMessageId = sourceMessageId,
        text = LocalizedText(english, chinese),
        createdAt = createdAt,
        pinnedByUser = true,
    )

    private companion object {
        const val CARD_ID = "card-nova"
    }
}

private fun ComposeContentTestRule.waitUntilTagDisplayed(tag: String, timeoutMillis: Long = 5000L) {
    waitUntil(timeoutMillis) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
}

private fun ComposeContentTestRule.waitUntilTagAbsent(tag: String, timeoutMillis: Long = 5000L) {
    waitUntil(timeoutMillis) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
    }
}
