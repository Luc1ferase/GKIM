package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.CompanionMemoryPin
import com.gkim.im.android.core.model.CompanionMemoryResetScope
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.data.remote.im.CompanionMemoryPinDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import java.util.UUID

class LiveCompanionMemoryRepository(
    private val backend: ImBackendClient,
    private val baseUrlProvider: () -> String,
    private val tokenProvider: suspend () -> String,
    initial: Map<String, CompanionMemorySnapshot> = emptyMap(),
    idGenerator: () -> String = { "pin-${UUID.randomUUID()}" },
    clock: () -> Long = { System.currentTimeMillis() },
) : DefaultCompanionMemoryRepository(initial, idGenerator, clock) {

    override suspend fun refresh(cardId: String) {
        val baseUrl = baseUrlProvider()
        val token = tokenProvider()
        val memoryDto = backend.getCompanionMemory(baseUrl, token, cardId)
        val pinsDto = backend.listCompanionMemoryPins(baseUrl, token, cardId)
        applySnapshot(
            cardId,
            CompanionMemorySnapshot(
                memory = memoryDto.toCompanionMemory(),
                pins = pinsDto.pins.map { it.toCompanionMemoryPin() },
            ),
        )
    }

    override suspend fun createPin(
        cardId: String,
        sourceMessageId: String?,
        text: LocalizedText,
    ): Result<CompanionMemoryPin> {
        val before = currentSnapshot(cardId)
        val localResult = super.createPin(cardId, sourceMessageId, text)
        val tentative = localResult.getOrElse { return Result.failure(it) }
        return runCatching {
            backend.createCompanionMemoryPin(
                baseUrlProvider(),
                tokenProvider(),
                cardId,
                CompanionMemoryPinDto.fromCompanionMemoryPin(tentative),
            ).toCompanionMemoryPin()
        }.fold(
            onSuccess = { serverPin ->
                val afterLocal = currentSnapshot(cardId)
                applySnapshot(
                    cardId,
                    afterLocal.copy(
                        pins = afterLocal.pins.map { if (it.id == tentative.id) serverPin else it },
                    ),
                )
                Result.success(serverPin)
            },
            onFailure = { error ->
                applySnapshot(cardId, before)
                Result.failure(error)
            },
        )
    }

    override suspend fun updatePin(
        pinId: String,
        text: LocalizedText,
    ): Result<CompanionMemoryPin> {
        val (cardId, _) = locatePin(pinId)
            ?: return Result.failure(UnknownPinException(pinId))
        val before = currentSnapshot(cardId)
        val localResult = super.updatePin(pinId, text)
        val optimistic = localResult.getOrElse { return Result.failure(it) }
        return runCatching {
            backend.updateCompanionMemoryPin(
                baseUrlProvider(),
                tokenProvider(),
                cardId,
                CompanionMemoryPinDto.fromCompanionMemoryPin(optimistic),
            ).toCompanionMemoryPin()
        }.fold(
            onSuccess = { serverPin ->
                val afterLocal = currentSnapshot(cardId)
                applySnapshot(
                    cardId,
                    afterLocal.copy(
                        pins = afterLocal.pins.map { if (it.id == pinId) serverPin else it },
                    ),
                )
                Result.success(serverPin)
            },
            onFailure = { error ->
                applySnapshot(cardId, before)
                Result.failure(error)
            },
        )
    }

    override suspend fun deletePin(pinId: String): Result<Unit> {
        val (cardId, _) = locatePin(pinId)
            ?: return Result.failure(UnknownPinException(pinId))
        val before = currentSnapshot(cardId)
        val localResult = super.deletePin(pinId)
        localResult.getOrElse { return Result.failure(it) }
        return runCatching {
            backend.deleteCompanionMemoryPin(
                baseUrlProvider(),
                tokenProvider(),
                cardId,
                pinId,
            )
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { error ->
                applySnapshot(cardId, before)
                Result.failure(error)
            },
        )
    }

    override suspend fun reset(cardId: String, scope: CompanionMemoryResetScope) {
        super.reset(cardId, scope)
        backend.resetCompanionMemory(baseUrlProvider(), tokenProvider(), cardId, scope)
    }
}
