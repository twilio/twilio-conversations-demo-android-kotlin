package com.twilio.conversations.app.common.extensions

import android.content.Context
import com.twilio.conversations.*
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.enums.CrashIn
import com.twilio.conversations.extensions.MessageBuilder
import com.twilio.conversations.extensions.sendMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
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

// non-inline wrapper for inline function - to make it mockable
suspend fun Conversation.doSendMessage(block: MessageBuilder.() -> Unit): Message {
    return sendMessage(block)
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

// @todo: remove once multiple media is supported
val Message.firstMedia: Media? get() = attachedMedia.firstOrNull()

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
