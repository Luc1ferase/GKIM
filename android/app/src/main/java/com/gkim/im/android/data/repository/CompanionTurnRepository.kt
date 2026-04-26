package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.CompanionTurnMeta
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.MessageStatus
import com.gkim.im.android.data.remote.im.CharacterPromptContextDto
import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class VariantGroupState(
    val variantGroupId: String,
    val siblingMessageIds: List<String>,
    val activeIndex: Int,
)

data class ConversationTurnTree(
    val conversationId: String,
    val orderedMessageIds: List<String> = emptyList(),
    val messagesById: Map<String, ChatMessage> = emptyMap(),
    val variantGroups: Map<String, VariantGroupState> = emptyMap(),
    val turnToMessageId: Map<String, String> = emptyMap(),
    val messageIdToVariantGroupId: Map<String, String> = emptyMap(),
    val lastDeltaSeqByTurn: Map<String, Int> = emptyMap(),
)

data class FailedCompanionSubmission(
    val userMessageId: String,
    val conversationId: String,
    val activeCompanionId: String,
    val userTurnBody: String,
    val activeLanguage: String,
    val parentMessageId: String?,
    val characterPromptContext: CharacterPromptContextDto? = null,
)

/**
 * §2.1 — wire-payload shape returned by `exportConversation`. Mirrors `ExportedCardPayload`
 * (filename + bytes + contentType) so the Compose dispatcher can route to share-sheet or
 * Downloads identically. The filename is the canonical
 * `chat-export-<active-path|full-tree>_<first8OfConversationId>.jsonl` produced by
 * `chatExportFilename(...)` in `ChatExportRouting.kt`.
 */
data class ExportedChatPayload(
    val filename: String,
    val bytes: ByteArray,
    val contentType: String,
) {
    override fun equals(other: Any?): Boolean =
        other is ExportedChatPayload &&
            other.filename == filename &&
            other.contentType == contentType &&
            other.bytes.contentEquals(bytes)

    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}

interface CompanionTurnRepository {
    val treeByConversation: StateFlow<Map<String, ConversationTurnTree>>
    val activePathByConversation: StateFlow<Map<String, List<ChatMessage>>>
    val failedSubmissions: StateFlow<Map<String, FailedCompanionSubmission>>

    fun recordUserTurn(userMessage: ChatMessage, conversationId: String)
    fun updateUserMessageStatus(conversationId: String, messageId: String, status: MessageStatus)

    fun handleTurnStarted(event: ImGatewayEvent.CompanionTurnStarted)
    fun handleTurnDelta(event: ImGatewayEvent.CompanionTurnDelta)
    fun handleTurnCompleted(event: ImGatewayEvent.CompanionTurnCompleted)
    fun handleTurnFailed(event: ImGatewayEvent.CompanionTurnFailed)
    fun handleTurnBlocked(event: ImGatewayEvent.CompanionTurnBlocked)

    fun selectVariant(turnId: String, variantIndex: Int)
    fun selectVariantByGroup(conversationId: String, variantGroupId: String, newIndex: Int)

    fun applyRecord(record: CompanionTurnRecordDto)
    fun applySnapshot(record: CompanionTurnRecordDto) = applyRecord(record)

    suspend fun submitUserTurn(
        conversationId: String,
        activeCompanionId: String,
        userTurnBody: String,
        activeLanguage: String,
        parentMessageId: String? = null,
        characterPromptContext: CharacterPromptContextDto? = null,
    ): Result<CompanionTurnRecordDto> =
        throw NotImplementedError("submit path requires a live repository")

    suspend fun retrySubmitUserTurn(userMessageId: String): Result<CompanionTurnRecordDto> =
        throw NotImplementedError("submit path requires a live repository")

    suspend fun regenerateTurn(turnId: String): Result<CompanionTurnRecordDto> =
        throw NotImplementedError("submit path requires a live repository")

    suspend fun editUserTurn(
        conversationId: String,
        parentMessageId: String,
        newUserText: String,
        activeCompanionId: String,
        activeLanguage: String,
        characterPromptContext: CharacterPromptContextDto? = null,
    ): Result<com.gkim.im.android.data.remote.im.EditUserTurnResponseDto> =
        throw NotImplementedError("edit path requires a live repository")

    suspend fun regenerateCompanionTurnAtTarget(
        conversationId: String,
        targetMessageId: String,
        characterPromptContext: CharacterPromptContextDto? = null,
    ): Result<CompanionTurnRecordDto> =
        throw NotImplementedError("regenerate-at path requires a live repository")

