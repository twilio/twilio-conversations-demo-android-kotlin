package com.twilio.conversations.app.data.localCache.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MessageWithMedia(
    @Embedded val message: MessageDataItem,
    @Relation(
        parentColumn = "uuid",
        entityColumn = "messageUuid"
    )
    val media: List<MediaDataItem>?
)