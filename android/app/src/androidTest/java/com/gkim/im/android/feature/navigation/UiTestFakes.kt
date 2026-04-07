package com.gkim.im.android.feature.navigation

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

    override val contactSortMode: Flow<ContactSortMode> = contactSortModeState.asStateFlow()
    override val activeProviderId: Flow<String> = activeProviderIdState.asStateFlow()
    override val customBaseUrl: Flow<String> = customBaseUrlState.asStateFlow()
    override val customModel: Flow<String> = customModelState.asStateFlow()

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
}

internal class UiInMemorySecureStore : SecureKeyValueStore {
    private val values = mutableMapOf<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }
}
