package com.gkim.im.android.feature.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
    val imHttpBaseUrl: String = "",
    val imWebSocketUrl: String = "",
    val imDevUserExternalId: String = "",
    val imValidationError: String? = null,
    val appLanguage: AppLanguage = AppLanguage.English,
    val themeMode: AppThemeMode = AppThemeMode.Light,
)

private enum class SettingsDestination {
    Menu,
    Appearance,
    AiProvider,
    ImValidation,
    Account,
}

internal class SettingsViewModel(
    private val repository: AigcRepository,
    private val preferencesStore: PreferencesStore,
) : ViewModel() {
    private data class ProviderSettingsState(
        val providers: List<AigcProvider>,
        val activeProviderId: String,
        val customProvider: CustomProviderConfig,
        val appLanguage: AppLanguage,
        val themeMode: AppThemeMode,
    )

    private val imValidationConfig = combine(
        preferencesStore.imHttpBaseUrl,
        preferencesStore.imWebSocketUrl,
        preferencesStore.imDevUserExternalId,
    ) { httpBaseUrl, webSocketUrl, devUserExternalId ->
        Triple(httpBaseUrl, webSocketUrl, devUserExternalId)
    }

    private val providerSettings = combine(
        repository.providers,
        repository.activeProviderId,
        repository.customProvider,
        preferencesStore.appLanguage,
        preferencesStore.appThemeMode,
    ) { providers, activeProviderId, customProvider, appLanguage, themeMode ->
        ProviderSettingsState(
            providers = providers,
            activeProviderId = activeProviderId,
            customProvider = customProvider,
            appLanguage = appLanguage,
            themeMode = themeMode,
        )
    }

    val uiState = combine(providerSettings, imValidationConfig) { providerSettings, validationConfig ->
        val (imHttpBaseUrl, imWebSocketUrl, imDevUserExternalId) = validationConfig
        SettingsUiState(
            providers = providerSettings.providers,
            activeProviderId = providerSettings.activeProviderId,
            customProvider = providerSettings.customProvider,
            imHttpBaseUrl = imHttpBaseUrl,
            imWebSocketUrl = imWebSocketUrl,
            imDevUserExternalId = imDevUserExternalId,
            imValidationError = validationErrorFor(imHttpBaseUrl, imWebSocketUrl, imDevUserExternalId),
            appLanguage = providerSettings.appLanguage,
            themeMode = providerSettings.themeMode,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(
            providers = repository.providers.value,
            activeProviderId = repository.activeProviderId.value,
            customProvider = repository.customProvider.value,
            imHttpBaseUrl = "http://127.0.0.1:18080/",
            imWebSocketUrl = "ws://127.0.0.1:18080/ws",
            imDevUserExternalId = "nox-dev",
            appLanguage = AppLanguage.Chinese,
            themeMode = AppThemeMode.Light,
        ),
    )

    fun setActiveProvider(id: String) = repository.setActiveProvider(id)
    fun updateCustom(baseUrl: String? = null, model: String? = null, apiKey: String? = null) = repository.updateCustomProvider(baseUrl, model, apiKey)
    fun updateImValidationConfig(
        httpBaseUrl: String? = null,
        webSocketUrl: String? = null,
        devUserExternalId: String? = null,
    ) {
        viewModelScope.launch {
            httpBaseUrl?.let { preferencesStore.setImHttpBaseUrl(it) }
            webSocketUrl?.let { preferencesStore.setImWebSocketUrl(it) }
            devUserExternalId?.let { preferencesStore.setImDevUserExternalId(it) }
        }
    }
    fun setAppLanguage(value: AppLanguage) {
        viewModelScope.launch { preferencesStore.setAppLanguage(value) }
    }
    fun setThemeMode(value: AppThemeMode) {
        viewModelScope.launch { preferencesStore.setAppThemeMode(value) }
    }

    private fun validationErrorFor(
        httpBaseUrl: String,
        webSocketUrl: String,
        devUserExternalId: String,
    ): String? {
        val httpValid = httpBaseUrl.startsWith("http://") || httpBaseUrl.startsWith("https://")
        val webSocketValid = webSocketUrl.startsWith("ws://") || webSocketUrl.startsWith("wss://")
        val userValid = devUserExternalId.isNotBlank()
        return if (httpValid && webSocketValid && userValid) {
            null
        } else {
            "IM validation config is incomplete or invalid."
        }
    }
}

@Composable
fun SettingsRoute(navController: NavHostController, container: AppContainer) {
    val viewModel = viewModel<SettingsViewModel>(factory = simpleViewModelFactory {
        SettingsViewModel(container.aigcRepository, container.preferencesStore)
    })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(SettingsDestination.Menu) }
    var baseUrl by remember(uiState.customProvider.baseUrl) { mutableStateOf(uiState.customProvider.baseUrl) }
    var model by remember(uiState.customProvider.model) { mutableStateOf(uiState.customProvider.model) }
    var apiKey by remember(uiState.customProvider.apiKey) { mutableStateOf(uiState.customProvider.apiKey) }
    var imHttpBaseUrl by remember(uiState.imHttpBaseUrl) { mutableStateOf(uiState.imHttpBaseUrl) }
    var imWebSocketUrl by remember(uiState.imWebSocketUrl) { mutableStateOf(uiState.imWebSocketUrl) }
    var imDevUserExternalId by remember(uiState.imDevUserExternalId) { mutableStateOf(uiState.imDevUserExternalId) }

    SettingsScreen(
        uiState = uiState,
        destination = destination,
        baseUrl = baseUrl,
        model = model,
        apiKey = apiKey,
        imHttpBaseUrl = imHttpBaseUrl,
        imWebSocketUrl = imWebSocketUrl,
        imDevUserExternalId = imDevUserExternalId,
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
        onImHttpBaseUrlChanged = {
            imHttpBaseUrl = it
            viewModel.updateImValidationConfig(httpBaseUrl = it)
        },
        onImWebSocketUrlChanged = {
            imWebSocketUrl = it
            viewModel.updateImValidationConfig(webSocketUrl = it)
        },
        onImDevUserExternalIdChanged = {
            imDevUserExternalId = it
            viewModel.updateImValidationConfig(devUserExternalId = it)
        },
        onNavigateToDestination = { destination = it },
        onSelectProvider = viewModel::setActiveProvider,
        onSelectLanguage = viewModel::setAppLanguage,
        onSelectThemeMode = viewModel::setThemeMode,
        onBack = { navController.popBackStack() },
    )
}

