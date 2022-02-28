package com.twilio.conversations.app.ui

import android.os.Bundle
import android.text.format.Formatter
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
import junit.framework.Assert.assertNotNull
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
    fun notificationNewMediaMessage() {
        val messageBody = "New Message Notification"
        val bundle = Bundle().apply {
            putString("twi_message_type", "twilio.conversations.new_message")
            putString("twi_body", "user01 sent message '' with 1 attachments; test_image.jpeg")
            putString("conversation_sid", "conversation_sid")
            putString("conversation_title", "The Conversation")
            putString("message_sid", "IMc5ebc87c7c824190bb9ff86e3dccd7a5")
            putString("message_index", "123")
            putString("media_count", "1")
            putString("media", """{"filename":"test_image.jpeg","size":9941,"content_type":"image\/jpeg","sid":"ME57ca700ddb9eeb512169c45f0e142328"}""")
        }
        val remoteMessage = RemoteMessage(bundle)
        clickOnNotification(remoteMessage, "Media: test_image.jpeg")
        verifyActivityVisible<MessageListActivity>()
    }

    @Test
    fun notificationNewMediaMessageNoFilename() {
        val messageBody = "New Message Notification"
        val bundle = Bundle().apply {
            putString("twi_message_type", "twilio.conversations.new_message")
            putString("twi_body", "user01 sent message '' with 1 attachments; test_image.jpeg")
            putString("conversation_sid", "conversation_sid")
            putString("conversation_title", "The Conversation")
            putString("message_sid", "IMc5ebc87c7c824190bb9ff86e3dccd7a5")
            putString("message_index", "123")
            putString("media_count", "1")
            putString("media", """{"size":9941,"content_type":"image\/jpeg","sid":"ME57ca700ddb9eeb512169c45f0e142328"}""")
        }
        val remoteMessage = RemoteMessage(bundle)
        clickOnNotification(remoteMessage, "Media: " + Formatter.formatShortFileSize(InstrumentationRegistry.getInstrumentation().targetContext, 9941))
        verifyActivityVisible<MessageListActivity>()
    }

    @Test
    fun notificationNewMultipleMediaMessage() {
        val messageBody = "New Message Notification"
        val bundle = Bundle().apply {
            putString("twi_message_type", "twilio.conversations.new_message")
            putString("twi_body", "user01 sent message '' with 10 attachments; test_image.jpeg")
            putString("conversation_sid", "conversation_sid")
            putString("conversation_title", "The Conversation")
            putString("message_sid", "IMc5ebc87c7c824190bb9ff86e3dccd7a5")
            putString("message_index", "123")
            putString("media_count", "10")
            putString("media", """{"filename":"test_image.jpeg","size":9941,"content_type":"image\/jpeg","sid":"ME57ca700ddb9eeb512169c45f0e142328"}""")
        }
        val remoteMessage = RemoteMessage(bundle)
        clickOnNotification(remoteMessage, "Media message with 10 attachments")
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

    private fun clickOnNotification(remoteMessage: RemoteMessage, findText: String? = null) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val payload = NotificationPayload(remoteMessage.data)
        fcmManager.showNotification(payload)
        device.openNotification()
        val notification = device.wait(
            Until.findObject(By.textContains(findText ?: payload.body)),
            NOTIFICATION_WAIT_TIME)
        assertNotNull(notification)
        notification.click()
    }
}
