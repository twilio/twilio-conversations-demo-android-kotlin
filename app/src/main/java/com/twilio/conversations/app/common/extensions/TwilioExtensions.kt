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
import com.twilio.util.ErrorInfo
import com.twilio.util.TwilioException
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
