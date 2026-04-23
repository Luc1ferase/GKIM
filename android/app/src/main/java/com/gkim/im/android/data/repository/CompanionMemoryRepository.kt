package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.CompanionMemory
import com.gkim.im.android.core.model.CompanionMemoryPin
import com.gkim.im.android.core.model.CompanionMemoryResetScope
import com.gkim.im.android.core.model.LocalizedText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.UUID

interface CompanionMemoryRepository {
    fun observeMemory(cardId: String): Flow<CompanionMemory?>
    fun observePins(cardId: String): Flow<List<CompanionMemoryPin>>
    suspend fun createPin(
        cardId: String,
        sourceMessageId: String?,
        text: LocalizedText,
    ): Result<CompanionMemoryPin>

    suspend fun updatePin(pinId: String, text: LocalizedText): Result<CompanionMemoryPin>
    suspend fun deletePin(pinId: String): Result<Unit>
    suspend fun reset(cardId: String, scope: CompanionMemoryResetScope)
    suspend fun refresh(cardId: String)
}

class UnknownPinException(pinId: String) : NoSuchElementException(
    "No pin with id=$pinId in local repository state",
)

data class CompanionMemorySnapshot(
    val memory: CompanionMemory? = null,
    val pins: List<CompanionMemoryPin> = emptyList(),
)

open class DefaultCompanionMemoryRepository(
    initial: Map<String, CompanionMemorySnapshot> = emptyMap(),
    private val idGenerator: () -> String = { "pin-${UUID.randomUUID()}" },
    private val clock: () -> Long = { System.currentTimeMillis() },
) : CompanionMemoryRepository {

    private val state: MutableStateFlow<Map<String, CompanionMemorySnapshot>> =
        MutableStateFlow(initial)

    override fun observeMemory(cardId: String): Flow<CompanionMemory?> =
        state.map { it[cardId]?.memory }.distinctUntilChanged()

    override fun observePins(cardId: String): Flow<List<CompanionMemoryPin>> =
        state.map { it[cardId]?.pins.orEmpty() }.distinctUntilChanged()

    override suspend fun createPin(
        cardId: String,
        sourceMessageId: String?,
        text: LocalizedText,
    ): Result<CompanionMemoryPin> {
        val pin = CompanionMemoryPin(
            id = idGenerator(),
            sourceMessageId = sourceMessageId,
            text = text,
            createdAt = clock(),
            pinnedByUser = true,
        )
        mutate(cardId) { it.copy(pins = it.pins + pin) }
        return Result.success(pin)
    }

    override suspend fun updatePin(pinId: String, text: LocalizedText): Result<CompanionMemoryPin> {
        val (cardId, existing) = locatePin(pinId)
            ?: return Result.failure(UnknownPinException(pinId))
        val updated = existing.copy(text = text)
        mutate(cardId) { snapshot ->
            snapshot.copy(
                pins = snapshot.pins.map { if (it.id == pinId) updated else it },
            )
        }
        return Result.success(updated)
    }

    override suspend fun deletePin(pinId: String): Result<Unit> {
        val (cardId, _) = locatePin(pinId)
            ?: return Result.failure(UnknownPinException(pinId))
        mutate(cardId) { snapshot ->
            snapshot.copy(pins = snapshot.pins.filterNot { it.id == pinId })
        }
        return Result.success(Unit)
    }

    override suspend fun reset(cardId: String, scope: CompanionMemoryResetScope) {
        mutate(cardId) { snapshot ->
            when (scope) {
                CompanionMemoryResetScope.Pins -> snapshot.copy(pins = emptyList())
                CompanionMemoryResetScope.Summary -> snapshot.copy(
                    memory = snapshot.memory?.copy(
                        summary = LocalizedText.Empty,
                        summaryUpdatedAt = 0L,
                        summaryTurnCursor = 0,
                    ),
                )
                CompanionMemoryResetScope.All -> CompanionMemorySnapshot(
                    memory = snapshot.memory?.copy(
                        summary = LocalizedText.Empty,
                        summaryUpdatedAt = 0L,
                        summaryTurnCursor = 0,
                    ),
                    pins = emptyList(),
                )
            }
        }
    }

    override suspend fun refresh(cardId: String) {
        // No-op for in-memory repository; LiveCompanionMemoryRepository overrides to pull from backend.
    }

    fun setSnapshot(cardId: String, snapshot: CompanionMemorySnapshot) {
        state.value = state.value + (cardId to snapshot)
    }

    protected fun currentSnapshot(cardId: String): CompanionMemorySnapshot =
        state.value[cardId] ?: CompanionMemorySnapshot()

    protected fun applySnapshot(cardId: String, snapshot: CompanionMemorySnapshot) {
        state.value = state.value + (cardId to snapshot)
    }

    private fun mutate(cardId: String, transform: (CompanionMemorySnapshot) -> CompanionMemorySnapshot) {
        val current = state.value[cardId] ?: CompanionMemorySnapshot()
        state.value = state.value + (cardId to transform(current))
    }

    protected fun locatePin(pinId: String): Pair<String, CompanionMemoryPin>? {
        state.value.forEach { (cardId, snapshot) ->
            val found = snapshot.pins.firstOrNull { it.id == pinId }
            if (found != null) return cardId to found
        }
        return null
    }
}
