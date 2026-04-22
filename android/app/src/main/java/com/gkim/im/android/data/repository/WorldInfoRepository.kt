package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.Lorebook
import com.gkim.im.android.core.model.LorebookBinding
import com.gkim.im.android.core.model.LorebookEntry
import com.gkim.im.android.core.model.LocalizedText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

sealed interface WorldInfoMutationResult<out T> {
    data class Success<T>(val value: T) : WorldInfoMutationResult<T>
    data class Rejected(val reason: RejectionReason) : WorldInfoMutationResult<Nothing>
    data class Failed(val cause: Throwable) : WorldInfoMutationResult<Nothing>

    enum class RejectionReason {
        UnknownLorebook,
        UnknownEntry,
        UnknownBinding,
        BuiltInLorebookImmutable,
        LorebookHasBindings,
        BindingAlreadyExists,
    }
}

interface WorldInfoRepository {
    fun observeLorebooks(): Flow<List<Lorebook>>
    fun observeEntries(): Flow<Map<String, List<LorebookEntry>>>
    fun observeBindings(): Flow<Map<String, List<LorebookBinding>>>

    suspend fun refresh()

    suspend fun createLorebook(draft: Lorebook): WorldInfoMutationResult<Lorebook>
    suspend fun updateLorebook(lorebook: Lorebook): WorldInfoMutationResult<Lorebook>
    suspend fun deleteLorebook(lorebookId: String): WorldInfoMutationResult<Lorebook>
    suspend fun duplicateLorebook(lorebookId: String): WorldInfoMutationResult<Lorebook>

    suspend fun createEntry(
        lorebookId: String,
        draft: LorebookEntry,
    ): WorldInfoMutationResult<LorebookEntry>

    suspend fun updateEntry(entry: LorebookEntry): WorldInfoMutationResult<LorebookEntry>

    suspend fun deleteEntry(
        lorebookId: String,
        entryId: String,
    ): WorldInfoMutationResult<LorebookEntry>

    suspend fun bind(
        lorebookId: String,
        characterId: String,
        isPrimary: Boolean,
    ): WorldInfoMutationResult<LorebookBinding>

    suspend fun updateBinding(
        lorebookId: String,
        characterId: String,
        isPrimary: Boolean,
    ): WorldInfoMutationResult<LorebookBinding>

    suspend fun unbind(
        lorebookId: String,
        characterId: String,
    ): WorldInfoMutationResult<LorebookBinding>
}

