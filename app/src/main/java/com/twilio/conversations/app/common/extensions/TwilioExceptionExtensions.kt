package com.twilio.conversations.app.common.extensions

import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.enums.toErrorInfo
import com.twilio.util.TwilioException

fun TwilioException.toConversationsError(): ConversationsError =
    ConversationsError.fromErrorInfo(errorInfo)

fun createTwilioException(error: ConversationsError): TwilioException =
    TwilioException(error.toErrorInfo())