@Composable
private fun SettingsScreen(
    uiState: SettingsUiState,
    destination: SettingsDestination,
    baseUrl: String,
    model: String,
    apiKey: String,
    imHttpBaseUrl: String,
    imWebSocketUrl: String,
    imDevUserExternalId: String,
    onBaseUrlChanged: (String) -> Unit,
    onModelChanged: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onImHttpBaseUrlChanged: (String) -> Unit,
    onImWebSocketUrlChanged: (String) -> Unit,
    onImDevUserExternalIdChanged: (String) -> Unit,
    onNavigateToDestination: (SettingsDestination) -> Unit,
    onSelectProvider: (String) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    if (destination != SettingsDestination.Menu) {
        BackHandler { onNavigateToDestination(SettingsDestination.Menu) }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("settings-screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        when (destination) {
            SettingsDestination.Menu -> SettingsMenuScreen(
                uiState = uiState,
                onNavigateToDestination = onNavigateToDestination,
                onBack = onBack,
            )

            SettingsDestination.Appearance -> SettingsAppearanceScreen(
                uiState = uiState,
                onSelectLanguage = onSelectLanguage,
                onSelectThemeMode = onSelectThemeMode,
                onBack = { onNavigateToDestination(SettingsDestination.Menu) },
            )

            SettingsDestination.AiProvider -> SettingsAiProviderScreen(
                uiState = uiState,
                baseUrl = baseUrl,
                model = model,
                apiKey = apiKey,
                onBaseUrlChanged = onBaseUrlChanged,
                onModelChanged = onModelChanged,
                onApiKeyChanged = onApiKeyChanged,
                onSelectProvider = onSelectProvider,
                onBack = { onNavigateToDestination(SettingsDestination.Menu) },
            )

            SettingsDestination.ImValidation -> SettingsImValidationScreen(
                uiState = uiState,
                imHttpBaseUrl = imHttpBaseUrl,
                imWebSocketUrl = imWebSocketUrl,
                imDevUserExternalId = imDevUserExternalId,
                onImHttpBaseUrlChanged = onImHttpBaseUrlChanged,
                onImWebSocketUrlChanged = onImWebSocketUrlChanged,
                onImDevUserExternalIdChanged = onImDevUserExternalIdChanged,
                onBack = { onNavigateToDestination(SettingsDestination.Menu) },
            )

            SettingsDestination.Account -> SettingsAccountScreen(
                onBack = { onNavigateToDestination(SettingsDestination.Menu) },
            )
        }
    }
}

