package com.gkim.im.android.data.repository

import android.net.Uri
import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.TaskStatus
import com.gkim.im.android.core.model.ContactSortMode
import com.gkim.im.android.core.model.AttachmentType
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.data.remote.aigc.EncodedMediaPayload
import com.gkim.im.android.data.remote.aigc.MediaInputEncoder
import com.gkim.im.android.data.remote.aigc.RemoteAigcGenerateRequest
import com.gkim.im.android.data.remote.aigc.RemoteAigcGenerateResult
import com.gkim.im.android.data.remote.aigc.RemoteAigcProviderClient
import com.gkim.im.android.core.model.MediaInput
import com.gkim.im.android.testing.FakePreferencesStore
import com.gkim.im.android.testing.InMemorySecureKeyValueStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoriesTest {
    @Test
    fun `preset providers expose requested image models and supported capabilities`() {
        val hunyuan = presetProviders.first { it.id == "hunyuan" }
        val tongyi = presetProviders.first { it.id == "tongyi" }

        assertEquals("hy-image-v3.0", hunyuan.model)
        assertEquals(setOf(AigcMode.TextToImage), hunyuan.capabilities)
        assertEquals("wan2.7-image", tongyi.model)
        assertEquals(setOf(AigcMode.TextToImage, AigcMode.ImageToImage), tongyi.capabilities)
    }

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
    fun `aigc repository persists custom provider inputs`() = runTest {
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

        assertEquals("custom", repository.activeProviderId.value)
        assertEquals("https://gateway.example.com/v1", repository.customProvider.value.baseUrl)
        assertEquals("gpt-image-1", repository.customProvider.value.model)
        assertEquals("secret-token", secureStore.peek("custom_api_key"))
        assertEquals("custom", preferencesStore.currentProviderId)
        assertEquals("https://gateway.example.com/v1", preferencesStore.currentBaseUrl)
        assertEquals("gpt-image-1", preferencesStore.currentModel)
    }

    @Test
    fun `aigc repository persists and restores preset provider credentials locally`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val preferencesStore = FakePreferencesStore(
            initialPresetModels = mapOf("hunyuan" to "hy-image-v3.5"),
        )
        val secureStore = InMemorySecureKeyValueStore(
            mapOf("preset_provider_hunyuan_api_key" to "h-secret")
        )
        val repository = DefaultAigcRepository(presetProviders, preferencesStore, secureStore, dispatcher)

        advanceUntilIdle()

        assertEquals("hy-image-v3.5", repository.providers.value.first { it.id == "hunyuan" }.model)
        assertEquals("h-secret", repository.presetProviderConfigs.value.getValue("hunyuan").apiKey)

        repository.updatePresetProviderConfig(
            providerId = "tongyi",
            model = "wan2.7-image-plus",
            apiKey = "t-secret",
        )
        advanceUntilIdle()

        assertEquals("wan2.7-image-plus", repository.providers.value.first { it.id == "tongyi" }.model)
        assertEquals("wan2.7-image-plus", repository.presetProviderConfigs.value.getValue("tongyi").model)
        assertEquals("t-secret", repository.presetProviderConfigs.value.getValue("tongyi").apiKey)
        assertEquals("wan2.7-image-plus", preferencesStore.currentPresetModels.getValue("tongyi"))
        assertEquals("t-secret", secureStore.peek("preset_provider_tongyi_api_key"))
    }

    @Test
    fun `aigc repository queues then resolves preset provider generations truthfully`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val preferencesStore = FakePreferencesStore()
        val secureStore = InMemorySecureKeyValueStore()
        val completion = CompletableDeferred<RemoteAigcGenerateResult>()
        val client = RecordingRemoteAigcProviderClient(completion)
        val repository = DefaultAigcRepository(
            presets = presetProviders,
            preferencesStore = preferencesStore,
            secureStore = secureStore,
            providerClients = mapOf("hunyuan" to client),
            mediaInputEncoder = FakeMediaInputEncoder(),
            dispatcher = dispatcher,
        )

        repository.updatePresetProviderConfig(providerId = "hunyuan", apiKey = "sk-hunyuan")
        advanceUntilIdle()

        val generation = async {
            repository.generate(
                mode = AigcMode.TextToImage,
                prompt = "Render a polished poster variation",
            )
        }

        advanceUntilIdle()

        val queuedTask = repository.history.value.first()
        assertEquals(TaskStatus.Queued, queuedTask.status)
        assertEquals("hunyuan", queuedTask.providerId)
        assertEquals("hy-image-v3.0", queuedTask.model)
        assertNull(queuedTask.outputPreview)
        assertEquals("Render a polished poster variation", repository.draftRequest.value.prompt)
        assertEquals(null, client.requests.single().imageBase64)

        completion.complete(
            RemoteAigcGenerateResult(
                remoteId = "remote-1",
                outputUrl = "https://cdn.example.com/generated.png",
            )
        )
        advanceUntilIdle()

        val completedTask = generation.await()

        assertEquals(TaskStatus.Succeeded, completedTask.status)
        assertEquals("https://cdn.example.com/generated.png", completedTask.outputPreview)
        assertEquals("remote-1", completedTask.remoteId)
        assertEquals(TaskStatus.Succeeded, repository.history.value.first().status)
    }

    @Test
    fun `aigc repository fails preset generation when the local api key is missing`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = DefaultAigcRepository(
            presets = presetProviders,
            preferencesStore = FakePreferencesStore(),
            secureStore = InMemorySecureKeyValueStore(),
            providerClients = mapOf("hunyuan" to RecordingRemoteAigcProviderClient(CompletableDeferred())),
            mediaInputEncoder = FakeMediaInputEncoder(),
            dispatcher = dispatcher,
        )

        val task = repository.generate(
            mode = AigcMode.TextToImage,
            prompt = "Render a quiet portrait",
        )

        assertEquals(TaskStatus.Failed, task.status)
        assertTrue(task.errorMessage.orEmpty().contains("API key"))
        assertNull(task.outputPreview)
    }

    @Test
    fun `aigc repository rejects image to image when source image is missing`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val completion = CompletableDeferred<RemoteAigcGenerateResult>()
        val client = RecordingRemoteAigcProviderClient(completion)
        val repository = DefaultAigcRepository(
            presets = presetProviders,
            preferencesStore = FakePreferencesStore(),
            secureStore = InMemorySecureKeyValueStore(mapOf("preset_provider_tongyi_api_key" to "sk-tongyi")),
            providerClients = mapOf("tongyi" to client),
            mediaInputEncoder = FakeMediaInputEncoder(),
            dispatcher = dispatcher,
        )

        repository.setActiveProvider("tongyi")
        advanceUntilIdle()

        val task = repository.generate(
            mode = AigcMode.ImageToImage,
            prompt = "Stylize this portrait with a cinematic grade.",
            mediaInput = null,
        )

        assertEquals(TaskStatus.Failed, task.status)
        assertTrue(task.errorMessage.orEmpty().contains("source image"))
        assertTrue(client.requests.isEmpty())
    }

    @Test
    fun `messaging repository sends image attachments as normal outgoing messages`() {
        val repository = InMemoryMessagingRepository(seedConversations)
        val attachment = com.gkim.im.android.core.model.MessageAttachment(
            type = AttachmentType.Image,
            preview = "content://chat-tests/attachment-image",
        )

        repository.sendMessage(
            conversationId = "room-leo",
            body = "",
            attachment = attachment,
        )

        val updatedConversation = repository.conversations.value.first { it.id == "room-leo" }
        val sentMessage = updatedConversation.messages.last()

        assertEquals(MessageDirection.Outgoing, sentMessage.direction)
        assertEquals(attachment, sentMessage.attachment)
        assertEquals("Sent an image", updatedConversation.lastMessage)
    }
}

private class RecordingRemoteAigcProviderClient(
    private val completion: CompletableDeferred<RemoteAigcGenerateResult>,
) : RemoteAigcProviderClient {
    val requests = mutableListOf<RemoteAigcGenerateRequest>()

    override suspend fun generate(request: RemoteAigcGenerateRequest): RemoteAigcGenerateResult {
        requests += request
        return completion.await()
    }
}

private class FakeMediaInputEncoder : MediaInputEncoder {
    override suspend fun encode(mediaInput: MediaInput): EncodedMediaPayload {
        return EncodedMediaPayload(
            base64Data = "FAKE_BASE64_IMAGE",
            mimeType = "image/png",
        )
    }
}
