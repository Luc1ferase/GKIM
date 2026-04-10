package com.gkim.im.android.feature.messages

import com.gkim.im.android.data.repository.InMemoryMessagingRepository
import com.gkim.im.android.data.repository.MessagingRepository
import com.gkim.im.android.data.repository.seedConversations
import com.gkim.im.android.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `messages view model exposes populated conversation summary`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = MessagesViewModel(InMemoryMessagingRepository(seedConversations))
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.conversations.size)
        assertEquals(3, viewModel.uiState.value.totalUnread)

        collector.cancel()
    }

    @Test
    fun `messages view model uses repository supplied conversations when wrapped by a custom repository`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = MessagesViewModel(
            FakeMessagingRepository(
                delegate = InMemoryMessagingRepository(seedConversations.take(1))
            )
        )
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.conversations.size)
        assertEquals(2, viewModel.uiState.value.totalUnread)
        assertEquals("room-leo", viewModel.uiState.value.conversations.single().id)

        collector.cancel()
    }

    private class FakeMessagingRepository(
        private val delegate: MessagingRepository = InMemoryMessagingRepository(seedConversations),
    ) : MessagingRepository by delegate
}
