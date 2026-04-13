package com.gkim.im.android.feature.settings

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.AppThemeMode
import com.gkim.im.android.data.remote.im.DEFAULT_IM_HTTP_BASE_URL
import com.gkim.im.android.data.remote.im.DEFAULT_IM_WEBSOCKET_URL
import com.gkim.im.android.data.repository.DefaultAigcRepository
import com.gkim.im.android.data.repository.InMemoryMessagingRepository
import com.gkim.im.android.data.repository.presetProviders
import com.gkim.im.android.data.repository.seedConversations
import com.gkim.im.android.testing.FakePreferencesStore
import com.gkim.im.android.testing.InMemorySecureKeyValueStore
import com.gkim.im.android.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `settings view model updates provider selection and custom config`() = runTest(mainDispatcherRule.dispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val preferencesStore = FakePreferencesStore()
        val secureStore = InMemorySecureKeyValueStore()
        val repository = DefaultAigcRepository(presetProviders, preferencesStore, secureStore, dispatcher)
        val viewModel = SettingsViewModel(repository, preferencesStore, InMemoryMessagingRepository(seedConversations))
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()

        viewModel.setActiveProvider("custom")
        viewModel.updateCustom(
            baseUrl = "https://gateway.example.com/v1",
            model = "gpt-image-1",
            apiKey = "secret-token",
        )
        advanceUntilIdle()

        assertEquals("custom", viewModel.uiState.value.activeProviderId)
        assertEquals("https://gateway.example.com/v1", viewModel.uiState.value.customProvider.baseUrl)
        assertEquals("gpt-image-1", viewModel.uiState.value.customProvider.model)
        assertEquals("secret-token", viewModel.uiState.value.customProvider.apiKey)
        assertEquals("secret-token", secureStore.peek("custom_api_key"))

        collector.cancel()
    }

    @Test
    fun `settings view model defaults to Chinese and light theme for first run`() = runTest(mainDispatcherRule.dispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val preferencesStore = FakePreferencesStore()
        val secureStore = InMemorySecureKeyValueStore()
        val repository = DefaultAigcRepository(presetProviders, preferencesStore, secureStore, dispatcher)
        val viewModel = SettingsViewModel(repository, preferencesStore, InMemoryMessagingRepository(seedConversations))
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()

        assertEquals(AppLanguage.Chinese, viewModel.uiState.value.appLanguage)
        assertEquals(AppThemeMode.Light, viewModel.uiState.value.themeMode)

        collector.cancel()
    }

    @Test
    fun `settings view model exposes and updates persisted language and theme preferences`() = runTest(mainDispatcherRule.dispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val preferencesStore = FakePreferencesStore(
            initialLanguage = AppLanguage.Chinese,
            initialThemeMode = AppThemeMode.Light,
        )
        val secureStore = InMemorySecureKeyValueStore()
        val repository = DefaultAigcRepository(presetProviders, preferencesStore, secureStore, dispatcher)
        val viewModel = SettingsViewModel(repository, preferencesStore, InMemoryMessagingRepository(seedConversations))
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()

        assertEquals(AppLanguage.Chinese, viewModel.uiState.value.appLanguage)
        assertEquals(AppThemeMode.Light, viewModel.uiState.value.themeMode)

        viewModel.setAppLanguage(AppLanguage.English)
        viewModel.setThemeMode(AppThemeMode.Dark)
        advanceUntilIdle()

        assertEquals(AppLanguage.English, viewModel.uiState.value.appLanguage)
        assertEquals(AppThemeMode.Dark, viewModel.uiState.value.themeMode)
        assertEquals(AppLanguage.English, preferencesStore.currentLanguage)
        assertEquals(AppThemeMode.Dark, preferencesStore.currentThemeMode)

        collector.cancel()
    }

    @Test
    fun `settings view model exposes and updates IM backend validation inputs`() = runTest(mainDispatcherRule.dispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val preferencesStore = FakePreferencesStore(
            initialImHttpBaseUrl = DEFAULT_IM_HTTP_BASE_URL,
            initialImWebSocketUrl = DEFAULT_IM_WEBSOCKET_URL,
            initialImDevUserExternalId = "nox-dev",
        )
        val secureStore = InMemorySecureKeyValueStore()
        val repository = DefaultAigcRepository(presetProviders, preferencesStore, secureStore, dispatcher)
        val viewModel = SettingsViewModel(repository, preferencesStore, InMemoryMessagingRepository(seedConversations))
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()

        assertEquals(DEFAULT_IM_HTTP_BASE_URL, viewModel.uiState.value.imHttpBaseUrl)
        assertEquals(DEFAULT_IM_WEBSOCKET_URL, viewModel.uiState.value.imWebSocketUrl)
        assertEquals("nox-dev", viewModel.uiState.value.imDevUserExternalId)
        assertEquals(null, viewModel.uiState.value.imValidationError)

        viewModel.updateImValidationConfig(
            httpBaseUrl = "https://forward.example.com/",
            webSocketUrl = "wss://forward.example.com/ws",
            devUserExternalId = "leo-vance",
        )
        advanceUntilIdle()

        assertEquals("https://forward.example.com/", viewModel.uiState.value.imHttpBaseUrl)
        assertEquals("wss://forward.example.com/ws", viewModel.uiState.value.imWebSocketUrl)
        assertEquals("leo-vance", viewModel.uiState.value.imDevUserExternalId)
        assertEquals("https://forward.example.com/", preferencesStore.currentImHttpBaseUrl)
        assertEquals("wss://forward.example.com/ws", preferencesStore.currentImWebSocketUrl)
        assertEquals("leo-vance", preferencesStore.currentImDevUserExternalId)

        viewModel.updateImValidationConfig(
            httpBaseUrl = "forward.example.com",
            webSocketUrl = "https://forward.example.com/ws",
            devUserExternalId = "",
        )
        advanceUntilIdle()

        assertEquals("IM validation config is incomplete or invalid.", viewModel.uiState.value.imValidationError)

        collector.cancel()
    }

    @Test
    fun `settings view model exposes and updates active preset provider credentials`() = runTest(mainDispatcherRule.dispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val preferencesStore = FakePreferencesStore(
            initialPresetModels = mapOf("hunyuan" to "hy-image-v3.5"),
        )
        val secureStore = InMemorySecureKeyValueStore(
            mapOf("preset_provider_hunyuan_api_key" to "preset-secret")
        )
        val repository = DefaultAigcRepository(presetProviders, preferencesStore, secureStore, dispatcher)
        val viewModel = SettingsViewModel(repository, preferencesStore, InMemoryMessagingRepository(seedConversations))
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()

        viewModel.setActiveProvider("hunyuan")
        advanceUntilIdle()

        assertEquals("hy-image-v3.5", viewModel.uiState.value.activePresetProviderConfig?.model)
        assertEquals("preset-secret", viewModel.uiState.value.activePresetProviderConfig?.apiKey)

        viewModel.updatePresetProvider(model = "hy-image-v3.6", apiKey = "next-secret")
        advanceUntilIdle()

        assertEquals("hy-image-v3.6", viewModel.uiState.value.activePresetProviderConfig?.model)
        assertEquals("next-secret", viewModel.uiState.value.activePresetProviderConfig?.apiKey)
        assertEquals("hy-image-v3.6", preferencesStore.currentPresetModels.getValue("hunyuan"))
        assertEquals("next-secret", secureStore.peek("preset_provider_hunyuan_api_key"))

        collector.cancel()
    }
}
