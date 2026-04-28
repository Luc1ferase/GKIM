package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.CompanionMemoryPin
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.data.repository.CompanionMemoryRepository

data class BubblePinDraft(
    val text: LocalizedText,
    val sourceMessageId: String,
)

object BubblePinActionDefaults {
    const val SecondaryStubEnglish: String = "(English translation pending)"
    const val SecondaryStubChinese: String = "(中文待补)"
}

fun buildBubblePinDraft(
    message: ChatMessage,
    language: AppLanguage,
): BubblePinDraft {
    val primary = message.body.trim()
    val text = when (language) {
        AppLanguage.English -> LocalizedText(
            english = primary,
            chinese = BubblePinActionDefaults.SecondaryStubChinese,
        )
        AppLanguage.Chinese -> LocalizedText(
            english = BubblePinActionDefaults.SecondaryStubEnglish,
            chinese = primary,
        )
    }
    return BubblePinDraft(text = text, sourceMessageId = message.id)
}

suspend fun submitBubblePin(
    draft: BubblePinDraft,
    cardId: String,
    repository: CompanionMemoryRepository,
): Result<CompanionMemoryPin> = repository.createPin(
    cardId = cardId,
    sourceMessageId = draft.sourceMessageId,
    text = draft.text,
)
