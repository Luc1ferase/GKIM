package com.gkim.im.android.testing

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.AppThemeMode
import com.gkim.im.android.core.model.ContactSortMode
import com.gkim.im.android.core.security.SecureKeyValueStore
import com.gkim.im.android.data.local.PreferencesStore
import com.gkim.im.android.data.remote.im.DEFAULT_IM_HTTP_BASE_URL
import com.gkim.im.android.data.remote.im.DEFAULT_IM_WEBSOCKET_URL
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakePreferencesStore(
    initialSortMode: ContactSortMode = ContactSortMode.Nickname,
    initialProviderId: String = "hunyuan",
    initialBaseUrl: String = "https://api.example.com/v1",
    initialModel: String = "gpt-image-1",
    initialImHttpBaseUrl: String = DEFAULT_IM_HTTP_BASE_URL,
    initialImWebSocketUrl: String = DEFAULT_IM_WEBSOCKET_URL,
    initialImDevUserExternalId: String = "nox-dev",
    initialLanguage: AppLanguage = AppLanguage.Chinese,
    initialThemeMode: AppThemeMode = AppThemeMode.Light,
) : PreferencesStore {
    private val contactSortModeState = MutableStateFlow(initialSortMode)
    private val activeProviderIdState = MutableStateFlow(initialProviderId)
    private val customBaseUrlState = MutableStateFlow(initialBaseUrl)
    private val customModelState = MutableStateFlow(initialModel)
    private val imHttpBaseUrlState = MutableStateFlow(initialImHttpBaseUrl)
    private val imWebSocketUrlState = MutableStateFlow(initialImWebSocketUrl)
    private val imDevUserExternalIdState = MutableStateFlow(initialImDevUserExternalId)
    private val appLanguageState = MutableStateFlow(initialLanguage)
    private val appThemeModeState = MutableStateFlow(initialThemeMode)

    override val contactSortMode: Flow<ContactSortMode> = contactSortModeState.asStateFlow()
    override val activeProviderId: Flow<String> = activeProviderIdState.asStateFlow()
    override val customBaseUrl: Flow<String> = customBaseUrlState.asStateFlow()
    override val customModel: Flow<String> = customModelState.asStateFlow()
    override val imHttpBaseUrl: Flow<String> = imHttpBaseUrlState.asStateFlow()
    override val imWebSocketUrl: Flow<String> = imWebSocketUrlState.asStateFlow()
    override val imDevUserExternalId: Flow<String> = imDevUserExternalIdState.asStateFlow()
    override val appLanguage: Flow<AppLanguage> = appLanguageState.asStateFlow()
    override val appThemeMode: Flow<AppThemeMode> = appThemeModeState.asStateFlow()

    val currentSortMode: ContactSortMode
        get() = contactSortModeState.value

    val currentProviderId: String
        get() = activeProviderIdState.value

    val currentBaseUrl: String
        get() = customBaseUrlState.value

    val currentModel: String
        get() = customModelState.value

    val currentImHttpBaseUrl: String
        get() = imHttpBaseUrlState.value

    val currentImWebSocketUrl: String
        get() = imWebSocketUrlState.value

    val currentImDevUserExternalId: String
        get() = imDevUserExternalIdState.value

    val currentLanguage: AppLanguage
        get() = appLanguageState.value

    val currentThemeMode: AppThemeMode
        get() = appThemeModeState.value

    override suspend fun setContactSortMode(mode: ContactSortMode) {
        contactSortModeState.value = mode
    }

    override suspend fun setActiveProviderId(value: String) {
        activeProviderIdState.value = value
    }

    override suspend fun setCustomBaseUrl(value: String) {
        customBaseUrlState.value = value
    }

    override suspend fun setCustomModel(value: String) {
        customModelState.value = value
    }

    override suspend fun setImHttpBaseUrl(value: String) {
        imHttpBaseUrlState.value = value
    }

    override suspend fun setImWebSocketUrl(value: String) {
        imWebSocketUrlState.value = value
    }

    override suspend fun setImDevUserExternalId(value: String) {
        imDevUserExternalIdState.value = value
    }

    override suspend fun setAppLanguage(value: AppLanguage) {
        appLanguageState.value = value
    }

    override suspend fun setAppThemeMode(value: AppThemeMode) {
        appThemeModeState.value = value
    }
}

class InMemorySecureKeyValueStore : SecureKeyValueStore {
    private val values = mutableMapOf<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    fun peek(key: String): String? = values[key]
}
