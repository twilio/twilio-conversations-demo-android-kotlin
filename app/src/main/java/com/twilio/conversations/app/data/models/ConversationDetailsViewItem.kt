package com.twilio.conversations.app.data.models

data class ConversationDetailsViewItem(
    val conversationSid: String,
    val conversationName: String,
    val createdBy: String,
    val dateCreated: String,
    val isMuted: Boolean = false
)
