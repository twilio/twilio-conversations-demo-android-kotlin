package com.twilio.conversations.app.data.localCache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.twilio.conversations.app.data.localCache.dao.ConversationsDao
import com.twilio.conversations.app.data.localCache.dao.MessagesDao
import com.twilio.conversations.app.data.localCache.dao.ParticipantsDao
import com.twilio.conversations.app.data.localCache.entity.ConversationDataItem
import com.twilio.conversations.app.data.localCache.entity.MessageDataItem
import com.twilio.conversations.app.data.localCache.entity.ParticipantDataItem

@Database(entities = [ConversationDataItem::class, MessageDataItem::class, ParticipantDataItem::class], version = 1, exportSchema = false)
abstract class LocalCacheProvider : RoomDatabase() {

    abstract fun conversationsDao(): ConversationsDao

    abstract fun messagesDao(): MessagesDao

    abstract fun participantsDao(): ParticipantsDao

    companion object {
        val INSTANCE get() = _instance ?: error("call LocalCacheProvider.createInstance() first")

        private var _instance: LocalCacheProvider? = null

        fun createInstance(context: Context) {
            check(_instance == null) { "LocalCacheProvider singleton instance has been already created" }
            _instance = Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                LocalCacheProvider::class.java
            ).build()
        }
    }
}
