package com.gkim.im.android.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.PillAction
import com.gkim.im.android.core.model.Conversation
import com.gkim.im.android.core.util.formatChatTimestamp
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.MessagingRepository
import com.gkim.im.android.feature.shared.simpleViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal data class MessagesUiState(
    val conversations: List<Conversation> = emptyList(),
    val totalUnread: Int = 0,
)

internal class MessagesViewModel(repository: MessagingRepository) : ViewModel() {
    val uiState = repository.conversations.map { conversations ->
        MessagesUiState(
            conversations = conversations,
            totalUnread = conversations.sumOf { it.unreadCount },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MessagesUiState())
}

@Composable
fun MessagesRoute(navController: NavHostController, container: AppContainer) {
    val viewModel = viewModel<MessagesViewModel>(factory = simpleViewModelFactory { MessagesViewModel(container.messagingRepository) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MessagesScreen(
        uiState = uiState,
        onOpenConversation = { navController.navigate("chat/$it") },
        onOpenSettings = { navController.navigate("settings") },
    )
}

@Composable
private fun MessagesScreen(
    uiState: MessagesUiState,
    onOpenConversation: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherColors.Surface)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("messages-screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Recent conversations", style = MaterialTheme.typography.titleLarge, color = AetherColors.OnSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${uiState.conversations.size} active", style = MaterialTheme.typography.bodyMedium, color = AetherColors.OnSurfaceVariant)
                PillAction(label = "Settings", onClick = onOpenSettings)
            }
        }

        if (uiState.conversations.isEmpty()) {
            GlassCard(modifier = Modifier.testTag("messages-empty")) {
                Text(text = "No active rooms yet", style = MaterialTheme.typography.headlineMedium, color = AetherColors.OnSurface)
                Text(text = "Seed data, websocket sync, or imported contacts will populate this lane once connected.", style = MaterialTheme.typography.bodyLarge, color = AetherColors.OnSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .weight(1f, fill = true)
                    .testTag("messages-list"),
            ) {
                items(uiState.conversations, key = { it.id }) { conversation ->
                    ConversationRow(conversation = conversation, onClick = { onOpenConversation(conversation.id) })
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.testTag("conversation-row-${conversation.id}").clickable(onClick = onClick)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(AetherColors.Primary.copy(alpha = 0.14f), CircleShape)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = conversation.avatarText, color = AetherColors.Primary, style = MaterialTheme.typography.titleLarge)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = conversation.contactName,
                        style = MaterialTheme.typography.titleLarge,
                        color = AetherColors.OnSurface,
                        modifier = Modifier.testTag("conversation-name-${conversation.id}"),
                    )
                    Text(
                        text = formatChatTimestamp(conversation.lastTimestamp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AetherColors.OnSurfaceVariant,
                        modifier = Modifier.testTag("conversation-time-${conversation.id}"),
                    )
                }
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AetherColors.OnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("conversation-preview-${conversation.id}"),
                )
            }
            if (conversation.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .background(AetherColors.PrimaryContainer, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = conversation.unreadCount.toString(),
                        color = AetherColors.OnSurface,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.testTag("conversation-unread-${conversation.id}"),
                    )
                }
            }
        }
    }
}

