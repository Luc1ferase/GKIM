package com.gkim.im.android.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.PageHeader
import com.gkim.im.android.core.designsystem.pick
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
        onSelectLanguage = viewModel::setAppLanguage,
        onSelectThemeMode = viewModel::setThemeMode,
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
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
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
            eyebrow = appLanguage.pick("Infrastructure", "基础设施"),
            title = appLanguage.pick("AI Settings", "AI 设置"),
            description = appLanguage.pick(
                "Switch between preset AI providers or wire a custom OpenAI-compatible gateway for AIGC requests.",
                "可以切换预设 AI 提供商，或接入兼容 OpenAI 的自定义网关来执行 AIGC 请求。",
            ),
            leadingLabel = appLanguage.pick("Back", "返回"),
            onLeading = onBack,
        )

        GlassCard {
            Text(text = appLanguage.pick("LANGUAGE", "语言"), style = MaterialTheme.typography.labelLarge, color = AetherColors.Primary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsOptionPill(
                    label = "English",
                    selected = uiState.appLanguage == AppLanguage.English,
                    testTag = "settings-language-english",
                ) { onSelectLanguage(AppLanguage.English) }
                SettingsOptionPill(
                    label = "中文",
                    selected = uiState.appLanguage == AppLanguage.Chinese,
                    testTag = "settings-language-chinese",
                ) { onSelectLanguage(AppLanguage.Chinese) }
            }
        }

        GlassCard {
            Text(text = appLanguage.pick("APPEARANCE", "外观"), style = MaterialTheme.typography.labelLarge, color = AetherColors.Primary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsOptionPill(
                    label = appLanguage.pick("Dark", "深色"),
                    selected = uiState.themeMode == AppThemeMode.Dark,
                    testTag = "settings-theme-dark",
                ) { onSelectThemeMode(AppThemeMode.Dark) }
                SettingsOptionPill(
                    label = appLanguage.pick("Light", "浅色"),
                    selected = uiState.themeMode == AppThemeMode.Light,
                    testTag = "settings-theme-light",
                ) { onSelectThemeMode(AppThemeMode.Light) }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            uiState.providers.forEach { provider ->
                val selected = provider.id == uiState.activeProviderId
                GlassCard(modifier = Modifier.testTag("settings-provider-${provider.id}").clickable { onSelectProvider(provider.id) }) {
                    Text(text = provider.vendor.uppercase(), style = MaterialTheme.typography.labelLarge, color = if (selected) AetherColors.Primary else AetherColors.OnSurfaceVariant)
                    Text(text = provider.label, style = MaterialTheme.typography.headlineMedium, color = AetherColors.OnSurface)
                    Text(text = provider.description, style = MaterialTheme.typography.bodyLarge, color = AetherColors.OnSurfaceVariant)
                    Text(
                        text = appLanguage.pick("Model", "模型") + " · ${provider.model}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AetherColors.OnSurfaceVariant,
                    )
                }
            }
        }

        GlassCard {
            Text(text = appLanguage.pick("CUSTOM ENDPOINT", "自定义端点"), style = MaterialTheme.typography.labelLarge, color = AetherColors.Tertiary)
            OutlinedTextField(
                value = baseUrl,
                onValueChange = onBaseUrlChanged,
                modifier = Modifier.fillMaxWidth().testTag("settings-base-url"),
                label = { Text(appLanguage.pick("Base URL", "基础 URL")) },
            )
            OutlinedTextField(
                value = model,
                onValueChange = onModelChanged,
                modifier = Modifier.fillMaxWidth().testTag("settings-model"),
                label = { Text(appLanguage.pick("Model", "模型")) },
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChanged,
                modifier = Modifier.fillMaxWidth().testTag("settings-api-key"),
                label = { Text(appLanguage.pick("API Key", "API 密钥")) },
            )
        }

        GlassCard {
            Text(text = appLanguage.pick("BACKEND-ONLY DATABASE NOTE", "仅后端数据库说明"), style = MaterialTheme.typography.labelLarge, color = AetherColors.Primary)
            Text(
                text = appLanguage.pick(
                    "Backend services can target 124.222.15.128:5432 through secret-managed environment values. PostgreSQL credentials and any optional TLS trust material stay on the backend and are never packaged into the Android client.",
                    "后端服务可以通过受密钥管理的环境变量连接到 124.222.15.128:5432。PostgreSQL 凭据以及任何可选 TLS 信任材料都只保留在后端，绝不会打包进 Android 客户端。",
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = AetherColors.OnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsOptionPill(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = if (selected) AetherColors.Surface else AetherColors.OnSurface,
        modifier = Modifier
            .testTag(testTag)
            .background(if (selected) AetherColors.Primary else AetherColors.SurfaceContainerHigh, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}
