@file:Suppress("IncorrectScope")

package com.twilio.conversations.app.testUtil

import com.twilio.conversations.Attributes
import com.twilio.conversations.Message
import com.twilio.conversations.Participant
import com.twilio.conversations.app.common.extensions.asDateString
import com.twilio.conversations.app.data.localCache.entity.MessageDataItem
import org.powermock.api.mockito.PowerMockito
import java.util.*

fun MessageDataItem.toMessageMock(participant: Participant): Message {
    val message = PowerMockito.mock(Message::class.java)

    whenCall(message.mediaSid).thenReturn(mediaSid)
    whenCall(message.mediaFileName).thenReturn(mediaFileName)
    whenCall(message.mediaType).thenReturn(mediaType)
    whenCall(message.sid).thenReturn(sid)
    whenCall(message.author).thenReturn(author)
    whenCall(message.conversationSid).thenReturn(conversationSid)
    whenCall(message.dateCreated).thenReturn(dateCreated.asDateString())
    whenCall(message.dateCreatedAsDate).thenReturn(Date(dateCreated))
    whenCall(message.participantSid).thenReturn(participantSid)
    whenCall(message.type).thenReturn(Message.Type.fromInt(type))
    whenCall(message.attributes).thenReturn(Attributes(attributes))
    whenCall(message.messageBody).thenReturn(body)
    whenCall(message.messageIndex).thenReturn(index)
    whenCall(message.participant).thenReturn(participant)

    return message
}
