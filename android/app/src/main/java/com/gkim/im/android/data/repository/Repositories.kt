package com.gkim.im.android.data.repository
import com.gkim.im.android.BuildConfig
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
import com.gkim.im.android.core.model.PresetProviderConfig
import com.gkim.im.android.core.model.PromptCategory
import com.gkim.im.android.core.model.TaskStatus
import com.gkim.im.android.core.model.WorkshopPrompt
import com.gkim.im.android.core.security.SecureKeyValueStore
import com.gkim.im.android.core.util.sortContacts
import com.gkim.im.android.data.local.PreferencesStore
import com.gkim.im.android.data.local.RuntimeSessionStore
import com.gkim.im.android.data.remote.aigc.MediaInputEncoder
import com.gkim.im.android.data.remote.aigc.RemoteAigcGenerateRequest
import com.gkim.im.android.data.remote.aigc.RemoteAigcProviderClient
import com.gkim.im.android.data.remote.aigc.UnsupportedMediaInputEncoder
import com.gkim.im.android.data.remote.im.ChatAttachmentEncoder
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.ImHttpEndpointResolver
import com.gkim.im.android.data.remote.im.SendDirectImageMessageRequestDto
import com.gkim.im.android.data.remote.im.UnsupportedChatAttachmentEncoder
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
import kotlinx.coroutines.flow.first
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

private data class PendingRealtimeMessage(
    val conversationId: String,
    val recipientExternalId: String,
    val clientMessageId: String,
    val body: String,
)

interface MessagingRepository {
    val contacts: StateFlow<List<Contact>>
    val conversations: StateFlow<List<Conversation>>
    val integrationState: StateFlow<MessagingIntegrationState>
    fun conversation(conversationId: String): Flow<Conversation?>
    fun ensureConversation(contact: Contact): Conversation
    fun ensureStudioRoom(): Conversation
    fun sendMessage(conversationId: String, body: String, attachment: MessageAttachment? = null)
    fun appendAigcResult(conversationId: String, task: AigcTask)
    fun loadConversationHistory(conversationId: String)
    fun refreshBootstrap()
}

