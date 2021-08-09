package com.twilio.conversations.app.common.extensions

import android.content.Context
import com.twilio.conversations.*
import com.twilio.conversations.ConversationsClient.Properties
import com.twilio.conversations.ConversationsClient.SynchronizationStatus.COMPLETED
import com.twilio.conversations.ErrorInfo.Companion.CONVERSATION_NOT_SYNCHRONIZED
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.enums.CrashIn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ConversationsException(val error: ConversationsError, val errorInfo: ErrorInfo? = null) : Exception("$error") {
    constructor(errorInfo: ErrorInfo) : this(ConversationsError.fromErrorInfo(errorInfo), errorInfo)
}

suspend fun createAndSyncClient(context: Context, token: String, properties: Properties = Properties.newBuilder().createProperties()): ConversationsClient {
    val client = createConversationsClient(context, token, properties)
    client.waitForSynchronization()
    return client
}

private suspend fun createConversationsClient(applicationContext: Context, token: String, properties: Properties) =
    suspendCoroutine<ConversationsClient> { continuation ->
        ConversationsClient.create(applicationContext, token, properties, object : CallbackListener<ConversationsClient> {
            override fun onSuccess(conversationsClient: ConversationsClient) = continuation.resume(conversationsClient)

            override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
        })
    }

private suspend fun ConversationsClient.waitForSynchronization(): Unit = suspendCancellableCoroutine { continuation ->
    addListener(
        onClientSynchronization = { status ->
            synchronized(continuation) {
                if (continuation.isActive && status >= COMPLETED) {
                    removeAllListeners()
                    continuation.resume(Unit)
                }
            }
        }
    )
}

suspend fun ConversationsClient.registerFCMToken(token: String) = suspendCancellableCoroutine<Unit> { continuation ->
    registerFCMToken(ConversationsClient.FCMToken(token), object : StatusListener {

        override fun onSuccess() {
            if (continuation.isActive) continuation.resume(Unit)
        }

        override fun onError(errorInfo: ErrorInfo) {
            Timber.d("Failed to register for FCM: $token, $errorInfo")
            if (continuation.isActive) continuation.resumeWithException(ConversationsException(errorInfo))
        }
    })
}

suspend fun ConversationsClient.unregisterFCMToken(token: String) = suspendCancellableCoroutine<Unit> { continuation ->
    unregisterFCMToken(ConversationsClient.FCMToken(token), object : StatusListener {

        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
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

suspend fun ConversationsClient.createConversation(friendlyName: String): Conversation = suspendCoroutine { continuation ->
    createConversation(friendlyName, object : CallbackListener<Conversation> {
        override fun onSuccess(result: Conversation) = continuation.resume(result)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.waitForSynchronization(): Conversation {
    val complete = CompletableDeferred<Unit>()
    val listener = addListener(
        onSynchronizationChanged = { conversation ->
            synchronized<Unit>(complete) {
                if (complete.isCompleted) return@addListener
                if (conversation.synchronizationStatus == Conversation.SynchronizationStatus.FAILED) {
                    val errorInfo = ErrorInfo(CONVERSATION_NOT_SYNCHRONIZED, "Conversation synchronization failed: ${conversation.sid}}")
                    complete.completeExceptionally(ConversationsException(errorInfo))
                } else if (conversation.synchronizationStatus.value >= Conversation.SynchronizationStatus.ALL.value) {
                    complete.complete(Unit)
                }
            }
        }
    )

    try {
        complete.await()
    } finally {
        removeListener(listener)
    }

    return this
}

suspend fun Conversation.removeParticipant(participant: Participant): Unit = suspendCancellableCoroutine { continuation ->
    removeParticipant(participant, object : StatusListener {

        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.addParticipantByIdentity(identity: String, attributes: Attributes = Attributes.DEFAULT): Unit = suspendCancellableCoroutine { continuation ->
    addParticipantByIdentity(identity, attributes, object : StatusListener {

        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.addParticipantByAddress(address: String, proxyAddress: String, attributes: Attributes = Attributes.DEFAULT): Unit = suspendCancellableCoroutine { continuation ->
    addParticipantByAddress(address, proxyAddress, attributes, object : StatusListener {

        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
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

suspend fun ConversationsClient.getConversation(sidOrUniqueName: String): Conversation = suspendCoroutine { continuation ->
    getConversation(sidOrUniqueName, object : CallbackListener<Conversation> {

        override fun onSuccess(result: Conversation) = continuation.resume(result)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.getLastMessages(count: Int): List<Message> = suspendCoroutine { continuation ->
    getLastMessages(count, object : CallbackListener<List<Message>> {

        override fun onSuccess(result: List<Message>) = continuation.resume(result)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.getMessagesBefore(index: Long, count: Int): List<Message> = suspendCoroutine { continuation ->
    getMessagesBefore(index, count, object : CallbackListener<List<Message>> {

        override fun onSuccess(result: List<Message>) = continuation.resume(result)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.sendMessage(message: Message.Options): Message = suspendCoroutine { continuation ->
    sendMessage(message, object : CallbackListener<Message> {

        override fun onSuccess(message: Message) = continuation.resume(message)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.advanceLastReadMessageIndex(index: Long): Long = suspendCoroutine { continuation ->
    advanceLastReadMessageIndex(index, object : CallbackListener<Long> {

        override fun onSuccess(index: Long) = continuation.resume(index)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.getMessageByIndex(index: Long): Message = suspendCoroutine { continuation ->
    getMessageByIndex(index, object : CallbackListener<Message> {

        override fun onSuccess(message: Message) = continuation.resume(message)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.removeMessage(message: Message): Unit = suspendCoroutine { continuation ->
    removeMessage(message, object : StatusListener {

        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Message.setAttributes(attributes: Attributes): Unit = suspendCoroutine { continuation ->
    setAttributes(attributes, object : StatusListener {

        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

suspend fun Conversation.join(): Unit = suspendCoroutine { continuation ->
    join(object : StatusListener {
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

suspend fun Message.getMediaContentTemporaryUrl(): String = suspendCoroutine { continuation ->
    getMediaContentTemporaryUrl(object : CallbackListener<String> {
        override fun onSuccess(contentTemporaryUrl: String) = continuation.resume(contentTemporaryUrl)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(ConversationsException(errorInfo))
    })
}

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

inline fun Message.Options.withMediaProgressListener(
    crossinline onStarted: () -> Unit = {},
    crossinline onProgress: (uploadedBytes: Long) -> Unit = {},
    crossinline onCompleted: () -> Unit = {}
): Message.Options = withMediaProgressListener(object : ProgressListener {
    override fun onStarted() = onStarted()

    override fun onProgress(uploadedBytes: Long) = onProgress(uploadedBytes)

    override fun onCompleted(mediaSid: String?) = onCompleted()
})
