package com.twilio.conversations.app.data.models

import com.twilio.conversations.ErrorInfo
import com.twilio.conversations.app.common.enums.ConversationsError

/**
 * Client creation response containing error info
 */
data class Error(val error: ConversationsError) : Response() {
    constructor(errorInfo: ErrorInfo) : this(ConversationsError.fromErrorInfo(errorInfo))
}
