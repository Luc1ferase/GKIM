package com.gkim.im.android.feature.space

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.gkim.im.android.core.model.DraftAigcRequest
import com.gkim.im.android.core.model.FeedPost
import com.gkim.im.android.core.model.RichDocument
import com.gkim.im.android.core.model.WorkshopPrompt
import com.gkim.im.android.core.rendering.MarkdownDocumentParser
import com.gkim.im.android.core.rendering.RichContentRenderer
import com.gkim.im.android.core.util.formatRelativeLabel
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.FeedRepository
import com.gkim.im.android.feature.shared.simpleViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private data class SpacePostUi(val post: FeedPost, val document: RichDocument)
private enum class SpaceDiscoveryFilter {
    ForYou,
    Prompting,
    AiTools,
    Activity,
}

private sealed interface SpaceFeedItem {
    val stableId: String

    data class Post(val item: SpacePostUi) : SpaceFeedItem {
        override val stableId: String = "post-${item.post.id}"
    }

    data class Prompt(val prompt: WorkshopPrompt) : SpaceFeedItem {
        override val stableId: String = "prompt-${prompt.id}"
    }
}

private data class SpaceUiState(
    val selectedFilter: SpaceDiscoveryFilter = SpaceDiscoveryFilter.ForYou,
    val items: List<SpaceFeedItem> = emptyList(),
)

private class SpaceViewModel(
    feedRepository: FeedRepository,
    parser: MarkdownDocumentParser,
) : ViewModel() {
    private val selectedFilter = MutableStateFlow(SpaceDiscoveryFilter.ForYou)
    private val promptingPrompts = combine(feedRepository.prompts, selectedFilter) { prompts, filter ->
        when (filter) {
            SpaceDiscoveryFilter.ForYou -> prompts
            SpaceDiscoveryFilter.Prompting -> prompts
            SpaceDiscoveryFilter.AiTools -> prompts
            SpaceDiscoveryFilter.Activity -> prompts
        }
    }

    val uiState = combine(feedRepository.posts, promptingPrompts, selectedFilter) { posts, prompts, filter ->
        val parsedPosts = posts.map { post ->
            SpacePostUi(post, parser.parse(post.body, mdxReady = true, cssHint = "architectural-glitch"))
        }
        SpaceUiState(
            selectedFilter = filter,
            items = when (filter) {
                SpaceDiscoveryFilter.ForYou -> mergeDiscoveryItems(parsedPosts, prompts)
                SpaceDiscoveryFilter.Prompting -> prompts.map { prompt -> SpaceFeedItem.Prompt(prompt) }
                SpaceDiscoveryFilter.AiTools -> mergeDiscoveryItems(parsedPosts, prompts)
                SpaceDiscoveryFilter.Activity -> mergeDiscoveryItems(parsedPosts, prompts)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceUiState())

    fun selectFilter(filter: SpaceDiscoveryFilter) {
        selectedFilter.value = filter
    }

    private fun mergeDiscoveryItems(
        posts: List<SpacePostUi>,
        prompts: List<WorkshopPrompt>,
    ): List<SpaceFeedItem> {
        val merged = mutableListOf<SpaceFeedItem>()
        val maxCount = maxOf(posts.size, prompts.size)
        repeat(maxCount) { index ->
            posts.getOrNull(index)?.let { merged += SpaceFeedItem.Post(it) }
            prompts.getOrNull(index)?.let { merged += SpaceFeedItem.Prompt(it) }
        }
        return merged
    }
}

@Composable
fun SpaceRoute(navController: NavHostController, container: AppContainer) {
    val viewModel = viewModel<SpaceViewModel>(factory = simpleViewModelFactory {
        SpaceViewModel(container.feedRepository, container.markdownParser)
    })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SpaceScreen(
        uiState = uiState,
        onSelectFilter = viewModel::selectFilter,
        onApplyPrompt = { prompt ->
            container.aigcRepository.updateDraft(DraftAigcRequest(prompt = prompt.prompt))
            navController.navigate("chat/studio")
        },
    )
}

@Composable
private fun SpaceScreen(
    uiState: SpaceUiState,
    onSelectFilter: (SpaceDiscoveryFilter) -> Unit,
    onApplyPrompt: (WorkshopPrompt) -> Unit,
) {
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
                "Developer posts and prompt templates now live in one discovery surface with the same waterfall browsing rhythm.",
                "开发者帖子与提示模板现在合并进同一个发现面，并统一使用同一种瀑布流浏览节奏。",
            ),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val filters = listOf(
                Triple(
                    SpaceDiscoveryFilter.ForYou,
                    "space-filter-for-you",
                    appLanguage.pick("For You", "为你推荐"),
                ),
                Triple(
                    SpaceDiscoveryFilter.Prompting,
                    "space-filter-prompting",
                    appLanguage.pick("Prompting", "提示工程"),
                ),
                Triple(
                    SpaceDiscoveryFilter.AiTools,
                    "space-filter-ai-tools",
                    appLanguage.pick("AI Tools", "AI 工具"),
                ),
                Triple(
                    SpaceDiscoveryFilter.Activity,
                    "space-filter-activity",
                    appLanguage.pick("Motion", "动态"),
                ),
            )
            filters.forEach { (filter, testTag, label) ->
                val selected = uiState.selectedFilter == filter
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) AetherColors.Surface else AetherColors.OnSurfaceVariant,
                    modifier = Modifier
                        .testTag(testTag)
                        .background(if (selected) AetherColors.Primary else AetherColors.SurfaceContainerHigh, CircleShape)
                        .clickable { onSelectFilter(filter) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.testTag("space-feed")) {
            items(uiState.items, key = { it.stableId }) { item ->
                when (item) {
                    is SpaceFeedItem.Post -> FeedPostCard(item.item)
                    is SpaceFeedItem.Prompt -> FeedPromptCard(item.prompt, onApplyPrompt)
                }
            }
        }
    }
}

@Composable
private fun FeedPostCard(item: SpacePostUi) {
    GlassCard(modifier = Modifier.testTag("space-feed-item-post-${item.post.id}")) {
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

@Composable
private fun FeedPromptCard(
    prompt: WorkshopPrompt,
    onApplyPrompt: (WorkshopPrompt) -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    GlassCard(modifier = Modifier.testTag("space-feed-item-prompt-${prompt.id}")) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = appLanguage.pick("PROMPT TEMPLATE", "提示模板"),
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.Tertiary,
                )
                Text(text = prompt.title, style = MaterialTheme.typography.headlineMedium, color = AetherColors.OnSurface)
                Text(text = prompt.summary, style = MaterialTheme.typography.bodyLarge, color = AetherColors.OnSurfaceVariant)
            }
            Text(
                text = prompt.category.name.uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                color = AetherColors.OnSurfaceVariant,
            )
        }
        Text(text = prompt.prompt, style = MaterialTheme.typography.bodyLarge, color = AetherColors.OnSurface)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = appLanguage.pick("By ${prompt.author} · ${prompt.uses} uses", "作者 ${prompt.author} · ${prompt.uses} 次使用"),
                style = MaterialTheme.typography.bodyMedium,
                color = AetherColors.OnSurfaceVariant,
            )
            SpacePromptAction(
                label = appLanguage.pick("Apply", "应用"),
                testTag = "space-apply-prompt-${prompt.id}",
            ) {
                onApplyPrompt(prompt)
            }
        }
    }
}

@Composable
private fun SpacePromptAction(
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = AetherColors.OnSurface,
        modifier = Modifier
            .testTag(testTag)
            .background(AetherColors.SurfaceContainerHigh, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

