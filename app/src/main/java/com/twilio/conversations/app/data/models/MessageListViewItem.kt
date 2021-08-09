package com.twilio.conversations.app.data.models

import android.net.Uri
import com.twilio.conversations.app.common.enums.Direction
import com.twilio.conversations.app.common.enums.DownloadState
import com.twilio.conversations.app.common.enums.MessageType
import com.twilio.conversations.app.common.enums.Reaction
import com.twilio.conversations.app.common.enums.Reactions
import com.twilio.conversations.app.common.enums.SendStatus

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
    val mediaSid: String?,
    val mediaFileName: String?,
    val mediaType: String?,
    val mediaSize: Long?,
    val mediaUri: Uri?,
    val mediaDownloadId: Long?,
    val mediaDownloadedBytes: Long?,
    val mediaDownloadState: DownloadState,
    val mediaUploading: Boolean,
    val mediaUploadedBytes: Long?,
    val mediaUploadUri: Uri?,
    val errorCode: Int
)
