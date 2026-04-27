package com.gkim.im.android.feature.chat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.BlockReasonCopy
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.SafetyCopy
import com.gkim.im.android.core.media.GeneratedImageSaveResult
import com.gkim.im.android.core.media.GeneratedImageSaver
import com.gkim.im.android.core.designsystem.PillAction
import com.gkim.im.android.core.media.MediaPickerControllerFactory
import com.gkim.im.android.core.media.rememberMediaPickerController
import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.AigcProvider
import com.gkim.im.android.core.model.AigcTask
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.AttachmentType
import com.gkim.im.android.core.model.BlockReason
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.CompanionTurnMeta
import com.gkim.im.android.core.model.FailedSubtype
import com.gkim.im.android.core.model.Conversation
import com.gkim.im.android.core.model.DraftAigcRequest
import com.gkim.im.android.core.model.MediaInput
import com.gkim.im.android.core.model.MessageAttachment
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageStatus
import com.gkim.im.android.core.model.TaskStatus
import com.gkim.im.android.core.model.UserPersona
import com.gkim.im.android.core.util.formatChatTimestamp
import com.gkim.im.android.data.repository.AigcRepository
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.CompanionTurnRepository
import com.gkim.im.android.data.repository.MessagingRepository
import com.gkim.im.android.data.repository.UserPersonaRepository
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.feature.shared.simpleViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal fun appLanguageWireKey(language: AppLanguage): String = when (language) {
    AppLanguage.English -> "en"
    AppLanguage.Chinese -> "zh"
}

internal data class ChatUiState(
    val conversation: Conversation? = null,
    val activeProvider: AigcProvider? = null,
    val activePersona: UserPersona? = null,
    val latestTask: AigcTask? = null,
    val draftRequest: DraftAigcRequest = DraftAigcRequest(),
    val generationActionFeedback: String? = null,
    val companionMessages: List<ChatMessage>? = null,
    val treeAffordanceLifecycle: TreeAffordanceLifecycle = TreeAffordanceLifecycle(),
)

/**
 * §4 — lifecycle state for the chat-tree affordances (Edit user bubble + Regenerate-from-here).
 * Drives the in-flight indicator + the inline error banner. At most one affordance is in-flight
 * at a time per conversation, so a single `inFlightForMessageId` field is sufficient (vs. a
 * map). On failure, `failedForMessageId` + `failureReason` carry the error context for the
 * banner; `dismissTreeAffordanceError()` clears them.
 */
internal data class TreeAffordanceLifecycle(
    val inFlightForMessageId: String? = null,
    val failedForMessageId: String? = null,
    val failureReason: String? = null,
)

private data class CoreChatState(
    val conversation: Conversation?,
    val providers: List<AigcProvider>,
    val activeProviderId: String,
    val history: List<AigcTask>,
    val draftRequest: DraftAigcRequest,
)

internal data class GenerationFeedback(
    val statusLine: String,
    val showPreview: Boolean,
)