@Composable
private fun SettingsMenuScreen(
    uiState: SettingsUiState,
    onNavigateToDestination: (SettingsDestination) -> Unit,
    onBack: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    val activeProvider = uiState.providers.firstOrNull { it.id == uiState.activeProviderId }
    val providerSummary = activeProvider?.let { provider ->
        appLanguage.pick("${provider.label} · ${provider.model}", "${provider.label} · ${provider.model}")
    } ?: appLanguage.pick("Choose a provider", "选择提供商")
    val appearanceSummary = appLanguage.pick(
        "${uiState.appLanguage.menuEnglishLabel()} · ${uiState.themeMode.menuEnglishLabel()}",
        "${uiState.appLanguage.menuChineseLabel()} · ${uiState.themeMode.menuChineseLabel()}",
    )
    val validationSummary = uiState.imValidationError ?: appLanguage.pick("Ready for IM validation", "已准备好进行 IM 验证")

    PageHeader(
        eyebrow = appLanguage.pick("Settings", "设置"),
        title = appLanguage.pick("Preferences", "偏好设置"),
        description = appLanguage.pick(
            "Open a focused settings surface for appearance, AI infrastructure, IM validation, or upcoming account actions.",
            "选择一个聚焦设置页，分别管理外观、AI 基础设施、IM 验证，以及即将接入的账号动作。",
        ),
        leadingLabel = appLanguage.pick("Back", "返回"),
        onLeading = onBack,
    )

    Column(
        modifier = Modifier.testTag("settings-menu-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsMenuEntry(
            testTag = "settings-menu-appearance",
            label = appLanguage.pick("Appearance & Language", "外观与语言"),
            summary = appearanceSummary,
        ) { onNavigateToDestination(SettingsDestination.Appearance) }
        SettingsMenuEntry(
            testTag = "settings-menu-ai-provider",
            label = appLanguage.pick("AI Provider", "AI 提供商"),
            summary = providerSummary,
        ) { onNavigateToDestination(SettingsDestination.AiProvider) }
        SettingsMenuEntry(
            testTag = "settings-menu-im-validation",
            label = appLanguage.pick("IM Validation", "IM 验证"),
            summary = validationSummary,
        ) { onNavigateToDestination(SettingsDestination.ImValidation) }
        SettingsMenuEntry(
            testTag = "settings-menu-account",
            label = appLanguage.pick("Account", "账号"),
            summary = appLanguage.pick("Login and registration actions staged for onboarding.", "登录和注册动作已预留，待欢迎页接入后启用。"),
        ) { onNavigateToDestination(SettingsDestination.Account) }
    }
}

