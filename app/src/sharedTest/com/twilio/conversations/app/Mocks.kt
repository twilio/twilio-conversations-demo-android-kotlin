package com.twilio.conversations.app

import com.twilio.conversations.Conversation
import com.twilio.conversations.Message
import com.twilio.conversations.app.common.enums.Direction
import com.twilio.conversations.app.common.enums.DownloadState
import com.twilio.conversations.app.common.enums.DownloadState.NOT_STARTED
import com.twilio.conversations.app.common.enums.SendStatus
import com.twilio.conversations.app.data.localCache.entity.ConversationDataItem
import com.twilio.conversations.app.data.localCache.entity.MessageDataItem
import com.twilio.conversations.app.data.localCache.entity.ParticipantDataItem
import com.twilio.conversations.app.data.models.ConversationDetailsViewItem
import com.twilio.conversations.app.data.models.ParticipantListViewItem
import com.twilio.conversations.app.data.models.UserViewItem
import java.util.*

fun createTestConversationDataItem(sid: String = UUID.randomUUID().toString(),
                              friendlyName: String = "",
                              attributes: String = "\"\"",
                              uniqueName: String = "",
                              dateUpdated: Long = 0,
                              dateCreated: Long = 0,
                              lastMessageDate: Long = 0,
                              lastMessageText: String = "",
                              lastMessageSendStatus: Int = 0,
                              createdBy: String = "",
                              participantsCount: Long = 0,
                              messagesCount: Long = 0,
                              unreadMessagesCount: Long = 0,
                              participatingStatus: Int = 1,
                              notificationLevel: Int = 0
) = ConversationDataItem(sid, friendlyName, attributes, uniqueName, dateUpdated, dateCreated, lastMessageDate,
    lastMessageText, lastMessageSendStatus, createdBy, participantsCount, messagesCount, unreadMessagesCount,
    participatingStatus, notificationLevel)

fun createTestMessageDataItem(sid: String = UUID.randomUUID().toString(),
                              conversationSid: String = UUID.randomUUID().toString(),
                              participantSid: String = UUID.randomUUID().toString(),
                              type: Int = 0,
                              author: String = "",
                              dateCreated: Long = 0,
                              body: String = "",
                              index: Long = 0,
                              attributes: String = "",
                              direction: Int = 0,
                              sendStatus: Int = 0,
                              uuid: String = UUID.randomUUID().toString(),
                              mediaSid: String? = null,
                              mediaFileName: String? = null,
                              mediaType: String? = null,
                              mediaSize: Long? = null,
                              mediaUri: String? = null,
                              mediaDownloadId: Long? = null,
                              mediaDownloadedBytes: Long? = null,
                              mediaDownloadState: Int = NOT_STARTED.value,
                              mediaUploading: Boolean = false,
                              mediaUploadedBytes: Long? = null,
                              mediaUploadUri: String? = null
) = MessageDataItem(sid, conversationSid, participantSid, type, author, dateCreated, body,
    index, attributes, direction, sendStatus, uuid, mediaSid, mediaFileName, mediaType,
    mediaSize, mediaUri, mediaDownloadId, mediaDownloadedBytes, mediaDownloadState, mediaUploading,
    mediaUploadedBytes, mediaUploadUri)

fun createTestParticipantDataItem(
    sid: String = "",
    identity: String = "",
    conversationSid: String = "",
    friendlyName: String = "",
    lastReadMessageIndex: Long? = null,
    lastReadTimestamp: String? = null,
    typing: Boolean = false
) = ParticipantDataItem(sid, identity, conversationSid, friendlyName, true,
    lastReadMessageIndex, lastReadTimestamp, typing)

fun createTestParticipantListViewItem(
    sid: String = "",
    identity: String = "",
    conversationSid: String = "",
    friendlyName: String = "",
    isOnline: Boolean = false,
) = ParticipantListViewItem(
    sid = sid,
    identity = identity,
    conversationSid = conversationSid,
    friendlyName = friendlyName,
    isOnline = isOnline,
)

fun createTestConversationDetailsViewItem(
    conversationName: String = "",
    createdBy: String = "",
    createdOn: String = "",
    isMuted:
    Boolean = false
) = ConversationDetailsViewItem("", conversationName, createdBy, createdOn, isMuted)

fun createTestUserViewItem(friendlyName: String = "", identity: String = "")
        = UserViewItem(friendlyName = friendlyName, identity = identity)

fun getMockedConversations(count: Int, name: String,
                      status: Conversation.ConversationStatus = Conversation.ConversationStatus.NOT_PARTICIPATING,
                      notificationLevel: Conversation.NotificationLevel = Conversation.NotificationLevel.DEFAULT
): ArrayList<ConversationDataItem> {
    val conversations = arrayListOf<ConversationDataItem>()
    for(index in 0 until count) {
        conversations.add(createTestConversationDataItem(friendlyName = "${name}_$index",
            participatingStatus = status.value, notificationLevel = notificationLevel.value))
    }
    return conversations
}

fun getMockedMessages(count: Int, body: String, conversationSid: String, direction: Int = Direction.OUTGOING.value,
                      author: String = "", attributes: String = "", type: Message.Type = Message.Type.TEXT,
                      mediaFileName: String = "", mediaSize: Long = 0, mediaDownloadState: DownloadState = NOT_STARTED,
                      mediaUri: String? = null, mediaDownloadedBytes: Long? = null,
                      sendStatus: SendStatus = SendStatus.UNDEFINED): List<MessageDataItem> {
    val messages = Array(count) { index ->
        createTestMessageDataItem(conversationSid = conversationSid, index = index.toLong(),
            body = "${body}_$index", direction = direction, author = author, attributes = attributes,
        type = type.value, mediaFileName = mediaFileName, mediaSize = mediaSize, mediaDownloadState = mediaDownloadState.value,
        mediaUri = mediaUri, mediaDownloadedBytes = mediaDownloadedBytes, sendStatus = sendStatus.value)
    }
    return messages.toList()
}

fun getMockedParticipants(count: Int, name: String): ArrayList<ParticipantDataItem> {
    val participants = arrayListOf<ParticipantDataItem>()
    for(index in 0 until count) {
        participants.add(
            createTestParticipantDataItem(friendlyName = "${name}_$index")
        )
    }
    return participants
}
