package com.twilio.conversations.app.manager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.twilio.conversations.NotificationPayload
import com.twilio.conversations.app.R
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.common.extensions.registerFCMToken
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.data.CredentialStorage
import com.twilio.conversations.app.ui.ConversationListActivity
import com.twilio.conversations.app.ui.MessageListActivity
import timber.log.Timber

private const val NOTIFICATION_CONVERSATION_ID = "twilio_notification_id"
private const val NOTIFICATION_NAME = "Twilio Notification"
private const val NOTIFICATION_ID = 1234

interface FCMManager : LifecycleObserver {
    suspend fun onNewToken(token: String)
    suspend fun onMessageReceived(payload: NotificationPayload)
    fun getTargetIntent(type: NotificationPayload.Type, conversationSid: String): Intent
    fun showNotification(payload: NotificationPayload)
}

class FCMManagerImpl(
    private val context: Context,
    private val conversationsClient: ConversationsClientWrapper,
    private val credentialStorage: CredentialStorage,
) : FCMManager {

    private val notificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private var isBackgrounded = true

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override suspend fun onNewToken(token: String) {
        Timber.d("FCM Token received: $token")
        try {
            if (token != credentialStorage.fcmToken && conversationsClient.isClientCreated) {
                conversationsClient.getConversationsClient().registerFCMToken(token)
            }
            credentialStorage.fcmToken = token
        } catch (e: ConversationsException) {
            Timber.d("Failed to register FCM token")
        }
    }

    override suspend fun onMessageReceived(payload: NotificationPayload) {
        if (conversationsClient.isClientCreated) {
            conversationsClient.getConversationsClient().handleNotification(payload)
        }
        Timber.d("Message received: $payload, ${payload.type}, $isBackgrounded")
        // Ignore everything we don't support
        if (payload.type == NotificationPayload.Type.UNKNOWN) return

        if (isBackgrounded) {
            showNotification(payload)
        }
    }

    override fun getTargetIntent(type: NotificationPayload.Type, conversationSid: String): Intent {
        return when (type) {
            NotificationPayload.Type.NEW_MESSAGE -> MessageListActivity.getStartIntent(context, conversationSid)
            NotificationPayload.Type.ADDED_TO_CONVERSATION -> MessageListActivity.getStartIntent(context, conversationSid)
            NotificationPayload.Type.REMOVED_FROM_CONVERSATION -> ConversationListActivity.getStartIntent(context)
            else -> ConversationListActivity.getStartIntent(context)
        }
    }

    override fun showNotification(payload: NotificationPayload) {
        Timber.d("showNotification: ${payload.conversationSid}")

        val intent = getTargetIntent(payload.type, payload.conversationSid)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val title = when (payload.type) {
            NotificationPayload.Type.NEW_MESSAGE -> context.getString(R.string.notification_new_message)
            NotificationPayload.Type.ADDED_TO_CONVERSATION -> context.getString(R.string.notification_added_to_conversation)
            NotificationPayload.Type.REMOVED_FROM_CONVERSATION -> context.getString(R.string.notification_removed_from_conversation)
            else -> context.getString(R.string.notification_generic)
        }

        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CONVERSATION_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(payload.body)
            .setAutoCancel(true)
            .setPriority(PRIORITY_HIGH)
            .setVisibility(VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setColor(Color.rgb(214, 10, 37))

        val soundFileName = payload.sound
        if (context.resources.getIdentifier(soundFileName, "raw", context.packageName) != 0) {
            val sound = Uri.parse("android.resource://${context.packageName}/raw/$soundFileName")
            notificationBuilder.setSound(sound)
            Timber.d("Playing specified sound $soundFileName")
        } else {
            notificationBuilder.setDefaults(Notification.DEFAULT_SOUND)
            Timber.d("Playing default sound")
        }
        val notification = notificationBuilder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CONVERSATION_ID,
                NOTIFICATION_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        isBackgrounded = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        isBackgrounded = false
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
