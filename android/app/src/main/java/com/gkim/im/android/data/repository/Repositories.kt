package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.AigcProvider
import com.gkim.im.android.core.model.AigcTask
import com.gkim.im.android.core.model.AttachmentType
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.core.model.ContactSortMode
import com.gkim.im.android.core.model.Conversation
import com.gkim.im.android.core.model.CustomProviderConfig
import com.gkim.im.android.core.model.DraftAigcRequest
import com.gkim.im.android.core.model.FeedPost
import com.gkim.im.android.core.model.MediaInput
import com.gkim.im.android.core.model.MessageAttachment
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.PromptCategory
import com.gkim.im.android.core.model.TaskStatus
import com.gkim.im.android.core.model.WorkshopPrompt
import com.gkim.im.android.core.security.SecureKeyValueStore
import com.gkim.im.android.core.util.sortContacts
import com.gkim.im.android.data.local.PreferencesStore
import com.gkim.im.android.data.remote.im.ImBackendClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

enum class MessagingIntegrationPhase {
    Idle,
    Authenticating,
    Bootstrapping,
    RealtimeConnecting,
    Ready,
    Error,
}

data class MessagingIntegrationState(
    val phase: MessagingIntegrationPhase = MessagingIntegrationPhase.Idle,
    val activeUserExternalId: String? = null,
    val message: String? = null,
)

interface MessagingRepository {
    val conversations: StateFlow<List<Conversation>>
    val integrationState: StateFlow<MessagingIntegrationState>
    fun conversation(conversationId: String): Flow<Conversation?>
    fun ensureConversation(contact: Contact): Conversation
    fun ensureStudioRoom(): Conversation
    fun sendMessage(conversationId: String, body: String)
    fun appendAigcResult(conversationId: String, task: AigcTask)
    fun loadConversationHistory(conversationId: String)
}

class InMemoryMessagingRepository(seed: List<Conversation>) : MessagingRepository {
    private val conversationState = MutableStateFlow(seed)
    private val integrationStateValue = MutableStateFlow(
        MessagingIntegrationState(phase = MessagingIntegrationPhase.Ready)
    )
    override val conversations: StateFlow<List<Conversation>> = conversationState
    override val integrationState: StateFlow<MessagingIntegrationState> = integrationStateValue

    override fun conversation(conversationId: String): Flow<Conversation?> = conversationState.map { items ->
        items.firstOrNull { it.id == conversationId }
    }

    override fun ensureConversation(contact: Contact): Conversation {
        val existing = conversationState.value.firstOrNull { it.contactId == contact.id }
        if (existing != null) return existing
        val timestamp = Instant.now().toString()
        val created = Conversation(
            id = "room-${contact.id}",
            contactId = contact.id,
            contactName = contact.nickname,
            contactTitle = contact.title,
            avatarText = contact.avatarText,
            lastMessage = "Room created. Start with a text prompt or an AIGC action.",
            lastTimestamp = timestamp,
            unreadCount = 0,
            isOnline = contact.isOnline,
            messages = listOf(
                ChatMessage(
                    id = "system-${contact.id}",
                    direction = MessageDirection.System,
                    kind = MessageKind.Text,
                    body = "Room created. Start with a text prompt or an AIGC action.",
                    createdAt = timestamp,
                )
            ),
        )
        conversationState.value = listOf(created) + conversationState.value
        return created
    }

    override fun ensureStudioRoom(): Conversation {
        return ensureConversation(Contact("studio", "Studio Core", "AIGC Control", "SC", Instant.now().toString(), true))
    }

    override fun sendMessage(conversationId: String, body: String) {
        val timestamp = Instant.now().toString()
        conversationState.value = conversationState.value.map { conversation ->
            if (conversation.id != conversationId) return@map conversation
            conversation.copy(
                messages = conversation.messages + ChatMessage(
                    id = "out-$timestamp",
                    direction = MessageDirection.Outgoing,
                    kind = MessageKind.Text,
                    body = body,
                    createdAt = timestamp,
                ),
                lastMessage = body,
                lastTimestamp = timestamp,
            )
        }
    }

