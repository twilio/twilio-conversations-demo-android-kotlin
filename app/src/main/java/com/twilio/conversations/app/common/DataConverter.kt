package com.twilio.conversations.app.common

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import com.google.gson.Gson
import com.twilio.conversations.Conversation
import com.twilio.conversations.Conversation.NotificationLevel
import com.twilio.conversations.Message
import com.twilio.conversations.Participant
import com.twilio.conversations.User
import com.twilio.conversations.app.common.enums.Direction
import com.twilio.conversations.app.common.enums.DownloadState
import com.twilio.conversations.app.common.enums.MessageType
import com.twilio.conversations.app.common.enums.Reaction
import com.twilio.conversations.app.common.enums.Reactions
import com.twilio.conversations.app.common.enums.SendStatus
import com.twilio.conversations.app.common.extensions.asDateString
import com.twilio.conversations.app.common.extensions.asLastMesageStatusIcon
import com.twilio.conversations.app.common.extensions.asLastMessageDateString
import com.twilio.conversations.app.common.extensions.asLastMessageTextColor
import com.twilio.conversations.app.common.extensions.asMessageCount
import com.twilio.conversations.app.common.extensions.asMessageDateString
import com.twilio.conversations.app.data.localCache.entity.ConversationDataItem
import com.twilio.conversations.app.data.localCache.entity.MessageDataItem
import com.twilio.conversations.app.data.localCache.entity.ParticipantDataItem
import com.twilio.conversations.app.data.models.*
import com.twilio.conversations.app.manager.friendlyName

fun Conversation.toConversationDataItem(): ConversationDataItem {
    return ConversationDataItem(
        this.sid,
        this.friendlyName,
        this.attributes.toString(),
        this.uniqueName,
        this.dateUpdatedAsDate?.time ?: 0,
        this.dateCreatedAsDate?.time ?: 0,
        0,
        "",
        SendStatus.UNDEFINED.value,
        this.createdBy,
        0,
        0,
        0,
        this.status.value,
        this.notificationLevel.value
    )
}

fun Message.toMessageDataItem(currentUserIdentity: String = participant.identity, uuid: String = ""): MessageDataItem {
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

fun MessageDataItem.toMessageListViewItem(authorChanged: Boolean): MessageListViewItem {
    return MessageListViewItem(
        this.sid,
        this.uuid,
        this.index,
        Direction.fromInt(this.direction),
        this.author,
        authorChanged,
        this.body ?: "",
        this.dateCreated.asMessageDateString(),
        SendStatus.fromInt(sendStatus),
        sendStatusIcon = SendStatus.fromInt(this.sendStatus).asLastMesageStatusIcon(),
        getReactions(attributes).asReactionList(),
        MessageType.fromInt(this.type),
        this.mediaSid,
        this.mediaFileName,
        this.mediaType,
        this.mediaSize,
        this.mediaUri?.toUri(),
        this.mediaDownloadId,
        this.mediaDownloadedBytes,
        DownloadState.fromInt(this.mediaDownloadState),
        this.mediaUploading,
        this.mediaUploadedBytes,
        this.mediaUploadUri?.toUri(),
        this.errorCode
    )
}

fun getReactions(attributes: String): Map<String, Set<String>> = try {
    Gson().fromJson(attributes, ReactionAttributes::class.java).reactions
} catch (e: Exception) {
    emptyMap()
}

@SuppressLint("NewApi")
fun Map<String, Set<String>>.asReactionList(): Reactions {
    val reactions: MutableMap<Reaction, Set<String>> = mutableMapOf()
    forEach {
        try {
            reactions[Reaction.fromString(it.key)] = it.value
        } catch (e: Exception) {
        }
    }
    return reactions
}

fun Participant.asParticipantDataItem(typing: Boolean = false, user: User? = null) = ParticipantDataItem(
    sid = this.sid,
    conversationSid = this.conversation.sid,
    identity = this.identity,
    friendlyName = user?.friendlyName?.takeIf { it.isNotEmpty() } ?: this.friendlyName ?: this.identity,
    isOnline = user?.isOnline ?: false,
    lastReadMessageIndex = this.lastReadMessageIndex,
    lastReadTimestamp = this.lastReadTimestamp,
    typing = typing
)

fun User.asUserViewItem() = UserViewItem(
    friendlyName = this.friendlyName,
    identity = this.identity
)

fun ConversationDataItem.asConversationListViewItem(
    context: Context,
) = ConversationListViewItem(
    this.sid,
    if (this.friendlyName.isNotEmpty()) this.friendlyName else this.sid,
    this.participantsCount.toInt(),
    this.unreadMessagesCount.asMessageCount(),
    showUnreadMessageCount = this.unreadMessagesCount > 0,
    this.participatingStatus,
    lastMessageStateIcon = SendStatus.fromInt(this.lastMessageSendStatus).asLastMesageStatusIcon(),
    this.lastMessageText,
    lastMessageColor = SendStatus.fromInt(this.lastMessageSendStatus).asLastMessageTextColor(context),
    this.lastMessageDate.asLastMessageDateString(context),
    isMuted = this.notificationLevel == NotificationLevel.MUTED.value
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

fun List<ConversationDataItem>.asConversationListViewItems(context: Context) =
    map { it.asConversationListViewItem(context) }

fun List<Message>.asMessageDataItems(identity: String) = map { it.toMessageDataItem(identity) }

fun List<MessageDataItem>.asMessageListViewItems() =
    mapIndexed { index, item -> item.toMessageListViewItem(isAuthorChanged(index)) }

private fun List<MessageDataItem>.isAuthorChanged(index: Int): Boolean {
    if (index == 0) return true
    return this[index].author != this[index - 1].author
}

fun List<ParticipantDataItem>.asParticipantListViewItems() = map { it.toParticipantListViewItem() }

fun List<ConversationListViewItem>.merge(oldConversationList: List<ConversationListViewItem>?): List<ConversationListViewItem> {
    val oldConversationMap = oldConversationList?.associate { it.sid to it } ?: return this
    return map { item ->
        val oldItem = oldConversationMap[item.sid] ?: return@map item
        item.copy(isLoading = oldItem.isLoading)
    }
}
