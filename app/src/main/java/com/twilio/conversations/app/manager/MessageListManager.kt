package com.twilio.conversations.app.manager

import com.google.gson.Gson
import com.twilio.conversations.Attributes
import com.twilio.conversations.Message
import com.twilio.conversations.app.common.*
import com.twilio.conversations.app.common.enums.Direction
import com.twilio.conversations.app.common.enums.Reaction
import com.twilio.conversations.app.common.enums.SendStatus
import com.twilio.conversations.app.common.extensions.*
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.data.localCache.entity.MessageDataItem
import com.twilio.conversations.app.data.models.ReactionAttributes
import com.twilio.conversations.app.repository.ConversationsRepository
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.InputStream
import java.util.*

interface MessageListManager {
    suspend fun sendTextMessage(text: String, uuid: String)
    suspend fun retrySendTextMessage(messageUuid: String)
    suspend fun sendMediaMessage(
        uri: String,
        inputStream: InputStream,
        fileName: String?,
        mimeType: String?,
        messageUuid: String
    )
    suspend fun retrySendMediaMessage(inputStream: InputStream, messageUuid: String)
    suspend fun updateMessageStatus(messageUuid: String, sendStatus: SendStatus)
    suspend fun updateMessageMediaDownloadStatus(
        index: Long,
        downloading: Boolean,
        downloadedBytes: Long,
        downloadedLocation: String?
    )
    suspend fun addRemoveReaction(index: Long, reaction: Reaction)
    suspend fun notifyMessageRead(index: Long)
    suspend fun typing()
    suspend fun getMediaContentTemporaryUrl(index: Long): String
    suspend fun setMessageMediaDownloadId(messageIndex: Long, id: Long)
}

