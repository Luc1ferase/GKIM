package com.gkim.im.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val nickname: String,
    val title: String,
    val avatarText: String,
    val addedAt: String,
    val isOnline: Boolean,
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val contactName: String,
    val contactTitle: String,
    val avatarText: String,
    val lastMessage: String,
    val lastTimestamp: String,
    val unreadCount: Int,
    val isOnline: Boolean,
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val direction: String,
    val kind: String,
    val body: String,
    val createdAt: String,
    val chips: String,
    val attachmentType: String?,
    val attachmentPreview: String?,
    val attachmentPrompt: String?,
    val generationId: String?,
)

@Entity(tableName = "feed_posts")
data class FeedPostEntity(
    @PrimaryKey val id: String,
    val author: String,
    val role: String,
    val title: String,
    val summary: String,
    val body: String,
    val tags: String,
    val createdAt: String,
    val accent: String,
)

@Entity(tableName = "workshop_prompts")
data class WorkshopPromptEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val prompt: String,
    val category: String,
    val author: String,
    val uses: Int,
    val mdxReady: Boolean,
)

@Entity(tableName = "provider_presets")
data class ProviderPresetEntity(
    @PrimaryKey val id: String,
    val label: String,
    val vendor: String,
    val description: String,
    val model: String,
    val accent: String,
    val preset: Boolean,
    val capabilities: String,
)

@Entity(tableName = "pending_aigc_jobs")
data class PendingAigcTaskEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val mode: String,
    val prompt: String,
    val createdAt: String,
    val status: String,
    val inputUri: String?,
    val inputType: String?,
    val outputPreview: String,
)
