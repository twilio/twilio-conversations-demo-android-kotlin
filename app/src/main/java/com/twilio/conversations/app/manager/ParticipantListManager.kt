package com.twilio.conversations.app.manager

import com.twilio.conversations.app.common.extensions.addParticipantByIdentity
import com.twilio.conversations.app.common.extensions.getConversation
import com.twilio.conversations.app.common.extensions.removeParticipantByIdentity
import com.twilio.conversations.app.common.extensions.waitForSynchronization
import com.twilio.conversations.app.data.ConversationsClientWrapper

interface ParticipantListManager {
    suspend fun addChatParticipant(identity: String)
    suspend fun removeParticipant(identity: String)
}

class ParticipantListManagerImpl(
    private val conversationSid: String,
    private val conversationsClient: ConversationsClientWrapper
) : ParticipantListManager {

    override suspend fun addChatParticipant(identity: String) {
        val conversation = conversationsClient.getConversationsClient().getConversation(conversationSid)
        conversation.waitForSynchronization()
        conversation.addParticipantByIdentity(identity)
    }

    override suspend fun removeParticipant(identity: String) {
        val conversation = conversationsClient.getConversationsClient().getConversation(conversationSid)
        conversation.waitForSynchronization()
        conversation.removeParticipantByIdentity(identity)
    }
}
