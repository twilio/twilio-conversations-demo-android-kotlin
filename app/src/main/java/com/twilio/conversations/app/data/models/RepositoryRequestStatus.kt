package com.twilio.conversations.app.data.models

import com.twilio.conversations.app.common.enums.ConversationsError

sealed class RepositoryRequestStatus {
    object FETCHING : RepositoryRequestStatus()
    object SUBSCRIBING : RepositoryRequestStatus()
    object COMPLETE : RepositoryRequestStatus()
    class Error(val error: ConversationsError) : RepositoryRequestStatus()
}
