package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.media.GeneratedImageSaveResult
import com.gkim.im.android.core.media.GeneratedImageSaver
import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.AigcProvider
import com.gkim.im.android.core.model.AigcTask
import com.gkim.im.android.core.model.CustomProviderConfig
import com.gkim.im.android.core.model.DraftAigcRequest
import com.gkim.im.android.core.model.MediaInput
import com.gkim.im.android.core.model.PresetProviderConfig
import com.gkim.im.android.core.model.UserPersona
import com.gkim.im.android.data.repository.AigcRepository
import com.gkim.im.android.data.repository.UserPersonaMutationResult
import com.gkim.im.android.data.repository.UserPersonaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

internal class StubAigcRepository : AigcRepository {
    override val providers: StateFlow<List<AigcProvider>> = MutableStateFlow(emptyList())
    override val activeProviderId: StateFlow<String> = MutableStateFlow("")
    override val customProvider: StateFlow<CustomProviderConfig> =
        MutableStateFlow(CustomProviderConfig("", "", ""))
    override val presetProviderConfigs: StateFlow<Map<String, PresetProviderConfig>> =
        MutableStateFlow(emptyMap())
    override val history: StateFlow<List<AigcTask>> = MutableStateFlow(emptyList())
    override val draftRequest: StateFlow<DraftAigcRequest> = MutableStateFlow(DraftAigcRequest())
    override fun setActiveProvider(id: String) = Unit
    override fun updateCustomProvider(baseUrl: String?, model: String?, apiKey: String?) = Unit
    override fun updatePresetProviderConfig(providerId: String, model: String?, apiKey: String?) = Unit
    override fun updateDraft(request: DraftAigcRequest) = Unit
    override suspend fun generate(mode: AigcMode, prompt: String, mediaInput: MediaInput?): AigcTask =
        error("stub aigc generate not wired for these tests")
}

internal object StubImageSaver : GeneratedImageSaver {
    override suspend fun saveImage(source: String, prompt: String): GeneratedImageSaveResult =
        GeneratedImageSaveResult.Failure("stub")
}

internal object StubUserPersonaRepository : UserPersonaRepository {
    override fun observePersonas(): Flow<List<UserPersona>> = flowOf(emptyList())
    override fun observeActivePersona(): Flow<UserPersona?> = flowOf(null)
    override suspend fun create(persona: UserPersona): UserPersonaMutationResult =
        UserPersonaMutationResult.Failed(IllegalStateException("stub"))
    override suspend fun update(persona: UserPersona): UserPersonaMutationResult =
        UserPersonaMutationResult.Failed(IllegalStateException("stub"))
    override suspend fun delete(personaId: String): UserPersonaMutationResult =
        UserPersonaMutationResult.Failed(IllegalStateException("stub"))
    override suspend fun activate(personaId: String): UserPersonaMutationResult =
        UserPersonaMutationResult.Failed(IllegalStateException("stub"))
    override suspend fun duplicate(personaId: String): UserPersonaMutationResult =
        UserPersonaMutationResult.Failed(IllegalStateException("stub"))
    override suspend fun refresh() = Unit
}
