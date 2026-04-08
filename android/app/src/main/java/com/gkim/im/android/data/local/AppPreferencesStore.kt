package com.gkim.im.android.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.AppThemeMode
import com.gkim.im.android.core.model.ContactSortMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferencesStore by preferencesDataStore(name = "gkim_preferences")

interface PreferencesStore {
    val contactSortMode: Flow<ContactSortMode>
    val activeProviderId: Flow<String>
    val customBaseUrl: Flow<String>
    val customModel: Flow<String>
    val imHttpBaseUrl: Flow<String>
    val imWebSocketUrl: Flow<String>
    val imDevUserExternalId: Flow<String>
    val appLanguage: Flow<AppLanguage>
    val appThemeMode: Flow<AppThemeMode>

    suspend fun setContactSortMode(mode: ContactSortMode)
    suspend fun setActiveProviderId(value: String)
    suspend fun setCustomBaseUrl(value: String)
    suspend fun setCustomModel(value: String)
    suspend fun setImHttpBaseUrl(value: String)
    suspend fun setImWebSocketUrl(value: String)
    suspend fun setImDevUserExternalId(value: String)
    suspend fun setAppLanguage(value: AppLanguage)
    suspend fun setAppThemeMode(value: AppThemeMode)
}

class AppPreferencesStore(private val context: Context) : PreferencesStore {
    private val sortKey = stringPreferencesKey("contact_sort_mode")
    private val providerKey = stringPreferencesKey("active_provider_id")
    private val customBaseUrlKey = stringPreferencesKey("custom_provider_base_url")
    private val customModelKey = stringPreferencesKey("custom_provider_model")
    private val imHttpBaseUrlKey = stringPreferencesKey("im_http_base_url")
    private val imWebSocketUrlKey = stringPreferencesKey("im_websocket_url")
    private val imDevUserExternalIdKey = stringPreferencesKey("im_dev_user_external_id")
    private val appLanguageKey = stringPreferencesKey("app_language")
    private val appThemeModeKey = stringPreferencesKey("app_theme_mode")

    override val contactSortMode: Flow<ContactSortMode> = context.preferencesStore.data.map {
        ContactSortMode.valueOf(it[sortKey] ?: ContactSortMode.Nickname.name)
    }

    override val activeProviderId: Flow<String> = context.preferencesStore.data.map { prefs ->
        prefs[providerKey] ?: "hunyuan"
    }

    override val customBaseUrl: Flow<String> = context.preferencesStore.data.map { prefs ->
        prefs[customBaseUrlKey] ?: "https://api.example.com/v1"
    }

    override val customModel: Flow<String> = context.preferencesStore.data.map { prefs ->
        prefs[customModelKey] ?: "gpt-image-1"
    }

    override val imHttpBaseUrl: Flow<String> = context.preferencesStore.data.map { prefs ->
        prefs[imHttpBaseUrlKey] ?: "http://127.0.0.1:18080/"
    }

    override val imWebSocketUrl: Flow<String> = context.preferencesStore.data.map { prefs ->
        prefs[imWebSocketUrlKey] ?: "ws://127.0.0.1:18080/ws"
    }

    override val imDevUserExternalId: Flow<String> = context.preferencesStore.data.map { prefs ->
        prefs[imDevUserExternalIdKey] ?: "nox-dev"
    }

    override val appLanguage: Flow<AppLanguage> = context.preferencesStore.data.map { prefs ->
        AppLanguage.valueOf(prefs[appLanguageKey] ?: AppLanguage.Chinese.name)
    }

    override val appThemeMode: Flow<AppThemeMode> = context.preferencesStore.data.map { prefs ->
        AppThemeMode.valueOf(prefs[appThemeModeKey] ?: AppThemeMode.Light.name)
    }

    override suspend fun setContactSortMode(mode: ContactSortMode) {
        context.preferencesStore.edit { it[sortKey] = mode.name }
    }

    override suspend fun setActiveProviderId(value: String) {
        context.preferencesStore.edit { it[providerKey] = value }
    }

    override suspend fun setCustomBaseUrl(value: String) {
        context.preferencesStore.edit { it[customBaseUrlKey] = value }
    }

    override suspend fun setCustomModel(value: String) {
        context.preferencesStore.edit { it[customModelKey] = value }
    }

    override suspend fun setImHttpBaseUrl(value: String) {
        context.preferencesStore.edit { it[imHttpBaseUrlKey] = value }
    }

    override suspend fun setImWebSocketUrl(value: String) {
        context.preferencesStore.edit { it[imWebSocketUrlKey] = value }
    }

    override suspend fun setImDevUserExternalId(value: String) {
        context.preferencesStore.edit { it[imDevUserExternalIdKey] = value }
    }

    override suspend fun setAppLanguage(value: AppLanguage) {
        context.preferencesStore.edit { it[appLanguageKey] = value.name }
    }

    override suspend fun setAppThemeMode(value: AppThemeMode) {
        context.preferencesStore.edit { it[appThemeModeKey] = value.name }
    }
}
