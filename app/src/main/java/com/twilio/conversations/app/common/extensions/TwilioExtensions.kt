package com.twilio.conversations.app.common.extensions

import android.content.Context
import com.twilio.conversations.*
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.enums.CrashIn
import com.twilio.conversations.extensions.buildAndSend
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ConversationsException(val error: ConversationsError, val errorInfo: ErrorInfo? = null) : Exception("$error") {
    constructor(errorInfo: ErrorInfo) : this(ConversationsError.fromErrorInfo(errorInfo), errorInfo)
}

suspend fun ConversationsClient.updateToken(token: String) = suspendCancellableCoroutine<Unit> { continuation ->
    updateToken(token, object : StatusListener {

        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

fun ConversationsClient.simulateCrash(where: CrashIn) {
    val method = this::class.java.getDeclaredMethod("simulateCrash", Int::class.java)
    method.isAccessible = true
    method.invoke(this, where.value)
}

suspend fun Participant.getAndSubscribeUser(): User = suspendCancellableCoroutine { continuation ->
    getAndSubscribeUser(object : CallbackListener<User> {

        override fun onSuccess(user: User) = continuation.resume(user)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.getParticipantCount(): Long = suspendCancellableCoroutine { continuation ->
    getParticipantsCount(object : CallbackListener<Long> {

        override fun onSuccess(count: Long) = continuation.resume(count)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.getMessageCount(): Long = suspendCancellableCoroutine { continuation ->
    getMessagesCount(object : CallbackListener<Long> {

        override fun onSuccess(count: Long) = continuation.resume(count)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.getUnreadMessageCount(): Long? = suspendCancellableCoroutine { continuation ->
    getUnreadMessagesCount(object : CallbackListener<Long?> {

        override fun onSuccess(count: Long?) = continuation.resume(count)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.sendTextMessage(attributes: Attributes, body: String?): Message {
    return prepareMessage()
        .setAttributes(attributes)
        .setBody(body)
        .buildAndSend()
}

suspend fun Conversation.sendMediaMessage(attributes: Attributes,
                                          inputStream: InputStream,
                                          mimeType: String?,
                                          fileName: String?,
                                          mediaUploadListener: MediaUploadListener): Message {
    return prepareMessage()
        .setAttributes(attributes)
        .addMedia(inputStream, mimeType ?: "", fileName, mediaUploadListener)
        .buildAndSend()
}

suspend fun Conversation.removeMessage(message: Message): Unit = suspendCoroutine { continuation ->
    removeMessage(message, object : StatusListener {

        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.muteConversation(): Unit = suspendCoroutine { continuation ->
    setNotificationLevel(Conversation.NotificationLevel.MUTED, object : StatusListener {
        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.unmuteConversation(): Unit = suspendCoroutine { continuation ->
    setNotificationLevel(Conversation.NotificationLevel.DEFAULT, object : StatusListener {
        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.leave(): Unit = suspendCoroutine { continuation ->
    leave(object : StatusListener {
        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.destroy(): Unit = suspendCoroutine { continuation ->
    destroy(object : StatusListener {
        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.setFriendlyName(friendlyName: String): Unit = suspendCoroutine { continuation ->
    setFriendlyName(friendlyName, object : StatusListener {
        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun User.setFriendlyName(friendlyName: String): Unit = suspendCoroutine { continuation ->
    setFriendlyName(friendlyName, object : StatusListener {
        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

// @todo: remove once support multiple media
val Message.firstMedia: Media? get() = attachedMedia.firstOrNull()

inline fun ConversationsClient.addListener(
    crossinline onConversationAdded: (conversation: Conversation) -> Unit = {},
    crossinline onConversationUpdated: (conversation: Conversation, reason: Conversation.UpdateReason) -> Unit = { _, _ -> Unit },
    crossinline onConversationDeleted: (conversation: Conversation) -> Unit = {},
    crossinline onConversationSynchronizationChange: (conversation: Conversation) -> Unit = {},
    crossinline onError: (errorInfo: ErrorInfo) -> Unit = {},
    crossinline onUserUpdated: (user: User, reason: User.UpdateReason) -> Unit = { _, _ -> Unit },
    crossinline onUserSubscribed: (user: User) -> Unit = {},
    crossinline onUserUnsubscribed: (user: User) -> Unit = {},
    crossinline onClientSynchronization: (status: ConversationsClient.SynchronizationStatus) -> Unit = {},
    crossinline onNewMessageNotification: (conversationSid: String, messageSid: String, messageIndex: Long) -> Unit = { _, _, _ -> Unit },
    crossinline onAddedToConversationNotification: (conversationSid: String) -> Unit = {},
    crossinline onRemovedFromConversationNotification: (conversationSid: String) -> Unit = {},
    crossinline onNotificationSubscribed: () -> Unit = {},
    crossinline onNotificationFailed: (errorInfo: ErrorInfo) -> Unit = {},
    crossinline onConnectionStateChange: (state: ConversationsClient.ConnectionState) -> Unit = {},
    crossinline onTokenExpired: () -> Unit = {},
    crossinline onTokenAboutToExpire: () -> Unit = {}) {

    val listener = createClientListener(
        onConversationAdded,
        onConversationUpdated,
        onConversationDeleted,
        onConversationSynchronizationChange,
        onError,
        onUserUpdated,
        onUserSubscribed,
        onUserUnsubscribed,
        onClientSynchronization,
        onNewMessageNotification,
        onAddedToConversationNotification,
        onRemovedFromConversationNotification,
        onNotificationSubscribed,
        onNotificationFailed,
        onConnectionStateChange,
        onTokenExpired,
        onTokenAboutToExpire)

    addListener(listener)
}

inline fun createClientListener(
    crossinline onConversationAdded: (conversation: Conversation) -> Unit = {},
    crossinline onConversationUpdated: (conversation: Conversation, reason: Conversation.UpdateReason) -> Unit = { _, _ -> Unit },
    crossinline onConversationDeleted: (conversation: Conversation) -> Unit = {},
    crossinline onConversationSynchronizationChange: (conversation: Conversation) -> Unit = {},
    crossinline onError: (errorInfo: ErrorInfo) -> Unit = {},
    crossinline onUserUpdated: (user: User, reason: User.UpdateReason) -> Unit = { _, _ -> Unit },
    crossinline onUserSubscribed: (user: User) -> Unit = {},
    crossinline onUserUnsubscribed: (user: User) -> Unit = {},
    crossinline onClientSynchronization: (status: ConversationsClient.SynchronizationStatus) -> Unit = {},
    crossinline onNewMessageNotification: (conversationSid: String, messageSid: String, messageIndex: Long) -> Unit = { _, _, _ -> Unit },
    crossinline onAddedToConversationNotification: (conversationSid: String) -> Unit = {},
    crossinline onRemovedFromConversationNotification: (conversationSid: String) -> Unit = {},
    crossinline onNotificationSubscribed: () -> Unit = {},
    crossinline onNotificationFailed: (errorInfo: ErrorInfo) -> Unit = {},
    crossinline onConnectionStateChange: (state: ConversationsClient.ConnectionState) -> Unit = {},
    crossinline onTokenExpired: () -> Unit = {},
    crossinline onTokenAboutToExpire: () -> Unit = {}
): ConversationsClientListener = object : ConversationsClientListener {

    override fun onConversationAdded(conversation: Conversation) = onConversationAdded(conversation)

    override fun onConversationUpdated(conversation: Conversation, reason: Conversation.UpdateReason) = onConversationUpdated(conversation, reason)

    override fun onConversationDeleted(conversation: Conversation) = onConversationDeleted(conversation)

    override fun onConversationSynchronizationChange(conversation: Conversation) = onConversationSynchronizationChange(conversation)

    override fun onError(errorInfo: ErrorInfo) = onError(errorInfo)

    override fun onUserUpdated(user: User, reason: User.UpdateReason) = onUserUpdated(user, reason)

    override fun onUserSubscribed(user: User) = onUserSubscribed(user)

    override fun onUserUnsubscribed(user: User) = onUserUnsubscribed(user)

    override fun onClientSynchronization(status: ConversationsClient.SynchronizationStatus) = onClientSynchronization(status)

    override fun onNewMessageNotification(conversationSid: String, messageSid: String, messageIndex: Long) = onNewMessageNotification(conversationSid, messageSid, messageIndex)

    override fun onAddedToConversationNotification(conversationSid: String) = onAddedToConversationNotification(conversationSid)

    override fun onRemovedFromConversationNotification(conversationSid: String) = onRemovedFromConversationNotification(conversationSid)

    override fun onNotificationSubscribed() = onNotificationSubscribed()

    override fun onNotificationFailed(errorInfo: ErrorInfo) = onNotificationFailed(errorInfo)

    override fun onConnectionStateChange(state: ConversationsClient.ConnectionState) = onConnectionStateChange(state)

    override fun onTokenExpired() = onTokenExpired()

    override fun onTokenAboutToExpire() = onTokenAboutToExpire()
}

inline fun Conversation.addListener(
    crossinline onMessageAdded: (message: Message) -> Unit = {},
    crossinline onMessageUpdated: (message: Message, reason: Message.UpdateReason) -> Unit = { _, _ -> Unit },
    crossinline onMessageDeleted: (message: Message) -> Unit = {},
    crossinline onParticipantAdded: (participant: Participant) -> Unit = {},
    crossinline onParticipantUpdated: (participant: Participant, reason: Participant.UpdateReason) -> Unit = { _, _ -> Unit },
    crossinline onParticipantDeleted: (participant: Participant) -> Unit = {},
    crossinline onTypingStarted: (conversation: Conversation, participant: Participant) -> Unit = { _, _ -> Unit },
    crossinline onTypingEnded: (conversation: Conversation, participant: Participant) -> Unit = { _, _ -> Unit },
    crossinline onSynchronizationChanged: (conversation: Conversation) -> Unit = {}): ConversationListener {

    val listener = createConversationListener(
        onParticipantAdded,
        onParticipantUpdated,
        onParticipantDeleted,
        onMessageAdded,
        onMessageUpdated,
        onMessageDeleted,
        onTypingStarted,
        onTypingEnded,
        onSynchronizationChanged
    )
    addListener(listener)
    return listener
}

inline fun createConversationListener(
    crossinline onParticipantAdded: (participant: Participant) -> Unit = {},
    crossinline onParticipantUpdated: (participant: Participant, updateReason: Participant.UpdateReason) -> Unit = { _, _ -> Unit },
    crossinline onParticipantDeleted: (participant: Participant) -> Unit = {},
    crossinline onMessageAdded: (message: Message) -> Unit = {},
    crossinline onMessageUpdated: (message: Message, updateReason: Message.UpdateReason) -> Unit = { _, _ -> Unit },
    crossinline onMessageDeleted: (message: Message) -> Unit = {},
    crossinline onTypingStarted: (conversation: Conversation, participant: Participant) -> Unit = { _, _ -> Unit },
    crossinline onTypingEnded: (conversation: Conversation, participant: Participant) -> Unit = { _, _ -> Unit },
    crossinline onSynchronizationChanged: (conversation: Conversation) -> Unit = {}
): ConversationListener = object : ConversationListener {

    override fun onParticipantAdded(participant: Participant) = onParticipantAdded(participant)

    override fun onParticipantUpdated(participant: Participant, updateReason: Participant.UpdateReason) =
        onParticipantUpdated(participant, updateReason)

    override fun onParticipantDeleted(participant: Participant) = onParticipantDeleted(participant)

    override fun onMessageAdded(message: Message) = onMessageAdded(message)

    override fun onMessageUpdated(message: Message, updateReason: Message.UpdateReason) =
        onMessageUpdated(message, updateReason)

    override fun onMessageDeleted(message: Message) = onMessageDeleted(message)

    override fun onTypingStarted(conversation: Conversation, participant: Participant) =
        onTypingStarted(conversation, participant)

    override fun onTypingEnded(conversation: Conversation, participant: Participant) =
        onTypingEnded(conversation, participant)

    override fun onSynchronizationChanged(conversation: Conversation) = onSynchronizationChanged(conversation)

}
