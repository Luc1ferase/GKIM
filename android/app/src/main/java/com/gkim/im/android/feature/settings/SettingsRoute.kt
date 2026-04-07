package com.gkim.im.android.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.AppThemeMode
import com.gkim.im.android.core.model.AigcProvider
import com.gkim.im.android.core.model.CustomProviderConfig
import com.gkim.im.android.data.local.PreferencesStore
import com.gkim.im.android.data.repository.AigcRepository
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.feature.shared.simpleViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class SettingsUiState(
    val providers: List<AigcProvider> = emptyList(),
    val activeProviderId: String = "",
    val customProvider: CustomProviderConfig = CustomProviderConfig("", "", ""),
    val appLanguage: AppLanguage = AppLanguage.English,
    val themeMode: AppThemeMode = AppThemeMode.Dark,
)

internal class SettingsViewModel(
    private val repository: AigcRepository,
    private val preferencesStore: PreferencesStore,
) : ViewModel() {
    val uiState = combine(
        repository.providers,
        repository.activeProviderId,
        repository.customProvider,
        preferencesStore.appLanguage,
        preferencesStore.appThemeMode,
    ) { providers, activeProviderId, customProvider, appLanguage, themeMode ->
        SettingsUiState(
            providers = providers,
            activeProviderId = activeProviderId,
            customProvider = customProvider,
            appLanguage = appLanguage,
            themeMode = themeMode,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(
            providers = repository.providers.value,
            activeProviderId = repository.activeProviderId.value,
            customProvider = repository.customProvider.value,
            appLanguage = AppLanguage.English,
            themeMode = AppThemeMode.Dark,
        ),
    )

    fun setActiveProvider(id: String) = repository.setActiveProvider(id)
    fun updateCustom(baseUrl: String? = null, model: String? = null, apiKey: String? = null) = repository.updateCustomProvider(baseUrl, model, apiKey)
    fun setAppLanguage(value: AppLanguage) {
        viewModelScope.launch { preferencesStore.setAppLanguage(value) }
    }
    fun setThemeMode(value: AppThemeMode) {
        viewModelScope.launch { preferencesStore.setAppThemeMode(value) }
    }
}

@Composable
fun SettingsRoute(navController: NavHostController, container: AppContainer) {
    val viewModel = viewModel<SettingsViewModel>(factory = simpleViewModelFactory {
        SettingsViewModel(container.aigcRepository, container.preferencesStore)
    })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var baseUrl by remember(uiState.customProvider.baseUrl) { mutableStateOf(uiState.customProvider.baseUrl) }
    var model by remember(uiState.customProvider.model) { mutableStateOf(uiState.customProvider.model) }
    var apiKey by remember(uiState.customProvider.apiKey) { mutableStateOf(uiState.customProvider.apiKey) }

    SettingsScreen(
        uiState = uiState,
        baseUrl = baseUrl,
        model = model,
        apiKey = apiKey,
        onBaseUrlChanged = {
            baseUrl = it
            viewModel.updateCustom(baseUrl = it)
        },
        onModelChanged = {
            model = it
            viewModel.updateCustom(model = it)
        },
        onApiKeyChanged = {
            apiKey = it
            viewModel.updateCustom(apiKey = it)
        },
        onSelectProvider = viewModel::setActiveProvider,
        onBack = { navController.popBackStack() },
    )
}

@Composable
private fun SettingsScreen(
    uiState: SettingsUiState,
    baseUrl: String,
    model: String,
    apiKey: String,
    onBaseUrlChanged: (String) -> Unit,
    onModelChanged: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onSelectProvider: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("settings-screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PageHeader(
            eyebrow = "Infrastructure",
            title = "AI Settings",
            description = "Switch between preset AI providers or wire a custom OpenAI-compatible gateway for AIGC requests.",
            leadingLabel = "Back",
            onLeading = onBack,
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            uiState.providers.forEach { provider ->
                val selected = provider.id == uiState.activeProviderId
                GlassCard(modifier = Modifier.testTag("settings-provider-${provider.id}").clickable { onSelectProvider(provider.id) }) {
                    Text(text = provider.vendor.uppercase(), style = MaterialTheme.typography.labelLarge, color = if (selected) AetherColors.Primary else AetherColors.OnSurfaceVariant)
                    Text(text = provider.label, style = MaterialTheme.typography.headlineMedium, color = AetherColors.OnSurface)
                    Text(text = provider.description, style = MaterialTheme.typography.bodyLarge, color = AetherColors.OnSurfaceVariant)
                    Text(text = "Model · ${provider.model}", style = MaterialTheme.typography.bodyMedium, color = AetherColors.OnSurfaceVariant)
                }
            }
        }

        GlassCard {
            Text(text = "CUSTOM ENDPOINT", style = MaterialTheme.typography.labelLarge, color = AetherColors.Tertiary)
            OutlinedTextField(value = baseUrl, onValueChange = onBaseUrlChanged, modifier = Modifier.fillMaxWidth().testTag("settings-base-url"), label = { Text("Base URL") })
            OutlinedTextField(value = model, onValueChange = onModelChanged, modifier = Modifier.fillMaxWidth().testTag("settings-model"), label = { Text("Model") })
            OutlinedTextField(value = apiKey, onValueChange = onApiKeyChanged, modifier = Modifier.fillMaxWidth().testTag("settings-api-key"), label = { Text("API Key") })
        }

        GlassCard {
            Text(text = "BACKEND-ONLY DATABASE NOTE", style = MaterialTheme.typography.labelLarge, color = AetherColors.Primary)
            Text(text = "The Postgres DSN and CA certificate belong to backend infrastructure and are not packaged into the Android client. Future server-side trust material should live under infra/certs/.", style = MaterialTheme.typography.bodyLarge, color = AetherColors.OnSurfaceVariant)
        }
    }
}
