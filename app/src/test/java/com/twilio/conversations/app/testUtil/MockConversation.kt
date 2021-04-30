package com.twilio.conversations.app.testUtil

import com.twilio.conversations.Attributes
import com.twilio.conversations.Conversation
import com.twilio.conversations.Conversation.ConversationStatus
import com.twilio.conversations.Conversation.NotificationLevel
import com.twilio.conversations.Conversation.SynchronizationStatus.ALL
import com.twilio.conversations.ConversationListener
import com.twilio.conversations.app.common.extensions.asDateString
import com.twilio.conversations.app.data.localCache.entity.ConversationDataItem
import org.mockito.ArgumentCaptor
import org.powermock.api.mockito.PowerMockito
import java.util.*

fun ConversationDataItem.toConversationMock(
    synchronizationStatus: Conversation.SynchronizationStatus = ALL,
    attributes: String = "",
    conversationListenerCaptor : ArgumentCaptor<ConversationListener>? = null
): Conversation {
    val conversation = PowerMockito.mock(Conversation::class.java)

    whenCall(conversation.sid).thenReturn(sid)
    whenCall(conversation.friendlyName).thenReturn(friendlyName)
    whenCall(conversation.uniqueName).thenReturn(uniqueName)
    whenCall(conversation.dateUpdated).thenReturn(dateUpdated.asDateString())
    whenCall(conversation.dateUpdatedAsDate).thenReturn(Date(dateUpdated))
    whenCall(conversation.dateCreated).thenReturn(dateCreated.asDateString())
    whenCall(conversation.dateCreatedAsDate).thenReturn(Date(dateCreated))
    whenCall(conversation.createdBy).thenReturn(createdBy)
    whenCall(conversation.synchronizationStatus).thenReturn(synchronizationStatus)
    whenCall(conversation.status).thenReturn(ConversationStatus.fromInt(participatingStatus))
    whenCall(conversation.attributes).thenReturn(Attributes(attributes))
    whenCall(conversation.notificationLevel).thenReturn(NotificationLevel.fromInt(notificationLevel))

    if (conversationListenerCaptor != null) {
        whenCall(conversation.addListener(conversationListenerCaptor.capture())).then {  }
    }

    return conversation
}
