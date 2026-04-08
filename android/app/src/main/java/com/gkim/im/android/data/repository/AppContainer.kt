package com.gkim.im.android.data.repository

import android.content.Context
import com.gkim.im.android.core.rendering.MarkdownDocumentParser
import com.gkim.im.android.core.security.AndroidSecureKeyValueStore
import com.gkim.im.android.core.security.SecureKeyValueStore
import com.gkim.im.android.data.local.AppDatabase
import com.gkim.im.android.data.local.AppPreferencesStore
import com.gkim.im.android.data.local.PreferencesStore
import com.gkim.im.android.data.remote.api.AigcService
import com.gkim.im.android.data.remote.api.FeedService
import com.gkim.im.android.data.remote.api.ServiceFactory
import com.gkim.im.android.data.remote.im.ImBackendHttpClient
import com.gkim.im.android.data.remote.realtime.RealtimeChatClient
import okhttp3.OkHttpClient

interface AppContainer {
    val messagingRepository: MessagingRepository
    val contactsRepository: ContactsRepository
    val feedRepository: FeedRepository
    val aigcRepository: AigcRepository
    val preferencesStore: PreferencesStore
    val markdownParser: MarkdownDocumentParser
    val realtimeChatClient: RealtimeChatClient
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val okHttpClient = OkHttpClient.Builder().build()
    @Suppress("unused")
    private val database = AppDatabase.create(context)
    override val preferencesStore: PreferencesStore = AppPreferencesStore(context)
    private val secureStore: SecureKeyValueStore = AndroidSecureKeyValueStore(context)
    private val retrofit = ServiceFactory.retrofit("https://api.example.com/", okHttpClient)
    @Suppress("unused")
    private val aigcService = retrofit.create(AigcService::class.java)
    @Suppress("unused")
    private val feedService = retrofit.create(FeedService::class.java)
    private val fallbackMessagingRepository = InMemoryMessagingRepository(seedConversations)
    private val imBackendClient = ImBackendHttpClient(okHttpClient)

    override val markdownParser = MarkdownDocumentParser()
    override val realtimeChatClient = RealtimeChatClient(okHttpClient, "wss://example.com/realtime")

    override val messagingRepository: MessagingRepository = LiveMessagingRepository(
        backendClient = imBackendClient,
        realtimeGateway = realtimeChatClient,
        preferencesStore = preferencesStore,
        fallbackRepository = fallbackMessagingRepository,
    )
    override val contactsRepository: ContactsRepository = DefaultContactsRepository(seedContacts, preferencesStore)
    override val feedRepository: FeedRepository = DefaultFeedRepository(seedPosts, seedPrompts)
    override val aigcRepository: AigcRepository = DefaultAigcRepository(
        presets = presetProviders,
        preferencesStore = preferencesStore,
        secureStore = secureStore,
    )
}
