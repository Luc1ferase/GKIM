package com.gkim.im.android.feature.settings

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.AppThemeMode
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
    fun `settings view model exposes resolved backend origin and developer override state`() = runTest(mainDispatcherRule.dispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val preferencesStore = FakePreferencesStore(
            initialImBackendOrigin = "",
            initialImDevUserExternalId = "nox-dev",
        )
        val secureStore = InMemorySecureKeyValueStore()
        val repository = DefaultAigcRepository(presetProviders, preferencesStore, secureStore, dispatcher)
        val viewModel = SettingsViewModel(repository, preferencesStore, InMemoryMessagingRepository(seedConversations))
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()

        assertEquals("https://chat.lastxuans.sbs/", viewModel.uiState.value.imResolvedBackendOrigin)
        assertEquals("wss://chat.lastxuans.sbs/ws", viewModel.uiState.value.imResolvedWebSocketUrl)
        assertEquals("", viewModel.uiState.value.imDeveloperOverrideOrigin)
        assertEquals("nox-dev", viewModel.uiState.value.imDevUserExternalId)
        assertEquals(null, viewModel.uiState.value.imValidationError)

        viewModel.updateImValidationConfig(
            backendOrigin = "https://forward.example.com/im",
            devUserExternalId = "leo-vance",
        )
        advanceUntilIdle()

        assertEquals("https://forward.example.com/im/", viewModel.uiState.value.imResolvedBackendOrigin)
        assertEquals("wss://forward.example.com/im/ws", viewModel.uiState.value.imResolvedWebSocketUrl)
        assertEquals("https://forward.example.com/im/", viewModel.uiState.value.imDeveloperOverrideOrigin)
        assertEquals("leo-vance", viewModel.uiState.value.imDevUserExternalId)
        assertEquals("https://forward.example.com/im/", preferencesStore.currentImBackendOrigin)
        assertEquals("leo-vance", preferencesStore.currentImDevUserExternalId)

        viewModel.updateImValidationConfig(
            backendOrigin = "forward.example.com",
            devUserExternalId = "",
        )
        advanceUntilIdle()

        assertEquals("连接设置不完整或格式无效。", viewModel.uiState.value.imValidationError)

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
