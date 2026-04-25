package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.data.remote.im.EditUserTurnRequestDto
import com.gkim.im.android.data.remote.im.EditUserTurnResponseDto

/**
 * §3.2 — presentation contract for the "Edit" overflow on user bubbles.
 *
 * The edit affordance opens a sheet prefilled with the bubble's text. Submitting calls
 * `POST /api/companion-turns/{conversationId}/edit` with [EditUserTurnRequestDto]; the
 * response carries the new user-message variant + a kicked-off companion turn, both of
 * which become the active variants for their parents in the conversation's branch tree.
 *
 * The §3.2 wire-up (HTTP call + ViewModel state mutation + ChatMessageRow overflow render)
 * consumes the helpers here:
 * - [editUserBubbleSheetState] — produces the sheet's initial state from a bubble; returns
 *   null when the bubble is non-user / has no parent / lacks an active companion id (those
 *   inputs make the edit endpoint contractually un-callable, so the sheet does not open).
 * - [EditUserBubbleSheetState.withDraft] — applies the user's textfield edits.
 * - [EditUserBubbleSheetState.canSubmit] — true when the draft is non-blank and differs from
 *   the original (no-op submits collapse to no network call).
 * - [EditUserBubbleSheetState.toRequestDto] — packages the sheet state into the wire DTO.
 * - [editUserBubbleActivePathEffect] — describes the active-path mutation triggered by a
 *   successful response: which user-message becomes active under its parent, and which
 *   companion-turn becomes active under the new user-message.
 */
internal data class EditUserBubbleSheetState(
    val sourceBubbleId: String,
    val parentMessageId: String,
    val conversationId: String,
    val activeCompanionId: String,
    val activeLanguage: String,
    val originalText: String,
    val draftText: String = originalText,
)

internal fun editUserBubbleSheetState(
    bubble: ChatMessage,
    conversationId: String,
    activeCompanionId: String?,
    activeLanguage: AppLanguage,
): EditUserBubbleSheetState? {
    if (bubble.direction != MessageDirection.Outgoing) return null
    val parentMessageId = bubble.parentMessageId ?: return null
    val companionId = activeCompanionId ?: return null
    return EditUserBubbleSheetState(
        sourceBubbleId = bubble.id,
        parentMessageId = parentMessageId,
        conversationId = conversationId,
        activeCompanionId = companionId,
        activeLanguage = appLanguageWireKey(activeLanguage),
        originalText = bubble.body,
    )
}

internal fun EditUserBubbleSheetState.withDraft(draft: String): EditUserBubbleSheetState =
    copy(draftText = draft)

internal fun EditUserBubbleSheetState.canSubmit(): Boolean =
    draftText.isNotBlank() && draftText != originalText

internal fun EditUserBubbleSheetState.toRequestDto(clientTurnId: String): EditUserTurnRequestDto? {
    if (!canSubmit()) return null
    return EditUserTurnRequestDto(
        parentMessageId = parentMessageId,
        newUserText = draftText,
        clientTurnId = clientTurnId,
        activeCompanionId = activeCompanionId,
        activeLanguage = activeLanguage,
    )
}

/**
 * The active-path mutation a successful edit-user-turn response triggers in the conversation's
 * branch tree. Two variant groups change their active sibling: the user-message group rooted at
 * `userMessageParentId`, and the companion-turn group rooted at `companionTurnParentId` (the
 * new user-message's id).
 */
internal data class EditUserBubbleActivePathEffect(
    val userMessageParentId: String,
    val newActiveUserMessageId: String,
    val companionTurnParentId: String,
    val newActiveCompanionMessageId: String,
)

internal fun editUserBubbleActivePathEffect(
    response: EditUserTurnResponseDto,
): EditUserBubbleActivePathEffect? {
    val userParent = response.userMessage.parentMessageId ?: return null
    val companionParent = response.companionTurn.parentMessageId ?: return null
    return EditUserBubbleActivePathEffect(
        userMessageParentId = userParent,
        newActiveUserMessageId = response.userMessage.messageId,
        companionTurnParentId = companionParent,
        newActiveCompanionMessageId = response.companionTurn.messageId,
    )
}
