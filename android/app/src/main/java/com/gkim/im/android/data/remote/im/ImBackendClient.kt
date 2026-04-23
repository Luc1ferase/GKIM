package com.gkim.im.android.data.remote.im

import com.gkim.im.android.core.model.CompanionMemoryResetScope

interface ImBackendClient {
    suspend fun issueDevSession(baseUrl: String, externalId: String): DevSessionResponseDto
    suspend fun register(baseUrl: String, username: String, password: String, displayName: String): AuthResponseDto
    suspend fun login(baseUrl: String, username: String, password: String): AuthResponseDto
    suspend fun searchUsers(baseUrl: String, token: String, query: String): List<UserSearchResultDto>
    suspend fun sendFriendRequest(baseUrl: String, token: String, toUserId: String): FriendRequestViewDto
    suspend fun listFriendRequests(baseUrl: String, token: String): List<FriendRequestViewDto>
    suspend fun acceptFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto
    suspend fun rejectFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto
    suspend fun loadBootstrap(baseUrl: String, token: String): BootstrapBundleDto
    suspend fun sendDirectImageMessage(
        baseUrl: String,
        token: String,
        request: SendDirectImageMessageRequestDto,
    ): SendDirectMessageResultDto
    suspend fun loadHistory(
        baseUrl: String,
        token: String,
        conversationId: String,
        limit: Int = 50,
        before: String? = null,
    ): MessageHistoryPageDto

    suspend fun loadCompanionRoster(baseUrl: String, token: String): CompanionRosterDto {
        error("loadCompanionRoster is not implemented for this backend client")
    }

    suspend fun drawCompanionCharacter(baseUrl: String, token: String): CompanionDrawResultDto {
        error("drawCompanionCharacter is not implemented for this backend client")
    }

    suspend fun selectCompanionCharacter(
        baseUrl: String,
        token: String,
        characterId: String,
    ): ActiveCompanionSelectionDto {
        error("selectCompanionCharacter is not implemented for this backend client")
    }

    suspend fun upsertCompanionCharacter(
        baseUrl: String,
        token: String,
        card: CompanionCharacterCardDto,
    ): CompanionCharacterCardDto {
        error("upsertCompanionCharacter is not implemented for this backend client")
    }

    suspend fun deleteCompanionCharacter(
        baseUrl: String,
        token: String,
        characterId: String,
    ) {
        error("deleteCompanionCharacter is not implemented for this backend client")
    }

    suspend fun submitCompanionTurn(
        baseUrl: String,
        token: String,
        request: CompanionTurnSubmitRequestDto,
    ): CompanionTurnRecordDto {
        error("submitCompanionTurn is not implemented for this backend client")
    }

    suspend fun regenerateCompanionTurn(
        baseUrl: String,
        token: String,
        turnId: String,
        clientTurnId: String,
    ): CompanionTurnRecordDto {
        error("regenerateCompanionTurn is not implemented for this backend client")
    }

    suspend fun listPendingCompanionTurns(
        baseUrl: String,
        token: String,
    ): CompanionTurnPendingListDto {
        error("listPendingCompanionTurns is not implemented for this backend client")
    }

    suspend fun snapshotCompanionTurn(
        baseUrl: String,
        token: String,
        turnId: String,
    ): CompanionTurnRecordDto {
        error("snapshotCompanionTurn is not implemented for this backend client")
    }

    suspend fun importCardPreview(
        baseUrl: String,
        token: String,
        bytes: ByteArray,
        filename: String,
    ): CardImportPreviewDto {
        error("importCardPreview is not implemented for this backend client")
    }

    suspend fun importCardCommit(
        baseUrl: String,
        token: String,
        preview: CardImportPreviewDto,
        overrides: CompanionCharacterCardDto? = null,
        languageOverride: String? = null,
    ): CompanionCharacterCardDto {
        error("importCardCommit is not implemented for this backend client")
    }

    suspend fun exportCard(
        baseUrl: String,
        token: String,
        cardId: String,
        format: String,
        language: String,
        includeTranslationAlt: Boolean = false,
    ): CardExportResponseDto {
        error("exportCard is not implemented for this backend client")
    }

    suspend fun listPersonas(baseUrl: String, token: String): UserPersonaListDto {
        error("listPersonas is not implemented for this backend client")
    }

    suspend fun createPersona(
        baseUrl: String,
        token: String,
        persona: UserPersonaDto,
    ): UserPersonaDto {
        error("createPersona is not implemented for this backend client")
    }

    suspend fun updatePersona(
        baseUrl: String,
        token: String,
        persona: UserPersonaDto,
    ): UserPersonaDto {
        error("updatePersona is not implemented for this backend client")
    }

    suspend fun deletePersona(baseUrl: String, token: String, personaId: String) {
        error("deletePersona is not implemented for this backend client")
    }

    suspend fun activatePersona(
        baseUrl: String,
        token: String,
        personaId: String,
    ): UserPersonaDto {
        error("activatePersona is not implemented for this backend client")
    }

    suspend fun getActivePersona(baseUrl: String, token: String): UserPersonaDto {
        error("getActivePersona is not implemented for this backend client")
    }

    suspend fun getCompanionMemory(
        baseUrl: String,
        token: String,
        cardId: String,
    ): CompanionMemoryDto {
        error("getCompanionMemory is not implemented for this backend client")
    }

    suspend fun resetCompanionMemory(
        baseUrl: String,
        token: String,
        cardId: String,
        scope: CompanionMemoryResetScope,
    ) {
        error("resetCompanionMemory is not implemented for this backend client")
    }

    suspend fun listCompanionMemoryPins(
        baseUrl: String,
        token: String,
        cardId: String,
    ): CompanionMemoryPinListDto {
        error("listCompanionMemoryPins is not implemented for this backend client")
    }

    suspend fun createCompanionMemoryPin(
        baseUrl: String,
        token: String,
        cardId: String,
        pin: CompanionMemoryPinDto,
    ): CompanionMemoryPinDto {
        error("createCompanionMemoryPin is not implemented for this backend client")
    }

    suspend fun updateCompanionMemoryPin(
        baseUrl: String,
        token: String,
        cardId: String,
        pin: CompanionMemoryPinDto,
    ): CompanionMemoryPinDto {
        error("updateCompanionMemoryPin is not implemented for this backend client")
    }

    suspend fun deleteCompanionMemoryPin(
        baseUrl: String,
        token: String,
        cardId: String,
        pinId: String,
    ) {
        error("deleteCompanionMemoryPin is not implemented for this backend client")
    }

    suspend fun listPresets(baseUrl: String, token: String): PresetListDto {
        error("listPresets is not implemented for this backend client")
    }

    suspend fun createPreset(
        baseUrl: String,
        token: String,
        preset: PresetDto,
    ): PresetDto {
        error("createPreset is not implemented for this backend client")
    }

    suspend fun updatePreset(
        baseUrl: String,
        token: String,
        preset: PresetDto,
    ): PresetDto {
        error("updatePreset is not implemented for this backend client")
    }

    suspend fun deletePreset(baseUrl: String, token: String, presetId: String) {
        error("deletePreset is not implemented for this backend client")
    }

    suspend fun activatePreset(
        baseUrl: String,
        token: String,
        presetId: String,
    ): PresetDto {
        error("activatePreset is not implemented for this backend client")
    }

    suspend fun getActivePreset(baseUrl: String, token: String): PresetDto {
        error("getActivePreset is not implemented for this backend client")
    }
}

