package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Preset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

sealed interface CompanionPresetMutationResult {
    data class Success(val preset: Preset) : CompanionPresetMutationResult
    data class Rejected(val reason: RejectionReason) : CompanionPresetMutationResult
    data class Failed(val cause: Throwable) : CompanionPresetMutationResult

    enum class RejectionReason {
        UnknownPreset,
        BuiltInPresetImmutable,
        ActivePresetNotDeletable,
    }
}

interface CompanionPresetRepository {
    fun observePresets(): Flow<List<Preset>>
    fun observeActivePreset(): Flow<Preset?>
    suspend fun create(preset: Preset): CompanionPresetMutationResult
    suspend fun update(preset: Preset): CompanionPresetMutationResult
    suspend fun delete(presetId: String): CompanionPresetMutationResult
    suspend fun activate(presetId: String): CompanionPresetMutationResult
    suspend fun duplicate(presetId: String): CompanionPresetMutationResult
    suspend fun refresh()
}

open class DefaultCompanionPresetRepository(
    initialPresets: List<Preset> = emptyList(),
    private val idGenerator: () -> String = { "preset-${UUID.randomUUID()}" },
    private val clock: () -> Long = { System.currentTimeMillis() },
) : CompanionPresetRepository {

    private val presetsState: MutableStateFlow<List<Preset>> =
        MutableStateFlow(enforceSingleActive(initialPresets))

    val presets: StateFlow<List<Preset>> = presetsState

    override fun observePresets(): Flow<List<Preset>> = presetsState

    override fun observeActivePreset(): Flow<Preset?> =
        presetsState.map { list -> list.firstOrNull { it.isActive } }

    override suspend fun create(preset: Preset): CompanionPresetMutationResult {
        val assignedId = if (preset.id.isBlank()) idGenerator() else preset.id
        val now = clock()
        val normalized = preset.copy(
            id = assignedId,
            isBuiltIn = false,
            isActive = false,
            createdAt = if (preset.createdAt == 0L) now else preset.createdAt,
            updatedAt = now,
        )
        presetsState.value = presetsState.value + normalized
        return CompanionPresetMutationResult.Success(normalized)
    }

    override suspend fun update(preset: Preset): CompanionPresetMutationResult {
        val existing = presetsState.value.firstOrNull { it.id == preset.id }
            ?: return CompanionPresetMutationResult.Rejected(
                CompanionPresetMutationResult.RejectionReason.UnknownPreset,
            )
        if (existing.isBuiltIn) {
            return CompanionPresetMutationResult.Rejected(
                CompanionPresetMutationResult.RejectionReason.BuiltInPresetImmutable,
            )
        }
        val updated = preset.copy(
            isBuiltIn = existing.isBuiltIn,
            isActive = existing.isActive,
            createdAt = existing.createdAt,
            updatedAt = clock(),
        )
        presetsState.value = presetsState.value.map {
            if (it.id == existing.id) updated else it
        }
        return CompanionPresetMutationResult.Success(updated)
    }

    override suspend fun delete(presetId: String): CompanionPresetMutationResult {
        val existing = presetsState.value.firstOrNull { it.id == presetId }
            ?: return CompanionPresetMutationResult.Rejected(
                CompanionPresetMutationResult.RejectionReason.UnknownPreset,
            )
        if (existing.isBuiltIn) {
            return CompanionPresetMutationResult.Rejected(
                CompanionPresetMutationResult.RejectionReason.BuiltInPresetImmutable,
            )
        }
        if (existing.isActive) {
            return CompanionPresetMutationResult.Rejected(
                CompanionPresetMutationResult.RejectionReason.ActivePresetNotDeletable,
            )
        }
        presetsState.value = presetsState.value.filterNot { it.id == presetId }
        return CompanionPresetMutationResult.Success(existing)
    }

    override suspend fun activate(presetId: String): CompanionPresetMutationResult {
        if (presetsState.value.none { it.id == presetId }) {
            return CompanionPresetMutationResult.Rejected(
                CompanionPresetMutationResult.RejectionReason.UnknownPreset,
            )
        }
        val now = clock()
        val nextList = presetsState.value.map {
            if (it.id == presetId) {
                it.copy(isActive = true, updatedAt = now)
            } else if (it.isActive) {
                it.copy(isActive = false, updatedAt = now)
            } else {
                it
            }
        }
        presetsState.value = nextList
        val activated = nextList.first { it.id == presetId }
        return CompanionPresetMutationResult.Success(activated)
    }

    override suspend fun duplicate(presetId: String): CompanionPresetMutationResult {
        val source = presetsState.value.firstOrNull { it.id == presetId }
            ?: return CompanionPresetMutationResult.Rejected(
                CompanionPresetMutationResult.RejectionReason.UnknownPreset,
            )
        val now = clock()
        val copy = source.copy(
            id = idGenerator(),
            displayName = LocalizedText(
                english = source.displayName.english + CopySuffix.English,
                chinese = source.displayName.chinese + CopySuffix.Chinese,
            ),
            isBuiltIn = false,
            isActive = false,
            createdAt = now,
            updatedAt = now,
        )
        presetsState.value = presetsState.value + copy
        return CompanionPresetMutationResult.Success(copy)
    }

    override suspend fun refresh() {
        // No-op for in-memory repository; LiveCompanionPresetRepository overrides to pull from backend.
    }

    fun setSnapshot(presets: List<Preset>) {
        presetsState.value = enforceSingleActive(presets)
    }

    protected fun currentPresets(): List<Preset> = presetsState.value

    protected fun applyPresets(presets: List<Preset>) {
        presetsState.value = enforceSingleActive(presets)
    }

    private fun enforceSingleActive(list: List<Preset>): List<Preset> {
        val firstActive = list.indexOfFirst { it.isActive }
        if (firstActive < 0) return list
        return list.mapIndexed { index, preset ->
            if (index == firstActive) preset
            else if (preset.isActive) preset.copy(isActive = false)
            else preset
        }
    }

    object CopySuffix {
        const val English: String = " (copy)"
        const val Chinese: String = "（副本）"
    }
}
