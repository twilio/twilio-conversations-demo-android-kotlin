package com.twilio.conversations.app.data.localCache.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.twilio.conversations.app.data.localCache.entity.ConversationDataItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationsDao {

    // Get User Conversations
    @Query("SELECT * FROM conversation_table WHERE participatingStatus = 1 ORDER BY lastMessageDate DESC")
    fun getUserConversations(): Flow<List<ConversationDataItem>>

    // Get Conversation by sid
    @Query("SELECT * FROM conversation_table WHERE sid = :sid")
    fun getConversation(sid: String): Flow<ConversationDataItem?>

    // Insert Conversation list
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(conversationDataItemList: List<ConversationDataItem>)

    // Insert single Conversation
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(conversationDataItem: ConversationDataItem)

    // Update Conversation
    @Query("UPDATE conversation_table SET participatingStatus = :status, notificationLevel = :level, friendlyName = :friendlyName WHERE sid = :sid")
    fun update(sid: String, status: Int, level: Int, friendlyName: String)

    @Query("UPDATE conversation_table SET participantsCount = :participantsCount WHERE sid = :sid")
    fun updateParticipantCount(sid: String, participantsCount: Long)

    @Query("UPDATE conversation_table SET messagesCount = :messagesCount WHERE sid = :sid")
    fun updateMessagesCount(sid: String, messagesCount: Long)

    @Query("UPDATE conversation_table SET unreadMessagesCount = :unreadMessagesCount WHERE sid = :sid")
    fun updateUnreadMessagesCount(sid: String, unreadMessagesCount: Long)

    @Query("UPDATE conversation_table SET lastMessageText = :lastMessageText, lastMessageSendStatus = :lastMessageSendStatus, lastMessageDate = :lastMessageDate WHERE sid = :sid")
    fun updateLastMessage(sid: String, lastMessageText: String, lastMessageSendStatus: Int, lastMessageDate: Long)

    // Delete Conversation
    @Query("DELETE FROM conversation_table WHERE sid = :sid")
    fun delete(sid: String)

    // Delete Gone User Conversations
    @Query("DELETE FROM conversation_table WHERE participatingStatus = 1 AND sid NOT IN (:sids)")
    fun deleteUserConversationsNotIn(sids: List<String>)

    fun deleteGoneUserConversations(newConversations: List<ConversationDataItem>) = deleteUserConversationsNotIn(newConversations.map { it.sid })
}
