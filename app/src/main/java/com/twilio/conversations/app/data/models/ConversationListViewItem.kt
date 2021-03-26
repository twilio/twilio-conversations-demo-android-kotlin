package com.twilio.conversations.app.data.models

data class ConversationListViewItem(
    val sid: String,
    val name: String,
    val dateCreated: String,
    val dateUpdated: String,
    val participantCount: Long,
    val messageCount: String,
    val participatingStatus: Int,
    val isMuted: Boolean = false,
    val isLoading: Boolean = false
)
