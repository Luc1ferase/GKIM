package com.gkim.im.android.data.remote.im

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
}

