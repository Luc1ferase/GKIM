package com.gkim.im.android.feature.navigation

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.media.GeneratedImageSaveResult
import com.gkim.im.android.core.media.GeneratedImageSaver
import com.gkim.im.android.core.media.MediaPickerController
import com.gkim.im.android.core.media.MediaPickerControllerFactory
import com.gkim.im.android.core.model.AttachmentType
import com.gkim.im.android.core.model.MediaInput
import com.gkim.im.android.data.local.SessionStore
import com.gkim.im.android.data.remote.aigc.EncodedMediaPayload
import com.gkim.im.android.data.remote.aigc.RemoteAigcProviderClient
import com.gkim.im.android.data.remote.im.ChatAttachmentEncoder
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.ImBackendHttpClient
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.remote.im.deriveWebSocketUrl
import com.gkim.im.android.data.repository.AigcRepository
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.CompanionRosterRepository
import com.gkim.im.android.data.repository.CompanionTurnRepository
import com.gkim.im.android.data.repository.CardInteropRepository
import com.gkim.im.android.data.repository.DefaultCardInteropRepository
import com.gkim.im.android.data.repository.LiveCardInteropRepository
import com.gkim.im.android.data.repository.ContactsRepository
import com.gkim.im.android.data.repository.DefaultAigcRepository
import com.gkim.im.android.data.repository.DefaultCompanionRosterRepository
import com.gkim.im.android.data.repository.DefaultCompanionTurnRepository
import com.gkim.im.android.data.repository.DefaultFeedRepository
import com.gkim.im.android.data.repository.FeedRepository
import com.gkim.im.android.data.repository.InMemoryMessagingRepository
import com.gkim.im.android.data.repository.LiveContactsRepository
import com.gkim.im.android.data.repository.LiveMessagingRepository
import com.gkim.im.android.data.repository.MessagingIntegrationPhase
import com.gkim.im.android.data.repository.MessagingRepository
import com.gkim.im.android.data.repository.presetProviders
import com.gkim.im.android.data.repository.seedDrawPoolCharacters
import com.gkim.im.android.data.repository.seedConversations
import com.gkim.im.android.data.repository.seedPresetCharacters
import com.gkim.im.android.data.repository.seedPosts
import com.gkim.im.android.data.repository.seedPrompts
import com.gkim.im.android.core.rendering.MarkdownDocumentParser
import com.gkim.im.android.data.remote.realtime.RealtimeChatClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveImageSendValidationTest {
    companion object {
        private const val VALIDATION_TIMEOUT_MS = 45_000L
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val backendOrigin = LiveEndpointOverrides.backendOrigin()
    private val httpBaseUrl = LiveEndpointOverrides.httpBaseUrl()
    private val webSocketUrl = LiveEndpointOverrides.webSocketUrl()

    private lateinit var backendClient: ImBackendHttpClient
    private lateinit var counterpartGateway: RealtimeChatClient
    private lateinit var counterpartToken: String
    private lateinit var noxToken: String
    private lateinit var conversationId: String

    @Before
    fun setUp() = runBlocking {
        backendClient = ImBackendHttpClient(OkHttpClient.Builder().build())
        val noxSession = backendClient.issueDevSession(httpBaseUrl, "nox-dev")
        noxToken = noxSession.token
        conversationId = backendClient
            .loadBootstrap(httpBaseUrl, noxToken)
            .conversations
            .first { it.contact.externalId == "leo-vance" }
            .conversationId

        val counterpartSession = backendClient.issueDevSession(httpBaseUrl, "leo-vance")
        counterpartToken = counterpartSession.token
        counterpartGateway = RealtimeChatClient(
            OkHttpClient.Builder().build(),
            webSocketUrl,
        )
        counterpartGateway.connect(
            token = counterpartToken,
            endpointOverride = webSocketUrl,
        )
        waitFor(timeoutMs = VALIDATION_TIMEOUT_MS) { counterpartGateway.isConnected.value }
    }

    @After
    fun tearDown() {
        if (::counterpartGateway.isInitialized) {
            counterpartGateway.disconnect()
        }
    }

    @Test
    fun emulatorValidationCanSendImageMessageThroughLiveBackend() = runBlocking {
        val container = LiveImageValidationContainer(
            context = composeRule.activity,
            backendOrigin = backendOrigin,
        )
        setApp(container, fakeMediaPickerControllerFactory())
        waitFor(timeoutMs = VALIDATION_TIMEOUT_MS) {
            container.messagingRepository.integrationState.value.phase == MessagingIntegrationPhase.Ready
        }

        composeRule.waitUntil(VALIDATION_TIMEOUT_MS) { nodeExists("conversation-row-$conversationId") }
        composeRule.onNodeWithTag("conversation-row-$conversationId").performClick()
        composeRule.waitUntil(VALIDATION_TIMEOUT_MS) { nodeExists("chat-screen") }

        composeRule.onNodeWithTag("chat-plus-button").performClick()
        composeRule.waitUntil(VALIDATION_TIMEOUT_MS) { nodeExists("chat-secondary-menu") }
        composeRule.onNodeWithTag("chat-action-send-image-message").performClick()
        composeRule.waitUntil(VALIDATION_TIMEOUT_MS) { nodeExists("chat-chat-attachment-ready") }
        composeRule.onNodeWithTag("chat-send-button").performClick()

        val counterpartEvent = waitForEvent {
            it is ImGatewayEvent.MessageReceived && it.message.attachment != null
        } as ImGatewayEvent.MessageReceived

        composeRule.waitUntil(VALIDATION_TIMEOUT_MS) {
            nodeExists("chat-message-attachment-${counterpartEvent.message.id}")
        }

        val noxHistory = backendClient.loadHistory(httpBaseUrl, noxToken, conversationId)
        check(noxHistory.messages.any { it.id == counterpartEvent.message.id && it.attachment != null }) {
            "Sender history did not include the uploaded attachment message."
        }
        val leoHistory = backendClient.loadHistory(httpBaseUrl, counterpartToken, conversationId)
        check(leoHistory.messages.any { it.id == counterpartEvent.message.id && it.attachment != null }) {
            "Recipient history did not include the uploaded attachment message."
        }
    }

    private fun setApp(
        container: AppContainer,
        mediaPickerControllerFactory: MediaPickerControllerFactory,
    ) {
        composeRule.setContent {
            val appContainer = remember { mutableStateOf(container) }
            GkimRootApp(
                container = appContainer.value,
                mediaPickerControllerFactory = mediaPickerControllerFactory,
                initialAuthStart = RootAuthStart.Authenticated,
            )
        }
        composeRule.waitUntil(5_000) { nodeExists("bottom-nav") }
    }

    private fun fakeMediaPickerControllerFactory(): MediaPickerControllerFactory = { onMediaSelected ->
        MediaPickerController(
            pickImage = {
                onMediaSelected(
                    MediaInput(
                        type = AttachmentType.Image,
                        uri = Uri.parse("content://live-image-validation/fake-image"),
                    )
                )
            },
            pickVideo = { error("Video picker should not be used in image send validation") },
        )
    }

    private fun nodeExists(tag: String): Boolean = runCatching {
        composeRule.onNodeWithTag(tag).fetchSemanticsNode()
        true
    }.getOrDefault(false)

    private suspend fun waitForEvent(predicate: (ImGatewayEvent) -> Boolean): ImGatewayEvent =
        withTimeout(VALIDATION_TIMEOUT_MS) {
            counterpartGateway.events.first(predicate)
        }

    private suspend fun waitFor(timeoutMs: Long = 10_000, predicate: () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!predicate()) {
                kotlinx.coroutines.delay(100)
            }
        }
    }
}