class DefaultWorldInfoRepository(
    initialLorebooks: List<Lorebook> = emptyList(),
    initialEntries: Map<String, List<LorebookEntry>> = emptyMap(),
    initialBindings: Map<String, List<LorebookBinding>> = emptyMap(),
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() },
) : WorldInfoRepository {

    private val lorebooksState = MutableStateFlow(initialLorebooks)
    private val entriesState = MutableStateFlow(initialEntries)
    private val bindingsState = MutableStateFlow(initialBindings)

    val lorebooks: StateFlow<List<Lorebook>> = lorebooksState
    val entries: StateFlow<Map<String, List<LorebookEntry>>> = entriesState
    val bindings: StateFlow<Map<String, List<LorebookBinding>>> = bindingsState

    override fun observeLorebooks(): Flow<List<Lorebook>> = lorebooksState
    override fun observeEntries(): Flow<Map<String, List<LorebookEntry>>> = entriesState
    override fun observeBindings(): Flow<Map<String, List<LorebookBinding>>> = bindingsState

    override suspend fun refresh() = Unit

    override suspend fun createLorebook(
        draft: Lorebook,
    ): WorldInfoMutationResult<Lorebook> {
        val assignedId = if (draft.id.isBlank()) "lorebook-${idGenerator()}" else draft.id
        val now = clock()
        val normalized = draft.copy(
            id = assignedId,
            isBuiltIn = false,
            createdAt = if (draft.createdAt == 0L) now else draft.createdAt,
            updatedAt = now,
        )
        lorebooksState.value = lorebooksState.value + normalized
        entriesState.value = entriesState.value + (normalized.id to emptyList())
        bindingsState.value = bindingsState.value + (normalized.id to emptyList())
        return WorldInfoMutationResult.Success(normalized)
    }

    override suspend fun updateLorebook(
        lorebook: Lorebook,
    ): WorldInfoMutationResult<Lorebook> {
        val existing = lorebooksState.value.firstOrNull { it.id == lorebook.id }
            ?: return WorldInfoMutationResult.Rejected(
                WorldInfoMutationResult.RejectionReason.UnknownLorebook,
            )
        if (existing.isBuiltIn) {
            return WorldInfoMutationResult.Rejected(
                WorldInfoMutationResult.RejectionReason.BuiltInLorebookImmutable,
            )
        }
        val updated = lorebook.copy(
            isBuiltIn = existing.isBuiltIn,
            createdAt = existing.createdAt,
            updatedAt = clock(),
        )
        lorebooksState.value = lorebooksState.value.map {
            if (it.id == existing.id) updated else it
        }
        return WorldInfoMutationResult.Success(updated)
    }

    override suspend fun deleteLorebook(
        lorebookId: String,
    ): WorldInfoMutationResult<Lorebook> {
        val existing = lorebooksState.value.firstOrNull { it.id == lorebookId }
            ?: return WorldInfoMutationResult.Rejected(
                WorldInfoMutationResult.RejectionReason.UnknownLorebook,
            )
        if (existing.isBuiltIn) {
            return WorldInfoMutationResult.Rejected(
                WorldInfoMutationResult.RejectionReason.BuiltInLorebookImmutable,
            )
        }
        val boundCharacters = bindingsState.value[lorebookId].orEmpty()
        if (boundCharacters.isNotEmpty()) {
            return WorldInfoMutationResult.Rejected(
                WorldInfoMutationResult.RejectionReason.LorebookHasBindings,
            )
        }
        lorebooksState.value = lorebooksState.value.filterNot { it.id == lorebookId }
        entriesState.value = entriesState.value - lorebookId
        bindingsState.value = bindingsState.value - lorebookId
        return WorldInfoMutationResult.Success(existing)
    }

    override suspend fun duplicateLorebook(
        lorebookId: String,
    ): WorldInfoMutationResult<Lorebook> {
        val source = lorebooksState.value.firstOrNull { it.id == lorebookId }
            ?: return WorldInfoMutationResult.Rejected(
                WorldInfoMutationResult.RejectionReason.UnknownLorebook,
            )
        val now = clock()
        val copyId = "lorebook-${idGenerator()}"
        val copy = source.copy(
            id = copyId,
            displayName = LocalizedText(
                english = source.displayName.english + CopySuffix.English,
                chinese = source.displayName.chinese + CopySuffix.Chinese,
            ),
            isBuiltIn = false,
            createdAt = now,
            updatedAt = now,
        )
        lorebooksState.value = lorebooksState.value + copy
        val duplicatedEntries = entriesState.value[lorebookId].orEmpty().map { entry ->
            entry.copy(
                id = "entry-${idGenerator()}",
                lorebookId = copyId,
            )
        }
        entriesState.value = entriesState.value + (copyId to duplicatedEntries)
        bindingsState.value = bindingsState.value + (copyId to emptyList())
        return WorldInfoMutationResult.Success(copy)
    }

    override suspend fun createEntry(
        lorebookId: String,
        draft: LorebookEntry,
    ): WorldInfoMutationResult<LorebookEntry> {
        if (lorebooksState.value.none { it.id == lorebookId }) {
            return WorldInfoMutationResult.Rejected(
                WorldInfoMutationResult.RejectionReason.UnknownLorebook,
            )
        }
        val assignedId = if (draft.id.isBlank()) "entry-${idGenerator()}" else draft.id
        val normalized = draft.copy(id = assignedId, lorebookId = lorebookId)
        val current = entriesState.value[lorebookId].orEmpty()
        entriesState.value = entriesState.value + (lorebookId to (current + normalized))
        return WorldInfoMutationResult.Success(normalized)
    }

    override suspend fun updateEntry(
        entry: LorebookEntry,
    ): WorldInfoMutationResult<LorebookEntry> {
        val current = entriesState.value[entry.lorebookId].orEmpty()
        current.firstOrNull { it.id == entry.id }
            ?: return WorldInfoMutationResult.Rejected(
                WorldInfoMutationResult.RejectionReason.UnknownEntry,
            )
        val next = current.map { if (it.id == entry.id) entry else it }
        entriesState.value = entriesState.value + (entry.lorebookId to next)
        return WorldInfoMutationResult.Success(entry)
    }

    override suspend fun deleteEntry(
        lorebookId: String,
        entryId: String,
    ): WorldInfoMutationResult<LorebookEntry> {
        val current = entriesState.value[lorebookId].orEmpty()
        val existing = current.firstOrNull { it.id == entryId }
            ?: return WorldInfoMutationResult.Rejected(
                WorldInfoMutationResult.RejectionReason.UnknownEntry,
            )
        entriesState.value = entriesState.value + (
            lorebookId to current.filterNot { it.id == entryId }
        )
        return WorldInfoMutationResult.Success(existing)
    }

    override suspend fun bind(
        lorebookId: String,
        characterId: String,
        isPrimary: Boolean,
    ): WorldInfoMutationResult<LorebookBinding> {
        if (lorebooksState.value.none { it.id == lorebookId }) {
            return WorldInfoMutationResult.Rejected(
                WorldInfoMutationResult.RejectionReason.UnknownLorebook,
            )
        }
        val current = bindingsState.value[lorebookId].orEmpty()
        if (current.any { it.characterId == characterId }) {
            return WorldInfoMutationResult.Rejected(
                WorldInfoMutationResult.RejectionReason.BindingAlreadyExists,
            )
        }
        val binding = LorebookBinding(
            lorebookId = lorebookId,
            characterId = characterId,
            isPrimary = isPrimary,
        )
        bindingsState.value = applyPrimarySweep(
            map = bindingsState.value,
            lorebookId = lorebookId,
            writes = current + binding,
            primaryTarget = if (isPrimary) characterId else null,
        )
        return WorldInfoMutationResult.Success(binding)
    }

    override suspend fun updateBinding(
        lorebookId: String,
        characterId: String,
        isPrimary: Boolean,
    ): WorldInfoMutationResult<LorebookBinding> {
        val current = bindingsState.value[lorebookId].orEmpty()
        val existing = current.firstOrNull { it.characterId == characterId }
            ?: return WorldInfoMutationResult.Rejected(
                WorldInfoMutationResult.RejectionReason.UnknownBinding,
            )
        val updated = existing.copy(isPrimary = isPrimary)
        val writes = current.map { if (it.characterId == characterId) updated else it }
        bindingsState.value = applyPrimarySweep(
            map = bindingsState.value,
            lorebookId = lorebookId,
            writes = writes,
            primaryTarget = if (isPrimary) characterId else null,
        )
        return WorldInfoMutationResult.Success(updated)
    }

    override suspend fun unbind(
        lorebookId: String,
        characterId: String,
    ): WorldInfoMutationResult<LorebookBinding> {
        val current = bindingsState.value[lorebookId].orEmpty()
        val existing = current.firstOrNull { it.characterId == characterId }
            ?: return WorldInfoMutationResult.Rejected(
                WorldInfoMutationResult.RejectionReason.UnknownBinding,
            )
        bindingsState.value = bindingsState.value + (
            lorebookId to current.filterNot { it.characterId == characterId }
        )
        return WorldInfoMutationResult.Success(existing)
    }

    fun setSnapshot(
        lorebooks: List<Lorebook>,
        entries: Map<String, List<LorebookEntry>>,
        bindings: Map<String, List<LorebookBinding>>,
    ) {
        lorebooksState.value = lorebooks
        entriesState.value = entries
        bindingsState.value = bindings
    }

    fun replaceLorebook(lorebook: Lorebook) {
        lorebooksState.value = lorebooksState.value.map {
            if (it.id == lorebook.id) lorebook else it
        }
    }

    fun replaceEntries(lorebookId: String, entries: List<LorebookEntry>) {
        entriesState.value = entriesState.value + (lorebookId to entries)
    }

    fun replaceBindings(lorebookId: String, bindings: List<LorebookBinding>) {
        bindingsState.value = bindingsState.value + (lorebookId to bindings)
    }

    fun removeLorebook(lorebookId: String) {
        lorebooksState.value = lorebooksState.value.filterNot { it.id == lorebookId }
        entriesState.value = entriesState.value - lorebookId
        bindingsState.value = bindingsState.value - lorebookId
    }

    private fun applyPrimarySweep(
        map: Map<String, List<LorebookBinding>>,
        lorebookId: String,
        writes: List<LorebookBinding>,
        primaryTarget: String?,
    ): Map<String, List<LorebookBinding>> {
        val next = map.toMutableMap()
        next[lorebookId] = writes
        if (primaryTarget != null) {
            for ((otherLorebookId, list) in map) {
                if (otherLorebookId == lorebookId) continue
                val demoted = list.map {
                    if (it.characterId == primaryTarget && it.isPrimary) {
                        it.copy(isPrimary = false)
                    } else {
                        it
                    }
                }
                next[otherLorebookId] = demoted
            }
        }
        return next
    }

    object CopySuffix {
        const val English: String = " (copy)"
        const val Chinese: String = "（副本）"
    }
}