class InMemoryMessagingRepository(
    seed: List<Conversation>,
    contactsSeed: List<Contact> = seedContactsFromConversations(seed),
) : MessagingRepository {
    private val contactState = MutableStateFlow(contactsSeed)
    private val conversationState = MutableStateFlow(seed)
    private val integrationStateValue = MutableStateFlow(
        MessagingIntegrationState(phase = MessagingIntegrationPhase.Ready)
    )
    override val contacts: StateFlow<List<Contact>> = contactState
    override val conversations: StateFlow<List<Conversation>> = conversationState
    override val integrationState: StateFlow<MessagingIntegrationState> = integrationStateValue

    override fun conversation(conversationId: String): Flow<Conversation?> = conversationState.map { items ->
        items.firstOrNull { it.id == conversationId }
    }

    override fun ensureConversation(contact: Contact): Conversation {
        val existing = conversationState.value.firstOrNull { it.contactId == contact.id }
        if (existing != null) {
            val refreshed = existing.copy(
                contactName = contact.nickname,
                contactTitle = contact.title,
                avatarText = contact.avatarText,
                isOnline = contact.isOnline,
            )
            conversationState.value = conversationState.value.map { conversation ->
                if (conversation.id == existing.id) refreshed else conversation
            }
            return refreshed
        }
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

    override fun sendMessage(conversationId: String, body: String, attachment: MessageAttachment?) {
        if (body.isBlank() && attachment == null) return
        val timestamp = Instant.now().toString()
        conversationState.value = conversationState.value.map { conversation ->
            if (conversation.id != conversationId) return@map conversation
            val lastMessage = when {
                body.isNotBlank() -> body
                attachment?.type == AttachmentType.Image -> "Sent an image"
                attachment?.type == AttachmentType.Video -> "Sent a video"
                else -> conversation.lastMessage
            }
            conversation.copy(
                messages = conversation.messages + ChatMessage(
                    id = "out-$timestamp",
                    direction = MessageDirection.Outgoing,
                    kind = MessageKind.Text,
                    body = body,
                    createdAt = timestamp,
                    attachment = attachment,
                ),
                lastMessage = lastMessage,
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
                        preview = task.outputPreview ?: "",
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

    override fun refreshBootstrap() = Unit
}

private fun seedContactsFromConversations(seed: List<Conversation>): List<Contact> =
    seed.map { conversation ->
        Contact(
            id = conversation.contactId,
            nickname = conversation.contactName,
            title = conversation.contactTitle,
            avatarText = conversation.avatarText,
            addedAt = conversation.lastTimestamp,
            isOnline = conversation.isOnline,
        )
    }.distinctBy { it.id }

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
    val presetProviderConfigs: StateFlow<Map<String, PresetProviderConfig>>
    val history: StateFlow<List<AigcTask>>
    val draftRequest: StateFlow<DraftAigcRequest>
    fun setActiveProvider(id: String)
    fun updateCustomProvider(baseUrl: String? = null, model: String? = null, apiKey: String? = null)
    fun updatePresetProviderConfig(providerId: String, model: String? = null, apiKey: String? = null)
    fun updateDraft(request: DraftAigcRequest)
    suspend fun generate(mode: AigcMode, prompt: String, mediaInput: MediaInput? = null): AigcTask
}

class DefaultAigcRepository(
    presets: List<AigcProvider>,
    private val preferencesStore: PreferencesStore,
    private val secureStore: SecureKeyValueStore,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val providerClients: Map<String, RemoteAigcProviderClient> = emptyMap(),
    private val mediaInputEncoder: MediaInputEncoder = UnsupportedMediaInputEncoder(),
) : AigcRepository {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val providerCatalog = presets
    private val presetDefaults = providerCatalog.filter { it.preset }.associateBy { it.id }
    override val providers = MutableStateFlow(providerCatalog)
    private val activeProviderState = MutableStateFlow(providerCatalog.firstOrNull()?.id ?: "hunyuan")
    private val customProviderState = MutableStateFlow(CustomProviderConfig("https://api.example.com/v1", "", "gpt-image-1"))
    private val presetProviderConfigState = MutableStateFlow(
        presetDefaults.mapValues { (_, provider) ->
            PresetProviderConfig(
                model = provider.model,
                apiKey = "",
            )
        }
    )
    override val history = MutableStateFlow<List<AigcTask>>(emptyList())
    override val draftRequest = MutableStateFlow(DraftAigcRequest())
    override val activeProviderId: StateFlow<String> = activeProviderState
    override val customProvider: StateFlow<CustomProviderConfig> = customProviderState
    override val presetProviderConfigs: StateFlow<Map<String, PresetProviderConfig>> = presetProviderConfigState

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
        presetDefaults.forEach { (providerId, provider) ->
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                preferencesStore.presetProviderModel(providerId).collect { model ->
                    val resolvedModel = model?.takeIf { it.isNotBlank() } ?: provider.model
                    updatePresetProviderState(providerId) { current ->
                        current.copy(model = resolvedModel)
                    }
                }
            }
            val secureKey = presetProviderApiKeyKey(providerId)
            secureStore.getString(secureKey)?.let { apiKey ->
                updatePresetProviderState(providerId) { current ->
                    current.copy(apiKey = apiKey)
                }
            }
        }
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

    override fun updatePresetProviderConfig(providerId: String, model: String?, apiKey: String?) {
        val defaultProvider = presetDefaults[providerId] ?: return
        val current = presetProviderConfigState.value[providerId]
            ?: PresetProviderConfig(model = defaultProvider.model, apiKey = "")
        val resolvedModel = model?.takeIf { it.isNotBlank() } ?: current.model
        val next = current.copy(
            model = resolvedModel,
            apiKey = apiKey ?: current.apiKey,
        )
        presetProviderConfigState.value = presetProviderConfigState.value + (providerId to next)
        providers.value = applyPresetProviderConfigs(providerCatalog, presetProviderConfigState.value)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            model?.let { preferencesStore.setPresetProviderModel(providerId, resolvedModel) }
            apiKey?.let { secureStore.putString(presetProviderApiKeyKey(providerId), it) }
        }
    }

    override fun updateDraft(request: DraftAigcRequest) {
        draftRequest.value = request
    }

    override suspend fun generate(mode: AigcMode, prompt: String, mediaInput: MediaInput?): AigcTask {
        val provider = providers.value.firstOrNull { it.id == activeProviderState.value }
            ?: return createTerminalTask(
                providerId = "unknown",
                model = "",
                mode = mode,
                prompt = prompt,
                mediaInput = mediaInput,
                status = TaskStatus.Failed,
                errorMessage = "No active AIGC provider is configured.",
            )
        draftRequest.value = DraftAigcRequest(mode = mode, prompt = prompt, mediaInput = mediaInput)

        val requiredInputType = requiredInputTypeFor(mode)
        if (requiredInputType != null && mediaInput == null) {
            return createTerminalTask(
                providerId = provider.id,
                model = provider.model,
                mode = mode,
                prompt = prompt,
                mediaInput = mediaInput,
                status = TaskStatus.Failed,
                errorMessage = when (mode) {
                    AigcMode.ImageToImage -> "Select a source image before running image-to-image."
                    AigcMode.VideoToVideo -> "Select a source video before running video-to-video."
                    AigcMode.TextToImage -> "Generation input is missing."
                },
            )
        }

        if (requiredInputType != null && mediaInput?.type != requiredInputType) {
            return createTerminalTask(
                providerId = provider.id,
                model = provider.model,
                mode = mode,
                prompt = prompt,
                mediaInput = mediaInput,
                status = TaskStatus.Failed,
                errorMessage = when (requiredInputType) {
                    AttachmentType.Image -> "Image-to-image requires an image source."
                    AttachmentType.Video -> "Video-to-video requires a video source."
                },
            )
        }

        if (mode !in provider.capabilities) {
            return createTerminalTask(
                providerId = provider.id,
                model = provider.model,
                mode = mode,
                prompt = prompt,
                mediaInput = mediaInput,
                status = TaskStatus.Failed,
                errorMessage = "${provider.label} does not support ${mode.name}.",
            )
        }

        val apiKey = resolveApiKey(provider.id)
        if (apiKey.isBlank()) {
            return createTerminalTask(
                providerId = provider.id,
                model = provider.model,
                mode = mode,
                prompt = prompt,
                mediaInput = mediaInput,
                status = TaskStatus.Failed,
                errorMessage = "${provider.label} API key is required before generation can start.",
            )
        }

        val client = providerClients[provider.id]
            ?: return createTerminalTask(
                providerId = provider.id,
                model = provider.model,
                mode = mode,
                prompt = prompt,
                mediaInput = mediaInput,
                status = TaskStatus.Failed,
                errorMessage = "${provider.label} is not wired for live generation in this build.",
            )

        val queuedTask = AigcTask(
            id = "${provider.id}-${System.currentTimeMillis()}",
            providerId = provider.id,
            model = provider.model,
            mode = mode,
            prompt = prompt,
            createdAt = Instant.now().toString(),
            status = TaskStatus.Queued,
            input = mediaInput,
        )
        history.value = listOf(queuedTask) + history.value

        return try {
            val encodedMedia = mediaInput?.let { mediaInputEncoder.encode(it) }
            val result = client.generate(
                RemoteAigcGenerateRequest(
                    model = provider.model,
                    prompt = prompt,
                    apiKey = apiKey,
                    imageBase64 = encodedMedia?.base64Data,
                    imageMimeType = encodedMedia?.mimeType,
                )
            )
            val succeededTask = queuedTask.copy(
                remoteId = result.remoteId,
                status = TaskStatus.Succeeded,
                outputPreview = result.outputUrl,
            )
            replaceTask(succeededTask)
            succeededTask
        } catch (error: Throwable) {
            val failedTask = queuedTask.copy(
                status = TaskStatus.Failed,
                errorMessage = error.message ?: "Generation failed.",
                outputPreview = null,
            )
            replaceTask(failedTask)
            failedTask
        }
    }

    private fun updatePresetProviderState(
        providerId: String,
        transform: (PresetProviderConfig) -> PresetProviderConfig,
    ) {
        val defaultProvider = presetDefaults[providerId] ?: return
        val current = presetProviderConfigState.value[providerId]
            ?: PresetProviderConfig(model = defaultProvider.model, apiKey = "")
        presetProviderConfigState.value = presetProviderConfigState.value + (providerId to transform(current))
        providers.value = applyPresetProviderConfigs(providerCatalog, presetProviderConfigState.value)
    }

    private fun applyPresetProviderConfigs(
        providers: List<AigcProvider>,
        configs: Map<String, PresetProviderConfig>,
    ): List<AigcProvider> = providers.map { provider ->
        if (!provider.preset) {
            provider
        } else {
            val config = configs[provider.id]
            provider.copy(model = config?.model ?: provider.model)
        }
    }

    private fun presetProviderApiKeyKey(providerId: String): String = "preset_provider_${providerId}_api_key"

    private fun resolveApiKey(providerId: String): String = when (providerId) {
        "custom" -> customProviderState.value.apiKey
        else -> presetProviderConfigState.value[providerId]?.apiKey.orEmpty()
    }

    private fun createTerminalTask(
        providerId: String,
        model: String,
        mode: AigcMode,
        prompt: String,
        mediaInput: MediaInput?,
        status: TaskStatus,
        errorMessage: String,
    ): AigcTask {
        val task = AigcTask(
            id = "$providerId-${System.currentTimeMillis()}",
            providerId = providerId,
            model = model,
            mode = mode,
            prompt = prompt,
            createdAt = Instant.now().toString(),
            status = status,
            input = mediaInput,
            errorMessage = errorMessage,
        )
        history.value = listOf(task) + history.value
        return task
    }

    private fun replaceTask(task: AigcTask) {
        history.value = history.value.map { existing ->
            if (existing.id == task.id) task else existing
        }
    }
}

class LiveContactsRepository(
    messagingRepository: MessagingRepository,
    private val preferencesStore: PreferencesStore,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ContactsRepository {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    override val contacts: StateFlow<List<Contact>> = messagingRepository.contacts
    private val sortState = MutableStateFlow(ContactSortMode.Nickname)
    override val sortMode: StateFlow<ContactSortMode> = sortState
    override val sortedContacts: StateFlow<List<Contact>> = combine(contacts, sortMode) { items, mode ->
        sortContacts(items, mode)
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        sortContacts(messagingRepository.contacts.value, ContactSortMode.Nickname),
    )

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

class LiveMessagingRepository(
    private val backendClient: ImBackendClient,
    private val realtimeGateway: com.gkim.im.android.data.remote.realtime.RealtimeGateway,
    private val sessionStore: RuntimeSessionStore,
    private val preferencesStore: PreferencesStore,
    private val fallbackRepository: MessagingRepository,
    private val chatAttachmentEncoder: ChatAttachmentEncoder = UnsupportedChatAttachmentEncoder(),
    private val shippedBackendOrigin: String = BuildConfig.IM_BACKEND_ORIGIN,
    private val allowDeveloperOverrides: Boolean = BuildConfig.DEBUG,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : MessagingRepository {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val contactState = MutableStateFlow<List<Contact>>(emptyList())
    private val conversationState = MutableStateFlow<List<Conversation>>(emptyList())
    private val integrationStateValue = MutableStateFlow(MessagingIntegrationState())
    private val loadedConversationIds = mutableSetOf<String>()
    private val loadingConversationIds = mutableSetOf<String>()
    private val backendConversationIds = mutableSetOf<String>()
    private val pendingHistoryConversationIds = mutableSetOf<String>()
    private val pendingRealtimeMessages = mutableListOf<PendingRealtimeMessage>()

    private var activeToken: String? = null
    private var activeUserExternalId: String? = null
    private var activeHttpBaseUrl: String? = null
    private var activeWebSocketUrl: String? = null
    private var hasObservedRealtimeSessionRegistration = false

    override val contacts: StateFlow<List<Contact>> = contactState
    override val conversations: StateFlow<List<Conversation>> = conversationState
    override val integrationState: StateFlow<MessagingIntegrationState> = integrationStateValue

    init {
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
        if (allowDeveloperOverrides) {
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                combine(
                    preferencesStore.imBackendOrigin,
                    preferencesStore.imDevUserExternalId,
                ) { backendOrigin, devUserExternalId ->
                    backendOrigin to devUserExternalId
                }.collect { (backendOrigin, devUserExternalId) ->
                    if (sessionStore.hasSession) return@collect
                    try {
                        refreshBootstrapInternal(
                            backendOrigin = backendOrigin,
                            devUserExternalId = devUserExternalId,
                        )
                    } catch (error: Throwable) {
                        if (error is CancellationException) throw error
                        reportError(error.message ?: "Failed to sync messages.")
                    }
                }
            }
        }
        if (sessionStore.hasSession) {
            refreshBootstrap()
        } else if (!allowDeveloperOverrides) {
            clearRuntimeState()
        }
    }

    override fun conversation(conversationId: String): Flow<Conversation?> = conversationState.map { conversations ->
        conversations.firstOrNull { it.id == conversationId || it.contactId == conversationId }
    }

    override fun ensureConversation(contact: Contact): Conversation {
        val existing = conversationState.value.firstOrNull { it.contactId == contact.id }
        if (existing != null) {
            val refreshed = existing.copy(
                contactName = contact.nickname,
                contactTitle = contact.title,
                avatarText = contact.avatarText,
                isOnline = contact.isOnline,
            )
            conversationState.value = conversationState.value.map { conversation ->
                if (conversation.id == existing.id) refreshed else conversation
            }
            contactState.value = contactState.value.map { existingContact ->
                if (existingContact.id == contact.id) contact else existingContact
            }
            return refreshed
        }

        val timestamp = Instant.now().toString()
        val placeholderConversation = Conversation(
            id = contact.id,
            contactId = contact.id,
            contactName = contact.nickname,
            contactTitle = contact.title,
            avatarText = contact.avatarText,
            lastMessage = "",
            lastTimestamp = timestamp,
            unreadCount = 0,
            isOnline = contact.isOnline,
            messages = emptyList(),
        )
        conversationState.value = listOf(placeholderConversation) + conversationState.value
        if (contactState.value.none { existingContact -> existingContact.id == contact.id }) {
            contactState.value = listOf(contact) + contactState.value
        }
        return placeholderConversation
    }

    override fun ensureStudioRoom(): Conversation {
        val existing = conversationState.value.firstOrNull { it.contactId == "studio" }
        if (existing != null) return existing

        val fallbackConversation = fallbackRepository.ensureStudioRoom()
        conversationState.value = listOf(fallbackConversation) + conversationState.value
        return fallbackConversation
    }

    override fun sendMessage(conversationId: String, body: String, attachment: MessageAttachment?) {
        if (body.isBlank() && attachment == null) return
        val conversation = conversationState.value.firstOrNull {
            it.id == conversationId || it.contactId == conversationId
        } ?: return
        val isBackendConversation = backendConversationIds.contains(conversation.id)
        if (attachment != null) {
            if (!isBackendConversation) {
                upsertLocalOutgoingMessage(conversation.id, body, attachment)
            } else {
                sendBackendAttachmentMessage(conversation, body, attachment)
            }
            return
        }
        if (!isBackendConversation) {
            upsertLocalOutgoingTextMessage(conversation.id, body)
        }
        val pendingMessage = PendingRealtimeMessage(
            conversationId = conversation.id,
            recipientExternalId = conversation.contactId,
            clientMessageId = "client-${System.currentTimeMillis()}",
            body = body,
        )
        if (!trySendRealtimeMessage(pendingMessage)) {
            queueRealtimeMessageForRetry(pendingMessage)
            reconnectRealtimeForPendingMessages()
        } else if (!isBackendConversation) {
            refreshBootstrap()
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
        val resolvedConversationId = conversationState.value.firstOrNull {
            it.id == conversationId || it.contactId == conversationId
        }?.id ?: conversationId
        if (loadedConversationIds.contains(resolvedConversationId) || loadingConversationIds.contains(resolvedConversationId)) return
        val token = activeToken
        val baseUrl = activeHttpBaseUrl
        if (token == null || baseUrl == null || !backendConversationIds.contains(resolvedConversationId)) {
            pendingHistoryConversationIds.add(resolvedConversationId)
            return
        }

        loadConversationHistoryNow(
            conversationId = resolvedConversationId,
            baseUrl = baseUrl,
            token = token,
        )
    }

    override fun refreshBootstrap() {
        scope.launch {
            try {
                refreshBootstrapInternal(
                    backendOrigin = preferencesStore.imBackendOrigin.first(),
                    devUserExternalId = preferencesStore.imDevUserExternalId.first(),
                )
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                reportError(error.message ?: "Failed to sync messages.")
            }
        }
    }

    private suspend fun refreshBootstrapInternal(
        backendOrigin: String,
        devUserExternalId: String,
    ) {
        val storedToken = sessionStore.token?.takeIf { it.isNotBlank() }
        val resolvedEndpoint = ImHttpEndpointResolver.resolve(
            sessionBaseUrl = sessionStore.baseUrl,
            developerOverrideOrigin = backendOrigin,
            shippedBackendOrigin = shippedBackendOrigin,
            allowDeveloperOverrides = allowDeveloperOverrides,
        )
        if (storedToken != null) {
            if (
                resolvedEndpoint.httpBaseUrl.isBlank() ||
                resolvedEndpoint.webSocketUrl.isBlank()
            ) {
                reportError("Stored session is missing a valid server address.")
                return
            }
            bootstrapAuthenticatedSession(
                httpBaseUrl = resolvedEndpoint.httpBaseUrl,
                webSocketUrl = resolvedEndpoint.webSocketUrl,
                token = storedToken,
                sessionUserExternalId = sessionStore.username?.takeIf { it.isNotBlank() },
            )
            return
        }

        if (!allowDeveloperOverrides) {
            clearRuntimeState()
            return
        }

        val hasAnyValidationInput = backendOrigin.isNotBlank() || devUserExternalId.isNotBlank()
        if (!hasAnyValidationInput) {
            clearRuntimeState()
            return
        }

        if (
            resolvedEndpoint.httpBaseUrl.isBlank() ||
            resolvedEndpoint.webSocketUrl.isBlank() ||
            devUserExternalId.isBlank()
        ) {
            integrationStateValue.value = MessagingIntegrationState(
                phase = MessagingIntegrationPhase.Error,
                activeUserExternalId = activeUserExternalId,
                message = "Connection settings are incomplete or invalid.",
            )
            return
        }

        bootstrapDevelopmentSession(
            httpBaseUrl = resolvedEndpoint.httpBaseUrl,
            webSocketUrl = resolvedEndpoint.webSocketUrl,
            devUserExternalId = devUserExternalId,
        )
    }

    private fun loadConversationHistoryNow(
        conversationId: String,
        baseUrl: String,
        token: String,
        forceReload: Boolean = false,
    ) {
        if (!loadingConversationIds.add(conversationId)) return
        if (forceReload) {
            loadedConversationIds.remove(conversationId)
        }
        pendingHistoryConversationIds.remove(conversationId)
        scope.launch {
            try {
                val history = backendClient.loadHistory(
                    baseUrl = baseUrl,
                    token = token,
                    conversationId = conversationId,
                )
                loadedConversationIds.add(conversationId)
                val mappedMessages = history.toChatMessages(
                    activeUserExternalId = activeUserExternalId.orEmpty(),
                    backendBaseUrl = baseUrl,
                    authToken = token,
                )
                conversationState.value = conversationState.value.map { conversation ->
                    if (conversation.id != conversationId) {
                        conversation
                    } else {
                        val latest = mappedMessages.lastOrNull()
                        conversation.copy(
                            messages = mappedMessages,
                            lastMessage = latest?.let(::conversationPreviewText) ?: conversation.lastMessage,
                            lastTimestamp = latest?.createdAt ?: conversation.lastTimestamp,
                        )
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                reportError(error.message ?: "Failed to load conversation history.")
            } finally {
                loadingConversationIds.remove(conversationId)
            }
        }
    }

    private suspend fun bootstrapDevelopmentSession(
        httpBaseUrl: String,
        webSocketUrl: String,
        devUserExternalId: String,
    ) {
        resetRuntimeForBootstrap()
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
        applyBootstrapState(
            bootstrapState = bootstrap.toBootstrapState(
                activeUserExternalId = session.user.externalId,
                backendBaseUrl = httpBaseUrl,
                authToken = session.token,
            ),
            resolvedUserExternalId = session.user.externalId,
        )
        flushPendingHistoryLoads()

        integrationStateValue.value = MessagingIntegrationState(
            phase = MessagingIntegrationPhase.RealtimeConnecting,
            activeUserExternalId = session.user.externalId,
        )
        realtimeGateway.connect(token = session.token, endpointOverride = webSocketUrl)
    }

    private suspend fun bootstrapAuthenticatedSession(
        httpBaseUrl: String,
        webSocketUrl: String,
        token: String,
        sessionUserExternalId: String?,
    ) {
        resetRuntimeForBootstrap()
        integrationStateValue.value = MessagingIntegrationState(
            phase = MessagingIntegrationPhase.Bootstrapping,
            activeUserExternalId = sessionUserExternalId,
        )
        activeToken = token
        activeUserExternalId = sessionUserExternalId
        activeHttpBaseUrl = httpBaseUrl
        activeWebSocketUrl = webSocketUrl

        val bootstrap = backendClient.loadBootstrap(httpBaseUrl, token)
        applyBootstrapState(
            bootstrapState = bootstrap.toBootstrapState(
                activeUserExternalId = bootstrap.user.externalId,
                backendBaseUrl = httpBaseUrl,
                authToken = token,
            ),
            resolvedUserExternalId = bootstrap.user.externalId,
        )
        flushPendingHistoryLoads()

        integrationStateValue.value = MessagingIntegrationState(
            phase = MessagingIntegrationPhase.RealtimeConnecting,
            activeUserExternalId = bootstrap.user.externalId,
        )
        realtimeGateway.connect(token = token, endpointOverride = webSocketUrl)
    }

    private fun resetRuntimeForBootstrap() {
        realtimeGateway.disconnect()
        loadedConversationIds.clear()
        loadingConversationIds.clear()
        backendConversationIds.clear()
        pendingRealtimeMessages.clear()
        hasObservedRealtimeSessionRegistration = false
        activeToken = null
        activeUserExternalId = null
        activeHttpBaseUrl = null
        activeWebSocketUrl = null
        conversationState.value = emptyList()
        contactState.value = emptyList()
    }

    private fun applyBootstrapState(
        bootstrapState: com.gkim.im.android.data.remote.im.ImBootstrapState,
        resolvedUserExternalId: String,
        preserveLoadedMessages: Boolean = false,
    ) {
        activeUserExternalId = resolvedUserExternalId
        contactState.value = bootstrapState.contacts
        val existingConversations = conversationState.value.associateBy { it.id }
        conversationState.value = bootstrapState.conversations.map { incoming ->
            val existing = existingConversations[incoming.id]
            if (!preserveLoadedMessages || existing == null || !loadedConversationIds.contains(incoming.id)) {
                incoming
            } else {
                incoming.copy(messages = existing.messages)
            }
        }
        backendConversationIds.clear()
        backendConversationIds += conversationState.value.map { it.id }
    }

    private fun flushPendingHistoryLoads() {
        val token = activeToken ?: return
        val baseUrl = activeHttpBaseUrl ?: return
        pendingHistoryConversationIds
            .filter { conversationId ->
                backendConversationIds.contains(conversationId) &&
                    !loadedConversationIds.contains(conversationId)
            }
            .forEach { conversationId ->
                loadConversationHistoryNow(
                    conversationId = conversationId,
                    baseUrl = baseUrl,
                    token = token,
                )
            }
    }

    private fun handleRealtimeEvent(event: com.gkim.im.android.data.remote.im.ImGatewayEvent) {
        when (event) {
            is com.gkim.im.android.data.remote.im.ImGatewayEvent.SessionRegistered -> {
                val needsResync = hasObservedRealtimeSessionRegistration
                hasObservedRealtimeSessionRegistration = true
                integrationStateValue.value = integrationStateValue.value.copy(
                    phase = MessagingIntegrationPhase.Ready,
                    activeUserExternalId = event.user.externalId,
                    message = null,
                )
                flushPendingRealtimeMessages()
                if (needsResync) {
                    resyncAfterReconnect()
                }
            }

            is com.gkim.im.android.data.remote.im.ImGatewayEvent.MessageSent -> {
                upsertMessage(
                    conversationId = event.conversationId,
                    message = event.message.toChatMessage(
                        activeUserExternalId = activeUserExternalId.orEmpty(),
                        backendBaseUrl = activeHttpBaseUrl,
                        authToken = activeToken,
                    ),
                    unreadCount = null,
                )
            }

            is com.gkim.im.android.data.remote.im.ImGatewayEvent.MessageReceived -> {
                upsertMessage(
                    conversationId = event.conversationId,
                    message = event.message.toChatMessage(
                        activeUserExternalId = activeUserExternalId.orEmpty(),
                        backendBaseUrl = activeHttpBaseUrl,
                        authToken = activeToken,
                    ),
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
            is com.gkim.im.android.data.remote.im.ImGatewayEvent.FriendRequestReceived -> Unit // TODO: handle in friend request UI
            is com.gkim.im.android.data.remote.im.ImGatewayEvent.FriendRequestAccepted -> Unit // TODO: handle in friend request UI
            is com.gkim.im.android.data.remote.im.ImGatewayEvent.FriendRequestRejected -> Unit // TODO: handle in friend request UI
            is com.gkim.im.android.data.remote.im.ImGatewayEvent.CompanionTurnStarted,
            is com.gkim.im.android.data.remote.im.ImGatewayEvent.CompanionTurnDelta,
            is com.gkim.im.android.data.remote.im.ImGatewayEvent.CompanionTurnCompleted,
            is com.gkim.im.android.data.remote.im.ImGatewayEvent.CompanionTurnFailed,
            is com.gkim.im.android.data.remote.im.ImGatewayEvent.CompanionTurnBlocked -> Unit
        }
    }

    private fun reportError(message: String) {
        integrationStateValue.value = integrationStateValue.value.copy(
            phase = MessagingIntegrationPhase.Error,
            message = message,
        )
    }

    private fun sendBackendAttachmentMessage(
        conversation: Conversation,
        body: String,
        attachment: MessageAttachment,
    ) {
        val token = activeToken
        val baseUrl = activeHttpBaseUrl
        if (token == null || baseUrl == null) {
            reportError("Image message delivery failed.")
            return
        }
        scope.launch {
            try {
                val encoded = chatAttachmentEncoder.encode(attachment)
                val result = backendClient.sendDirectImageMessage(
                    baseUrl = baseUrl,
                    token = token,
                    request = SendDirectImageMessageRequestDto(
                        recipientExternalId = conversation.contactId,
                        clientMessageId = "client-image-${System.currentTimeMillis()}",
                        body = body,
                        contentType = encoded.mimeType,
                        imageBase64 = encoded.base64Data,
                    ),
                )
                upsertMessage(
                    conversationId = result.conversationId,
                    message = result.message.toChatMessage(
                        activeUserExternalId = activeUserExternalId.orEmpty(),
                        backendBaseUrl = baseUrl,
                        authToken = token,
                    ),
                    unreadCount = null,
                )
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                reportError(error.message ?: "Image message delivery failed.")
            }
        }
    }

    private fun resyncAfterReconnect() {
        val token = activeToken ?: return
        val baseUrl = activeHttpBaseUrl ?: return
        scope.launch {
            try {
                val bootstrap = backendClient.loadBootstrap(baseUrl, token)
                applyBootstrapState(
                    bootstrapState = bootstrap.toBootstrapState(
                        activeUserExternalId = bootstrap.user.externalId,
                        backendBaseUrl = baseUrl,
                        authToken = token,
                    ),
                    resolvedUserExternalId = bootstrap.user.externalId,
                    preserveLoadedMessages = true,
                )
                loadedConversationIds.toList().forEach { conversationId ->
                    loadConversationHistoryNow(
                        conversationId = conversationId,
                        baseUrl = baseUrl,
                        token = token,
                        forceReload = true,
                    )
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                reportError(error.message ?: "Failed to resync messages after reconnect.")
            }
        }
    }

    private fun trySendRealtimeMessage(message: PendingRealtimeMessage): Boolean =
        realtimeGateway.sendMessage(
            recipientExternalId = message.recipientExternalId,
            clientMessageId = message.clientMessageId,
            body = message.body,
        )

    private fun queueRealtimeMessageForRetry(message: PendingRealtimeMessage) {
        pendingRealtimeMessages.removeAll { pending -> pending.clientMessageId == message.clientMessageId }
        pendingRealtimeMessages += message
    }

    private fun reconnectRealtimeForPendingMessages() {
        val token = activeToken
        val webSocketUrl = activeWebSocketUrl
        if (token == null || webSocketUrl == null) {
            reportError("Message delivery failed.")
            return
        }
        integrationStateValue.value = integrationStateValue.value.copy(
            phase = MessagingIntegrationPhase.RealtimeConnecting,
            message = "Connection interrupted. Reconnecting...",
        )
        realtimeGateway.disconnect()
        realtimeGateway.connect(token = token, endpointOverride = webSocketUrl)
    }

    private fun flushPendingRealtimeMessages() {
        if (pendingRealtimeMessages.isEmpty()) return
        val iterator = pendingRealtimeMessages.iterator()
        var needsBootstrapRefresh = false
        while (iterator.hasNext()) {
            val pendingMessage = iterator.next()
            if (trySendRealtimeMessage(pendingMessage)) {
                if (!backendConversationIds.contains(pendingMessage.conversationId)) {
                    needsBootstrapRefresh = true
                }
                iterator.remove()
            } else {
                reportError("Message delivery failed.")
                return
            }
        }
        if (needsBootstrapRefresh) {
            refreshBootstrap()
        }
    }

    private fun upsertLocalOutgoingMessage(
        conversationId: String,
        body: String,
        attachment: MessageAttachment,
    ) {
        val timestamp = Instant.now().toString()
        val lastMessage = when {
            body.isNotBlank() -> body
            attachment.type == AttachmentType.Image -> "Sent an image"
            else -> "Sent a video"
        }
        conversationState.value = conversationState.value.map { conversation ->
            if (conversation.id != conversationId) {
                conversation
            } else {
                conversation.copy(
                    messages = conversation.messages + ChatMessage(
                        id = "local-$timestamp",
                        direction = MessageDirection.Outgoing,
                        kind = MessageKind.Text,
                        body = body,
                        createdAt = timestamp,
                        attachment = attachment,
                    ),
                    lastMessage = lastMessage,
                    lastTimestamp = timestamp,
                )
            }
        }
    }

    private fun upsertLocalOutgoingTextMessage(
        conversationId: String,
        body: String,
    ) {
        val timestamp = Instant.now().toString()
        conversationState.value = conversationState.value.map { conversation ->
            if (conversation.id != conversationId) {
                conversation
            } else {
                conversation.copy(
                    messages = conversation.messages + ChatMessage(
                        id = "local-$timestamp",
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
                    lastMessage = conversationPreviewText(message),
                    lastTimestamp = message.createdAt,
                    messages = messages.sortedBy { it.createdAt },
                )
            }
        }
    }

    private fun clearRuntimeState() {
        realtimeGateway.disconnect()
        loadedConversationIds.clear()
        loadingConversationIds.clear()
        backendConversationIds.clear()
        pendingHistoryConversationIds.clear()
        pendingRealtimeMessages.clear()
        hasObservedRealtimeSessionRegistration = false
        activeToken = null
        activeUserExternalId = null
        activeHttpBaseUrl = null
        activeWebSocketUrl = null
        contactState.value = emptyList()
        conversationState.value = emptyList()
        integrationStateValue.value = MessagingIntegrationState()
    }
}

private fun conversationPreviewText(message: ChatMessage): String = when {
    message.body.isNotBlank() -> message.body
    message.attachment?.type == AttachmentType.Image -> "Sent an image"
    message.attachment?.type == AttachmentType.Video -> "Sent a video"
    else -> ""
}

private fun requiredInputTypeFor(mode: AigcMode): AttachmentType? = when (mode) {
    AigcMode.TextToImage -> null
    AigcMode.ImageToImage -> AttachmentType.Image
    AigcMode.VideoToVideo -> AttachmentType.Video
}


