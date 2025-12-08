package com.twilio.conversations.app.data.localCache.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.twilio.conversations.app.common.enums.DownloadState
import com.twilio.conversations.app.data.localCache.entity.MessageDataItem

@Dao
interface MessagesDao {

    // Get all Messages sorted
    @Query("SELECT * FROM message_table WHERE conversationSid = :conversationSid ORDER BY CASE WHEN `index` < 0 THEN dateCreated ELSE `index` END ASC")
    fun getMessagesSorted(conversationSid: String): DataSource.Factory<Int, MessageDataItem>

    // Get last message
    @Query("SELECT * FROM message_table WHERE conversationSid = :conversationSid ORDER BY CASE WHEN `index` < 0 THEN dateCreated ELSE `index` END DESC LIMIT 1")
    fun getLastMessage(conversationSid: String): MessageDataItem?

    // Get single Message by SID
    @Query("SELECT * FROM message_table WHERE sid = :sid")
    fun getMessageBySid(sid: String): MessageDataItem?

    // Get single Message by UUID
    @Query("SELECT * FROM message_table WHERE uuid = :uuid")
    fun getMessageByUuid(uuid: String): MessageDataItem?

    // Insert Message list
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(messages: List<MessageDataItem>)

    // Insert a message when it doesn't exist or replace it if it does exist.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(message: MessageDataItem)

    // Update single Message Status
    @Query("UPDATE message_table SET sendStatus = :sendStatus, errorCode = :errorCode WHERE uuid = :uuid")
    fun updateMessageStatus(uuid: String, sendStatus: Int, errorCode: Int)

    // Update single Message
    @Query("UPDATE message_table SET sid = :sid, sendStatus = :sendStatus, `index` = :index, mediaSize = :mediaSize WHERE uuid = :uuid")
    fun updateByUuid(sid: String, uuid: String, sendStatus: Int, index: Long, mediaSize: Long?)

    @Transaction
    fun updateByUuidOrInsert(message: MessageDataItem) {
        if (message.uuid.isNotEmpty() && getMessageByUuid(message.uuid) != null) {
            updateByUuid(message.sid, message.uuid, message.sendStatus, message.index, message.mediaSize)
        } else {
            insertOrReplace(message)
        }
    }

    @Delete
    fun delete(message: MessageDataItem)

    @Query("SELECT * FROM message_table WHERE sid = :messageSid")
    fun getMessageForAttachmentsUpdate(messageSid: String): MessageDataItem?
    
    @Query("UPDATE message_table SET attachmentsList = :updatedMediaJson WHERE sid = :messageSid")
    fun updateMessageAttachments(messageSid: String, updatedMediaJson: String)

    @Query("UPDATE message_table SET attachmentsList = :updatedMediaJson WHERE uuid = :messageUuid")
    fun updateMessageAttachmentsByUuid(messageUuid: String, updatedMediaJson: String)

    @Transaction
    fun updateAttachmentDownloadState(messageSid: String, attachmentSid: String, downloadState: DownloadState) {
        val helper = MediaUpdateHelper()
        helper.updateAttachmentDownloadState(this, messageSid, attachmentSid, downloadState)
    }

    @Transaction
    fun updateAttachmentDownloadProgress(messageSid: String, attachmentSid: String, downloadedBytes: Long) {
        val helper = MediaUpdateHelper()
        helper.updateAttachmentDownloadProgress(this, messageSid, attachmentSid, downloadedBytes)
    }

    @Transaction
    fun updateAttachmentUri(messageSid: String, attachmentSid: String, uri: String) {
        val helper = MediaUpdateHelper()
        helper.updateAttachmentDownloadLocation(this, messageSid, attachmentSid, uri)
    }
    
    @Transaction
    fun updateAttachmentDownloadId(messageSid: String, attachmentSid: String, downloadId: Long) {
        val helper = MediaUpdateHelper()
        helper.updateAttachmentDownloadId(this, messageSid, attachmentSid, downloadId)
    }

    @Transaction
    fun updateMediaUploadStatus(messageUuid: String, attachmentUuid: String, uploadedBytes: Long, uploading: Boolean) {
        val helper = MediaUpdateHelper()
        helper.updateAttachmentUploadProgressByUuid(this, messageUuid, attachmentUuid, uploadedBytes, uploading)
    }
}
