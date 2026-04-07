package com.gkim.im.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gkim.im.android.data.local.dao.ContactDao
import com.gkim.im.android.data.local.dao.ConversationDao
import com.gkim.im.android.data.local.dao.FeedDao
import com.gkim.im.android.data.local.dao.MessageDao
import com.gkim.im.android.data.local.dao.PendingTaskDao
import com.gkim.im.android.data.local.dao.ProviderPresetDao
import com.gkim.im.android.data.local.dao.WorkshopPromptDao
import com.gkim.im.android.data.local.entity.ContactEntity
import com.gkim.im.android.data.local.entity.ConversationEntity
import com.gkim.im.android.data.local.entity.FeedPostEntity
import com.gkim.im.android.data.local.entity.MessageEntity
import com.gkim.im.android.data.local.entity.PendingAigcTaskEntity
import com.gkim.im.android.data.local.entity.ProviderPresetEntity
import com.gkim.im.android.data.local.entity.WorkshopPromptEntity

@Database(
    entities = [
        ContactEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        FeedPostEntity::class,
        WorkshopPromptEntity::class,
        ProviderPresetEntity::class,
        PendingAigcTaskEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun feedDao(): FeedDao
    abstract fun workshopPromptDao(): WorkshopPromptDao
    abstract fun providerPresetDao(): ProviderPresetDao
    abstract fun pendingTaskDao(): PendingTaskDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "gkim.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
