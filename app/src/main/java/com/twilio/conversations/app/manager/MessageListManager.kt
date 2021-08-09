package com.twilio.conversations.app.manager

import com.google.gson.Gson
import com.twilio.conversations.Attributes
import com.twilio.conversations.Message
import com.twilio.conversations.app.common.*
import com.twilio.conversations.app.common.enums.Direction
import com.twilio.conversations.app.common.enums.DownloadState
import com.twilio.conversations.app.common.enums.Reaction
import com.twilio.conversations.app.common.enums.Reactions
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
    suspend fun updateMessageStatus(messageUuid: String, sendStatus: SendStatus, errorCode: Int = 0)
    suspend fun updateMessageMediaDownloadState(
        index: Long,
        downloadState: DownloadState,
        downloadedBytes: Long,
        downloadedLocation: String?
    )
    suspend fun setReactions(index: Long, reactions: Reactions)
    suspend fun notifyMessageRead(index: Long)
    suspend fun typing()
    suspend fun getMediaContentTemporaryUrl(index: Long): String
    suspend fun setMessageMediaDownloadId(messageIndex: Long, id: Long)
    suspend fun removeMessage(messageIndex: Long)
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

    override suspend fun updateMessageStatus(messageUuid: String, sendStatus: SendStatus, errorCode: Int) {
        conversationsRepository.updateMessageStatus(messageUuid, sendStatus.value, errorCode)
    }

    override suspend fun updateMessageMediaDownloadState(
        index: Long,
        downloadState: DownloadState,
        downloadedBytes: Long,
        downloadedLocation: String?
    ) {
        val message = conversationsClient.getConversationsClient().getConversation(conversationSid).getMessageByIndex(index)
        conversationsRepository.updateMessageMediaDownloadStatus(
            messageSid = message.sid,
            downloadedBytes = downloadedBytes,
            downloadLocation = downloadedLocation,
            downloadState = downloadState.value
        )
    }

    override suspend fun setReactions(index: Long, reactions: Reactions) {
        val message = conversationsClient
            .getConversationsClient()
            .getConversation(conversationSid)
            .getMessageByIndex(index)

        val reactionsMap: Map<String, Set<String>> = reactions.map { it.key.value to it.value }.toMap()
        val reactionAttributes = ReactionAttributes(reactionsMap)

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

    override suspend fun removeMessage(messageIndex: Long) {
        val message = conversationsClient.getConversationsClient().getConversation(conversationSid).getMessageByIndex(messageIndex)
        conversationsClient.getConversationsClient().getConversation(conversationSid).removeMessage(message)
    }
}
