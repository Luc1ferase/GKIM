package com.gkim.im.android.feature.messages

import com.gkim.im.android.data.repository.MessagingIntegrationPhase
import com.gkim.im.android.data.repository.MessagingIntegrationState
import com.gkim.im.android.data.repository.MessagingRepository
import com.gkim.im.android.data.repository.InMemoryMessagingRepository
import com.gkim.im.android.data.repository.seedConversations
import com.gkim.im.android.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    fun `messages view model exposes integration state from live repository`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = MessagesViewModel(
            FakeMessagingRepository(
                initialIntegrationState = MessagingIntegrationState(
                    phase = MessagingIntegrationPhase.Error,
                    activeUserExternalId = "nox-dev",
                    message = "bootstrap offline",
                )
            )
        )
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()

        assertEquals(MessagingIntegrationPhase.Error, viewModel.uiState.value.integrationState.phase)
        assertEquals("bootstrap offline", viewModel.uiState.value.integrationState.message)

        collector.cancel()
    }

    private class FakeMessagingRepository(
        private val delegate: MessagingRepository = InMemoryMessagingRepository(seedConversations),
        initialIntegrationState: MessagingIntegrationState,
    ) : MessagingRepository by delegate {
        private val integrationStateValue = MutableStateFlow(initialIntegrationState)

        override val integrationState: StateFlow<MessagingIntegrationState> = integrationStateValue
    }
}
