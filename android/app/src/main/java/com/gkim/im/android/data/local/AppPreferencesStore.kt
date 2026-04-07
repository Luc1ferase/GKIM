package com.gkim.im.android.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gkim.im.android.core.model.ContactSortMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferencesStore by preferencesDataStore(name = "gkim_preferences")

interface PreferencesStore {
    val contactSortMode: Flow<ContactSortMode>
    val activeProviderId: Flow<String>
    val customBaseUrl: Flow<String>
    val customModel: Flow<String>

    suspend fun setContactSortMode(mode: ContactSortMode)
    suspend fun setActiveProviderId(value: String)
    suspend fun setCustomBaseUrl(value: String)
    suspend fun setCustomModel(value: String)
}

class AppPreferencesStore(private val context: Context) : PreferencesStore {
    private val sortKey = stringPreferencesKey("contact_sort_mode")
    private val providerKey = stringPreferencesKey("active_provider_id")
    private val customBaseUrlKey = stringPreferencesKey("custom_provider_base_url")
    private val customModelKey = stringPreferencesKey("custom_provider_model")

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
}
