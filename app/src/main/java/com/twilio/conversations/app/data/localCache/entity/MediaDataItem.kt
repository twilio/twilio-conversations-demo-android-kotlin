package com.twilio.conversations.app.data.localCache.entity

import androidx.room.Ignore
import java.io.InputStream
import androidx.room.Entity;

@Entity(tableName = "media_table", primaryKeys = ["mediaSid"])
data class MediaDataItem(
    val messageSid: String? = null,
    val mediaSid: String,
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
    val messageUuid: String
) {
    @Ignore
    @Transient
    var inputStream: InputStream? = null

    constructor(
        messageSid: String,
        mediaSid: String,
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
        inputStream: InputStream,
        messageUuid: String
    ) :
            this(
                messageSid,
                mediaSid,
                mediaFileName,
                mediaType,
                mediaSize,
                mediaUri,
                mediaDownloadId,
                mediaDownloadedBytes,
                mediaDownloadState,
                mediaUploading,
                mediaUploadedBytes,
                mediaUploadUri,
                messageUuid
            ) {
        this.inputStream = inputStream
    }
}
