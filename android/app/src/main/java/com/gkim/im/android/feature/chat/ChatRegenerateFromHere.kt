package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import com.gkim.im.android.data.remote.im.RegenerateAtRequestDto

/**
 * §3.3 — presentation contract for the "Regenerate from here" overflow on companion bubbles.
 *
 * The affordance must work on **every** companion bubble (not only the latest), so the helper
 * gates on `direction == Incoming` + presence of [CompanionTurnMeta] (system messages without a
 * turn meta are not regeneratable). Invoking the action calls
 * `POST /api/companion-turns/{conversationId}/regenerate-at` with [RegenerateAtRequestDto];
 * the response is a fresh [CompanionTurnRecordDto] sibling under the same `variantGroupId`
 * with `variantIndex = max + 1`, which becomes the active variant.
 *
 * The §3.3 wire-up consumes:
 * - [regenerateFromHereRequest] — produces the wire DTO from a companion bubble + a fresh
 *   client-turn id; null for non-companion / non-companion-turn-meta bubbles.
 * - [regenerateFromHereActivePathEffect] — projects a successful response into an
 *   active-path mutation: which variantGroupId now points at which new sibling messageId at
 *   what variantIndex.
 *
 * Note the route slug: the backend kept the legacy `:turn_id/regenerate` endpoint for
 * backward compatibility, so the conversation-scoped extension lives at the distinct
 * `:conversation_id/regenerate-at` path. The two routes share the same service-layer
 * sibling-creation logic on the backend.
 */
internal fun regenerateFromHereRequest(
    bubble: ChatMessage,
    clientTurnId: String,
): RegenerateAtRequestDto? {
    if (bubble.direction == MessageDirection.Outgoing) return null
    if (bubble.companionTurnMeta == null) return null
    return RegenerateAtRequestDto(
        clientTurnId = clientTurnId,
        targetMessageId = bubble.id,
    )
}

internal data class RegenerateFromHereActivePathEffect(
    val variantGroupId: String,
    val newActiveMessageId: String,
    val newActiveVariantIndex: Int,
)

internal fun regenerateFromHereActivePathEffect(
    response: CompanionTurnRecordDto,
): RegenerateFromHereActivePathEffect = RegenerateFromHereActivePathEffect(
    variantGroupId = response.variantGroupId,
    newActiveMessageId = response.messageId,
    newActiveVariantIndex = response.variantIndex,
)