internal class ChatViewModel(
    conversationId: String,
    private val messagingRepository: MessagingRepository,
    private val companionTurnRepository: CompanionTurnRepository,
    private val aigcRepository: AigcRepository,
    private val generatedImageSaver: GeneratedImageSaver,
    userPersonaRepository: UserPersonaRepository,
) : ViewModel() {
    private val resolvedConversationId = if (conversationId.isBlank() || conversationId == "studio") messagingRepository.ensureStudioRoom().id else conversationId
    private val generationActionFeedback = MutableStateFlow<String?>(null)
    private val treeAffordanceLifecycleState = MutableStateFlow(TreeAffordanceLifecycle())
    /** Direct read of the lifecycle state (bypasses `uiState`'s `WhileSubscribed` gating). */
    val treeAffordanceLifecycle: StateFlow<TreeAffordanceLifecycle> = treeAffordanceLifecycleState

    init {
        messagingRepository.loadConversationHistory(resolvedConversationId)
    }

    private val coreChatState = combine(
        messagingRepository.conversation(resolvedConversationId),
        aigcRepository.providers,
        aigcRepository.activeProviderId,
        aigcRepository.history,
        aigcRepository.draftRequest,
    ) { conversation, providers, activeProviderId, history, draftRequest ->
        CoreChatState(conversation, providers, activeProviderId, history, draftRequest)
    }

    private val companionMessagesFlow =
        companionTurnRepository.activePathByConversation.map { byConversation ->
            byConversation[resolvedConversationId]
        }

    private val baseUiState = combine(
        coreChatState,
        userPersonaRepository.observeActivePersona(),
        companionMessagesFlow,
    ) { core, activePersona, companionMessages ->
        val isCompanion = core.conversation?.companionCardId != null
        ChatUiState(
            conversation = core.conversation,
            activeProvider = core.providers.firstOrNull { it.id == core.activeProviderId },
            activePersona = activePersona,
            latestTask = core.history.firstOrNull(),
            draftRequest = core.draftRequest,
            companionMessages = if (isCompanion) companionMessages.orEmpty() else null,
        )
    }

    val uiState = combine(
        baseUiState,
        generationActionFeedback,
        treeAffordanceLifecycleState,
    ) { baseState, actionFeedback, treeLifecycle ->
        baseState.copy(
            generationActionFeedback = actionFeedback,
            treeAffordanceLifecycle = treeLifecycle,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    fun sendMessage(
        body: String,
        attachmentInput: MediaInput? = null,
        activeLanguage: AppLanguage = AppLanguage.English,
    ) {
        if (body.isBlank() && attachmentInput == null) return
        val companionCardId = currentConversationSnapshot()?.companionCardId
        if (companionCardId != null && attachmentInput == null) {
            viewModelScope.launch {
                companionTurnRepository.submitUserTurn(
                    conversationId = resolvedConversationId,
                    activeCompanionId = companionCardId,
                    userTurnBody = body,
                    activeLanguage = appLanguageWireKey(activeLanguage),
                    parentMessageId = null,
                )
            }
            generationActionFeedback.value = null
            return
        }
        messagingRepository.sendMessage(
            conversationId = resolvedConversationId,
            body = body,
            attachment = attachmentInput?.asMessageAttachment(),
        )
        generationActionFeedback.value = null
    }

    private fun currentConversationSnapshot(): Conversation? =
        messagingRepository.conversations.value.firstOrNull {
            it.id == resolvedConversationId || it.contactId == resolvedConversationId
        }

    /**
     * §4.1 — Edit a user bubble. Resolves the bubble from the current active path, builds the
     * §3.2 `EditUserBubbleSheetState`, and delegates to
     * `companionTurnRepository.editUserTurn`. The lifecycle map gates the affordance UI's
     * in-flight indicator + inline-error banner; on success the repository's applyRecord
     * already advances the active path so no extra effect application is needed at the
     * ViewModel layer.
     */
    fun editUserTurn(
        messageId: String,
        newDraftText: String,
        activeLanguage: AppLanguage = AppLanguage.English,
    ) {
        val companionId = currentConversationSnapshot()?.companionCardId ?: return
        val activeMessages = companionTurnRepository.activePathByConversation.value[resolvedConversationId].orEmpty()
        val bubble = activeMessages.firstOrNull { it.id == messageId } ?: return
        val sheet = editUserBubbleSheetState(
            bubble = bubble,
            conversationId = resolvedConversationId,
            activeCompanionId = companionId,
            activeLanguage = activeLanguage,
        ) ?: return
        val draftSheet = sheet.withDraft(newDraftText)
        if (!draftSheet.canSubmit()) return

        treeAffordanceLifecycleState.value = TreeAffordanceLifecycle(inFlightForMessageId = messageId)
        viewModelScope.launch {
            val result = companionTurnRepository.editUserTurn(
                conversationId = resolvedConversationId,
                parentMessageId = draftSheet.parentMessageId,
                newUserText = draftSheet.draftText,
                activeCompanionId = draftSheet.activeCompanionId,
                activeLanguage = draftSheet.activeLanguage,
            )
            treeAffordanceLifecycleState.value = result.fold(
                onSuccess = { TreeAffordanceLifecycle() },
                onFailure = { t ->
                    TreeAffordanceLifecycle(
                        failedForMessageId = messageId,
                        failureReason = t.message ?: "edit_failed",
                    )
                },
            )
        }
    }

    /**
     * §4.2 — Regenerate a companion bubble (mid-conversation supported per the §3.3 contract).
     * Resolves the bubble, validates via the §3.3 helper, and delegates to
     * `companionTurnRepository.regenerateCompanionTurnAtTarget`. Lifecycle handling mirrors
     * §4.1.
     */
    fun regenerateFromHere(messageId: String) {
        val activeMessages = companionTurnRepository.activePathByConversation.value[resolvedConversationId].orEmpty()
        val bubble = activeMessages.firstOrNull { it.id == messageId } ?: return
        val request = regenerateFromHereRequest(bubble, clientTurnId = "ignored-server-side") ?: return
        val targetMessageId = request.targetMessageId ?: return

        treeAffordanceLifecycleState.value = TreeAffordanceLifecycle(inFlightForMessageId = messageId)
        viewModelScope.launch {
            val result = companionTurnRepository.regenerateCompanionTurnAtTarget(
                conversationId = resolvedConversationId,
                targetMessageId = targetMessageId,
            )
            treeAffordanceLifecycleState.value = result.fold(
                onSuccess = { TreeAffordanceLifecycle() },
                onFailure = { t ->
                    TreeAffordanceLifecycle(
                        failedForMessageId = messageId,
                        failureReason = t.message ?: "regenerate_failed",
                    )
                },
            )
        }
    }

    /**
     * §3.1 chevron callback. Forwards the `(variantGroupId, newIndex)` pair to the
     * repository's selectVariantByGroup mutation, which the §2.1 projection re-runs to
     * surface the new active variant.
     */
    fun selectVariantAt(variantGroupId: String, newIndex: Int) {
        companionTurnRepository.selectVariantByGroup(
            conversationId = resolvedConversationId,
            variantGroupId = variantGroupId,
            newIndex = newIndex,
        )
    }

    fun dismissTreeAffordanceError() {
        treeAffordanceLifecycleState.value = TreeAffordanceLifecycle()
    }

    fun runAigc(mode: AigcMode, prompt: String, mediaInput: MediaInput?) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            val task = aigcRepository.generate(mode, prompt, mediaInput)
            if (task.status == TaskStatus.Succeeded) {
                messagingRepository.appendAigcResult(resolvedConversationId, task)
            }
        }
    }

    fun saveGeneratedImage(task: AigcTask) {
        val preview = task.outputPreview
        if (preview.isNullOrBlank()) {
            generationActionFeedback.value = "No generated image is available to save yet."
            return
        }
        viewModelScope.launch {
            generationActionFeedback.value = when (val result = generatedImageSaver.saveImage(preview, task.prompt)) {
                is GeneratedImageSaveResult.Success -> "Saved generated image locally."
                is GeneratedImageSaveResult.Failure -> result.message
            }
        }
    }

    fun sendGeneratedImage(task: AigcTask) {
        val attachment = task.asGeneratedMessageAttachment()
        if (attachment == null) {
            generationActionFeedback.value = "No generated image is available to send yet."
            return
        }
        messagingRepository.sendMessage(
            conversationId = resolvedConversationId,
            body = "",
            attachment = attachment,
        )
        generationActionFeedback.value = "Generated image sent to the conversation."
    }
}

