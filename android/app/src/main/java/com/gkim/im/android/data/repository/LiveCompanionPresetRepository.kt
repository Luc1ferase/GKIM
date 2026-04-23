package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.Preset
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.PresetDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LiveCompanionPresetRepository(
    private val default: DefaultCompanionPresetRepository,
    private val backendClient: ImBackendClient,
    private val baseUrlProvider: () -> String?,
    private val tokenProvider: () -> String?,
) : CompanionPresetRepository {

    override fun observePresets(): Flow<List<Preset>> = default.observePresets()

    override fun observeActivePreset(): Flow<Preset?> = default.observeActivePreset()

    override suspend fun refresh() {
        val baseUrl = baseUrlProvider() ?: return
        val token = tokenProvider() ?: return
        val merged = runCatching {
            coroutineScope {
                val listDeferred = async(Dispatchers.IO) {
                    backendClient.listPresets(baseUrl, token)
                }
                val activeDeferred = async(Dispatchers.IO) {
                    runCatching { backendClient.getActivePreset(baseUrl, token) }.getOrNull()
                }
                val listDto = listDeferred.await()
                val activeDto = activeDeferred.await()
                mergeRemote(listDto.presets, listDto.activePresetId, activeDto)
            }
        }.getOrNull() ?: return
        default.setSnapshot(merged)
    }

    override suspend fun create(preset: Preset): CompanionPresetMutationResult {
        val localResult = default.create(preset)
        if (localResult !is CompanionPresetMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider()
        val token = tokenProvider()
        if (baseUrl == null || token == null) return localResult

        return try {
            val remote = withContext(Dispatchers.IO) {
                backendClient.createPreset(
                    baseUrl = baseUrl,
                    token = token,
                    preset = PresetDto.fromPreset(localResult.preset),
                )
            }
            val reconciled = remote.toPreset()
            default.setSnapshot(
                default.presets.value.map {
                    if (it.id == localResult.preset.id) reconciled else it
                },
            )
            CompanionPresetMutationResult.Success(reconciled)
        } catch (t: Throwable) {
            default.setSnapshot(
                default.presets.value.filterNot { it.id == localResult.preset.id },
            )
            CompanionPresetMutationResult.Failed(t)
        }
    }

    override suspend fun update(preset: Preset): CompanionPresetMutationResult {
        val snapshot = default.presets.value
        val localResult = default.update(preset)
        if (localResult !is CompanionPresetMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider()
        val token = tokenProvider()
        if (baseUrl == null || token == null) return localResult

        return try {
            val remote = withContext(Dispatchers.IO) {
                backendClient.updatePreset(
                    baseUrl = baseUrl,
                    token = token,
                    preset = PresetDto.fromPreset(localResult.preset),
                )
            }
            val reconciled = remote.toPreset()
            default.setSnapshot(
                default.presets.value.map {
                    if (it.id == reconciled.id) reconciled else it
                },
            )
            CompanionPresetMutationResult.Success(reconciled)
        } catch (t: Throwable) {
            default.setSnapshot(snapshot)
            CompanionPresetMutationResult.Failed(t)
        }
    }

    override suspend fun delete(presetId: String): CompanionPresetMutationResult {
        val snapshot = default.presets.value
        val localResult = default.delete(presetId)
        if (localResult !is CompanionPresetMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider()
        val token = tokenProvider()
        if (baseUrl == null || token == null) return localResult

        return try {
            withContext(Dispatchers.IO) {
                backendClient.deletePreset(baseUrl = baseUrl, token = token, presetId = presetId)
            }
            localResult
        } catch (t: Throwable) {
            default.setSnapshot(snapshot)
            CompanionPresetMutationResult.Failed(t)
        }
    }

    override suspend fun activate(presetId: String): CompanionPresetMutationResult {
        val snapshot = default.presets.value
        val localResult = default.activate(presetId)
        if (localResult !is CompanionPresetMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider()
        val token = tokenProvider()
        if (baseUrl == null || token == null) return localResult

        return try {
            val remote = withContext(Dispatchers.IO) {
                backendClient.activatePreset(baseUrl = baseUrl, token = token, presetId = presetId)
            }
            val activated = remote.toPreset()
            default.setSnapshot(
                default.presets.value.map { p ->
                    when {
                        p.id == activated.id -> activated
                        p.isActive -> p.copy(isActive = false)
                        else -> p
                    }
                },
            )
            CompanionPresetMutationResult.Success(activated)
        } catch (t: Throwable) {
            default.setSnapshot(snapshot)
            CompanionPresetMutationResult.Failed(t)
        }
    }

    override suspend fun duplicate(presetId: String): CompanionPresetMutationResult {
        val localResult = default.duplicate(presetId)
        if (localResult !is CompanionPresetMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider()
        val token = tokenProvider()
        if (baseUrl == null || token == null) return localResult

        return try {
            val remote = withContext(Dispatchers.IO) {
                backendClient.createPreset(
                    baseUrl = baseUrl,
                    token = token,
                    preset = PresetDto.fromPreset(localResult.preset),
                )
            }
            val reconciled = remote.toPreset()
            default.setSnapshot(
                default.presets.value.map {
                    if (it.id == localResult.preset.id) reconciled else it
                },
            )
            CompanionPresetMutationResult.Success(reconciled)
        } catch (t: Throwable) {
            default.setSnapshot(
                default.presets.value.filterNot { it.id == localResult.preset.id },
            )
            CompanionPresetMutationResult.Failed(t)
        }
    }

    private fun mergeRemote(
        remoteList: List<PresetDto>,
        activePresetId: String?,
        activeDto: PresetDto?,
    ): List<Preset> {
        val byId = remoteList.associateBy { it.id }.toMutableMap()
        if (activeDto != null) {
            byId[activeDto.id] = activeDto
        }
        val targetActiveId = activePresetId ?: activeDto?.id
        return byId.values.map { it.toPreset() }.map { preset ->
            if (targetActiveId != null) preset.copy(isActive = preset.id == targetActiveId)
            else preset
        }
    }
}