    suspend fun exportConversation(
        conversationId: String,
        format: String,
        pathOnly: Boolean,
    ): Result<ExportedChatPayload> =
        throw NotImplementedError("export path requires a live repository")
}

class DefaultCompanionTurnRepository : CompanionTurnRepository {
    private val treeState = MutableStateFlow<Map<String, ConversationTurnTree>>(emptyMap())
    private val activePathState = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    private val failedSubmissionsState =
        MutableStateFlow<Map<String, FailedCompanionSubmission>>(emptyMap())

    override val treeByConversation: StateFlow<Map<String, ConversationTurnTree>> = treeState
    override val activePathByConversation: StateFlow<Map<String, List<ChatMessage>>> = activePathState
    override val failedSubmissions: StateFlow<Map<String, FailedCompanionSubmission>> =
        failedSubmissionsState

    override fun recordUserTurn(userMessage: ChatMessage, conversationId: String) {
        require(userMessage.direction == MessageDirection.Outgoing) {
            "recordUserTurn requires an outgoing user message"
        }
        mutateTree(conversationId) { tree ->
            if (tree.messagesById.containsKey(userMessage.id)) {
                tree
            } else {
                tree.copy(
                    orderedMessageIds = tree.orderedMessageIds + userMessage.id,
                    messagesById = tree.messagesById + (userMessage.id to userMessage),
                )
            }
        }
    }

    override fun updateUserMessageStatus(
        conversationId: String,
        messageId: String,
        status: MessageStatus,
    ) {
        mutateTree(conversationId) { tree ->
            val existing = tree.messagesById[messageId] ?: return@mutateTree tree
            if (existing.direction != MessageDirection.Outgoing) return@mutateTree tree
            if (existing.status == status) return@mutateTree tree
            tree.copy(
                messagesById = tree.messagesById + (messageId to existing.copy(status = status)),
            )
        }
    }

    override fun handleTurnStarted(event: ImGatewayEvent.CompanionTurnStarted) {
        mutateTree(event.conversationId) { tree ->
            if (tree.messagesById.containsKey(event.messageId)) {
                return@mutateTree tree
            }
            val meta = CompanionTurnMeta(
                turnId = event.turnId,
                variantGroupId = event.variantGroupId,
                variantIndex = event.variantIndex,
                providerId = event.providerId,
                model = event.model,
                isEditable = false,
                canRegenerate = false,
            )
            val thinkingMessage = ChatMessage(
                id = event.messageId,
                direction = MessageDirection.Incoming,
                kind = MessageKind.Text,
                body = "",
                createdAt = "",
                parentMessageId = event.parentMessageId,
                status = MessageStatus.Thinking,
                companionTurnMeta = meta,
            )
            val existingGroup = tree.variantGroups[event.variantGroupId]
            val siblingIds = (existingGroup?.siblingMessageIds ?: emptyList()) + event.messageId
            val activeIndex = siblingIds.indexOf(event.messageId).coerceAtLeast(0)
            val variantGroup = VariantGroupState(
                variantGroupId = event.variantGroupId,
                siblingMessageIds = siblingIds,
                activeIndex = activeIndex,
            )
            tree.copy(
                orderedMessageIds = if (existingGroup == null) {
                    tree.orderedMessageIds + event.messageId
                } else {
                    tree.orderedMessageIds
                },
                messagesById = tree.messagesById + (event.messageId to thinkingMessage),
                variantGroups = tree.variantGroups + (event.variantGroupId to variantGroup),
                turnToMessageId = tree.turnToMessageId + (event.turnId to event.messageId),
                messageIdToVariantGroupId = tree.messageIdToVariantGroupId + (event.messageId to event.variantGroupId),
                lastDeltaSeqByTurn = tree.lastDeltaSeqByTurn + (event.turnId to -1),
            )
        }
    }

    override fun handleTurnDelta(event: ImGatewayEvent.CompanionTurnDelta) {
        mutateTree(event.conversationId) { tree ->
            val messageId = tree.turnToMessageId[event.turnId] ?: event.messageId
            val existing = tree.messagesById[messageId] ?: return@mutateTree tree
            val lastSeq = tree.lastDeltaSeqByTurn[event.turnId] ?: -1
            if (event.deltaSeq <= lastSeq) {
                return@mutateTree tree
            }
            val appended = existing.copy(
                body = existing.body + event.textDelta,
                status = MessageStatus.Streaming,
            )
            tree.copy(
                messagesById = tree.messagesById + (messageId to appended),
                lastDeltaSeqByTurn = tree.lastDeltaSeqByTurn + (event.turnId to event.deltaSeq),
            )
        }
    }

