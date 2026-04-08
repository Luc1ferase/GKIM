package com.gkim.im.android.feature.navigation

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.AppThemeMode
import com.gkim.im.android.core.model.ContactSortMode
import com.gkim.im.android.core.security.SecureKeyValueStore
import com.gkim.im.android.data.local.PreferencesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class UiTestPreferencesStore : PreferencesStore {
    private val contactSortModeState = MutableStateFlow(ContactSortMode.Nickname)
    private val activeProviderIdState = MutableStateFlow("hunyuan")
    private val customBaseUrlState = MutableStateFlow("https://api.example.com/v1")
    private val customModelState = MutableStateFlow("gpt-image-1")
    private val imHttpBaseUrlState = MutableStateFlow("http://127.0.0.1:18080/")
    private val imWebSocketUrlState = MutableStateFlow("ws://127.0.0.1:18080/ws")
    private val imDevUserExternalIdState = MutableStateFlow("nox-dev")
    private val appLanguageState = MutableStateFlow(AppLanguage.English)
    private val appThemeModeState = MutableStateFlow(AppThemeMode.Dark)

    override val contactSortMode: Flow<ContactSortMode> = contactSortModeState.asStateFlow()
    override val activeProviderId: Flow<String> = activeProviderIdState.asStateFlow()
    override val customBaseUrl: Flow<String> = customBaseUrlState.asStateFlow()
    override val customModel: Flow<String> = customModelState.asStateFlow()
    override val imHttpBaseUrl: Flow<String> = imHttpBaseUrlState.asStateFlow()
    override val imWebSocketUrl: Flow<String> = imWebSocketUrlState.asStateFlow()
    override val imDevUserExternalId: Flow<String> = imDevUserExternalIdState.asStateFlow()
    override val appLanguage: Flow<AppLanguage> = appLanguageState.asStateFlow()
    override val appThemeMode: Flow<AppThemeMode> = appThemeModeState.asStateFlow()

    val currentLanguage: AppLanguage
        get() = appLanguageState.value

    val currentThemeMode: AppThemeMode
        get() = appThemeModeState.value

    val currentImHttpBaseUrl: String
        get() = imHttpBaseUrlState.value

    val currentImWebSocketUrl: String
        get() = imWebSocketUrlState.value

    val currentImDevUserExternalId: String
        get() = imDevUserExternalIdState.value

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

internal class UiInMemorySecureStore : SecureKeyValueStore {
    private val values = mutableMapOf<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }
}
