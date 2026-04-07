package com.gkim.im.android.feature.space

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.PageHeader
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.FeedPost
import com.gkim.im.android.core.model.RichDocument
import com.gkim.im.android.core.rendering.MarkdownDocumentParser
import com.gkim.im.android.core.rendering.RichContentRenderer
import com.gkim.im.android.core.util.formatRelativeLabel
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.FeedRepository
import com.gkim.im.android.data.repository.MessagingRepository
import com.gkim.im.android.feature.shared.simpleViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private data class SpacePostUi(val post: FeedPost, val document: RichDocument)
private data class SpaceUiState(
    val posts: List<SpacePostUi> = emptyList(),
    val totalUnread: Int = 0,
)

private class SpaceViewModel(
    feedRepository: FeedRepository,
    messagingRepository: MessagingRepository,
    parser: MarkdownDocumentParser,
) : ViewModel() {
    val uiState = combine(feedRepository.posts, messagingRepository.conversations) { posts, conversations ->
        SpaceUiState(
            posts = posts.map { post -> SpacePostUi(post, parser.parse(post.body, mdxReady = true, cssHint = "architectural-glitch")) },
            totalUnread = conversations.sumOf { it.unreadCount },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceUiState())
}

@Composable
fun SpaceRoute(navController: NavHostController, container: AppContainer) {
    val viewModel = viewModel<SpaceViewModel>(factory = simpleViewModelFactory {
        SpaceViewModel(container.feedRepository, container.messagingRepository, container.markdownParser)
    })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SpaceScreen(uiState = uiState, onOpenWorkshop = { navController.navigate("workshop") })
}

@Composable
private fun SpaceScreen(uiState: SpaceUiState, onOpenWorkshop: () -> Unit) {
    val appLanguage = LocalAppLanguage.current
    Column(
        modifier = Modifier
            .background(AetherColors.Surface)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("space-screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PageHeader(
            eyebrow = appLanguage.pick("Builder Feed", "创作者动态"),
            title = appLanguage.pick("Space", "空间"),
            description = appLanguage.pick(
                "Developer-first posts, prompt breakdowns, and community knowledge rendered through the shared Markdown pipeline.",
                "这里展示以开发者为中心的帖子、提示词拆解与社区知识，并统一走 Markdown 内容渲染链路。",
            ),
            actionLabel = appLanguage.pick("Workshop", "工作台"),
            onAction = onOpenWorkshop,
        )

        GlassCard(modifier = Modifier.testTag("space-unread-summary")) {
            Text(
                text = appLanguage.pick("Unread signals", "未读信号"),
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.Primary,
            )
            Text(
                text = if (appLanguage == com.gkim.im.android.core.model.AppLanguage.Chinese) {
                    "活跃会话中共有 ${uiState.totalUnread} 条未读"
                } else {
                    "${uiState.totalUnread} total across active conversations"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = AetherColors.OnSurface,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val filters = listOf(
                appLanguage.pick("For You", "为你推荐"),
                appLanguage.pick("Prompting", "提示工程"),
                appLanguage.pick("AI Tools", "AI 工具"),
                appLanguage.pick("Motion", "动态"),
            )
            filters.forEachIndexed { index, label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (index == 0) AetherColors.Surface else AetherColors.OnSurfaceVariant,
                    modifier = Modifier
                        .background(if (index == 0) AetherColors.Primary else AetherColors.SurfaceContainerHigh, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.testTag("space-feed")) {
            items(uiState.posts, key = { it.post.id }) { post ->
                FeedPostCard(post)
            }
        }
    }
}

@Composable
private fun FeedPostCard(item: SpacePostUi) {
    GlassCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = item.post.author.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = when (item.post.accent) {
                        AccentTone.Primary -> AetherColors.Primary
                        AccentTone.Secondary -> AetherColors.Secondary
                        AccentTone.Tertiary -> AetherColors.Tertiary
                    },
                )
                Text(text = item.post.title, style = MaterialTheme.typography.headlineMedium, color = AetherColors.OnSurface)
                Text(text = item.post.summary, style = MaterialTheme.typography.bodyLarge, color = AetherColors.OnSurfaceVariant)
            }
            Text(text = formatRelativeLabel(item.post.createdAt), style = MaterialTheme.typography.bodyMedium, color = AetherColors.OnSurfaceVariant)
        }
        RichContentRenderer(document = item.document)
    }
}

