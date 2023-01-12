package com.twilio.conversations.app.common.extensions

import com.twilio.conversations.CallbackListener
import com.twilio.conversations.Conversation
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.Media
import com.twilio.conversations.Message
import com.twilio.conversations.Participant
import com.twilio.conversations.StatusListener
import com.twilio.conversations.User
import com.twilio.conversations.app.common.enums.CrashIn
import com.twilio.conversations.extensions.addListener
import com.twilio.util.ErrorInfo
import com.twilio.util.ErrorInfo.Companion.CONVERSATION_NOT_SYNCHRONIZED
import com.twilio.util.TwilioException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// @todo: remove once multiple media is supported
val Message.firstMedia: Media? get() = attachedMedia.firstOrNull()

fun ConversationsClient.simulateCrash(where: CrashIn) {
    val method = this::class.java.getDeclaredMethod("simulateCrash", Int::class.java)
    method.isAccessible = true
    method.invoke(this, where.value)
}

suspend fun ConversationsClient.updateToken(token: String) = suspendCancellableCoroutine<Unit> { continuation ->
    updateToken(token, object : StatusListener {

        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(TwilioException(errorInfo))
    })
}

suspend fun Conversation.muteConversation(): Unit = suspendCoroutine { continuation ->
    setNotificationLevel(Conversation.NotificationLevel.MUTED, object : StatusListener {
        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(TwilioException(errorInfo))
    })
}

suspend fun Conversation.unmuteConversation(): Unit = suspendCoroutine { continuation ->
    setNotificationLevel(Conversation.NotificationLevel.DEFAULT, object : StatusListener {
        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(TwilioException(errorInfo))
    })
}

suspend fun Conversation.setFriendlyName(friendlyName: String): Unit = suspendCoroutine { continuation ->
    setFriendlyName(friendlyName, object : StatusListener {
        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(TwilioException(errorInfo))
    })
}

suspend fun Conversation.removeMessage(message: Message): Unit = suspendCoroutine { continuation ->
    removeMessage(message, object : StatusListener {

        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(TwilioException(errorInfo))
    })
}

suspend fun Conversation.getParticipantCount(): Long = suspendCancellableCoroutine { continuation ->
    getParticipantsCount(object : CallbackListener<Long> {

        override fun onSuccess(count: Long) = continuation.resume(count)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(TwilioException(errorInfo))
    })
}

suspend fun Conversation.getMessageCount(): Long = suspendCancellableCoroutine { continuation ->
    getMessagesCount(object : CallbackListener<Long> {

        override fun onSuccess(count: Long) = continuation.resume(count)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(TwilioException(errorInfo))
    })
}

suspend fun Conversation.getUnreadMessageCount(): Long? = suspendCancellableCoroutine { continuation ->
    getUnreadMessagesCount(object : CallbackListener<Long?> {

        override fun onSuccess(count: Long?) = continuation.resume(count)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(TwilioException(errorInfo))
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
                    complete.completeExceptionally(TwilioException(errorInfo))
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

suspend fun Participant.getAndSubscribeUser(): User = suspendCancellableCoroutine { continuation ->
    getAndSubscribeUser(object : CallbackListener<User> {

        override fun onSuccess(user: User) = continuation.resume(user)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(TwilioException(errorInfo))
    })
}

suspend fun User.setFriendlyName(friendlyName: String): Unit = suspendCoroutine { continuation ->
    setFriendlyName(friendlyName, object : StatusListener {
        override fun onSuccess() = continuation.resume(Unit)

        override fun onError(errorInfo: ErrorInfo) = continuation.resumeWithException(TwilioException(errorInfo))
    })
}

