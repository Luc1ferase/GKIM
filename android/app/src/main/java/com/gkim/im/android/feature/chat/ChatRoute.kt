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
import com.gkim.im.android.core.designsystem.GradientPrimaryButton
import com.gkim.im.android.core.designsystem.PillAction
import com.gkim.im.android.core.media.rememberMediaPickerController
import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.AigcProvider
import com.gkim.im.android.core.model.AigcTask
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
fun ChatRoute(navController: NavHostController, container: AppContainer, conversationId: String) {
    val viewModel = viewModel<ChatViewModel>(factory = simpleViewModelFactory {
        ChatViewModel(conversationId, container.messagingRepository, container.aigcRepository)
    })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var prompt by remember { mutableStateOf("Create an editorial portrait with indigo rim light and polished glass surfaces.") }
    var selectedMedia by remember { mutableStateOf<MediaInput?>(null) }
    val mediaPicker = rememberMediaPickerController { selectedMedia = it }

    LaunchedEffect(uiState.draftRequest.prompt) {
        if (uiState.draftRequest.prompt.isNotBlank()) {
            prompt = uiState.draftRequest.prompt
        }
    }

    ChatScreen(
        uiState = uiState,
        prompt = prompt,
        selectedMedia = selectedMedia,
        onPromptChanged = { prompt = it },
        onBack = { navController.popBackStack() },
        onOpenWorkshop = { navController.navigate("workshop") },
        onPickImage = mediaPicker.pickImage,
        onPickVideo = mediaPicker.pickVideo,
        onSendMessage = { viewModel.sendMessage(prompt) },
        onRunMode = { mode -> viewModel.runAigc(mode, prompt, selectedMedia) },
    )
}

@Composable
private fun ChatScreen(
    uiState: ChatUiState,
    prompt: String,
    selectedMedia: MediaInput?,
    onPromptChanged: (String) -> Unit,
    onBack: () -> Unit,
    onOpenWorkshop: () -> Unit,
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

        GlassCard {
            Text(text = "AIGC ACTIONS", style = MaterialTheme.typography.labelLarge, color = AetherColors.Primary)
            Text(text = "Use the same prompt across text, image, and video flows. Provider: ${uiState.activeProvider?.label ?: "Unknown"}", style = MaterialTheme.typography.bodyLarge, color = AetherColors.OnSurfaceVariant)
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChanged,
                modifier = Modifier.fillMaxWidth().testTag("chat-prompt-field"),
                label = { Text("Prompt") },
            )
            if (selectedMedia != null) {
                Text(text = "Selected media: ${selectedMedia.type.name}", style = MaterialTheme.typography.bodyMedium, color = AetherColors.OnSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionChip(label = "Pick image", onClick = onPickImage)
                ActionChip(label = "Pick video", onClick = onPickVideo)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionChip(label = "Text to image") { onRunMode(AigcMode.TextToImage) }
                ActionChip(label = "Image to image") { onRunMode(AigcMode.ImageToImage) }
                ActionChip(label = "Video to video") { onRunMode(AigcMode.VideoToVideo) }
            }
            GradientPrimaryButton(label = "Send text", onClick = onSendMessage)
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
private fun ActionChip(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = AetherColors.OnSurface,
        modifier = Modifier
            .background(AetherColors.SurfaceContainerHigh, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("chat-action-${label.replace(" ", "-")}"),
    )
}

