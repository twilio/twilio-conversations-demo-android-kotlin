package com.twilio.conversations.app.data.localCache.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.twilio.conversations.app.data.localCache.entity.MessageAttachmentDataItem

class AttachmentsListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromAttachmentsList(attachmentsList: List<MessageAttachmentDataItem>): String {
        return gson.toJson(attachmentsList)
    }

    @TypeConverter
    fun toAttachmentsList(attachmentsListString: String): List<MessageAttachmentDataItem> {
        val listType = object : TypeToken<List<MessageAttachmentDataItem>>() {}.type
        return gson.fromJson(attachmentsListString, listType) ?: emptyList()
    }
}