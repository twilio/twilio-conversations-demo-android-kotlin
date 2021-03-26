package com.twilio.conversations.app.common

import androidx.core.net.toUri
import com.google.gson.Gson
import com.twilio.conversations.*
import com.twilio.conversations.Conversation.NotificationLevel
import com.twilio.conversations.app.common.enums.Direction
import com.twilio.conversations.app.common.enums.MessageType
import com.twilio.conversations.app.common.enums.Reaction
import com.twilio.conversations.app.common.enums.SendStatus
import com.twilio.conversations.app.common.extensions.asDateString
import com.twilio.conversations.app.common.extensions.asMessageCount
import com.twilio.conversations.app.common.extensions.asTimeString
import com.twilio.conversations.app.data.localCache.entity.ConversationDataItem
import com.twilio.conversations.app.data.localCache.entity.MessageDataItem
import com.twilio.conversations.app.data.localCache.entity.ParticipantDataItem
import com.twilio.conversations.app.data.models.*

fun Conversation.toConversationDataItem() : ConversationDataItem {
    return ConversationDataItem(
        this.sid,
        this.friendlyName,
        this.attributes.toString(),
        this.uniqueName,
        this.dateUpdatedAsDate?.time ?: 0,
        this.dateCreatedAsDate?.time ?: 0,
        this.createdBy,
        0,
        0,
        0,
        this.status.value,
        this.notificationLevel.value
    )
}

fun Message.toMessageDataItem(currentUserIdentity: String = participant.identity, uuid: String = "") : MessageDataItem {
    return MessageDataItem(
        this.sid,
        this.conversationSid,
        this.participantSid,
        this.type.value,
        this.author,
        this.dateCreatedAsDate.time,
        this.messageBody ?: "",
        this.messageIndex,
        this.attributes.toString(),
        if (this.author == currentUserIdentity) Direction.OUTGOING.value else Direction.INCOMING.value,
        if (this.author == currentUserIdentity) SendStatus.SENT.value else SendStatus.UNDEFINED.value,
        uuid,
        if (this.type == Message.Type.MEDIA) this.mediaSid else null,
        if (this.type == Message.Type.MEDIA) this.mediaFileName else null,
        if (this.type == Message.Type.MEDIA) this.mediaType else null,
        if (this.type == Message.Type.MEDIA) this.mediaSize else null
    )
}

fun MessageDataItem.toMessageListViewItem() : MessageListViewItem {
    return MessageListViewItem(
        this.sid,
        this.uuid,
        this.index,
        Direction.fromInt(this.direction),
        this.author,
        this.body ?: "",
        this.dateCreated.asDateString(),
        SendStatus.fromInt(sendStatus),
        getReactions(attributes).asReactionList(),
        MessageType.fromInt(this.type),
        this.mediaSid,
        this.mediaFileName,
        this.mediaType,
        this.mediaSize,
        this.mediaUri?.toUri(),
        this.mediaDownloadId,
        this.mediaDownloadedBytes,
        this.mediaDownloading,
        this.mediaUploading,
        this.mediaUploadedBytes,
        this.mediaUploadUri?.toUri()
    )
}

fun getReactions(attributes: String): Map<String, Set<String>> = try {
    Gson().fromJson(attributes, ReactionAttributes::class.java).reactions
} catch (e: Exception) {
    emptyMap()
}

fun Map<String, Set<String>>.asReactionList(): Map<Reaction, Set<String>> {
    val reactions: MutableMap<Reaction, Set<String>> = mutableMapOf()
    forEach {
        try {
            reactions[Reaction.fromString(it.key)] = it.value
        } catch (e: Exception) {}
    }
    return reactions
}

fun Participant.asParticipantDataItem(typing : Boolean = false, user: User? = null) = ParticipantDataItem(
    sid = this.sid,
    conversationSid = this.conversation.sid,
    identity = this.identity,
    friendlyName = user?.identity ?: "",
    isOnline = user?.isOnline ?: false,
    lastReadMessageIndex = this.lastReadMessageIndex,
    lastReadTimestamp = this.lastReadTimestamp,
    typing = typing
)

fun User.asUserViewItem() = UserViewItem(
    friendlyName = this.friendlyName,
    identity = this.identity
)

fun ConversationDataItem.asConversationListViewItem() = ConversationListViewItem(
    this.sid,
    this.friendlyName,
    this.dateCreated.asDateString(),
    this.dateUpdated.asTimeString(),
    this.participantsCount,
    this.messagesCount.asMessageCount(),
    this.participatingStatus,
    this.notificationLevel == NotificationLevel.MUTED.value
)

fun ConversationDataItem.asConversationDetailsViewItem() = ConversationDetailsViewItem(
    this.sid,
    this.friendlyName,
    this.createdBy,
    this.dateCreated.asDateString(),
    this.notificationLevel == NotificationLevel.MUTED.value
)

fun ParticipantDataItem.toParticipantListViewItem() = ParticipantListViewItem(
    conversationSid = this.conversationSid,
    sid = this.sid,
    identity = this.identity,
    friendlyName = this.friendlyName,
    isOnline = this.isOnline
)

fun List<ConversationDataItem>.asConversationListViewItems() = map { it.asConversationListViewItem() }

fun List<Message>.asMessageDataItems(identity: String) = map { it.toMessageDataItem(identity) }

fun List<MessageDataItem>.asMessageListViewItems() = map { it.toMessageListViewItem() }

fun List<ParticipantDataItem>.asParticipantListViewItems() = map { it.toParticipantListViewItem() }

fun List<ConversationListViewItem>.merge(oldConversationList: List<ConversationListViewItem>?): List<ConversationListViewItem> {
    val oldConversationMap = oldConversationList?.associate { it.sid to it } ?: return this
    return map { item ->
        val oldItem = oldConversationMap[item.sid] ?: return@map item
        item.copy(isLoading = oldItem.isLoading)
    }
}
