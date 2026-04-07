package com.gkim.im.android.feature.workshop

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
import androidx.compose.material3.OutlinedTextField
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
import com.gkim.im.android.core.designsystem.PageHeader
import com.gkim.im.android.core.designsystem.PillAction
import com.gkim.im.android.core.model.PromptCategory
import com.gkim.im.android.core.model.WorkshopPrompt
import com.gkim.im.android.data.repository.AigcRepository
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.FeedRepository
import com.gkim.im.android.feature.shared.simpleViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private data class WorkshopUiState(
    val prompts: List<WorkshopPrompt> = emptyList(),
    val promptCategory: PromptCategory = PromptCategory.All,
    val promptQuery: String = "",
)

private class WorkshopViewModel(
    private val feedRepository: FeedRepository,
    private val aigcRepository: AigcRepository,
) : ViewModel() {
    val uiState = combine(feedRepository.filteredPrompts, feedRepository.promptCategory, feedRepository.promptQuery) { prompts, category, query ->
        WorkshopUiState(prompts = prompts, promptCategory = category, promptQuery = query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkshopUiState())

    fun setCategory(category: PromptCategory) = feedRepository.setPromptCategory(category)
    fun setQuery(query: String) = feedRepository.setPromptQuery(query)
    fun applyPrompt(prompt: WorkshopPrompt) {
        aigcRepository.updateDraft(com.gkim.im.android.core.model.DraftAigcRequest(prompt = prompt.prompt))
    }
}

@Composable
fun WorkshopRoute(navController: NavHostController, container: AppContainer) {
    val viewModel = viewModel<WorkshopViewModel>(factory = simpleViewModelFactory {
        WorkshopViewModel(container.feedRepository, container.aigcRepository)
    })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WorkshopScreen(
        uiState = uiState,
        onQueryChanged = viewModel::setQuery,
        onCategorySelected = viewModel::setCategory,
        onApplyPrompt = {
            viewModel.applyPrompt(it)
            navController.navigate("chat/studio")
        },
        onBack = { navController.popBackStack() },
        onOpenSettings = { navController.navigate("settings") },
    )
}

@Composable
private fun WorkshopScreen(
    uiState: WorkshopUiState,
    onQueryChanged: (String) -> Unit,
    onCategorySelected: (PromptCategory) -> Unit,
    onApplyPrompt: (WorkshopPrompt) -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val categories = listOf(PromptCategory.All, PromptCategory.Portrait, PromptCategory.Video, PromptCategory.Cyberpunk, PromptCategory.CodeArt)

    Column(
        modifier = Modifier
            .background(AetherColors.Surface)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("workshop-screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PageHeader(
            eyebrow = "Community Prompt Hub",
            title = "Workshop",
            description = "Prompt templates for users who want a strong starting point before they write their own creative direction.",
            leadingLabel = "Back",
            onLeading = onBack,
            actionLabel = "Settings",
            onAction = onOpenSettings,
        )

        OutlinedTextField(
            value = uiState.promptQuery,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth().testTag("workshop-query"),
            label = { Text("Search prompts") },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            categories.forEach { category ->
                val selected = uiState.promptCategory == category
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) AetherColors.Surface else AetherColors.OnSurfaceVariant,
                    modifier = Modifier
                        .background(if (selected) AetherColors.Primary else AetherColors.SurfaceContainerHigh, CircleShape)
                        .clickable { onCategorySelected(category) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.testTag("workshop-prompts")) {
            items(uiState.prompts, key = { it.id }) { prompt ->
                GlassCard {
                    Text(text = prompt.category.name.uppercase(), style = MaterialTheme.typography.labelLarge, color = AetherColors.Tertiary)
                    Text(text = prompt.title, style = MaterialTheme.typography.headlineMedium, color = AetherColors.OnSurface)
                    Text(text = prompt.summary, style = MaterialTheme.typography.bodyLarge, color = AetherColors.OnSurfaceVariant)
                    Text(text = prompt.prompt, style = MaterialTheme.typography.bodyLarge, color = AetherColors.OnSurface)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "By ${prompt.author} · ${prompt.uses} uses", style = MaterialTheme.typography.bodyMedium, color = AetherColors.OnSurfaceVariant)
                        PillAction(label = "Apply") { onApplyPrompt(prompt) }
                    }
                }
            }
        }
    }
}