    override fun appendAigcResult(conversationId: String, task: AigcTask) {
        conversationState.value = conversationState.value.map { conversation ->
            if (conversation.id != conversationId) return@map conversation
            val body = "Generated ${task.mode.name} output with ${task.providerId}."
            conversation.copy(
                messages = conversation.messages + ChatMessage(
                    id = "aigc-${task.id}",
                    direction = MessageDirection.System,
                    kind = MessageKind.Aigc,
                    body = body,
                    createdAt = task.createdAt,
                    attachment = MessageAttachment(
                        type = if (task.mode == AigcMode.VideoToVideo) AttachmentType.Video else AttachmentType.Image,
                        preview = task.outputPreview,
                        prompt = task.prompt,
                        generationId = task.id,
                    ),
                ),
                lastMessage = body,
                lastTimestamp = task.createdAt,
            )
        }
    }

    override fun loadConversationHistory(conversationId: String) = Unit
}

interface ContactsRepository {
    val contacts: StateFlow<List<Contact>>
    val sortMode: StateFlow<ContactSortMode>
    val sortedContacts: StateFlow<List<Contact>>
    fun setSortMode(mode: ContactSortMode)
}

class DefaultContactsRepository(
    seed: List<Contact>,
    private val preferencesStore: PreferencesStore,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ContactsRepository {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    override val contacts = MutableStateFlow(seed)
    private val sortState = MutableStateFlow(ContactSortMode.Nickname)
    override val sortMode: StateFlow<ContactSortMode> = sortState
    override val sortedContacts: StateFlow<List<Contact>> = combine(contacts, sortMode) { items, mode ->
        sortContacts(items, mode)
    }.stateIn(scope, SharingStarted.Eagerly, sortContacts(seed, ContactSortMode.Nickname))

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            preferencesStore.contactSortMode.collect { sortState.value = it }
        }
    }

    override fun setSortMode(mode: ContactSortMode) {
        sortState.value = mode
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            preferencesStore.setContactSortMode(mode)
        }
    }
}

interface FeedRepository {
    val posts: StateFlow<List<FeedPost>>
    val prompts: StateFlow<List<WorkshopPrompt>>
    val promptCategory: StateFlow<PromptCategory>
    val promptQuery: StateFlow<String>
    val filteredPrompts: StateFlow<List<WorkshopPrompt>>
    fun setPromptCategory(category: PromptCategory)
    fun setPromptQuery(query: String)
}

