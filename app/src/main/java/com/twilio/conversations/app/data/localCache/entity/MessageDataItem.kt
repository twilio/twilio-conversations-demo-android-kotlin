package com.twilio.conversations.app.data.localCache.entity

import androidx.room.Entity
import androidx.room.Ignore
import java.io.InputStream

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
) {
    @Ignore
    @Transient
    var inputStream: InputStream? = null

    constructor(
        sid: String,
        conversationSid: String,
        participantSid: String?,
        type: Int,
        author: String,
        dateCreated: Long,
        body: String?,
        index: Long,
        attributes: String,
        direction: Int,
        sendStatus: Int,
        uuid: String,
        mediaSid: String? = null,
        mediaFileName: String? = null,
        mediaType: String? = null,
        mediaSize: Long? = null,
        mediaUri: String? = null,
        mediaDownloadId: Long? = null,
        mediaDownloadedBytes: Long? = null,
        mediaDownloadState: Int = 0,
        mediaUploading: Boolean = false,
        mediaUploadedBytes: Long? = null,
        mediaUploadUri: String? = null,
        errorCode: Int = 0,
        inputStream: InputStream? // The additional parameter
    ) : this( // Calls the primary constructor
        sid, conversationSid, participantSid, type, author, dateCreated, body, index,
        attributes, direction, sendStatus, uuid, mediaSid, mediaFileName, mediaType,
        mediaSize, mediaUri, mediaDownloadId, mediaDownloadedBytes, mediaDownloadState,
        mediaUploading, mediaUploadedBytes, mediaUploadUri, errorCode
    ) {
        this.inputStream = inputStream // Set the inputStream property
    }
}
