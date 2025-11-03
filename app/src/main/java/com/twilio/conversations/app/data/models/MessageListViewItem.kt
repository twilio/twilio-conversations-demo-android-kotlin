package com.twilio.conversations.app.data.models

import android.net.Uri
import com.twilio.conversations.app.common.enums.Direction
import com.twilio.conversations.app.common.enums.DownloadState
import com.twilio.conversations.app.common.enums.MessageType
import com.twilio.conversations.app.common.enums.Reactions
import com.twilio.conversations.app.common.enums.SendStatus

data class MessageAttachmentViewItem(
    val sid: String,
    val fileName: String?,
    val type: String?,
    val size: Long?,
    val uri: Uri?,
    val downloadId: Long?,
    val downloadedBytes: Long?,
    val downloadState: DownloadState,
    val uploading: Boolean,
    val uploadedBytes: Long?,
    val uploadUri: Uri?
)

data class MessageListViewItem(
    val sid: String,
    val uuid: String,
    val index: Long,
    val direction: Direction,
    val author: String,
    val authorChanged: Boolean,
    val body: String,
    val dateCreated: String,
    val sendStatus: SendStatus,
    val sendStatusIcon: Int,
    val reactions: Reactions,
    val type: MessageType,
    val attachmentsList: List<MessageAttachmentViewItem> = emptyList(),
    val mediaSize: Long?,
    val errorCode: Int
)
