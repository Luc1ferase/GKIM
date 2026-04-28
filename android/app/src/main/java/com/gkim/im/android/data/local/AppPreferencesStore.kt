package com.gkim.im.android.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.AppThemeMode
import com.gkim.im.android.core.model.ContactSortMode
import com.gkim.im.android.data.remote.im.normalizeImBackendOriginForStorage
import com.gkim.im.android.data.remote.im.resolveStoredImBackendOrigin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferencesStore by preferencesDataStore(name = "gkim_preferences")

interface PreferencesStore {
    val contactSortMode: Flow<ContactSortMode>
    val activeProviderId: Flow<String>
    val customBaseUrl: Flow<String>
    val customModel: Flow<String>
    val imBackendOrigin: Flow<String>
    val imDevUserExternalId: Flow<String>
    val appLanguage: Flow<AppLanguage>
    val appThemeMode: Flow<AppThemeMode>
    val blockReasonVerbosity: Flow<Boolean>
    val contentPolicyAcknowledgedAtMillis: Flow<Long?>
    val contentPolicyAcknowledgedVersion: Flow<String>

    suspend fun setContactSortMode(mode: ContactSortMode)
    suspend fun setActiveProviderId(value: String)
    suspend fun setCustomBaseUrl(value: String)
    suspend fun setCustomModel(value: String)
    fun presetProviderModel(providerId: String): Flow<String?>
    suspend fun setPresetProviderModel(providerId: String, value: String)
    suspend fun setImBackendOrigin(value: String)
    suspend fun setImDevUserExternalId(value: String)
    suspend fun setAppLanguage(value: AppLanguage)
    suspend fun setAppThemeMode(value: AppThemeMode)
    suspend fun setBlockReasonVerbosity(value: Boolean)
    suspend fun setContentPolicyAcknowledgment(acceptedAtMillis: Long, version: String)
    suspend fun clearContentPolicyAcknowledgment()
}

class AppPreferencesStore(private val context: Context) : PreferencesStore {
    private val sortKey = stringPreferencesKey("contact_sort_mode")
    private val providerKey = stringPreferencesKey("active_provider_id")
    private val customBaseUrlKey = stringPreferencesKey("custom_provider_base_url")
    private val customModelKey = stringPreferencesKey("custom_provider_model")
    private val imBackendOriginKey = stringPreferencesKey("im_backend_origin")
    private val legacyImHttpBaseUrlKey = stringPreferencesKey("im_http_base_url")
    private val legacyImWebSocketUrlKey = stringPreferencesKey("im_websocket_url")
    private val imDevUserExternalIdKey = stringPreferencesKey("im_dev_user_external_id")
    private val appLanguageKey = stringPreferencesKey("app_language")
    private val appThemeModeKey = stringPreferencesKey("app_theme_mode")
    private val blockReasonVerbosityKey = booleanPreferencesKey("content_safety_block_reason_verbosity")
    private val contentPolicyAcceptedAtKey = longPreferencesKey("content_policy_accepted_at_millis")
    private val contentPolicyVersionKey = stringPreferencesKey("content_policy_accepted_version")

    private fun presetModelKey(providerId: String) = stringPreferencesKey("preset_provider_${providerId}_model")

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

    override fun presetProviderModel(providerId: String): Flow<String?> = context.preferencesStore.data.map { prefs ->
        prefs[presetModelKey(providerId)]
    }

    override val imBackendOrigin: Flow<String> = context.preferencesStore.data.map { prefs ->
        val storedBackendOrigin = prefs[imBackendOriginKey].orEmpty()
        when {
            storedBackendOrigin.isNotBlank() -> normalizeImBackendOriginForStorage(storedBackendOrigin)
            else -> resolveStoredImBackendOrigin(
                storedBackendOrigin = "",
                legacyHttpBaseUrl = prefs[legacyImHttpBaseUrlKey].orEmpty(),
                legacyWebSocketUrl = prefs[legacyImWebSocketUrlKey].orEmpty(),
                shippedBackendOrigin = "",
            )
        }
    }

    override val imDevUserExternalId: Flow<String> = context.preferencesStore.data.map { prefs ->
        prefs[imDevUserExternalIdKey].orEmpty()
    }

    override val appLanguage: Flow<AppLanguage> = context.preferencesStore.data.map { prefs ->
        AppLanguage.valueOf(prefs[appLanguageKey] ?: AppLanguage.Chinese.name)
    }

    override val appThemeMode: Flow<AppThemeMode> = context.preferencesStore.data.map { prefs ->
        AppThemeMode.valueOf(prefs[appThemeModeKey] ?: AppThemeMode.Light.name)
    }

    override val blockReasonVerbosity: Flow<Boolean> = context.preferencesStore.data.map { prefs ->
        prefs[blockReasonVerbosityKey] ?: true
    }

    override val contentPolicyAcknowledgedAtMillis: Flow<Long?> = context.preferencesStore.data.map { prefs ->
        prefs[contentPolicyAcceptedAtKey]
    }

    override val contentPolicyAcknowledgedVersion: Flow<String> = context.preferencesStore.data.map { prefs ->
        prefs[contentPolicyVersionKey].orEmpty()
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

    override suspend fun setPresetProviderModel(providerId: String, value: String) {
        context.preferencesStore.edit { it[presetModelKey(providerId)] = value }
    }

    override suspend fun setImBackendOrigin(value: String) {
        val storedValue = normalizeImBackendOriginForStorage(value)
        context.preferencesStore.edit { prefs ->
            if (storedValue.isBlank()) {
                prefs.remove(imBackendOriginKey)
            } else {
                prefs[imBackendOriginKey] = storedValue
            }
            prefs.remove(legacyImHttpBaseUrlKey)
            prefs.remove(legacyImWebSocketUrlKey)
        }
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

    override suspend fun setBlockReasonVerbosity(value: Boolean) {
        context.preferencesStore.edit { it[blockReasonVerbosityKey] = value }
    }

    override suspend fun setContentPolicyAcknowledgment(acceptedAtMillis: Long, version: String) {
        context.preferencesStore.edit { prefs ->
            prefs[contentPolicyAcceptedAtKey] = acceptedAtMillis
            prefs[contentPolicyVersionKey] = version
        }
    }

    override suspend fun clearContentPolicyAcknowledgment() {
        context.preferencesStore.edit { prefs ->
            prefs.remove(contentPolicyAcceptedAtKey)
            prefs.remove(contentPolicyVersionKey)
        }
    }
}
