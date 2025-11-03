package com.twilio.conversations.app.data.localCache.dao

import com.twilio.conversations.app.common.enums.DownloadState
import com.twilio.conversations.app.data.localCache.converters.AttachmentsListConverter

class MediaUpdateHelper {
    private val mediaConverter = AttachmentsListConverter()

    fun updateAttachmentDownloadState(
        dao: MessagesDao,
        messageSid: String,
        attachmentSid: String,
        downloadState: DownloadState
    ) {
        val message = dao.getMessageForAttachmentsUpdate(messageSid) ?: return
        
        val updatedMediaList = message.attachmentsList.map { mediaItem ->
            if (mediaItem.sid == attachmentSid) {
                mediaItem.copy(downloadState = downloadState)
            } else {
                mediaItem
            }
        }
        
        val updatedMediaJson = mediaConverter.fromAttachmentsList(updatedMediaList)
        dao.updateMessageAttachments(messageSid, updatedMediaJson)
    }

    fun updateAttachmentDownloadProgress(
        dao: MessagesDao,
        messageSid: String,
        attachmentSid: String,
        downloadedBytes: Long,
    ) {
        val message = dao.getMessageForAttachmentsUpdate(messageSid) ?: return
        
        val updatedMediaList = message.attachmentsList.map { mediaItem ->
            if (mediaItem.sid == attachmentSid) {
                mediaItem.copy(
                    downloadedBytes = downloadedBytes,
                )
            } else {
                mediaItem
            }
        }
        
        val updatedMediaJson = mediaConverter.fromAttachmentsList(updatedMediaList)
        dao.updateMessageAttachments(messageSid, updatedMediaJson)
    }

    fun updateAttachmentDownloadLocation(
        dao: MessagesDao,
        messageSid: String,
        attachmentSid: String,
        uri: String,
    ) {
        val message = dao.getMessageForAttachmentsUpdate(messageSid) ?: return
        
        val updatedMediaList = message.attachmentsList.map { mediaItem ->
            if (mediaItem.sid == attachmentSid) {
                mediaItem.copy(uri = uri)
            } else {
                mediaItem
            }
        }
        
        val updatedMediaJson = mediaConverter.fromAttachmentsList(updatedMediaList)
        dao.updateMessageAttachments(messageSid, updatedMediaJson)
    }
    
    fun updateAttachmentDownloadId(
        dao: MessagesDao,
        messageSid: String,
        attachmentSid: String,
        downloadId: Long
    ) {
        val message = dao.getMessageForAttachmentsUpdate(messageSid) ?: return

        val updatedMediaList = message.attachmentsList.map { mediaItem ->
            if (mediaItem.sid == attachmentSid) {
                mediaItem.copy(downloadId = downloadId)
            } else {
                mediaItem
            }
        }

        val updatedMediaJson = mediaConverter.fromAttachmentsList(updatedMediaList)
        dao.updateMessageAttachments(messageSid, updatedMediaJson)
    }

    fun updateAttachmentUploadProgressByUuid(
        dao: MessagesDao,
        messageUuid: String,
        attachmentUuid: String,
        uploadedBytes: Long,
        uploading: Boolean
    ) {
        val message = dao.getMessageByUuid(messageUuid) ?: return
        
        val updatedMediaList = message.attachmentsList.map { mediaItem ->
            if (mediaItem.uuid == attachmentUuid) {
                mediaItem.copy(
                    uploadedBytes = uploadedBytes,
                    uploading = uploading
                )
            } else {
                mediaItem
            }
        }
        
        val updatedMediaJson = mediaConverter.fromAttachmentsList(updatedMediaList)
        dao.updateMessageAttachmentsByUuid(messageUuid, updatedMediaJson)
    }
}