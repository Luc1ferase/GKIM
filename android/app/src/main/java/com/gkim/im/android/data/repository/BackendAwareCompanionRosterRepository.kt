package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.CompanionDrawResult
import com.gkim.im.android.data.local.RuntimeSessionStore
import com.gkim.im.android.data.remote.im.CompanionCharacterCardDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BackendAwareCompanionRosterRepository(
    private val backendClient: ImBackendClient,
    private val sessionStore: RuntimeSessionStore,
    presetCharacters: List<CompanionCharacterCard>,
    drawPool: List<CompanionCharacterCard>,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CompanionRosterRepository {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val fallbackRepository = DefaultCompanionRosterRepository(
        presetCharacters = presetCharacters,
        drawPool = drawPool,
    )
    private val presetCharactersState = MutableStateFlow(fallbackRepository.presetCharacters.value)
    private val ownedCharactersState = MutableStateFlow(fallbackRepository.ownedCharacters.value)
    private val userCharactersState = MutableStateFlow(fallbackRepository.userCharacters.value)
    private val activeCharacterIdState = MutableStateFlow(fallbackRepository.activeCharacterId.value)
    private val lastDrawResultState = MutableStateFlow<CompanionDrawResult?>(fallbackRepository.lastDrawResult.value)

    override val presetCharacters: StateFlow<List<CompanionCharacterCard>> = presetCharactersState
    override val ownedCharacters: StateFlow<List<CompanionCharacterCard>> = ownedCharactersState
    override val userCharacters: StateFlow<List<CompanionCharacterCard>> = userCharactersState
    override val activeCharacterId: StateFlow<String> = activeCharacterIdState
    override val lastDrawResult: StateFlow<CompanionDrawResult?> = lastDrawResultState

    init {
        syncFromFallback()
        if (canUseBackend()) {
            scope.launch {
                runCatching {
                    backendClient.loadCompanionRoster(
                        baseUrl = sessionStore.baseUrl.orEmpty(),
                        token = sessionStore.token.orEmpty(),
                    )
                }.onSuccess { roster ->
                    val remotePreset = roster.presetCharacters.map { it.toCompanionCharacterCard() }
                    val remoteOwned = roster.ownedCharacters.map { it.toCompanionCharacterCard() }
                    presetCharactersState.value = remotePreset.filter { it.source == CompanionCharacterSource.Preset }
                    ownedCharactersState.value = remoteOwned.filter { it.source == CompanionCharacterSource.Drawn }
                    userCharactersState.value = remoteOwned.filter { it.source == CompanionCharacterSource.UserAuthored }
                    activeCharacterIdState.value = roster.activeCharacterId.orEmpty()
                }
            }
        }
    }

    override fun characterById(characterId: String): CompanionCharacterCard? =
        presetCharactersState.value.firstOrNull { it.id == characterId }
            ?: ownedCharactersState.value.firstOrNull { it.id == characterId }
            ?: userCharactersState.value.firstOrNull { it.id == characterId }

    override fun activateCharacter(characterId: String) {
        fallbackRepository.activateCharacter(characterId)
        syncFromFallback()
        if (characterById(characterId) == null) {
            return
        }
        activeCharacterIdState.value = characterId
        if (canUseBackend()) {
            scope.launch {
                runCatching {
                    backendClient.selectCompanionCharacter(
                        baseUrl = sessionStore.baseUrl.orEmpty(),
                        token = sessionStore.token.orEmpty(),
                        characterId = characterId,
                    )
                }.onSuccess { selection ->
                    activeCharacterIdState.value = selection.characterId
                }
            }
        }
    }

    override suspend fun drawCharacter(): CompanionDrawResult {
        if (canUseBackend()) {
            return runCatching {
                backendClient.drawCompanionCharacter(
                    baseUrl = sessionStore.baseUrl.orEmpty(),
                    token = sessionStore.token.orEmpty(),
                )
            }.map { result ->
                val mapped = result.toCompanionDrawResult()
                if (mapped.card.source == CompanionCharacterSource.UserAuthored) {
                    if (userCharactersState.value.none { it.id == mapped.card.id }) {
                        userCharactersState.value = userCharactersState.value + mapped.card
                    }
                } else if (ownedCharactersState.value.none { it.id == mapped.card.id }) {
                    ownedCharactersState.value = ownedCharactersState.value + mapped.card
                }
                lastDrawResultState.value = mapped
                mapped
            }.getOrElse {
                fallbackRepository.drawCharacter().also {
                    syncFromFallback()
                }
            }
        }
        return fallbackRepository.drawCharacter().also {
            syncFromFallback()
        }
    }

    override fun clearDrawResult() {
        fallbackRepository.clearDrawResult()
        lastDrawResultState.value = null
    }

    override fun upsertUserCharacter(draft: CompanionCharacterCard): CompanionCardMutationResult {
        val fallbackResult = fallbackRepository.upsertUserCharacter(draft)
        syncFromFallback()
        if (fallbackResult !is CompanionCardMutationResult.Success || !canUseBackend()) {
            return fallbackResult
        }

        scope.launch {
            runCatching {
                backendClient.upsertCompanionCharacter(
                    baseUrl = sessionStore.baseUrl.orEmpty(),
                    token = sessionStore.token.orEmpty(),
                    card = CompanionCharacterCardDto.fromCompanionCharacterCard(fallbackResult.card),
                )
            }.onSuccess { remoteCard ->
                fallbackRepository.upsertUserCharacter(remoteCard.toCompanionCharacterCard())
                syncFromFallback()
            }
        }
        return fallbackResult
    }

    override fun deleteUserCharacter(characterId: String): CompanionCardMutationResult {
        val fallbackResult = fallbackRepository.deleteUserCharacter(characterId)
        syncFromFallback()
        if (fallbackResult !is CompanionCardMutationResult.Success || !canUseBackend()) {
            return fallbackResult
        }

        scope.launch {
            runCatching {
                backendClient.deleteCompanionCharacter(
                    baseUrl = sessionStore.baseUrl.orEmpty(),
                    token = sessionStore.token.orEmpty(),
                    characterId = characterId,
                )
            }
        }
        return fallbackResult
    }

    private fun canUseBackend(): Boolean = sessionStore.hasSession && !sessionStore.baseUrl.isNullOrBlank()

    private fun syncFromFallback() {
        presetCharactersState.value = fallbackRepository.presetCharacters.value
        ownedCharactersState.value = fallbackRepository.ownedCharacters.value
        userCharactersState.value = fallbackRepository.userCharacters.value
        activeCharacterIdState.value = fallbackRepository.activeCharacterId.value
        lastDrawResultState.value = fallbackRepository.lastDrawResult.value
    }
}
