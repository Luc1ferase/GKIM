package com.gkim.im.android.testing

import com.gkim.im.android.core.model.ContactSortMode
import com.gkim.im.android.core.security.SecureKeyValueStore
import com.gkim.im.android.data.local.PreferencesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakePreferencesStore(
    initialSortMode: ContactSortMode = ContactSortMode.Nickname,
    initialProviderId: String = "hunyuan",
    initialBaseUrl: String = "https://api.example.com/v1",
    initialModel: String = "gpt-image-1",
) : PreferencesStore {
    private val contactSortModeState = MutableStateFlow(initialSortMode)
    private val activeProviderIdState = MutableStateFlow(initialProviderId)
    private val customBaseUrlState = MutableStateFlow(initialBaseUrl)
    private val customModelState = MutableStateFlow(initialModel)

    override val contactSortMode: Flow<ContactSortMode> = contactSortModeState.asStateFlow()
    override val activeProviderId: Flow<String> = activeProviderIdState.asStateFlow()
    override val customBaseUrl: Flow<String> = customBaseUrlState.asStateFlow()
    override val customModel: Flow<String> = customModelState.asStateFlow()

    val currentSortMode: ContactSortMode
        get() = contactSortModeState.value

    val currentProviderId: String
        get() = activeProviderIdState.value

    val currentBaseUrl: String
        get() = customBaseUrlState.value

    val currentModel: String
        get() = customModelState.value

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

class InMemorySecureKeyValueStore : SecureKeyValueStore {
    private val values = mutableMapOf<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    fun peek(key: String): String? = values[key]
}
