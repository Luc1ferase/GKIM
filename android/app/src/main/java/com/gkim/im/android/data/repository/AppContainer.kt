package com.gkim.im.android.data.repository

import android.content.Context
import com.gkim.im.android.BuildConfig
import com.gkim.im.android.core.media.AndroidGeneratedImageSaver
import com.gkim.im.android.core.media.GeneratedImageSaver
import com.gkim.im.android.core.rendering.MarkdownDocumentParser
import com.gkim.im.android.core.security.AndroidSecureKeyValueStore
import com.gkim.im.android.core.security.SecureKeyValueStore
import com.gkim.im.android.data.local.AppDatabase
import com.gkim.im.android.data.local.AppPreferencesStore
import com.gkim.im.android.data.local.PreferencesStore
import com.gkim.im.android.data.local.SessionStore
import com.gkim.im.android.data.remote.aigc.AndroidMediaInputEncoder
import com.gkim.im.android.data.remote.aigc.HunyuanImageProviderClient
import com.gkim.im.android.data.remote.aigc.TongyiImageProviderClient
import com.gkim.im.android.data.remote.api.AigcService
import com.gkim.im.android.data.remote.api.FeedService
import com.gkim.im.android.data.remote.api.ServiceFactory
import com.gkim.im.android.data.remote.im.AndroidChatAttachmentEncoder
import com.gkim.im.android.data.remote.im.ChatAttachmentEncoder
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.ImBackendHttpClient
import com.gkim.im.android.data.remote.im.ImHttpEndpointResolver
import com.gkim.im.android.data.remote.realtime.RealtimeChatClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

interface AppContainer {
    val messagingRepository: MessagingRepository
    val contactsRepository: ContactsRepository
    val feedRepository: FeedRepository
    val companionRosterRepository: CompanionRosterRepository
    val companionTurnRepository: CompanionTurnRepository
    val aigcRepository: AigcRepository
    val preferencesStore: PreferencesStore
    val sessionStore: SessionStore
    val imBackendClient: ImBackendClient
    val markdownParser: MarkdownDocumentParser
    val realtimeChatClient: RealtimeChatClient
    val generatedImageSaver: GeneratedImageSaver
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val okHttpClient = OkHttpClient.Builder().build()
    @Suppress("unused")
    private val database = AppDatabase.create(context)
    override val preferencesStore: PreferencesStore = AppPreferencesStore(context)
    override val sessionStore: SessionStore = SessionStore(context)
    private val secureStore: SecureKeyValueStore = AndroidSecureKeyValueStore(context)
    private val retrofit = ServiceFactory.retrofit("https://api.example.com/", okHttpClient)
    @Suppress("unused")
    private val aigcService = retrofit.create(AigcService::class.java)
    @Suppress("unused")
    private val feedService = retrofit.create(FeedService::class.java)
    private val fallbackMessagingRepository = InMemoryMessagingRepository(seedConversations)
    private val imBackendClientImpl = ImBackendHttpClient(okHttpClient)
    private val mediaInputEncoder = AndroidMediaInputEncoder(context)
    private val chatAttachmentEncoder: ChatAttachmentEncoder = AndroidChatAttachmentEncoder(
        httpClient = okHttpClient,
        mediaInputEncoder = mediaInputEncoder,
    )
    override val imBackendClient: ImBackendClient = imBackendClientImpl

    override val markdownParser = MarkdownDocumentParser()
    override val realtimeChatClient = RealtimeChatClient(
        okHttpClient,
        ImHttpEndpointResolver.resolve(
            sessionBaseUrl = null,
            developerOverrideOrigin = null,
            shippedBackendOrigin = BuildConfig.IM_BACKEND_ORIGIN,
            allowDeveloperOverrides = false,
        ).webSocketUrl,
    )
    override val generatedImageSaver: GeneratedImageSaver = AndroidGeneratedImageSaver(
        context = context,
        httpClient = okHttpClient,
    )

    override val messagingRepository: MessagingRepository = LiveMessagingRepository(
        backendClient = imBackendClient,
        realtimeGateway = realtimeChatClient,
        sessionStore = sessionStore,
        preferencesStore = preferencesStore,
        fallbackRepository = fallbackMessagingRepository,
        chatAttachmentEncoder = chatAttachmentEncoder,
    )
    override val contactsRepository: ContactsRepository = LiveContactsRepository(
        messagingRepository = messagingRepository,
        preferencesStore = preferencesStore,
    )
    override val feedRepository: FeedRepository = DefaultFeedRepository(seedPosts, seedPrompts)
    override val companionRosterRepository: CompanionRosterRepository = BackendAwareCompanionRosterRepository(
        backendClient = imBackendClient,
        sessionStore = sessionStore,
        presetCharacters = seedPresetCharacters,
        drawPool = seedDrawPoolCharacters,
    )
    private val companionTurnScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override val companionTurnRepository: CompanionTurnRepository = LiveCompanionTurnRepository(
        backendClient = imBackendClient,
        gateway = realtimeChatClient,
        scope = companionTurnScope,
        baseUrlProvider = { sessionStore.baseUrl },
        tokenProvider = { sessionStore.token },
    )
    override val aigcRepository: AigcRepository = DefaultAigcRepository(
        presets = presetProviders,
        preferencesStore = preferencesStore,
        secureStore = secureStore,
        providerClients = mapOf(
            "hunyuan" to HunyuanImageProviderClient(httpClient = okHttpClient),
            "tongyi" to TongyiImageProviderClient(httpClient = okHttpClient),
        ),
        mediaInputEncoder = mediaInputEncoder,
    )
}
