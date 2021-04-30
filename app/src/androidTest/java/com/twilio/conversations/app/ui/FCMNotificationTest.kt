package com.twilio.conversations.app.ui

import android.os.Bundle
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.firebase.messaging.RemoteMessage
import com.twilio.conversations.NotificationPayload
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.data.CredentialStorage
import com.twilio.conversations.app.manager.FCMManager
import com.twilio.conversations.app.manager.FCMManagerImpl
import com.twilio.conversations.app.testUtil.verifyActivityVisible
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val NOTIFICATION_WAIT_TIME = 5000L

@RunWith(AndroidJUnit4::class)
class FCMNotificationTest {

    private lateinit var credentialStorage: CredentialStorage
    private lateinit var fcmManager: FCMManager

    @Before
    @UiThreadTest
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ConversationsClientWrapper.recreateInstance(context)
        credentialStorage = CredentialStorage(context)
        fcmManager = FCMManagerImpl(context, ConversationsClientWrapper.INSTANCE, credentialStorage)
    }

    @Test
    fun notificationNewMessage() {
        val messageBody = "New Message Notification"
        val bundle = Bundle().apply {
            putString("conversation_sid", "conversation_sid")
            putString("twi_body", messageBody)
            putString("twi_message_type", "twilio.conversations.new_message")
        }
        val remoteMessage = RemoteMessage(bundle)
        clickOnNotification(remoteMessage)
        verifyActivityVisible<MessageListActivity>()
    }

    @Test
    fun notificationAddedToConversation() {
        val messageBody = "Added to conversation"
        val bundle = Bundle().apply {
            putString("conversation_sid", "conversation_sid")
            putString("twi_body", messageBody)
            putString("twi_message_type", "twilio.conversations.added_to_conversation")
        }
        val remoteMessage = RemoteMessage(bundle)
        clickOnNotification(remoteMessage)
        verifyActivityVisible<MessageListActivity>()
    }

    @Test
    fun notificationRemovedFromConversation() {
        val messageBody = "Removed from conversation"
        val bundle = Bundle().apply {
            putString("conversation_sid", "conversation_sid")
            putString("twi_body", messageBody)
            putString("twi_message_type", "twilio.conversations.removed_from_conversation")
        }
        val remoteMessage = RemoteMessage(bundle)
        clickOnNotification(remoteMessage)
        verifyActivityVisible<ConversationListActivity>()
    }

    private fun clickOnNotification(remoteMessage: RemoteMessage) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val payload = NotificationPayload(remoteMessage.data)
        fcmManager.showNotification(payload)
        device.openNotification()
        val notification = device.wait(Until.findObject(By.textContains(payload.body)), NOTIFICATION_WAIT_TIME)
        notification.click()
    }
}