@Composable
fun ChatRoute(
    navController: NavHostController,
    container: AppContainer,
    conversationId: String,
    mediaPickerControllerFactory: MediaPickerControllerFactory? = null,
) {
    val viewModel = viewModel<ChatViewModel>(
        key = "chat-$conversationId-${System.identityHashCode(container)}",
        factory = simpleViewModelFactory {
            ChatViewModel(
                conversationId = conversationId,
                messagingRepository = container.messagingRepository,
                companionTurnRepository = container.companionTurnRepository,
                aigcRepository = container.aigcRepository,
                generatedImageSaver = container.generatedImageSaver,
                userPersonaRepository = container.userPersonaRepository,
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appLanguage = LocalAppLanguage.current
    var prompt by remember { mutableStateOf("") }
    var chatAttachmentMedia by remember { mutableStateOf<MediaInput?>(null) }
    var generationSourceMedia by remember { mutableStateOf<MediaInput?>(null) }
    var isSecondaryMenuOpen by remember { mutableStateOf(false) }
    var showExportDialog by remember(conversationId) { mutableStateOf(false) }
    val exportDispatcher = rememberChatExportDispatcher()
    val chatAttachmentPicker = mediaPickerControllerFactory?.invoke { chatAttachmentMedia = it }
        ?: rememberMediaPickerController { chatAttachmentMedia = it }
    val generationSourcePicker = mediaPickerControllerFactory?.invoke { generationSourceMedia = it }
        ?: rememberMediaPickerController { generationSourceMedia = it }

    LaunchedEffect(uiState.draftRequest.prompt) {
        if (uiState.draftRequest.prompt.isNotBlank()) {
            prompt = uiState.draftRequest.prompt
        }
    }
    LaunchedEffect(uiState.draftRequest.mediaInput) {
        uiState.draftRequest.mediaInput?.let { generationSourceMedia = it }
    }

    ChatScreen(
        uiState = uiState,
        prompt = prompt,
        chatAttachmentMedia = chatAttachmentMedia,
        generationSourceMedia = generationSourceMedia,
        isSecondaryMenuOpen = isSecondaryMenuOpen,
        onPromptChanged = { prompt = it },
        onBack = { navController.popBackStack() },
        onOpenPortrait = { route -> navController.navigate(route) },
        onPersonaPillTap = {
            navController.navigate(ChatChromePersonaPillDefaults.DestinationRoute)
        },
        onToggleSecondaryMenu = { isSecondaryMenuOpen = !isSecondaryMenuOpen },
        onPickChatImage = chatAttachmentPicker.pickImage,
        onPickGenerationImage = generationSourcePicker.pickImage,
        onPickGenerationVideo = generationSourcePicker.pickVideo,
        onSendMessage = {
            if (prompt.isBlank() && chatAttachmentMedia == null) return@ChatScreen
            viewModel.sendMessage(prompt, chatAttachmentMedia, appLanguage)
            prompt = ""
            chatAttachmentMedia = null
            isSecondaryMenuOpen = false
        },
        onRunMode = { mode ->
            if (prompt.isBlank()) return@ChatScreen
            val mediaInput = when (mode) {
                AigcMode.TextToImage -> null
                AigcMode.ImageToImage,
                AigcMode.VideoToVideo,
                -> generationSourceMedia
            }
            viewModel.runAigc(mode, prompt, mediaInput)
            isSecondaryMenuOpen = false
        },
        onSaveGeneratedImage = viewModel::saveGeneratedImage,
        onSendGeneratedImage = viewModel::sendGeneratedImage,
        onSelectVariantAt = viewModel::selectVariantAt,
        onEditUserBubble = { messageId, draftText ->
            viewModel.editUserTurn(messageId, draftText, appLanguage)
        },
        onRegenerateFromHere = viewModel::regenerateFromHere,
        onDismissTreeAffordanceError = viewModel::dismissTreeAffordanceError,
        onOpenExportDialog = if (uiState.conversation?.companionCardId != null) {
            { showExportDialog = true }
        } else {
            null
        },
        onOpenSettings = if (uiState.conversation?.companionCardId != null) {
            { navController.navigate("settings") }
        } else {
            null
        },
    )

    if (showExportDialog) {
        ChatExportDialog(
            conversationId = conversationId,
            repository = container.companionTurnRepository,
            dispatcher = exportDispatcher,
            onDismiss = { showExportDialog = false },
        )
    }
}

@Composable
private fun ChatScreen(
    uiState: ChatUiState,
    prompt: String,
    chatAttachmentMedia: MediaInput?,
    generationSourceMedia: MediaInput?,
    isSecondaryMenuOpen: Boolean,
    onPromptChanged: (String) -> Unit,
    onBack: () -> Unit,
    onOpenPortrait: (String) -> Unit,
    onPersonaPillTap: () -> Unit,
    onToggleSecondaryMenu: () -> Unit,
    onPickChatImage: () -> Unit,
    onPickGenerationImage: () -> Unit,
    onPickGenerationVideo: () -> Unit,
    onSendMessage: () -> Unit,
    onRunMode: (AigcMode) -> Unit,
    onSaveGeneratedImage: (AigcTask) -> Unit,
    onSendGeneratedImage: (AigcTask) -> Unit,
    onSelectVariantAt: (variantGroupId: String, newIndex: Int) -> Unit = { _, _ -> },
    onEditUserBubble: (messageId: String, draftText: String) -> Unit = { _, _ -> },
    onRegenerateFromHere: (messageId: String) -> Unit = { _ -> },
    onDismissTreeAffordanceError: () -> Unit = {},
    onOpenExportDialog: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    val appLanguage = LocalAppLanguage.current
    val timelineMessages = uiState.companionMessages ?: uiState.conversation?.messages.orEmpty()
    val timelineState = rememberLazyListState()
    val visibleModes = visibleAigcModes(uiState.activeProvider)
    val readyModes = readyAigcModes(uiState.activeProvider, generationSourceMedia?.type)

    LaunchedEffect(timelineMessages.size, uiState.latestTask?.id) {
        val totalItems = timelineMessages.size + if (uiState.latestTask != null) 1 else 0
        if (totalItems > 0) {
            timelineState.scrollToItem(totalItems - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherColors.Surface)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("chat-screen"),
    ) {
        val headerPortraitRoute = com.gkim.im.android.feature.tavern.portraitTapRouteForChatHeader(uiState.conversation)
        val bubblePortraitRoute = com.gkim.im.android.feature.tavern.portraitTapRouteForChatBubble(uiState.conversation)
        ChatTopBar(
            conversation = uiState.conversation,
            activePersona = uiState.activePersona,
            onBack = onBack,
            onPersonaPillTap = onPersonaPillTap,
            onHeaderAvatarTap = headerPortraitRoute?.let { route -> { onOpenPortrait(route) } },
            onOpenExportDialog = onOpenExportDialog,
            onOpenSettings = onOpenSettings,
        )
        chatChromePersonaFooter(uiState.activePersona, LocalAppLanguage.current)?.let { footer ->
            Text(
                text = footer.text,
                style = MaterialTheme.typography.labelSmall,
                color = AetherColors.OnSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .semantics { contentDescription = footer.contentDescription }
                    .testTag("chat-persona-footer"),
            )
        }

        val treeLifecycle = uiState.treeAffordanceLifecycle
        if (treeLifecycle.inFlightForMessageId != null) {
            Text(
                text = if (appLanguage == AppLanguage.English) "Updating…" else "更新中…",
                style = MaterialTheme.typography.labelMedium,
                color = AetherColors.OnSurfaceVariant,
                modifier = Modifier.testTag("chat-tree-affordance-inflight"),
            )
        }
        if (treeLifecycle.failedForMessageId != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat-tree-affordance-error"),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = (if (appLanguage == AppLanguage.English) "Update failed: " else "更新失败：") +
                        (treeLifecycle.failureReason ?: ""),
                    style = MaterialTheme.typography.labelMedium,
                    color = AetherColors.Danger,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onDismissTreeAffordanceError,
                    modifier = Modifier.testTag("chat-tree-affordance-error-dismiss"),
                ) {
                    Text(text = if (appLanguage == AppLanguage.English) "Dismiss" else "关闭")
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("chat-timeline"),
                state = timelineState,
            ) {
                val lastCompanionIndex = timelineMessages.indexOfLast { it.companionTurnMeta != null }
                items(timelineMessages, key = { it.id }) { message ->
                    val isMostRecentCompanion = message.companionTurnMeta != null &&
                        timelineMessages.indexOf(message) == lastCompanionIndex
                    ChatMessageRow(
                        conversation = uiState.conversation,
                        message = message,
                        isMostRecentCompanionVariant = isMostRecentCompanion,
                        onBubbleAvatarTap = bubblePortraitRoute?.let { route -> { onOpenPortrait(route) } },
                        onSelectVariantAt = onSelectVariantAt,
                        onEditUserBubble = onEditUserBubble,
                        onRegenerateFromHere = onRegenerateFromHere,
                    )
                }
                uiState.latestTask?.let { task ->
                    item(key = "latest-task-${task.id}") {
                        LatestGenerationCard(
                            task = task,
                            actionFeedback = uiState.generationActionFeedback,
                            onSaveGeneratedImage = onSaveGeneratedImage,
                            onSendGeneratedImage = onSendGeneratedImage,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isSecondaryMenuOpen) {
                GlassCard(modifier = Modifier.testTag("chat-secondary-menu")) {
                    Text(text = "More tools", style = MaterialTheme.typography.labelLarge, color = AetherColors.Primary)
                    Text(
                        text = "Provider: ${uiState.activeProvider?.label ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AetherColors.OnSurfaceVariant,
                    )
                    Text(
                        text = "AIGC actions and media tools stay behind this menu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AetherColors.OnSurfaceVariant,
                    )
                    chatAttachmentMedia?.let { mediaInput ->
                        Text(
                            text = if (mediaInput.type == AttachmentType.Image) "Image message ready" else "Video message ready",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AetherColors.OnSurface,
                            modifier = Modifier
                                .background(AetherColors.SurfaceContainerHigh, RoundedCornerShape(999.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("chat-chat-attachment-ready"),
                        )
                    }
                    generationSourceMedia?.let { mediaInput ->
                        val selectedLabel = when (mediaInput.type) {
                            AttachmentType.Image -> "Image-to-image source ready"
                            AttachmentType.Video -> "Video-to-video source ready"
                        }
                        Text(
                            text = selectedLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AetherColors.OnSurface,
                            modifier = Modifier
                                .background(AetherColors.SurfaceContainerHigh, RoundedCornerShape(999.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("chat-generation-source-ready"),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionChip(
                            label = "Send image message",
                            testTag = "chat-action-send-image-message",
                            modifier = Modifier.weight(1f),
                            onClick = onPickChatImage,
                        )
                        if (AigcMode.ImageToImage in visibleModes) {
                            ActionChip(
                                label = "Choose image source",
                                testTag = "chat-action-choose-image-source",
                                modifier = Modifier.weight(1f),
                                onClick = onPickGenerationImage,
                            )
                        }
                    }
                    if (AigcMode.VideoToVideo in visibleModes) {
                        ActionChip(
                            label = "Choose video source",
                            testTag = "chat-action-choose-video-source",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onPickGenerationVideo,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (AigcMode.TextToImage in visibleModes) {
                            ActionChip(
                                label = "Text to image",
                                testTag = "chat-action-text-to-image",
                                modifier = Modifier.weight(1f),
                            ) { onRunMode(AigcMode.TextToImage) }
                        }
                        if (AigcMode.ImageToImage in readyModes) {
                            ActionChip(
                                label = "Image to image",
                                testTag = "chat-action-image-to-image",
                                modifier = Modifier.weight(1f),
                            ) { onRunMode(AigcMode.ImageToImage) }
                        }
                    }
                    if (AigcMode.ImageToImage in visibleModes && generationSourceMedia?.type != AttachmentType.Image) {
                        Text(
                            text = "Choose an image source to enable image-to-image.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AetherColors.OnSurfaceVariant,
                        )
                    }
                    if (AigcMode.VideoToVideo in readyModes) {
                        ActionChip(
                            label = "Video to video",
                            testTag = "chat-action-video-to-video",
                            modifier = Modifier.fillMaxWidth(),
                        ) { onRunMode(AigcMode.VideoToVideo) }
                    }
                    if (AigcMode.VideoToVideo in visibleModes && generationSourceMedia?.type != AttachmentType.Video) {
                        Text(
                            text = "Choose a video source to enable video-to-video.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AetherColors.OnSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat-composer-row"),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .background(AetherColors.SurfaceContainerHigh, RoundedCornerShape(18.dp))
                        .clickable(onClick = onToggleSecondaryMenu)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("chat-plus-button"),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text(text = "+", style = MaterialTheme.typography.titleLarge, color = AetherColors.OnSurface)
                }
                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChanged,
                    modifier = Modifier.weight(1f).testTag("chat-composer-input"),
                    placeholder = { Text("Send a message") },
                    singleLine = true,
                )
                Box(
                    modifier = Modifier
                        .background(AetherColors.PrimaryContainer, RoundedCornerShape(18.dp))
                        .clickable(onClick = onSendMessage)
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                        .testTag("chat-send-button"),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text(text = "Send", style = MaterialTheme.typography.titleMedium, color = AetherColors.OnSurface)
                }
            }
        }
    }
}

@Composable
private fun LatestGenerationCard(
    task: AigcTask,
    actionFeedback: String?,
    onSaveGeneratedImage: (AigcTask) -> Unit,
    onSendGeneratedImage: (AigcTask) -> Unit,
) {
    val feedback = generationFeedback(task)
    val showImageActions = task.status == TaskStatus.Succeeded &&
        !task.outputPreview.isNullOrBlank() &&
        task.mode != AigcMode.VideoToVideo
    GlassCard(modifier = Modifier.testTag("chat-latest-generation-card")) {
        Text(text = "LATEST GENERATION", style = MaterialTheme.typography.labelLarge, color = AetherColors.Tertiary)
        if (feedback.showPreview && task.outputPreview != null) {
            AsyncImage(model = task.outputPreview, contentDescription = null, modifier = Modifier.fillMaxWidth().height(220.dp))
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .background(AetherColors.SurfaceContainerHigh, RoundedCornerShape(18.dp))
                    .testTag("chat-generation-placeholder"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = feedback.statusLine,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AetherColors.OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }
        }
        Text(
            text = "${task.mode.name} · ${task.prompt}",
            style = MaterialTheme.typography.bodyLarge,
            color = AetherColors.OnSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = feedback.statusLine,
            style = MaterialTheme.typography.bodyMedium,
            color = if (task.status == TaskStatus.Failed) AetherColors.OnSurface else AetherColors.OnSurfaceVariant,
            modifier = Modifier.testTag("chat-generation-status"),
        )
        task.errorMessage?.let { errorMessage ->
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = AetherColors.OnSurface,
                modifier = Modifier.testTag("chat-generation-error"),
            )
        }
        if (showImageActions) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionChip(
                    label = "Save locally",
                    testTag = "chat-generation-save",
                    modifier = Modifier.weight(1f),
                ) { onSaveGeneratedImage(task) }
                ActionChip(
                    label = "Send to chat",
                    testTag = "chat-generation-send",
                    modifier = Modifier.weight(1f),
                ) { onSendGeneratedImage(task) }
            }
        }
        actionFeedback?.let { feedbackMessage ->
            Text(
                text = feedbackMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = AetherColors.OnSurfaceVariant,
                modifier = Modifier.testTag("chat-generation-action-feedback"),
            )
        }
    }
}

@Composable
internal fun ChatTopBar(
    conversation: Conversation?,
    activePersona: UserPersona?,
    onBack: () -> Unit,
    onPersonaPillTap: () -> Unit,
    onHeaderAvatarTap: (() -> Unit)? = null,
    onOpenExportDialog: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    val language = LocalAppLanguage.current
    val personaPill = chatChromePersonaPill(activePersona, language)
    var overflowOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat-top-bar"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .background(AetherColors.SurfaceContainerHigh, RoundedCornerShape(14.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("chat-back-button"),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Text(text = "<", style = MaterialTheme.typography.titleLarge, color = AetherColors.OnSurface)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AetherColors.Primary.copy(alpha = 0.18f), CircleShape)
                    .let { if (onHeaderAvatarTap != null) it.clickable(onClick = onHeaderAvatarTap) else it }
                    .testTag("chat-header-avatar"),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Text(
                    text = conversation?.avatarText ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.Primary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation?.contactName ?: "Chat",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AetherColors.OnSurface,
                    modifier = Modifier.testTag("chat-contact-name"),
                )
                Text(
                    text = conversation?.contactTitle ?: "AIGC-enabled conversation surface.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("chat-contact-title"),
                )
            }
        }
        Box(modifier = Modifier.testTag("chat-persona-pill")) {
            PillAction(label = personaPill.label, onClick = onPersonaPillTap)
        }
        if (onOpenExportDialog != null) {
            Box(modifier = Modifier.testTag("chat-top-overflow")) {
                Box(
                    modifier = Modifier
                        .background(AetherColors.SurfaceContainerHigh, RoundedCornerShape(14.dp))
                        .clickable { overflowOpen = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("chat-top-overflow-trigger"),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text(text = "⋮", style = MaterialTheme.typography.titleLarge, color = AetherColors.OnSurface)
                }
                DropdownMenu(
                    expanded = overflowOpen,
                    onDismissRequest = { overflowOpen = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (language == AppLanguage.English) "Export chat" else "导出对话",
                                modifier = Modifier.testTag("chat-top-overflow-export-text"),
                            )
                        },
                        onClick = {
                            overflowOpen = false
                            onOpenExportDialog()
                        },
                        modifier = Modifier.testTag("chat-top-overflow-export"),
                    )
                    if (onOpenSettings != null) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (language == AppLanguage.English) "Settings" else "设置",
                                    modifier = Modifier.testTag("chat-top-overflow-settings-text"),
                                )
                            },
                            onClick = {
                                overflowOpen = false
                                onOpenSettings()
                            },
                            modifier = Modifier.testTag("chat-top-overflow-settings"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChatMessageRow(
    conversation: Conversation?,
    message: ChatMessage,
    isMostRecentCompanionVariant: Boolean = false,
    onBubbleAvatarTap: (() -> Unit)? = null,
    onSelectVariantAt: (variantGroupId: String, newIndex: Int) -> Unit = { _, _ -> },
    onRetrySubmission: () -> Unit = {},
    onComposeNewMessage: () -> Unit = {},
    onLearnMorePolicy: () -> Unit = {},
    onRetryCompanionTurn: () -> Unit = {},
    onEditUserTurn: () -> Unit = {},
    onEditUserBubble: (messageId: String, draftText: String) -> Unit = { _, _ -> },
    onRegenerateFromHere: (messageId: String) -> Unit = { _ -> },
) {
    var editDialogDraft by remember(message.id) { mutableStateOf<String?>(null) }
    val showUserBubbleEdit = shouldShowUserBubbleEdit(message, conversation)
    val showRegenerateFromHere = shouldShowRegenerateFromHere(message)

    val variantNavigation = chatBubbleVariantNavigation(message.companionTurnMeta)
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val isOutgoing = message.direction == MessageDirection.Outgoing
    val companionPresentation = companionLifecyclePresentation(message, isMostRecentCompanionVariant)
    val hasAttachment = message.attachment != null
    val hasBody = if (companionPresentation != null) companionPresentation.showBody else message.body.isNotBlank()
    val isOutgoingTextOnly = isOutgoing && !hasAttachment && hasBody
    val isIncomingOrSystem = !isOutgoing
    val authorName = when (message.direction) {
        MessageDirection.Incoming -> conversation?.contactName ?: "Contact"
        MessageDirection.Outgoing -> "You"
        MessageDirection.System -> "Aether System"
    }
    val avatarText = when (message.direction) {
        MessageDirection.Incoming -> conversation?.avatarText ?: "CT"
        MessageDirection.Outgoing -> "ME"
        MessageDirection.System -> "AI"
    }
    val bubbleColor = when (message.direction) {
        MessageDirection.Outgoing -> AetherColors.PrimaryContainer
        MessageDirection.System -> AetherColors.SurfaceContainerHigh
        MessageDirection.Incoming -> AetherColors.SurfaceContainerLow
    }
    val avatarBackground = when (message.direction) {
        MessageDirection.Outgoing -> AetherColors.Primary.copy(alpha = 0.22f)
        MessageDirection.System -> AetherColors.Tertiary.copy(alpha = 0.2f)
        MessageDirection.Incoming -> AetherColors.Primary.copy(alpha = 0.14f)
    }
    val avatarForeground = when (message.direction) {
        MessageDirection.Outgoing -> AetherColors.Primary
        MessageDirection.System -> AetherColors.Tertiary
        MessageDirection.Incoming -> AetherColors.Primary
    }
    val bubbleSpacing = 4.dp
    val bubblePadding = if (isOutgoing) 16.dp else 14.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat-message-row-${message.id}"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (isOutgoing) {
            Box(modifier = Modifier.weight(1f))
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(avatarBackground, CircleShape)
                    .let { if (onBubbleAvatarTap != null && message.direction == MessageDirection.Incoming) it.clickable(onClick = onBubbleAvatarTap) else it }
                    .testTag("chat-message-avatar-${message.id}"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = avatarText,
                    color = avatarForeground,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Column(
            modifier = if (isOutgoing) Modifier else Modifier.fillMaxWidth(fraction = 0.8f),
            verticalArrangement = Arrangement.spacedBy(if (isIncomingOrSystem) 4.dp else 6.dp),
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
        ) {
            if (isIncomingOrSystem) {
                Text(
                    text = authorName,
                    style = MaterialTheme.typography.labelMedium,
                    color = AetherColors.OnSurfaceVariant,
                    modifier = Modifier.testTag("chat-message-sender-${message.id}"),
                )
            }
            Column(
                modifier = Modifier
                    .then(if (isIncomingOrSystem) Modifier.fillMaxWidth() else Modifier)
                    .then(if (isOutgoingTextOnly) Modifier.widthIn(max = 320.dp) else Modifier)
                    .background(bubbleColor, RoundedCornerShape(24.dp))
                    .padding(horizontal = bubblePadding, vertical = bubblePadding)
                    .testTag("chat-message-bubble-${message.id}"),
                verticalArrangement = Arrangement.spacedBy(bubbleSpacing),
                horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
            ) {
                if (companionPresentation != null) {
                    Text(
                        text = companionPresentation.statusLine,
                        style = MaterialTheme.typography.labelMedium,
                        color = AetherColors.OnSurfaceVariant,
                        modifier = Modifier.testTag("chat-companion-status-${message.id}"),
                    )
                    if (companionPresentation.showBody) {
                        Text(
                            text = companionPresentation.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AetherColors.OnSurface,
                            modifier = Modifier.testTag("chat-message-body-${message.id}"),
                        )
                    }
                    companionPresentation.blockReason?.let { reason ->
                        Text(
                            text = BlockReasonCopy.localizedCopy(reason, language),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AetherColors.OnSurface,
                            modifier = Modifier.testTag("chat-companion-block-copy-${message.id}"),
                        )
                    }
                    companionPresentation.failedSubtype?.let { subtype ->
                        Text(
                            text = SafetyCopy.localizedFailedCopy(subtype, language),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AetherColors.OnSurface,
                            modifier = Modifier.testTag("chat-companion-failed-copy-${message.id}"),
                        )
                    }
                    if (companionPresentation.tone == CompanionLifecycleTone.Timeout) {
                        Text(
                            text = SafetyCopy.localizedTimeoutCopy(language),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AetherColors.OnSurface,
                            modifier = Modifier.testTag("chat-companion-timeout-copy-${message.id}"),
                        )
                    }
                    if (companionPresentation.showSwitchPresetHint) {
                        Text(
                            text = SafetyCopy.localizedTimeoutPresetHint(language),
                            style = MaterialTheme.typography.labelMedium,
                            color = AetherColors.OnSurfaceVariant,
                            modifier = Modifier.testTag("chat-companion-switch-preset-hint-${message.id}"),
                        )
                    }
                    if (companionPresentation.showCheckConnectionHint) {
                        Text(
                            text = if (language == AppLanguage.English) "Check your connection, then retry." else "请检查网络连接后重试。",
                            style = MaterialTheme.typography.labelMedium,
                            color = AetherColors.OnSurfaceVariant,
                            modifier = Modifier.testTag("chat-companion-connection-hint-${message.id}"),
                        )
                    }
                    if (variantNavigation != null) {
                        val meta = message.companionTurnMeta
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("chat-companion-variant-nav-${message.id}"),
                        ) {
                            Text(
                                text = "<",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (variantNavigation.hasPrevious) AetherColors.Primary else AetherColors.OnSurfaceVariant,
                                modifier = Modifier
                                    .clickable(enabled = variantNavigation.hasPrevious) {
                                        resolveVariantSelection(meta, VariantSwipeDirection.Previous)
                                            ?.let { (groupId, newIndex) -> onSelectVariantAt(groupId, newIndex) }
                                    }
                                    .testTag("chat-companion-variant-prev-${message.id}"),
                            )
                            Text(
                                text = variantNavigation.indicator,
                                style = MaterialTheme.typography.labelMedium,
                                color = AetherColors.OnSurfaceVariant,
                                modifier = Modifier.testTag("chat-companion-variant-indicator-${message.id}"),
                            )
                            Text(
                                text = ">",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (variantNavigation.hasNext) AetherColors.Primary else AetherColors.OnSurfaceVariant,
                                modifier = Modifier
                                    .clickable(enabled = variantNavigation.hasNext) {
                                        resolveVariantSelection(meta, VariantSwipeDirection.Next)
                                            ?.let { (groupId, newIndex) -> onSelectVariantAt(groupId, newIndex) }
                                    }
                                    .testTag("chat-companion-variant-next-${message.id}"),
                            )
                        }
                    }
                    if (showRegenerateFromHere) {
                        Text(
                            text = if (language == AppLanguage.English) "Regenerate from here" else "从这里重新生成",
                            style = MaterialTheme.typography.labelLarge,
                            color = AetherColors.Primary,
                            modifier = Modifier
                                .clickable { onRegenerateFromHere(message.id) }
                                .testTag("chat-companion-regenerate-from-here-${message.id}"),
                        )
                    }
                    if (companionPresentation.showRetry) {
                        Text(
                            text = if (language == AppLanguage.English) "Retry" else "重试",
                            style = MaterialTheme.typography.labelLarge,
                            color = AetherColors.Primary,
                            modifier = Modifier
                                .clickable(onClick = onRetryCompanionTurn)
                                .testTag("chat-companion-retry-${message.id}"),
                        )
                    }
                    if (companionPresentation.showEditUserTurn) {
                        Text(
                            text = if (language == AppLanguage.English) "Edit message" else "编辑消息",
                            style = MaterialTheme.typography.labelLarge,
                            color = AetherColors.Primary,
                            modifier = Modifier
                                .clickable(onClick = onEditUserTurn)
                                .testTag("chat-companion-edit-user-turn-${message.id}"),
                        )
                    }
                    if (companionPresentation.showComposeNew) {
                        Text(
                            text = if (language == AppLanguage.English) "Compose a new message" else "撰写新消息",
                            style = MaterialTheme.typography.labelLarge,
                            color = AetherColors.Primary,
                            modifier = Modifier
                                .clickable(onClick = onComposeNewMessage)
                                .testTag("chat-companion-compose-new-${message.id}"),
                        )
                    }
                    if (companionPresentation.showLearnMorePolicy) {
                        Text(
                            text = if (language == AppLanguage.English) "Learn more" else "了解更多",
                            style = MaterialTheme.typography.labelMedium,
                            color = AetherColors.OnSurfaceVariant,
                            modifier = Modifier
                                .clickable(onClick = onLearnMorePolicy)
                                .testTag("chat-companion-learn-more-policy-${message.id}"),
                        )
                    }
                } else if (hasBody) {
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AetherColors.OnSurface,
                        modifier = Modifier.testTag("chat-message-body-${message.id}"),
                    )
                    if (showUserBubbleEdit) {
                        Text(
                            text = if (language == AppLanguage.English) "Edit" else "编辑",
                            style = MaterialTheme.typography.labelLarge,
                            color = AetherColors.Primary,
                            modifier = Modifier
                                .clickable { editDialogDraft = message.body }
                                .testTag("chat-edit-user-overflow-${message.id}"),
                        )
                    }
                }
                val userFailure = outgoingSubmissionFailureLine(message)
                if (userFailure != null) {
                    Text(
                        text = userFailure,
                        style = MaterialTheme.typography.labelMedium,
                        color = AetherColors.Danger,
                        modifier = Modifier.testTag("chat-user-submission-status-${message.id}"),
                    )
                    Text(
                        text = "Retry",
                        style = MaterialTheme.typography.labelLarge,
                        color = AetherColors.Primary,
                        modifier = Modifier
                            .clickable(onClick = onRetrySubmission)
                            .testTag("chat-user-submission-retry-${message.id}"),
                    )
                }
                message.attachment?.let { attachment ->
                    AsyncImage(
                        model = messageAttachmentModel(context, attachment),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("chat-message-attachment-${message.id}"),
                    )
                }
                if (isOutgoingTextOnly) {
                    Text(
                        text = formatChatTimestamp(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = AetherColors.OnSurfaceVariant,
                        modifier = Modifier.testTag("chat-message-time-${message.id}"),
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = formatChatTimestamp(message.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = AetherColors.OnSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .testTag("chat-message-time-${message.id}"),
                        )
                    }
                }
            }
        }
    }
    val draft = editDialogDraft
    if (draft != null) {
        AlertDialog(
            onDismissRequest = { editDialogDraft = null },
            modifier = Modifier.testTag("chat-edit-user-dialog"),
            title = {
                Text(text = if (language == AppLanguage.English) "Edit message" else "编辑消息")
            },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { editDialogDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat-edit-user-textfield"),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val current = editDialogDraft.orEmpty()
                        editDialogDraft = null
                        onEditUserBubble(message.id, current)
                    },
                    enabled = draft.isNotBlank() && draft != message.body,
                    modifier = Modifier.testTag("chat-edit-user-submit"),
                ) {
                    Text(text = if (language == AppLanguage.English) "Save" else "保存")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { editDialogDraft = null },
                    modifier = Modifier.testTag("chat-edit-user-cancel"),
                ) {
                    Text(text = if (language == AppLanguage.English) "Cancel" else "取消")
                }
            },
        )
    }
}

@Composable
private fun ActionChip(
    label: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = AetherColors.OnSurface,
        modifier = Modifier
            .then(modifier)
            .background(AetherColors.SurfaceContainerHigh, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
    )
}

internal fun visibleAigcModes(activeProvider: AigcProvider?): List<AigcMode> =
    activeProvider?.capabilities?.toList()?.sortedBy { it.ordinal }.orEmpty()

internal fun readyAigcModes(
    activeProvider: AigcProvider?,
    generationSourceType: AttachmentType?,
): List<AigcMode> = visibleAigcModes(activeProvider).filter { mode ->
    when (mode) {
        AigcMode.TextToImage -> true
        AigcMode.ImageToImage -> generationSourceType == AttachmentType.Image
        AigcMode.VideoToVideo -> generationSourceType == AttachmentType.Video
    }
}

private fun MediaInput.asMessageAttachment(): MessageAttachment = MessageAttachment(
    type = type,
    preview = uri.toString(),
)

private fun AigcTask.asGeneratedMessageAttachment(): MessageAttachment? {
    val preview = outputPreview?.takeIf { it.isNotBlank() } ?: return null
    return MessageAttachment(
        type = AttachmentType.Image,
        preview = preview,
        prompt = prompt,
        generationId = id,
    )
}

private fun messageAttachmentModel(
    context: android.content.Context,
    attachment: MessageAttachment,
): Any = if (attachment.authToken.isNullOrBlank()) {
    attachment.preview
} else {
    ImageRequest.Builder(context)
        .data(attachment.preview)
        .addHeader("Authorization", "Bearer ${attachment.authToken}")
        .build()
}

internal fun generationFeedback(task: AigcTask): GenerationFeedback = when (task.status) {
    TaskStatus.Queued -> GenerationFeedback(
        statusLine = "Generating with ${task.providerId} · ${task.model}",
        showPreview = false,
    )
    TaskStatus.Failed -> GenerationFeedback(
        statusLine = task.errorMessage ?: "Generation failed with ${task.providerId} · ${task.model}",
        showPreview = false,
    )
    TaskStatus.Succeeded -> GenerationFeedback(
        statusLine = "Ready from ${task.providerId} · ${task.model}",
        showPreview = !task.outputPreview.isNullOrBlank(),
    )
}

internal fun outgoingSubmissionFailureLine(message: ChatMessage): String? {
    if (message.direction != MessageDirection.Outgoing) return null
    return when (message.status) {
        MessageStatus.Failed -> "Failed to send"
        MessageStatus.Timeout -> "Timed out — tap retry"
        else -> null
    }
}

internal data class VariantNavigationState(
    val indicator: String,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val activeIndex: Int,
    val total: Int,
)

internal fun variantNavigationState(
    variantGroupSiblingCount: Int,
    activeIndex: Int,
): VariantNavigationState? {
    if (variantGroupSiblingCount <= 1) return null
    val clampedIndex = activeIndex.coerceIn(0, variantGroupSiblingCount - 1)
    return VariantNavigationState(
        indicator = "${clampedIndex + 1}/$variantGroupSiblingCount",
        hasPrevious = clampedIndex > 0,
        hasNext = clampedIndex < variantGroupSiblingCount - 1,
        activeIndex = clampedIndex,
        total = variantGroupSiblingCount,
    )
}

internal enum class VariantSwipeDirection { Previous, Next }

internal fun chatBubbleVariantNavigation(meta: CompanionTurnMeta?): VariantNavigationState? {
    if (meta == null) return null
    return variantNavigationState(meta.siblingCount, meta.siblingActiveIndex)
}

internal fun resolveVariantSelection(
    meta: CompanionTurnMeta?,
    direction: VariantSwipeDirection,
): Pair<String, Int>? {
    if (meta == null) return null
    val nav = chatBubbleVariantNavigation(meta) ?: return null
    val newIndex = when (direction) {
        VariantSwipeDirection.Previous -> if (nav.hasPrevious) nav.activeIndex - 1 else return null
        VariantSwipeDirection.Next -> if (nav.hasNext) nav.activeIndex + 1 else return null
    }
    return meta.variantGroupId to newIndex
}

internal data class CompanionLifecyclePresentation(
    val statusLine: String,
    val body: String,
    val showBody: Boolean,
    val showRegenerate: Boolean,
    val showRetry: Boolean,
    val modelBadge: String?,
    val tone: CompanionLifecycleTone,
    val blockReason: BlockReason? = null,
    val showComposeNew: Boolean = false,
    val showLearnMorePolicy: Boolean = false,
    val failedSubtype: FailedSubtype? = null,
    val showEditUserTurn: Boolean = false,
    val showCheckConnectionHint: Boolean = false,
    val showSwitchPresetHint: Boolean = false,
)

internal const val TIMEOUT_PRESET_HINT_MAX_REPLY_TOKENS_CAP: Int = 1024

internal enum class CompanionLifecycleTone {
    Thinking, Streaming, Completed, Failed, Timeout, Blocked,
}

internal fun companionLifecyclePresentation(
    message: ChatMessage,
    isMostRecentCompanionVariant: Boolean,
    activePresetMaxReplyTokens: Int? = null,
): CompanionLifecyclePresentation? {
    val meta = message.companionTurnMeta ?: return null
    val modelBadge = meta.model?.takeIf { it.isNotBlank() }
    return when (message.status) {
        MessageStatus.Thinking -> CompanionLifecyclePresentation(
            statusLine = "Thinking…",
            body = "",
            showBody = false,
            showRegenerate = false,
            showRetry = false,
            modelBadge = modelBadge,
            tone = CompanionLifecycleTone.Thinking,
        )
        MessageStatus.Streaming -> CompanionLifecyclePresentation(
            statusLine = "Streaming…",
            body = message.body,
            showBody = message.body.isNotEmpty(),
            showRegenerate = false,
            showRetry = false,
            modelBadge = modelBadge,
            tone = CompanionLifecycleTone.Streaming,
        )
        MessageStatus.Completed -> CompanionLifecyclePresentation(
            statusLine = modelBadge?.let { "Model · $it" } ?: "Ready",
            body = message.body,
            showBody = message.body.isNotBlank(),
            showRegenerate = meta.canRegenerate && isMostRecentCompanionVariant,
            showRetry = false,
            modelBadge = modelBadge,
            tone = CompanionLifecycleTone.Completed,
        )
        MessageStatus.Failed -> {
            val subtype = meta.failedSubtypeKey?.let(FailedSubtype::fromWireKey)
            val canRetry = when (subtype) {
                FailedSubtype.PromptBudgetExceeded, FailedSubtype.AuthenticationFailed -> false
                else -> true
            }
            val needsEdit = when (subtype) {
                FailedSubtype.PromptBudgetExceeded, FailedSubtype.AuthenticationFailed -> true
                else -> false
            }
            val needsConnectionHint = when (subtype) {
                FailedSubtype.ProviderUnavailable, FailedSubtype.NetworkError -> true
                else -> false
            }
            CompanionLifecyclePresentation(
                statusLine = message.body.takeIf { it.isNotBlank() }?.let { "Failed · $it" } ?: "Failed",
                body = "",
                showBody = false,
                showRegenerate = false,
                showRetry = canRetry,
                modelBadge = modelBadge,
                tone = CompanionLifecycleTone.Failed,
                failedSubtype = subtype,
                showEditUserTurn = needsEdit,
                showCheckConnectionHint = needsConnectionHint,
            )
        }
        MessageStatus.Timeout -> {
            val showSwitchPresetHint = activePresetMaxReplyTokens != null &&
                activePresetMaxReplyTokens > TIMEOUT_PRESET_HINT_MAX_REPLY_TOKENS_CAP
            CompanionLifecyclePresentation(
                statusLine = message.body.takeIf { it.isNotBlank() }?.let { "Timed out · $it" } ?: "Timed out",
                body = "",
                showBody = false,
                showRegenerate = false,
                showRetry = true,
                modelBadge = modelBadge,
                tone = CompanionLifecycleTone.Timeout,
                showSwitchPresetHint = showSwitchPresetHint,
            )
        }
        MessageStatus.Blocked -> CompanionLifecyclePresentation(
            statusLine = message.body.takeIf { it.isNotBlank() }?.let { "Blocked · $it" } ?: "Blocked",
            body = "",
            showBody = false,
            showRegenerate = false,
            showRetry = false,
            modelBadge = modelBadge,
            tone = CompanionLifecycleTone.Blocked,
            blockReason = BlockReason.fromWireKey(meta.blockReasonKey),
            showComposeNew = true,
            showLearnMorePolicy = true,
        )
        MessageStatus.Pending -> null
    }
}

