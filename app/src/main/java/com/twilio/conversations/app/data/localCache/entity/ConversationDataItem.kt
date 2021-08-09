package com.twilio.conversations.app.data.localCache.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_table")
data class ConversationDataItem(
    @PrimaryKey
    val sid: String,
    val friendlyName: String,
    val attributes: String,
    val uniqueName: String,
    val dateUpdated: Long,
    val dateCreated: Long,
    val lastMessageDate: Long,
    val lastMessageText: String,
    val lastMessageSendStatus: Int,
    val createdBy: String,
    val participantsCount: Long,
    val messagesCount: Long,
    val unreadMessagesCount: Long,
    val participatingStatus: Int,
    val notificationLevel: Int
)