private class LiveImageValidationContainer(
    context: Context,
    backendOrigin: String,
) : AppContainer {
    private val okHttpClient = OkHttpClient.Builder().build()
    override val preferencesStore = UiTestPreferencesStore(initialImBackendOrigin = backendOrigin)
    override val sessionStore: SessionStore = SessionStore(ApplicationProvider.getApplicationContext()).apply {
        clear()
        baseUrl = null
    }
    override val imBackendClient: ImBackendClient = ImBackendHttpClient(okHttpClient)
    override val realtimeChatClient: RealtimeChatClient =
        RealtimeChatClient(okHttpClient, deriveWebSocketUrl(backendOrigin))
    override val messagingRepository: MessagingRepository = LiveMessagingRepository(
        backendClient = imBackendClient,
        realtimeGateway = realtimeChatClient,
        sessionStore = sessionStore,
        preferencesStore = preferencesStore,
        fallbackRepository = InMemoryMessagingRepository(seedConversations),
        chatAttachmentEncoder = LiveValidationAttachmentEncoder(),
        dispatcher = Dispatchers.Main,
    )
    override val contactsRepository: ContactsRepository = LiveContactsRepository(
        messagingRepository = messagingRepository,
        preferencesStore = preferencesStore,
        dispatcher = Dispatchers.Main,
    )
    override val feedRepository: FeedRepository = DefaultFeedRepository(seedPosts, seedPrompts, Dispatchers.Main)
    override val companionRosterRepository: CompanionRosterRepository = DefaultCompanionRosterRepository(
        presetCharacters = seedPresetCharacters,
        drawPool = seedDrawPoolCharacters,
    )
    override val companionTurnRepository: CompanionTurnRepository = DefaultCompanionTurnRepository()
    override val cardInteropRepository: CardInteropRepository = DefaultCardInteropRepository(
        delegate = LiveCardInteropRepository(
            backendClient = imBackendClient,
            baseUrlProvider = { sessionStore.baseUrl },
            tokenProvider = { sessionStore.token },
        ),
    )
    private val secureStore = UiInMemorySecureStore().apply {
        putString("preset_provider_hunyuan_api_key", "ui-test-hunyuan-key")
        putString("preset_provider_tongyi_api_key", "ui-test-tongyi-key")
    }
    override val aigcRepository: AigcRepository = DefaultAigcRepository(
        presets = presetProviders,
        preferencesStore = preferencesStore,
        secureStore = secureStore,
        dispatcher = Dispatchers.Main,
        providerClients = mapOf<String, RemoteAigcProviderClient>(
            "hunyuan" to UiTestRemoteAigcProviderClient("hunyuan"),
            "tongyi" to UiTestRemoteAigcProviderClient("tongyi"),
        ),
        mediaInputEncoder = UiTestMediaInputEncoder(),
    )
    override val markdownParser: MarkdownDocumentParser = MarkdownDocumentParser()
    override val generatedImageSaver: GeneratedImageSaver = object : GeneratedImageSaver {
        override suspend fun saveImage(imageUrl: String, prompt: String): GeneratedImageSaveResult {
            return GeneratedImageSaveResult.Success(Uri.parse("content://live-image-validation/saved-image"))
        }
    }
}

private class LiveValidationAttachmentEncoder : ChatAttachmentEncoder {
    override suspend fun encode(attachment: com.gkim.im.android.core.model.MessageAttachment): EncodedMediaPayload {
        return EncodedMediaPayload(
            base64Data = "aGVsbG8taW1hZ2U=",
            mimeType = "image/png",
        )
    }
}
