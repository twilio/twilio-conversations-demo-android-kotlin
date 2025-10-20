package com.twilio.conversations.app.data.localCache.entity
import androidx.room.Entity

@Entity(tableName = "message_table", primaryKeys = ["sid", "uuid"])
data class MessageDataItem(
    val sid: String,
    val conversationSid: String,
    val participantSid: String?,
    val type: Int,
    val author: String,
    val dateCreated: Long,
    val body: String?,
    val index: Long,
    val attributes: String,
    val direction: Int,
    val sendStatus: Int,
    val uuid: String,
    val errorCode: Int = 0
)


