package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.MessageStatus
import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import com.gkim.im.android.data.remote.im.CompanionTurnSubmitRequestDto
import com.gkim.im.android.data.remote.im.EditUserTurnRequestDto
import com.gkim.im.android.data.remote.im.EditUserTurnResponseDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.remote.im.RegenerateAtRequestDto
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

    /**
     * §3.1 — calls `POST /api/companion-turns/:conversationId/edit`, projects the response
     * into the local sibling tree, and emits the resulting `ChatMessage` updates through
     * `companionMessages`. The new user-message lands as an Outgoing entry under its
     * response-supplied `parentMessageId` (typically the original user message being
     * edited); the new companion-turn lands as an Incoming entry via the existing
     * `applyRecord` projection that already tracks variantGroups + activePath.
     */
    override suspend fun editUserTurn(
        conversationId: String,
        parentMessageId: String,
        newUserText: String,
        activeCompanionId: String,
        activeLanguage: String,
    ): Result<EditUserTurnResponseDto> {
        val baseUrl = baseUrlProvider() ?: return Result.failure(IllegalStateException("no base url"))
        val token = tokenProvider() ?: return Result.failure(IllegalStateException("no token"))
        val clientTurnId = clientTurnIdGenerator()
        return try {
            val response = backendClient.editUserTurn(
                baseUrl = baseUrl,
                token = token,
                conversationId = conversationId,
                request = EditUserTurnRequestDto(
                    parentMessageId = parentMessageId,
                    newUserText = newUserText,
                    clientTurnId = clientTurnId,
                    activeCompanionId = activeCompanionId,
                    activeLanguage = activeLanguage,
                ),
            )
            val newUserMessage = ChatMessage(
                id = response.userMessage.messageId,
                direction = MessageDirection.Outgoing,
                kind = MessageKind.Text,
                body = newUserText,
                createdAt = clock(),
                parentMessageId = response.userMessage.parentMessageId,
                status = MessageStatus.Completed,
            )
            default.recordUserTurn(newUserMessage, conversationId)
            default.applyRecord(response.companionTurn)
            Result.success(response)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * §3.2 — calls `POST /api/companion-turns/:conversationId/regenerate-at` with an explicit
     * `targetMessageId` so the backend appends a sibling under that message's variantGroup.
     * The response is projected through `applyRecord` which already advances the active
     * variant to the new sibling and re-runs the §2.1 projection so chevrons reflect the new
     * `siblingCount` / `siblingActiveIndex`.
     */
    override suspend fun regenerateCompanionTurnAtTarget(
        conversationId: String,
        targetMessageId: String,
    ): Result<CompanionTurnRecordDto> {
        val baseUrl = baseUrlProvider() ?: return Result.failure(IllegalStateException("no base url"))
        val token = tokenProvider() ?: return Result.failure(IllegalStateException("no token"))
        val clientTurnId = clientTurnIdGenerator()
        return try {
            val record = backendClient.regenerateCompanionTurnAtTarget(
                baseUrl = baseUrl,
                token = token,
                conversationId = conversationId,
                request = RegenerateAtRequestDto(
                    clientTurnId = clientTurnId,
                    targetMessageId = targetMessageId,
                ),
            )
            default.applyRecord(record)
            Result.success(record)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * §2.1 — calls `GET /api/conversations/:conversationId/export?format=...&pathOnly=...`,
     * wraps the JSONL response body + a deterministic filename + `application/x-ndjson`
     * content-type into [ExportedChatPayload] for the dispatcher. Wire failures are remapped
     * to stable error codes so the dialog can render localized copy without inspecting
     * exception types: `404_unknown_conversation` (HTTP 404), `unsupported_format` (HTTP 400
     * with the backend's `unsupported_format` body), `network_failure` (everything else).
     *
     * The filename mirrors the §5.2 [chatExportFilename] formula so the dispatcher's
     * Downloads-target name and the share-sheet's display name agree:
     * `chat-export-<active-path|full-tree>_<first8OfConversationId>.jsonl`.
     */
    override suspend fun exportConversation(
        conversationId: String,
        format: String,
        pathOnly: Boolean,
    ): Result<ExportedChatPayload> {
        val baseUrl = baseUrlProvider() ?: return Result.failure(IllegalStateException("no base url"))
        val token = tokenProvider() ?: return Result.failure(IllegalStateException("no token"))
        return try {
            val body = backendClient.exportConversation(
                baseUrl = baseUrl,
                token = token,
                conversationId = conversationId,
                format = format,
                pathOnly = pathOnly,
            )
            val pathLabel = if (pathOnly) "active-path" else "full-tree"
            val filename = "chat-export-${pathLabel}_${conversationId.take(8)}.jsonl"
            Result.success(
                ExportedChatPayload(
                    filename = filename,
                    bytes = body.toByteArray(Charsets.UTF_8),
                    contentType = "application/x-ndjson",
                ),
            )
        } catch (t: Throwable) {
            Result.failure(remapExportError(t))
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

    override fun selectVariantByGroup(
        conversationId: String,
        variantGroupId: String,
        newIndex: Int,
    ) = default.selectVariantByGroup(conversationId, variantGroupId, newIndex)

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

    private fun remapExportError(t: Throwable): Throwable {
        val httpException = t as? retrofit2.HttpException
        return when (httpException?.code()) {
            404 -> RuntimeException("404_unknown_conversation")
            400 -> {
                val errorBody = runCatching { httpException.response()?.errorBody()?.string() }.getOrNull().orEmpty()
                if (errorBody.contains("unsupported_format")) {
                    RuntimeException("unsupported_format")
                } else {
                    RuntimeException("network_failure")
                }
            }
            null -> RuntimeException("network_failure", t)
            else -> RuntimeException("network_failure", t)
        }
    }
}
