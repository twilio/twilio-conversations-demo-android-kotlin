package com.twilio.conversations.app.manager

import com.twilio.conversations.app.common.extensions.createConversation
import com.twilio.conversations.app.common.extensions.destroy
import com.twilio.conversations.app.common.extensions.getConversation
import com.twilio.conversations.app.common.extensions.join
import com.twilio.conversations.app.common.extensions.leave
import com.twilio.conversations.app.common.extensions.muteConversation
import com.twilio.conversations.app.common.extensions.setFriendlyName
import com.twilio.conversations.app.common.extensions.unmuteConversation
import com.twilio.conversations.app.data.ConversationsClientWrapper

interface ConversationListManager {
    suspend fun createConversation(friendlyName: String): String
    suspend fun joinConversation(conversationSid: String)
    suspend fun removeConversation(conversationSid: String)
    suspend fun leaveConversation(conversationSid: String)
    suspend fun muteConversation(conversationSid: String)
    suspend fun unmuteConversation(conversationSid: String)
    suspend fun renameConversation(conversationSid: String, friendlyName: String)
}

class ConversationListManagerImpl(private val conversationsClient: ConversationsClientWrapper) : ConversationListManager {

    override suspend fun createConversation(friendlyName: String): String
            = conversationsClient.getConversationsClient().createConversation(friendlyName).sid

    override suspend fun joinConversation(conversationSid: String): Unit
            = conversationsClient.getConversationsClient().getConversation(conversationSid).join()

    override suspend fun removeConversation(conversationSid: String): Unit
            = conversationsClient.getConversationsClient().getConversation(conversationSid).destroy()

    override suspend fun leaveConversation(conversationSid: String): Unit
            = conversationsClient.getConversationsClient().getConversation(conversationSid).leave()

    override suspend fun muteConversation(conversationSid: String): Unit
            = conversationsClient.getConversationsClient().getConversation(conversationSid).muteConversation()

    override suspend fun unmuteConversation(conversationSid: String): Unit
            = conversationsClient.getConversationsClient().getConversation(conversationSid).unmuteConversation()

    override suspend fun renameConversation(conversationSid: String, friendlyName: String)
            = conversationsClient.getConversationsClient().getConversation(conversationSid).setFriendlyName(friendlyName)

}
