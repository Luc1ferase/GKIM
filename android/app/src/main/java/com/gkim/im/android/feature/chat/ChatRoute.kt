package com.gkim.im.android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    Column(
        modifier = Modifier
            .background(AetherColors.Surface)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("chat-screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ChatTopBar(
            conversation = uiState.conversation,
            onBack = onBack,
            onOpenWorkshop = onOpenWorkshop,
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false).testTag("chat-timeline"),
        ) {
            items(uiState.conversation?.messages.orEmpty(), key = { it.id }) { message ->
                ChatMessageBubble(message)
            }
        }

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

        uiState.latestTask?.let { task ->
            GlassCard {
                Text(text = "LATEST GENERATION", style = MaterialTheme.typography.labelLarge, color = AetherColors.Tertiary)
                AsyncImage(model = task.outputPreview, contentDescription = null, modifier = Modifier.fillMaxWidth().height(220.dp))
                Text(text = "${task.mode.name} · ${task.prompt}", style = MaterialTheme.typography.bodyLarge, color = AetherColors.OnSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
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
                Text(text = conversation?.contactName ?: "Chat", style = MaterialTheme.typography.headlineMedium, color = AetherColors.OnSurface)
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
private fun ChatMessageBubble(message: ChatMessage) {
    val bubbleColor = when (message.direction) {
        MessageDirection.Outgoing -> AetherColors.PrimaryContainer
        MessageDirection.System -> AetherColors.SurfaceContainerHigh
        MessageDirection.Incoming -> AetherColors.SurfaceContainerLow
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bubbleColor, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = message.body, style = MaterialTheme.typography.bodyLarge, color = AetherColors.OnSurface)
        message.attachment?.let { attachment ->
            AsyncImage(model = attachment.preview, contentDescription = null, modifier = Modifier.fillMaxWidth().height(180.dp))
        }
        Text(text = formatChatTimestamp(message.createdAt), style = MaterialTheme.typography.bodyMedium, color = AetherColors.OnSurfaceVariant)
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

