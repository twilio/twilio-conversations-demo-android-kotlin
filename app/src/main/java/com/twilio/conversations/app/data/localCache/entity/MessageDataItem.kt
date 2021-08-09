package com.twilio.conversations.app.data.localCache.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_table", primaryKeys = ["sid", "uuid"])
data class MessageDataItem(
    val sid: String,
    val conversationSid: String,
    val participantSid: String,
    val type: Int,
    val author: String,
    val dateCreated: Long,
    val body: String?,
    val index: Long,
    val attributes: String,
    val direction: Int,
    val sendStatus: Int,
    val uuid: String,
    val mediaSid: String? = null,
    val mediaFileName: String? = null,
    val mediaType: String? = null,
    val mediaSize: Long? = null,
    val mediaUri: String? = null,
    val mediaDownloadId: Long? = null,
    val mediaDownloadedBytes: Long? = null,
    val mediaDownloadState: Int = 0,
    val mediaUploading: Boolean = false,
    val mediaUploadedBytes: Long? = null,
    val mediaUploadUri: String? = null,
    val errorCode: Int = 0
)
