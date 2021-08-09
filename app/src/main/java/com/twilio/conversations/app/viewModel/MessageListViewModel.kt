package com.twilio.conversations.app.viewModel

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import com.twilio.conversations.app.common.SingleLiveEvent
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.enums.DownloadState
import com.twilio.conversations.app.common.enums.Reaction
import com.twilio.conversations.app.common.enums.Reactions
import com.twilio.conversations.app.common.enums.SendStatus
import com.twilio.conversations.app.common.extensions.*
import com.twilio.conversations.app.data.localCache.entity.ParticipantDataItem
import com.twilio.conversations.app.data.models.MessageListViewItem
import com.twilio.conversations.app.data.models.RepositoryRequestStatus
import com.twilio.conversations.app.manager.MessageListManager
import com.twilio.conversations.app.repository.ConversationsRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.InputStream
import java.util.*

const val MESSAGE_COUNT = 50

class MessageListViewModel(
    private val appContext: Context,
    val conversationSid: String,
    private val conversationsRepository: ConversationsRepository,
    private val messageListManager: MessageListManager
) : ViewModel() {

    val conversationName = MutableLiveData<String>()

    val selfUser = conversationsRepository.getSelfUser().asLiveData(viewModelScope.coroutineContext)

    val messageItems = conversationsRepository.getMessages(conversationSid, MESSAGE_COUNT)
        .onEach { repositoryResult ->
            if (repositoryResult.requestStatus is RepositoryRequestStatus.Error) {
                onMessageError.postValue(ConversationsError.MESSAGE_FETCH_FAILED)
            }
        }
        .asLiveData(viewModelScope.coroutineContext)
        .map { it.data }

    val onMessageError = SingleLiveEvent<ConversationsError>()

    val onMessageSent = SingleLiveEvent<Unit>()

    val onShowRemoveMessageDialog = SingleLiveEvent<Unit>()

    val onMessageRemoved = SingleLiveEvent<Unit>()

    val onMessageCopied = SingleLiveEvent<Unit>()

    var selectedMessageIndex: Long = -1

    val selectedMessage: MessageListViewItem? get() = messageItems.value?.firstOrNull { it.index == selectedMessageIndex }

    val typingParticipantsList = conversationsRepository.getTypingParticipants(conversationSid)
        .map { participants -> participants.map { it.typingIndicatorName } }
        .distinctUntilChanged()
        .asLiveData(viewModelScope.coroutineContext)

    private val messagesObserver: Observer<PagedList<MessageListViewItem>> =
        Observer { list ->
            list.iterator().forEach { message ->
                if (message.mediaDownloadState == DownloadState.DOWNLOADING && message.mediaDownloadId != null) {
                    if (updateMessageMediaDownloadState(message.index, message.mediaDownloadId)) {
                        observeMessageMediaDownload(message.index, message.mediaDownloadId)
                    }
                }
            }
        }

    init {
        Timber.d("init: $conversationSid")
        viewModelScope.launch {
            getConversationResult()
        }
        messageItems.observeForever(messagesObserver)
    }

    override fun onCleared() {
        messageItems.removeObserver(messagesObserver)
    }

    private suspend fun getConversationResult() {
        conversationsRepository.getConversation(conversationSid).collect { result ->
            if (result.requestStatus is RepositoryRequestStatus.Error) {
                onMessageError.value = ConversationsError.CONVERSATION_GET_FAILED
                return@collect
            }
            conversationName.value = result.data?.friendlyName?.takeIf { it.isNotEmpty() } ?: result.data?.sid
        }
    }

    fun sendTextMessage(message: String) = viewModelScope.launch {
        val messageUuid = UUID.randomUUID().toString()
        Timber.d("messageUuid: $messageUuid")
        try {
            messageListManager.sendTextMessage(message, messageUuid)
            onMessageSent.call()
            Timber.d("Message sent: $messageUuid")
        } catch (e: ConversationsException) {
            Timber.d("Text message send error: ${e.errorInfo?.status}:${e.errorInfo?.code} ${e.errorInfo?.message}")
            messageListManager.updateMessageStatus(messageUuid, SendStatus.ERROR, e.errorInfo?.code ?: 0)
            onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
        }
    }

    fun resendTextMessage(messageUuid: String) = viewModelScope.launch {
        try {
            messageListManager.retrySendTextMessage(messageUuid)
            onMessageSent.call()
            Timber.d("Message re-sent: $messageUuid")
        } catch (e: ConversationsException) {
            messageListManager.updateMessageStatus(messageUuid, SendStatus.ERROR, e.errorInfo?.code ?: 0)
            onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
        }
    }

    fun sendMediaMessage(uri: String, inputStream: InputStream, fileName: String?, mimeType: String?) =
        viewModelScope.launch {
            val messageUuid = UUID.randomUUID().toString()
            try {
                messageListManager.sendMediaMessage(uri, inputStream, fileName, mimeType, messageUuid)
                onMessageSent.call()
                Timber.d("Media message sent: $messageUuid")
            } catch (e: ConversationsException) {
                Timber.d("Media message send error: ${e.errorInfo?.status}:${e.errorInfo?.code} ${e.errorInfo?.message}")
                messageListManager.updateMessageStatus(messageUuid, SendStatus.ERROR, e.errorInfo?.code ?: 0)
                onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
            }
        }

    fun resendMediaMessage(inputStream: InputStream, messageUuid: String) = viewModelScope.launch {
        try {
            messageListManager.retrySendMediaMessage(inputStream, messageUuid)
            onMessageSent.call()
            Timber.d("Media re-sent: $messageUuid")
        } catch (e: ConversationsException) {
            messageListManager.updateMessageStatus(messageUuid, SendStatus.ERROR, e.errorInfo?.code ?: 0)
            onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
        }
    }

    fun handleMessageDisplayed(messageIndex: Long) = viewModelScope.launch {
        try {
            messageListManager.notifyMessageRead(messageIndex)
        } catch (e: ConversationsException) {
            // Ignored
        }
    }

    fun typing() = viewModelScope.launch {
        Timber.d("Typing in conversation $conversationSid")
        messageListManager.typing()
    }

    fun setReactions(reactions: Reactions) = viewModelScope.launch {
        try {
            messageListManager.setReactions(selectedMessageIndex, reactions)
        } catch (e: ConversationsException) {
            onMessageError.value = ConversationsError.REACTION_UPDATE_FAILED
        }
    }

    fun copyMessageToClipboard() {
        try {
            val message = selectedMessage ?: error("No message selected")
            val clip = ClipData.newPlainText("Message text", message.body)
            val clipboard = ContextCompat.getSystemService(appContext, ClipboardManager::class.java)
            clipboard!!.setPrimaryClip(clip)
            onMessageCopied.call()
        } catch (e: Exception) {
            Timber.d("Failed to copy message")
            onMessageError.value = ConversationsError.MESSAGE_COPY_FAILED
        }
    }

    fun removeMessage() = viewModelScope.launch {
        try {
            messageListManager.removeMessage(selectedMessageIndex)
            onMessageRemoved.call()
        } catch (e: ConversationsException) {
            Timber.d("Failed to remove message")
            onMessageError.value = ConversationsError.MESSAGE_REMOVE_FAILED
        }
    }

    fun updateMessageMediaDownloadStatus(
        messageIndex: Long,
        downloadState: DownloadState,
        downloadedBytes: Long = 0,
        downloadedLocation: String? = null
    ) = viewModelScope.launch {
        messageListManager.updateMessageMediaDownloadState(
            messageIndex,
            downloadState,
            downloadedBytes,
            downloadedLocation
        )
    }

    fun startMessageMediaDownload(messageIndex: Long, fileName: String?) = viewModelScope.launch {
        Timber.d("Start file download for message index $messageIndex")
        updateMessageMediaDownloadStatus(messageIndex, DownloadState.DOWNLOADING)

        val sourceUriResult = runCatching { Uri.parse(messageListManager.getMediaContentTemporaryUrl(messageIndex)) }
        val sourceUri = sourceUriResult.getOrElse { e ->
            Timber.w(e, "Message media download failed: cannot get sourceUri")
            updateMessageMediaDownloadStatus(messageIndex, DownloadState.ERROR)
            return@launch
        }

        val downloadManager =
            appContext.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
        val downloadRequest = DownloadManager.Request(sourceUri).apply {
            setDestinationInExternalFilesDir(
                appContext,
                Environment.DIRECTORY_DOWNLOADS,
                fileName ?: sourceUri.pathSegments.last()
            )
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }
        val downloadId = downloadManager.enqueue(downloadRequest)
        Timber.d("Download enqueued with ID: $downloadId")

        messageListManager.setMessageMediaDownloadId(messageIndex, downloadId)
        observeMessageMediaDownload(messageIndex, downloadId)
    }

    private fun observeMessageMediaDownload(messageIndex: Long, downloadId: Long) {
        val downloadManager = appContext.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
        val downloadCursor = downloadManager.queryById(downloadId)
        val downloadObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                if (!updateMessageMediaDownloadState(messageIndex, downloadId)) {
                    Timber.d("Download $downloadId completed")
                    downloadCursor.unregisterContentObserver(this)
                    downloadCursor.close()
                }
            }
        }
        downloadCursor.registerContentObserver(downloadObserver)
    }

    /**
     * Notifies the view model of the current download state
     * @return true if the download is still in progress
     */
    private fun updateMessageMediaDownloadState(messageIndex: Long, downloadId: Long): Boolean {
        val downloadManager = appContext.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = downloadManager.queryById(downloadId)

        if (!cursor.moveToFirst()) {
            cursor.close()
            return false
        }

        val status = cursor.getInt(DownloadManager.COLUMN_STATUS)
        val downloadInProgress = status != DownloadManager.STATUS_FAILED && status != DownloadManager.STATUS_SUCCESSFUL
        val downloadedBytes = cursor.getLong(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
        Timber.d("Download status changed. Status: $status, downloaded bytes: $downloadedBytes")

        updateMessageMediaDownloadStatus(messageIndex, DownloadState.DOWNLOADING, downloadedBytes)

        when (status) {
            DownloadManager.STATUS_SUCCESSFUL -> {
                val downloadedFile = cursor.getString(DownloadManager.COLUMN_LOCAL_URI).toUri().toFile()
                val downloadedLocation =
                    FileProvider.getUriForFile(appContext, "com.twilio.conversations.app.fileprovider", downloadedFile)
                        .toString()
                updateMessageMediaDownloadStatus(
                    messageIndex,
                    DownloadState.COMPLETED,
                    downloadedBytes,
                    downloadedLocation
                )
            }
            DownloadManager.STATUS_FAILED -> {
                onMessageError.value = ConversationsError.MESSAGE_MEDIA_DOWNLOAD_FAILED
                updateMessageMediaDownloadStatus(messageIndex, DownloadState.ERROR, downloadedBytes)
                Timber.w(
                    "Message media download failed. Failure reason: %s",
                    cursor.getString(DownloadManager.COLUMN_REASON)
                )
            }
        }

        cursor.close()
        return downloadInProgress
    }

    private val ParticipantDataItem.typingIndicatorName get() = if (friendlyName.isNotEmpty()) friendlyName else identity
}
