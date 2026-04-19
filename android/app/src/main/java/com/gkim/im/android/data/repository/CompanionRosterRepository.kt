package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.CompanionDrawResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Result of a mutation against the companion roster.
 * Callers treat [Success] as the authoritative card state after the operation.
 */
sealed interface CompanionCardMutationResult {
    data class Success(val card: CompanionCharacterCard) : CompanionCardMutationResult
    data class Rejected(val reason: RejectionReason) : CompanionCardMutationResult

    enum class RejectionReason {
        PresetImmutable,
        DrawnCardNotDeletable,
        UnknownCharacter,
    }
}

interface CompanionRosterRepository {
    val presetCharacters: StateFlow<List<CompanionCharacterCard>>
    val ownedCharacters: StateFlow<List<CompanionCharacterCard>>
    val userCharacters: StateFlow<List<CompanionCharacterCard>>
    val activeCharacterId: StateFlow<String>
    val lastDrawResult: StateFlow<CompanionDrawResult?>

    fun characterById(characterId: String): CompanionCharacterCard?
    fun activateCharacter(characterId: String)
    suspend fun drawCharacter(): CompanionDrawResult
    fun clearDrawResult()

    /**
     * Create or update a user-authored or draw-acquired companion card.
     *
     * Preset cards (`source == Preset`) are rejected with [CompanionCardMutationResult.Rejected].
     * A new card is detected when [draft]'s id is blank or does not exist yet; in that case the
     * implementation assigns a stable id and treats the card as [CompanionCharacterSource.UserAuthored].
     */
    fun upsertUserCharacter(draft: CompanionCharacterCard): CompanionCardMutationResult

    /**
     * Delete a user-authored companion card. Preset and draw-acquired cards are not deletable
     * through the first-authored editor flow and will return
     * [CompanionCardMutationResult.Rejected].
     */
    fun deleteUserCharacter(characterId: String): CompanionCardMutationResult
}

class DefaultCompanionRosterRepository(
    presetCharacters: List<CompanionCharacterCard>,
    private val drawPool: List<CompanionCharacterCard>,
    initialUserCharacters: List<CompanionCharacterCard> = emptyList(),
    private val idGenerator: () -> String = { "user-${UUID.randomUUID()}" },
) : CompanionRosterRepository {
    private val presetCharactersState = MutableStateFlow(presetCharacters)
    private val ownedCharactersState = MutableStateFlow<List<CompanionCharacterCard>>(emptyList())
    private val userCharactersState = MutableStateFlow(initialUserCharacters)
    private val activeCharacterIdState = MutableStateFlow(presetCharacters.firstOrNull()?.id.orEmpty())
    private val lastDrawResultState = MutableStateFlow<CompanionDrawResult?>(null)
    private var drawCursor = 0

    override val presetCharacters: StateFlow<List<CompanionCharacterCard>> = presetCharactersState
    override val ownedCharacters: StateFlow<List<CompanionCharacterCard>> = ownedCharactersState
    override val userCharacters: StateFlow<List<CompanionCharacterCard>> = userCharactersState
    override val activeCharacterId: StateFlow<String> = activeCharacterIdState
    override val lastDrawResult: StateFlow<CompanionDrawResult?> = lastDrawResultState

    override fun characterById(characterId: String): CompanionCharacterCard? =
        presetCharactersState.value.firstOrNull { it.id == characterId }
            ?: ownedCharactersState.value.firstOrNull { it.id == characterId }
            ?: userCharactersState.value.firstOrNull { it.id == characterId }

    override fun activateCharacter(characterId: String) {
        if (characterById(characterId) != null) {
            activeCharacterIdState.value = characterId
        }
    }

    override suspend fun drawCharacter(): CompanionDrawResult {
        require(drawPool.isNotEmpty()) { "draw pool must not be empty" }
        val card = drawPool[drawCursor % drawPool.size]
        drawCursor += 1
        val wasNew = ownedCharactersState.value.none { it.id == card.id }
        if (wasNew) {
            ownedCharactersState.value = ownedCharactersState.value + card
        }
        return CompanionDrawResult(
            card = card,
            wasNew = wasNew,
        ).also { result ->
            lastDrawResultState.value = result
        }
    }

    override fun clearDrawResult() {
        lastDrawResultState.value = null
    }

    override fun upsertUserCharacter(draft: CompanionCharacterCard): CompanionCardMutationResult {
        val existing = if (draft.id.isNotBlank()) characterById(draft.id) else null
        if (existing != null && existing.source == CompanionCharacterSource.Preset) {
            return CompanionCardMutationResult.Rejected(
                CompanionCardMutationResult.RejectionReason.PresetImmutable
            )
        }

        return when {
            existing == null -> createNewUserCharacter(draft)
            existing.source == CompanionCharacterSource.Drawn -> updateDrawnCharacter(existing, draft)
            existing.source == CompanionCharacterSource.UserAuthored -> updateUserCharacter(existing, draft)
            else -> CompanionCardMutationResult.Rejected(
                CompanionCardMutationResult.RejectionReason.PresetImmutable
            )
        }
    }

    override fun deleteUserCharacter(characterId: String): CompanionCardMutationResult {
        val existing = characterById(characterId)
            ?: return CompanionCardMutationResult.Rejected(
                CompanionCardMutationResult.RejectionReason.UnknownCharacter
            )
        return when (existing.source) {
            CompanionCharacterSource.Preset -> CompanionCardMutationResult.Rejected(
                CompanionCardMutationResult.RejectionReason.PresetImmutable
            )
            CompanionCharacterSource.Drawn -> CompanionCardMutationResult.Rejected(
                CompanionCardMutationResult.RejectionReason.DrawnCardNotDeletable
            )
            CompanionCharacterSource.UserAuthored -> {
                userCharactersState.value = userCharactersState.value.filterNot { it.id == characterId }
                if (activeCharacterIdState.value == characterId) {
                    activeCharacterIdState.value = presetCharactersState.value.firstOrNull()?.id.orEmpty()
                }
                CompanionCardMutationResult.Success(existing)
            }
        }
    }

    private fun createNewUserCharacter(draft: CompanionCharacterCard): CompanionCardMutationResult {
        val assignedId = if (draft.id.isBlank()) idGenerator() else draft.id
        val normalized = draft.copy(
            id = assignedId,
            source = CompanionCharacterSource.UserAuthored,
        )
        userCharactersState.value = userCharactersState.value + normalized
        return CompanionCardMutationResult.Success(normalized)
    }

    private fun updateUserCharacter(
        existing: CompanionCharacterCard,
        draft: CompanionCharacterCard,
    ): CompanionCardMutationResult {
        val updated = draft.copy(
            id = existing.id,
            source = CompanionCharacterSource.UserAuthored,
        )
        userCharactersState.value = userCharactersState.value.map {
            if (it.id == existing.id) updated else it
        }
        return CompanionCardMutationResult.Success(updated)
    }

    private fun updateDrawnCharacter(
        existing: CompanionCharacterCard,
        draft: CompanionCharacterCard,
    ): CompanionCardMutationResult {
        val updated = draft.copy(
            id = existing.id,
            source = CompanionCharacterSource.Drawn,
        )
        ownedCharactersState.value = ownedCharactersState.value.map {
            if (it.id == existing.id) updated else it
        }
        return CompanionCardMutationResult.Success(updated)
    }
}
