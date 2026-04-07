package com.gkim.im.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gkim.im.android.data.local.entity.ContactEntity
import com.gkim.im.android.data.local.entity.ConversationEntity
import com.gkim.im.android.data.local.entity.FeedPostEntity
import com.gkim.im.android.data.local.entity.MessageEntity
import com.gkim.im.android.data.local.entity.PendingAigcTaskEntity
import com.gkim.im.android.data.local.entity.ProviderPresetEntity
import com.gkim.im.android.data.local.entity.WorkshopPromptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts")
    fun observeAll(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ContactEntity>)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastTimestamp DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ConversationEntity>)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeByConversation(conversationId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MessageEntity>)
}

@Dao
interface FeedDao {
    @Query("SELECT * FROM feed_posts ORDER BY createdAt DESC")
    fun observePosts(): Flow<List<FeedPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(items: List<FeedPostEntity>)
}

@Dao
interface WorkshopPromptDao {
    @Query("SELECT * FROM workshop_prompts")
    fun observeAll(): Flow<List<WorkshopPromptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WorkshopPromptEntity>)
}

@Dao
interface ProviderPresetDao {
    @Query("SELECT * FROM provider_presets")
    fun observeAll(): Flow<List<ProviderPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ProviderPresetEntity>)
}

@Dao
interface PendingTaskDao {
    @Query("SELECT * FROM pending_aigc_jobs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PendingAigcTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: PendingAigcTaskEntity)
}
