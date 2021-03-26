package com.twilio.conversations.app.common.enums

enum class MessageType(val value: Int) {
    TEXT(0),
    MEDIA(1);

    companion object {
        private val valuesMap = values().associateBy { it.value }
        fun fromInt(value: Int) = valuesMap[value] ?: error("Invalid value $value for MessageType")
    }
}
