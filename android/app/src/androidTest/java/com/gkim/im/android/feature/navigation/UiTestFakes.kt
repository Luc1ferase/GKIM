package com.gkim.im.android.feature.navigation

import com.gkim.im.android.core.model.MediaInput
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.AppThemeMode
import com.gkim.im.android.core.model.ContactSortMode
import com.gkim.im.android.core.security.SecureKeyValueStore
import com.gkim.im.android.data.local.PreferencesStore
import com.gkim.im.android.data.remote.aigc.EncodedMediaPayload
import com.gkim.im.android.data.remote.aigc.MediaInputEncoder
import com.gkim.im.android.data.remote.aigc.RemoteAigcGenerateRequest
import com.gkim.im.android.data.remote.aigc.RemoteAigcGenerateResult
import com.gkim.im.android.data.remote.aigc.RemoteAigcProviderClient
import com.gkim.im.android.data.remote.im.normalizeImBackendOriginForStorage
import com.gkim.im.android.data.remote.im.resolveStoredImBackendOrigin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

internal class UiTestPreferencesStore(
    initialActiveProviderId: String = "hunyuan",
    initialImBackendOrigin: String = "",
) : PreferencesStore {
    private val contactSortModeState = MutableStateFlow(ContactSortMode.Nickname)
    private val activeProviderIdState = MutableStateFlow(initialActiveProviderId)
    private val customBaseUrlState = MutableStateFlow("https://api.example.com/v1")
    private val customModelState = MutableStateFlow("gpt-image-1")
    private val presetModelStates = mutableMapOf<String, MutableStateFlow<String>>()
    private val imBackendOriginState = MutableStateFlow(
        initialImBackendOrigin.takeIf { it.isNotBlank() }?.let(::normalizeImBackendOriginForStorage)
            ?: resolveStoredImBackendOrigin(
                storedBackendOrigin = "",
                legacyHttpBaseUrl = "",
                legacyWebSocketUrl = "",
                shippedBackendOrigin = "",
            )
    )
    private val imDevUserExternalIdState = MutableStateFlow("nox-dev")
    private val appLanguageState = MutableStateFlow(AppLanguage.Chinese)
    private val appThemeModeState = MutableStateFlow(AppThemeMode.Light)

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

    val currentLanguage: AppLanguage
        get() = appLanguageState.value

    val currentThemeMode: AppThemeMode
        get() = appThemeModeState.value

    val currentImBackendOrigin: String
        get() = imBackendOriginState.value

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
}

internal class UiInMemorySecureStore : SecureKeyValueStore {
    private val values = mutableMapOf<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }
}

internal class UiTestRemoteAigcProviderClient(
    private val providerId: String,
) : RemoteAigcProviderClient {
    val requests = mutableListOf<RemoteAigcGenerateRequest>()

    override suspend fun generate(request: RemoteAigcGenerateRequest): RemoteAigcGenerateResult {
        requests += request
        return RemoteAigcGenerateResult(
            remoteId = "$providerId-${requests.size}",
            outputUrl = "https://cdn.example.com/${providerId}-${requests.size}.png",
        )
    }
}

internal class UiTestMediaInputEncoder : MediaInputEncoder {
    override suspend fun encode(mediaInput: MediaInput): EncodedMediaPayload {
        return EncodedMediaPayload(
            base64Data = "UI_TEST_BASE64",
            mimeType = "image/png",
        )
    }
}
