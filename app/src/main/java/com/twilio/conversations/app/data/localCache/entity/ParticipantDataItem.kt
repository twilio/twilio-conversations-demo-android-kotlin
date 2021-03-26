package com.twilio.conversations.app.data.localCache.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "participant_table")
data class ParticipantDataItem(
    @PrimaryKey
    val sid: String,
    val identity: String,
    val conversationSid: String,
    val friendlyName: String,
    val isOnline: Boolean,
    val lastReadMessageIndex: Long?,
    val lastReadTimestamp: String?,
    val typing: Boolean = false
)
