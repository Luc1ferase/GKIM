package com.gkim.im.android.core.model

data class CompanionMemory(
    val userId: String,
    val companionCardId: String,
    val summary: LocalizedText = LocalizedText.Empty,
    val summaryUpdatedAt: Long = 0L,
    val summaryTurnCursor: Int = 0,
    val tokenBudgetHint: Int? = null,
)

data class CompanionMemoryPin(
    val id: String,
    val sourceMessageId: String? = null,
    val text: LocalizedText,
    val createdAt: Long = 0L,
    val pinnedByUser: Boolean = true,
)

enum class CompanionMemoryResetScope {
    Pins,
    Summary,
    All,
}
