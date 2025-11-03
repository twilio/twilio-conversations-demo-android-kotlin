package com.twilio.conversations.app.data.localCache.entity

import androidx.room.Entity
import com.twilio.conversations.app.common.enums.DownloadState

data class MessageAttachmentDataItem(
    val uuid: String = "",
    val sid: String = "",
    val fileName: String? = null,
    val type: String? = null,
    val size: Long? = null,
    val uri: String? = null,
    val downloadId: Long? = null,
    val downloadedBytes: Long? = null,
    val downloadState: DownloadState = DownloadState.NOT_STARTED,
    val uploading: Boolean = false,
    val uploadedBytes: Long? = null,
    val uploadUri: String? = null,
)

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
    val attachmentsList: List<MessageAttachmentDataItem> = emptyList(),
    val mediaSize: Long? = null,
    val errorCode: Int = 0
)
