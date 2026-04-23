package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.CompanionMemory
import com.gkim.im.android.core.model.CompanionMemoryPin
import com.gkim.im.android.core.model.CompanionMemoryResetScope
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.data.repository.CompanionMemorySnapshot
import com.gkim.im.android.data.repository.DefaultCompanionMemoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryPanelPresentationTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val cardId = "card-nova"

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `summary and pins hydrate into ui state from repository snapshot`() = runTest(dispatcher) {
        val repo = seededRepo(
            summary = LocalizedText("Nova remembers the picnic.", "Nova 记得那次野餐。"),
            pins = listOf(
                pin("pin-1", "Birthday is April 22.", "生日是 4 月 22 日。", createdAt = 100L),
                pin("pin-2", "Favorite tea is matcha.", "最爱抹茶。", createdAt = 200L),
            ),
        )
        val vm = MemoryPanelViewModel(repo, cardId, currentTurnProvider = { 4 })
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(cardId, state.cardId)
        assertEquals("Nova remembers the picnic.", state.memory?.summary?.english)
        assertEquals(listOf("pin-1", "pin-2"), state.pins.map { it.id })
        assertEquals(4, state.currentTurn)
        assertNull(state.pinEditor)
        assertNull(state.resetConfirmation)
        assertNull(state.operationError)
    }

    @Test
    fun `pins are sorted by createdAt regardless of snapshot order`() = runTest(dispatcher) {
        val repo = seededRepo(
            summary = LocalizedText.Empty,
            pins = listOf(
                pin("pin-late", "Late pin", "后一个", createdAt = 500L),
                pin("pin-early", "Early pin", "前一个", createdAt = 100L),
                pin("pin-mid", "Middle pin", "中间", createdAt = 250L),
            ),
        )
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        assertEquals(
            listOf("pin-early", "pin-mid", "pin-late"),
            vm.uiState.value.pins.map { it.id },
        )
    }

    @Test
    fun `turnsSinceSummaryUpdate computes from cursor and current turn`() = runTest(dispatcher) {
        val repo = seededRepo(
            summary = LocalizedText("Snapshot summary.", "摘要快照。"),
            summaryTurnCursor = 7,
        )
        val vm = MemoryPanelViewModel(repo, cardId, currentTurnProvider = { 12 })
        advanceUntilIdle()

        assertEquals(5, vm.uiState.value.turnsSinceSummaryUpdate)
    }

    @Test
    fun `turnsSinceSummaryUpdate coerces negative deltas to zero`() = runTest(dispatcher) {
        val repo = seededRepo(
            summary = LocalizedText("Snapshot.", "快照。"),
            summaryTurnCursor = 10,
        )
        val vm = MemoryPanelViewModel(repo, cardId, currentTurnProvider = { 3 })
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.turnsSinceSummaryUpdate)
    }

    @Test
    fun `turnsSinceSummaryUpdate is null when no memory snapshot exists`() = runTest(dispatcher) {
        val repo = DefaultCompanionMemoryRepository()
        val vm = MemoryPanelViewModel(repo, cardId, currentTurnProvider = { 5 })
        advanceUntilIdle()

        assertNull(vm.uiState.value.turnsSinceSummaryUpdate)
    }

    @Test
    fun `openPinEditorForNew starts editor in create mode with source message id`() = runTest(dispatcher) {
        val repo = DefaultCompanionMemoryRepository()
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        vm.openPinEditorForNew(sourceMessageId = "msg-42")

        val editor = vm.uiState.value.pinEditor
        assertNotNull(editor)
        assertTrue(editor!!.isCreate)
        assertEquals("msg-42", editor.sourceMessageId)
        assertEquals("", editor.englishText)
        assertEquals("", editor.chineseText)
        assertFalse("empty fields cannot be saved", editor.canSave)
    }

    @Test
    fun `openPinEditorForEdit loads existing pin text into editor`() = runTest(dispatcher) {
        val repo = seededRepo(
            pins = listOf(
                pin("pin-edit", "Loves hiking.", "喜欢远足。", sourceMessageId = "msg-7", createdAt = 10L),
            ),
        )
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        vm.openPinEditorForEdit("pin-edit")

        val editor = vm.uiState.value.pinEditor
        assertNotNull(editor)
        assertFalse("editing an existing pin is not create", editor!!.isCreate)
        assertEquals("pin-edit", editor.pinId)
        assertEquals("msg-7", editor.sourceMessageId)
        assertEquals("Loves hiking.", editor.englishText)
        assertEquals("喜欢远足。", editor.chineseText)
        assertTrue("populated fields allow save", editor.canSave)
    }

    @Test
    fun `openPinEditorForEdit is a no-op when pin id is unknown`() = runTest(dispatcher) {
        val repo = DefaultCompanionMemoryRepository()
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        vm.openPinEditorForEdit("missing-pin")

        assertNull(vm.uiState.value.pinEditor)
    }

    @Test
    fun `save pin editor in create mode persists pin with both texts`() = runTest(dispatcher) {
        val repo = DefaultCompanionMemoryRepository()
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        vm.openPinEditorForNew(sourceMessageId = "msg-11")
        vm.setPinEnglish("Allergic to peanuts.")
        vm.setPinChinese("对花生过敏。")
        assertTrue(vm.uiState.value.pinEditor!!.canSave)

        vm.savePinEditor()
        advanceUntilIdle()

        assertNull("editor closes after successful save", vm.uiState.value.pinEditor)
        val pins = repo.observePins(cardId).first()
        assertEquals(1, pins.size)
        assertEquals("Allergic to peanuts.", pins.first().text.english)
        assertEquals("对花生过敏。", pins.first().text.chinese)
        assertEquals("msg-11", pins.first().sourceMessageId)
    }

    @Test
    fun `save pin editor in edit mode updates existing pin without creating a new one`() = runTest(dispatcher) {
        val repo = seededRepo(
            pins = listOf(pin("pin-xx", "Old english.", "旧中文。", createdAt = 10L)),
        )
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        vm.openPinEditorForEdit("pin-xx")
        vm.setPinEnglish("New english.")
        vm.setPinChinese("新中文。")
        vm.savePinEditor()
        advanceUntilIdle()

        val pins = repo.observePins(cardId).first()
        assertEquals("still only one pin", 1, pins.size)
        assertEquals("pin-xx", pins.first().id)
        assertEquals("New english.", pins.first().text.english)
        assertEquals("新中文。", pins.first().text.chinese)
        assertNull(vm.uiState.value.pinEditor)
    }

    @Test
    fun `save pin editor is a no-op when one language text is blank`() = runTest(dispatcher) {
        val repo = DefaultCompanionMemoryRepository()
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        vm.openPinEditorForNew()
        vm.setPinEnglish("Only english.")
        // leave chinese blank
        assertFalse(vm.uiState.value.pinEditor!!.canSave)

        vm.savePinEditor()
        advanceUntilIdle()

        assertEquals("no pin should have been created", 0, repo.observePins(cardId).first().size)
        assertNotNull("editor stays open so user can finish", vm.uiState.value.pinEditor)
    }

    @Test
    fun `cancel pin editor closes editor without touching repository`() = runTest(dispatcher) {
        val repo = DefaultCompanionMemoryRepository()
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        vm.openPinEditorForNew()
        vm.setPinEnglish("Draft.")
        vm.setPinChinese("草稿。")
        vm.cancelPinEditor()
        advanceUntilIdle()

        assertNull(vm.uiState.value.pinEditor)
        assertEquals(0, repo.observePins(cardId).first().size)
    }

    @Test
    fun `delete pin removes it from repository`() = runTest(dispatcher) {
        val repo = seededRepo(
            pins = listOf(
                pin("pin-keep", "Keep me.", "保留。", createdAt = 10L),
                pin("pin-drop", "Drop me.", "移除。", createdAt = 20L),
            ),
        )
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        vm.deletePin("pin-drop")
        advanceUntilIdle()

        val remaining = repo.observePins(cardId).first().map { it.id }
        assertEquals(listOf("pin-keep"), remaining)
    }

    @Test
    fun `delete pin surfaces failure message on unknown pin`() = runTest(dispatcher) {
        val repo = DefaultCompanionMemoryRepository()
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        vm.deletePin("pin-missing")
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.operationError)
    }

    @Test
    fun `clearError resets operation error field`() = runTest(dispatcher) {
        val repo = DefaultCompanionMemoryRepository()
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        vm.deletePin("pin-missing")
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.operationError)

        vm.clearError()
        assertNull(vm.uiState.value.operationError)
    }

    @Test
    fun `requestReset stores pending scope without touching repository`() = runTest(dispatcher) {
        val repo = seededRepo(
            summary = LocalizedText("Snapshot.", "快照。"),
            pins = listOf(pin("pin-1", "A", "甲", createdAt = 10L)),
        )
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        vm.requestReset(CompanionMemoryResetScope.Summary)

        assertEquals(CompanionMemoryResetScope.Summary, vm.uiState.value.resetConfirmation)
        assertEquals("Snapshot.", repo.observeMemory(cardId).first()?.summary?.english)
        assertEquals(1, repo.observePins(cardId).first().size)
    }

    @Test
    fun `confirmReset with pins scope clears pins but preserves summary`() = runTest(dispatcher) {
        val repo = seededRepo(
            summary = LocalizedText("Keep the summary.", "保留摘要。"),
            pins = listOf(pin("pin-a", "A", "甲", createdAt = 10L)),
        )
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        vm.requestReset(CompanionMemoryResetScope.Pins)
        vm.confirmReset()
        advanceUntilIdle()

        assertNull(vm.uiState.value.resetConfirmation)
        assertEquals(0, repo.observePins(cardId).first().size)
        assertEquals("Keep the summary.", repo.observeMemory(cardId).first()?.summary?.english)
    }

    @Test
    fun `confirmReset with summary scope clears summary but preserves pins`() = runTest(dispatcher) {
        val repo = seededRepo(
            summary = LocalizedText("Drop the summary.", "清除摘要。"),
            pins = listOf(pin("pin-b", "B", "乙", createdAt = 10L)),
        )
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        vm.requestReset(CompanionMemoryResetScope.Summary)
        vm.confirmReset()
        advanceUntilIdle()

        assertNull(vm.uiState.value.resetConfirmation)
        assertEquals("", repo.observeMemory(cardId).first()?.summary?.english)
        assertEquals(listOf("pin-b"), repo.observePins(cardId).first().map { it.id })
    }

    @Test
    fun `confirmReset with all scope clears both pins and summary`() = runTest(dispatcher) {
        val repo = seededRepo(
            summary = LocalizedText("Drop everything.", "全部清除。"),
            pins = listOf(
                pin("pin-c1", "C1", "丙1", createdAt = 10L),
                pin("pin-c2", "C2", "丙2", createdAt = 20L),
            ),
        )
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        vm.requestReset(CompanionMemoryResetScope.All)
        vm.confirmReset()
        advanceUntilIdle()

        assertNull(vm.uiState.value.resetConfirmation)
        assertEquals("", repo.observeMemory(cardId).first()?.summary?.english)
        assertEquals(0, repo.observePins(cardId).first().size)
    }

    @Test
    fun `cancelResetConfirmation discards pending scope without touching repository`() = runTest(dispatcher) {
        val repo = seededRepo(
            summary = LocalizedText("Intact.", "完好。"),
            pins = listOf(pin("pin-keep", "K", "保", createdAt = 10L)),
        )
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        vm.requestReset(CompanionMemoryResetScope.All)
        vm.cancelResetConfirmation()
        advanceUntilIdle()

        assertNull(vm.uiState.value.resetConfirmation)
        assertEquals("Intact.", repo.observeMemory(cardId).first()?.summary?.english)
        assertEquals(1, repo.observePins(cardId).first().size)
    }

    @Test
    fun `confirmReset is a no-op when no pending scope is set`() = runTest(dispatcher) {
        val repo = seededRepo(
            summary = LocalizedText("Still here.", "仍在。"),
            pins = listOf(pin("pin-x", "X", "叉", createdAt = 10L)),
        )
        val vm = MemoryPanelViewModel(repo, cardId)
        advanceUntilIdle()

        vm.confirmReset()
        advanceUntilIdle()

        assertEquals("Still here.", repo.observeMemory(cardId).first()?.summary?.english)
        assertEquals(1, repo.observePins(cardId).first().size)
    }

    private fun seededRepo(
        summary: LocalizedText = LocalizedText.Empty,
        summaryTurnCursor: Int = 0,
        summaryUpdatedAt: Long = 0L,
        pins: List<CompanionMemoryPin> = emptyList(),
    ): DefaultCompanionMemoryRepository {
        val memory = CompanionMemory(
            userId = "user-local",
            companionCardId = cardId,
            summary = summary,
            summaryUpdatedAt = summaryUpdatedAt,
            summaryTurnCursor = summaryTurnCursor,
        )
        val repo = DefaultCompanionMemoryRepository()
        repo.setSnapshot(cardId, CompanionMemorySnapshot(memory = memory, pins = pins))
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
}
