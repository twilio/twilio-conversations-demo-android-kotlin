@file:Suppress("IncorrectScope")

package com.twilio.conversations.app.testUtil

import com.twilio.conversations.Attributes
import com.twilio.conversations.MediaCategory
import com.twilio.conversations.Message
import com.twilio.conversations.Participant
import com.twilio.conversations.app.common.enums.MessageType
import com.twilio.conversations.app.common.extensions.asDateString
import com.twilio.conversations.app.common.extensions.firstMedia
import com.twilio.conversations.app.data.localCache.entity.MessageDataItem
import io.mockk.every
import io.mockk.mockk
import org.powermock.api.mockito.PowerMockito
import java.util.*

fun MessageDataItem.toMessageMock(participant: Participant): Message {
    val message = PowerMockito.mock(Message::class.java)

    every { message.firstMedia } returns if (type == MessageType.TEXT.value) null else mockk {
        every { sid } returns (mediaSid ?: "")
        every { contentType } returns (mediaType ?: "")
        every { category } returns MediaCategory.MEDIA
        every { filename } returns mediaFileName
        every { size } returns (mediaSize ?: 0)
    }
    whenCall(message.sid).thenReturn(sid)
    whenCall(message.author).thenReturn(author)
    whenCall(message.conversationSid).thenReturn(conversationSid)
    whenCall(message.dateCreated).thenReturn(dateCreated.asDateString())
    whenCall(message.dateCreatedAsDate).thenReturn(Date(dateCreated))
    whenCall(message.participantSid).thenReturn(participantSid)
    whenCall(message.attributes).thenReturn(Attributes(attributes))
    whenCall(message.body).thenReturn(body)
    whenCall(message.messageIndex).thenReturn(index)
    whenCall(message.participant).thenReturn(participant)

    return message
}
