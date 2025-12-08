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
import org.powermock.api.mockito.PowerMockito
import java.util.*

fun MessageDataItem.toMessageMock(participant: Participant): Message {
    val message = PowerMockito.mock(Message::class.java)

    val mediaList = if (type == MessageType.TEXT.value || attachmentsList.isEmpty()) {
        emptyList()
    } else {
        listOf(
            PowerMockito.mock(com.twilio.conversations.Media::class.java).apply {
                val firstAttachment = attachmentsList.first()
                whenCall(sid).thenReturn(firstAttachment.sid)
                whenCall(contentType).thenReturn(firstAttachment.type ?: "")
                whenCall(category).thenReturn(MediaCategory.MEDIA)
                whenCall(filename).thenReturn(firstAttachment.fileName)
                whenCall(size).thenReturn(firstAttachment.size ?: 0)
            }
        )
    }
    
    whenCall(message.attachedMedia).thenReturn(mediaList)
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
