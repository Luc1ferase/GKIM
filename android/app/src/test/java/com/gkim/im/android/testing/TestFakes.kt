package com.gkim.im.android.testing

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.AppThemeMode
import com.gkim.im.android.core.model.ContactSortMode
import com.gkim.im.android.core.security.SecureKeyValueStore
import com.gkim.im.android.data.local.PreferencesStore
import com.gkim.im.android.data.local.RuntimeSessionStore
import com.gkim.im.android.data.remote.im.DEFAULT_IM_DEVELOPER_BACKEND_ORIGIN
import com.gkim.im.android.data.remote.im.normalizeImBackendOriginForStorage
import com.gkim.im.android.data.remote.im.resolveStoredImBackendOrigin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakePreferencesStore(
    initialSortMode: ContactSortMode = ContactSortMode.Nickname,
    initialProviderId: String = "hunyuan",
    initialBaseUrl: String = "https://api.example.com/v1",
    initialModel: String = "gpt-image-1",
    initialPresetModels: Map<String, String> = emptyMap(),
    initialImBackendOrigin: String = DEFAULT_IM_DEVELOPER_BACKEND_ORIGIN,
    initialLegacyImHttpBaseUrl: String = "",
    initialLegacyImWebSocketUrl: String = "",
    initialImDevUserExternalId: String = "nox-dev",
    initialLanguage: AppLanguage = AppLanguage.Chinese,
    initialThemeMode: AppThemeMode = AppThemeMode.Light,
    initialBlockReasonVerbosity: Boolean = true,
    initialContentPolicyAcceptedAtMillis: Long? = null,
    initialContentPolicyVersion: String = "",
) : PreferencesStore {
    private val contactSortModeState = MutableStateFlow(initialSortMode)
    private val activeProviderIdState = MutableStateFlow(initialProviderId)
    private val customBaseUrlState = MutableStateFlow(initialBaseUrl)
    private val customModelState = MutableStateFlow(initialModel)
    private val presetModelStates = initialPresetModels.mapValuesTo(mutableMapOf()) { MutableStateFlow(it.value) }
    private val imBackendOriginState = MutableStateFlow(
        initialImBackendOrigin.takeIf { it.isNotBlank() }?.let(::normalizeImBackendOriginForStorage)
            ?: resolveStoredImBackendOrigin(
                storedBackendOrigin = "",
                legacyHttpBaseUrl = initialLegacyImHttpBaseUrl,
                legacyWebSocketUrl = initialLegacyImWebSocketUrl,
                shippedBackendOrigin = "",
            )
    )
    private val imDevUserExternalIdState = MutableStateFlow(initialImDevUserExternalId)
    private val appLanguageState = MutableStateFlow(initialLanguage)
    private val appThemeModeState = MutableStateFlow(initialThemeMode)
    private val blockReasonVerbosityState = MutableStateFlow(initialBlockReasonVerbosity)
    private val contentPolicyAcceptedAtState = MutableStateFlow(initialContentPolicyAcceptedAtMillis)
    private val contentPolicyVersionState = MutableStateFlow(initialContentPolicyVersion)

    override val contactSortMode: Flow<ContactSortMode> = contactSortModeState.asStateFlow()
    override val activeProviderId: Flow<String> = activeProviderIdState.asStateFlow()
    override val customBaseUrl: Flow<String> = customBaseUrlState.asStateFlow()
    override val customModel: Flow<String> = customModelState.asStateFlow()
    override fun presetProviderModel(providerId: String): Flow<String?> = presetModelStates.getOrPut(providerId) { MutableStateFlow("") }
        .asStateFlow()
        .map { value -> value.takeIf { it.isNotBlank() } }
    override val imBackendOrigin: Flow<String> = imBackendOriginState.asStateFlow()
    override val imDevUserExternalId: Flow<String> = imDevUserExternalIdState.asStateFlow()
    override val appLanguage: Flow<AppLanguage> = appLanguageState.asStateFlow()
    override val appThemeMode: Flow<AppThemeMode> = appThemeModeState.asStateFlow()
    override val blockReasonVerbosity: Flow<Boolean> = blockReasonVerbosityState.asStateFlow()
    override val contentPolicyAcknowledgedAtMillis: Flow<Long?> = contentPolicyAcceptedAtState.asStateFlow()
    override val contentPolicyAcknowledgedVersion: Flow<String> = contentPolicyVersionState.asStateFlow()

    val currentSortMode: ContactSortMode
        get() = contactSortModeState.value

    val currentProviderId: String
        get() = activeProviderIdState.value

    val currentBaseUrl: String
        get() = customBaseUrlState.value

    val currentModel: String
        get() = customModelState.value

    val currentPresetModels: Map<String, String>
        get() = presetModelStates.mapValues { it.value.value }

    val currentImBackendOrigin: String
        get() = imBackendOriginState.value

    val currentImDevUserExternalId: String
        get() = imDevUserExternalIdState.value

    val currentLanguage: AppLanguage
        get() = appLanguageState.value

    val currentThemeMode: AppThemeMode
        get() = appThemeModeState.value

    val currentBlockReasonVerbosity: Boolean
        get() = blockReasonVerbosityState.value

    val currentContentPolicyAcceptedAtMillis: Long?
        get() = contentPolicyAcceptedAtState.value

    val currentContentPolicyVersion: String
        get() = contentPolicyVersionState.value

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

    override suspend fun setPresetProviderModel(providerId: String, value: String) {
        presetModelStates.getOrPut(providerId) { MutableStateFlow("") }.value = value
    }

    override suspend fun setImBackendOrigin(value: String) {
        imBackendOriginState.value = normalizeImBackendOriginForStorage(value)
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

    override suspend fun setBlockReasonVerbosity(value: Boolean) {
        blockReasonVerbosityState.value = value
    }

    override suspend fun setContentPolicyAcknowledgment(acceptedAtMillis: Long, version: String) {
        contentPolicyAcceptedAtState.value = acceptedAtMillis
        contentPolicyVersionState.value = version
    }

    override suspend fun clearContentPolicyAcknowledgment() {
        contentPolicyAcceptedAtState.value = null
        contentPolicyVersionState.value = ""
    }
}

class InMemorySecureKeyValueStore : SecureKeyValueStore {
    private val values = mutableMapOf<String, String>()

    constructor(initialValues: Map<String, String> = emptyMap()) {
        values.putAll(initialValues)
    }

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    fun peek(key: String): String? = values[key]
}

class FakeRuntimeSessionStore(
    override var token: String? = null,
    override var username: String? = null,
    override var baseUrl: String? = null,
) : RuntimeSessionStore {
    override val hasSession: Boolean
        get() = !token.isNullOrBlank()

    override fun clear() {
        token = null
        username = null
        baseUrl = null
    }
}