@Composable
private fun SettingsAppearanceScreen(
    uiState: SettingsUiState,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    PageHeader(
        eyebrow = appLanguage.pick("Settings", "设置"),
        title = appLanguage.pick("Appearance & Language", "外观与语言"),
        description = appLanguage.pick(
            "Choose the app language and theme used across the authenticated shell.",
            "配置整个应用壳层的语言和主题。",
        ),
        leadingLabel = appLanguage.pick("Back", "返回"),
        onLeading = onBack,
    )

    Column(
        modifier = Modifier.testTag("settings-detail-appearance"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
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
    }
}

@Composable
private fun SettingsAiProviderScreen(
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
    val appLanguage = LocalAppLanguage.current
    PageHeader(
        eyebrow = appLanguage.pick("Settings", "设置"),
        title = appLanguage.pick("AI Provider", "AI 提供商"),
        description = appLanguage.pick(
            "Switch between presets or wire a custom OpenAI-compatible gateway for generation requests.",
            "切换预设模型提供商，或接入兼容 OpenAI 的自定义生成网关。",
        ),
        leadingLabel = appLanguage.pick("Back", "返回"),
        onLeading = onBack,
    )

    Column(
        modifier = Modifier.testTag("settings-detail-ai-provider"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
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
    }
}

@Composable
private fun SettingsImValidationScreen(
    uiState: SettingsUiState,
    imHttpBaseUrl: String,
    imWebSocketUrl: String,
    imDevUserExternalId: String,
    onImHttpBaseUrlChanged: (String) -> Unit,
    onImWebSocketUrlChanged: (String) -> Unit,
    onImDevUserExternalIdChanged: (String) -> Unit,
    onBack: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    PageHeader(
        eyebrow = appLanguage.pick("Settings", "设置"),
        title = appLanguage.pick("IM Validation", "IM 验证"),
        description = appLanguage.pick(
            "Point the emulator shell at the IM backend endpoints used for validation and troubleshooting.",
            "配置模拟器连接 IM 后端时使用的验证与排障地址。",
        ),
        leadingLabel = appLanguage.pick("Back", "返回"),
        onLeading = onBack,
    )

    Column(
        modifier = Modifier.testTag("settings-detail-im-validation"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        GlassCard {
            Text(text = appLanguage.pick("IM VALIDATION", "IM 验证"), style = MaterialTheme.typography.labelLarge, color = AetherColors.Primary)
            OutlinedTextField(
                value = imHttpBaseUrl,
                onValueChange = onImHttpBaseUrlChanged,
                modifier = Modifier.fillMaxWidth().testTag("settings-im-http-base-url"),
                label = { Text(appLanguage.pick("HTTP Base URL", "HTTP 基础 URL")) },
            )
            OutlinedTextField(
                value = imWebSocketUrl,
                onValueChange = onImWebSocketUrlChanged,
                modifier = Modifier.fillMaxWidth().testTag("settings-im-websocket-url"),
                label = { Text(appLanguage.pick("WebSocket URL", "WebSocket URL")) },
            )
            OutlinedTextField(
                value = imDevUserExternalId,
                onValueChange = onImDevUserExternalIdChanged,
                modifier = Modifier.fillMaxWidth().testTag("settings-im-dev-user"),
                label = { Text(appLanguage.pick("Dev User", "开发用户")) },
            )
            Text(
                text = uiState.imValidationError ?: appLanguage.pick("Ready for IM validation", "已准备好进行 IM 验证"),
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.imValidationError == null) AetherColors.Primary else AetherColors.OnSurfaceVariant,
                modifier = Modifier.testTag("settings-im-validation-status"),
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
private fun SettingsAccountScreen(
    onBack: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    PageHeader(
        eyebrow = appLanguage.pick("Settings", "设置"),
        title = appLanguage.pick("Account", "账号"),
        description = appLanguage.pick(
            "Account onboarding actions are staged here so the welcome/auth flow can plug into the same settings surface in the next phase.",
            "账号引导动作会从这里接入，下一阶段欢迎页和鉴权流会复用这一设置入口。",
        ),
        leadingLabel = appLanguage.pick("Back", "返回"),
        onLeading = onBack,
    )

    Column(
        modifier = Modifier.testTag("settings-detail-account"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        GlassCard {
            Text(text = appLanguage.pick("SESSION", "会话"), style = MaterialTheme.typography.labelLarge, color = AetherColors.Primary)
            Text(
                text = appLanguage.pick(
                    "The shell is still using the temporary development session seam until the authenticated welcome flow lands.",
                    "在欢迎页鉴权流接入前，当前壳层仍然使用临时开发会话过渡。",
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = AetherColors.OnSurfaceVariant,
            )
        }

        GlassCard {
            Text(text = appLanguage.pick("ACCOUNT ACTIONS", "账号动作"), style = MaterialTheme.typography.labelLarge, color = AetherColors.Primary)
            Text(
                text = appLanguage.pick(
                    "These actions are visible now so the shell information architecture is ready before the real auth flows arrive.",
                    "先把入口形态落位，后续接入真实注册/登录流程时就不需要再次重做设置结构。",
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = AetherColors.OnSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings-account-login"),
                ) {
                    Text(appLanguage.pick("Login", "登录"))
                }
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings-account-register"),
                ) {
                    Text(appLanguage.pick("Register", "注册"))
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuEntry(
    testTag: String,
    label: String,
    summary: String,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick),
    ) {
        Text(text = label, style = MaterialTheme.typography.headlineMedium, color = AetherColors.OnSurface)
        Text(text = summary, style = MaterialTheme.typography.bodyLarge, color = AetherColors.OnSurfaceVariant)
    }
}

private fun AppLanguage.menuEnglishLabel(): String = when (this) {
    AppLanguage.English -> "English"
    AppLanguage.Chinese -> "Chinese"
}

private fun AppLanguage.menuChineseLabel(): String = when (this) {
    AppLanguage.English -> "英文"
    AppLanguage.Chinese -> "中文"
}

private fun AppThemeMode.menuEnglishLabel(): String = when (this) {
    AppThemeMode.Dark -> "Dark"
    AppThemeMode.Light -> "Light"
}

private fun AppThemeMode.menuChineseLabel(): String = when (this) {
    AppThemeMode.Dark -> "深色"
    AppThemeMode.Light -> "浅色"
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
