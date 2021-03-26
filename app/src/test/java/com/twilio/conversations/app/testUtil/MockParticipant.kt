package com.twilio.conversations.app.testUtil

import com.twilio.conversations.Attributes
import com.twilio.conversations.Conversation
import com.twilio.conversations.Participant
import com.twilio.conversations.app.data.localCache.entity.ParticipantDataItem
import org.powermock.api.mockito.PowerMockito

fun ParticipantDataItem.toParticipantMock(conversation: Conversation): Participant {
    val participant = PowerMockito.mock(Participant::class.java)
    whenCall(participant.sid).thenReturn(sid)
    whenCall(participant.identity).thenReturn(identity)
    whenCall(participant.conversation).thenReturn(conversation)
    whenCall(participant.attributes).thenReturn(Attributes("\"\""))
    whenCall(participant.lastReadMessageIndex).thenReturn(lastReadMessageIndex)
    whenCall(participant.lastReadTimestamp).thenReturn(lastReadTimestamp)
    whenCall(participant.type).thenReturn(Participant.Type.CHAT)
    return participant
}
