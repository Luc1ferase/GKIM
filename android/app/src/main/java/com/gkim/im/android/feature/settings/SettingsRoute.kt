package com.gkim.im.android.feature.settings

import com.gkim.im.android.BuildConfig
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
import com.gkim.im.android.core.model.PresetProviderConfig
import com.gkim.im.android.core.util.messagingIntegrationStatusLabel
import com.gkim.im.android.data.local.PreferencesStore
import com.gkim.im.android.data.remote.im.ImHttpEndpointResolver
import com.gkim.im.android.data.repository.AigcRepository
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.MessagingIntegrationPhase
import com.gkim.im.android.data.repository.MessagingIntegrationState
import com.gkim.im.android.data.repository.MessagingRepository
import com.gkim.im.android.feature.shared.simpleViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class SettingsUiState(
    val providers: List<AigcProvider> = emptyList(),
    val activeProviderId: String = "",
    val customProvider: CustomProviderConfig = CustomProviderConfig("", "", ""),
    val activePresetProviderConfig: PresetProviderConfig? = null,
    val imResolvedBackendOrigin: String = "",
    val imResolvedWebSocketUrl: String = "",
    val imDeveloperOverrideOrigin: String = "",
    val imDevUserExternalId: String = "",
    val imValidationError: String? = null,
    val messagingIntegrationState: MessagingIntegrationState = MessagingIntegrationState(),
    val appLanguage: AppLanguage = AppLanguage.English,
    val themeMode: AppThemeMode = AppThemeMode.Light,
)

internal enum class SettingsDestination {
    Menu,
    Appearance,
    AiProvider,
    ImValidation,
    Personas,
    PersonaEditor,
    WorldInfo,
    Account,
}

internal data class SettingsMenuItem(
    val destination: SettingsDestination,
    val testTag: String,
    val englishLabel: String,
    val chineseLabel: String,
    val englishSummary: String,
    val chineseSummary: String,
)

internal fun buildSettingsMenuItems(
    uiState: SettingsUiState,
): List<SettingsMenuItem> {
    val activeProvider = uiState.providers.firstOrNull { it.id == uiState.activeProviderId }
    val providerEnglishSummary = activeProvider?.let { "${it.label} · ${it.model}" } ?: "Choose a provider"
    val providerChineseSummary = activeProvider?.let { "${it.label} · ${it.model}" } ?: "选择提供商"
    val appearanceEnglishSummary = "${uiState.appLanguage.menuEnglishLabel()} · ${uiState.themeMode.menuEnglishLabel()}"
    val appearanceChineseSummary = "${uiState.appLanguage.menuChineseLabel()} · ${uiState.themeMode.menuChineseLabel()}"
    val validationResolved = uiState.imResolvedBackendOrigin.removeSuffix("/")
    val validationEnglishSummary = uiState.imValidationError ?: "Backend $validationResolved"
    val validationChineseSummary = uiState.imValidationError ?: "后端 $validationResolved"

    return listOf(
        SettingsMenuItem(
            destination = SettingsDestination.Appearance,
            testTag = "settings-menu-appearance",
            englishLabel = "Appearance & Language",
            chineseLabel = "外观与语言",
            englishSummary = appearanceEnglishSummary,
            chineseSummary = appearanceChineseSummary,
        ),
        SettingsMenuItem(
            destination = SettingsDestination.AiProvider,
            testTag = "settings-menu-ai-provider",
            englishLabel = "AI Provider",
            chineseLabel = "AI 提供商",
            englishSummary = providerEnglishSummary,
            chineseSummary = providerChineseSummary,
        ),
        SettingsMenuItem(
            destination = SettingsDestination.ImValidation,
            testTag = "settings-menu-im-validation",
            englishLabel = "Connection",
            chineseLabel = "连接信息",
            englishSummary = validationEnglishSummary,
            chineseSummary = validationChineseSummary,
        ),
        SettingsMenuItem(
            destination = SettingsDestination.Personas,
            testTag = "settings-menu-personas",
            englishLabel = "Personas",
            chineseLabel = "用户角色",
            englishSummary = "Manage the {{user}} personas used across companion chats.",
            chineseSummary = "管理陪伴对话中的 {{user}} 角色资料。",
        ),
        SettingsMenuItem(
            destination = SettingsDestination.WorldInfo,
            testTag = "settings-menu-worldinfo",
            englishLabel = "World Info",
            chineseLabel = "世界信息",
            englishSummary = "Manage lorebooks bound to companion characters.",
            chineseSummary = "管理绑定到伙伴角色的世界书。",
        ),
        SettingsMenuItem(
            destination = SettingsDestination.Account,
            testTag = "settings-menu-account",
            englishLabel = "Account",
            chineseLabel = "账号",
            englishSummary = "Manage sign-in and session details.",
            chineseSummary = "管理登录与会话信息。",
        ),
    )
}