    override fun handleTurnCompleted(event: ImGatewayEvent.CompanionTurnCompleted) {
        mutateTree(event.conversationId) { tree ->
            val messageId = tree.turnToMessageId[event.turnId] ?: event.messageId
            val existing = tree.messagesById[messageId] ?: return@mutateTree tree
            val updatedMeta = existing.companionTurnMeta?.copy(
                isEditable = true,
                canRegenerate = true,
            )
            val completed = existing.copy(
                body = event.finalBody,
                createdAt = event.completedAt,
                status = MessageStatus.Completed,
                companionTurnMeta = updatedMeta,
            )
            tree.copy(
                messagesById = tree.messagesById + (messageId to completed),
            )
        }
    }

    override fun handleTurnFailed(event: ImGatewayEvent.CompanionTurnFailed) {
        mutateTree(event.conversationId) { tree ->
            val messageId = tree.turnToMessageId[event.turnId] ?: event.messageId
            val existing = tree.messagesById[messageId] ?: return@mutateTree tree
            val status = if (event.subtype.equals("timeout", ignoreCase = true)) {
                MessageStatus.Timeout
            } else {
                MessageStatus.Failed
            }
            val updatedMeta = existing.companionTurnMeta?.copy(
                isEditable = false,
                canRegenerate = true,
                failedSubtypeKey = event.subtype,
            )
            val failed = existing.copy(
                status = status,
                companionTurnMeta = updatedMeta,
                body = event.errorMessage ?: existing.body,
            )
            tree.copy(
                messagesById = tree.messagesById + (messageId to failed),
            )
        }
    }

    override fun handleTurnBlocked(event: ImGatewayEvent.CompanionTurnBlocked) {
        mutateTree(event.conversationId) { tree ->
            val messageId = tree.turnToMessageId[event.turnId] ?: event.messageId
            val existing = tree.messagesById[messageId] ?: return@mutateTree tree
            val updatedMeta = existing.companionTurnMeta?.copy(
                isEditable = false,
                canRegenerate = false,
                blockReasonKey = event.reason,
            )
            val blocked = existing.copy(
                status = MessageStatus.Blocked,
                companionTurnMeta = updatedMeta,
                body = event.reason,
            )
            tree.copy(
                messagesById = tree.messagesById + (messageId to blocked),
            )
        }
    }

    override fun selectVariant(turnId: String, variantIndex: Int) {
        val currentTrees = treeState.value
        val matchingConversation = currentTrees.entries.firstOrNull { (_, tree) ->
            tree.turnToMessageId.containsKey(turnId)
        } ?: return
        mutateTree(matchingConversation.key) { tree ->
            val messageId = tree.turnToMessageId[turnId] ?: return@mutateTree tree
            val variantGroupId = tree.messageIdToVariantGroupId[messageId] ?: return@mutateTree tree
            val group = tree.variantGroups[variantGroupId] ?: return@mutateTree tree
            if (variantIndex !in group.siblingMessageIds.indices) {
                return@mutateTree tree
            }
            val updated = group.copy(activeIndex = variantIndex)
            tree.copy(
                variantGroups = tree.variantGroups + (variantGroupId to updated),
            )
        }
    }

    override fun selectVariantByGroup(
        conversationId: String,
        variantGroupId: String,
        newIndex: Int,
    ) {
        mutateTree(conversationId) { tree ->
            val group = tree.variantGroups[variantGroupId] ?: return@mutateTree tree
            if (group.siblingMessageIds.isEmpty()) return@mutateTree tree
            val clamped = newIndex.coerceIn(0, group.siblingMessageIds.size - 1)
            if (clamped == group.activeIndex) return@mutateTree tree
            tree.copy(
                variantGroups = tree.variantGroups + (variantGroupId to group.copy(activeIndex = clamped)),
            )
        }
    }