class MessageListManagerImpl(
    private val conversationSid: String,
    private val conversationsClient: ConversationsClientWrapper,
    private val conversationsRepository: ConversationsRepository,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : MessageListManager {

    override suspend fun sendTextMessage(text: String, uuid: String) {
        val identity = conversationsClient.getConversationsClient().myIdentity
        val conversation = conversationsClient.getConversationsClient().getConversation(conversationSid)
        val participantSid = conversation.getParticipantByIdentity(identity).sid
        val attributes = Attributes(uuid)
        val options = Message.options().withBody(text).withAttributes(attributes)
        val message = MessageDataItem(
            "",
            conversationSid,
            participantSid,
            Message.Type.TEXT.value,
            identity,
            Date().time,
            text,
            -1,
            attributes.toString(),
            Direction.OUTGOING.value,
            SendStatus.SENDING.value,
            uuid
        )
        conversationsRepository.insertMessage(message)
        val sentMessage = conversation.sendMessage(options).toMessageDataItem(identity, uuid)
        conversationsRepository.updateMessageByUuid(sentMessage)
    }

    override suspend fun retrySendTextMessage(messageUuid: String) {
        val message = withContext(dispatchers.io()) { conversationsRepository.getMessageByUuid(messageUuid) } ?: return
        if (message.sendStatus == SendStatus.SENDING.value) return
        conversationsRepository.updateMessageByUuid(message.copy(sendStatus = SendStatus.SENDING.value))
        val identity = conversationsClient.getConversationsClient().myIdentity
        val attributes = Attributes(message.uuid)
        val options = Message.options().withBody(message.body).withAttributes(attributes)
        val sentMessage = conversationsClient
            .getConversationsClient()
            .getConversation(conversationSid)
            .sendMessage(options)
            .toMessageDataItem(identity, message.uuid)

        conversationsRepository.updateMessageByUuid(sentMessage)
    }

    override suspend fun sendMediaMessage(
        uri: String,
        inputStream: InputStream,
        fileName: String?,
        mimeType: String?,
        messageUuid: String
    ) {
        val identity = conversationsClient.getConversationsClient().myIdentity
        val conversation = conversationsClient.getConversationsClient().getConversation(conversationSid)
        val participantSid = conversation.getParticipantByIdentity(identity).sid
        val attributes = Attributes(messageUuid)
        val options = getMediaMessageOptions(uri, inputStream, fileName, mimeType, messageUuid)
        val message = MessageDataItem(
            "",
            conversationSid,
            participantSid,
            Message.Type.MEDIA.value,
            identity,
            Date().time,
            null,
            -1,
            attributes.toString(),
            Direction.OUTGOING.value,
            SendStatus.SENDING.value,
            messageUuid,
            mediaFileName = fileName,
            mediaUploadUri = uri,
            mediaType = mimeType
        )
        conversationsRepository.insertMessage(message)
        val sentMessage = conversation.sendMessage(options).toMessageDataItem(identity, messageUuid)
        conversationsRepository.updateMessageByUuid(sentMessage)
    }

    override suspend fun retrySendMediaMessage(
        inputStream: InputStream,
        messageUuid: String
    ) {
        val message = withContext(dispatchers.io()) { conversationsRepository.getMessageByUuid(messageUuid) } ?: return
        if (message.sendStatus == SendStatus.SENDING.value) return
        if (message.mediaUploadUri == null) {
            Timber.w("Missing mediaUploadUri in retrySendMediaMessage: $message")
            return
        }
        conversationsRepository.updateMessageByUuid(message.copy(sendStatus = SendStatus.SENDING.value))
        val identity = conversationsClient.getConversationsClient().myIdentity
        val options = getMediaMessageOptions(message.mediaUploadUri, inputStream,
            message.mediaFileName, message.mediaType, messageUuid)

        val sentMessage = conversationsClient
            .getConversationsClient()
            .getConversation(conversationSid)
            .sendMessage(options)
            .toMessageDataItem(identity, message.uuid)

        conversationsRepository.updateMessageByUuid(sentMessage)
    }

    private fun getMediaMessageOptions(
        uri: String,
        inputStream: InputStream,
        fileName: String?,
        mimeType: String?,
        messageUuid: String
    ): Message.Options {
        val attributes = Attributes(messageUuid)
        var options = Message.options().withMedia(inputStream, mimeType).withAttributes(attributes)
            .withMediaProgressListener(
                onStarted = {
                    Timber.d("Upload started for $uri")
                    conversationsRepository.updateMessageMediaUploadStatus(messageUuid, uploading = true)
                },
                onProgress = { uploadedBytes ->
                    Timber.d("Upload progress for $uri: $uploadedBytes bytes")
                    conversationsRepository.updateMessageMediaUploadStatus(
                        messageUuid,
                        uploadedBytes = uploadedBytes
                    )
                },
                onCompleted = {
                    Timber.d("Upload for $uri complete")
                    conversationsRepository.updateMessageMediaUploadStatus(messageUuid, uploading = false)
                }
            )
        if (fileName != null) {
            options = options.withMediaFileName(fileName)
        }
        return options
    }

    override suspend fun updateMessageStatus(messageUuid: String, sendStatus: SendStatus) {
        conversationsRepository.updateMessageStatus(messageUuid, sendStatus.value)
    }

    override suspend fun updateMessageMediaDownloadStatus(
        index: Long,
        downloading: Boolean,
        downloadedBytes: Long,
        downloadedLocation: String?
    ) {
        val message = conversationsClient.getConversationsClient().getConversation(conversationSid).getMessageByIndex(index)
        conversationsRepository.updateMessageMediaDownloadStatus(
            messageSid = message.sid,
            downloadedBytes = downloadedBytes,
            downloadLocation = downloadedLocation,
            downloading = downloading
        )
    }

    override suspend fun addRemoveReaction(index: Long, reaction: Reaction) {
        val identity = conversationsClient.getConversationsClient().myIdentity
        val message = conversationsClient
            .getConversationsClient()
            .getConversation(conversationSid)
            .getMessageByIndex(index)
        val attributes = message.attributes
        val reactions = getReactions("$attributes").toMutableMap()
        val participantSids = reactions.getOrPut(reaction.value, ::emptySet).toMutableSet()

        if (identity in participantSids) {
            participantSids -= identity
        } else {
            participantSids += identity
        }

        reactions[reaction.value] = participantSids
        val reactionAttributes = ReactionAttributes(reactions)
        Timber.d("Updating reactions: $reactions")
        message.setAttributes(Attributes(JSONObject(Gson().toJson(reactionAttributes))))
    }

    override suspend fun notifyMessageRead(index: Long) {
        val messages = conversationsClient.getConversationsClient().getConversation(conversationSid)
        if (index > messages.lastReadMessageIndex ?: -1) {
            messages.advanceLastReadMessageIndex(index)
        }
    }

    override suspend fun typing() {
        conversationsClient.getConversationsClient().getConversation(conversationSid).typing()
    }

    override suspend fun getMediaContentTemporaryUrl(index: Long): String {
        val message = conversationsClient.getConversationsClient().getConversation(conversationSid).getMessageByIndex(index)
        return message.getMediaContentTemporaryUrl()
    }

    override suspend fun setMessageMediaDownloadId(messageIndex: Long, id: Long) {
        val message = conversationsClient.getConversationsClient().getConversation(conversationSid).getMessageByIndex(messageIndex)
        conversationsRepository.updateMessageMediaDownloadStatus(messageSid = message.sid, downloadId = id)
    }
}
