package com.gkim.im.android.feature.contacts

import com.gkim.im.android.core.model.ContactSortMode
import com.gkim.im.android.data.repository.DefaultContactsRepository
import com.gkim.im.android.data.repository.InMemoryMessagingRepository
import com.gkim.im.android.data.repository.seedContacts
import com.gkim.im.android.data.repository.seedConversations
import com.gkim.im.android.testing.FakePreferencesStore
import com.gkim.im.android.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `contacts view model applies sort changes and opens conversations`() = runTest(mainDispatcherRule.dispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val preferencesStore = FakePreferencesStore()
        val contactsRepository = DefaultContactsRepository(seedContacts, preferencesStore, dispatcher)
        val messagingRepository = InMemoryMessagingRepository(seedConversations)
        val viewModel = ContactsViewModel(contactsRepository, messagingRepository)
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()
        assertEquals(listOf("aria-thorne", "clara-wu", "leo-vance"), viewModel.uiState.value.contacts.map { it.id })

        viewModel.setSortMode(ContactSortMode.AddedDescending)
        advanceUntilIdle()

        assertEquals(ContactSortMode.AddedDescending, viewModel.uiState.value.sortMode)
        assertEquals(listOf("clara-wu", "leo-vance", "aria-thorne"), viewModel.uiState.value.contacts.map { it.id })

        val conversationId = viewModel.openContact(seedContacts.first())
        assertTrue(conversationId.startsWith("room-"))

        collector.cancel()
    }
}
