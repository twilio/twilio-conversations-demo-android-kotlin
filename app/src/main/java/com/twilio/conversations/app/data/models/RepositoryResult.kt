package com.twilio.conversations.app.data.models

data class RepositoryResult<T>(
    val data: T,
    val requestStatus: RepositoryRequestStatus
)