internal class SettingsViewModel(
    private val repository: AigcRepository,
    private val preferencesStore: PreferencesStore,
    messagingRepository: MessagingRepository,
) : ViewModel() {
    private data class ProviderConfigState(
        val providers: List<AigcProvider>,
        val activeProviderId: String,
        val customProvider: CustomProviderConfig,
        val presetProviderConfigs: Map<String, PresetProviderConfig>,
    )

    private data class ProviderSettingsState(
        val providers: List<AigcProvider>,
        val activeProviderId: String,
        val customProvider: CustomProviderConfig,
        val presetProviderConfigs: Map<String, PresetProviderConfig>,
        val appLanguage: AppLanguage,
        val themeMode: AppThemeMode,
    )

    private val imValidationConfig = combine(
        preferencesStore.imBackendOrigin,
        preferencesStore.imDevUserExternalId,
    ) { backendOrigin, devUserExternalId ->
        backendOrigin to devUserExternalId
    }

    private val providerConfig = combine(
        repository.providers,
        repository.activeProviderId,
        repository.customProvider,
        repository.presetProviderConfigs,
    ) { providers, activeProviderId, customProvider, presetProviderConfigs ->
        ProviderConfigState(
            providers = providers,
            activeProviderId = activeProviderId,
            customProvider = customProvider,
            presetProviderConfigs = presetProviderConfigs,
        )
    }

    private val providerSettings = combine(
        providerConfig,
        preferencesStore.appLanguage,
        preferencesStore.appThemeMode,
    ) { providerConfig, appLanguage, themeMode ->
        ProviderSettingsState(
            providers = providerConfig.providers,
            activeProviderId = providerConfig.activeProviderId,
            customProvider = providerConfig.customProvider,
            presetProviderConfigs = providerConfig.presetProviderConfigs,
            appLanguage = appLanguage,
            themeMode = themeMode,
        )
    }

    val uiState = combine(providerSettings, imValidationConfig, messagingRepository.integrationState) { providerSettings, validationConfig, messagingIntegrationState ->
        val (imDeveloperOverrideOrigin, imDevUserExternalId) = validationConfig
        val resolvedEndpoint = ImHttpEndpointResolver.resolve(
            sessionBaseUrl = null,
            developerOverrideOrigin = imDeveloperOverrideOrigin,
            shippedBackendOrigin = BuildConfig.IM_BACKEND_ORIGIN,
            allowDeveloperOverrides = BuildConfig.DEBUG,
        )
        SettingsUiState(
            providers = providerSettings.providers,
            activeProviderId = providerSettings.activeProviderId,
            customProvider = providerSettings.customProvider,
            activePresetProviderConfig = providerSettings.presetProviderConfigs[providerSettings.activeProviderId],
            imResolvedBackendOrigin = resolvedEndpoint.backendOrigin,
            imResolvedWebSocketUrl = resolvedEndpoint.webSocketUrl,
            imDeveloperOverrideOrigin = imDeveloperOverrideOrigin,
            imDevUserExternalId = imDevUserExternalId,
            imValidationError = validationErrorFor(
                appLanguage = providerSettings.appLanguage,
                backendOrigin = imDeveloperOverrideOrigin,
                devUserExternalId = imDevUserExternalId,
            ),
            messagingIntegrationState = messagingIntegrationState,
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
            imResolvedBackendOrigin = BuildConfig.IM_BACKEND_ORIGIN,
            imResolvedWebSocketUrl = ImHttpEndpointResolver.resolve(
                sessionBaseUrl = null,
                developerOverrideOrigin = null,
                shippedBackendOrigin = BuildConfig.IM_BACKEND_ORIGIN,
                allowDeveloperOverrides = BuildConfig.DEBUG,
            ).webSocketUrl,
            imDeveloperOverrideOrigin = "",
            imDevUserExternalId = "",
            messagingIntegrationState = messagingRepository.integrationState.value,
            appLanguage = AppLanguage.Chinese,
            themeMode = AppThemeMode.Light,
        ),
    )

    fun setActiveProvider(id: String) = repository.setActiveProvider(id)
    fun updateCustom(baseUrl: String? = null, model: String? = null, apiKey: String? = null) = repository.updateCustomProvider(baseUrl, model, apiKey)
    fun updatePresetProvider(model: String? = null, apiKey: String? = null) {
        val activeProviderId = uiState.value.activeProviderId
        if (activeProviderId == "custom") return
        repository.updatePresetProviderConfig(
            providerId = activeProviderId,
            model = model,
            apiKey = apiKey,
        )
    }
    fun updateImValidationConfig(
        backendOrigin: String? = null,
        devUserExternalId: String? = null,
    ) {
        viewModelScope.launch {
            backendOrigin?.let { preferencesStore.setImBackendOrigin(it) }
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
        appLanguage: AppLanguage,
        backendOrigin: String,
        devUserExternalId: String,
    ): String? {
        val hasOverride = backendOrigin.isNotBlank()
        if (!hasOverride) return null
        val backendValid = backendOrigin.startsWith("http://") || backendOrigin.startsWith("https://")
        return if (!backendValid || devUserExternalId.isBlank()) {
            appLanguage.pick(
                "Connection settings are incomplete or invalid.",
                "连接设置不完整或格式无效。",
            )
        } else {
            null
        }
    }
}

@Composable
fun SettingsRoute(navController: NavHostController, container: AppContainer) {
    val viewModel = viewModel<SettingsViewModel>(factory = simpleViewModelFactory {
        SettingsViewModel(container.aigcRepository, container.preferencesStore, container.messagingRepository)
    })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeProvider = uiState.providers.firstOrNull { it.id == uiState.activeProviderId }
    val presetProviderConfig = uiState.activePresetProviderConfig
    val activeModelValue = if (activeProvider?.id == "custom") uiState.customProvider.model else presetProviderConfig?.model.orEmpty()
    val activeApiKeyValue = if (activeProvider?.id == "custom") uiState.customProvider.apiKey else presetProviderConfig?.apiKey.orEmpty()
    var destination by rememberSaveable { mutableStateOf(SettingsDestination.Menu) }
    var baseUrl by remember(uiState.customProvider.baseUrl) { mutableStateOf(uiState.customProvider.baseUrl) }
    var model by remember(uiState.activeProviderId, activeModelValue) { mutableStateOf(activeModelValue) }
    var apiKey by remember(uiState.activeProviderId, activeApiKeyValue) { mutableStateOf(activeApiKeyValue) }
    var imDeveloperOverrideOrigin by remember(uiState.imDeveloperOverrideOrigin) { mutableStateOf(uiState.imDeveloperOverrideOrigin) }
    var imDevUserExternalId by remember(uiState.imDevUserExternalId) { mutableStateOf(uiState.imDevUserExternalId) }
    var showImDeveloperControls by rememberSaveable { mutableStateOf(false) }
    var editingPersonaId by rememberSaveable { mutableStateOf<String?>(null) }

    SettingsScreen(
        container = container,
        uiState = uiState,
        destination = destination,
        editingPersonaId = editingPersonaId,
        baseUrl = baseUrl,
        model = model,
        apiKey = apiKey,
        imDeveloperOverrideOrigin = imDeveloperOverrideOrigin,
        imResolvedWebSocketUrl = uiState.imResolvedWebSocketUrl,
        imDevUserExternalId = imDevUserExternalId,
        showImDeveloperControls = showImDeveloperControls,
        onBaseUrlChanged = {
            baseUrl = it
            viewModel.updateCustom(baseUrl = it)
        },
        onModelChanged = {
            model = it
            if (activeProvider?.id == "custom") {
                viewModel.updateCustom(model = it)
            } else {
                viewModel.updatePresetProvider(model = it)
            }
        },
        onApiKeyChanged = {
            apiKey = it
            if (activeProvider?.id == "custom") {
                viewModel.updateCustom(apiKey = it)
            } else {
                viewModel.updatePresetProvider(apiKey = it)
            }
        },
        onImBackendOriginChanged = {
            imDeveloperOverrideOrigin = it
            viewModel.updateImValidationConfig(backendOrigin = it)
        },
        onImDevUserExternalIdChanged = {
            imDevUserExternalId = it
            viewModel.updateImValidationConfig(devUserExternalId = it)
        },
        onToggleImDeveloperControls = { showImDeveloperControls = !showImDeveloperControls },
        onNavigateToDestination = { destination = it },
        onEditPersona = { id ->
            editingPersonaId = id
            destination = SettingsDestination.PersonaEditor
        },
        onPersonaEditorDone = {
            editingPersonaId = null
            destination = SettingsDestination.Personas
        },
        onSelectProvider = viewModel::setActiveProvider,
        onSelectLanguage = viewModel::setAppLanguage,
        onSelectThemeMode = viewModel::setThemeMode,
        onBack = { navController.popBackStack() },
    )
}

@Composable
private fun SettingsScreen(
    container: AppContainer,
    uiState: SettingsUiState,
    destination: SettingsDestination,
    editingPersonaId: String?,
    baseUrl: String,
    model: String,
    apiKey: String,
    imDeveloperOverrideOrigin: String,
    imResolvedWebSocketUrl: String,
    imDevUserExternalId: String,
    showImDeveloperControls: Boolean,
    onBaseUrlChanged: (String) -> Unit,
    onModelChanged: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onImBackendOriginChanged: (String) -> Unit,
    onImDevUserExternalIdChanged: (String) -> Unit,
    onToggleImDeveloperControls: () -> Unit,
    onNavigateToDestination: (SettingsDestination) -> Unit,
    onEditPersona: (String) -> Unit,
    onPersonaEditorDone: () -> Unit,
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
                imDeveloperOverrideOrigin = imDeveloperOverrideOrigin,
                imResolvedWebSocketUrl = imResolvedWebSocketUrl,
                imDevUserExternalId = imDevUserExternalId,
                showImDeveloperControls = showImDeveloperControls,
                onImBackendOriginChanged = onImBackendOriginChanged,
                onImDevUserExternalIdChanged = onImDevUserExternalIdChanged,
                onToggleImDeveloperControls = onToggleImDeveloperControls,
                onBack = { onNavigateToDestination(SettingsDestination.Menu) },
            )

            SettingsDestination.Personas -> SettingsPersonasScreen(
                container = container,
                onEditPersona = onEditPersona,
                onBack = { onNavigateToDestination(SettingsDestination.Menu) },
            )

            SettingsDestination.PersonaEditor -> {
                val id = editingPersonaId
                if (id != null) {
                    PersonaEditorRoute(
                        container = container,
                        personaId = id,
                        onDone = onPersonaEditorDone,
                    )
                } else {
                    androidx.compose.runtime.SideEffect { onPersonaEditorDone() }
                }
            }

            SettingsDestination.WorldInfo -> com.gkim.im.android.feature.settings.worldinfo.WorldInfoLibraryRoute(
                container = container,
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

    PageHeader(
        eyebrow = appLanguage.pick("Settings", "设置"),
        title = appLanguage.pick("Preferences", "偏好设置"),
        description = appLanguage.pick(
            "Manage appearance, AI services, connection info, and account options.",
            "管理外观、AI 服务、连接信息和账号选项。",
        ),
        leadingLabel = appLanguage.pick("Back", "返回"),
        onLeading = onBack,
    )

    Column(
        modifier = Modifier.testTag("settings-menu-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        buildSettingsMenuItems(uiState).forEach { item ->
            SettingsMenuEntry(
                testTag = item.testTag,
                label = appLanguage.pick(item.englishLabel, item.chineseLabel),
                summary = appLanguage.pick(item.englishSummary, item.chineseSummary),
            ) { onNavigateToDestination(item.destination) }
        }
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
            val activeProvider = uiState.providers.firstOrNull { it.id == uiState.activeProviderId }
            val isCustomProvider = activeProvider?.id == "custom"
            Text(
                text = if (isCustomProvider) appLanguage.pick("CUSTOM ENDPOINT", "自定义端点") else appLanguage.pick("PRESET PROVIDER", "预设提供商"),
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.Tertiary,
            )
            Text(
                text = if (isCustomProvider) {
                    appLanguage.pick(
                        "The custom gateway uses your own base URL, model, and key.",
                        "自定义网关使用你自己的基础 URL、模型和密钥。",
                    )
                } else {
                    appLanguage.pick(
                        "The selected preset keeps its provider-managed endpoint while you edit the local model override and API key.",
                        "当前预设会保留提供商托管端点，你只需维护本地模型覆盖与 API 密钥。",
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = AetherColors.OnSurfaceVariant,
                modifier = Modifier.testTag("settings-active-provider-summary"),
            )
            if (isCustomProvider) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = onBaseUrlChanged,
                    modifier = Modifier.fillMaxWidth().testTag("settings-base-url"),
                    label = { Text(appLanguage.pick("Base URL", "基础 URL")) },
                )
            }
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
    imDeveloperOverrideOrigin: String,
    imResolvedWebSocketUrl: String,
    imDevUserExternalId: String,
    showImDeveloperControls: Boolean,
    onImBackendOriginChanged: (String) -> Unit,
    onImDevUserExternalIdChanged: (String) -> Unit,
    onToggleImDeveloperControls: () -> Unit,
    onBack: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    PageHeader(
        eyebrow = appLanguage.pick("Settings", "设置"),
        title = appLanguage.pick("Connection", "连接信息"),
        description = appLanguage.pick(
            "Review the current server address and connection status for messaging.",
            "查看消息服务当前使用的地址和连接状态。",
        ),
        leadingLabel = appLanguage.pick("Back", "返回"),
        onLeading = onBack,
    )

    Column(
        modifier = Modifier.testTag("settings-detail-im-validation"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        GlassCard {
            Text(text = appLanguage.pick("MESSAGE CONNECTION", "消息连接"), style = MaterialTheme.typography.labelLarge, color = AetherColors.Primary)
            Text(
                text = appLanguage.pick("Resolved backend origin", "当前后端地址"),
                style = MaterialTheme.typography.bodyMedium,
                color = AetherColors.OnSurfaceVariant,
            )
            Text(
                text = uiState.imResolvedBackendOrigin,
                style = MaterialTheme.typography.bodyLarge,
                color = AetherColors.OnSurface,
                modifier = Modifier.testTag("settings-im-resolved-backend-origin"),
            )
            Text(
                text = appLanguage.pick("Derived realtime endpoint", "派生的实时连接地址"),
                style = MaterialTheme.typography.bodyMedium,
                color = AetherColors.OnSurfaceVariant,
            )
            Text(
                text = imResolvedWebSocketUrl,
                style = MaterialTheme.typography.bodyLarge,
                color = AetherColors.OnSurface,
                modifier = Modifier.testTag("settings-im-resolved-websocket-url"),
            )
            Text(
                text = when {
                    uiState.imValidationError != null -> uiState.imValidationError
                    else -> messagingIntegrationStatusLabel(uiState.messagingIntegrationState, appLanguage)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    uiState.imValidationError != null -> AetherColors.OnSurfaceVariant
                    uiState.messagingIntegrationState.phase == MessagingIntegrationPhase.Error -> AetherColors.OnSurfaceVariant
                    else -> AetherColors.Primary
                },
                modifier = Modifier.testTag("settings-im-validation-status"),
            )
            if (BuildConfig.DEBUG) {
                OutlinedButton(
                    onClick = onToggleImDeveloperControls,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings-im-developer-toggle"),
                ) {
                    Text(
                        appLanguage.pick(
                            if (showImDeveloperControls) "Hide advanced connection settings" else "Show advanced connection settings",
                            if (showImDeveloperControls) "隐藏高级连接设置" else "显示高级连接设置",
                        )
                    )
                }
            }
            if (BuildConfig.DEBUG && showImDeveloperControls) {
                OutlinedTextField(
                    value = imDeveloperOverrideOrigin,
                    onValueChange = onImBackendOriginChanged,
                    modifier = Modifier.fillMaxWidth().testTag("settings-im-backend-origin"),
                    label = { Text(appLanguage.pick("Alternate server origin", "备用服务地址")) },
                )
                OutlinedTextField(
                    value = imDevUserExternalId,
                    onValueChange = onImDevUserExternalIdChanged,
                    modifier = Modifier.fillMaxWidth().testTag("settings-im-dev-user"),
                    label = { Text(appLanguage.pick("Test account", "测试账号")) },
                )
            }
        }

        if (BuildConfig.DEBUG) {
            GlassCard {
                Text(text = appLanguage.pick("DEBUG NOTE", "调试说明"), style = MaterialTheme.typography.labelLarge, color = AetherColors.Primary)
                Text(
                    text = appLanguage.pick(
                        "Server-side database credentials stay on the backend and are never bundled into the Android client.",
                        "服务端数据库凭据只保留在后端，不会打包进 Android 客户端。",
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AetherColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsPersonasScreen(
    container: AppContainer,
    onEditPersona: (String) -> Unit,
    onBack: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    val viewModel = viewModel<PersonaLibraryViewModel>(
        key = "personaLibrary",
        factory = simpleViewModelFactory {
            PersonaLibraryViewModel(
                repository = container.userPersonaRepository,
                language = { appLanguage },
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PageHeader(
        eyebrow = appLanguage.pick("Settings", "设置"),
        title = appLanguage.pick("Personas", "用户角色"),
        description = appLanguage.pick(
            "Choose how companions address you. The active persona powers the {{user}} macro in chats.",
            "选择陪伴对象称呼你的方式。当前启用的角色资料会驱动对话中的 {{user}} 占位。",
        ),
        leadingLabel = appLanguage.pick("Back", "返回"),
        onLeading = onBack,
    )

    Column(
        modifier = Modifier.testTag("settings-detail-personas"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        uiState.errorMessage?.let { message ->
            GlassCard(modifier = Modifier.testTag("settings-personas-error")) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                )
                OutlinedButton(
                    onClick = viewModel::clearError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings-personas-error-dismiss"),
                ) {
                    Text(appLanguage.pick("Dismiss", "知道了"))
                }
            }
        }

        OutlinedButton(
            onClick = { /* hook for task 3.2 PersonaEditor */ },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings-personas-new"),
        ) {
            Text(appLanguage.pick("New persona", "新建角色"))
        }

        uiState.items.forEach { item ->
            val pendingOperation = uiState.pendingOperation?.takeIf { it.personaId == item.persona.id }
            val isPending = pendingOperation != null
            GlassCard(modifier = Modifier.testTag("settings-personas-card-${item.persona.id}")) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = item.resolved.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = AetherColors.OnSurface,
                    )
                    if (item.isActive) {
                        Text(
                            text = appLanguage.pick("ACTIVE", "已启用"),
                            style = MaterialTheme.typography.labelLarge,
                            color = AetherColors.Surface,
                            modifier = Modifier
                                .testTag("settings-personas-active-${item.persona.id}")
                                .background(AetherColors.Primary, RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    if (item.persona.isBuiltIn) {
                        Text(
                            text = appLanguage.pick("BUILT-IN", "内置"),
                            style = MaterialTheme.typography.labelLarge,
                            color = AetherColors.OnSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = item.resolved.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.activate(item.persona.id) },
                        enabled = item.canActivate && !isPending,
                        modifier = Modifier.testTag("settings-personas-activate-${item.persona.id}"),
                    ) {
                        Text(appLanguage.pick("Activate", "启用"))
                    }
                    OutlinedButton(
                        onClick = { onEditPersona(item.persona.id) },
                        enabled = item.canEdit && !isPending,
                        modifier = Modifier.testTag("settings-personas-edit-${item.persona.id}"),
                    ) {
                        Text(appLanguage.pick("Edit", "编辑"))
                    }
                    OutlinedButton(
                        onClick = { viewModel.duplicate(item.persona.id) },
                        enabled = item.canDuplicate && !isPending,
                        modifier = Modifier.testTag("settings-personas-duplicate-${item.persona.id}"),
                    ) {
                        Text(appLanguage.pick("Duplicate", "复制"))
                    }
                    OutlinedButton(
                        onClick = { viewModel.delete(item.persona.id) },
                        enabled = item.canDelete && !isPending,
                        modifier = Modifier.testTag("settings-personas-delete-${item.persona.id}"),
                    ) {
                        Text(appLanguage.pick("Delete", "删除"))
                    }
                }
            }
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
            "Manage your sign-in status and account entry points.",
            "管理登录状态和账号入口。",
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
                    "Your signed-in session is stored securely after login so the app can restore the IM shell on the next launch.",
                    "登录后会安全保存你的会话，方便下次启动时直接恢复 IM 壳层。",
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = AetherColors.OnSurfaceVariant,
            )
        }

        GlassCard {
            Text(text = appLanguage.pick("ACCOUNT ACTIONS", "账号动作"), style = MaterialTheme.typography.labelLarge, color = AetherColors.Primary)
            Text(
                text = appLanguage.pick(
                    "The current release already uses the welcome login and registration flow for server-backed access.",
                    "当前版本已经通过欢迎页登录和注册接入服务端。",
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