    override fun applyRecord(record: CompanionTurnRecordDto) {
        mutateTree(record.conversationId) { tree ->
            val existingSiblings = tree.variantGroups[record.variantGroupId]?.siblingMessageIds ?: emptyList()
            val siblingIds = if (existingSiblings.contains(record.messageId)) {
                existingSiblings
            } else {
                existingSiblings + record.messageId
            }
            val status = parseStatus(record.status)
            val meta = CompanionTurnMeta(
                turnId = record.turnId,
                variantGroupId = record.variantGroupId,
                variantIndex = record.variantIndex,
                providerId = record.providerId,
                model = record.model,
                isEditable = status == MessageStatus.Completed,
                canRegenerate = status == MessageStatus.Completed ||
                    status == MessageStatus.Failed ||
                    status == MessageStatus.Timeout,
                blockReasonKey = record.blockReason,
                failedSubtypeKey = record.failureSubtype,
            )
            val renderedBody = when (status) {
                MessageStatus.Blocked -> record.blockReason ?: record.accumulatedBody
                MessageStatus.Failed, MessageStatus.Timeout -> record.errorMessage ?: record.accumulatedBody
                else -> record.accumulatedBody
            }
            val message = ChatMessage(
                id = record.messageId,
                direction = MessageDirection.Incoming,
                kind = MessageKind.Text,
                body = renderedBody,
                createdAt = record.completedAt ?: record.startedAt,
                parentMessageId = record.parentMessageId,
                status = status,
                companionTurnMeta = meta,
            )
            val activeIndex = siblingIds.indexOf(record.messageId).coerceAtLeast(0)
            val variantGroup = VariantGroupState(
                variantGroupId = record.variantGroupId,
                siblingMessageIds = siblingIds,
                activeIndex = activeIndex,
            )
            val orderedMessageIds = if (tree.messagesById.containsKey(record.messageId)) {
                tree.orderedMessageIds
            } else {
                tree.orderedMessageIds + record.messageId
            }
            tree.copy(
                orderedMessageIds = orderedMessageIds,
                messagesById = tree.messagesById + (record.messageId to message),
                variantGroups = tree.variantGroups + (record.variantGroupId to variantGroup),
                turnToMessageId = tree.turnToMessageId + (record.turnId to record.messageId),
                messageIdToVariantGroupId = tree.messageIdToVariantGroupId + (record.messageId to record.variantGroupId),
                lastDeltaSeqByTurn = tree.lastDeltaSeqByTurn + (record.turnId to record.lastDeltaSeq),
            )
        }
    }

    private fun parseStatus(raw: String): MessageStatus = when (raw.lowercase()) {
        "pending" -> MessageStatus.Pending
        "thinking" -> MessageStatus.Thinking
        "streaming" -> MessageStatus.Streaming
        "completed" -> MessageStatus.Completed
        "failed" -> MessageStatus.Failed
        "blocked" -> MessageStatus.Blocked
        "timeout" -> MessageStatus.Timeout
        else -> MessageStatus.Pending
    }

    private fun mutateTree(
        conversationId: String,
        transform: (ConversationTurnTree) -> ConversationTurnTree,
    ) {
        val current = treeState.value
        val existing = current[conversationId] ?: ConversationTurnTree(conversationId = conversationId)
        val updated = transform(existing)
        if (updated === existing) return
        treeState.value = current + (conversationId to updated)
        activePathState.value = activePathState.value + (conversationId to resolveActivePath(updated))
    }

    private fun resolveActivePath(tree: ConversationTurnTree): List<ChatMessage> {
        val emittedGroups = mutableSetOf<String>()
        return tree.orderedMessageIds.mapNotNull { id ->
            val variantGroupId = tree.messageIdToVariantGroupId[id]
            if (variantGroupId == null) {
                tree.messagesById[id]
            } else {
                if (!emittedGroups.add(variantGroupId)) {
                    null
                } else {
                    val group = tree.variantGroups[variantGroupId]
                    val activeId = group?.siblingMessageIds?.getOrNull(group.activeIndex)
                    val message = activeId?.let { tree.messagesById[it] }
                    if (message != null && group != null) {
                        message.withSiblingProjection(group)
                    } else {
                        message
                    }
                }
            }
        }
    }
}

/**
 * §2.1 — projects the variant-group's sibling count + active index onto the rendered
 * `ChatMessage.companionTurnMeta` so the §3.1 chevron rendering path picks up multi-sibling
 * groups in production. For single-sibling groups the projection emits `siblingCount = 1`
 * which the chevron-suppression rule treats as "no chevrons rendered", matching the
 * pre-projection behavior. The §3.1 caption "n / total" reads the same fields.
 */
private fun ChatMessage.withSiblingProjection(group: VariantGroupState): ChatMessage {
    val meta = companionTurnMeta ?: return this
    val siblingCount = group.siblingMessageIds.size
    val siblingActiveIndex = group.activeIndex
    if (meta.siblingCount == siblingCount && meta.siblingActiveIndex == siblingActiveIndex) {
        return this
    }
    return copy(
        companionTurnMeta = meta.copy(
            siblingCount = siblingCount,
            siblingActiveIndex = siblingActiveIndex,
        ),
    )
}
