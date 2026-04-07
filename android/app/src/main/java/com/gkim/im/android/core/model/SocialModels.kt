package com.gkim.im.android.core.model

enum class ContactSortMode {
    Nickname,
    AddedAscending,
    AddedDescending,
}

enum class PromptCategory {
    All,
    Portrait,
    Video,
    Cyberpunk,
    CodeArt,
}

data class Contact(
    val id: String,
    val nickname: String,
    val title: String,
    val avatarText: String,
    val addedAt: String,
    val isOnline: Boolean,
)

data class FeedPost(
    val id: String,
    val author: String,
    val role: String,
    val title: String,
    val summary: String,
    val body: String,
    val tags: List<String>,
    val createdAt: String,
    val accent: AccentTone,
)

data class WorkshopPrompt(
    val id: String,
    val title: String,
    val summary: String,
    val prompt: String,
    val category: PromptCategory,
    val author: String,
    val uses: Int,
    val mdxReady: Boolean,
)
