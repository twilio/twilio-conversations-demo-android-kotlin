package com.twilio.conversations.app.common

import com.twilio.conversations.Conversation
import com.twilio.conversations.Conversation.ConversationStatus
import com.twilio.conversations.Conversation.NotificationLevel
import com.twilio.conversations.Message

private fun <K, V> Map<K, V>.reverse() = map { it.value to it.key }.toMap()

private val notificationLevelToIntMap = mapOf(
    NotificationLevel.DEFAULT to 0,
    NotificationLevel.MUTED to 1
)

val NotificationLevel.value: Int
    get() = notificationLevelToIntMap[this] ?: error("Unknown NotificationLevel: $this")

private val intToNotificationLevelMap = notificationLevelToIntMap.reverse()

fun NotificationLevelFromInt(value: Int): NotificationLevel =
    intToNotificationLevelMap[value] ?: error("Cannot find NotificationLevel: $value")

private val conversationStatusToIntMap = mapOf(
    ConversationStatus.JOINED to 1,
    ConversationStatus.NOT_PARTICIPATING to 2,
)

val ConversationStatus.value: Int
    get() = conversationStatusToIntMap[this] ?: error("Unknown ConversationStatus: $this")

private val intToConversationStatusMap = conversationStatusToIntMap.reverse()

fun ConversationStatusFromInt(value: Int): ConversationStatus =
    intToConversationStatusMap[value] ?: error("Cannot find ConversationStatus: $value")

private val messageTypeToIntMap = mapOf(
    Message.Type.TEXT to 0,
    Message.Type.MEDIA to 1,
)

val Message.Type.value: Int
    get() = messageTypeToIntMap[this] ?: error("Unknown Message.Type: $this")

private val intToMessageTypeMap = messageTypeToIntMap.reverse()

fun MessageTypeFromInt(value: Int): Message.Type =
    intToMessageTypeMap[value] ?: error("Cannot find Message.Type: $value")

private val conversationSynchronizationStatusToIntMap = mapOf(
    Conversation.SynchronizationStatus.NONE to 0,
    Conversation.SynchronizationStatus.IDENTIFIER to 1,
    Conversation.SynchronizationStatus.METADATA to 2,
    Conversation.SynchronizationStatus.ALL to 3,
    Conversation.SynchronizationStatus.FAILED to 4,
)

val Conversation.SynchronizationStatus.value: Int
    get() = conversationSynchronizationStatusToIntMap[this] ?: error("Unknown Conversation.SynchronizationStatus: $this")

private val intToConversationSynchronizationStatusMap = conversationSynchronizationStatusToIntMap.reverse()

fun Conversation.SynchronizationStatusFromInt(value: Int): Conversation.SynchronizationStatus =
    intToConversationSynchronizationStatusMap[value] ?: error("Cannot find Conversation.SynchronizationStatus: $this")
