package com.twilio.conversations.app.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.twilio.conversations.NotificationPayload
import com.twilio.conversations.app.common.injector
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class FCMListenerService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private fun launch(block: suspend CoroutineScope.() -> Unit) = serviceScope.launch(
        context = CoroutineExceptionHandler { _, e -> Timber.e(e, "Coroutine failed ${e.localizedMessage}") },
        block = block
    )

    private val fcmManager by lazy { injector.createFCMManager(application) }

    private val credentialStorage by lazy { injector.createCredentialStorage(applicationContext) }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        launch {
            fcmManager.onNewToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        launch {
            Timber.d("onMessageReceived for FCM from: ${remoteMessage.from}")

            // Check if message contains a data payload and we have saved FCM token
            // If we don't have FCM token - probably we receive this message because of invalidating the token
            // on logout has failed (probably device has been offline). We will invalidate the token on
            // next login then.
            if (remoteMessage.data.isNotEmpty() && credentialStorage.fcmToken.isNotEmpty()) {
                Timber.d("Data Message Body: ${remoteMessage.data}")
                fcmManager.onMessageReceived(NotificationPayload(remoteMessage.data))
            }
        }
    }
}
