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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.PillAction
import com.gkim.im.android.core.media.MediaPickerControllerFactory
import com.gkim.im.android.core.media.rememberMediaPickerController
import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.AigcProvider
import com.gkim.im.android.core.model.AigcTask
import com.gkim.im.android.core.model.AttachmentType
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.Conversation
import com.gkim.im.android.core.model.DraftAigcRequest
import com.gkim.im.android.core.model.MediaInput
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.util.formatChatTimestamp
import com.gkim.im.android.data.repository.AigcRepository
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.MessagingRepository
import com.gkim.im.android.feature.shared.simpleViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private data class ChatUiState(
    val conversation: Conversation? = null,
    val activeProvider: AigcProvider? = null,
    val latestTask: AigcTask? = null,
    val draftRequest: DraftAigcRequest = DraftAigcRequest(),
)

private class ChatViewModel(
    conversationId: String,
    private val messagingRepository: MessagingRepository,
    private val aigcRepository: AigcRepository,
) : ViewModel() {
    private val resolvedConversationId = if (conversationId.isBlank() || conversationId == "studio") messagingRepository.ensureStudioRoom().id else conversationId

    init {
        messagingRepository.loadConversationHistory(resolvedConversationId)
    }

    val uiState = combine(
        messagingRepository.conversation(resolvedConversationId),
        aigcRepository.providers,
        aigcRepository.activeProviderId,
        aigcRepository.history,
        aigcRepository.draftRequest,
    ) { conversation, providers, activeProviderId, history, draftRequest ->
        ChatUiState(
            conversation = conversation,
            activeProvider = providers.firstOrNull { it.id == activeProviderId },
            latestTask = history.firstOrNull(),
            draftRequest = draftRequest,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    fun sendMessage(body: String) {
        if (body.isBlank()) return
        messagingRepository.sendMessage(resolvedConversationId, body)
    }

    fun runAigc(mode: AigcMode, prompt: String, mediaInput: MediaInput?) {
        if (prompt.isBlank()) return
        val task = aigcRepository.generate(mode, prompt, mediaInput)
        messagingRepository.appendAigcResult(resolvedConversationId, task)
    }
}

@Composable
fun ChatRoute(
    navController: NavHostController,
    container: AppContainer,
    conversationId: String,
    mediaPickerControllerFactory: MediaPickerControllerFactory? = null,
) {
    val viewModel = viewModel<ChatViewModel>(factory = simpleViewModelFactory {
        ChatViewModel(conversationId, container.messagingRepository, container.aigcRepository)
    })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var prompt by remember { mutableStateOf("") }
    var selectedMedia by remember { mutableStateOf<MediaInput?>(null) }
    var isSecondaryMenuOpen by remember { mutableStateOf(false) }
    val mediaPicker = mediaPickerControllerFactory?.invoke { selectedMedia = it } ?: rememberMediaPickerController { selectedMedia = it }

    LaunchedEffect(uiState.draftRequest.prompt) {
        if (uiState.draftRequest.prompt.isNotBlank()) {
            prompt = uiState.draftRequest.prompt
        }
    }
    LaunchedEffect(uiState.draftRequest.mediaInput) {
        uiState.draftRequest.mediaInput?.let { selectedMedia = it }
    }

    ChatScreen(
        uiState = uiState,
        prompt = prompt,
        selectedMedia = selectedMedia,
        isSecondaryMenuOpen = isSecondaryMenuOpen,
        onPromptChanged = { prompt = it },
        onBack = { navController.popBackStack() },
        onOpenWorkshop = { navController.navigate("workshop") },
        onToggleSecondaryMenu = { isSecondaryMenuOpen = !isSecondaryMenuOpen },
        onPickImage = mediaPicker.pickImage,
        onPickVideo = mediaPicker.pickVideo,
        onSendMessage = {
            if (prompt.isBlank()) return@ChatScreen
            viewModel.sendMessage(prompt)
            prompt = ""
        },
        onRunMode = { mode ->
            if (prompt.isBlank()) return@ChatScreen
            val mediaInput = when (mode) {
                AigcMode.TextToImage -> null
                AigcMode.ImageToImage,
                AigcMode.VideoToVideo,
                -> selectedMedia
            }
            viewModel.runAigc(mode, prompt, mediaInput)
            isSecondaryMenuOpen = false
        },
    )
}

@Composable
private fun ChatScreen(
    uiState: ChatUiState,
    prompt: String,
    selectedMedia: MediaInput?,
    isSecondaryMenuOpen: Boolean,
    onPromptChanged: (String) -> Unit,
    onBack: () -> Unit,
    onOpenWorkshop: () -> Unit,
    onToggleSecondaryMenu: () -> Unit,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onSendMessage: () -> Unit,
    onRunMode: (AigcMode) -> Unit,
) {
    val timelineMessages = uiState.conversation?.messages.orEmpty()
    val timelineState = rememberLazyListState()

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
        ChatTopBar(
            conversation = uiState.conversation,
            onBack = onBack,
            onOpenWorkshop = onOpenWorkshop,
        )

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
                items(timelineMessages, key = { it.id }) { message ->
                    ChatMessageRow(
                        conversation = uiState.conversation,
                        message = message,
                    )
                }
                uiState.latestTask?.let { task ->
                    item(key = "latest-task-${task.id}") {
                        LatestGenerationCard(task = task)
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
                    selectedMedia?.let { mediaInput ->
                        val selectedLabel = if (mediaInput.type == AttachmentType.Image) "Image ready" else "Video ready"
                        Text(
                            text = selectedLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AetherColors.OnSurface,
                            modifier = Modifier
                                .background(AetherColors.SurfaceContainerHigh, RoundedCornerShape(999.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("chat-selected-media"),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionChip(
                            label = "Pick image",
                            testTag = "chat-action-pick-image",
                            modifier = Modifier.weight(1f),
                            onClick = onPickImage,
                        )
                        ActionChip(
                            label = "Pick video",
                            testTag = "chat-action-pick-video",
                            modifier = Modifier.weight(1f),
                            onClick = onPickVideo,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionChip(
                            label = "Text to image",
                            testTag = "chat-action-text-to-image",
                            modifier = Modifier.weight(1f),
                        ) { onRunMode(AigcMode.TextToImage) }
                        ActionChip(
                            label = "Image to image",
                            testTag = "chat-action-image-to-image",
                            modifier = Modifier.weight(1f),
                        ) { onRunMode(AigcMode.ImageToImage) }
                    }
                    ActionChip(
                        label = "Video to video",
                        testTag = "chat-action-video-to-video",
                        modifier = Modifier.fillMaxWidth(),
                    ) { onRunMode(AigcMode.VideoToVideo) }
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
private fun LatestGenerationCard(task: AigcTask) {
    GlassCard(modifier = Modifier.testTag("chat-latest-generation-card")) {
        Text(text = "LATEST GENERATION", style = MaterialTheme.typography.labelLarge, color = AetherColors.Tertiary)
        AsyncImage(model = task.outputPreview, contentDescription = null, modifier = Modifier.fillMaxWidth().height(220.dp))
        Text(
            text = "${task.mode.name} · ${task.prompt}",
            style = MaterialTheme.typography.bodyLarge,
            color = AetherColors.OnSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ChatTopBar(
    conversation: Conversation?,
    onBack: () -> Unit,
    onOpenWorkshop: () -> Unit,
) {
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
                )
            }
        }
        PillAction(label = "Workshop", onClick = onOpenWorkshop)
    }
}

@Composable
private fun ChatMessageRow(
    conversation: Conversation?,
    message: ChatMessage,
) {
    val isOutgoing = message.direction == MessageDirection.Outgoing
    val hasAttachment = message.attachment != null
    val isOutgoingTextOnly = isOutgoing && !hasAttachment
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
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AetherColors.OnSurface,
                    modifier = Modifier.testTag("chat-message-body-${message.id}"),
                )
                message.attachment?.let { attachment ->
                    AsyncImage(
                        model = attachment.preview,
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

