package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.ContactSortMode
import com.gkim.im.android.testing.FakePreferencesStore
import com.gkim.im.android.testing.InMemorySecureKeyValueStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoriesTest {
    @Test
    fun `contacts repository sorts contacts using selected mode`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val preferencesStore = FakePreferencesStore()
        val repository = DefaultContactsRepository(seedContacts, preferencesStore, dispatcher)

        advanceUntilIdle()
        assertEquals(listOf("aria-thorne", "clara-wu", "leo-vance"), repository.sortedContacts.value.map { it.id })

        repository.setSortMode(ContactSortMode.AddedDescending)
        advanceUntilIdle()

        assertEquals(ContactSortMode.AddedDescending, preferencesStore.currentSortMode)
        assertEquals(listOf("clara-wu", "leo-vance", "aria-thorne"), repository.sortedContacts.value.map { it.id })
    }

    @Test
    fun `feed repository filters prompts by category and query`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = DefaultFeedRepository(seedPosts, seedPrompts, dispatcher)

        repository.setPromptCategory(com.gkim.im.android.core.model.PromptCategory.CodeArt)
        repository.setPromptQuery("terminal")
        advanceUntilIdle()

        assertEquals(listOf("prompt-3"), repository.filteredPrompts.value.map { it.id })
    }

    @Test
    fun `aigc repository persists custom provider inputs and generation history`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val preferencesStore = FakePreferencesStore()
        val secureStore = InMemorySecureKeyValueStore()
        val repository = DefaultAigcRepository(presetProviders, preferencesStore, secureStore, dispatcher)

        repository.setActiveProvider("custom")
        repository.updateCustomProvider(
            baseUrl = "https://gateway.example.com/v1",
            model = "gpt-image-1",
            apiKey = "secret-token",
        )
        advanceUntilIdle()

        val task = repository.generate(AigcMode.TextToImage, "Render a neon portrait")

        assertEquals("custom", repository.activeProviderId.value)
        assertEquals("https://gateway.example.com/v1", repository.customProvider.value.baseUrl)
        assertEquals("gpt-image-1", repository.customProvider.value.model)
        assertEquals("secret-token", secureStore.peek("custom_api_key"))
        assertEquals("custom", preferencesStore.currentProviderId)
        assertEquals("https://gateway.example.com/v1", preferencesStore.currentBaseUrl)
        assertEquals("gpt-image-1", preferencesStore.currentModel)
        assertEquals("Render a neon portrait", repository.draftRequest.value.prompt)
        assertEquals(task.id, repository.history.value.first().id)
        assertTrue(task.outputPreview.isNotBlank())
    }
}
