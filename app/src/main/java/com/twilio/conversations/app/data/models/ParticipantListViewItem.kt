package com.twilio.conversations.app.data.models

data class ParticipantListViewItem(
    val sid: String,
    val identity: String,
    val conversationSid: String,
    val friendlyName: String,
    val isOnline: Boolean
)
