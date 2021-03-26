package com.twilio.conversations.app.data.models

import com.twilio.conversations.ConversationsClient

/**
 * Client creation response containing successfully created conversations client
 */
data class Client(val conversationsClient: ConversationsClient) : Response()
