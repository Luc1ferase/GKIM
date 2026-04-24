package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.MessageStatus
import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import com.gkim.im.android.data.remote.im.CompanionTurnSubmitRequestDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.remote.realtime.RealtimeGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class LiveCompanionTurnRepository(
    private val default: CompanionTurnRepository = DefaultCompanionTurnRepository(),
    private val backendClient: ImBackendClient,
    private val gateway: RealtimeGateway,
    private val scope: CoroutineScope,
    private val baseUrlProvider: () -> String?,
    private val tokenProvider: () -> String?,
    private val clientTurnIdGenerator: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> String = { java.time.Instant.now().toString() },
) : CompanionTurnRepository {

    override val treeByConversation: StateFlow<Map<String, ConversationTurnTree>> =
        default.treeByConversation
    override val activePathByConversation: StateFlow<Map<String, List<ChatMessage>>> =
        default.activePathByConversation

    private val failedSubmissionsState =
        MutableStateFlow<Map<String, FailedCompanionSubmission>>(emptyMap())
    override val failedSubmissions: StateFlow<Map<String, FailedCompanionSubmission>> =
        failedSubmissionsState.asStateFlow()

    init {
        scope.launch {
            gateway.events.collect(::handleGatewayEvent)
        }
        scope.launch {
            gateway.isConnected.collect { connected ->
                if (connected) {
                    rehydratePending()
                }
            }
        }
    }

    override suspend fun submitUserTurn(
        conversationId: String,
        activeCompanionId: String,
        userTurnBody: String,
        activeLanguage: String,
        parentMessageId: String?,
    ): Result<CompanionTurnRecordDto> {
        val clientTurnId = clientTurnIdGenerator()
        val userMessageId = "user-$clientTurnId"
        val userMessage = ChatMessage(
            id = userMessageId,
            direction = MessageDirection.Outgoing,
            kind = MessageKind.Text,
            body = userTurnBody,
            createdAt = clock(),
            status = MessageStatus.Pending,
        )
        default.recordUserTurn(userMessage, conversationId)

        val baseUrl = baseUrlProvider()
        val token = tokenProvider()
        if (baseUrl == null || token == null) {
            return handleSubmissionFailure(
                userMessageId = userMessageId,
                submission = FailedCompanionSubmission(
                    userMessageId = userMessageId,
                    conversationId = conversationId,
                    activeCompanionId = activeCompanionId,
                    userTurnBody = userTurnBody,
                    activeLanguage = activeLanguage,
                    parentMessageId = parentMessageId,
                ),
                error = IllegalStateException(
                    if (baseUrl == null) "no base url" else "no token",
                ),
            )
        }

        return try {
            val record = backendClient.submitCompanionTurn(
                baseUrl = baseUrl,
                token = token,
                request = CompanionTurnSubmitRequestDto(
                    conversationId = conversationId,
                    activeCompanionId = activeCompanionId,
                    userTurnBody = userTurnBody,
                    activeLanguage = activeLanguage,
                    clientTurnId = clientTurnId,
                    parentMessageId = parentMessageId,
                ),
            )
            default.updateUserMessageStatus(
                conversationId = conversationId,
                messageId = userMessageId,
                status = MessageStatus.Completed,
            )
            clearFailedSubmission(userMessageId)
            default.applyRecord(record)
            Result.success(record)
        } catch (t: Throwable) {
            handleSubmissionFailure(
                userMessageId = userMessageId,
                submission = FailedCompanionSubmission(
                    userMessageId = userMessageId,
                    conversationId = conversationId,
                    activeCompanionId = activeCompanionId,
                    userTurnBody = userTurnBody,
                    activeLanguage = activeLanguage,
                    parentMessageId = parentMessageId,
                ),
                error = t,
            )
        }
    }

    override suspend fun retrySubmitUserTurn(userMessageId: String): Result<CompanionTurnRecordDto> {
        val submission = failedSubmissionsState.value[userMessageId]
            ?: return Result.failure(IllegalStateException("no failed submission for $userMessageId"))

        default.updateUserMessageStatus(
            conversationId = submission.conversationId,
            messageId = userMessageId,
            status = MessageStatus.Pending,
        )

        val baseUrl = baseUrlProvider()
        val token = tokenProvider()
        if (baseUrl == null || token == null) {
            return handleSubmissionFailure(
                userMessageId = userMessageId,
                submission = submission,
                error = IllegalStateException(
                    if (baseUrl == null) "no base url" else "no token",
                ),
            )
        }

        val clientTurnId = clientTurnIdGenerator()
        return try {
            val record = backendClient.submitCompanionTurn(
                baseUrl = baseUrl,
                token = token,
                request = CompanionTurnSubmitRequestDto(
                    conversationId = submission.conversationId,
                    activeCompanionId = submission.activeCompanionId,
                    userTurnBody = submission.userTurnBody,
                    activeLanguage = submission.activeLanguage,
                    clientTurnId = clientTurnId,
                    parentMessageId = submission.parentMessageId,
                ),
            )
            default.updateUserMessageStatus(
                conversationId = submission.conversationId,
                messageId = userMessageId,
                status = MessageStatus.Completed,
            )
            clearFailedSubmission(userMessageId)
            default.applyRecord(record)
            Result.success(record)
        } catch (t: Throwable) {
            handleSubmissionFailure(
                userMessageId = userMessageId,
                submission = submission,
                error = t,
            )
        }
    }

    private fun handleSubmissionFailure(
        userMessageId: String,
        submission: FailedCompanionSubmission,
        error: Throwable,
    ): Result<CompanionTurnRecordDto> {
        default.updateUserMessageStatus(
            conversationId = submission.conversationId,
            messageId = userMessageId,
            status = MessageStatus.Failed,
        )
        failedSubmissionsState.value = failedSubmissionsState.value + (userMessageId to submission)
        return Result.failure(error)
    }

    private fun clearFailedSubmission(userMessageId: String) {
        val current = failedSubmissionsState.value
        if (current.containsKey(userMessageId)) {
            failedSubmissionsState.value = current - userMessageId
        }
    }

    override suspend fun regenerateTurn(turnId: String): Result<CompanionTurnRecordDto> {
        val baseUrl = baseUrlProvider() ?: return Result.failure(IllegalStateException("no base url"))
        val token = tokenProvider() ?: return Result.failure(IllegalStateException("no token"))
        return try {
            val clientTurnId = clientTurnIdGenerator()
            val record = backendClient.regenerateCompanionTurn(
                baseUrl = baseUrl,
                token = token,
                turnId = turnId,
                clientTurnId = clientTurnId,
            )
            default.applyRecord(record)
            Result.success(record)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override fun recordUserTurn(userMessage: ChatMessage, conversationId: String) =
        default.recordUserTurn(userMessage, conversationId)

    override fun updateUserMessageStatus(
        conversationId: String,
        messageId: String,
        status: MessageStatus,
    ) = default.updateUserMessageStatus(conversationId, messageId, status)

    override fun handleTurnStarted(event: ImGatewayEvent.CompanionTurnStarted) =
        default.handleTurnStarted(event)

    override fun handleTurnDelta(event: ImGatewayEvent.CompanionTurnDelta) =
        default.handleTurnDelta(event)

    override fun handleTurnCompleted(event: ImGatewayEvent.CompanionTurnCompleted) =
        default.handleTurnCompleted(event)

    override fun handleTurnFailed(event: ImGatewayEvent.CompanionTurnFailed) =
        default.handleTurnFailed(event)

    override fun handleTurnBlocked(event: ImGatewayEvent.CompanionTurnBlocked) =
        default.handleTurnBlocked(event)

    override fun selectVariant(turnId: String, variantIndex: Int) =
        default.selectVariant(turnId, variantIndex)

    override fun applyRecord(record: CompanionTurnRecordDto) = default.applyRecord(record)

    private suspend fun rehydratePending() {
        val baseUrl = baseUrlProvider() ?: return
        val token = tokenProvider() ?: return
        try {
            val pending = backendClient.listPendingCompanionTurns(baseUrl, token)
            pending.turns.forEach { default.applyRecord(it) }
        } catch (_: Throwable) {
            // Silent: pending rehydration is best-effort.
        }
    }

    private fun handleGatewayEvent(event: ImGatewayEvent) {
        when (event) {
            is ImGatewayEvent.CompanionTurnStarted -> default.handleTurnStarted(event)
            is ImGatewayEvent.CompanionTurnDelta -> {
                val tree = default.treeByConversation.value[event.conversationId]
                val lastSeq = tree?.lastDeltaSeqByTurn?.get(event.turnId) ?: -1
                if (event.deltaSeq > lastSeq + 1) {
                    scope.launch { snapshotAndApply(event.turnId) }
                } else {
                    default.handleTurnDelta(event)
                }
            }
            is ImGatewayEvent.CompanionTurnCompleted -> default.handleTurnCompleted(event)
            is ImGatewayEvent.CompanionTurnFailed -> default.handleTurnFailed(event)
            is ImGatewayEvent.CompanionTurnBlocked -> default.handleTurnBlocked(event)
            else -> Unit
        }
    }

    private suspend fun snapshotAndApply(turnId: String) {
        val baseUrl = baseUrlProvider() ?: return
        val token = tokenProvider() ?: return
        try {
            val record = backendClient.snapshotCompanionTurn(baseUrl, token, turnId)
            default.applyRecord(record)
        } catch (_: Throwable) {
            // Silent: snapshot fallback is best-effort.
        }
    }
}
