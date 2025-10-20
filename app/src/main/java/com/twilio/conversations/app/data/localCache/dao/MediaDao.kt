package com.twilio.conversations.app.data.localCache.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.twilio.conversations.app.data.localCache.entity.MediaDataItem


@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(mediaList: List<MediaDataItem>)

    @Query("SELECT * FROM media_table WHERE messageSid = :messageSid")
    fun getAllMediaByMessageSid(messageSid: String): List<MediaDataItem>?

    @Query("SELECT * FROM media_table WHERE messageUuid = :messageUuid")
    fun getAllMediaByMessageUuid(messageUuid: String): List<MediaDataItem>?

    @Query("UPDATE media_table SET mediaDownloadState = :downloadState WHERE mediaSid = :mediaSid")
    fun updateMediaDownloadState(mediaSid: String, downloadState: Int)

    @Query("UPDATE media_table SET mediaDownloadedBytes = :downloadedBytes WHERE mediaSid = :mediaSid")
    fun updateMediaDownloadedBytes(mediaSid: String, downloadedBytes: Long)

    @Query("UPDATE media_table SET mediaUri = :location WHERE mediaSid = :mediaSid")
    fun updateMediaDownloadLocation(mediaSid: String, location: String)

    @Query("UPDATE media_table SET mediaDownloadId = :downloadId WHERE mediaSid = :mediaSid")
    fun updateMediaDownloadId(mediaSid: String, downloadId: Long)

    @Query("UPDATE media_table SET mediaUploading = :downloading WHERE mediaSid = :mediaSid")
    fun updateMediaUploadStatus(mediaSid: String, downloading: Boolean)

    @Query("UPDATE media_table SET mediaUploadedBytes = :downloadedBytes WHERE mediaSid = :mediaSid")
    fun updateMediaUploadedBytes(mediaSid: String, downloadedBytes: Long)
}