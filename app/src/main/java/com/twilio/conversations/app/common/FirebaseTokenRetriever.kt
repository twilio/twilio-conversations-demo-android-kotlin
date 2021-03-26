package com.twilio.conversations.app.common

import com.google.firebase.messaging.FirebaseMessaging
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.extensions.ConversationsException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseTokenRetriever {

    suspend fun retrieveToken() = suspendCoroutine<String> { continuation ->
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener { task ->
            try {
                task.result?.let { continuation.resume(it) }
                    ?: continuation.resumeWithException(ConversationsException(ConversationsError.TOKEN_ERROR))
            } catch (e: Exception) {
                // TOO_MANY_REGISTRATIONS thrown on devices with too many Firebase instances
                continuation.resumeWithException(ConversationsException(ConversationsError.TOKEN_ERROR))
            }
        }
    }
}