class DefaultFeedRepository(
    seedPosts: List<FeedPost>,
    seedPrompts: List<WorkshopPrompt>,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : FeedRepository {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    override val posts = MutableStateFlow(seedPosts)
    override val prompts = MutableStateFlow(seedPrompts)
    override val promptCategory = MutableStateFlow(PromptCategory.All)
    override val promptQuery = MutableStateFlow("")
    override val filteredPrompts: StateFlow<List<WorkshopPrompt>> = combine(prompts, promptCategory, promptQuery) { items, category, query ->
        items.filter { prompt ->
            val matchesCategory = category == PromptCategory.All || prompt.category == category
            val matchesQuery = query.isBlank() || (prompt.title + " " + prompt.summary).contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(scope, SharingStarted.Eagerly, seedPrompts)

    override fun setPromptCategory(category: PromptCategory) {
        promptCategory.value = category
    }

    override fun setPromptQuery(query: String) {
        promptQuery.value = query
    }
}

interface AigcRepository {
    val providers: StateFlow<List<AigcProvider>>
    val activeProviderId: StateFlow<String>
    val customProvider: StateFlow<CustomProviderConfig>
    val history: StateFlow<List<AigcTask>>
    val draftRequest: StateFlow<DraftAigcRequest>
    fun setActiveProvider(id: String)
    fun updateCustomProvider(baseUrl: String? = null, model: String? = null, apiKey: String? = null)
    fun updateDraft(request: DraftAigcRequest)
    fun generate(mode: AigcMode, prompt: String, mediaInput: MediaInput? = null): AigcTask
}

class DefaultAigcRepository(
    presets: List<AigcProvider>,
    private val preferencesStore: PreferencesStore,
    private val secureStore: SecureKeyValueStore,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : AigcRepository {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    override val providers = MutableStateFlow(presets)
    private val activeProviderState = MutableStateFlow(presets.firstOrNull()?.id ?: "hunyuan")
    private val customProviderState = MutableStateFlow(CustomProviderConfig("https://api.example.com/v1", "", "gpt-image-1"))
    override val history = MutableStateFlow<List<AigcTask>>(emptyList())
    override val draftRequest = MutableStateFlow(DraftAigcRequest())
    override val activeProviderId: StateFlow<String> = activeProviderState
    override val customProvider: StateFlow<CustomProviderConfig> = customProviderState

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            preferencesStore.activeProviderId.collect { activeProviderState.value = it }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            preferencesStore.customBaseUrl.collect { base ->
                customProviderState.value = customProviderState.value.copy(baseUrl = base)
            }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            preferencesStore.customModel.collect { model ->
                customProviderState.value = customProviderState.value.copy(model = model)
            }
        }
        secureStore.getString("custom_api_key")?.let { apiKey -> customProviderState.value = customProviderState.value.copy(apiKey = apiKey) }
    }

    override fun setActiveProvider(id: String) {
        activeProviderState.value = id
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            preferencesStore.setActiveProviderId(id)
        }
    }

    override fun updateCustomProvider(baseUrl: String?, model: String?, apiKey: String?) {
        val next = customProviderState.value.copy(
            baseUrl = baseUrl ?: customProviderState.value.baseUrl,
            model = model ?: customProviderState.value.model,
            apiKey = apiKey ?: customProviderState.value.apiKey,
        )
        customProviderState.value = next
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            baseUrl?.let { preferencesStore.setCustomBaseUrl(it) }
            model?.let { preferencesStore.setCustomModel(it) }
            apiKey?.let { secureStore.putString("custom_api_key", it) }
        }
    }

    override fun updateDraft(request: DraftAigcRequest) {
        draftRequest.value = request
    }

    override fun generate(mode: AigcMode, prompt: String, mediaInput: MediaInput?): AigcTask {
        val providerId = activeProviderState.value
        val task = AigcTask(
            id = "$providerId-${System.currentTimeMillis()}",
            providerId = providerId,
            mode = mode,
            prompt = prompt,
            createdAt = Instant.now().toString(),
            status = TaskStatus.Succeeded,
            input = mediaInput,
            outputPreview = when (mode) {
                AigcMode.VideoToVideo -> "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=1200&q=80"
                AigcMode.ImageToImage -> "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=1200&q=80"
                AigcMode.TextToImage -> "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80"
            },
        )
        history.value = listOf(task) + history.value
        draftRequest.value = DraftAigcRequest(mode = mode, prompt = prompt, mediaInput = mediaInput)
        return task
    }
}

