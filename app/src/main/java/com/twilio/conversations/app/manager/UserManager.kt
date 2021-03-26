package com.twilio.conversations.app.manager

import com.twilio.conversations.app.common.extensions.setFriendlyName
import com.twilio.conversations.app.data.ConversationsClientWrapper

interface UserManager {
    suspend fun setFriendlyName(friendlyName:String)
}

class UserManagerImpl(private val conversationsClient: ConversationsClientWrapper) : UserManager {

    override suspend fun setFriendlyName(friendlyName: String)
            = conversationsClient.getConversationsClient().myUser.setFriendlyName(friendlyName)

}
