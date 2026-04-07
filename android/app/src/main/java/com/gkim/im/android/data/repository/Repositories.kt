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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
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

interface MessagingRepository {
    val conversations: StateFlow<List<Conversation>>
    fun conversation(conversationId: String): Flow<Conversation?>
    fun ensureConversation(contact: Contact): Conversation
    fun ensureStudioRoom(): Conversation
    fun sendMessage(conversationId: String, body: String)
    fun appendAigcResult(conversationId: String, task: AigcTask)
}

class InMemoryMessagingRepository(seed: List<Conversation>) : MessagingRepository {
    private val conversationState = MutableStateFlow(seed)
    override val conversations: StateFlow<List<Conversation>> = conversationState

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