class LiveMessagingRepository(
    private val backendClient: ImBackendClient,
    private val realtimeGateway: com.gkim.im.android.data.remote.realtime.RealtimeGateway,
    private val preferencesStore: PreferencesStore,
    private val fallbackRepository: MessagingRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : MessagingRepository {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val conversationState = MutableStateFlow<List<Conversation>>(emptyList())
    private val integrationStateValue = MutableStateFlow(MessagingIntegrationState())
    private val loadedConversationIds = mutableSetOf<String>()

    private var activeToken: String? = null
    private var activeUserExternalId: String? = null
    private var activeHttpBaseUrl: String? = null
    private var activeWebSocketUrl: String? = null

    override val conversations: StateFlow<List<Conversation>> = conversationState
    override val integrationState: StateFlow<MessagingIntegrationState> = integrationStateValue

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            combine(
                preferencesStore.imHttpBaseUrl,
                preferencesStore.imWebSocketUrl,
                preferencesStore.imDevUserExternalId,
            ) { httpBaseUrl, webSocketUrl, devUserExternalId ->
                Triple(httpBaseUrl, webSocketUrl, devUserExternalId)
            }.collect { (httpBaseUrl, webSocketUrl, devUserExternalId) ->
                if (httpBaseUrl.isBlank() || webSocketUrl.isBlank() || devUserExternalId.isBlank()) {
                    integrationStateValue.value = MessagingIntegrationState(
                        phase = MessagingIntegrationPhase.Error,
                        activeUserExternalId = activeUserExternalId,
                        message = "IM validation config is incomplete or invalid.",
                    )
                } else {
                    try {
                        bootstrap(httpBaseUrl, webSocketUrl, devUserExternalId)
                    } catch (error: Throwable) {
                        if (error is CancellationException) throw error
                        reportError(error.message ?: "Failed to bootstrap live IM.")
                    }
                }
            }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            realtimeGateway.events.collect { handleRealtimeEvent(it) }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            realtimeGateway.lastFailure.collect { failure ->
                if (failure != null) {
                    integrationStateValue.value = integrationStateValue.value.copy(
                        phase = MessagingIntegrationPhase.Error,
                        message = failure,
                    )
                }
            }
        }
    }

    override fun conversation(conversationId: String): Flow<Conversation?> = conversationState.map { conversations ->
        conversations.firstOrNull { it.id == conversationId }
    }

    override fun ensureConversation(contact: Contact): Conversation {
        val existing = conversationState.value.firstOrNull { it.contactId == contact.id }
        if (existing != null) return existing

        val fallbackConversation = fallbackRepository.ensureConversation(contact)
        conversationState.value = listOf(fallbackConversation) + conversationState.value
        return fallbackConversation
    }

    override fun ensureStudioRoom(): Conversation {
        val existing = conversationState.value.firstOrNull { it.contactId == "studio" }
        if (existing != null) return existing

        val fallbackConversation = fallbackRepository.ensureStudioRoom()
        conversationState.value = listOf(fallbackConversation) + conversationState.value
        return fallbackConversation
    }

    override fun sendMessage(conversationId: String, body: String) {
        if (body.isBlank()) return
        val conversation = conversationState.value.firstOrNull { it.id == conversationId } ?: return
        val clientMessageId = "client-${System.currentTimeMillis()}"
        val sent = realtimeGateway.sendMessage(
            recipientExternalId = conversation.contactId,
            clientMessageId = clientMessageId,
            body = body,
        )
        if (!sent) {
            integrationStateValue.value = integrationStateValue.value.copy(
                phase = MessagingIntegrationPhase.Error,
                message = "Failed to send realtime message.",
            )
        }
    }

    override fun appendAigcResult(conversationId: String, task: AigcTask) {
        fallbackRepository.appendAigcResult(conversationId, task)
        val fallbackConversation = fallbackRepository.conversations.value.firstOrNull { it.id == conversationId } ?: return
        conversationState.value = conversationState.value.map { conversation ->
            if (conversation.id == conversationId) fallbackConversation else conversation
        }
    }

    override fun loadConversationHistory(conversationId: String) {
        if (loadedConversationIds.contains(conversationId)) return
        val token = activeToken ?: return
        val baseUrl = activeHttpBaseUrl ?: return

        scope.launch {
            try {
                val history = backendClient.loadHistory(
                    baseUrl = baseUrl,
                    token = token,
                    conversationId = conversationId,
                )
                loadedConversationIds.add(conversationId)
                val mappedMessages = history.toChatMessages(activeUserExternalId = activeUserExternalId.orEmpty())
                conversationState.value = conversationState.value.map { conversation ->
                    if (conversation.id != conversationId) {
                        conversation
                    } else {
                        val latest = mappedMessages.lastOrNull()
                        conversation.copy(
                            messages = mappedMessages,
                            lastMessage = latest?.body ?: conversation.lastMessage,
                            lastTimestamp = latest?.createdAt ?: conversation.lastTimestamp,
                        )
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                reportError(error.message ?: "Failed to load conversation history.")
            }
        }
    }

    private suspend fun bootstrap(
        httpBaseUrl: String,
        webSocketUrl: String,
        devUserExternalId: String,
    ) {
        realtimeGateway.disconnect()
        loadedConversationIds.clear()
        integrationStateValue.value = MessagingIntegrationState(
            phase = MessagingIntegrationPhase.Authenticating,
            activeUserExternalId = devUserExternalId,
        )
        val session = backendClient.issueDevSession(httpBaseUrl, devUserExternalId)
        activeToken = session.token
        activeUserExternalId = session.user.externalId
        activeHttpBaseUrl = httpBaseUrl
        activeWebSocketUrl = webSocketUrl

        integrationStateValue.value = MessagingIntegrationState(
            phase = MessagingIntegrationPhase.Bootstrapping,
            activeUserExternalId = session.user.externalId,
        )
        val bootstrap = backendClient.loadBootstrap(httpBaseUrl, session.token)
        conversationState.value = bootstrap.toBootstrapState(activeUserExternalId = session.user.externalId).conversations

        integrationStateValue.value = MessagingIntegrationState(
            phase = MessagingIntegrationPhase.RealtimeConnecting,
            activeUserExternalId = session.user.externalId,
        )
        realtimeGateway.connect(token = session.token, endpointOverride = webSocketUrl)
    }

    private fun handleRealtimeEvent(event: com.gkim.im.android.data.remote.im.ImGatewayEvent) {
        when (event) {
            is com.gkim.im.android.data.remote.im.ImGatewayEvent.SessionRegistered -> {
                integrationStateValue.value = integrationStateValue.value.copy(
                    phase = MessagingIntegrationPhase.Ready,
                    activeUserExternalId = event.user.externalId,
                    message = null,
                )
            }

            is com.gkim.im.android.data.remote.im.ImGatewayEvent.MessageSent -> {
                upsertMessage(
                    conversationId = event.conversationId,
                    message = event.message.toChatMessage(activeUserExternalId.orEmpty()),
                    unreadCount = null,
                )
            }

            is com.gkim.im.android.data.remote.im.ImGatewayEvent.MessageReceived -> {
                upsertMessage(
                    conversationId = event.conversationId,
                    message = event.message.toChatMessage(activeUserExternalId.orEmpty()),
                    unreadCount = event.unreadCount,
                )
            }

            is com.gkim.im.android.data.remote.im.ImGatewayEvent.MessageDelivered -> {
                conversationState.value = conversationState.value.map { conversation ->
                    if (conversation.id != event.conversationId) {
                        conversation
                    } else {
                        conversation.copy(
                            messages = conversation.messages.map { message ->
                                if (message.id == event.messageId) {
                                    message.copy(deliveredAt = event.deliveredAt)
                                } else {
                                    message
                                }
                            }
                        )
                    }
                }
            }

            is com.gkim.im.android.data.remote.im.ImGatewayEvent.MessageRead -> {
                conversationState.value = conversationState.value.map { conversation ->
                    if (conversation.id != event.conversationId) {
                        conversation
                    } else {
                        conversation.copy(
                            unreadCount = event.unreadCount,
                            messages = conversation.messages.map { message ->
                                if (message.id == event.messageId) {
                                    message.copy(readAt = event.readAt)
                                } else {
                                    message
                                }
                            }
                        )
                    }
                }
            }

            is com.gkim.im.android.data.remote.im.ImGatewayEvent.Error -> {
                integrationStateValue.value = integrationStateValue.value.copy(
                    phase = MessagingIntegrationPhase.Error,
                    message = event.message,
                )
            }

            is com.gkim.im.android.data.remote.im.ImGatewayEvent.Pong -> Unit
        }
    }

    private fun reportError(message: String) {
        integrationStateValue.value = integrationStateValue.value.copy(
            phase = MessagingIntegrationPhase.Error,
            message = message,
        )
    }

    private fun upsertMessage(
        conversationId: String,
        message: ChatMessage,
        unreadCount: Int?,
    ) {
        conversationState.value = conversationState.value.map { conversation ->
            if (conversation.id != conversationId) {
                conversation
            } else {
                val messages = conversation.messages.filterNot { it.id == message.id } + message
                conversation.copy(
                    unreadCount = unreadCount ?: conversation.unreadCount,
                    lastMessage = message.body,
                    lastTimestamp = message.createdAt,
                    messages = messages.sortedBy { it.createdAt },
                )
            }
        }
    }
}


