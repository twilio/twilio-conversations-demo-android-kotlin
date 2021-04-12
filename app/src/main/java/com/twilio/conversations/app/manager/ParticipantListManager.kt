package com.twilio.conversations.app.manager

import com.twilio.conversations.Attributes
import com.twilio.conversations.Participant
import com.twilio.conversations.app.common.extensions.*
import com.twilio.conversations.app.data.ConversationsClientWrapper
import org.json.JSONObject

interface ParticipantListManager {
    suspend fun addChatParticipant(identity: String)
    suspend fun addNonChatParticipant(phone: String, proxyPhone: String, friendlyName: String)
    suspend fun removeParticipant(sid: String)
}

private const val FRIENDLY_NAME_ATTRIBUTE = "friendlyName"

// Non-chat participants don't have associated user object by design, but we still need
// a friendlyName to display in UI
val Participant.friendlyName get() =
    runCatching { attributes.jsonObject?.getString(FRIENDLY_NAME_ATTRIBUTE) }.getOrNull()

class ParticipantListManagerImpl(
    private val conversationSid: String,
    private val conversationsClient: ConversationsClientWrapper
) : ParticipantListManager {

    override suspend fun addChatParticipant(identity: String) {
        val conversation = conversationsClient.getConversationsClient().getConversation(conversationSid)
        conversation.waitForSynchronization()
        conversation.addParticipantByIdentity(identity)
    }

    override suspend fun addNonChatParticipant(phone: String, proxyPhone: String, friendlyName: String) {
        val conversation = conversationsClient.getConversationsClient().getConversation(conversationSid)
        conversation.waitForSynchronization()

        val json = JSONObject("{ \"$FRIENDLY_NAME_ATTRIBUTE\": \"$friendlyName\" }")
        conversation.addParticipantByAddress(phone, proxyPhone, Attributes(json))
    }

    override suspend fun removeParticipant(sid: String) {
        val conversation = conversationsClient.getConversationsClient().getConversation(conversationSid)
        conversation.waitForSynchronization()

        val participant = conversation.participantsList.firstOrNull { it.sid == sid } ?: return
        conversation.removeParticipant(participant)
    }
}
